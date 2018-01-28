package com.goh.weechien.HappinessDiary;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

public class Settings extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(R.id.settings_content, new GeneralPreferenceFragment())
                .commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        TextView textView = findViewById(R.id.toolbar_settings_textview);
        textView.setText(R.string.title_activity_settings); // Title of the activity
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base, "en"));
    }

    // Fragment in the settings activity to show the preferences
    public static class GeneralPreferenceFragment extends PreferenceFragment
            implements SharedPreferences.OnSharedPreferenceChangeListener {

        public static final String GUIDANCE_TIME = "pref_guidanceNotification";
        public static final String GUIDANCE_CARD_COLOR = "pref_cardColor";

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences); // Load the preference xml
            final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());

            // Set timeSummary to be the user-description for the guidance time
            Preference timePref = findPreference(GUIDANCE_TIME);
            String timeSummary = getString(R.string.guidance_notification_summary) + sharedPref.getString(GUIDANCE_TIME, "");
            timePref.setSummary(timeSummary);

            // Set timeSummary to be the user-description for the guidance card color
            final Preference colorPref = findPreference(GUIDANCE_CARD_COLOR);
            boolean colorSummary = sharedPref.getBoolean(GUIDANCE_CARD_COLOR, true);
            colorPref.setSummary(colorSummary ? R.string.card_color_enabled : R.string.card_color_disabled);
        }

        @Override
        public void onResume() {
            super.onResume();
            // Listen to any changes in the preferences (calls onSharedPreferenceChanged)
            getPreferenceScreen().getSharedPreferences()
                    .registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onPause() {
            super.onPause();
            // Remove the listener
            getPreferenceScreen().getSharedPreferences()
                    .unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        // Called when the shared preference changes
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            Preference connectionPref = findPreference(key);

            if (key.equals(GUIDANCE_TIME)) {
                // Set summary to be the user-description for the guidance time
                String summary = getString(R.string.guidance_notification_summary) +
                        sharedPreferences.getString(GUIDANCE_TIME, "");
                connectionPref.setSummary(summary);

                // Reset the alarm whenever the shared preferences change
                SettingsGuidanceNoti.setAlarm(getActivity(), 414179);

            } else if (key.equals(GUIDANCE_CARD_COLOR)) {
                // Set summary to be the user-description for the guidance card color
                final boolean summary = sharedPreferences.getBoolean(GUIDANCE_CARD_COLOR, true);
                connectionPref.setSummary(summary ? R.string.card_color_enabled : R.string.card_color_disabled);
            }
        }
    }
}