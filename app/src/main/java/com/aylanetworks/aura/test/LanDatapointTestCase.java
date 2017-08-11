package com.aylanetworks.aura.test;
/*
 * Aura_Android
 *
 * Copyright 2015 Ayla Networks, all rights reserved
 */

import android.text.TextUtils;

import com.aylanetworks.aylasdk.AylaDatapoint;
import com.aylanetworks.aylasdk.AylaDevice;
import com.aylanetworks.aylasdk.AylaProperty;
import com.aylanetworks.aylasdk.error.RequestFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class LanDatapointTestCase extends TestCase {
    public LanDatapointTestCase() {
        super("LAN Datapoint Test");
    }

    public void run(TestSuite suite) {
        super.run(suite);

        AylaDevice device = suite.getDeviceUnderTest();
        if (!device.isLanModeActive()) {
            suite.logMessage(LogEntry.LogType.Error, "Device is not in LAN mode");
            fail();
            return;
        }

        AylaProperty<Integer> blueLED = device.getProperty("Blue_LED");
        AylaProperty<Integer> greenLED = device.getProperty("Green_LED");

        if (blueLED == null || greenLED == null) {
            suite.logMessage(LogEntry.LogType.Error, "Cannot find blue / green LED properties");
            fail();
            return;
        }

        // Check to see if we're stopped
        if (!suite.isRunning()) {
            _testStatus = TestStatus.Stopped;
            suite.logMessage(LogEntry.LogType.Info, "Test suite was stopped");
            return;
        }

        // This test turns on both LEDs and then turns them both off.

        // First turn on the blue LED
        suite.logMessage(LogEntry.LogType.Message, "Turning blue LED ON");
        RequestFuture<AylaDatapoint<Integer>> future = RequestFuture.newFuture();
        blueLED.createDatapointLAN(1, null, future, future);
        AylaDatapoint dp;
        try {
            dp = future.get(3000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            fail();
            suite.logMessage(LogEntry.LogType.Error, "Interrupted");
            return;
        } catch (ExecutionException e) {
            fail();
            suite.logMessage(LogEntry.LogType.Error, "Error " + e.getMessage());
            return;
        } catch (TimeoutException e) {
            fail();
            suite.logMessage(LogEntry.LogType.Error, "Timed out waiting for response");
            return;
        }

        if ((Integer)dp.getValue() != 1) {
            fail();
            suite.logMessage(LogEntry.LogType.Error, "Returned datapoint value is " + dp.getValue());
            return;
        }

        // Check to see if we're stopped
        if (!suite.isRunning()) {
            _testStatus = TestStatus.Stopped;
            suite.logMessage(LogEntry.LogType.Info, "Test suite was stopped");
            return;
        }

        // Turn on the green LED
        suite.logMessage(LogEntry.LogType.Message, "Turning green LED ON");
        future = RequestFuture.newFuture();
        greenLED.createDatapointLAN(1, null, future, future);
        try {
            dp = future.get(3000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            fail();
            suite.logMessage(LogEntry.LogType.Error, "Interrupted");
            return;
        } catch (ExecutionException e) {
            fail();
            suite.logMessage(LogEntry.LogType.Error, "Error " + e.getMessage());
            return;
        } catch (TimeoutException e) {
            fail();
            suite.logMessage(LogEntry.LogType.Error, "Timed out waiting for response");
            return;
        }

        if ((Integer)dp.getValue() != 1) {
            fail();
            suite.logMessage(LogEntry.LogType.Error, "Returned datapoint value is " + dp.getValue());
            return;
        }


        // Check to see if we're stopped
        if (!suite.isRunning()) {
            _testStatus = TestStatus.Stopped;
            suite.logMessage(LogEntry.LogType.Info, "Test suite was stopped");
            return;
        }

        // Turn off the blue LED
        suite.logMessage(LogEntry.LogType.Message, "Turning blue LED OFF");
        future = RequestFuture.newFuture();
        blueLED.createDatapointLAN(0, null, future, future);
        try {
            dp = future.get(3000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            fail();
            suite.logMessage(LogEntry.LogType.Error, "Interrupted");
            return;
        } catch (ExecutionException e) {
            fail();
            suite.logMessage(LogEntry.LogType.Error, "Error " + e.getMessage());
            return;
        } catch (TimeoutException e) {
            fail();
            suite.logMessage(LogEntry.LogType.Error, "Timed out waiting for response");
            return;
        }

        if ((Integer)dp.getValue() != 0) {
            fail();
            suite.logMessage(LogEntry.LogType.Error, "Returned datapoint value is " + dp.getValue());
            return;
        }

        // Check to see if we're stopped
        if (!suite.isRunning()) {
            _testStatus = TestStatus.Stopped;
            suite.logMessage(LogEntry.LogType.Info, "Test suite was stopped");
            return;
        }

        // Green LED on
        suite.logMessage(LogEntry.LogType.Message, "Turning green LED OFF");
        future = RequestFuture.newFuture();
        greenLED.createDatapointLAN(0, null, future, future);
        try {
            dp = future.get(3000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            fail();
            suite.logMessage(LogEntry.LogType.Error, "Interrupted");
            return;
        } catch (ExecutionException e) {
            fail();
            suite.logMessage(LogEntry.LogType.Error, "Error " + e.getMessage());
            return;
        } catch (TimeoutException e) {
            fail();
            suite.logMessage(LogEntry.LogType.Error, "Timed out waiting for response");
            return;
        }

        if ((Integer)dp.getValue() != 0) {
            fail();
            suite.logMessage(LogEntry.LogType.Error, "Returned datapoint value is " + dp.getValue());
            return;
        }

        pass();
    }
}
