package com.mobdeve.s17.catchow;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;
import com.google.firebase.dynamiclinks.PendingDynamicLinkData;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.mobdeve.s17.catchow.adapters.Restaurant_RVAdapter;
import com.mobdeve.s17.catchow.models.Restaurant;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    ArrayList<Restaurant> restaurantList = new ArrayList<>();
    FirebaseFirestore db;
    ImageButton profile_btn;
    SearchView res_sv;
    Button res_btn;
    Button coffee_btn;
    Button des_btn;
    BottomNavigationView navbar;
    private String currentEmail;

    // Restaurant Recycler View Variables
    RecyclerView restaurant_rv;
    Restaurant_RVAdapter adapter;
    LinearLayoutManager horizontalLayoutManager;

    GoogleSignInOptions gso;
    GoogleSignInClient gsc;
    TextView greeting_text;
    FirebaseAuth auth;
    FirebaseFirestore firestore;

    private static final String PREFS_NAME = "CatChowPrefs";
    private static final String KEY_DB_INITIALIZED = "database_initialized";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Declarations
        profile_btn = findViewById(R.id.top_profile_btn);
        restaurant_rv = findViewById(R.id.restaurant_rv);
        res_sv = findViewById(R.id.restaurant_search);
        res_btn = findViewById(R.id.res_btn);
        coffee_btn = findViewById(R.id.coffee_btn);
        des_btn = findViewById(R.id.dessert_btn);
        navbar = findViewById(R.id.navbar);
        greeting_text = findViewById(R.id.greeting_txt);
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isInitialized = prefs.getBoolean(KEY_DB_INITIALIZED, false);

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            currentEmail = currentUser.getEmail();
        }

        if (!isInitialized) {
            initializeRoles();
            setupAdminUser("admin@email.com");

            prefs.edit().putBoolean(KEY_DB_INITIALIZED, true).apply();
        }

        //Setup Bottom Navigation View
        setupBottomNavigationView();

        // Setup Search View
        setupSearchView();

        // Setup Firestore Database
        db = FirebaseFirestore.getInstance();

        setupRolesAndAdmin();

        // Setup Restaurant Recycler View
        setupRestaurantRecyclerView();

        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build();
        gsc = GoogleSignIn.getClient(this,gso);

        GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(this);
        if (acct != null) {
            String personName = acct.getDisplayName();
            greeting_text.setText("Hello, " + personName + "!");
        }

        if (currentUser != null) {
            String userEmail = currentUser.getEmail();

            firestore.collection("users")
                    .whereEqualTo("email", userEmail)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                            String name = documentSnapshot.getString("name");
                            String firstName = extractFirstName(name);

                            greeting_text.setText("Hello, " + firstName + "!");
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(MainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
        checkForDynamicLinks();
    }

    private void checkForDynamicLinks() {
        FirebaseDynamicLinks.getInstance()
                .getDynamicLink(getIntent())
                .addOnSuccessListener(this, new OnSuccessListener<PendingDynamicLinkData>() {
                    @Override
                    public void onSuccess(PendingDynamicLinkData pendingDynamicLinkData) {
                        // Get deep link from result (may be null if no link is found)
                        Uri deepLink = null;
                        if (pendingDynamicLinkData != null) {
                            deepLink = pendingDynamicLinkData.getLink();

                            // Only forward if we actually have a deep link
                            if (deepLink != null) {
                                // Forward to your LoginWithEmailLinkActivity
                                Intent intent = new Intent(MainActivity.this, LoginWithEmailLinkActivity.class);
                                intent.setData(deepLink);
                                startActivity(intent);
                            }
                        }
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "getDynamicLink:onFailure", e);
                    }
                });
    }

    private void initializeRoles() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Define admin role
        Map<String, Object> adminRole = new HashMap<>();
        adminRole.put("name", "admin");
        adminRole.put("permissions", Arrays.asList(
                "view_users", "delete_users", "view_orders", "view_logs"
        ));

        // Define user role
        Map<String, Object> userRole = new HashMap<>();
        userRole.put("name", "user");
        userRole.put("permissions", Arrays.asList(
                "place_orders", "manage_profile", "manage_addresses"
        ));

        // Add roles to Firestore
        db.collection("roles").document("admin").set(adminRole);
        db.collection("roles").document("user").set(userRole);
    }

    private void setupAdminUser(String adminEmail) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users")
                .whereEqualTo("email", adminEmail)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot userDoc = queryDocumentSnapshots.getDocuments().get(0);
                        db.collection("users").document(userDoc.getId())
                                .update("role", "admin")
                                .addOnSuccessListener(aVoid ->
                                        Log.d("Admin", "User updated to admin successfully"))
                                .addOnFailureListener(e ->
                                        Log.e("Admin", "Error updating user to admin", e));
                    }
                });
    }

    private String extractFirstName(String fullName) {
        if (fullName != null && fullName.contains(" ")) {
            return fullName.substring(0, fullName.indexOf(" "));
        }
        return fullName;
    }

    private void setupBottomNavigationView() {
        navbar.setSelectedItemId(R.id.menu_home);
        navbar.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_home) {
                return true;
            }
            else if (id == R.id.menu_cart) {
                startActivity(new Intent(getApplicationContext(),CartActivity.class));
                overridePendingTransition(R.anim.fade_in,R.anim.fade_out);
                return true;
            }
            else if (id == R.id.menu_address) {
                startActivity(new Intent(getApplicationContext(), AddressActivity.class));
                overridePendingTransition(R.anim.fade_in,R.anim.fade_out);
                return true;
            }
            else if (id == R.id.menu_profile) {
                startActivity(new Intent(getApplicationContext(),ProfileActivity.class));
                overridePendingTransition(R.anim.fade_in,R.anim.fade_out);
                return true;
            }
            return false;
        });
    }

    private void setupRolesAndAdmin() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Check if roles collection exists
        db.collection("roles").get().addOnSuccessListener(queryDocumentSnapshots -> {
            if (queryDocumentSnapshots.isEmpty()) {
                // Initialize roles
                initializeRoles();

                // Set up initial admin (replace with your admin email)
                setupAdminUser("admin@example.com");
            }
        });
    }

    private void setupSearchView() {
        res_sv.clearFocus();
        res_sv.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                search(newText);
                return true;
            }
        });
    }

    private void setupRestaurantRecyclerView() {
        db.collection("restaurants")
                .orderBy("name", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                Log.d(TAG, "Success: Get Restaurants Request Successful!");
                List<DocumentSnapshot> snapshotList = queryDocumentSnapshots.getDocuments();
                for (DocumentSnapshot snapshot: snapshotList) {
                    Log.d(TAG, "Restaurant: " + snapshot.getData());
                    Restaurant res = new Restaurant(
                            snapshot.getString("imageurl"),
                            snapshot.getString("name"),
                            snapshot.getString("type"),
                            snapshot.getString("level"),
                            snapshot.getDouble("minimum"),
                            snapshot.getDouble("fee"),
                            snapshot.getString("duration"),
                            snapshot.getString("distance")
                    );
                    restaurantList.add(res);
                }

                // Initialize Recycler View
                adapter = new Restaurant_RVAdapter(MainActivity.this, restaurantList);
                restaurant_rv.setAdapter(adapter);
                horizontalLayoutManager = new LinearLayoutManager(MainActivity.this, LinearLayoutManager.HORIZONTAL, false);
                restaurant_rv.setLayoutManager(horizontalLayoutManager);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e(TAG, "Failure: ", e);
            }
        });
    }

    public void profile(View v) {
        startActivity(new Intent(getApplicationContext(),ProfileActivity.class));
        overridePendingTransition(R.anim.fade_in,R.anim.fade_out);
        finish();
    }

    private void search(String newText) {
        AuditLogger.log(currentEmail, "SEARCH_RESTAURANTS", "QUERY_" + newText, true);
        ArrayList<Restaurant> filteredList = new ArrayList<>();
        for (Restaurant res: restaurantList) {
            if (res.getName().toLowerCase().contains(newText.toLowerCase())) {
                filteredList.add(res);
            }
            adapter.setFilteredList(filteredList);
        }
    }

    public void filterAll(View v) {
        adapter.setFilteredList(restaurantList);
    }

    public void filterRestaurant(View v) {
        AuditLogger.log(currentEmail, "FILTER_RESTAURANTS", "FILTER_TYPE_RESTAURANT", true);
        ArrayList<Restaurant> filteredList = new ArrayList<>();
        for (Restaurant res: restaurantList) {
            if (res.getType().toLowerCase().contains("Restaurant".toLowerCase())) {
                filteredList.add(res);
            }
            adapter.setFilteredList(filteredList);
        }
    }

    public void filterCoffee(View v) {
        AuditLogger.log(currentEmail, "FILTER_RESTAURANTS", "FILTER_TYPE_CAFE", true);
        ArrayList<Restaurant> filteredList = new ArrayList<>();
        for (Restaurant res: restaurantList) {
            if (res.getType().toLowerCase().contains("Cafe".toLowerCase())) {
                filteredList.add(res);
            }
            adapter.setFilteredList(filteredList);
        }
    }

    public void filterDessert(View v) {
        AuditLogger.log(currentEmail, "FILTER_RESTAURANTS", "FILTER_TYPE_DESSERT", true);
        ArrayList<Restaurant> filteredList = new ArrayList<>();
        for (Restaurant res: restaurantList) {
            if (res.getType().toLowerCase().contains("Dessert".toLowerCase())) {
                filteredList.add(res);
            }
            adapter.setFilteredList(filteredList);
        }
    }

    private void clearSharedPreferencesData() {
        SharedPreferences sharedPreferences = getSharedPreferences("cartPreferences", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart: Activity started");
//        clearSharedPreferencesData();
        checkForDynamicLinks();
    }

    @Override
    protected void onResume() {
        super.onResume();
        AuditLogger.log(currentEmail, "VIEW_HOME", "MAIN_SCREEN", true);
        Log.d(TAG, "onResume: Activity resumed");
//        clearSharedPreferencesData();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: Activity paused");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop: Activity stopped");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: Activity destroyed");
    }
}