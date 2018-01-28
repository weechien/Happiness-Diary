package com.goh.weechien.HappinessDiary;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;

import java.util.Calendar;

// Build and send a notification when triggered by the GuidanceNotiRec
public class GuidanceNotiSvc extends IntentService {

    public GuidanceNotiSvc() {
        super("GuidanceNotiSvc");
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void onHandleIntent(Intent intent) {
        int NOTIFY_REQUEST_CODE = 881911;
        int NOTIFICATION_ID = 911881;

        // Get the day of the year
        Calendar calendar = Calendar.getInstance();
        int dayOfYear = calendar.get(Calendar.DAY_OF_YEAR);

        // Add the string array
        String[] myDate = getResources().getStringArray(R.array.daily_encouragement_and_gosho_date);
        String[] myContent = getResources().getStringArray(R.array.daily_encouragement_content);
        String mySource = getString(R.string.daisaku_ikeda);

        // Use this class to adjust the string arrays by calculating the correct days (365/366)
        GuidanceEncouragementFragment.ArrangeDays arrangeDays = new GuidanceEncouragementFragment.ArrangeDays();
        arrangeDays.init(myDate, myContent, mySource);

        //Get the date, content and source
        String date = arrangeDays.getDateList().get(dayOfYear - 1);
        date = date.trim(); // Trim the date to remove the spacing at the beginning
        String content = arrangeDays.getContentList().get(dayOfYear - 1);
        String source = arrangeDays.getSourceList();

        // Intent to start the Guidance activity
        Intent notifyIntent = new Intent(this, Guidance.class);
        notifyIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        notifyIntent.putExtra("EncouragementToday", dayOfYear);
        PendingIntent pendingNotifyIntent = PendingIntent.getActivity(
                this, NOTIFY_REQUEST_CODE, notifyIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        // Intent to share the guidance via the notification
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.daily_encouragement)
                + " " + Guidance.getSunriseEmoji()
                + "\n" + date + "\n\n" + content + "\n\n" + source + "\n\n"
                + getString(R.string.share_promotion1)
                + new String(Character.toChars(0x1F54A))
                + getString(R.string.share_promotion2));

        PendingIntent pendingShareIntent = PendingIntent.getActivity(this, NOTIFY_REQUEST_CODE,
                Intent.createChooser(shareIntent, getString(R.string.share_with)), PendingIntent.FLAG_CANCEL_CURRENT);

        // Build the notification and add the intents
        NotificationCompat.Builder builder;
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationChannel();
            builder = new NotificationCompat.Builder(this, getString(R.string.guidance_notification));
        } else {
            builder = new NotificationCompat.Builder(this);
        }

        // Basic items to build
        builder.setContentTitle(getString(R.string.daily_guidance));
        builder.setContentText(date);
        // Use a different set of icon depending on the Android OS version
        int smallIcon = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) ?
                R.drawable.ic_stat_noti_launcher : R.mipmap.ic_launcher;
        builder.setSmallIcon(smallIcon);

        // Add the wall of text
        NotificationCompat.BigTextStyle bigText = new NotificationCompat.BigTextStyle();
        bigText.setBigContentTitle(getString(R.string.daily_guidance));
        bigText.bigText(date + "\n\n" + content + "\n\n" + source);
        builder.setStyle(bigText);

        // Add the share intent
        NotificationCompat.Action action = new NotificationCompat.Action.Builder(
                android.R.drawable.ic_menu_share, getString(R.string.guidance_share), pendingShareIntent).build();
        builder.addAction(action);

        // Cancel intent
        Intent cancelIntent = new Intent(this, CancelNotiRec.class);
        Bundle extras = new Bundle();
        extras.putInt("notification_id", NOTIFICATION_ID);
        cancelIntent.putExtras(extras);
        PendingIntent pendingCancelIntent =
                PendingIntent.getBroadcast(this, NOTIFICATION_ID, cancelIntent,
                        PendingIntent.FLAG_CANCEL_CURRENT);
        NotificationCompat.Action actionCancel = new NotificationCompat.Action.Builder(
                R.drawable.ic_close_layer, getString(R.string.dismiss), pendingCancelIntent).build();
        builder.addAction(actionCancel);

        builder.setColor(ContextCompat.getColor(this, R.color.colorAccent_Guidance));
        builder.setContentIntent(pendingNotifyIntent); // Enable launch of activity from notifications
        builder.setVibrate(new long[]{1000, 1000}); // Vibrate when a notification is received
        builder.setLights(Color.CYAN, 3000, 3000); // Notification lights
        // Use the default sound
        builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
        builder.setAutoCancel(true); // Automatically dismiss the notification when touched

        // Send out the notification
        Notification notificationCompat = builder.build();
        NotificationManagerCompat managerCompat = NotificationManagerCompat.from(this);
        managerCompat.notify(NOTIFICATION_ID, notificationCompat);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    void notificationChannel() {
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // The id of the channel.
        String id = getString(R.string.guidance_notification);
        // The user-visible name of the channel.
        CharSequence name = getString(R.string.guidance_notification);
        // The user-visible description of the channel.
        String description = getString(R.string.guidance_notification);
        int importance = NotificationManager.IMPORTANCE_LOW;
        NotificationChannel mChannel = new NotificationChannel(id, name, importance);
        // Configure the notification channel.
        mChannel.setDescription(description);
        mChannel.enableLights(true);
        // Sets the notification light color for notifications posted to this
        // channel, if the device supports this feature.
        mChannel.setLightColor(Color.RED);
        mChannel.enableVibration(true);
        mChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
        mNotificationManager.createNotificationChannel(mChannel);
    }
}
