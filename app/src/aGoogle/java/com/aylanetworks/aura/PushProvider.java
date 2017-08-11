package com.aylanetworks.aura;

import android.content.Context;

import com.aylanetworks.aylasdk.AylaNetworks;
import com.aylanetworks.aylasdk.AylaSystemSettings;

/**
 * Aura
 * <p/>
 * Copyright 2016 Ayla Networks Inc, all rights reserved
 */

public class PushProvider {

    public static void start(String email) {
        AylaSystemSettings settings = AylaNetworks.sharedInstance().getSystemSettings();
        if (settings != null &&
                settings.pushNotificationSenderId != null) {
            PushNotification pushNotification =
                    new PushNotification();
            pushNotification.init(
                    settings.pushNotificationSenderId,
                    email,
                    settings.appId);

        }
    }
}