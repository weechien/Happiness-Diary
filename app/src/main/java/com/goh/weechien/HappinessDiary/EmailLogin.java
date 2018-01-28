package com.goh.weechien.HappinessDiary;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


public class EmailLogin extends AppCompatActivity implements View.OnClickListener {
    FirebaseAuth mAuth;
    FirebaseAuth.AuthStateListener authStateListener;
    DatabaseReference mDatabase, mDataChild;

    // UI references
    private AutoCompleteTextView mEmailView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_login);

        // Get an instance of firebase database
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance(); // Get an instance of the firebase auth

        // Refresh the user
        if (mAuth.getCurrentUser() != null) {
            mAuth.getCurrentUser().reload();
        }

        if (mAuth.getCurrentUser() != null) { // Proceed if user is signed in
            // Get a reference to the firebase database and check if email verification is sent
            mDataChild = mDatabase.child("users").child(mAuth.getCurrentUser().getUid());
            mDataChild.child("emailVerSent").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (!(boolean) dataSnapshot.getValue()) { // Proceed to send email verification
                        mAuth.getCurrentUser().sendEmailVerification();
                        Toast.makeText(EmailLogin.this, getString(R.string.verification_sent) + " "
                                + mAuth.getCurrentUser().getEmail(), Toast.LENGTH_LONG).show();
                        mDataChild.child("emailVerSent").setValue(true); // Change database value
                    }
                    mDataChild.child("emailVerSent").removeEventListener(this); // Avoid looping
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Toast.makeText(EmailLogin.this,
                            getString(R.string.unknown_error), Toast.LENGTH_LONG).show();
                }
            });
        }

        // Run when the listener is registered, user is signed in/signed out, token is refreshed, etc.
        authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                if (firebaseAuth.getCurrentUser() != null) {
                    firebaseAuth.getCurrentUser().reload(); // Refresh the user data
                }
                if (firebaseAuth.getCurrentUser() != null) { // User is signed in
                    mEmailView.setText(mAuth.getCurrentUser().getEmail()); // Set the email
                    mPasswordView.requestFocus(); // Pass the focus to the password edit text
                    showProgress(false); // Hide the progress bar

                    // Login to the Guidance activity if email is verified
                    if (firebaseAuth.getCurrentUser().isEmailVerified()) {
                        // Start the main activity
                        Intent intent = new Intent(EmailLogin.this, Guidance.class);
                        startActivity(intent);
                        finish();
                    }

                } else {
                    Intent intent = new Intent(EmailLogin.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                }
            }
        };

        // Set up the login form
        mEmailView = findViewById(R.id.verify_login_email); // Email edit text
        mPasswordView = findViewById(R.id.verify_login_password); // Password edit text
        // Enable pressing the keyboard 'enter' for submission
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin(); // Attempt to login
                    return true;
                }
                return false;
            }
        });

        // Attempt to login when the sign in button is clicked
        Button mEmailSignInButton = findViewById(R.id.email_sign_in_button);
        mEmailSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        mLoginFormView = findViewById(R.id.my_login_form); // Entire view layout
        mProgressView = findViewById(R.id.email_login_progress); // Progress bar
        findViewById(R.id.email_trouble_button).setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        final int id = view.getId();
        if (id == R.id.email_trouble_button) {
            Intent intent = new Intent(this, TroubleSigningIn.class);
            intent.putExtra("PASSWORD_RESET_EMAIL", mEmailView.getText().toString());
            startActivity(intent);
            finish();
        }
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase, "en"));
    }

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(authStateListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (authStateListener != null) {
            mAuth.removeAuthStateListener(authStateListener);
        }
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {

        // Reset errors
        mEmailView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt
        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one
        if (TextUtils.isEmpty(password) || !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password), null);
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid email address
        if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.error_field_required), null);
            focusView = mEmailView;
            cancel = true;
        } else if (!isEmailValid(email)) {
            mEmailView.setError(getString(R.string.error_invalid_email), null);
            focusView = mEmailView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and attempt to login
            showProgress(true);
            // Prompt the user to re-provide their sign-in credentials
            if (mAuth.getCurrentUser() != null) {
                // Get auth credentials from the user for re-authentication
                AuthCredential credential = EmailAuthProvider.getCredential(
                        mEmailView.getText().toString(), mPasswordView.getText().toString());

                mAuth.getCurrentUser().reauthenticate(credential).addOnCompleteListener(
                        new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                showProgress(false);
                                if (task.isSuccessful()) {
                                    // Login to the Guidance activity if email is verified
                                    if (mAuth.getCurrentUser().isEmailVerified()) {
                                        // Start the main activity
                                        Intent intent = new Intent(EmailLogin.this, Guidance.class);
                                        startActivity(intent);
                                        finish();
                                    } else {
                                        // Ask the user to verify his/her email
                                        Toast.makeText(EmailLogin.this,
                                                getString(R.string.please_verify_email),
                                                Toast.LENGTH_LONG).show();
                                        mPasswordView.requestFocus();
                                    }
                                } else {
                                    Toast.makeText(EmailLogin.this,
                                            getString(R.string.sign_in_failed),
                                            Toast.LENGTH_LONG).show();
                                    mPasswordView.requestFocus();
                                }
                            }
                        });
            }
        }
    }

    // Logic to verify validity of email
    private boolean isEmailValid(String email) {
        return email.contains("@");
    }

    // Logic to verify validity of password
    private boolean isPasswordValid(String password) {
        return password.length() > 5;
    }


    // Shows the progress UI and hides the login form
    private void showProgress(final boolean show) {
        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

        mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            }
        });

        mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
        mProgressView.animate().setDuration(shortAnimTime).alpha(
                show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });
    }

    @Override
    // Sign the user out and go back to the login activity
    public void onBackPressed() {
        super.onBackPressed();
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(EmailLogin.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}

