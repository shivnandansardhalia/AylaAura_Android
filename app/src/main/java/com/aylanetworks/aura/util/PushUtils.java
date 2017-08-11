package com.aylanetworks.aura.util;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;

import com.aylanetworks.aura.MainActivity;
import com.aylanetworks.aura.R;
import com.aylanetworks.aylasdk.AylaLog;

import java.io.File;
import java.io.IOException;

/**
 * Aura
 * <p/>
 * Copyright 2016 Ayla Networks Inc, all rights reserved
 */
public class PushUtils {

    private static final String tag = PushUtils.class.getSimpleName();

    static final String RESPONSE_METHOD = "method";
    static final String RESPONSE_CONTENT = "content";
    static final String RESPONSE_ERRCODE = "errcode";
    protected static final String EXTRA_ACCESS_TOKEN = "access_token";
    public static final String EXTRA_MESSAGE = "message";

    private static final int PRIORITY_MAX = 0;
    public static final int NOTIFICATION_ID = 1;

    public static boolean isBaiduPushAvailable(Context context) {
        String appKey = getMetaValue(context, "api_key");
        if (!TextUtils.isEmpty(appKey)) {
            AylaLog.d(tag, "Baidu push is available. API Key:" + appKey);
            return true;
        }
        return false;
    }

    /**
     * Retrieve API_KEY for Baidu Push.
     * @param context Application context.
     * @param metaKey The string claimed in meta-data name field.
     * @return API key claimed in manifest file.
     * */
    public static String getMetaValue(final Context context, final String metaKey) {
        Bundle metaData = null;
        String apiKey = null;
        if (context == null || metaKey == null) {
            return null;
        }
        try {
            ApplicationInfo ai = context.getPackageManager()
                    .getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            if ( null != ai ) {
                metaData = ai.metaData;
            }
            if ( null != metaData ) {
                apiKey = metaData.getString(metaKey);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return apiKey;
    }


    /**
     * Restore shared preferences for Baidu Push.
     * @param context  Application context.
     * @return true if it is binding, else false.
     */
    public static boolean hasBind(final Context context) {
        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(context);
        String flag = sp.getString("bind_flag", "");
        if ("ok".equalsIgnoreCase(flag)) {
            return true;
        }
        return false;
    }


    /**
     * Save shared preferences for Baidu Push.
     * @param context Application context.
     * @param flag  binding state. True on binding successfully, else false.
     * */
    public static void setBind(Context context, boolean flag) {
        String flagStr = "not";
        if (flag) {
            flagStr = "ok";
        }
        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("bind_flag", flagStr);
        editor.commit();
    }

    private final static MediaPlayer mp = new MediaPlayer();
    /**
     * Play sound when push notification pops up. Only available for Baidu Push for now.
     * @param audioFileName the music file name to be played, root path is Environment
     *                      .DIRECTORY_MUSIC by default.
     * @return True if playing successfully, else false.
     * */
    public static boolean playSound(final String audioFileName) {
        boolean playedSound = false;
        if (TextUtils.isEmpty(audioFileName)) {
            return playedSound;
        }

        if(mp.isPlaying()) {
            mp.stop();
            mp.reset();
        }
        try {
            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
            File soundFile = new File(path, audioFileName);
            mp.setDataSource(soundFile.getPath());
            mp.prepare();
            mp.start();
            playedSound = true;
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return playedSound;
    }


    /**
     * Email ID when registering push service.
     * @param context Application context.
     * @param emailAddress Receiver`s email address.
     * @param Id registration_id for google push, "user_id channel_id" for baidu push.
     */
    public static void emailID(final Context context, final String emailAddress, final String Id) {
        String emailSubject = "Registering device for " + Id;
        String emailMessage = Id;
        // below is the code for sending an email
        Intent emailIntent = new Intent(
                Intent.ACTION_SEND);
        emailIntent.putExtra(Intent.EXTRA_EMAIL, emailAddress);
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, emailSubject);
        emailIntent.setType("plain/text");
        emailIntent.putExtra(Intent.EXTRA_TEXT, emailMessage);
        if (context!=null) {
            context.startActivity(emailIntent);
        }
    }



    /**
     * Build the notification and post it to the local notification service.
     * When the notification is tapped in the notification bar, the appIntent
     * is sent, launching the app.
     * @param context Application context.
     * @param msg The message to be poped up.
     * @param soundFileName The file name of the sound played when notifications popped up. Empty
     *                      or "none" means silence; "alarm" means system default alarm voice; or
     *                      customized sound in Environment.DIRECTORY_MUSIC; default is system`s
     *                      default notification voice.
     * */
    public static void sendNotification(final Context context
            , final String msg, final String soundFileName) {
        if (context == null) {
            Log.d(tag, "sendNotification, context param is null.");
            return;
        }

        NotificationManager manager
                = (NotificationManager)context.getSystemService(context.NOTIFICATION_SERVICE);

        Intent i = new Intent(context, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(context, 0, i, 0);
        Uri uri = null;

        if (TextUtils.isEmpty(soundFileName)) {
            Log.d(tag, "sendNotification, sound is empty.");
        } else if ("none".equalsIgnoreCase(soundFileName)) {
            // NOP
        }
        else if ("default".equalsIgnoreCase(soundFileName)) {
            // TYPE_NOTIFICATION or TYPE_ALARM
            uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        } else if ("alarm".equalsIgnoreCase(soundFileName)) {
            // TYPE_NOTIFICATION or TYPE_ALARM
            uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        } else {
            boolean isSoundPlayed;
            isSoundPlayed = PushUtils.playSound(soundFileName);
            if (!isSoundPlayed) {
                // TYPE_NOTIFICATION or TYPE_ALARM
                uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Ayla Control")
                .setStyle(new NotificationCompat.BigTextStyle().bigText(msg))
                .setLights(0xFFff0000, 500, 500) // flashing red light
                .setContentText(msg)
                .setDefaults(Notification.DEFAULT_VIBRATE
                        | Notification.FLAG_SHOW_LIGHTS);

        if (uri != null) {
            builder.setSound(uri);
        }
        builder.setPriority(PRIORITY_MAX);
        builder.setContentIntent(pi);

        manager.notify(NOTIFICATION_ID, builder.build());
    }


}





