package com.mobdeve.s17.catchow;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mobdeve.s17.catchow.adapters.UserAdapter;
import com.mobdeve.s17.catchow.models.Users;

import java.util.ArrayList;

public class AdminActivity extends AppCompatActivity {

    private RecyclerView usersRecyclerView;
    private UserAdapter userAdapter;
    private ArrayList<Users> usersList = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private TextView adminNameTextView;
    private String currentUserEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        currentUserEmail = auth.getCurrentUser().getEmail();

        // Initialize views
        adminNameTextView = findViewById(R.id.admin_name);
        usersRecyclerView = findViewById(R.id.users_recycler_view);

        // Set up RecyclerView
        userAdapter = new UserAdapter(this, usersList, currentUserEmail);
        usersRecyclerView.setAdapter(userAdapter);
        usersRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Check admin permissions
        checkAdminPermissions();

        // Load users
        loadUsers();

        // Set up back button
        Button backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());
    }

    private void checkAdminPermissions() {
        db.collection("users")
                .whereEqualTo("email", currentUserEmail)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot userDoc = queryDocumentSnapshots.getDocuments().get(0);
                        String role = userDoc.getString("role");

                        if (!"admin".equals(role)) {
                            Toast.makeText(this, "Access denied. Admin permissions required.", Toast.LENGTH_LONG).show();
                            finish();
                        } else {
                            adminNameTextView.setText("Admin: " + userDoc.getString("name"));
                        }
                    } else {
                        Toast.makeText(this, "User not found.", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("AdminActivity", "Error checking permissions", e);
                    Toast.makeText(this, "Error checking permissions.", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void loadUsers() {
        db.collection("users")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    usersList.clear();
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        Users user = document.toObject(Users.class);
                        usersList.add(user);
                    }
                    userAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e("AdminActivity", "Error loading users", e);
                    Toast.makeText(this, "Error loading users.", Toast.LENGTH_SHORT).show();
                });
    }
}