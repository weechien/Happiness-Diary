package com.goh.weechien.HappinessDiary;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.method.PasswordTransformationMethod;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.esafirm.imagepicker.model.Image;
import com.goh.weechien.HappinessDiary.View.CircleRectView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

// Activity to view the user detail
public class UserAccount extends AppCompatActivity {
    private static final int REQUEST_CODE_PICKER = 49482;
    FirebaseAuth mAuth;
    FirebaseAuth.AuthStateListener authStateListener;
    String username, email, password;
    Button dialogOkButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_account);

        // Setup the appbar and toolbar
        Toolbar toolbar = findViewById(R.id.view_account_toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Title of the activity
        TextView title = findViewById(R.id.view_account_toolbar_textview);
        title.setText(getString(R.string.title_activity_view_user_account));

        mAuth = FirebaseAuth.getInstance(); // Get an instance of the firebase auth
        authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                if (firebaseAuth.getCurrentUser() != null) {
                    firebaseAuth.getCurrentUser().reload(); // Refresh the user data
                }
                if (firebaseAuth.getCurrentUser() != null) {
                    for (UserInfo user : firebaseAuth.getCurrentUser().getProviderData()) {
                        // Login is via email/password
                        if (user.getProviderId().equals("password")) {
                            // Show the necessary UI elements
                            findViewById(R.id.change_password_textview).setVisibility(View.VISIBLE);
                            findViewById(R.id.email_textview).setVisibility(View.VISIBLE);
                            findViewById(R.id.edit_name_textview).setVisibility(View.VISIBLE);

                            // Login is via facebook/google
                        } else if (user.getProviderId().equals("facebook.com")
                                || user.getProviderId().equals("google.com")) {
                            // Show the necessary UI elements
                            findViewById(R.id.email_textview).setVisibility(View.VISIBLE);
                            findViewById(R.id.edit_name_textview).setVisibility(View.VISIBLE);
                        }
                    }
                    // Shared preference to get the user's email and username
                    SharedPreferences pref = getSharedPreferences("USER_ACC", MODE_PRIVATE);
                    SharedPreferences.Editor editor;

                    email = firebaseAuth.getCurrentUser().getEmail();
                    username = firebaseAuth.getCurrentUser().getDisplayName();

                    // If there is no email, then the user is signed in anonymously
                    if (email != null) {
                        // Proceed if there are no records or if user email has changed
                        if ((!pref.contains("EMAIL") && !pref.contains("USERNAME"))
                                || !email.equals(pref.getString("EMAIL", null))) {
                            editor = pref.edit();
                            // Save the user's email and username
                            editor.putString("EMAIL", email);
                            editor.putString("USERNAME", username);
                            editor.apply();

                            // Display the username's email and username
                            TextView emailTv = findViewById(R.id.email_textview);
                            emailTv.setText(email);
                            // Set a new username
                            TextView usernameTv = findViewById(R.id.username_textview);
                            usernameTv.setText(username);

                            // There are records and the user's email remains the same
                        } else {
                            TextView emailTv = findViewById(R.id.email_textview);
                            emailTv.setText(email);
                            // Load the username from the shared preference since the user's
                            // email matches the email saved in the shared preference
                            TextView usernameTv = findViewById(R.id.username_textview);
                            usernameTv.setText(pref.getString("USERNAME", null));
                        }
                    } else { // Anonymous sign in
                        TextView usernameTv = findViewById(R.id.username_textview);
                        usernameTv.setText(getString(R.string.anonymous_name));
                    }

                } else {
                    // If the user is not signed in, then exit to the Login activity
                    Toast.makeText(UserAccount.this, getString(
                            R.string.please_signin_again), Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(UserAccount.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                }
            }
        };

        // Profile pic circular image view
        findViewById(R.id.view_account_imageview).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(UserAccount.this, UserAccountProfilePic.class);
                intent.putExtra("CIRCLE_RADIUS", CircleRectView.getCircleRadius());
                ActivityOptionsCompat transitionActivityOptions = ActivityOptionsCompat
                        .makeSceneTransitionAnimation(UserAccount.this, view, getString(R.string.circle));

                // Start the UserAccountProfilePic activity
                ActivityCompat.startActivityForResult(UserAccount.this, intent, REQUEST_CODE_PICKER,
                        transitionActivityOptions.toBundle());
            }
        });

        // Button to edit the user's username
        findViewById(R.id.edit_name_textview).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Layout parameters
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT);

                // Edit text for the new username
                final TextInputEditText input = new TextInputEditText(UserAccount.this);
                input.setLayoutParams(params);
                int padding_in_dp = 16;
                float scale = getResources().getDisplayMetrics().density;
                int padding_in_px = (int) (padding_in_dp * scale + 0.5f);
                input.setPadding(padding_in_px, padding_in_px, padding_in_px, padding_in_px);
                input.setHint(R.string.username_hint);
                input.setSingleLine(true);
                // Enter key in the keyword will trigger the Ok button
                input.setImeOptions(EditorInfo.IME_ACTION_DONE);
                input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                        if (i == EditorInfo.IME_ACTION_DONE) {
                            dialogOkButton.performClick();
                            return true;
                        }
                        return false;
                    }
                });

                // Container for the edit text
                final TextInputLayout layout = new TextInputLayout(UserAccount.this);
                layout.setLayoutParams(params);
                layout.addView(input);

                // Parent layout
                LinearLayout parent = new LinearLayout(UserAccount.this);
                parent.setLayoutParams(params);
                parent.setOrientation(LinearLayout.VERTICAL);
                parent.setPadding(padding_in_px, padding_in_px, padding_in_px, padding_in_px);
                parent.addView(layout);

                // Alert dialog
                final AlertDialog.Builder builder = new AlertDialog.Builder(UserAccount.this);
                builder.setTitle(R.string.change_username);
                builder.setMessage(R.string.enter_new_username);
                builder.setView(parent); // Add the parent layout view
                builder.setIcon(R.drawable.account_box);
                // Set the buttons later to customize the Ok button behavior
                builder.setPositiveButton(R.string.ok_button, null);
                builder.setNegativeButton(R.string.cancel, null);
                final AlertDialog dialog = builder.create();
                dialog.show();
                dialogOkButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);

                // Setup the positive button
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        layout.setError(null);
                        // Make sure the edit text is not empty
                        if (input.getText().toString().trim().equals("")) {
                            layout.setError(getString(R.string.empty_username));
                            input.requestFocus();

                        } else { // Proceed is the edit text is not empty
                            // Save the string into the shared preference
                            SharedPreferences pref = getSharedPreferences("USER_ACC", MODE_PRIVATE);
                            SharedPreferences.Editor editor;

                            editor = pref.edit();
                            editor.putString("USERNAME", input.getText().toString());
                            editor.apply();

                            // Display the new username
                            TextView usernameTv = findViewById(R.id.username_textview);
                            usernameTv.setText(input.getText().toString());

                            // Hide the soft keyboard
                            getWindow().setSoftInputMode(
                                    WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
                            );
                            dialog.dismiss(); // Close the alert dialog
                        }
                    }
                });

                // Setup the negative button
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        layout.setError(null);
                        // Hide the soft keyboard
                        getWindow().setSoftInputMode(
                                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
                        );
                        dialog.dismiss();
                    }
                });

                input.requestFocus();
                // Show the soft keyboard
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
            }
        });

        // Button to change the user's password
        findViewById(R.id.change_password_textview).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Layout parameters
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT);

                // Edit text to verify the existing password
                final TextInputEditText input = new TextInputEditText(UserAccount.this);
                input.setLayoutParams(params);
                int padding_in_dp = 16;
                float scale = getResources().getDisplayMetrics().density;
                int padding_in_px = (int) (padding_in_dp * scale + 0.5f);
                input.setPadding(padding_in_px, padding_in_px, padding_in_px, padding_in_px);
                input.setHint(R.string.prompt_password);
                input.setSingleLine(true);
                input.setImeOptions(EditorInfo.IME_ACTION_DONE);
                input.setTransformationMethod(PasswordTransformationMethod.getInstance());
                // Enter key in the soft keyboard will trigger the Ok button
                input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                        if (i == EditorInfo.IME_ACTION_DONE) {
                            dialogOkButton.performClick();
                            return true;
                        }
                        return false;
                    }
                });

                // Container of the edit text
                final TextInputLayout layout = new TextInputLayout(UserAccount.this);
                layout.setLayoutParams(params);
                layout.setPasswordVisibilityToggleEnabled(true);
                layout.addView(input);

                // Parent layout
                LinearLayout parent = new LinearLayout(UserAccount.this);
                parent.setLayoutParams(params);
                parent.setOrientation(LinearLayout.VERTICAL);
                parent.setPadding(padding_in_px, padding_in_px, padding_in_px, padding_in_px);
                parent.addView(layout);

                // Alert dialog
                final AlertDialog.Builder builder = new AlertDialog.Builder(UserAccount.this);
                builder.setTitle(R.string.enter_password_title);
                builder.setMessage(R.string.enter_password_msg);
                builder.setView(parent); // Set the parent layout view
                builder.setIcon(R.drawable.account_lock);
                builder.setPositiveButton(R.string.ok_button, null);
                builder.setNegativeButton(R.string.cancel, null);
                final AlertDialog dialog = builder.create();
                dialog.show();
                dialogOkButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);

                // Setup the positive button
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        layout.setError(null);
                        // Make sure the edit text is not empty and has at least 6 characters
                        if (input.getText().toString().trim().equals("")
                                || input.getText().toString().length() < 6) {
                            layout.setError(getString(R.string.error_invalid_password));
                            input.requestFocus();

                        } else {
                            // Animate the Ok button by showing a progress bar
                            dialogOkButton.setBackgroundResource(R.drawable.dialog_button_progress_bar);
                            Animation a = AnimationUtils.loadAnimation(UserAccount.this, R.anim.rotate_progress_bar);
                            a.setDuration(1000);
                            dialogOkButton.setText(null);
                            dialogOkButton.startAnimation(a);

                            if (mAuth.getCurrentUser() != null) {
                                mAuth.getCurrentUser().reload(); // Refresh the user data
                            }
                            if (mAuth.getCurrentUser() != null) {
                                String email = mAuth.getCurrentUser().getEmail();
                                assert email != null;
                                // Get auth credentials from the user for re-authentication
                                AuthCredential credential = EmailAuthProvider.getCredential(
                                        email, input.getText().toString());

                                // Check if the credential is valid
                                mAuth.getCurrentUser().reauthenticate(credential)
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                // Stop the animation
                                                dialogOkButton.setText(getString(R.string.ok_button));
                                                dialogOkButton.setBackgroundResource(0);
                                                dialogOkButton.clearAnimation();

                                                // Proceed to the next alert dialog to change password
                                                if (task.isSuccessful()) {
                                                    password = input.getText().toString();
                                                    layout.setError(null);
                                                    passwordChangeDialog();
                                                    // Hide the soft keyboard
                                                    getWindow().setSoftInputMode(
                                                            WindowManager.LayoutParams
                                                                    .SOFT_INPUT_STATE_ALWAYS_HIDDEN
                                                    );
                                                    dialog.dismiss();
                                                } else {
                                                    layout.setError(getString(
                                                            R.string.error_incorrect_password));
                                                    input.requestFocus();
                                                }
                                            }
                                        });

                            } else {
                                // If the user is not signed in, then exit to the Login activity
                                Toast.makeText(UserAccount.this, getString(
                                        R.string.please_signin_again),
                                        Toast.LENGTH_LONG).show();

                                // Hide the soft keyboard
                                getWindow().setSoftInputMode(
                                        WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
                                );
                                dialog.dismiss();
                                Intent intent = new Intent(UserAccount.this, LoginActivity.class);
                                startActivity(intent);
                                finish();
                            }
                        }
                    }
                });

                // Setup the negative button
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        layout.setError(null);
                        // Hide the soft keyboard
                        getWindow().setSoftInputMode(
                                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
                        );
                        dialog.dismiss();
                    }
                });

                input.requestFocus();
                // Show the soft keyboard
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
            }
        });
    }

    // Run this method to change password if the user's existing password is confirmed
    private void passwordChangeDialog() {
        // Layout parameters
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);

        // Place 2 edit texts to confirm new password
        final TextInputEditText input1 = new TextInputEditText(UserAccount.this);
        final TextInputEditText input2 = new TextInputEditText(UserAccount.this);
        input1.setLayoutParams(params);
        input2.setLayoutParams(params);
        int padding_in_dp = 16;
        float scale = getResources().getDisplayMetrics().density;
        int padding_in_px = (int) (padding_in_dp * scale + 0.5f);
        input1.setPadding(padding_in_px, padding_in_px, padding_in_px, padding_in_px);
        input2.setPadding(padding_in_px, padding_in_px, padding_in_px, padding_in_px);
        input1.setHint(R.string.password_hint);
        input2.setHint(R.string.confirm_password);
        input1.setSingleLine(true);
        input2.setSingleLine(true);
        input1.setImeOptions(EditorInfo.IME_ACTION_DONE);
        input2.setImeOptions(EditorInfo.IME_ACTION_DONE);
        input1.setTransformationMethod(PasswordTransformationMethod.getInstance());
        input2.setTransformationMethod(PasswordTransformationMethod.getInstance());
        // Enter key in the keyword will trigger the Ok button
        input1.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i == EditorInfo.IME_ACTION_DONE) {
                    dialogOkButton.performClick();
                    return true;
                }
                return false;
            }
        });
        // Enter key in the keyword will trigger the Ok button
        input2.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i == EditorInfo.IME_ACTION_DONE) {
                    dialogOkButton.performClick();
                    return true;
                }
                return false;
            }
        });

        // Container for the edit text
        final TextInputLayout layout1 = new TextInputLayout(UserAccount.this);
        final TextInputLayout layout2 = new TextInputLayout(UserAccount.this);
        layout1.setLayoutParams(params);
        layout2.setLayoutParams(params);
        layout1.setPasswordVisibilityToggleEnabled(true);
        layout2.setPasswordVisibilityToggleEnabled(true);
        layout1.addView(input1);
        layout2.addView(input2);

        // Parent layout
        LinearLayout parent = new LinearLayout(UserAccount.this);
        parent.setLayoutParams(params);
        parent.setOrientation(LinearLayout.VERTICAL);
        parent.setPadding(padding_in_px, padding_in_px, padding_in_px, padding_in_px);
        parent.addView(layout1);
        parent.addView(layout2);

        // Alert dialog
        final AlertDialog.Builder builder = new AlertDialog.Builder(UserAccount.this);
        builder.setTitle(R.string.change_password);
        builder.setMessage(R.string.enter_new_password);
        builder.setView(parent); // Add the parent layout view
        builder.setIcon(R.drawable.account_lock);
        builder.setPositiveButton(R.string.ok_button, null);
        builder.setNegativeButton(R.string.cancel, null);
        final AlertDialog dialog = builder.create();
        dialog.show();
        dialogOkButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);

        // Setup the positive button
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                layout1.setError(null);
                layout2.setError(null);
                // Make sure the new password is not empty and has at least 6 characters
                if (input1.getText().toString().trim().equals("")
                        || input1.getText().toString().length() < 6) {
                    layout1.setError(getString(R.string.error_invalid_password));
                    input1.requestFocus();

                    // Make sure the new password is not empty and has at least 6 characters
                } else if (input2.getText().toString().trim().equals("")
                        || input2.getText().toString().length() < 6) {
                    layout2.setError(getString(R.string.error_invalid_password));
                    input2.requestFocus();

                    // Make sure the first and second input has the same password
                } else if (!input1.getText().toString().equals(input2.getText().toString())) {
                    layout2.setError(getString(R.string.password_dont_match));
                    input2.requestFocus();

                    // Make sure the old password is not the same as the new password
                } else if (password.equals(input2.getText().toString())) {
                    layout2.setError(getString(R.string.new_and_old_password));
                    input2.requestFocus();

                } else { // Proceed to change user's password
                    // Animate the Ok button by showing a progress bar
                    dialogOkButton.setBackgroundResource(R.drawable.dialog_button_progress_bar);
                    Animation a = AnimationUtils.loadAnimation(UserAccount.this, R.anim.rotate_progress_bar);
                    a.setDuration(1000);
                    dialogOkButton.setText(null);
                    dialogOkButton.startAnimation(a);

                    if (mAuth.getCurrentUser() != null) {
                        mAuth.getCurrentUser().reload(); // Refresh the user data
                    }
                    if (mAuth.getCurrentUser() != null) {
                        // Update the user's password
                        mAuth.getCurrentUser().updatePassword(input2.getText().toString())
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        // Stop the animation
                                        dialogOkButton.setText(getString(R.string.ok_button));
                                        dialogOkButton.setBackgroundResource(0);
                                        dialogOkButton.clearAnimation();

                                        // Show a toast to determine if the password change
                                        // was successful or not
                                        if (task.isSuccessful()) {
                                            Toast.makeText(UserAccount.this, getString(
                                                    R.string.update_password_success),
                                                    Toast.LENGTH_LONG).show();
                                        } else {
                                            Toast.makeText(UserAccount.this, getString(
                                                    R.string.update_password_fail),
                                                    Toast.LENGTH_LONG).show();
                                        }

                                        // Hide the soft keyboard
                                        getWindow().setSoftInputMode(
                                                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
                                        );
                                        dialog.dismiss();
                                    }
                                });
                    } else {
                        // If the user is not signed in, then exit to the Login activity
                        Toast.makeText(UserAccount.this, getString(
                                R.string.unknown_error), Toast.LENGTH_LONG).show();
                        // Hide the soft keyboard
                        getWindow().setSoftInputMode(
                                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
                        );
                        dialog.dismiss();

                        Intent intent = new Intent(UserAccount.this, LoginActivity.class);
                        startActivity(intent);
                        finish();
                    }
                }
            }
        });

        // Setup the negative button
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                layout1.setError(null);
                layout2.setError(null);

                // Hide the soft keyboard
                getWindow().setSoftInputMode(
                        WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
                );
                dialog.dismiss();
            }
        });
        input1.requestFocus();
    }

    @Override
    // Get the result from the UserAccountProfilePic activity
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_PICKER && resultCode == RESULT_OK && data != null) {
            final CircleRectView imageView = findViewById(R.id.view_account_imageview);
            List<Image> images = data.getParcelableArrayListExtra("IMAGES");
            Glide.with(getApplicationContext()).load(images.get(0).getPath()).into(imageView);
        }
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base, "en"));
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

    @Override
    protected void onResume() {
        super.onResume();
        // Run async task to read the stored profile pic path from the database
        new GetProfilePic().execute(this, null, null);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    // Read/write the database and get the image path
    private class GetProfilePic extends AsyncTask<Context, Void, Void> {
        ProfilePicDbHelper dbHelper;
        List<String> imageList;

        @Override
        // Before entering the background thread
        protected void onPreExecute() {
            // Get the database helper
            dbHelper = new ProfilePicDbHelper(getApplicationContext());
        }

        @Override
        // Background thread
        protected Void doInBackground(Context... contexts) {
            // Gets the data repository in write mode
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            String count = "SELECT COUNT(*) FROM " + ProfilePicContract.ProfilePic.TABLE_NAME;
            Cursor cursor = db.rawQuery(count, null);
            cursor.moveToFirst();

            if (cursor.getInt(0) > 0) {
                // Define a projection that specifies which columns from the database
                // you will actually use after this query.
                String[] projection = {
                        ProfilePicContract.ProfilePic._ID,
                        ProfilePicContract.ProfilePic.COLUMN_IMAGE_PATH,
                };

                // Filter results WHERE "id" = '1'
                String selection = ProfilePicContract.ProfilePic._ID + " = ?";
                String[] selectionArgs = {"1"};

                // Cursor to query and fetch data
                Cursor mCursor = db.query(
                        ProfilePicContract.ProfilePic.TABLE_NAME, // The table to query
                        projection,                               // The columns to return
                        selection,                                // The columns for the WHERE clause
                        selectionArgs,                            // The values for the WHERE clause
                        null,                                     // don't group the rows
                        null,                                     // don't filter by row groups
                        null                                      // The sort order
                );

                // Loop through the cursor and read the image path
                while (mCursor.moveToNext()) {
                    imageList = new ArrayList<>();
                    String imagePath = mCursor.getString(
                            mCursor.getColumnIndexOrThrow(ProfilePicContract.ProfilePic.COLUMN_IMAGE_PATH)
                    );
                    imageList.add(imagePath);
                }
                mCursor.close();

            } else { // If there is no image path stored in the database, then save a default
                // image path in the database and display the image
                Uri path = Uri.parse("android.resource://" + getPackageName() + "/"
                        + R.drawable.jumping_girl);
                String imagePath = path.toString();
                imageList = new ArrayList<>();
                imageList.add(imagePath);
                // Create a new map of values, where column names are the keys
                ContentValues values = new ContentValues();
                values.put(ProfilePicContract.ProfilePic.COLUMN_IMAGE_PATH, imagePath);

                // Insert the new row, returning the primary key value of the new row
                db.insert(ProfilePicContract.ProfilePic.TABLE_NAME, null, values);
            }
            cursor.close();
            return null;
        }

        @Override
        // Update the UI
        protected void onPostExecute(Void aVoid) {
            dbHelper.close();
            CircleRectView imageView = findViewById(R.id.view_account_imageview);

            // Check if the image path is valid or not
            File file = new File(imageList.get(0));
            if (file.exists()) {
                // Load the image with Glide
                Glide.with(getApplicationContext()).load(imageList.get(0)).into(imageView);
            } else {
                // If image path does not have any image, use the default image
                Uri path = Uri.parse("android.resource://" + getPackageName() + "/"
                        + R.drawable.jumping_girl);
                String imagePath = path.toString();
                Glide.with(getApplicationContext()).load(imagePath).into(imageView);
            }
        }
    }
}
