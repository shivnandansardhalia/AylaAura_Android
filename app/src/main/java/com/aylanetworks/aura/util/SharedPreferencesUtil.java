package com.aylanetworks.aura.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * SharedPreference Helper, using Application Context.
 * Created by andy on 4/26/16.
 */
public class SharedPreferencesUtil {

    private static final String KEY_SERVICE_LOCATION = "key_service_location";
    private static final String KEY_SERVICE_Type = "key_service_type";
    private static final String KEY_EMAIL = "key_email";

    private static SharedPreferencesUtil instance;

    private SharedPreferences mPrefs;

    private SharedPreferencesUtil(Context context) {
        mPrefs = context.getApplicationContext().getSharedPreferences("developer_options", Context.MODE_PRIVATE);
    }

    public synchronized static SharedPreferencesUtil getInstance(Context context) {
        if (instance == null) {
            instance = new SharedPreferencesUtil(context);
        }

        return instance;
    }

    /**
     * Get cached service location index:
     * <li>0 - US</li>
     * <li>1 - CN</li>
     * <li>2 - EU</li>
     * @return
     */
    public int getServiceLocationIndex() {
        return mPrefs.getInt(KEY_SERVICE_LOCATION, 0);
    }

    public void saveServiceLocationIndex(int index) {
        mPrefs.edit().putInt(KEY_SERVICE_LOCATION, index).apply();
    }

    /**
     * Get cached service type index:
     * <li>0 - development</li>
     * <li>1 - filed</li>
     * <li>2 - staging</li>
     * @return
     */
    public int getServiceTypeIndex() {
        return mPrefs.getInt(KEY_SERVICE_Type, 0);
    }

    public void saveServiceTypeIndex(int index) {
        mPrefs.edit().putInt(KEY_SERVICE_Type, index).apply();
    }

    public String getCachedEmail() {
        return mPrefs.getString(KEY_EMAIL, "");
    }

    public void saveAccountEmail(String email) {
        mPrefs.edit().putString(KEY_EMAIL, email).apply();
    }
}
