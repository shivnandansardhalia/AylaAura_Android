package com.aylanetworks.aura.localdevice;

/*
 * Aura_Android
 *
 * Copyright 2015 Ayla Networks, all rights reserved
 */


import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.text.TextUtils;

import com.android.volley.Response;
import com.aylanetworks.aylasdk.AylaAPIRequest;
import com.aylanetworks.aylasdk.AylaLog;
import com.aylanetworks.aylasdk.AylaNetworks;
import com.aylanetworks.aylasdk.change.Change;
import com.aylanetworks.aylasdk.change.PropertyChange;
import com.aylanetworks.aylasdk.error.ErrorListener;
import com.aylanetworks.aylasdk.error.PreconditionError;
import com.aylanetworks.aylasdk.localdevice.AylaLocalProperty;
import com.aylanetworks.aylasdk.localdevice.AylaLocalRegistrationCandidate;
import com.aylanetworks.aylasdk.localdevice.ble.AylaBLEDevice;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * The GrillRightDevice class represents an AylaBLEDevice configured to communication with an
 * Oregon Scientific GrillRight Bluetooth Thermometer.
 *
 * The GrillRight thermometer is a product of Oregon Scientific. This software is not in any way
 * supported by Oregon Scientific.
 *
 * https://www.amazon.com/Oregon-Scientific-AW133-Bluetooth-Thermometer/dp/B00JFSW0AQ
 *
 * GrillRight thermometers can be purchased on Amazon as well as other retailers.
 *
 * The device template should be configured with the following properties:
 *
 * - Sensor1Temp            Integer             Sensor 1 temperature in 1/10 degree F
 * - Sensor2Temp            Integer             Sensor 2 temperature in 1/10 degree F
 *
 */
public class GrillRightDevice extends AylaBLEDevice {
    private static final String LOG_TAG = "GrillRightDevice";

    public static final String GRILL_RIGHT_MODEL = "GrillRight";
    public static final String GRILL_RIGHT_OEM = "GrillRight";
    public static final String GRILL_RIGHT_OEM_MODEL = "GrillRight";

    // Property names
    public static final String PROP_SENSOR1_TEMP = "00:grillrt:TEMP";
    public static final String PROP_SENSOR2_TEMP = "01:grillrt:TEMP";
    public static final String PROP_SENSOR1_MEAT = "00:grillrt:MEAT";
    public static final String PROP_SENSOR2_MEAT = "01:grillrt:MEAT";
    public static final String PROP_SENSOR1_DONENESS = "00:grillrt:DONENESS";
    public static final String PROP_SENSOR2_DONENESS = "01:grillrt:DONENESS";
    public static final String PROP_SENSOR1_TARGET_TEMP = "00:grillrt:TARGET_TEMP";
    public static final String PROP_SENSOR2_TARGET_TEMP = "01:grillrt:TARGET_TEMP";
    public static final String PROP_SENSOR1_PCT_DONE = "00:grillrt:PCT_DONE";
    public static final String PROP_SENSOR2_PCT_DONE = "01:grillrt:PCT_DONE";
    public static final String PROP_SENSOR1_COOKING = "00:grillrt:COOKING";
    public static final String PROP_SENSOR2_COOKING = "01:grillrt:COOKING";
    public static final String PROP_SENSOR1_TARGET_TIME = "00:grillrt:TARGET_TIME";
    public static final String PROP_SENSOR2_TARGET_TIME = "01:grillrt:TARGET_TIME";
    public static final String PROP_SENSOR1_TIME = "00:grillrt:TIME";
    public static final String PROP_SENSOR2_TIME = "01:grillrt:TIME";
    public static final String PROP_SENSOR1_CONTROL_MODE = "00:grillrt:CONTROL_MODE";
    public static final String PROP_SENSOR2_CONTROL_MODE = "01:grillrt:CONTROL_MODE";
    public static final String PROP_SENSOR1_ALARM = "00:grillrt:ALARM";
    public static final String PROP_SENSOR2_ALARM = "01:grillrt:ALARM";


    public static final int NO_SENSOR = -1;

    protected BluetoothGattService _grillRightService;
    protected BluetoothGattService _batteryLevelService;

    public static String[] getManagedPropertyNames() {
        return new String[]{PROP_SENSOR1_ALARM, PROP_SENSOR1_CONTROL_MODE, PROP_SENSOR1_COOKING,
                PROP_SENSOR1_DONENESS, PROP_SENSOR1_MEAT, PROP_SENSOR1_PCT_DONE,
                PROP_SENSOR1_TARGET_TEMP, PROP_SENSOR1_TARGET_TIME, PROP_SENSOR1_TEMP,
                PROP_SENSOR1_TIME,
                PROP_SENSOR2_ALARM, PROP_SENSOR2_CONTROL_MODE, PROP_SENSOR2_COOKING,
                PROP_SENSOR2_DONENESS, PROP_SENSOR2_MEAT, PROP_SENSOR2_PCT_DONE,
                PROP_SENSOR2_TARGET_TEMP, PROP_SENSOR2_TARGET_TIME, PROP_SENSOR2_TEMP,
                PROP_SENSOR2_TIME};
    }

    public enum ControlMode {
        None(0),
        Meat(1),
        Temp(2),
        Time(3);

        private int _index;

        ControlMode(int index) {
            _index = index;
        }

        public int getIndex() {
            return _index;
        }

        public static ControlMode fromIndex(int index) {
            for (ControlMode type : values()) {
                if (type._index == index) {
                    return type;
                }
            }
            return None;
        }
    }

    public enum AlarmState {
        None(0),
        AlmostDone(1),
        Overdone(2);

        private int _index;

        AlarmState(int index) {
            _index = index;
        }

        public int getIndex() {
            return _index;
        }

        public static AlarmState fromIndex(int index) {
            for (AlarmState type : values()) {
                if (type._index == index) {
                    return type;
                }
            }
            return None;
        }
    }

    public enum MeatType {
        None(0),
        Beef(1),
        Veal(2),
        Lamb(3),
        Pork(4),
        Chicken(5),
        Turkey(6),
        Fish(7),
        Hamburger(8);

        private int _index;

        MeatType(int index) {
            _index = index;
        }

        public int getIndex() {
            return _index;
        }

        public static MeatType fromIndex(int index) {
            for (MeatType type : values()) {
                if (type._index == index) {
                    return type;
                }
            }
            return None;
        }
    }

    public enum Doneness {
        None(0),
        Rare(1),
        MediumRare(2),
        Medium(3),
        MediumWell(4),
        WellDone(5);

        private int _index;

        Doneness(int index) {
            _index = index;
        }

        public int getIndex() {
            return _index;
        }

        public static Doneness fromIndex(int index) {
            for (Doneness type : values()) {
                if (type._index == index) {
                    return type;
                }
            }
            return None;
        }
    }

    private Sensor _sensor1 = new Sensor("Sensor 1", 1);
    private Sensor _sensor2 = new Sensor("Sensor 2", 2);

    // GrillRight service UUID
    public static UUID SERVICE_GRILL_RIGHT = UUID.fromString
            ("2899FE00-C277-48A8-91CB-B29AB0F01AC4");

    // GrillRight custom UUIDs
    public static final UUID CHARACTERISTIC_ID_CONTROL = UUID.fromString
            ("28998E03-C277-48A8-91CB-B29AB0F01AC4");

    public static final UUID CHARACTERISTIC_ID_SENSOR1 = UUID.fromString
            ("28998E10-C277-48A8-91CB-B29AB0F01AC4");

    public static final UUID CHARACTERISTIC_ID_SENSOR2 = UUID.fromString
            ("28998E11-C277-48A8-91CB-B29AB0F01AC4");

    // Battery Service UUIDs
    private static final UUID SERVICE_BATTERY = UUID.fromString
            ("0000180F-0000-1000-8000-00805f9b34fb");

    private static final UUID CHARACTERISTIC_ID_BATTERY_LEVEL = UUID.fromString
            ("00002a19-0000-1000-8000-00805f9b34fb");

    public GrillRightDevice() {
        super();
    }

    public GrillRightDevice(BluetoothDevice discoveredDevice, int rssi, byte[] scanData) {
        super(discoveredDevice, rssi, scanData);

        // Set our known fields so they will be serialized with JSON
        model = GRILL_RIGHT_MODEL;
        oemModel = GRILL_RIGHT_OEM_MODEL;
        oem = GRILL_RIGHT_OEM;
    }

    @Override
    public String getOemModel() {
        if (TextUtils.isEmpty(oemModel)) {
            oemModel = GRILL_RIGHT_OEM_MODEL;
        }
        return oemModel;
    }

    @Override
    public String getOem() {
        if (TextUtils.isEmpty(oem)) {
            oem = GRILL_RIGHT_OEM;
        }
        return oem;
    }

    @Override
    public String getModel() {
        if (TextUtils.isEmpty(model)) {
            model = GRILL_RIGHT_MODEL;
        }
        return model;
    }

    @Override
    public String getProductName() {
        if (productName == null) {
            productName = "GrillRight Thermometer";
        }
        return productName;
    }

    @Override
    public boolean isPropertyReadOnly(AylaLocalProperty property) {
        if (property == null) {
            return true;
        }

        switch (property.getName()) {
            case PROP_SENSOR1_COOKING:
            case PROP_SENSOR2_COOKING:
            case PROP_SENSOR1_MEAT:
            case PROP_SENSOR2_MEAT:
            case PROP_SENSOR1_TARGET_TEMP:
            case PROP_SENSOR2_TARGET_TEMP:
            case PROP_SENSOR1_TARGET_TIME:
            case PROP_SENSOR2_TARGET_TIME:
                return false;
        }
        return true;
    }

    // We need to assume that T is the type we expect it to be
    @SuppressWarnings("unchecked")
    @Override
    public <T> T getValueForProperty(AylaLocalProperty<T> property) {
        if (!isConnectedLocal()) {
            return property.getOriginalProperty().getValue();
        }

        switch (property.getName()) {
            case PROP_SENSOR1_CONTROL_MODE:
                return (T) (Integer) _sensor1.getControlMode().getIndex();

            case PROP_SENSOR2_CONTROL_MODE:
                return (T) (Integer) _sensor2.getControlMode().getIndex();

            case PROP_SENSOR1_ALARM:
                return (T) (Integer) _sensor1.getAlarmState();

            case PROP_SENSOR2_ALARM:
                return (T) (Integer) _sensor2.getAlarmState();

            case PROP_SENSOR1_TEMP:
                return (T) (Integer) _sensor1.getCurrentTemp();

            case PROP_SENSOR1_MEAT:
                return (T) (Integer) _sensor1.getMeatType().getIndex();

            case PROP_SENSOR2_TEMP:
                return (T) (Integer) _sensor2.getCurrentTemp();

            case PROP_SENSOR2_MEAT:
                return (T) (Integer) _sensor2.getMeatType().getIndex();

            case PROP_SENSOR1_TARGET_TEMP:
                return (T) (Integer) _sensor1.getTargetTemp();

            case PROP_SENSOR2_TARGET_TEMP:
                return (T) (Integer) _sensor2.getTargetTemp();

            case PROP_SENSOR1_PCT_DONE:
                return (T) (Integer) _sensor1.getPercentDone();

            case PROP_SENSOR2_PCT_DONE:
                return (T) (Integer) _sensor2.getPercentDone();

            case PROP_SENSOR1_DONENESS:
                return (T) (Integer) _sensor1.getDoneness().getIndex();

            case PROP_SENSOR2_DONENESS:
                return (T) (Integer) _sensor2.getDoneness().getIndex();

            case PROP_SENSOR1_COOKING:
                return (T) (Integer) (_sensor1.isCooking() ? 1 : 0);

            case PROP_SENSOR2_COOKING:
                return (T) (Integer) (_sensor2.isCooking() ? 1 : 0);

            case PROP_SENSOR1_TARGET_TIME:
                return (T) _sensor1.getTargetTime();

            case PROP_SENSOR2_TARGET_TIME:
                return (T) _sensor2.getTargetTime();

            case PROP_SENSOR1_TIME:
                return (T) _sensor1.getCurrentTime();

            case PROP_SENSOR2_TIME:
                return (T) _sensor2.getCurrentTime();

            default:
                return null;
        }
    }

    @Override
    public <T> AylaAPIRequest<T> setValueForProperty(AylaLocalProperty<T> property,
                                                     final T value,
                                                     final Response.Listener<T> successListener,
                                                     final ErrorListener errorListener) {
        if (!isConnectedLocal()) {
            errorListener.onErrorResponse(new PreconditionError("Properties are read-only unless "
                    + "the device is connected locally."));
            return null;
        }

        byte[] command = null;
        Sensor sensorCopy;
        switch(property.getName()) {
            case PROP_SENSOR1_COOKING:
                if ((Integer)value == 0) {
                    command = Command.stopCookingCommand(1);
                } else {
                    command = Command.startCookingCommand(1, ControlMode.fromIndex((Integer)value));
                }
                break;

            case PROP_SENSOR2_COOKING:
                if (_sensor2.isCooking()) {
                    command = Command.stopCookingCommand(2);
                } else {
                    command = Command.startCookingCommand(2, ControlMode.fromIndex((Integer)value));
                }
                break;

            case PROP_SENSOR1_MEAT:
                sensorCopy = new Sensor(_sensor1);
                sensorCopy.setMeatType(MeatType.fromIndex((Integer)value));
                command = Command.setFields(sensorCopy);
                break;

            case PROP_SENSOR2_MEAT:
                sensorCopy = new Sensor(_sensor2);
                sensorCopy.setMeatType(MeatType.fromIndex((Integer)value));
                command = Command.setFields(sensorCopy);
                break;

            case PROP_SENSOR1_DONENESS:
                sensorCopy = new Sensor(_sensor1);
                sensorCopy.setDoneness(Doneness.fromIndex((Integer)value));
                command = Command.setFields(sensorCopy);
                break;

            case PROP_SENSOR2_DONENESS:
                sensorCopy = new Sensor(_sensor2);
                sensorCopy.setDoneness(Doneness.fromIndex((Integer)value));
                command = Command.setFields(sensorCopy);
                break;

            case PROP_SENSOR1_TARGET_TEMP:
                sensorCopy = new Sensor(_sensor1);
                sensorCopy.setTargetTemp((Integer)value);
                command = Command.setFields(sensorCopy);
                break;

            case PROP_SENSOR2_TARGET_TEMP:
                sensorCopy = new Sensor(_sensor2);
                sensorCopy.setTargetTemp((Integer)value);
                command = Command.setFields(sensorCopy);
                break;

            case PROP_SENSOR1_TARGET_TIME:
                sensorCopy = new Sensor(_sensor1);
                sensorCopy.setTargetTime((String)value);
                command = Command.setFields(sensorCopy);
                break;

            case PROP_SENSOR2_TARGET_TIME:
                sensorCopy = new Sensor(_sensor2);
                sensorCopy.setTargetTime((String)value);
                command = Command.setFields(sensorCopy);
                break;

            default:
                errorListener.onErrorResponse(new PreconditionError("Not a writable property: " +
                        property));
        }

        if (command != null) {
            // Write the command to the control characteristic
            BluetoothGattCharacteristic control = _grillRightService.getCharacteristic
                    (CHARACTERISTIC_ID_CONTROL);
            control.setValue(command);

            //noinspection unchecked
            return writeCharacteristic(control, new Response.Listener<BluetoothGattCharacteristic>() {
                @Override
                public void onResponse(BluetoothGattCharacteristic response) {
                    successListener.onResponse(value);
                }
            }, errorListener);
        }
        return null;
    }

    @Override
    public String getCandidateJson(String oem) {
        AylaLocalRegistrationCandidate rc = new AylaLocalRegistrationCandidate();
        rc.device.unique_hardware_id = getHardwareAddress();
        rc.device.oem_model = getOemModel();
        rc.device.oem = oem;
        rc.device.model = getModel();
        rc.device.sw_version = "1.0";

        // This device is composed of 2 subdevices, one for each probe
        rc.device.subdevices = new AylaLocalRegistrationCandidate.Subdevice[2];
        for (int i = 0; i < 2; i++) {
            rc.device.subdevices[i] = new AylaLocalRegistrationCandidate.Subdevice();
            rc.device.subdevices[i].subdevice_key = String.format(Locale.US, "%02d", i);
            rc.device.subdevices[i].templates = new AylaLocalRegistrationCandidate.Template[1];
            rc.device.subdevices[i].templates[0] = new AylaLocalRegistrationCandidate.Template();
            rc.device.subdevices[i].templates[0].template_key = "grillrt";
            rc.device.subdevices[i].templates[0].version = "1.1";
        }

        String json = AylaNetworks.sharedInstance().getGson().toJson(rc);
        AylaLog.d(LOG_TAG, "Reg candidate JSON:\n" + json);
        return json;
    }

    @Override
    public String toString() {
        return "GrillRight [" + getHardwareAddress() + "]";
    }

    @Override
    protected void fetchCharacteristics() {
        onCharacteristicsFetched();
    }

    @Override
    protected void onCharacteristicsFetched() {
        super.onCharacteristicsFetched();
        AylaLog.d(LOG_TAG, "onCharacteristicsFetched. Going to set up the sensor notifications");
        BluetoothGattCharacteristic characteristic =
                _grillRightService.getCharacteristic(CHARACTERISTIC_ID_CONTROL);
        _bluetoothGatt.setCharacteristicNotification(characteristic, true);
        BluetoothGattDescriptor enableDescriptor = characteristic.getDescriptor
                (CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR);
        enableDescriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
        if (!_bluetoothGatt.writeDescriptor(enableDescriptor)) {
            AylaLog.e(LOG_TAG, "Failed to write enable indication to sensor 1");
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        AylaLog.d(BTCB_TAG, "onServicesDiscovered: " + status);
        List<BluetoothGattService> services = _bluetoothGatt.getServices();
        AylaLog.d(BTCB_TAG, "bluetoothGatt has " + services.size() + " services:");
        for (BluetoothGattService svc : services) {
            AylaLog.d(BTCB_TAG, svc.getUuid().toString());
        }

        _grillRightService = _bluetoothGatt.getService(SERVICE_GRILL_RIGHT);
        _batteryLevelService = _bluetoothGatt.getService(SERVICE_BATTERY);

        if (_batteryLevelService == null) {
            AylaLog.e(BTCB_TAG, "Could not find the battery level GATT service!");
        }

        if (_grillRightService == null) {
            AylaLog.e(BTCB_TAG, "Could not find the GrillRight GATT service!");
        } else {
            AylaLog.d(BTCB_TAG, "Reading device characteristics...");
            fetchCharacteristics();
        }
    }

    @Override
    public String getHardwareAddress() {
        // GrillRight devices don't have a unique hardware ID other than the BD_ADDR. We'll use
        // that.
        return getBluetoothAddress();
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt,
                                     BluetoothGattCharacteristic characteristic,
                                     int status) {
        super.onCharacteristicRead(gatt, characteristic, status);
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt,
                                      BluetoothGattCharacteristic characteristic,
                                      int status) {
        super.onCharacteristicWrite(gatt, characteristic, status);
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt,
                                        BluetoothGattCharacteristic characteristic) {
        Change[] changes = null;
        UUID uuid = characteristic.getUuid();
        String hexString = bytesToHex(characteristic.getValue());
        if (uuid.equals(CHARACTERISTIC_ID_SENSOR1)) {
            AylaLog.d("CHAR", "Sensor 1: " + hexString);
            changes = _sensor1.updateFrom(characteristic.getValue());
            if (changes != null) {
                AylaLog.i(LOG_TAG, "Sensor 1 changed: " + _sensor1 + " change: \n" + changes);
            }
        } else if (uuid.equals(CHARACTERISTIC_ID_SENSOR2)) {
            AylaLog.d("CHAR", "Sensor 2: " + hexString);
            changes = _sensor2.updateFrom(characteristic.getValue());
            if (changes != null) {
                AylaLog.i(LOG_TAG, "Sensor 2 changed: " + _sensor2);
            }
        } else if (uuid.equals(CHARACTERISTIC_ID_CONTROL)) {
            AylaLog.d("CHAR", "Control : " + hexString);
        } else {
            AylaLog.d("CHAR", "Unknown characteristic changed: " + characteristic.getUuid());
        }

        if (changes != null) {
            for (Change change : changes) {
                notifyDeviceChanged(change);
            }
        }
    }

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor,
                                  int status) {
        if (descriptor.getCharacteristic().getUuid().equals(CHARACTERISTIC_ID_CONTROL) &&
                descriptor.getUuid().equals(CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR)) {
            // We just set notifications for the first sensor. Do the same for the 2nd.
            AylaLog.d(LOG_TAG, "onDescriptorWrite for sensor 1 notification: " + status);
            BluetoothGattCharacteristic characteristic = _grillRightService.getCharacteristic
                    (CHARACTERISTIC_ID_SENSOR1);
            BluetoothGattDescriptor descriptor2 = characteristic.getDescriptor
                    (CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR);

            descriptor2.setValue(descriptor.getValue());
            _bluetoothGatt.setCharacteristicNotification(characteristic, true);
            _bluetoothGatt.writeDescriptor(descriptor2);
        } else if (descriptor.getCharacteristic().getUuid().equals(CHARACTERISTIC_ID_SENSOR1) &&
                descriptor.getUuid().equals(CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR)) {
            // We just set notifications for the first sensor. Do the same for the 2nd.
            AylaLog.d(LOG_TAG, "onDescriptorWrite for sensor 1 notification: " + status);
            BluetoothGattCharacteristic characteristic = _grillRightService.getCharacteristic
                    (CHARACTERISTIC_ID_SENSOR2);
            BluetoothGattDescriptor descriptor2 = characteristic.getDescriptor
                    (CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR);

            descriptor2.setValue(descriptor.getValue());
            _bluetoothGatt.setCharacteristicNotification(characteristic, true);
            _bluetoothGatt.writeDescriptor(descriptor2);
        } else if (descriptor.getCharacteristic().getUuid().equals
                (CHARACTERISTIC_ID_SENSOR2) &&
                descriptor.getUuid().equals(CHARACTERISTIC_UPDATE_NOTIFICATION_DESCRIPTOR)) {
            AylaLog.d(LOG_TAG, "onDescriptorWrite for sensor 2 notification: " + status);
        } else {
            super.onDescriptorWrite(gatt, descriptor, status);
        }
    }

    /**
     * Class representing each of the sensors on the GrillRight. There are two separate services,
     * one for each sensor. The _currentValue field holds the current characteristic value for
     * the sensor. The remaining fields are derived from the current value field.
     */
    public class Sensor {
        private byte[] _currentValue;

        private int _currentTemp;
        private MeatType _meatType = MeatType.None;
        private Doneness _doneness = Doneness.None;
        private ControlMode _controlMode = ControlMode.None;
        private int _alarmState = 0;

        // Target timer values
        private int _targetHours;
        private int _targetMinutes;
        private int _targetSeconds;

        // Current timer values
        private int _currentHours;
        private int _currentMinutes;
        private int _currentSeconds;

        private int _targetTemp = 0;
        private int _pctDone = 0;
        private String _name;
        private int _index;
        private boolean _isCooking;

        public Sensor(Sensor other) {
            _name = other._name;
            _index = other._index;
            updateFrom(other._currentValue);
        }

        public Sensor(String name, int index) {
            _name = name;
            _index = index;
        }

        public int getIndex() {
            return _index;
        }

        public void setCurrentTemp(int currentTemp) {
            this._currentTemp = currentTemp;
        }

        public void setMeatType(MeatType meatType) {
            this._meatType = meatType;
        }

        public void setDoneness(Doneness doneness) {
            this._doneness = doneness;
        }

        public void setTargetTime(String time) {
            String[] fields = time.split(":");
            if (fields.length != 3) {
                return;
            }
            setTargetHours(Integer.parseInt(fields[0]));
            setTargetMinutes(Integer.parseInt(fields[1]));
            setTargetSeconds(Integer.parseInt(fields[2]));
        }

        public void setTargetHours(int targetHours) {
            this._targetHours = targetHours;
        }

        public void setTargetMinutes(int targetMinutes) {
            this._targetMinutes = targetMinutes;
        }

        public void setTargetSeconds(int targetSeconds) {
            this._targetSeconds = targetSeconds;
        }

        public void setTargetTemp(int targetTemp) {
            this._targetTemp = targetTemp;
        }

        /**
         * Updates the sensor from the characteristic byte array value
         * @param characteristicValue Array of bytes for the characteristic
         * @return an array of  PropertyChange objects, or null if nothing changed
         */
        public Change[] updateFrom(byte[] characteristicValue) {
            // For property changes
            List<Change> changes = new ArrayList<>();

            // Current probe temperature

            int loByte = characteristicValue[12] & 0xFF;
            int hiByte = characteristicValue[13] & 0xFF;
            int temp = (loByte | (hiByte << 8));

            // 0x8FFF == no sensor
            if (loByte == 0xFF && hiByte == 0x8F) {
                temp = NO_SENSOR;
            }
            if (temp !=  _currentTemp) {
                _currentTemp = temp;
                String propName = _index == 1 ? PROP_SENSOR1_TEMP : PROP_SENSOR2_TEMP;
                changes.add(new PropertyChange(propName));

                // Reflect the change on the cloud
                AylaLocalProperty<Integer> prop = (AylaLocalProperty<Integer>)getProperty(propName);
                if (prop != null) {
                    prop.pushUpdateToCloud();
                }
            }

            int lowNybble = (characteristicValue[0] & 0x0F);
            int alarmState = (lowNybble == 0x0B ? AlarmState.AlmostDone.getIndex() :
                    (lowNybble == 0x0F) ? AlarmState.Overdone.getIndex() : 0);
            if (alarmState != _alarmState) {
                _alarmState = alarmState;
                String propName = _index == 1 ? PROP_SENSOR1_ALARM : PROP_SENSOR2_ALARM;
                AylaLog.d(LOG_TAG, _name + ": Alarm state: " + _alarmState);
                changes.add(new PropertyChange(propName));
                // Reflect the change on the cloud
                AylaLocalProperty<Integer> prop = (AylaLocalProperty<Integer>)getProperty(propName);
                if (prop != null) {
                    prop.pushUpdateToCloud();
                }
            }

            // Is cooking?
            boolean isCooking = (lowNybble & 0x04) == 0x04;

            // Always cooking if the alarm is set
            if (_alarmState != AlarmState.None.getIndex()) {
                isCooking = true;
            }

            if (isCooking != _isCooking) {
                _isCooking = isCooking;
                String propName = _index == 1 ? PROP_SENSOR1_COOKING : PROP_SENSOR2_COOKING;
                AylaLog.d(LOG_TAG, _name + ": Cooking changed to " + _isCooking);
                changes.add(new PropertyChange(propName));
                // Reflect the change on the cloud
                AylaLocalProperty<Integer> prop = (AylaLocalProperty<Integer>)getProperty(propName);
                if (prop != null) {
                    prop.pushUpdateToCloud();
                }
            }

            // Control mode, only if no alarm is set
            ControlMode mode = _controlMode;
            if (alarmState == 0) {
                mode = ControlMode.fromIndex(lowNybble & 0x03);
            }

            if (_controlMode != mode) {
                String propName = _index == 1 ? PROP_SENSOR1_CONTROL_MODE :
                        PROP_SENSOR2_CONTROL_MODE;
                _controlMode = mode;
                AylaLog.d(LOG_TAG, "Control mode changed to " + _controlMode);
                changes.add(new PropertyChange(propName));
                // Reflect the change on the cloud
                AylaLocalProperty<Integer> prop = (AylaLocalProperty<Integer>)getProperty(propName);
                if (prop != null) {
                    prop.pushUpdateToCloud();
                }
            }

            // Target temperature

            loByte = characteristicValue[10] & 0xFF;
            hiByte = characteristicValue[11] & 0xFF;
            temp = (loByte | (hiByte << 8));
            if (temp != _targetTemp) {
                _targetTemp = temp;
                String propName = _index == 1 ? PROP_SENSOR1_TARGET_TEMP : PROP_SENSOR2_TARGET_TEMP;
                changes.add(new PropertyChange(propName));
                // Reflect the change on the cloud
                AylaLocalProperty<Integer> prop = (AylaLocalProperty<Integer>)getProperty(propName);
                if (prop != null) {
                    prop.pushUpdateToCloud();
                }
            }

            int meat = characteristicValue[1];
            if (meat != _meatType.getIndex()) {
                _meatType = MeatType.fromIndex(meat);
                AylaLog.d(LOG_TAG, _name + ": Meat type changed to " + _meatType);
                String propName = _index == 1 ? PROP_SENSOR1_MEAT : PROP_SENSOR2_MEAT;
                changes.add(new PropertyChange(propName));
                // Reflect the change on the cloud
                AylaLocalProperty<Integer> prop = (AylaLocalProperty<Integer>)getProperty(propName);
                if (prop != null) {
                    prop.pushUpdateToCloud();
                }
            }

            int doneness = characteristicValue[2];
            if (doneness != _doneness.getIndex()) {
                _doneness = Doneness.fromIndex(doneness);
                AylaLog.d(LOG_TAG, _name + ": Doneness changed to " + _doneness);
                String propName = _index == 1 ? PROP_SENSOR1_DONENESS : PROP_SENSOR2_DONENESS;
                changes.add(new PropertyChange(propName));
                // Reflect the change on the cloud
                AylaLocalProperty<Integer> prop = (AylaLocalProperty<Integer>)getProperty(propName);
                if (prop != null) {
                    prop.pushUpdateToCloud();
                }
            }

            // 3, 4, 5 are H:M:S for the timer
            boolean targetTimeChanged = false;
            int targetHours = characteristicValue[3];
            if (targetHours != _targetHours) {
                _targetHours = targetHours;
                targetTimeChanged = true;
            }

            int targetMinutes = characteristicValue[4];
            if (targetMinutes != _targetMinutes) {
                _targetMinutes = targetMinutes;
                targetTimeChanged = true;
            }
            int targetSeconds = characteristicValue[5];
            if (targetSeconds != _targetSeconds) {
                _targetSeconds = targetSeconds;
                targetTimeChanged = true;
            }

            if (targetTimeChanged) {
                String propName = _index == 1 ? PROP_SENSOR1_TARGET_TIME : PROP_SENSOR2_TARGET_TIME;
                changes.add(new PropertyChange(propName));
                // Reflect the change on the cloud
                AylaLocalProperty<Integer> prop = (AylaLocalProperty<Integer>)getProperty(propName);
                if (prop != null) {
                    prop.pushUpdateToCloud();
                }
            }

            // 6, 7, 8 are H:M:S for the current time on the timer
            boolean currentTimeChanged = false;
            int currentHours = characteristicValue[6];
            if (currentHours != _currentHours) {
                _currentHours = currentHours;
                currentTimeChanged = true;
            }

            int currentMinutes = characteristicValue[7];
            if (currentMinutes != _currentMinutes) {
                _currentMinutes = currentMinutes;
                currentTimeChanged = true;
            }
            int currentSeconds = characteristicValue[8];
            if (currentSeconds != _currentSeconds) {
                _currentSeconds = currentSeconds;
                currentTimeChanged = true;
            }

            if (currentTimeChanged) {
                String propName = _index == 1 ? PROP_SENSOR1_TIME : PROP_SENSOR2_TIME;
                changes.add(new PropertyChange(propName));
                // Reflect the change on the cloud
                AylaLocalProperty<Integer> prop = (AylaLocalProperty<Integer>)getProperty(propName);
                if (prop != null) {
                    prop.pushUpdateToCloud();
                }
            }

            // Percent done
            loByte = characteristicValue[14] & 0xFF;
            hiByte = characteristicValue[15] & 0xFF;
            int pctDone = (loByte | (hiByte << 8));
            if (pctDone != _pctDone) {
                _pctDone = pctDone;
                String propName = _index == 1 ? PROP_SENSOR1_PCT_DONE : PROP_SENSOR2_PCT_DONE;
                changes.add(new PropertyChange(propName));
                // Reflect the change on the cloud
                AylaLocalProperty<Integer> prop = (AylaLocalProperty<Integer>)getProperty(propName);
                if (prop != null) {
                    prop.pushUpdateToCloud();
                }
            }

            _currentValue = characteristicValue;

            if (changes.size() > 0) {
                return changes.toArray(new Change[changes.size()]);
            }

            return null;
        }

        public ControlMode getControlMode() {
            return _controlMode;
        }

        public void setControlMode(ControlMode mode) {
            _controlMode = mode;
        }

        /**
         * Returns the current temperature in 10ths of a degree F
         * @return Current temp in 1/10 degree F
         */
        public int getCurrentTemp() {
            return _currentTemp;
        }

        public int getTargetTemp() {
            return _targetTemp;
        }

        public MeatType getMeatType() {
            return _meatType;
        }

        public int getPercentDone() {
            return _pctDone;
        }

        public Doneness getDoneness() {
            return _doneness;
        }

        public boolean isCooking() {
            return _isCooking;
        }

        public String getTargetTime() {
            return String.format(Locale.US, "%d:%02d:%02d", _targetHours, _targetMinutes,
                    _targetSeconds);
        }

        public String getCurrentTime() {
            return String.format(Locale.US, "%d:%02d:%02d", _currentHours, _currentMinutes,
                    _currentSeconds);
        }

        public String toString() {
            String tempString;
            int temp = getCurrentTemp();
            if (temp == NO_SENSOR) {
                tempString = "No Sensor";
            } else {
                tempString = ""+(temp / 10f);
            }
            return _name + " temp: " + tempString;
        }

        public int getAlarmState() {
            return _alarmState;
        }
    }

    public static class Command {
        public static byte[] startCookingCommand(int index, ControlMode mode) {
            return new byte[] {(byte)0x83, (byte)index, (byte)mode.getIndex()};
        }

        public static byte[] stopCookingCommand(int index) {
            return new byte[] {(byte)0x84, (byte)index, (byte)0x00};
        }

        public static byte[] setFields(Sensor sensor) {
            byte command[] = new byte[13];
            command[0] = (byte)0x82;        // Set values command ID?
            command[1] = (byte)sensor.getIndex();
            command[2] = (byte)sensor.getMeatType().getIndex();
            command[3] = (byte)sensor.getDoneness().getIndex();
            int temp = sensor.getTargetTemp();
            command[4] = (byte)(temp & 0xFF);
            command[5] = (byte)((temp & 0xFF00) >> 8);
            String timeString = sensor.getTargetTime();
            String hms[] = timeString.split(":");
            command[6] = Byte.parseByte(hms[0]);
            command[7] = Byte.parseByte(hms[1]);
            command[8] = Byte.parseByte(hms[2]);

            // TODO: Are these different than the previous 3?
            command[9] = Byte.parseByte(hms[0]);
            command[10] = Byte.parseByte(hms[1]);
            command[11] = Byte.parseByte(hms[2]);

            return command;
        }
    }
}
