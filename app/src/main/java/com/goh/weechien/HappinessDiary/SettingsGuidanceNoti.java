package com.goh.weechien.HappinessDiary;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.os.Build;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TimePicker;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import static android.content.Context.ALARM_SERVICE;

// Custom preference class to show the time picker in the settings under Guidance Notification
public class SettingsGuidanceNoti extends DialogPreference implements PreferenceManager.OnActivityDestroyListener {
    private int lastHour = 0;
    private int lastMinute = 0;
    private TimePicker picker = null;
    private SettingsGuidanceNotiEnabler settingsGuidanceNotiEnabler;

    public SettingsGuidanceNoti(Context context, AttributeSet attrs) {
        super(context, attrs);

        // Button texts
        setPositiveButtonText(context.getString(R.string.set));
        setNegativeButtonText(context.getString(R.string.cancel));
    }

    @Override
    // Use a custom layout
    protected View onCreateView(ViewGroup parent) {
        super.onCreateView(parent);
        LayoutInflater li = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        return li.inflate(R.layout.pref_time, parent, false);
    }

    @Override
    // Called when the view is ready to bind to the parent layout
    protected void onBindView(View view) {
        super.onBindView(view);

        // Create an instance of the switch view and attach a listener
        // The listener checks if the switch view is attached to or detached from the window
        settingsGuidanceNotiEnabler = new SettingsGuidanceNotiEnabler(getContext(), new Switch(getContext()));
        settingsGuidanceNotiEnabler.setSwitch((Switch) view.findViewById(R.id.switch_notification_time));
        settingsGuidanceNotiEnabler.mSwitch.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {
                settingsGuidanceNotiEnabler.resume();
            }

            @Override
            public void onViewDetachedFromWindow(View v) {
                settingsGuidanceNotiEnabler.pause();
            }
        });
    }

    @Override
    // Only show the time picker if the switch view is checked
    protected void onClick() {
        if (!settingsGuidanceNotiEnabler.isSwitchOn()) {
            return;
        }
        super.onClick();
    }

    @Override
    // If the instance of your Preference class specifies a default value, then the system calls
    // onGetDefaultValue() when it instantiates the object in order to retrieve the value.
    // You must implement this method in order for the system to save the default value in the SharedPreferences.
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getString(index);
    }

    @Override
    // When the system adds your Preference to the screen, it calls onSetInitialValue() to notify
    // you whether the setting has a persisted value. If there is no persisted value, this call
    // provides you the default value.
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        String time;

        if (restoreValue) {
            // Restore existing state
            time = getPersistedString("08:00 AM");
        } else {
            // Set default state from the XML attribute
            time = defaultValue.toString();
            persistString(time); // The shared preference is always in a 12-hour format
        }
        // Convert from a 12-hour format to a 24-hour format
        lastHour = getHour(time);
        lastMinute = getMinute(time);
    }

    // Get the hour by splitting the string
    private static int getHour(String time) {
        // Convert 12-hour format to a 24-hour format
        SimpleDateFormat displayFormat = new SimpleDateFormat("HH:mm", Locale.US);
        SimpleDateFormat parseFormat = new SimpleDateFormat("hh:mm a", Locale.US);
        Date date = null;
        try {
            date = parseFormat.parse(time);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        String[] hour = displayFormat.format(date).split(":");
        return Integer.parseInt(hour[0]);
    }

    // Get the minute by splitting the string
    private static int getMinute(String time) {
        // Convert 12-hour format to a 24-hour format
        SimpleDateFormat displayFormat = new SimpleDateFormat("HH:mm", Locale.US);
        SimpleDateFormat parseFormat = new SimpleDateFormat("hh:mm a", Locale.US);
        Date date = null;
        try {
            date = parseFormat.parse(time);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        String[] minute = displayFormat.format(date).split(":");
        return Integer.parseInt(minute[1]);
    }

    @Override
    // Create a dialog box with the time picker
    protected View onCreateDialogView() {
        picker = new TimePicker(getContext());
        return picker;
    }

    @SuppressWarnings("deprecation")
    @Override
    // Set the preset time when showing the time picker
    protected void onBindDialogView(View v) {
        super.onBindDialogView(v);

        // Use a different method if API > 23
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            picker.setHour(lastHour);
            picker.setMinute(lastMinute);

        } else {
            picker.setCurrentHour(lastHour);
            picker.setCurrentMinute(lastMinute);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    // Get the new time when the dialog box closes
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult) {
            // Use a different method if API > 23
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                lastHour = picker.getHour();
                lastMinute = picker.getMinute();
            } else {
                lastHour = picker.getCurrentHour();
                lastMinute = picker.getCurrentMinute();
            }
            // // Convert 24-hour format to a 12-hour format
            String amPM = lastHour >= 12 ? "PM" : "AM";
            String time = String.format("%02d:%02d %s",
                    lastHour % 12 == 0 ? 12 : lastHour % 12, lastMinute, amPM);

            if (callChangeListener(time)) {
                persistString(time); // Set the new time
            }
        }
    }

    // Set an alarm to send out notifications at a specific time
    static void setAlarm(Context mContext, int requestCode) {
        Intent myIntent;
        final String GUIDANCE_ENABLED = "GUIDANCE_ENABLED";
        final String GUIDANCE_TIME = "pref_guidanceNotification";

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        // Check if the Guidance Notification setting is enabled
        boolean isGuidanceOn = prefs.getBoolean(GUIDANCE_ENABLED, true);
        // Check the time set to send the daily guidance
        String guidanceTime = prefs.getString(GUIDANCE_TIME, "8:00 AM");

        // Intent to start the broadcast receiver, which will in turn start an intent service
        // to build and send out notifications
        myIntent = new Intent(mContext, GuidanceNotiRec.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, requestCode, myIntent, 0);

        // Cancel an existing alarm
        cancelAlarmIfExists(mContext, requestCode, myIntent);

        // Proceed only if the Guidance Notification setting is enabled
        if (isGuidanceOn) {
            Calendar firingCal = Calendar.getInstance();
            Calendar currentCal = Calendar.getInstance();

            int hour = getHour(guidanceTime); // Convert to hour
            int minute = getMinute(guidanceTime); // Convert to minute

            firingCal.set(Calendar.HOUR_OF_DAY, hour); // At the hour to fire
            firingCal.set(Calendar.MINUTE, minute); // Particular minute to fire
            firingCal.set(Calendar.SECOND, 0); // Particular second to fire

            // If the time set has already past, set it to the next day
            if (firingCal.compareTo(currentCal) < 0) {
                firingCal.add(Calendar.DAY_OF_MONTH, 1);
            }
            // Set the alarm to repeat at an approximate time and repeat every day
            AlarmManager alarmManager = (AlarmManager) mContext.getSystemService(ALARM_SERVICE);
            alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, firingCal.getTimeInMillis(),
                    AlarmManager.INTERVAL_DAY, pendingIntent);
        }
    }

    // Cancel an existing alarm
    private static void cancelAlarmIfExists(Context mContext, int requestCode, Intent intent) {
        try {
            // Try to cancel an existing alarm
            PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, requestCode, intent, 0);
            AlarmManager am = (AlarmManager) mContext.getSystemService(ALARM_SERVICE);
            am.cancel(pendingIntent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Control the switch button in the pref_time layout
    private class SettingsGuidanceNotiEnabler implements CompoundButton.OnCheckedChangeListener {
        final Context mContext;
        Switch mSwitch;

        SettingsGuidanceNotiEnabler(Context context, Switch swtch) {
            mContext = context;
            setSwitch(swtch);
        }

        // Get the switch view and attach a listener
        void setSwitch(Switch swtch) {
            if (mSwitch == swtch)
                return;

            if (mSwitch != null)
                mSwitch.setOnCheckedChangeListener(null);
            mSwitch = swtch;
            mSwitch.setOnCheckedChangeListener(this);

            mSwitch.setChecked(isSwitchOn());
        }

        // Called when the switch is changed and update the shared preference
        public void onCheckedChanged(CompoundButton view, boolean isChecked) {
            SharedPreferences prefs;
            SharedPreferences.Editor editor;

            prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
            editor = prefs.edit();

            editor.putBoolean("GUIDANCE_ENABLED", isChecked);
            editor.apply();

            setAlarm(mContext, 414179);
        }

        // Check the current state of the switch by assessing the shared preference
        boolean isSwitchOn() {
            SharedPreferences prefs;
            prefs = PreferenceManager.getDefaultSharedPreferences(mContext);

            return prefs.getBoolean("GUIDANCE_ENABLED", true);
        }

        // Enable the listener and set the switch based on its shared preference
        void resume() {
            mSwitch.setOnCheckedChangeListener(this);
            mSwitch.setChecked(isSwitchOn());
        }

        // Detach the listener
        void pause() {
            mSwitch.setOnCheckedChangeListener(null);
        }
    }
}









