package com.goh.weechien.HappinessDiary;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

// Start the intent service when the alarm manager fires
public class GuidanceNotiRec extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent intent1 = new Intent(context, GuidanceNotiSvc.class);
        context.startService(intent1);
    }
}
