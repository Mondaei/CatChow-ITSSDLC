package com.mobdeve.s17.catchow.utils;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class RBACHelper {

    private static final String TAG = "RBACHelper";

    public interface PermissionCallback {
        void onPermissionResult(boolean hasPermission);
    }

    public static void checkUserRole(String requiredRole, PermissionCallback callback) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            callback.onPermissionResult(false);
            return;
        }

        FirebaseFirestore.getInstance().collection("users")
                .whereEqualTo("email", currentUser.getEmail())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        callback.onPermissionResult(false);
                        return;
                    }

                    DocumentSnapshot userDoc = queryDocumentSnapshots.getDocuments().get(0);
                    String userRole = userDoc.getString("role");

                    if (userRole != null && userRole.equals(requiredRole)) {
                        callback.onPermissionResult(true);
                    } else {
                        callback.onPermissionResult(false);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking user role", e);
                    callback.onPermissionResult(false);
                });
    }

    public static void requireAdmin(Context context, Runnable onSuccess) {
        checkUserRole("admin", hasPermission -> {
            if (hasPermission) {
                onSuccess.run();
            } else {
                Toast.makeText(context, "Admin access required", Toast.LENGTH_SHORT).show();
            }
        });
    }
}