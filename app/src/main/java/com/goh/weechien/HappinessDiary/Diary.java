package com.goh.weechien.HappinessDiary;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.CheckedTextView;
import android.widget.TextView;

import java.lang.ref.WeakReference;

public class Diary extends BaseActivity {
    private static WeakReference<Activity> mainActivity; // Main activity's instance

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // If 0 is set, use the default container
        setLayoutID(0); // Setup the container layout
        setContentView(R.layout.activity_diary); // Setup the child layout for the container
    }

    @Override
    protected void onResume() {
        super.onResume();
        setDiaryLocale(LocaleHelper.getLanguage(this));

        // Set the text view in the navigation drawer as checked
        CheckedTextView checkedTextView = findViewById(R.id.diary);
        checkedTextView.setChecked(true);
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base, "en"));
    }

    @Override
    // Called when an item on the appbar is selected
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        // Set a default language
        if (id == R.id.action_english && !LocaleHelper.getLanguage(this).equals("en")) {
            // Reset the locale in the guidance activity (currently paused)
            // When the guidance activity resumes, the language will be changed
            ((Guidance) mainActivity.get()).setGuidanceLocale("en");
            setDiaryLocale("en"); // Reset this activity's views

        } else if (id == R.id.action_chinese && !LocaleHelper.getLanguage(this).equals("zh")) {
            ((Guidance) mainActivity.get()).setGuidanceLocale("zh");
            setDiaryLocale("zh");
        }
        return super.onOptionsItemSelected(item);
    }

    // Get the new language and reapply to the current activity's views
    void setDiaryLocale(String language) {
        Context context = LocaleHelper.setLocale(this, language);
        Resources resources = context.getResources();

        TextView textView = findViewById(R.id.toolbar_base_textview);
        textView.setText(resources.getString(R.string.title_activity_diary)); // Title of the activity

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
    }

    // Method to get the instance of the main activity
    public static void getMainContext(Activity activity) {
        mainActivity = new WeakReference<>(activity);
    }
}
