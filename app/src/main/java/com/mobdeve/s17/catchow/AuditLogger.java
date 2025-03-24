package com.mobdeve.s17.catchow;

import android.os.Build;
import android.util.Log;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AuditLogger {
    private static final String AUDIT_TAG = "CATCHOW_AUDIT";

    public static void log(String userId, String action, String resource, boolean success) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        String status = success ? "SUCCESS" : "FAILURE";

        String logMessage = String.format("USER:%s | ACTION:%s | RESOURCE:%s | STATUS:%s | TIME:%s",
                userId != null ? userId : "UNKNOWN",
                action, resource, status, timestamp);

        // Log to Android's LogCat
        Log.i(AUDIT_TAG, logMessage);

        // Log to Firebase Firestore
        logToFirestore(userId, action, resource, success, timestamp);
    }

    private static void logToFirestore(String userId, String action, String resource, boolean success, String timestamp) {
        if (userId == null || userId.isEmpty()) {
            userId = "UNKNOWN_USER";
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> logEntry = new HashMap<>();
        logEntry.put("userId", userId);
        logEntry.put("action", action);
        logEntry.put("resource", resource);
        logEntry.put("success", success);
        logEntry.put("timestamp", FieldValue.serverTimestamp());
        logEntry.put("deviceInfo", Build.MANUFACTURER + " " + Build.MODEL);

        // This is the line that creates the "audit_logs" collection
        db.collection("audit_logs")
                .add(logEntry)
                .addOnSuccessListener(documentReference ->
                        Log.d(AUDIT_TAG, "Audit log added with ID: " + documentReference.getId()))
                .addOnFailureListener(e ->
                        Log.e(AUDIT_TAG, "Failed to store audit log: " + e.getMessage()));
    }
}