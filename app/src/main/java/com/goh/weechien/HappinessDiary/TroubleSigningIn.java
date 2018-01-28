package com.goh.weechien.HappinessDiary;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.ui.email.RecoveryEmailSentDialog;
import com.firebase.ui.auth.ui.email.fieldvalidators.EmailFieldValidator;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;

import fr.castorflex.android.circularprogressbar.CircularProgressBar;

// Activity to reset user's password
public class TroubleSigningIn extends AppCompatActivity implements View.OnClickListener {
    private FirebaseAuth mAuth;
    private EditText mEmailEditText;
    private EmailFieldValidator mEmailFieldValidator;
    private TextInputLayout mEmailView;
    private LinearLayout mLoginFormView;
    private TextView mTextView;
    private Button mVerifyButton, mResetButton, mSendButton;
    private CircularProgressBar mProgressView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trouble_signing_in);

        mLoginFormView = findViewById(R.id.my_password_reset_form); // Entire layout
        mProgressView = findViewById(R.id.my_password_reset_progress); // Progress bar
        mTextView = findViewById(R.id.trouble_signing_in_textview); // Description
        mEmailView = findViewById(R.id.my_email_layout); // Email form
        mVerifyButton = findViewById(R.id.resend_email_verification); // Send email verification
        mResetButton = findViewById(R.id.reset_password); // Reset password
        mSendButton = findViewById(R.id.button_send_email); // Send button

        // Set initial text
        mTextView.setText(getString(R.string.please_select_signin_helper));

        // Initial view
        mProgressView.setVisibility(View.GONE);
        mEmailView.setVisibility(View.GONE);
        mSendButton.setVisibility(View.GONE);

        mAuth = FirebaseAuth.getInstance();

        // Email validator
        mEmailFieldValidator = new EmailFieldValidator(mEmailView);
        mEmailEditText = findViewById(R.id.my_email);

        // Get the email from the EmailLogin activity
        String email = getIntent().getStringExtra("PASSWORD_RESET_EMAIL");
        if (email != null) {
            mEmailEditText.setText(email);
        }

        mVerifyButton.setOnClickListener(this);
        mResetButton.setOnClickListener(this);
        mSendButton.setOnClickListener(this);
    }

    @Override
    // Button to send the password reset email
    public void onClick(View view) {
        if (view.getId() == R.id.resend_email_verification) {
            // Set initial text
            mTextView.setText(getString(R.string.resend_verification_email_body));

            mVerifyButton.setVisibility(View.GONE);
            mResetButton.setVisibility(View.GONE);

            mEmailView.setVisibility(View.VISIBLE);
            mSendButton.setVisibility(View.VISIBLE);

        } else if (view.getId() == R.id.reset_password) {
            // Set initial text
            mTextView.setText(getString(R.string.fui_password_recovery_body));

            mVerifyButton.setVisibility(View.GONE);
            mResetButton.setVisibility(View.GONE);

            mEmailView.setVisibility(View.VISIBLE);
            mSendButton.setVisibility(View.VISIBLE);

        } else if (view.getId() == R.id.button_send_email) {
            // Get the title of the respective sign-in assistants
            String emailVer = getString(R.string.resend_verification_email_body);
            String passReset = getString(R.string.fui_password_recovery_body);

            if (mTextView.getText().toString().equals(emailVer)) { // Email verification
                // Validate the email
                if (mEmailFieldValidator.validate(mEmailEditText.getText())) {
                    showProgress(true); // Show progress bar
                    sendVerificationEmail(mEmailEditText.getText().toString()); // Pass the email
                }
            } else if (mTextView.getText().toString().equals(passReset)) { // Password reset
                if (mEmailFieldValidator.validate(mEmailEditText.getText())) { // Validate the email
                    showProgress(true); // Show progress bar
                    sendPwResetEmail(mEmailEditText.getText().toString()); // Pass the email
                }
            }
        }
    }

    // Send the password reset email
    private void sendPwResetEmail(final String email) {
        mAuth.sendPasswordResetEmail(email).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                showProgress(false);
                if (task.isSuccessful()) { // Successful - Make an alert dialog
                    RecoveryEmailSentDialog.show(
                            email, getSupportFragmentManager());
                } else { // Failed - Make a toast
                    Toast.makeText(TroubleSigningIn.this,
                            getString(R.string.password_reset_fail), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    // Send the verification email
    private void sendVerificationEmail(final String email) {
        // Send if the user is signed in
        if (mAuth.getCurrentUser() != null) {
            mAuth.getCurrentUser().sendEmailVerification().addOnCompleteListener(
                    new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            showProgress(false);
                            if (task.isSuccessful()) {
                                Toast.makeText(TroubleSigningIn.this,
                                        getString(R.string.verification_sent) + " "
                                                + mAuth.getCurrentUser().getEmail(),
                                        Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(TroubleSigningIn.this,
                                        getString(R.string.unknown_error), Toast.LENGTH_LONG).show();
                            }
                        }
                    }
            );
        } else {
            Toast.makeText(TroubleSigningIn.this, getString(R.string.please_signin_again),
                    Toast.LENGTH_LONG).show();
            Intent intent = new Intent(TroubleSigningIn.this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
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
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase, "en"));
    }

    @Override
    // Sign the user out and go back to the login activity
    public void onBackPressed() {
        super.onBackPressed();
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}
