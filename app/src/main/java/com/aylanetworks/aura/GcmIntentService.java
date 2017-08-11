/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.aylanetworks.aura;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import java.util.List;
/*
 * Copyright 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * This {@code IntentService} does the actual handling of the GCM message.
 * {@code GcmBroadcastReceiver} (a {@code WakefulBroadcastReceiver}) holds a
 * partial wake lock for this service while the service does its work. When the
 * service is finished, it calls {@code completeWakefulIntent()} to release the
 * wake lock.
 */
public class GcmIntentService extends IntentService {
    public static final int NOTIFICATION_ID = 1;
    private NotificationManager mNotificationManager;
    NotificationCompat.Builder builder;

    public GcmIntentService() {
        super("GcmIntentService");
    }

    public static final String TAG = "GCM Demo";
    private static final int PRIORITY_MAX = 0;

    @SuppressWarnings("unchecked")
    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        // The getMessageType() intent parameter must be the intent you received
        // in your BroadcastReceiver.
        String messageType = gcm.getMessageType(intent);

        if (!extras.isEmpty()) {  // has effect of unparcelling Bundle
            /*
             * Filter messages based on message type. Since it is likely that GCM will be
             * extended in the future with new message types, just ignore any message types you're
             * not interested in, or that you don't recognize.
             */
            if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
                sendNotification("Send error: " + extras.toString(), null);
            } else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(messageType)) {
                sendNotification("Deleted messages on server: " + extras.toString(), null);
                // If it's a regular GCM message, do some work.
            } else if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
                // This loop represents the service doing some work.
                //PushNotification.playSound("Beep_once.ogg");
                for (int i = 0; i < 3; i++) {
                    Log.i(TAG, "Working... " + (i + 1) + "/3 @ " + SystemClock.elapsedRealtime());
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                    }
                }
                Log.i(TAG, "Completed work @ " + SystemClock.elapsedRealtime());
                // Post notification of received message.
                String message = extras.getString("message");
                String sound = extras.getString("sound");
                //String other = extras.getString("other");
                sendNotification(message, sound);
                Log.i(TAG, "Received: " + extras.toString());
            }
        }
        // Release the wake lock provided by the WakefulBroadcastReceiver.
        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }

    // Build the notification and post it to the local notification service.
    // When the notification is tapped in the notification bar, the appIntent is sent, launching the app
    private void sendNotification(String msg, String sound) {
        mNotificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);

        // Find the launcher class for our application
        PackageManager pm = getPackageManager();
        String packageName = getPackageName();
        Intent query = new Intent(Intent.ACTION_MAIN);
        Class launcherClass = null;
        query.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> foundIntents = pm.queryIntentActivities(query, 0);
        for (ResolveInfo info : foundIntents) {
            if (TextUtils.equals(info.activityInfo.packageName, packageName)) {
                launcherClass = MainActivity.class;
            }
        }

        if (launcherClass == null) {
            Log.e(TAG, "Could not find application launcher class");
            return;
        }

        Intent appIntent = new Intent(this, launcherClass); // main activity of Ayla Control/aMCA
        appIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        Log.d(TAG, "ContentIntent activity "+appIntent.getComponent());
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, appIntent, 0);

        //Determine the sound to be played
        Uri soundUri = null;
        if (sound == null) {
            // NOP
            //PushNotification.playSound("bdth.mp3");
        } else if (sound.equals("none")) {
            // NOP
        } else if (sound.equals("default")) {
            soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION); // TYPE_NOTIFICATION or TYPE_ALARM
        } else if (sound.equals("alarm")) {
            soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM); // TYPE_NOTIFICATION or TYPE_ALARM
        } else {
            boolean playedSound;
            playedSound = PushNotification.playSound(sound);
            if (playedSound == false) {
                soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION); // TYPE_NOTIFICATION or TYPE_ALARM
            }
        }

        // @formatter:off
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                //.setSound(soundUri)
                .setSmallIcon(R.drawable.ic_push_icon)
                .setContentTitle(getResources().getString(R.string.app_name))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(msg))
                .setLights(0xFFff0000, 500, 500) // flashing red light
                .setContentText(msg)
                .setAutoCancel(true)
                //.setPriority(Notification.FLAG_HIGH_PRIORITY)
                .setDefaults(Notification.DEFAULT_VIBRATE | Notification.FLAG_SHOW_LIGHTS);
        // @formatter:on

        if (soundUri != null) {
            mBuilder.setSound(soundUri);
        }
        mBuilder.setPriority(PRIORITY_MAX);
        mBuilder.setContentIntent(contentIntent);

        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }
}
