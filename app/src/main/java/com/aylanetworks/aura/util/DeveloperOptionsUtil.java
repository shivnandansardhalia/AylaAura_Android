package com.aylanetworks.aura.util;

import com.aylanetworks.aura.Constants;
import com.aylanetworks.aylasdk.AylaNetworks;
import com.aylanetworks.aylasdk.AylaSystemSettings;

/*
 * Aura
 *
 * Copyright 2016 Ayla Networks, all rights reserved
 */
public class DeveloperOptionsUtil {

    /**
     * Get `AylaSystemSettings` object base on the given service location and type.
     * @param location Service location: 0 - US, 1 - CN, 2 - EU
     * @param service Service type: 0 - Development, 1 - Field, 3 - Staging
     * @return AylaSystemSettings object with following properties set: App Id/Secret, service location/type.
     */
    public static AylaSystemSettings getSystemSettings(int location, int service) {
        AylaNetworks sharedInstance = AylaNetworks.sharedInstance();
        AylaSystemSettings settings;
        if (sharedInstance == null) {
            settings = new AylaSystemSettings();
        } else {
            settings = AylaNetworks.sharedInstance().getSystemSettings();
        }

        if (service == 0) { // Development
            settings.serviceType = AylaSystemSettings.ServiceType.Development;
            switch (location) {
                case 0: // US
                    settings.serviceLocation = AylaSystemSettings.ServiceLocation.USA;
                    settings.appId = Constants.APP_ID_US_DEV;
                    settings.appSecret = Constants.APP_SECRET_US_DEV;
                    break;
                case 1: // CN
                    settings.serviceLocation = AylaSystemSettings.ServiceLocation.China;
                    settings.appId = Constants.APP_ID_CN_DEV;
                    settings.appSecret = Constants.APP_SECRET_CN_DEV;
                    break;
                case 2: // EU
                    settings.serviceLocation = AylaSystemSettings.ServiceLocation.Europe;
                    settings.appId = Constants.APP_ID_EU_DEV;
                    settings.appSecret = Constants.APP_SECRET_EU_DEV;
                    break;
            }
        }
        else if (service == 1) { // Field
            settings.serviceType = AylaSystemSettings.ServiceType.Field;
            switch (location) {
                case 0: // US
                    settings.serviceLocation = AylaSystemSettings.ServiceLocation.USA;
                    settings.appId = Constants.APP_ID_US_FIELD;
                    settings.appSecret = Constants.APP_SECRET_US_FIELD;
                    break;
                case 1: // CN
                    settings.serviceLocation = AylaSystemSettings.ServiceLocation.China;
                    settings.appId = Constants.APP_ID_CN_FIELD;
                    settings.appSecret = Constants.APP_SECRET_CN_FIELD;
                    break;
                case 2: // EU
                    settings.serviceLocation = AylaSystemSettings.ServiceLocation.Europe;
                    settings.appId = Constants.APP_ID_EU_FIELD;
                    settings.appSecret = Constants.APP_SECRET_EU_FIELD;
                    break;
            }
        }
        else if (service == 2) { // Staging
            settings.serviceType = AylaSystemSettings.ServiceType.Staging;
            // Staging service is only located in the USA
            settings.appId = Constants.APP_ID_STAGING;
            settings.appSecret = Constants.APP_SECRET_STAGING;
        }

        return settings;
    }
}
