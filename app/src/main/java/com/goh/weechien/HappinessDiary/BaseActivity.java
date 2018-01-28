package com.goh.weechien.HappinessDiary;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.CheckedTextView;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;

public abstract class BaseActivity extends AppCompatActivity {
    DrawerLayout drawer;
    private int navId, layoutID = 0;
    FirebaseAuth mAuth;
    FirebaseAuth.AuthStateListener authStateListener;
    private boolean pendingAppBarAnimation, isNavMenuClicked;
    static final int RC_BOOKMARK = 13007; // Key to send and retrieve the bookmarks

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance(); // Get an instance of the firebase auth
        authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                SharedPreferences pref = getSharedPreferences("ANON_SIGN_IN", MODE_PRIVATE);

                if (firebaseAuth.getCurrentUser() == null && !pref.getBoolean("HAS_SIGN_IN", false)) {
                    Intent signOutIntent = new Intent(getActivityInstance(), LoginActivity.class);
                    signOutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(signOutIntent);
                    finish();
                }
            }
        };

        if (savedInstanceState == null) {
            pendingAppBarAnimation = true;
        }
    }

    public void setLayoutID(int layoutID) {
        this.layoutID = layoutID;
    } // Set custom layout

    // Use default layout if there is none
    public int getLayoutID() {
        return layoutID;
    } // Get custom layout

    @Override
    // Setup a generic navigation drawer for each activity
    public void setContentView(@LayoutRes int layoutResID) {
        // Inflate the layout container
        // Use the default layout container if none is given
        if (getLayoutID() == 0) { // Use the default layout container
            drawer = (DrawerLayout) getLayoutInflater().inflate(R.layout.activity_container, null);
        } else { // If a layout is given, use that layout container
            drawer = (DrawerLayout) getLayoutInflater().inflate(getLayoutID(), null);
        }

        // Inflate the respective activity's layout into the drawer's container
        RelativeLayout activityContainer = drawer.findViewById(R.id.content_base);
        getLayoutInflater().inflate(layoutResID, activityContainer, true);
        super.setContentView(drawer);

        // Setup the appbar
        Toolbar toolbar = drawer.findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Setup the toggle button for the navigation drawer
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                LaunchActivity();
            }
        };
        drawer.addDrawerListener(toggle);
        toggle.syncState();
    }

    @Override
    // If the drawer is open, close it, else go back to the previous activity
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
            finish();
        }
    }

    @Override
    // Render the menu layout
    public boolean onCreateOptionsMenu(Menu menu) {
        //Start the app bar animation when the activity is first launched
        if (pendingAppBarAnimation) { // Run if there is no saved instance state
            pendingAppBarAnimation = false;
            startAppBarAnimation(menu); // Run entrance animation then inflate the menu
        } else {
            inflateMenu(menu); // Call this to inflate the menu
        }
        return true;
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

    // Animate the app bar and its items then inflate the menu
    public void startAppBarAnimation(final Menu menu) {
        final int ANIM_DURATION_APPBAR = 300;
        int actionBarHeight = 0;

        final View content = drawer.findViewById(R.id.content_base); // Get the base content
        View appbar = drawer.findViewById(R.id.appbar); // Get the app bar
        final Toolbar toolbar = drawer.findViewById(R.id.toolbar); // Get the toolbar
        // Get the text view located in the toolbar
        TextView toolbarTextView = toolbar.findViewById(R.id.toolbar_base_textview);

        // Define the layout params for the dummy views
        Toolbar.LayoutParams layoutParams = new Toolbar.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.END);
        // Inflate a dummy language image button view and add it to the toolbar
        final ImageButton languageIcon = (ImageButton) getLayoutInflater().inflate(R.layout.custom_language_icon, null);
        toolbar.addView(languageIcon, layoutParams);
        // Inflate a dummy search button view and add it to the toolbar
        final ImageButton searchIcon = (ImageButton) getLayoutInflater().inflate(R.layout.custom_search_icon, null);
        searchIcon.setVisibility(View.INVISIBLE);
        toolbar.addView(searchIcon, layoutParams);

        // Calculate ActionBar height
        TypedValue tv = new TypedValue();
        if (getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data, getResources().getDisplayMetrics());
        }

        // Get the device height
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        // Place the views outside of the screen
        //content.setTranslationY(deviceHeight);
        appbar.setTranslationY(-actionBarHeight);
        toolbar.setTranslationY(-actionBarHeight);
        toolbarTextView.setTranslationY(-actionBarHeight);
        languageIcon.setTranslationY(-actionBarHeight);
        searchIcon.setTranslationY(-actionBarHeight); // The animation is invisible by default

        // Method to run tab layout animation
        // The search icon can be made visible here
        tabLayoutAnimation(searchIcon, drawer, actionBarHeight, ANIM_DURATION_APPBAR);

        // Run the animations
        appbar.animate()
                .translationY(0)
                .setDuration(ANIM_DURATION_APPBAR)
                .setStartDelay(200);
        toolbar.animate()
                .translationY(0)
                .setDuration(ANIM_DURATION_APPBAR)
                .setStartDelay(300);
        toolbarTextView.animate()
                .translationY(0)
                .setDuration(ANIM_DURATION_APPBAR)
                .setStartDelay(400);
        searchIcon.animate()
                .translationY(0)
                .setDuration(ANIM_DURATION_APPBAR)
                .setStartDelay(500);
        languageIcon.animate()
                .translationY(0)
                .setDuration(ANIM_DURATION_APPBAR)
                .setStartDelay(600)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        // At the end of the animation, remove the dummy views, animate the content
                        // and inflate the menu
                        toolbar.removeView(languageIcon);
                        toolbar.removeView(searchIcon);
                        content.animate()
                                .translationY(0)
                                .setInterpolator(new DecelerateInterpolator(3.f))
                                .setDuration(700)
                                .setStartDelay(0)
                                .setListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        doneAppBarAnimation(); // Adjust the images
                                    }
                                });
                        inflateMenu(menu); // Call this to inflate the menu
                    }
                })
                .start();
    }

    public void tabLayoutAnimation(ImageButton searchIcon, DrawerLayout drawer,
                                   int actionBarHeight, int ANIM_DURATION_APPBAR) {
        // Override this method to run tab layout animation
        // Only override if your activity has a tab layout
    }

    // Inflate the menu
    public void inflateMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.base, menu);
    }

    // Called when the app bar animation is complete
    public void doneAppBarAnimation() {
    }

    // This method is called when an item in the navigation drawer is clicked
    public void onNavMenuClick(View view) {
        navId = view.getId(); // Store the ID of the view
        int NAV_CLOSE_DELAY = 200; // Set the delay time
        isNavMenuClicked = true;

        // Reference all the items in the navigation drawer
        CheckedTextView guidance = drawer.findViewById(R.id.guidance);
        CheckedTextView bookmark = drawer.findViewById(R.id.guidance_bookmark_nav);
        CheckedTextView prayer = drawer.findViewById(R.id.prayer);
        CheckedTextView diary = drawer.findViewById(R.id.diary);
        CheckedTextView settings = drawer.findViewById(R.id.settings);
        CheckedTextView about = drawer.findViewById(R.id.about);

        // Mark item as unchecked
        if (guidance.isChecked()) {
            guidance.setChecked(false);
        } else if (bookmark.isChecked()) {
            bookmark.setChecked(false);
        } else if (prayer.isChecked()) {
            prayer.setChecked(false);
        } else if (diary.isChecked()) {
            diary.setChecked(false);
        } else if (settings.isChecked()) {
            settings.setChecked(false);
        } else if (about.isChecked()) {
            about.setChecked(false);
        }

        // When an item is selected, mark it as checked
        if (navId == R.id.guidance) {
            guidance.setChecked(true);
        } else if (navId == R.id.guidance_bookmark_nav) {
            bookmark.setChecked(true);
        } else if (navId == R.id.prayer) {
            prayer.setChecked(true);
        } else if (navId == R.id.diary) {
            diary.setChecked(true);
        } else if (navId == R.id.settings) {
            settings.setChecked(true);
        } else if (navId == R.id.about) {
            about.setChecked(true);
        }

        // Create a handler and  runnable to delay the closing of the navigation drawer
        // which will invoke the callback method - onDrawerClosed
        Runnable r = new Runnable() {
            @Override
            public void run() {
                drawer.closeDrawer(GravityCompat.START);
            }
        };

        Handler handler = new Handler();
        handler.postDelayed(r, NAV_CLOSE_DELAY);
    }

    // Called when the drop down button in the navigation drawer is clicked
    public void onNavDropDownClick(View view) {
        navId = view.getId(); // Store the ID of the view

        FrameLayout frame = drawer.findViewById(R.id.nav_acc_dropdown_container);

        // Set the text of the popup menu when it's first created
        Context context = LocaleHelper.setLocale(this, LocaleHelper.getLanguage(this));
        Resources resources = context.getResources();

        SharedPreferences pref = getSharedPreferences("ANON_SIGN_IN", MODE_PRIVATE);

        PopupMenu popup = new PopupMenu(this, frame);
        if (mAuth.getCurrentUser() != null || pref.getBoolean("HAS_SIGN_IN", false)) {
            // Inflate different menu depending on whether the user is anonymous or not
            if (pref.getBoolean("HAS_SIGN_IN", false)) {
                popup.getMenuInflater().inflate(R.menu.anon_acc_details, popup.getMenu());
                popup.getMenu().findItem(R.id.sign_in_sign_up).setTitle(resources.getString(
                        R.string.sign_in_sign_up));
            } else {
                popup.getMenuInflater().inflate(R.menu.acc_details, popup.getMenu());
                popup.getMenu().findItem(R.id.acc_view).setTitle(resources.getString(
                        R.string.view_account));
                popup.getMenu().findItem(R.id.acc_sign_out).setTitle(resources.getString(
                        R.string.sign_out));
            }
        }

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int id = item.getItemId();

                if (id == R.id.acc_view) {
                    Intent intent = new Intent(getActivityInstance(), UserAccount.class);
                    startActivity(intent);
                    return true;

                } else if (id == R.id.acc_sign_out) {
                    // Remove auth listener
                    mAuth.removeAuthStateListener(authStateListener);

                    // Sign the user out
                    AuthUI.getInstance().signOut((FragmentActivity) getActivityInstance())
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        Intent signOutIntent =
                                                new Intent(getActivityInstance(), LoginActivity.class);
                                        signOutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                        startActivity(signOutIntent);
                                        finish();
                                    } else {
                                        Toast.makeText(getActivityInstance(),
                                                getString(R.string.unknown_error),
                                                Toast.LENGTH_LONG).show();
                                    }
                                }
                            });
                    return true;

                } else if (id == R.id.sign_in_sign_up) {
                    // Remove auth listener
                    mAuth.removeAuthStateListener(authStateListener);

                    // Edit the shared preference to mark that the user has left the main activity
                    SharedPreferences pref = getSharedPreferences("ANON_SIGN_IN", MODE_PRIVATE);
                    pref.edit().putBoolean("HAS_SIGN_IN", false).apply();

                    // Sign the guest out
                    AuthUI.getInstance().signOut((FragmentActivity) getActivityInstance())
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        Intent signOutIntent =
                                                new Intent(getActivityInstance(), LoginActivity.class);
                                        signOutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                        startActivity(signOutIntent);
                                        finish();
                                    } else {
                                        Toast.makeText(getActivityInstance(),
                                                getString(R.string.unknown_error),
                                                Toast.LENGTH_LONG).show();
                                    }
                                }
                            });
                    return true;
                }
                return false;
            }
        });
        popup.show();
    }

    Context getActivityInstance() {
        return this;
    }

    // This method is called to launch a new activity after the navigation drawer is closed
    private void LaunchActivity() {
        // Check if the onNavMenuClick has run
        if (!isNavMenuClicked) {
            return;
        } else {
            isNavMenuClicked = false;
        }

        // Reference all the items in the navigation drawer
        // Items are unchecked here to avoid duplicated checked items when the user navigates back
        CheckedTextView guidance = drawer.findViewById(R.id.guidance);
        CheckedTextView bookmark = drawer.findViewById(R.id.guidance_bookmark_nav);
        CheckedTextView prayer = drawer.findViewById(R.id.prayer);
        CheckedTextView diary = drawer.findViewById(R.id.diary);
        CheckedTextView settings = drawer.findViewById(R.id.settings);
        CheckedTextView about = drawer.findViewById(R.id.about);

        if (navId == R.id.guidance && !this.getClass().getSimpleName().equals("Guidance")) {
            Intent i = new Intent(this, Guidance.class);
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);
            guidance.setChecked(false);

        } else if (navId == R.id.guidance_bookmark_nav && !this.getClass().getSimpleName().equals("Bookmark")) {
            // Refresh the user
            if (mAuth.getCurrentUser() != null) {
                mAuth.getCurrentUser().reload();
            }

            // Proceed to the bookmark activity if user is signed in
            if (mAuth.getCurrentUser() != null) {
                Intent i = new Intent(this, Bookmark.class);
                // Pass the array as an intent to the next activity
                // Only the guidance activity can start the bookmark activity
                i.putExtra("DE_ENG_BOOKMARKS", ((Guidance) this).bookmarkListDEeng);
                i.putExtra("DE_CHI_BOOKMARKS", ((Guidance) this).bookmarkListDEchi);
                i.putExtra("DG_ENG_BOOKMARKS", ((Guidance) this).bookmarkListDGeng);
                i.putExtra("DG_CHI_BOOKMARKS", ((Guidance) this).bookmarkListDGchi);
                i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                startActivityForResult(i, RC_BOOKMARK);
                bookmark.setChecked(false);

                // Exit to the login activity if user is not signed in
            } else {
                Toast.makeText(this, getString(R.string.please_signin_again),
                        Toast.LENGTH_LONG).show();
                Intent i = new Intent(this, LoginActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                finish();
            }


        } else if (navId == R.id.prayer && !this.getClass().getSimpleName().equals("Prayer")) {
            Intent i = new Intent(this, Prayer.class);
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);
            prayer.setChecked(false);

        } else if (navId == R.id.diary & !this.getClass().getSimpleName().equals("Diary")) {
            Intent i = new Intent(this, Diary.class);
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);
            diary.setChecked(false);

        } else if (navId == R.id.settings && !this.getClass().getSimpleName().equals("Settings")) {
            Intent i = new Intent(this, Settings.class);
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);
            settings.setChecked(false);

        } else if (navId == R.id.about && !this.getClass().getSimpleName().equals("About")) {
            Intent i = new Intent(this, About.class);
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);
            about.setChecked(false);
        }
    }
}






