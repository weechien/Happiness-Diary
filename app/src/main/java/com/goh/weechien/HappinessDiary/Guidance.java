package com.goh.weechien.HappinessDiary;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.yayandroid.parallaxrecyclerview.ParallaxViewHolder;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import me.everything.android.ui.overscroll.OverScrollDecoratorHelper;

public class Guidance extends BaseActivity {
    static final int SEARCH_RESULT_ACTIVITY = 555; // Key to send and retrieve search result
    static final int RC_BOOKMARK = 13007; // Key to send and retrieve the bookmarks
    long searchResult = -1;
    SharedPreferences pref;
    GuidanceEncouragementFragment fragEncouragement;
    GuidanceGoshoFragment fragGosho;
    TextView encouragementTabTV, goshoTabTV;
    String deviceLang = "en";
    ViewPagerAdapter viewPagerAdapter;
    ProgressBar leftBarEncouragement, rightBarEncouragement, leftBarGosho, rightBarGosho;
    FirebaseAuth mAuth;
    ArrayList<String> bookmarkListDEeng, bookmarkListDEchi, bookmarkListDGeng, bookmarkListDGchi;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Pass the main activity's context
        Diary.getMainContext(this);
        Prayer.getMainContext(this);

        // Get the array list of bookmarks saved in firebase database
        bookmarkListDEeng = getIntent().getStringArrayListExtra("DE_ENG_BOOKMARKS");
        bookmarkListDEchi = getIntent().getStringArrayListExtra("DE_CHI_BOOKMARKS");
        bookmarkListDGeng = getIntent().getStringArrayListExtra("DG_ENG_BOOKMARKS");
        bookmarkListDGchi = getIntent().getStringArrayListExtra("DG_CHI_BOOKMARKS");

        // Create an instance of the array list if it's null
        if (bookmarkListDEeng == null) {
            bookmarkListDEeng = new ArrayList<>();
        }

        if (bookmarkListDEchi == null) {
            bookmarkListDEchi = new ArrayList<>();
        }

        if (bookmarkListDGeng == null) {
            bookmarkListDGeng = new ArrayList<>();
        }

        if (bookmarkListDGchi == null) {
            bookmarkListDGchi = new ArrayList<>();
        }

        mAuth = FirebaseAuth.getInstance(); // Get a firebase auth instance

        // Set the app title, icon and color when in overview mode (recent apps)
        if (Build.VERSION.SDK_INT >= 26) {
            Bitmap bitmap = AppIconHelperV26.getAppIcon(getPackageManager(),
                    "com.goh.weechien.HappinessDiary");

            // Set the title, icon and color
            ActivityManager.TaskDescription taskDescription = new ActivityManager.TaskDescription(
                    getString(R.string.app_name), bitmap,
                    ContextCompat.getColor(this, R.color.colorPrimary_Guidance));
            setTaskDescription(taskDescription);

        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                && Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            try {
                // Get the app icon
                Drawable icon = getPackageManager().getApplicationIcon("com.goh.weechien.HappinessDiary");
                // Set the title, icon and color
                ActivityManager.TaskDescription taskDescription = new ActivityManager.TaskDescription(
                        getString(R.string.app_name), ((BitmapDrawable) icon).getBitmap(),
                        ContextCompat.getColor(this, R.color.colorPrimary_Guidance));
                setTaskDescription(taskDescription);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }

        // Run if the app is started for the first time
        pref = getSharedPreferences("com.goh.weechien.HappinessDiary.Guidance", 0);
        if (!pref.getBoolean("INITIALIZED", false)) {
            pref.edit().putBoolean("INITIALIZED", true).apply();

            // Set the guidance notification alarm when the app is launched for the first time
            SettingsGuidanceNoti.setAlarm(this, 414179);
        }

        // If 0 is set, use the default container
        setLayoutID(R.layout.activity_container_tab); // Setup the container layout
        setContentView(R.layout.activity_guidance); // Setup the child layout for the container

        // Add the ViewPager and attach a new adapter
        final ViewPager viewPager = findViewById(R.id.guidance_viewpager);
        viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager(), Guidance.this);
        viewPager.setAdapter(viewPagerAdapter);

        // Create a parallax effect when sliding horizontally
        viewPager.setPageTransformer(false, new ParallaxPagerTransformer(R.id.image_frame));

        // Add the TabLayout, link the TabLayout with the ViewPager and set a click listener
        final TabLayout tabLayout = findViewById(R.id.guidance_tablayout);
        tabLayout.setupWithViewPager(viewPager);

        // Iterate over all tabs and set the custom view
        for (int i = 0; i < tabLayout.getTabCount(); i++) {
            TabLayout.Tab tab = tabLayout.getTabAt(i);
            assert tab != null;
            // Setup the text view and progress bar
            tab.setCustomView(viewPagerAdapter.getTabView(i));
        }

        // Iterate over all tabs and set a touch listener
        LinearLayout tabStrip = (LinearLayout) tabLayout.getChildAt(0);
        for (int i = 0; i < tabStrip.getChildCount(); i++) {
            final int finalI = i;
            // Listen to touch event and update the progress bar
            // Setup the listener for all tabs and pass the correct progress bar
            if (i == 0) {
                tabStrip.getChildAt(i).setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        if (event.getAction() == MotionEvent.ACTION_DOWN &&
                                leftBarEncouragement.getVisibility() == View.INVISIBLE &&
                                rightBarEncouragement.getVisibility() == View.INVISIBLE) {
                            if (tabLayout.getSelectedTabPosition() == finalI) {
                                // Show the progress bars and run it
                                leftBarEncouragement.setVisibility(View.VISIBLE);
                                rightBarEncouragement.setVisibility(View.VISIBLE);
                                runProgressBar(event, leftBarEncouragement, rightBarEncouragement);
                            }
                        }
                        return false;
                    }
                });
            } else if (i == 1) {
                tabStrip.getChildAt(i).setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        if (event.getAction() == MotionEvent.ACTION_DOWN &&
                                leftBarGosho.getVisibility() == View.INVISIBLE &&
                                rightBarGosho.getVisibility() == View.INVISIBLE) {
                            if (tabLayout.getSelectedTabPosition() == finalI) {
                                // Show the progress bars and run it
                                leftBarGosho.setVisibility(View.VISIBLE);
                                rightBarGosho.setVisibility(View.VISIBLE);
                                runProgressBar(event, leftBarGosho, rightBarGosho);
                            }
                        }
                        return false;
                    }
                });
            }
        }

        // Change the display whenever the tab changes
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
            }
        });
        // Enable the view pager to over scroll
        OverScrollDecoratorHelper.setUpOverScroll(viewPager);
    }

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        super.setContentView(layoutResID);

        // Refresh the user
        if (mAuth.getCurrentUser() != null) {
            mAuth.getCurrentUser().reload();
        }

        // Show the bookmark navigation only when the user is signed in
        if (mAuth.getCurrentUser() != null) {
            findViewById(R.id.guidance_bookmark_nav).setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Get the default language and return a context reference
        Context context = LocaleHelper.setLocale(this, LocaleHelper.getLanguage(this));
        Resources resources = context.getResources();

        // Set the title of the activity
        TextView textView = findViewById(R.id.toolbar_base_textview);
        textView.setText(resources.getString(R.string.title_activity_guidance));

        // Set the text view in the navigation drawer as checked
        CheckedTextView checkedTextView = findViewById(R.id.guidance);
        checkedTextView.setChecked(true);
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base, deviceLang));
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.getExtras() != null) {
            int dayOfYear = intent.getExtras().getInt("EncouragementToday");

            // Scroll to the date position when the notification is clicked
            // Set the position to the day of the year
            setSearchResult(dayOfYear - 1);
            fragEncouragement.scrollToCardEncouragement(); // Scroll to the position
        }
    }

    @Override
    // Called when an item on the appbar is selected
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        // Set a default language
        if (id == R.id.action_english && !LocaleHelper.getLanguage(this).equals("en")) {
            setGuidanceLocale("en");

        } else if (id == R.id.action_chinese && !LocaleHelper.getLanguage(this).equals("zh")) {
            setGuidanceLocale("zh");
        }
        return super.onOptionsItemSelected(item);
    }

    // Get the new language and reapply to the current activity's views
    void setGuidanceLocale(String language) {
        Context context = LocaleHelper.setLocale(this, language);
        Resources resources = context.getResources();

        // Set the app title, icon and color when in overview mode (recent apps)
        if (Build.VERSION.SDK_INT >= 26) {
            Bitmap bitmap = AppIconHelperV26.getAppIcon(getPackageManager(),
                    "com.goh.weechien.HappinessDiary");

            // Set the title, icon and color
            ActivityManager.TaskDescription taskDescription = new ActivityManager.TaskDescription(
                    getString(R.string.app_name), bitmap,
                    ContextCompat.getColor(this, R.color.colorPrimary_Guidance));
            setTaskDescription(taskDescription);

        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                && Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            try {
                // Get the app icon
                Drawable icon = getPackageManager().getApplicationIcon("com.goh.weechien.HappinessDiary");
                // Set the title, icon and color
                ActivityManager.TaskDescription taskDescription = new ActivityManager.TaskDescription(
                        getString(R.string.app_name), ((BitmapDrawable) icon).getBitmap(),
                        ContextCompat.getColor(this, R.color.colorPrimary_Guidance));
                setTaskDescription(taskDescription);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }

        // Add the new encouragement string array
        String[] myDate = resources.getStringArray(R.array.daily_encouragement_and_gosho_date);
        String[] encouragementContent = resources.getStringArray(R.array.daily_encouragement_content);
        String encouragementSource = resources.getString(R.string.daisaku_ikeda);
        // Reassign the new string array to the class
        fragEncouragement.arrangeDays.init(myDate, encouragementContent, encouragementSource);
        fragEncouragement.resetExpandedPos(); // Reset all expanded card views to its original size
        fragEncouragement.refreshRecyclerView(); // Reset the data in the recycler view

        // Add the new gosho string array
        String[] goshoContent = resources.getStringArray(R.array.daily_gosho_content);
        String[] goshoSource = resources.getStringArray(R.array.daily_gosho_source);
        // Reassign the new string array to the class
        fragGosho.arrangeDays.init(myDate, goshoContent, goshoSource);
        fragGosho.resetExpandedPos(); // Reset all expanded card views to its original size
        fragGosho.refreshRecyclerView(); // Reset the data in the recycler view

        // Set the tabs' text
        encouragementTabTV.setText(resources.getString(R.string.daily_encouragement));
        goshoTabTV.setText(resources.getString(R.string.daily_gosho));

        // Set the activity's title
        TextView activityTitle = findViewById(R.id.toolbar_base_textview);
        activityTitle.setText(resources.getString(R.string.title_activity_guidance));

        // Set the navigation drawer's texts
        CheckedTextView guidanceCTV = findViewById(R.id.guidance);
        guidanceCTV.setText(resources.getString(R.string.title_activity_guidance));
        CheckedTextView bookmarkCTV = findViewById(R.id.guidance_bookmark_nav);
        bookmarkCTV.setText(resources.getString(R.string.guidance_bookmark));
        CheckedTextView prayerCTV = findViewById(R.id.prayer);
        prayerCTV.setText(resources.getString(R.string.title_activity_prayer));
        CheckedTextView diaryCTV = findViewById(R.id.diary);
        diaryCTV.setText(resources.getString(R.string.title_activity_diary));
        CheckedTextView settingsCTV = findViewById(R.id.settings);
        settingsCTV.setText(resources.getString(R.string.title_activity_settings));
        CheckedTextView aboutCTV = findViewById(R.id.about);
        aboutCTV.setText(resources.getString(R.string.title_activity_about));

        // Set the encouragement menu items' texts
        for (int i = 0; i < fragEncouragement.getPopupMenu().size(); i++) {
            MenuItem guidanceShare =
                    fragEncouragement.getPopupMenu().get(i).getMenu().findItem(R.id.guidance_share);
            guidanceShare.setTitle(resources.getString(R.string.guidance_share));

            MenuItem guidanceShareImage =
                    fragEncouragement.getPopupMenu().get(i).getMenu().findItem(R.id.guidance_share_image);
            guidanceShareImage.setTitle(resources.getString(R.string.guidance_share_image));

            MenuItem guidanceShareText =
                    fragEncouragement.getPopupMenu().get(i).getMenu().findItem(R.id.guidance_share_text);
            guidanceShareText.setTitle(resources.getString(R.string.guidance_share_text));

            MenuItem guidanceCopy =
                    fragEncouragement.getPopupMenu().get(i).getMenu().findItem(R.id.guidance_copy);
            guidanceCopy.setTitle(resources.getString(R.string.guidance_copy));

            MenuItem guidanceBookmark =
                    fragEncouragement.getPopupMenu().get(i).getMenu().findItem(R.id.guidance_bookmark);

            // Check for null as guest login do not have bookmarks
            if (guidanceBookmark != null) {
                guidanceBookmark.setTitle(resources.getString(R.string.guidance_bookmark));
            }
        }

        // Set the gosho menu items' texts
        for (int i = 0; i < fragGosho.getPopupMenu().size(); i++) {
            MenuItem guidanceShare =
                    fragGosho.getPopupMenu().get(i).getMenu().findItem(R.id.guidance_share);
            guidanceShare.setTitle(resources.getString(R.string.guidance_share));

            MenuItem guidanceShareImage =
                    fragGosho.getPopupMenu().get(i).getMenu().findItem(R.id.guidance_share_image);
            guidanceShareImage.setTitle(resources.getString(R.string.guidance_share_image));

            MenuItem guidanceShareText =
                    fragGosho.getPopupMenu().get(i).getMenu().findItem(R.id.guidance_share_text);
            guidanceShareText.setTitle(resources.getString(R.string.guidance_share_text));

            MenuItem guidanceCopy =
                    fragGosho.getPopupMenu().get(i).getMenu().findItem(R.id.guidance_copy);
            guidanceCopy.setTitle(resources.getString(R.string.guidance_copy));

            MenuItem guidanceBookmark =
                    fragGosho.getPopupMenu().get(i).getMenu().findItem(R.id.guidance_bookmark);

            // Check for null as guest login do not have bookmarks
            if (guidanceBookmark != null) {
                guidanceBookmark.setTitle(resources.getString(R.string.guidance_bookmark));
            }
        }
    }

    // Enum to get the strings easily
    public enum DGuidance {
        DE("Daily Encouragement"),
        DG("Daily Gosho");

        private final String text;

        DGuidance(String text) {
            this.text = text;
        }

        @Override
        public String toString() {
            return text;
        }
    }

    // Get a reference to the storage path of firebase
    static StorageReference getFirebaseStorageRef(
            Context context, int position, DGuidance guidance, boolean ignoreLeap) {
        StorageReference pathRef = null;
        String lang, firePath = null;
        int leap;

        // Create a storage reference from firebase
        StorageReference storageRef = FirebaseStorage.getInstance().getReference();

        // Set the firebase download path based on the device's density
        DisplayMetrics metrics = new DisplayMetrics();
        ((Activity) context).getWindowManager().getDefaultDisplay().getMetrics(metrics);

        if (metrics.densityDpi <= DisplayMetrics.DENSITY_LOW) {
            firePath = guidance.toString() + "/ldpi";
        } else if (metrics.densityDpi > DisplayMetrics.DENSITY_LOW &&
                metrics.densityDpi <= DisplayMetrics.DENSITY_MEDIUM) {
            firePath = guidance.toString() + "/mdpi";
        } else if (metrics.densityDpi > DisplayMetrics.DENSITY_MEDIUM &&
                metrics.densityDpi <= DisplayMetrics.DENSITY_HIGH) {
            firePath = guidance.toString() + "/hdpi";
        } else if (metrics.densityDpi > DisplayMetrics.DENSITY_HIGH) {
            firePath = guidance.toString() + "/xhdpi";
        }

        // Get the language to determine the firebase database to load
        if (guidance == DGuidance.DG) {
            lang = "";
        } else {
            if (LocaleHelper.getLanguage(context).equals("en")) { // English
                lang = "/Eng";
            } else { // Else chinese
                lang = "/Chi";
            }
        }

        // Get the max no. of days in this year
        Calendar calendar = Calendar.getInstance();
        int calendarMaxDays = calendar.getActualMaximum(Calendar.DAY_OF_YEAR);

        // Determine whether to add an extra day for a leap year
        if (ignoreLeap) {
            leap = 0;
        } else {
            if (calendarMaxDays == 365 && position >= 59) {
                leap = 1; // Not leap year
            } else {
                leap = 0; // Leap year
            }
        }

        if (firePath == null) {
            return null;
        }

        // Link the image from firebase
        if (0 <= position && position <= 30) { // Jan
            pathRef = storageRef.child(firePath + lang + "/Jan/" + position + ".webp");

        } else if (31 <= position && position <= (59 - leap)) { // Feb
            pathRef = storageRef.child(firePath + lang + "/Feb/" + (position - 31) + ".webp");

        } else if ((60 - leap) <= (position) && (position) <= (90 - leap)) { // Mar
            pathRef = storageRef.child(firePath + lang + "/Mar/" + (position - (60 - leap)) + ".webp");

        } else if ((91 - leap) <= (position) && (position) <= (120 - leap)) { // Apr
            pathRef = storageRef.child(firePath + lang + "/Apr/" + (position - (91 - leap)) + ".webp");

        } else if ((121 - leap) <= (position) && (position) <= (151 - leap)) { // May
            pathRef = storageRef.child(firePath + lang + "/May/" + (position - (121 - leap)) + ".webp");

        } else if ((152 - leap) <= (position) && (position) <= (181 - leap)) { // Jun
            pathRef = storageRef.child(firePath + lang + "/Jun/" + (position - (152 - leap)) + ".webp");

        } else if ((182 - leap) <= (position) && (position) <= (212 - leap)) { // Jul
            pathRef = storageRef.child(firePath + lang + "/Jul/" + (position - (182 - leap)) + ".webp");

        } else if ((213 - leap) <= (position) && (position) <= (243 - leap)) { // Aug
            pathRef = storageRef.child(firePath + lang + "/Aug/" + (position - (213 - leap)) + ".webp");

        } else if ((244 - leap) <= (position) && (position) <= (273 - leap)) { // Sep
            pathRef = storageRef.child(firePath + lang + "/Sep/" + (position - (244 - leap)) + ".webp");

        } else if ((274 - leap) <= (position) && (position) <= (304 - leap)) { // Oct
            pathRef = storageRef.child(firePath + lang + "/Oct/" + (position - (274 - leap)) + ".webp");

        } else if ((305 - leap) <= (position) && (position) <= (334 - leap)) { // Nov
            pathRef = storageRef.child(firePath + lang + "/Nov/" + (position - (305 - leap)) + ".webp");

        } else if ((335 - leap) <= (position) && (position) <= (365 - leap)) { // Dec
            pathRef = storageRef.child(firePath + lang + "/Dec/" + (position - (335 - leap)) + ".webp");
        }
        return pathRef;
    }

    static String getSunriseEmoji() {
        // Compare time and show a different emoji
        // Set the morning time
        Calendar morning = Calendar.getInstance();
        morning.set(Calendar.HOUR, 6);
        morning.set(Calendar.MINUTE, 0);
        morning.set(Calendar.AM_PM, Calendar.AM);

        // Set the evening time
        Calendar evening = Calendar.getInstance();
        evening.set(Calendar.HOUR, 6);
        evening.set(Calendar.MINUTE, 0);
        evening.set(Calendar.AM_PM, Calendar.PM);

        // Get the time now
        Calendar now = Calendar.getInstance();

        // Emoji variable
        String emoji;

        // Proceed if the time now is between the morning and evening
        if (now.after(morning) && now.before(evening)) { // Morning
            return emoji = new String(Character.toChars(0x1F305));

            // Else proceed if the time now is between evening and morning
        } else { // Evening
            return emoji = new String(Character.toChars(0x1F304));
        }
    }

    // Update the progress bar when touched
    private void runProgressBar(final MotionEvent event, final ProgressBar leftBar, final ProgressBar rightBar) {
        final int progressTime = 1000; // Time for the progress bar to complete
        final int intervalTime = progressTime / 100; // Progress bar update interval

        // Update the progress bar
        TimerTask task = new TimerTask() {
            // Cumulative time is placed outside of Run because Run is called at each interval
            int cumuTime = 0;

            @Override
            public void run() {
                cumuTime += intervalTime;
                // Update the progress bar from 100 to 0
                rightBar.setProgress((progressTime / intervalTime) - (cumuTime * 100 / progressTime));
                leftBar.setProgress((progressTime / intervalTime) - (cumuTime * 100 / progressTime));

                // Stop the time when the progress bar is complete and no touch is registered
                // Reset the progress bar
                if (cumuTime >= progressTime) {
                    rightBar.setProgress(0);
                    leftBar.setProgress(0);

                    // Update the UI by making the progress bar invisible
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            leftBar.setVisibility(View.INVISIBLE);
                            rightBar.setVisibility(View.INVISIBLE);
                        }
                    });
                    this.cancel();

                    // Get the current day of the year
                    Calendar calendar = Calendar.getInstance();
                    int dayOfYear = calendar.get(Calendar.DAY_OF_YEAR);

                    TabLayout tabLayout = findViewById(R.id.guidance_tablayout);
                    int tabPosition = tabLayout.getSelectedTabPosition(); // Get current tab
                    switch (tabPosition) {
                        case 0:
                            setSearchResult(dayOfYear - 1); // Set the position to the day of the year
                            fragEncouragement.scrollToCardEncouragement(); // Scroll to the position
                        case 1:
                            setSearchResult(dayOfYear - 1);
                            fragGosho.scrollToCardGosho();
                    }

                    // Cancel if the screen is not touched
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    rightBar.setProgress(0);
                    leftBar.setProgress(0);

                    // Update the UI by making the progress bar invisible
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            leftBar.setVisibility(View.INVISIBLE);
                            rightBar.setVisibility(View.INVISIBLE);
                        }
                    });
                    this.cancel();
                }
            }
        };
        // Instantiate to update the progress bar
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(task, 500, intervalTime);

    }

    @Override
    // Only override if your activity has a tab layout
    // No tab animation will be run if this method is not overridden
    public void tabLayoutAnimation(ImageButton searchIcon, DrawerLayout drawer,
                                   int actionBarHeight, int ANIM_DURATION_APPBAR) {
        TabLayout tabLayout = drawer.findViewById(R.id.guidance_tablayout);
        tabLayout.setTranslationY(-actionBarHeight);
        tabLayout.animate()
                .translationY(0)
                .setDuration(ANIM_DURATION_APPBAR)
                .setStartDelay(100);
        searchIcon.setVisibility(View.VISIBLE); // Make the icon visible if it's in this activity

    }

    @Override
    // Called when the app bar animation is complete
    // Use this method to adjust the images' matrix
    public void doneAppBarAnimation() {
        if (!fragEncouragement.getViewHolder().isEmpty()) {
            for (HashMap.Entry<String, RecyclerView.ViewHolder> hash : fragEncouragement.getViewHolder().entrySet()) {
                // Notify ParallaxImageView that it will be displayed, so it will re-center itself
                ((ParallaxViewHolder) hash.getValue()).getBackgroundImage().doTranslate();
            }
        }

        if (!fragGosho.getViewHolder().isEmpty()) {
            for (HashMap.Entry<String, RecyclerView.ViewHolder> hash : fragGosho.getViewHolder().entrySet()) {
                // Notify ParallaxImageView that it will be displayed, so it will re-center itself
                ((ParallaxViewHolder) hash.getValue()).getBackgroundImage().doTranslate();
            }
        }
    }

    @Override
    // Inflate the menu and set a click listener for the search function
    public void inflateMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.base_guidance, menu); // Menu with search icon
        MenuItem searchView = menu.findItem(R.id.search);
        // Launch the search result activity when the search icon is clicked
        searchView.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                // Get the selected tab position so the search result activity will know which
                // guidance it should load
                TabLayout tabLayout = findViewById(R.id.guidance_tablayout);
                int tabPosition = tabLayout.getSelectedTabPosition();

                Context context = LocaleHelper.setLocale(Guidance.this, LocaleHelper.getLanguage(Guidance.this));
                Resources resources = context.getResources();

                // Decide the text of the search hint based on the current tab position
                String searchHint = (tabPosition == 0) ?
                        resources.getString(R.string.search_encouragement)
                        : resources.getString(R.string.search_gosho);

                Intent i = new Intent(Guidance.this, SearchResultActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                i.putExtra("TAB_POSITION", tabPosition);
                i.putExtra("SEARCH_HINT", searchHint);
                overridePendingTransition(0, 0); // No transition animation
                startActivityForResult(i, SEARCH_RESULT_ACTIVITY); // Key as a unique identifier
                return true;
            }
        });
    }

    @Override
    // If the drawer is open, close it, else exit the application
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            Intent a = new Intent(Intent.ACTION_MAIN);
            a.addCategory(Intent.CATEGORY_HOME);
            a.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(a);
            finish();
        }
    }

    @Override
    // This method will run when the search result activity ends and return a result of the search
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case (SEARCH_RESULT_ACTIVITY): { // Match the key
                if (resultCode == Activity.RESULT_OK) { // If the search result activity ends normally
                    searchResult = data.getLongExtra("SEARCH_RESULT", -1); // Get the ID

                    // Determine the tab position and scroll the card to the search result
                    TabLayout tabLayout = findViewById(R.id.guidance_tablayout);
                    int tabPosition = tabLayout.getSelectedTabPosition();

                    if (tabPosition == 0) {
                        fragEncouragement.scrollToCardEncouragement();
                    } else if (tabPosition == 1) {
                        fragGosho.scrollToCardGosho();
                    }
                }
                break;

            }
            case (RC_BOOKMARK): {
                if (resultCode == Activity.RESULT_OK) {
                    // Pass the bookmark array list from the bookmark activity
                    bookmarkListDEeng = data.getStringArrayListExtra("DE_ENG_BOOKMARKS");
                    bookmarkListDEchi = data.getStringArrayListExtra("DE_CHI_BOOKMARKS");
                    bookmarkListDGeng = data.getStringArrayListExtra("DG_ENG_BOOKMARKS");
                    bookmarkListDGchi = data.getStringArrayListExtra("DG_CHI_BOOKMARKS");
                }
            }
        }
    }

    public long getSearchResult() {
        return searchResult;
    }

    public void setSearchResult(long searchResult) {
        this.searchResult = searchResult;
    }

    // View Pager Adapter to launch the fragments and name the tab layout
    private class ViewPagerAdapter extends FragmentPagerAdapter {
        private String fragments[] = {getString(R.string.daily_encouragement),
                getString(R.string.daily_gosho)};
        private Context context;

        private ViewPagerAdapter(FragmentManager fm, Context context) {
            super(fm);
            this.context = context;
        }

        @Override
        public int getCount() {
            return fragments.length;
        }

        @Override
        // Launch a fragment based on the tab position
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return new GuidanceEncouragementFragment();
                case 1:
                    return new GuidanceGoshoFragment();
                default:
                    return null;
            }
        }

        @Override
        // Get the fragments references which would be used to scroll to a specified card position
        public Object instantiateItem(ViewGroup container, int position) {
            Fragment frag = (Fragment) super.instantiateItem(container, position);

            // save the appropriate reference depending on position
            switch (position) {
                case 0:
                    fragEncouragement = (GuidanceEncouragementFragment) frag;
                    break;
                case 1:
                    fragGosho = (GuidanceGoshoFragment) frag;
                    break;
            }
            return frag;
        }

        @Override
        // Set the title for the tab
        public CharSequence getPageTitle(int position) {
            return fragments[position];
        }

        @Override
        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }

        // Setup the text view and progress bar of the tabs in the tab layout
        View getTabView(int position) {
            // Assign a custom text view for the tab layout's title
            View v = LayoutInflater.from(context).inflate(R.layout.custom_textview, null);
            TextView tv = v.findViewById(R.id.tablayout_tabItem_textView);
            tv.setText(fragments[position]); // Set the title based on the tab position
            tv.bringToFront(); // Place the text view in front of the progress bar

            // Get the height of the custom text view
            tv.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            int tvHeight = tv.getMeasuredHeight();

            // Get the progress bar and set the height to be the same as the text view's height
            // There are 2 progress bars in each tab to create an animation effect
            if (position == 0) {
                encouragementTabTV = tv;
                // Left
                leftBarEncouragement = v.findViewById(R.id.tablayout_tabItem_leftbar);
                leftBarEncouragement.setScaleY(tvHeight); // Set the height to match the text view
                leftBarEncouragement.setRotation(180); // Reverse the loading direction
                // Set the color to match the background and loading color
                leftBarEncouragement.setProgressDrawable(ContextCompat.getDrawable(
                        getApplicationContext(), R.drawable.progress_bar));
                leftBarEncouragement.setVisibility(View.INVISIBLE); // Hide the progress bar

                // Right
                rightBarEncouragement = v.findViewById(R.id.tablayout_tabItem_rightbar);
                rightBarEncouragement.setScaleY(tvHeight);
                rightBarEncouragement.setProgressDrawable(ContextCompat.getDrawable(
                        getApplicationContext(), R.drawable.progress_bar));
                rightBarEncouragement.setVisibility(View.INVISIBLE);
            } else if (position == 1) {
                goshoTabTV = tv;
                // Left
                leftBarGosho = v.findViewById(R.id.tablayout_tabItem_leftbar);
                leftBarGosho.setScaleY(tvHeight);
                leftBarGosho.setRotation(180); // Reverse the loading direction
                leftBarGosho.setProgressDrawable(ContextCompat.getDrawable(
                        getApplicationContext(), R.drawable.progress_bar));
                leftBarGosho.setVisibility(View.INVISIBLE);

                // Right
                rightBarGosho = v.findViewById(R.id.tablayout_tabItem_rightbar);
                rightBarGosho.setScaleY(tvHeight);
                rightBarGosho.setProgressDrawable(ContextCompat.getDrawable(
                        getApplicationContext(), R.drawable.progress_bar));
                rightBarGosho.setVisibility(View.INVISIBLE);
            }
            return v;
        }
    }

    // Class to adjust the items in the view pager to create a parallax effect
    private class ParallaxPagerTransformer implements ViewPager.PageTransformer {
        private int id;
        private int border = 0;
        private float speed = 0.2f;

        ParallaxPagerTransformer(int id) {
            this.id = id;
        }

        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        @Override
        public void transformPage(View view, float position) {

            View parallaxView = view.findViewById(id);

            if (parallaxView != null) {
                if (position > -1 && position < 1) {
                    float width = parallaxView.getWidth();
                    parallaxView.setTranslationX(-(position * width * speed));
                    float sc = ((float) view.getWidth() - border) / view.getWidth();
                    if (position == 0) {
                        view.setScaleX(1);
                        view.setScaleY(1);
                    } else {
                        view.setScaleX(sc);
                        view.setScaleY(sc);
                    }
                }
            }
        }

        public void setBorder(int px) {
            border = px;
        }

        public void setSpeed(float speed) {
            this.speed = speed;
        }
    }
}
