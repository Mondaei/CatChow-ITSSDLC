package com.mobdeve.s17.catchow;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.ActionCodeSettings;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.SignInMethodQueryResult;
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class LoginWithEmailLinkActivity extends AppCompatActivity {
    private static final String TAG = "EmailLinkAuth";
    FirebaseAuth auth;
    EditText emailInput;
    Button sendLinkButton, backtologin;
    FirebaseFirestore firestore;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_with_email_link);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        emailInput = findViewById(R.id.email_input);
        sendLinkButton = findViewById(R.id.send_link_button);
        backtologin = findViewById(R.id.backtologin);

        // Initialize progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Please wait...");
        progressDialog.setCancelable(false);

        sendLinkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailInput.getText().toString().trim(); // Get email from input field
                sendSignInLinkToEmail(email);
            }
        });

        // Navigate back to Login Activity
        backtologin.setOnClickListener(v -> {
            Intent intent = new Intent(LoginWithEmailLinkActivity.this, LogInActivity.class);
            startActivity(intent);
        });

        // Check for Firebase Dynamic Links first
        checkForDynamicLinks();
    }

    private void checkForDynamicLinks() {
        // Add debug logging
        Log.d(TAG, "Checking for dynamic links...");

        FirebaseDynamicLinks.getInstance()
                .getDynamicLink(getIntent())
                .addOnSuccessListener(this, pendingDynamicLinkData -> {
                    // Get deep link from result (may be null if no link is found)
                    Uri deepLink = null;
                    if (pendingDynamicLinkData != null) {
                        deepLink = pendingDynamicLinkData.getLink();
                        Log.d(TAG, "Dynamic link found: " + (deepLink != null ? deepLink.toString() : "null"));

                        // Handle the email sign-in link if present
                        if (deepLink != null) {
                            String emailLink = deepLink.toString();
                            // Check if this is an email sign-in link
                            if (auth.isSignInWithEmailLink(emailLink)) {
                                handleSignInWithEmailLink(emailLink);
                            } else {
                                Log.d(TAG, "Link is not a valid email sign-in link");
                            }
                        }
                    } else {
                        Log.d(TAG, "No dynamic link found");

                        // Check if intent data contains a direct email link (fallback method)
                        handleEmailLink();
                    }
                })
                .addOnFailureListener(this, e -> {
                    Log.w(TAG, "getDynamicLink:onFailure", e);
                    // Fallback to direct intent method
                    handleEmailLink();
                });
    }

    private void sendSignInLinkToEmail(String email) {
        if (email.isEmpty()) {
            Toast.makeText(this, "Enter your email", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.show();

        // Check if email exists in Firestore before sending the sign-in link
        firestore.collection("users")
                .whereEqualTo("email", email)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        // Email exists in Firestore
                        Log.d("FirestoreCheck", "Email exists in Firestore");

                        // Save email in SharedPreferences for later verification
                        SharedPreferences preferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
                        preferences.edit().putString("user_email", email).apply();

                        // Configure the Action Code Settings for email link
                        ActionCodeSettings actionCodeSettings =
                                ActionCodeSettings.newBuilder()
                                        .setUrl("https://catchow.page.link/login")  // Updated with path component
                                        .setHandleCodeInApp(true)
                                        .setAndroidPackageName(
                                                "com.mobdeve.s17.catchow",
                                                true, /* installIfNotAvailable */
                                                "12"    /* minimumVersion */)
                                        .build();

                        // Send sign-in link
                        auth.sendSignInLinkToEmail(email, actionCodeSettings)
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> sendTask) {
                                        progressDialog.dismiss();

                                        if (sendTask.isSuccessful()) {
                                            Log.d(TAG, "Email sent.");
                                            Toast.makeText(LoginWithEmailLinkActivity.this,
                                                    "Check your email for the sign-in link",
                                                    Toast.LENGTH_LONG).show();
                                        } else {
                                            Log.e(TAG, "Error sending email", sendTask.getException());
                                            Toast.makeText(LoginWithEmailLinkActivity.this,
                                                    "Failed to send email: " + sendTask.getException().getMessage(),
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                    } else {
                        // Email does NOT exist in Firestore
                        progressDialog.dismiss();
                        Log.d("FirestoreCheck", "Email does not exist in Firestore");
                        Toast.makeText(this, "Email not registered", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Log.e("FirestoreCheck", "Error checking email", e);
                    Toast.makeText(this, "Error checking email", Toast.LENGTH_SHORT).show();
                });
    }

    private void handleEmailLink() {
        // Check if the activity was started with an email link directly from intent
        Intent intent = getIntent();
        Uri data = intent.getData();

        if (data != null) {
            Log.d(TAG, "Intent has data URI: " + data.toString());
            String emailLink = data.toString();

            // Check if it's a valid sign-in link
            if (auth.isSignInWithEmailLink(emailLink)) {
                handleSignInWithEmailLink(emailLink);
            } else {
                Log.d(TAG, "Intent URI is not a valid email sign-in link");
            }
        } else {
            Log.d(TAG, "Intent has no data URI");
        }
    }

    private void handleSignInWithEmailLink(String emailLink) {
        Log.d(TAG, "Handling sign-in with email link: " + emailLink);

        // Retrieve email from SharedPreferences
        SharedPreferences preferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String email = preferences.getString("user_email", "");

        if (email.isEmpty()) {
            // If we don't have the email, prompt the user to provide it
            Toast.makeText(this, "Please enter your email to complete sign-in",
                    Toast.LENGTH_LONG).show();

            // Show email input field and hide "Send Link" button
            // Add button to submit email for verification
            emailInput.setVisibility(View.VISIBLE);
            sendLinkButton.setText("Complete Sign-in");
            sendLinkButton.setOnClickListener(v -> {
                String inputEmail = emailInput.getText().toString().trim();
                if (!inputEmail.isEmpty()) {
                    completeSignIn(inputEmail, emailLink);
                } else {
                    Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // We have the email, proceed with sign-in
            completeSignIn(email, emailLink);
        }
    }

    private void completeSignIn(String email, String emailLink) {
        progressDialog.show();

        // The client SDK will parse the code from the link for you.
        auth.signInWithEmailLink(email, emailLink)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        progressDialog.dismiss();

                        if (task.isSuccessful()) {
                            Log.d(TAG, "Successfully signed in with email link!");
                            AuthResult result = task.getResult();

                            // Clear the email from SharedPreferences
                            SharedPreferences preferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
                            preferences.edit().remove("user_email").apply();

                            Toast.makeText(LoginWithEmailLinkActivity.this,
                                    "Sign in successful!", Toast.LENGTH_SHORT).show();

                            // Navigate to main activity or dashboard
                            Intent mainIntent = new Intent(LoginWithEmailLinkActivity.this, MainActivity.class);
                            mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(mainIntent);
                            finish();
                        } else {
                            Log.e(TAG, "Error signing in with email link", task.getException());
                            Toast.makeText(LoginWithEmailLinkActivity.this,
                                    "Error signing in: " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    // Method to link the email credential with existing account
    private void linkWithSignInLink(String email, String emailLink) {
        // Construct the email link credential from the current URL.
        AuthCredential credential =
                EmailAuthProvider.getCredentialWithLink(email, emailLink);

        // Link the credential to the current user.
        auth.getCurrentUser().linkWithCredential(credential)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Successfully linked emailLink credential!");
                            Toast.makeText(LoginWithEmailLinkActivity.this,
                                    "Email linked successfully", Toast.LENGTH_SHORT).show();
                        } else {
                            Log.e(TAG, "Error linking emailLink credential", task.getException());
                            Toast.makeText(LoginWithEmailLinkActivity.this,
                                    "Error linking email", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    // Method to reauthenticate a user with email link
    private void reauthWithLink(String email, String emailLink) {
        // Construct the email link credential from the current URL.
        AuthCredential credential =
                EmailAuthProvider.getCredentialWithLink(email, emailLink);

        // Re-authenticate the user with this credential.
        auth.getCurrentUser().reauthenticateAndRetrieveData(credential)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "User successfully reauthenticated");
                            Toast.makeText(LoginWithEmailLinkActivity.this,
                                    "Reauthentication successful", Toast.LENGTH_SHORT).show();
                        } else {
                            Log.e(TAG, "Error reauthenticating", task.getException());
                            Toast.makeText(LoginWithEmailLinkActivity.this,
                                    "Reauthentication failed", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    // Helper method to check sign-in methods for an email
    private void checkSignInMethods(String email) {
        auth.fetchSignInMethodsForEmail(email)
                .addOnCompleteListener(new OnCompleteListener<SignInMethodQueryResult>() {
                    @Override
                    public void onComplete(@NonNull Task<SignInMethodQueryResult> task) {
                        if (task.isSuccessful()) {
                            SignInMethodQueryResult result = task.getResult();
                            List<String> signInMethods = result.getSignInMethods();
                            if (signInMethods.contains(EmailAuthProvider.EMAIL_PASSWORD_SIGN_IN_METHOD)) {
                                // User can sign in with email/password
                                Toast.makeText(LoginWithEmailLinkActivity.this,
                                        "Email uses password authentication", Toast.LENGTH_SHORT).show();
                            } else if (signInMethods.contains(EmailAuthProvider.EMAIL_LINK_SIGN_IN_METHOD)) {
                                // User can sign in with email/link
                                Toast.makeText(LoginWithEmailLinkActivity.this,
                                        "Email uses link authentication", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(LoginWithEmailLinkActivity.this,
                                        "Email not registered", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Log.e(TAG, "Error getting sign in methods", task.getException());
                            Toast.makeText(LoginWithEmailLinkActivity.this,
                                    "Error checking authentication methods", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}