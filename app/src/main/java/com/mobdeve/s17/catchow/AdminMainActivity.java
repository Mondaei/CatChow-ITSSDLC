package com.mobdeve.s17.catchow;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdminMainActivity extends AppCompatActivity {

    private static final String TAG = "AdminMainActivity";
    private TextView adminNameTextView;
    private CardView usersCardView;
    private CardView ordersCardView;
    private CardView logsCardView;
    private Button logoutButton;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_main);

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize views
        adminNameTextView = findViewById(R.id.admin_name);
        usersCardView = findViewById(R.id.manage_users_card);
        ordersCardView = findViewById(R.id.view_orders_card);
        logsCardView = findViewById(R.id.view_logs_card);
        logoutButton = findViewById(R.id.admin_logout_button);

        // Verify admin status
        verifyAdminAccess();

        // Set up click listeners
        usersCardView.setOnClickListener(v -> startActivity(new Intent(AdminMainActivity.this, AdminActivity.class)));

        ordersCardView.setOnClickListener(v -> {
            // You can create an OrdersAdminActivity later
            // startActivity(new Intent(AdminMainActivity.this, OrdersAdminActivity.class));
        });

        logsCardView.setOnClickListener(v -> {
            // You can create a LogsViewerActivity later
            // startActivity(new Intent(AdminMainActivity.this, LogsViewerActivity.class));
        });

        logoutButton.setOnClickListener(v -> {
            auth.signOut();
            startActivity(new Intent(AdminMainActivity.this, LogInActivity.class));
            finish();
        });
    }

    private void verifyAdminAccess() {
        if (auth.getCurrentUser() == null) {
            // Not logged in, redirect to login
            startActivity(new Intent(AdminMainActivity.this, LogInActivity.class));
            finish();
            return;
        }

        String userEmail = auth.getCurrentUser().getEmail();

        db.collection("users")
                .whereEqualTo("email", userEmail)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot userDoc = queryDocumentSnapshots.getDocuments().get(0);
                        String role = userDoc.getString("role");

                        if ("admin".equals(role)) {
                            // User is an admin, update UI
                            String name = userDoc.getString("name");
                            adminNameTextView.setText("Administrator: " + name);
                        } else {
                            // Not an admin, redirect to regular UI
                            startActivity(new Intent(AdminMainActivity.this, MainActivity.class));
                            finish();
                        }
                    } else {
                        // User not found
                        Log.e(TAG, "User document not found");
                        startActivity(new Intent(AdminMainActivity.this, LogInActivity.class));
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking admin status", e);
                    startActivity(new Intent(AdminMainActivity.this, LogInActivity.class));
                    finish();
                });
    }
}