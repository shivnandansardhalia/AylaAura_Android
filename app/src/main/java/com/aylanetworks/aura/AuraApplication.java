package com.aylanetworks.aura;
/*
 * Aura_Android
 *
 * Copyright 2015 Ayla Networks, all rights reserved
 */

import android.app.Application;
import android.content.Context;

public class AuraApplication extends Application {
    private static Context __context;
    public void onCreate() {
        super.onCreate();
        __context = getApplicationContext();
    }

    public static Context getAppContext() {
        return __context;
    }
}
