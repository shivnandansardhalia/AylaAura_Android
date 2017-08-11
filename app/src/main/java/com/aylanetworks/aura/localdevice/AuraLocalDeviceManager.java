package com.aylanetworks.aura.localdevice;

/*
 * Aura_Android
 *
 * Copyright 2015 Ayla Networks, all rights reserved
 */


import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.text.TextUtils;

import com.aylanetworks.aylasdk.AylaDevice;
import com.aylanetworks.aylasdk.localdevice.ble.AylaBLEDemoBoard;
import com.aylanetworks.aylasdk.localdevice.ble.AylaBLEDevice;
import com.aylanetworks.aylasdk.localdevice.ble.AylaBLEDeviceManager;
import com.aylanetworks.aylasdk.localdevice.ble.ScanRecordHelper;

import java.util.UUID;

/**
 * The AuraLocalDeviceManager is a plugin injected into the Ayla SDK during app initialization.
 * It is responsible for managing local BLE devices including any devices supporting the Ayla
 * GATT profile as well as the Oregon Scientific GrillRight thermometer.
 *
 * Generic Ayla GATT devices are supported by the superclass, AylaBLEDeviceManager. This class
 * contains additional support for the GrillRight thermometer.
 */
public class AuraLocalDeviceManager extends AylaBLEDeviceManager {
    public AuraLocalDeviceManager(Context context) {
        super(context, new UUID[] {GrillRightDevice.SERVICE_GRILL_RIGHT,
                AylaBLEDevice.SERVICE_AYLA_BLE});
    }

    @Override
    public Class<? extends AylaDevice> getDeviceClass(String model, String oemModel, String uniqueId) {

        if (TextUtils.equals(model, GrillRightDevice.GRILL_RIGHT_MODEL) &&
                TextUtils.equals(oemModel, GrillRightDevice.GRILL_RIGHT_OEM_MODEL)) {
            return GrillRightDevice.class;
        }

        if (TextUtils.equals(model, AylaBLEDemoBoard.BLE_DEMO_MODEL)) {
            return AylaBLEDemoBoard.class;
        }
        return super.getDeviceClass(model, oemModel, uniqueId);
    }

    @Override
    protected AylaBLEDevice createLocalDevice(BluetoothDevice bluetoothDevice, int rssi,
                                              byte[] scanRecord) {

        ScanRecordHelper srh = ScanRecordHelper.parseFromBytes(scanRecord);

        if (srh != null && srh.containsService(GrillRightDevice.SERVICE_GRILL_RIGHT)) {
            return new GrillRightDevice(bluetoothDevice, rssi, scanRecord);
        }

        if (srh != null && srh.containsService(AylaBLEDevice.SERVICE_AYLA_BLE)) {
            return new AylaBLEDemoBoard(bluetoothDevice, rssi, scanRecord);
        }

        return super.createLocalDevice(bluetoothDevice, rssi, scanRecord);
    }
}
