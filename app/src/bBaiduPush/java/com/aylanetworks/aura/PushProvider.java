package com.aylanetworks.aura;

import com.aylanetworks.aura.util.PushUtils;
import com.aylanetworks.aylasdk.AylaNetworks;
import com.aylanetworks.aylasdk.AylaSystemSettings;
import com.baidu.android.pushservice.PushConstants;
import com.baidu.android.pushservice.PushManager;

/**
 * Aura
 * <p/>
 * Copyright 2016 Ayla Networks Inc, all rights reserved
 */

public class PushProvider {

    public static void start(String email) {
        AylaSystemSettings settings = AylaNetworks.sharedInstance().getSystemSettings();
        PushManager.startWork(
                settings.context
                , PushConstants.LOGIN_TYPE_API_KEY
                , PushUtils.getMetaValue(settings.context, "api_key")
        );
    }
}
