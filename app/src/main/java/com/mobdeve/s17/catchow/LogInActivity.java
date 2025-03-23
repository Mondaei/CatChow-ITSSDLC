package com.mobdeve.s17.catchow;

import static android.content.ContentValues.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.mobdeve.s17.catchow.databinding.ActivityLogInBinding;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.mobdeve.s17.catchow.models.Users;
import com.mobdeve.s17.catchow.AuditLogger;

public class LogInActivity extends AppCompatActivity {

    GoogleSignInOptions gso;
    GoogleSignInClient gsc;
    FirebaseAuth auth;
    FirebaseFirestore firestore;
    ProgressDialog progressDialog;
    ActivityLogInBinding binding;
    Button google_button;
    Button emailLink_button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLogInBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Logging in...");
        progressDialog.setMessage("Please wait");

        TextView title = findViewById(R.id.name_login);
        Button signUpButton = findViewById(R.id.sign_up_button);
        String text = "CatChow";
        String buttonText = "Don't have an account? Sign Up";

        Spannable spannable1 = new SpannableString(text);
        SpannableString spannable2 = new SpannableString(buttonText);

        ForegroundColorSpan catColorSpan = new ForegroundColorSpan(Color.parseColor("#FFE4B5"));
        spannable1.setSpan(catColorSpan, 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        ForegroundColorSpan chowColorSpan = new ForegroundColorSpan(Color.parseColor("#8B4513"));
        spannable1.setSpan(chowColorSpan, 3, text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        title.setText(spannable1);

        ForegroundColorSpan donthaveAccountColor = new ForegroundColorSpan(Color.parseColor("#8B4513"));
        spannable2.setSpan(donthaveAccountColor, 0, 21, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        ForegroundColorSpan signUpColor = new ForegroundColorSpan(Color.parseColor("#Ef8A07"));
        spannable2.setSpan(signUpColor, 22, buttonText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        signUpButton.setText(spannable2);

        emailLink_button = findViewById(R.id.emailLink_button);

        emailLink_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LogInActivity.this, LoginWithEmailLinkActivity.class);
                startActivity(intent);
            }
        });

        google_button = findViewById(R.id.google_button);
        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build();
        gsc = GoogleSignIn.getClient(this,gso);

        google_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signIn();
            }
        });

        binding.loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = binding.edittextEmail.getText().toString();
                String password = binding.edittextPass.getText().toString();

                if(email.isEmpty()) {
                    binding.edittextEmail.setError("Enter your email");
                } else if(password.isEmpty()) {
                    binding.edittextPass.setError("Enter your pass");
                } else {
                    progressDialog.show();

                    auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if(task.isSuccessful()) {
                                progressDialog.dismiss();
                                AuditLogger.log(email, "LOGIN", "USER_AUTH", true);
                                checkUserRoleAndRedirect(email);
                                startActivity(new Intent(LogInActivity.this, MainActivity.class));
                                finish();
                            } else {
                                progressDialog.dismiss();
                                AuditLogger.log(email, "LOGIN", "USER_AUTH", false);
                                if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                    Toast.makeText(LogInActivity.this, "Invalid username or password", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    });
                }
            }
        });

        binding.signUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LogInActivity.this, SignUpActivity.class);
                startActivity(intent);
            }
        });

        if (auth.getCurrentUser() != null) {
            // User is already authenticated, navigate to MainActivity
            navigateToMainActivity();
        }

    }

    void signIn(){
        Intent signInIntent = gsc.getSignInIntent();
        startActivityForResult(signInIntent, 1000);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 1000) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);

            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                handleGoogleSignInResult(account);
            } catch (ApiException e) {
                Log.e(TAG, "Google sign-in failed. Error code: " + e.getStatusCode());
                Toast.makeText(getApplicationContext(), "Something went wrong", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void checkUserRoleAndRedirect(String email) {
        firestore.collection("users")
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot userDoc = queryDocumentSnapshots.getDocuments().get(0);
                        String role = userDoc.getString("role");

                        if ("admin".equals(role)) {
                            // User is admin, go to admin interface
                            startActivity(new Intent(LogInActivity.this, AdminMainActivity.class));
                        } else {
                            // Regular user, go to main app
                            startActivity(new Intent(LogInActivity.this, MainActivity.class));
                        }
                        finish();
                    } else {
                        // User not found in database
                        Toast.makeText(LogInActivity.this, "User not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(LogInActivity.this, "Error checking user role", Toast.LENGTH_SHORT).show();
                });
    }
    private void handleGoogleSignInResult(GoogleSignInAccount account) {
        String name = account.getDisplayName();
        String email = account.getEmail();

        firestore.collection("users")
                .whereEqualTo("email", email)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            QuerySnapshot querySnapshot = task.getResult();
                            if (querySnapshot != null && !querySnapshot.isEmpty()) {
                                navigateToMainActivity();
                            } else {
                                addUserToDatabase(name, email); // Placeholder for password
                            }
                        } else {
                            Toast.makeText(LogInActivity.this, "Error checking user data.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void addUserToDatabase(String name, String email) {
        Users model = new Users(name, email);

        firestore.collection("users")
                .document(email)
                .set(model)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            navigateToMainActivity();
                        } else {
                            Toast.makeText(LogInActivity.this, "Failed to add user data.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }


    void navigateToMainActivity() {
        finish();
        Intent intent = new Intent(LogInActivity.this, MainActivity.class);
        startActivity(intent);
    }

}