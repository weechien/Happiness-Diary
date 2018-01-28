package com.goh.weechien.HappinessDiary;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

// Reset the alarm if the timezone, time, and date change.
// Also reset the alarm when the system reboots and when the app is updated.
public class GuidanceAlarmChgRec extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // Reset the alarm
        SettingsGuidanceNoti.setAlarm(context, 414179);
    }
}
