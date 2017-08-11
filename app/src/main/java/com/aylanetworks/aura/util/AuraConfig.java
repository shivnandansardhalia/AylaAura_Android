package com.aylanetworks.aura.util;
/*
 * Aura_Android
 *
 * Copyright 2015 Ayla Networks, all rights reserved
 */

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.text.TextUtils;

import com.aylanetworks.aura.AuraApplication;
import com.aylanetworks.aura.Constants;
import com.aylanetworks.aylasdk.AylaDevice;
import com.aylanetworks.aylasdk.AylaLog;
import com.aylanetworks.aylasdk.AylaSystemSettings;
import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import static com.aylanetworks.aylasdk.AylaDSManager.AylaDSSubscriptionType.*;
import static com.aylanetworks.aylasdk.AylaSystemSettings.ServiceLocation.*;
import static com.aylanetworks.aylasdk.AylaSystemSettings.ServiceType.*;

// Sample .auraconfig file contents:
//
//{
//        "allowDSS": true,
//        "allowOfflineUse": true,
//        "appId": "aura_0dfc7900-id",
//        "appSecret": "aura_0dfc7900-eUo-3Se7R25Z_QLeEiXqYkQDUNA",
//        "defaultNetworkTimeoutMs": 5000,
//        "name": "Brian's Cool Config",
//        "serviceLocation": "USA",
//        "serviceType": "Development",
//        "managedDevices": [
//        {
//        // Ayla EVB
//        "model" : "AY001MTC1",
//        "managedProperties" : ["Blue_button", "Blue_LED", "Green_LED"]
//        },
//        {
//        // Ayla EVB
//        "model" : "AY001MUS1",
//        "managedProperties" : ["Blue_button", "Blue_LED", "Green_LED"]
//        },
//        {
//        // Generic Gateway
//        "model" : "AY001MRT1",
//        "oemModel" : "generic",
//        "managedProperties" : ["num_nodes", "log"]
//        },
//        {
//        // Generic Gateway
//        "model" : "AY001MRT1",
//        "oemModel" : "ggdemo",
//        "managedProperties" : ["num_nodes", "log"]
//        },
//        {
//        // Smart plug
//        "oemModel" : "smartplug1",
//        "managedProperties" : ["outlet1"]
//        },
//        {
//        // Smart plug
//        "oemModel" : "EWPlug1",
//        "managedProperties" : ["outlet1"]
//        }
//    ]
// }

/**
 * Class used for storing and retrieving configuration settings for the Aura application. The
 * Developer Options screen, available by selecting "Forgot Password" from the login screen and
 * entering in "aylarocks", will show a set of AuraConfigs that the user can select before
 * signing in.
 *
 * A set of default configurations is provided, and may be added to by opening .auraconfig files
 * from email or the local file system. Configurations will replace existing configurations that
 * have the same name.
 *
 * An .auraconfig file contains a single JSON representation of an AuraConfig object.
 *
 * Configurations are saved in Shared Preferences, as well as the currently-selected configuration.
 */
public class AuraConfig implements AylaSystemSettings.DeviceDetailProvider {
    private static final String CONFIG_PREFS = "com.aylanetworks.aura.config";
    private static final String PREFS_KEY = "com.aylanetworks.aura.configs";
    private static final String PREFS_KEY_SELECTED = "com.aylanetworks.aura.configs.selected";

    public static final String CONFIG_DEVELOPMENT = "US Development";
    public static final String CONFIG_STAGING = "US Staging";
    public static final String CONFIG_FIELD = "US Field";
    public static final String CONFIG_DEMO = "US Demo";
    public static final String CONFIG_CHINA_DEV = "China Development";
    public static final String CONFIG_CHINA_FIELD = "China Field";

    private static AuraConfig __configDevelopment;
    private static AuraConfig __configStaging;
    private static AuraConfig __configDemo;
    private static AuraConfig __configField;
    private static AuraConfig __configChinaDev;
    private static AuraConfig __configChinaField;
    private static List<AuraConfig> __availableConfigurations;
    private static AuraConfig __selectedConfiguration;

    static {
        __configDevelopment = new AuraConfig(CONFIG_DEVELOPMENT,
                Constants.APP_ID_US_DEV, Constants.APP_SECRET_US_DEV,
                Development, USA);

        __configStaging = new AuraConfig(CONFIG_STAGING,
                Constants.APP_ID_STAGING, Constants.APP_SECRET_STAGING,
                Staging, USA);

        __configDemo = new AuraConfig(CONFIG_DEMO,
                Constants.APP_ID_DEMO, Constants.APP_SECRET_DEMO,
                Demo, USA);

        __configField = new AuraConfig(CONFIG_FIELD,
                Constants.APP_ID_US_FIELD, Constants.APP_SECRET_US_FIELD,
                Field, USA);

        __configChinaDev = new AuraConfig(CONFIG_CHINA_DEV,
                Constants.APP_ID_CN_DEV, Constants.APP_SECRET_CN_DEV,
                Development, China);

        __configChinaField = new AuraConfig(CONFIG_CHINA_FIELD,
                Constants.APP_ID_CN_FIELD, Constants.APP_SECRET_CN_FIELD,
                Field, China);

        setDefaultConfigurations();

        loadConfigurations();
    }

    /** Name for this configuration. Requred. */
    @Expose public String name;

    // Required fields
    @Expose public String appId;
    @Expose public String appSecret;

    // Optional fields- these fields are initialized with some reasonable default values
    @Expose public String serviceType = Development.name();
    @Expose public String serviceLocation = USA.name();
    @Expose public boolean allowDSS = true;
    @Expose public boolean allowOfflineUse = false;
    @Expose public int defaultNetworkTimeoutMs = 5000;
    @Expose public ManagedDevice[] managedDevices;
    @Expose public String[] dssSubscriptionTypeList = new String[]{
            AylaDSSubscriptionTypeDatapoint.stringValue()};

    /**
     * Returns a list of available AuraConfigs.
     * @return a list of available AuraConfigs
     */
    public static List<AuraConfig> getAvailableConfigurations() {
        return __availableConfigurations;
    }

    /**
     * This is a helper method that is called from the
     * {@link com.aylanetworks.aura.AuraDeviceDetailProvider} to provide a set of managed
     * property names for a given device.
     *
     * The device is checked against the array of managedDevices from this configuration, and if
     * matches are found, the appropriate array of property names will be returned. Otherwise,
     * null will be returned.
     *
     * All fields present in the Aura Configuration file must match in order for the managed
     * properties to be returned.
     *
     * @param device Device to return managed property names for
     * @return the array of managed property names for this device, or null
     */
    public String[] getManagedPropertyNames(AylaDevice device) {
        if (managedDevices == null) {
            return null;
        }

        for (ManagedDevice managedDevice : managedDevices) {
            if ((managedDevice.dsn == null ||
                    TextUtils.equals(device.getDsn(), managedDevice .dsn)) &&
                    (managedDevice.oemModel == null ||
                            TextUtils.equals(device.getOemModel(), managedDevice.oemModel)) &&
                    (managedDevice.model == null ||
                            TextUtils.equals(device.getModel(), managedDevice.model))) {
                return managedDevice.managedProperties;
            }
        }
        return null;
    }

    /**
     * Constructor for an AuraConfig object. Required fields are provided in the constructor
     * arguments.
     *
     * @param name Name for this configuration.
     * @param appId app ID for this configuration
     * @param appSecret app secret for this configuration
     */
    public AuraConfig(String name, String appId, String appSecret,
                      AylaSystemSettings.ServiceType serviceType,
                      AylaSystemSettings.ServiceLocation serviceLocation) {
        this.name = name;
        this.appId = appId;
        this.appSecret = appSecret;
        this.serviceType = serviceType.name();
        this.serviceLocation = serviceLocation.name();
    }

    public AuraConfig(AuraConfig other) {
        this.name = other.name;
        this.appId = other.appId;
        this.appSecret = other.appSecret;
        this.serviceType = other.serviceType;
        this.serviceLocation = other.serviceLocation;
        this.allowDSS = other.allowDSS;
        this.allowOfflineUse = other.allowOfflineUse;
        this.defaultNetworkTimeoutMs = other.defaultNetworkTimeoutMs;
        if (other.dssSubscriptionTypeList != null) {
            this.dssSubscriptionTypeList = new String[other.dssSubscriptionTypeList.length];
            System.arraycopy(other.dssSubscriptionTypeList, 0, this.dssSubscriptionTypeList, 0,
                    other.dssSubscriptionTypeList.length);
        }
        if (other.managedDevices != null) {
            this.managedDevices = new ManagedDevice[other.managedDevices.length];
            System.arraycopy(other.managedDevices, 0, this.managedDevices, 0,
                    other.managedDevices.length);
        }
    }

    /**
     * Applies this configuration to an AylaSystemSettings object
     * @param systemSettings the AylaSystemSettings object to apply this configuration to
     */
    public void apply(AylaSystemSettings systemSettings) {
        systemSettings.appId = appId;
        systemSettings.appSecret = appSecret;
        systemSettings.serviceType = AylaSystemSettings.ServiceType.valueOf(serviceType);
        systemSettings.serviceLocation = AylaSystemSettings.ServiceLocation
                .valueOf(serviceLocation);
        systemSettings.allowDSS = allowDSS;
        systemSettings.allowOfflineUse = allowOfflineUse;
        systemSettings.defaultNetworkTimeoutMs = defaultNetworkTimeoutMs;
        systemSettings.deviceDetailProvider = this;
        systemSettings.dssSubscriptionTypes = dssSubscriptionTypeList;
        if (dssSubscriptionTypeList != null) {
            systemSettings.dssSubscriptionTypes = new String[dssSubscriptionTypeList.length];
            System.arraycopy(dssSubscriptionTypeList, 0, systemSettings.dssSubscriptionTypes, 0,
                    dssSubscriptionTypeList.length);
        }
    }

    /**
     * Returns the currently-selected configuration.
     * @return the currently-selected configuration
     */
    public static AuraConfig getSelectedConfiguration() {
        return __selectedConfiguration;
    }

    public static void setSelectedConfiguration(AuraConfig config) {
        SharedPreferences.Editor editor = AuraApplication.getAppContext()
                .getSharedPreferences(CONFIG_PREFS, Context.MODE_PRIVATE).edit();
        editor.putString(PREFS_KEY_SELECTED, config.name);
        __selectedConfiguration = config;
        editor.apply();
    }

    /**
     * Loads configurations from SharedPreferences and sets the __selectedConfiguration
     */
    private static void loadConfigurations() {
        SharedPreferences prefs = AuraApplication.getAppContext()
                .getSharedPreferences(CONFIG_PREFS, Context.MODE_PRIVATE);

        String currentPrefs = prefs.getString(PREFS_KEY, null);
        String selectedConfig = prefs.getString(PREFS_KEY_SELECTED, null);
        Gson gson = new Gson();
        if (currentPrefs == null) {
            __selectedConfiguration = __availableConfigurations.get(0);
            saveConfigurations();
        } else {
            AuraConfig[] configs = gson.fromJson(currentPrefs, AuraConfig[].class);
            __availableConfigurations = new ArrayList<>(Arrays.asList(configs));
            for (AuraConfig config : __availableConfigurations) {
                if (TextUtils.equals(config.name, selectedConfig)) {
                    __selectedConfiguration = config;
                    break;
                }
            }
            if (__selectedConfiguration == null) {
                // Select the first item
                __selectedConfiguration = __availableConfigurations.get(0);
                setSelectedConfiguration(__selectedConfiguration);
            }
        }
    }

    /**
     * Writes the current configurations and selection to Shared Preferences
     */
    private static void saveConfigurations() {
        Gson gson = new Gson();

        SharedPreferences prefs = AuraApplication.getAppContext()
                .getSharedPreferences(CONFIG_PREFS, Context.MODE_PRIVATE);

        // Save our default preferences
        AuraConfig[] configs = __availableConfigurations.toArray(
                new AuraConfig[__availableConfigurations.size()]);
        String json = gson.toJson(configs);

        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PREFS_KEY, json);

        // Save selected item name
        editor.putString(PREFS_KEY_SELECTED, __selectedConfiguration.name);

        editor.apply();
    }

    private static void setDefaultConfigurations() {
        __availableConfigurations = new ArrayList<>();
        __availableConfigurations.add(__configDevelopment);
        __availableConfigurations.add(__configStaging);
        __availableConfigurations.add(__configDemo);
        __availableConfigurations.add(__configField);
        __availableConfigurations.add(__configChinaDev);
        __availableConfigurations.add(__configChinaField);
    }

    @Override
    public String toString() {
        return name;
    }

    /**
     * Imports data from a file. This is called from the DeveloperOptionsActivity when the user
     * launches the application from an .auraconfig file.
     * @param data The Uri data from the intent the app was launched with
     * @return true if the data was imported, false otherwise
     */
    public static boolean importData(Uri data) {
        final String scheme = data.getScheme();
        if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {
            try {
                ContentResolver cr = AuraApplication.getAppContext().getContentResolver();
                InputStream is = cr.openInputStream(data);
                if (is == null) {
                    return false;
                }
                // This delimiter means "beginning of the input" so we get a single
                // "token" containing the entire string
                Scanner scanner = new Scanner(is).useDelimiter("\\A");
                if (scanner.hasNext()) {
                    String json = scanner.next();
                    Gson gson = new Gson();
                    AuraConfig config = gson.fromJson(json, AuraConfig.class);
                    if (config != null) {
                        // Set the new configuration as the default
                        addConfig(config);
                        setSelectedConfiguration(config);
                    } else {
                        scanner.close();
                        is.close();
                        AylaLog.e("AuraConfig", "No configuration found in file");
                        return false;
                    }
                }
                scanner.close();
                is.close();

            } catch (FileNotFoundException e) {
                AylaLog.e("AuraConfig", "File not found exception trying to import auraconfig: "
                        + e.getMessage());
                return false;
            } catch (Exception e) {
                AylaLog.e("AuraConfig", "Exception trying to import auraconfig: " + e.getMessage());
                return false;
            }
            return true;
        }
        return false;
    }

    /**
     * Internal method to add or replace a configuration to our list. Saves the list too.
     * @param config The new configuration to add.
     */
    public static void addConfig(AuraConfig config) {
        // If we already have a config with this name, replace it
        removeConfig(config);
        __availableConfigurations.add(config);
        saveConfigurations();
    }

    public static void removeConfig(AuraConfig config) {
        AuraConfig foundConfig = null;
        if (__availableConfigurations.size() == 1) {
            // Don't delete the last one
            return;
        }

        for (AuraConfig existing : __availableConfigurations) {
            if (existing.name.equals(config.name)) {
                foundConfig = existing;
                break;
            }
        }

        if (foundConfig != null) {
            __availableConfigurations.remove(foundConfig);
        }

        // Select a new default if needed
        if (TextUtils.equals(__selectedConfiguration.name, config.name)) {
            __selectedConfiguration = __availableConfigurations.get(0);
        }

        saveConfigurations();
    }

    public static void restoreDefaults() {
        setDefaultConfigurations();
        saveConfigurations();
    }

    public class ManagedDevice {
        // Each ManagedDevice entry will be checked against all non-null values of a ManagedDevice
        // when determining the correct set of managed properties to return. If all defined values
        // match a given device, the managedProperties will be returned from the
        // DeviceDetailProvider for the matching device.
        @Expose
        public String dsn;
        @Expose
        public String oemModel;
        @Expose
        public String model;
        @Expose
        public String[] managedProperties;
    }
}
