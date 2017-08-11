package com.aylanetworks.aura.test;
/*
 * Aura_Android
 *
 * Copyright 2015 Ayla Networks, all rights reserved
 */

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.aylanetworks.aura.MainActivity;
import com.aylanetworks.aylasdk.AylaAPIRequest;
import com.aylanetworks.aylasdk.AylaDevice;
import com.aylanetworks.aylasdk.AylaDeviceManager;
import com.aylanetworks.aylasdk.AylaNetworks;
import com.aylanetworks.aylasdk.error.AylaError;
import com.aylanetworks.aylasdk.error.RequestFuture;
import com.aylanetworks.aylasdk.setup.AylaRegistration;
import com.aylanetworks.aylasdk.setup.AylaRegistrationCandidate;
import com.aylanetworks.aylasdk.setup.AylaSetup;
import com.aylanetworks.aylasdk.setup.AylaSetupDevice;
import com.aylanetworks.aylasdk.setup.AylaWifiStatus;
import com.aylanetworks.aylasdk.util.ObjectUtils;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class SetupAndRegistrationTestCase extends TestCase {

    private static final String KEY_WIFI_SSID = "setup_reg_test_ssid";
    private static final String KEY_WIFI_PASSWORD = "setup_reg_test_password";

    private enum RegistrationResult {SUCCESS, CANDIDATE_NOT_FOUND, REGISTRATION_FAILED}

    private static String _ssid;
    private static String _password;
    private static String _deviceSsid;
    private static String _deviceDsn;

    private Context _context;
    private CountDownLatch mLatch;

    private BroadcastReceiver _wifiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            List<ScanResult> results = wifiManager.getScanResults();

            for (ScanResult result : results) {
                if (TextUtils.equals(result.SSID, _deviceSsid)) {
                    context.unregisterReceiver(this);
                    mLatch.countDown();
                    return;
                }
            }

            wifiManager.startScan();
        }
    };

    public SetupAndRegistrationTestCase(Context context) {
        super("Setup and Registration Test");
        _context = context;
    }

    @Override
    public View getConfigView() {
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(_context);

        LinearLayout root = new LinearLayout(_context);
        root.setOrientation(LinearLayout.VERTICAL);

        EditText ssidInput = new EditText(_context);
        ssidInput.setInputType(InputType.TYPE_CLASS_TEXT);
        ssidInput.setHint("SSID");
        ssidInput.setContentDescription("SSID");
        _ssid = sharedPreferences.getString(KEY_WIFI_SSID, "");
        ssidInput.setText(_ssid);
        ssidInput.setSelection(_ssid.length());
        ssidInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                _ssid = editable.toString();
                sharedPreferences.edit().putString(KEY_WIFI_SSID, _ssid).apply();
            }
        });
        root.addView(ssidInput);

        EditText passwordInput = new EditText(_context);
        passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        passwordInput.setHint("Password");
        _password = sharedPreferences.getString(KEY_WIFI_PASSWORD, "");
        passwordInput.setText(_password);
        passwordInput.setSelection(_password.length());
        passwordInput.setContentDescription("PASSWORD");
        passwordInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                _password = editable.toString();
                sharedPreferences.edit().putString(KEY_WIFI_PASSWORD, _password).apply();
            }
        });
        root.addView(passwordInput);

        return root;
    }

    @Override
    public void run(final TestSuite suite) {
        super.run(suite);

        AylaDeviceManager deviceManager = MainActivity.getDeviceManager();
        AylaDevice testingDevice = suite.getDeviceUnderTest();
        if (suite.getCurrentIteration() > 1) {
            suite.logMessage(LogEntry.LogType.Info, "Waiting for 5 seconds before restarting test");
            try {
                Thread.sleep(5 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // refresh the AylaDevice instance by getting
        // one from AylaDeviceManager with the provided DSN
        testingDevice = deviceManager.deviceWithDSN(testingDevice.getDsn());
        if (testingDevice == null) {
            suite.logMessage(LogEntry.LogType.Warning, "Device doesn't exist; Attempting device registration");

            // first, attempt device registration in case it's already connected to the wifi network
            // but not registered
            RegistrationResult regResult = startDeviceRegistration(suite, _deviceDsn, true);
            if (regResult == RegistrationResult.SUCCESS) {
                pass();
                return;
            } else if (regResult == RegistrationResult.REGISTRATION_FAILED) {
                fail();
                suite.logMessage(LogEntry.LogType.Error, "Device candidate found but registration failed");
                suite.logMessage(LogEntry.LogType.Warning, "You may run the test again to attempt device rescue");
                return;
            }
        } else {
            if (!testingDevice.isLanModeActive()) {
                suite.logMessage(LogEntry.LogType.Error, "Device is not in LAN mode");
                fail();
                return;
            }

            // delete current wifi profile from device
            if (!deleteWifiProfile(suite, testingDevice)) {
                suite.logMessage(LogEntry.LogType.Warning, "You may run the test again to attempt device rescue");
                return;
            }

            // unregister device from Ayla service
            if (!unregisterDevice(suite, testingDevice)) {
                suite.logMessage(LogEntry.LogType.Warning, "You may run the test again to attempt device rescue");
                return;
            }

            _deviceSsid = "Ayla-" + testingDevice.getMac();
            _deviceDsn = testingDevice.getDsn();
        }

        // start receiver to listen for the device AP as it should switch to AP mode after
        // wifi profile deletion, block until it's found
        _context.registerReceiver(_wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        WifiManager wifiManager = (WifiManager) _context.getSystemService(Context.WIFI_SERVICE);
        wifiManager.startScan();

        suite.logMessage(LogEntry.LogType.Message, "Waiting for device to switch to AP mode");

        mLatch = new CountDownLatch(1);
        try {
            mLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (!startDeviceSetup(suite, _deviceDsn)) {
            suite.logMessage(LogEntry.LogType.Warning, "You may run the test again to attempt device rescue");
            return;
        }

        if (startDeviceRegistration(suite, _deviceDsn, false) != RegistrationResult.SUCCESS) {
            suite.logMessage(LogEntry.LogType.Warning, "You may run the test again to attempt device rescue");
            return;
        }

        pass();
    }

    private RegistrationResult startDeviceRegistration(TestSuite suite, String dsn, boolean rescue) {
        AylaRegistration registration = AylaNetworks.sharedInstance().getSessionManager
                (MainActivity.SESSION_NAME).getDeviceManager().getAylaRegistration();

        // fetch device candidate on the same LAN
        AylaRegistrationCandidate candidate = fetchCandidate(suite, dsn, registration, rescue);
        if (candidate == null) {
            if (rescue) {
                suite.logMessage(LogEntry.LogType.Warning, "Device is not connected to the wifi network, attempting wifi setup");
            }
            return RegistrationResult.CANDIDATE_NOT_FOUND;
        }

        // register device candidate
        if (!registerCandidate(suite, candidate, registration)) {
            return RegistrationResult.REGISTRATION_FAILED;
        }

        return RegistrationResult.SUCCESS;
    }

    private boolean startDeviceSetup(TestSuite suite, String dsn) {
        // Device should switch to AP mode
        AylaSetup setup;
        try {
            setup = new AylaSetup(_context, MainActivity.getSession());
        } catch (AylaError e) {
            fail();
            suite.logMessage(LogEntry.LogType.Error, "Failed to create setup object: "
                    + e.getMessage());
            return false;
        }

        // connect to device in AP mode
        if (!connectToDevice(suite, setup)) {
            exitSetup(suite, setup);
            return false;
        }

        // device scan for wifi networks
        if (!orderWifiScan(suite, setup)) {
            exitSetup(suite, setup);
            return false;
        }

        // wait for device to be ready
        suite.logMessage(LogEntry.LogType.Info, "Waiting 5 seconds for device AP scan");
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // connect device to wifi network
        String setupToken = ObjectUtils.generateRandomToken(8);

        // don't fail on exception because of device bugs and poor wifi connection reliability
        // instead, ignore the result and check with ayla service for the device status
        connectDeviceToAp(suite, setup, 10, setupToken);

        // confirm that the device is connected to the service
        if (!confirmConnection(suite, setup, dsn, setupToken)) {
            exitSetup(suite, setup);
            return false;
        }

        exitSetup(suite, setup);

        return true;
    }

    /* delete the current wifi profile from device */
    private boolean deleteWifiProfile(TestSuite suite, AylaDevice testingDevice) {
        suite.logMessage(LogEntry.LogType.Message, "Deleting WiFi profile from device");
        RequestFuture<AylaAPIRequest.EmptyResponse> emptyFuture = RequestFuture.newFuture();

        testingDevice.deleteWifiProfile(_ssid, emptyFuture, emptyFuture);
        try {
            emptyFuture.get(5000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            fail();
            suite.logMessage(LogEntry.LogType.Error, "Interrupted");
            return false;
        } catch (ExecutionException e) {
            fail();
            suite.logMessage(LogEntry.LogType.Error, "Error " + e.getMessage());
            e.printStackTrace();
            return false;
        } catch (TimeoutException e) {
            fail();
            suite.logMessage(LogEntry.LogType.Error, "Timed out waiting for response");
            return false;
        }

        suite.logMessage(LogEntry.LogType.Message,
                "Successfully deleted WiFi profile from device");

        // Check to see if we're stopped
        if (!suite.isRunning()) {
            _testStatus = TestStatus.Stopped;
            suite.logMessage(LogEntry.LogType.Info, "Test suite was stopped");
            return false;
        }

        return true;
    }

    /* Unregister the device from Ayla service */
    private boolean unregisterDevice(TestSuite suite, AylaDevice testingDevice) {
        suite.logMessage(LogEntry.LogType.Message, "Unregistering device");

        RequestFuture<AylaAPIRequest.EmptyResponse> unregisterFuture = RequestFuture.newFuture();
        testingDevice.unregister(
                unregisterFuture,
                unregisterFuture);
        try {
            unregisterFuture.get(10 * 1000,
                    TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            fail();
            suite.logMessage(LogEntry.LogType.Error, "Interrupted");
            return false;
        } catch (ExecutionException e) {
            fail();
            suite.logMessage(LogEntry.LogType.Error, "Error " + e.getMessage());
            return false;
        } catch (TimeoutException e) {
            fail();
            suite.logMessage(LogEntry.LogType.Error, "Timed out waiting for response");
            return false;
        }

        suite.logMessage(LogEntry.LogType.Message,
                "Successfully unregistered device");

        // Check to see if we're stopped
        if (!suite.isRunning()) {
            _testStatus = TestStatus.Stopped;
            suite.logMessage(LogEntry.LogType.Info, "Test suite was stopped");
            return false;
        }

        return true;
    }

    /* Connect to the device in AP mode */
    private boolean connectToDevice(TestSuite suite, AylaSetup setup) {
        suite.logMessage(LogEntry.LogType.Message,
                "Reconnecting to device in AP mode " + _deviceSsid);

        RequestFuture<AylaSetupDevice> setupFuture = RequestFuture.newFuture();
        setup.connectToNewDevice(_deviceSsid, 15,
                setupFuture, setupFuture);
        AylaSetupDevice setupDevice;
        try {
            setupDevice = setupFuture.get(15 * 1000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            fail();
            suite.logMessage(LogEntry.LogType.Error, "Interrupted");
            return false;
        } catch (ExecutionException e) {
            fail();
            suite.logMessage(LogEntry.LogType.Error, "Error " + e.getMessage());
            e.printStackTrace();
            return false;
        } catch (TimeoutException e) {
            fail();
            suite.logMessage(LogEntry.LogType.Error, "Timed out waiting for response");
            return false;
        }

        if (setupDevice == null) {
            fail();
            suite.logMessage(LogEntry.LogType.Error, "Failed to reconnect to device");
            return false;
        }

        suite.logMessage(LogEntry.LogType.Message,
                "Successfully reconnected to device in AP mode");

        // Check to see if we're stopped
        if (!suite.isRunning()) {
            _testStatus = TestStatus.Stopped;
            suite.logMessage(LogEntry.LogType.Info, "Test suite was stopped");
            return false;
        }

        return true;
    }

    /* Order device to scan for wifi networks */
    private boolean orderWifiScan(TestSuite suite, AylaSetup setup) {
        suite.logMessage(LogEntry.LogType.Message,
                "Ordering device to scan for WiFi networks");

        RequestFuture<AylaAPIRequest.EmptyResponse> scanFuture = RequestFuture.newFuture();
        setup.startDeviceScanForAccessPoints(
                scanFuture, scanFuture);
        try {
            scanFuture.get(10 * 1000,
                    TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            fail();
            suite.logMessage(LogEntry.LogType.Error, "Interrupted");
            return false;
        } catch (ExecutionException e) {
            fail();
            suite.logMessage(LogEntry.LogType.Error, "Error " + e.getMessage());
            e.printStackTrace();
            return false;
        } catch (TimeoutException e) {
            fail();
            suite.logMessage(LogEntry.LogType.Error, "Timed out waiting for response");
            return false;
        }

        suite.logMessage(LogEntry.LogType.Message,
                "Successfully ordered device to scan for WiFi networks");

        // Check to see if we're stopped
        if (!suite.isRunning()) {
            _testStatus = TestStatus.Stopped;
            suite.logMessage(LogEntry.LogType.Info, "Test suite was stopped");
            return false;
        }

        return true;
    }

    /* Connect the device to the wifi network */
    private boolean connectDeviceToAp(TestSuite suite, AylaSetup setup, int timeoutSec,
                                      String setupToken) {
        suite.logMessage(LogEntry.LogType.Message,
                "Connecting device to Wifi network " + _ssid + " with timeout " + timeoutSec + "s");

        RequestFuture<AylaWifiStatus> reconnectFuture = RequestFuture.newFuture();
        setup.connectDeviceToService(_ssid, _password,
                setupToken,
                null, null,
                timeoutSec,
                reconnectFuture,
                reconnectFuture);
        AylaWifiStatus status;
        try {
            status = reconnectFuture.get(timeoutSec * 1000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            suite.logMessage(LogEntry.LogType.Warning, "Interrupted");
            return false;
        } catch (ExecutionException e) {
            suite.logMessage(LogEntry.LogType.Warning, "Error " + e.getMessage());
            e.printStackTrace();
            //return false;
        } catch (TimeoutException e) {
            suite.logMessage(LogEntry.LogType.Warning, "Timed out waiting for response");
            //return false;
        }

        /*if (status == null) {
            suite.logMessage(LogEntry.LogType.Warning, "Failed to connect device to wifi network");
            return false;
        }*/

        // NOTE: Not failing test suite on exception because of device and wifi bugs

        suite.logMessage(LogEntry.LogType.Message,
                "Successfully connected device to wifi network");

        return true;
    }

    /* Confirm device to Ayla service connection*/
    private boolean confirmConnection(TestSuite suite, AylaSetup setup, String dsn,
                                      String setupToken) {
        suite.logMessage(LogEntry.LogType.Message,
                "Checking service connection");

        // Part 1: Reconnect mobile to original wifi network
        RequestFuture<AylaAPIRequest.EmptyResponse> emptyFuture = RequestFuture.newFuture();
        setup.reconnectToOriginalNetwork(10,
                emptyFuture, emptyFuture);
        try {
            emptyFuture.get(10 * 1000,
                    TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            fail();
            suite.logMessage(LogEntry.LogType.Error, "Interrupted");
            return false;
        } catch (ExecutionException e) {
            fail();
            suite.logMessage(LogEntry.LogType.Error, "Error " + e.getMessage());
            e.printStackTrace();
            return false;
        } catch (TimeoutException e) {
            fail();
            suite.logMessage(LogEntry.LogType.Error, "Timed out waiting for response");
            return false;
        }

        // Part 2: Check device connection to Ayla service
        RequestFuture<AylaSetupDevice> confirmFuture = RequestFuture.newFuture();
        setup.confirmDeviceConnected(10, dsn, setupToken,
                confirmFuture, confirmFuture);
        AylaSetupDevice device;
        try {
            device = confirmFuture.get(10 * 1000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            fail();
            suite.logMessage(LogEntry.LogType.Error, "Interrupted");
            return false;
        } catch (ExecutionException e) {
            fail();
            suite.logMessage(LogEntry.LogType.Error, "Error " + e.getMessage());
            e.printStackTrace();
            return false;
        } catch (TimeoutException e) {
            fail();
            suite.logMessage(LogEntry.LogType.Error, "Timed out waiting for response");
            return false;
        }

        if (device == null) {
            fail();
            suite.logMessage(LogEntry.LogType.Error, "Device to Ayla service connection failed");
            return false;
        }

        suite.logMessage(LogEntry.LogType.Message,
                "Device to Ayla service connection confirmed");

        // Check to see if we're stopped
        if (!suite.isRunning()) {
            _testStatus = TestStatus.Stopped;
            suite.logMessage(LogEntry.LogType.Info, "Test suite was stopped");
            return false;
        }

        return true;
    }

    private void exitSetup(TestSuite suite, AylaSetup setup) {
        RequestFuture<AylaAPIRequest.EmptyResponse> exitFuture = RequestFuture.newFuture();
        setup.exitSetup(exitFuture, exitFuture);
        try {
            exitFuture.get(10 * 1000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            suite.logMessage(LogEntry.LogType.Error, "Interrupted");
        } catch (ExecutionException e) {
            suite.logMessage(LogEntry.LogType.Error, "Error " + e.getMessage());
            e.printStackTrace();
        } catch (TimeoutException e) {
            suite.logMessage(LogEntry.LogType.Error, "Timed out waiting for response");
        }
    }

    /* Fetch device registration candidate */
    private AylaRegistrationCandidate fetchCandidate(TestSuite suite, String dsn, AylaRegistration registration, boolean rescue) {
        suite.logMessage(LogEntry.LogType.Message, "Fetching device candidate");

        AylaRegistrationCandidate candidate;
        AylaDevice.RegistrationType regType = AylaDevice.RegistrationType.SameLan;
        RequestFuture<AylaRegistrationCandidate> candidateFuture = RequestFuture.newFuture();
        registration.fetchCandidate(rescue ? null : dsn, regType,
                candidateFuture,
                candidateFuture);
        try {
            candidate = candidateFuture.get(10 * 1000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            if (!rescue) {
                fail();
                suite.logMessage(LogEntry.LogType.Error, "Interrupted");
            }
            return null;
        } catch (ExecutionException e) {
            if (!rescue) {
                fail();
                suite.logMessage(LogEntry.LogType.Error, "Error " + e.getMessage());
                e.printStackTrace();
            }
            return null;
        } catch (TimeoutException e) {
            if (!rescue) {
                fail();
                suite.logMessage(LogEntry.LogType.Error, "Timed out waiting for response");
            }
            return null;
        }

        if (candidate == null || !TextUtils.equals(candidate.getDsn(), _deviceDsn)) {
            if (!rescue) {
                fail();
                suite.logMessage(LogEntry.LogType.Error, "Failed to fetch device candidate");
            }
            return null;
        }

        suite.logMessage(LogEntry.LogType.Message,
                "Successfully fetched device candidate");

        // Check to see if we're stopped
        if (!suite.isRunning()) {
            _testStatus = TestStatus.Stopped;
            suite.logMessage(LogEntry.LogType.Info, "Test suite was stopped");
            return null;
        }

        return candidate;
    }

    /* Register the device candidate */
    private boolean registerCandidate(TestSuite suite, AylaRegistrationCandidate candidate, AylaRegistration registration) {
        suite.logMessage(LogEntry.LogType.Message,
                "Registering device candidate");

        candidate.setRegistrationType(AylaDevice.RegistrationType.SameLan);

        RequestFuture<AylaDevice> registrationFuture = RequestFuture.newFuture();
        registration.registerCandidate(candidate,
                registrationFuture,
                registrationFuture);
        AylaDevice registeredDevice;
        try {
            registeredDevice = registrationFuture.get(10 * 1000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            fail();
            suite.logMessage(LogEntry.LogType.Error, "Interrupted");
            return false;
        } catch (ExecutionException e) {
            fail();
            suite.logMessage(LogEntry.LogType.Error, "Error " + e.getMessage());
            e.printStackTrace();
            return false;
        } catch (TimeoutException e) {
            fail();
            suite.logMessage(LogEntry.LogType.Error, "Timed out waiting for response");
            return false;
        }

        if (registeredDevice == null) {
            fail();
            suite.logMessage(LogEntry.LogType.Error, "Failed to register device candidate");
            return false;
        }

        suite.logMessage(LogEntry.LogType.Message,
                "Successfully registered device candidate");

        return true;
    }
}
