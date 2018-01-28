package com.goh.weechien.HappinessDiary;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.RenderScript;
import android.support.v8.renderscript.ScriptIntrinsicBlur;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;

import fr.castorflex.android.circularprogressbar.CircularProgressBar;
import io.branch.indexing.BranchUniversalObject;
import io.branch.referral.Branch;
import io.branch.referral.BranchError;
import io.branch.referral.util.LinkProperties;

// First page to login to the app
public class LoginActivity extends AppCompatActivity {
    FirebaseAuth mAuth;
    FirebaseAuth.AuthStateListener authStateListener;
    private boolean hasAuthRun;
    CircularProgressBar mProgressView;
    static final int RC_SIGN_IN = 5154; // Key to start the sign in activity

    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != 0) {
            // Activity was brought to front and not created,
            // Thus finishing this will get us to the last viewed activity
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        // Progress view
        mProgressView = findViewById(R.id.login_progress);
        mProgressView.bringToFront();

        mAuth = FirebaseAuth.getInstance(); // Get an instance of the firebase auth

        // Run when the listener is registered, user is signed in/signed out, token is refreshed, etc.
        authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull final FirebaseAuth firebaseAuth) {
                if (firebaseAuth.getCurrentUser() != null) {
                    firebaseAuth.getCurrentUser().reload(); // Refresh the user data
                }
                if (firebaseAuth.getCurrentUser() != null) { // User is logged in
                    // Check the login method used
                    for (UserInfo user : firebaseAuth.getCurrentUser().getProviderData()) {
                        // Login via facebook or google
                        if (user.getProviderId().equals("facebook.com")
                                || user.getProviderId().equals("google.com")) {
                            // Prepare to go to the main activity
                            Intent intent = new Intent(LoginActivity.this, Guidance.class);

                            // Hide the buttons and apply a blur effect
                            findViewById(R.id.happiness_button).setVisibility(View.INVISIBLE);
                            findViewById(R.id.anonymous_button).setVisibility(View.INVISIBLE);
                            ImageView image = findViewById(R.id.splash_login);
                            FrameLayout frame = findViewById(R.id.happiness_button_frame_layout);
                            applyBlur(image, frame);
                            frame.bringToFront();
                            mProgressView.bringToFront();

                            // Get the user's bookmarks before entering the guidance activity
                            getBookmark(intent);

                            // Login via email
                        } else if (user.getProviderId().equals("password")) {
                            // Sign out if onCreate is first run and the user is not verified
                            if (!hasAuthRun && !firebaseAuth.getCurrentUser().isEmailVerified()) {
                                firebaseAuth.signOut();
                            }
                            // Proceed if email is verified
                            else if (firebaseAuth.getCurrentUser().isEmailVerified()) {
                                // Prepare to go to main activity
                                Intent intent = new Intent(LoginActivity.this, Guidance.class);

                                // Hide the buttons and apply a blur effect
                                findViewById(R.id.happiness_button).setVisibility(View.INVISIBLE);
                                findViewById(R.id.anonymous_button).setVisibility(View.INVISIBLE);
                                ImageView image = findViewById(R.id.splash_login);
                                FrameLayout frame = findViewById(R.id.happiness_button_frame_layout);
                                applyBlur(image, frame);
                                frame.bringToFront();
                                mProgressView.bringToFront();

                                // Get the user's bookmarks before entering the guidance activity
                                getBookmark(intent);

                                // If email is not verified and this is the second run
                            } else if (hasAuthRun && !firebaseAuth.getCurrentUser().isEmailVerified()) {
                                // Start the email login activity
                                Intent intent = new Intent(LoginActivity.this, EmailLogin.class);
                                startActivity(intent);
                                finish();
                            }
                            break;
                        }
                    }
                } else if (!hasAuthRun) { // Only run this once
                    hasAuthRun = true;

                    // Get the shared preference to check if the user had previously
                    // entered the main activity without signing in
                    SharedPreferences pref = getSharedPreferences("ANON_SIGN_IN", MODE_PRIVATE);
                    if (pref.getBoolean("HAS_SIGN_IN", false)) {
                        // Go to the main activity
                        Intent intent = new Intent(LoginActivity.this, Guidance.class);
                        startActivity(intent);
                        finish();
                    }
                }
            }
        };

        // Start the firebase UI when the sign in button is clicked
        findViewById(R.id.happiness_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startFirebaseUI();
            }
        });

        // Enter without signing in
        findViewById(R.id.anonymous_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Edit the shared preference to mark that the user has entered the main
                // activity without signing in
                SharedPreferences pref = getSharedPreferences("ANON_SIGN_IN", MODE_PRIVATE);
                pref.edit().putBoolean("HAS_SIGN_IN", true).apply();

                // Go to the main activity
                Intent intent = new Intent(LoginActivity.this, Guidance.class);
                startActivity(intent);
                finish();
            }
        });
    }

    // Get the bookmarks saved and launch the guidance activity
    private void getBookmark(final Intent intent) {
        FirebaseDatabase mData = FirebaseDatabase.getInstance();
        DatabaseReference mDataRef = mData.getReference(); // Base reference
        DatabaseReference mDataChild; // Child reference

        // Show the progress view
        mProgressView.setVisibility(View.VISIBLE);

        // Get a reference to the firebase database
        mDataChild = mDataRef.child("users-bookmarks").child(mAuth.getCurrentUser().getUid());
        mDataChild.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    ArrayList<String> bookmarksDEeng = new ArrayList<>();
                    ArrayList<String> bookmarksDEchi = new ArrayList<>();
                    ArrayList<String> bookmarksDGeng = new ArrayList<>();
                    ArrayList<String> bookmarksDGchi = new ArrayList<>();

                    // Save all the bookmarks from the firebase database to an array
                    for (DataSnapshot child : dataSnapshot.getChildren()) {

                        // Daily encouragement
                        if (child.getKey().substring(0, 6).equals("DE Eng")) {
                            bookmarksDEeng.add(child.getKey());
                        } else if (child.getKey().substring(0, 6).equals("DE Chi")) {
                            bookmarksDEchi.add(child.getKey());
                        } else if (child.getKey().substring(0, 6).equals("DG Eng")) {
                            bookmarksDGeng.add(child.getKey());
                        } else if (child.getKey().substring(0, 6).equals("DG Chi")) {
                            bookmarksDGchi.add(child.getKey());
                        }
                    }
                    // Pass the array as an intent to the next activity
                    intent.putExtra("DE_ENG_BOOKMARKS", bookmarksDEeng);
                    intent.putExtra("DE_CHI_BOOKMARKS", bookmarksDEchi);
                    intent.putExtra("DG_ENG_BOOKMARKS", bookmarksDGeng);
                    intent.putExtra("DG_CHI_BOOKMARKS", bookmarksDGchi);
                }
                // Hide the progress view
                mProgressView.setVisibility(View.INVISIBLE);
                startActivity(intent);
                finish();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                mProgressView.setVisibility(View.INVISIBLE);
                Toast.makeText(LoginActivity.this,
                        getString(R.string.unknown_error), Toast.LENGTH_LONG).show();

                // Show the buttons and remove a blur effect
                findViewById(R.id.happiness_button).setVisibility(View.VISIBLE);
                findViewById(R.id.anonymous_button).setVisibility(View.VISIBLE);
                FrameLayout frame = findViewById(R.id.happiness_button_frame_layout);
                frame.setBackground(null);
            }
        });
    }

    // Method to start the firebase UI
    private void startFirebaseUI() {
        startActivityForResult(
                // Get an instance of AuthUI based on the default app
                AuthUI.getInstance().createSignInIntentBuilder()
                        .setTheme(R.style.LoginTheme)
                        .setLogo(R.drawable.logo)
                        .setTosUrl("https://diary-of-happiness.firebaseapp.com/ToS/")
                        .setPrivacyPolicyUrl("https://diary-of-happiness.firebaseapp.com/PrivacyPolicy/")
                        .setAvailableProviders(
                                Arrays.asList(new AuthUI.IdpConfig.Builder(AuthUI.GOOGLE_PROVIDER).build(),
                                        new AuthUI.IdpConfig.Builder(AuthUI.FACEBOOK_PROVIDER).build(),
                                        new AuthUI.IdpConfig.Builder(AuthUI.EMAIL_PROVIDER).build()))
                        .build(), RC_SIGN_IN);
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase, "en"));
    }

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(authStateListener);

        Branch branch = Branch.getInstance();

        branch.initSession(new Branch.BranchUniversalReferralInitListener() {
            @Override
            public void onInitFinished(BranchUniversalObject branchUniversalObject, LinkProperties linkProperties, BranchError error) {
                if (error == null) {
                    // params are the deep linked params associated with the link that the user clicked -> was re-directed to this app
                    // params will be empty if no data found
                    // ... insert custom logic here ...
                } else {
                    Log.i("MyApp", error.getMessage());
                }
            }
        }, this.getIntent().getData(), this);
    }

    @Override
    public void onNewIntent(Intent intent) {
        this.setIntent(intent);
    }


    @Override
    protected void onStop() {
        super.onStop();
        if (authStateListener != null) {
            mAuth.removeAuthStateListener(authStateListener);
        }
    }

    @Override
    // Exit the application
    public void onBackPressed() {
        Intent a = new Intent(Intent.ACTION_MAIN);
        a.addCategory(Intent.CATEGORY_HOME);
        a.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(a);
    }

    private class ApplyBlur extends AsyncTask<Void, Void, Void> {
        Bitmap bitmap, overlay;
        FrameLayout frame;

        ApplyBlur(Bitmap bitmap, FrameLayout frame) {
            this.bitmap = bitmap;
            this.frame = frame;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            overlay = blur(bitmap, frame); // Apply the blur effect
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            // Set the frame background with the blurred image
            frame.setBackground(new BitmapDrawable(getResources(), overlay));
        }
    }

    // Apply a blur effect when the user auto signs in while collecting the bookmark list from the
    // firebase database
    private void applyBlur(final ImageView image, final FrameLayout frame) {
        image.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            // Apply the blur effect just before the frame is drawn
            public boolean onPreDraw() {
                image.getViewTreeObserver().removeOnPreDrawListener(this); // Remove listener
                image.buildDrawingCache(); // Build drawing cache

                Bitmap bmp = image.getDrawingCache(); // Retrieve drawing cache
                new ApplyBlur(bmp, frame).execute(); // Apply the blur effect in a background thread

                return true;
            }
        });
    }

    // Apply the blur effect
    private Bitmap blur(Bitmap imageBit, FrameLayout frame) {
        float radius = 20; // Blur intensity

        // Create an empty bitmap with the frame's layout params
        // This bitmap will be blurred later and set as the frame's background
        Bitmap overlay = Bitmap.createBitmap(frame.getMeasuredWidth(),
                frame.getMeasuredHeight(), Bitmap.Config.ARGB_8888);

        // Create a canvas backed up by this bitmap (overlay)
        Canvas canvas = new Canvas(overlay);

        // Translate canvas to the position of the frame within parent layout
        canvas.translate(-frame.getLeft(), -frame.getTop());

        // Draw part of the image view (imageBit) to the bitmap (overlay)
        canvas.drawBitmap(imageBit, 0, 0, null);

        // Create Renderscript instance
        RenderScript rs = RenderScript.create(this);

        // Copy bitmap (overlay) to Renderscript-friendly piece of data
        Allocation overlayAlloc = Allocation.createFromBitmap(rs, overlay);

        // Create Renderscript blur instance
        ScriptIntrinsicBlur blur = ScriptIntrinsicBlur.create(rs, overlayAlloc.getElement());

        // Set input, radius and apply blur
        blur.setInput(overlayAlloc);
        blur.setRadius(radius);
        blur.forEach(overlayAlloc);

        // Copy result back to bitmap (overlay)
        overlayAlloc.copyTo(overlay);

        rs.destroy();
        return overlay;
    }
}