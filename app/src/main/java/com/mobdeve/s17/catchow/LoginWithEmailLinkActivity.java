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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.mobdeve.s17.catchow.databinding.ActivityLogInBinding;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.mobdeve.s17.catchow.models.Users;

public class LoginWithEmailLinkActivity extends AppCompatActivity {
    Button backtologin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_with_email_link);

        // Initialize the button inside onCreate
        backtologin = findViewById(R.id.backtologin);

        // Set click listener correctly
        backtologin.setOnClickListener(v -> {
            Intent intent = new Intent(LoginWithEmailLinkActivity.this, LogInActivity.class);
            startActivity(intent);
        });
    }
}