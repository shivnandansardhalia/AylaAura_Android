package com.aylanetworks.aura;
/*
 * Aura
 *
 * Copyright 2015 Ayla Networks, all rights reserved
 */

import android.text.TextUtils;

import com.aylanetworks.aura.localdevice.GrillRightDevice;
import com.aylanetworks.aura.util.AuraConfig;
import com.aylanetworks.aylasdk.AylaDevice;
import com.aylanetworks.aylasdk.AylaSystemSettings;
import com.aylanetworks.aylasdk.localdevice.ble.AylaBLEDemoBoard;

public class AuraDeviceDetailProvider implements AylaSystemSettings.DeviceDetailProvider {
    public static final String[] EVB_LAN_PROPERTIES = new String[]{"Blue_button", "Blue_LED",
            "Green_LED","Red_LED"};
    public static final String[] GG_LAN_PROPERTIES = new String[]{"join_enable", "join_status", "cmd",
            "batch_hold"};
    public static final String[] PLUG_PROPERTIES = new String[]{"outlet1"};

    public static final String EVB_MODEL = "AY001MTC1";
    public static final String EVB_MODEL_1 = "AY001MUS1";
    public static final String GG_MODEL = "AY001MRT1";
    public static final String GG_OEM_MODEL = "generic";

    public static final String[] EVB_SCHEDULE_PROPERTIES = new String[]{"Blue_LED", "Green_LED"};
    public static final String[] PLUG_SCHEDULE_PROPERTIES = new String[]{"outlet1"};
    public static final String OEM_MODEL_EVB = "ledevb";
    public static final String OEM_MODEL_PLUG="smartplug1";
    public static final String PLUG_OEM_MODEL = "EWPlug1";


    @Override
    public String[] getManagedPropertyNames(AylaDevice device) {

        // First check the AuraConfig before using our default property names
        String[] properties = AuraConfig.getSelectedConfiguration().getManagedPropertyNames(device);
        if (properties != null) {
            return properties;
        }

        if (TextUtils.equals(device.getOemModel(), PLUG_OEM_MODEL)) {
            return PLUG_PROPERTIES;
        }

        if (TextUtils.equals(device.getModel(), EVB_MODEL) || TextUtils.equals(device.getModel(),
                EVB_MODEL_1)) {
            // This is an EVB
            return EVB_LAN_PROPERTIES;
        }

        if (TextUtils.equals(device.getModel(), GG_MODEL) &&
                TextUtils.equals(device.getOemModel(), GG_OEM_MODEL)) {
            // This is a generic gateway
            return GG_LAN_PROPERTIES;
        }

        if (device instanceof GrillRightDevice) {
            return GrillRightDevice.getManagedPropertyNames();
        }

        if (device instanceof AylaBLEDemoBoard) {
            return AylaBLEDemoBoard.getManagedPropertyNames();
        }

        return null;
    }
}
