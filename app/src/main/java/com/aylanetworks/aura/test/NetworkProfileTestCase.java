package com.aylanetworks.aura.test;
/*
 * Aura_Android
 *
 * Copyright 2015 Ayla Networks, all rights reserved
 */

import android.content.Context;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;

import com.android.volley.Request;
import com.aylanetworks.aura.R;
import com.aylanetworks.aylasdk.AylaAPIRequest;
import com.aylanetworks.aylasdk.AylaDatapoint;
import com.aylanetworks.aylasdk.AylaDevice;
import com.aylanetworks.aylasdk.AylaDeviceManager;
import com.aylanetworks.aylasdk.AylaLog;
import com.aylanetworks.aylasdk.AylaProperty;
import com.aylanetworks.aylasdk.error.RequestFuture;

import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class NetworkProfileTestCase extends TestCase {

    public static final String LOG_TAG = "NETWORK_PROFILER";
    public static final String DEVICE_PING = "/ads-ping";
    public static final String USER_PING = "/user-ping";
    public static long _cloudTotalTime = 0;
    public static long _cloudNetworkTime = 0;
    public static float _cloudNetworkTimePercentage = 0;

    public static long _lanTotalTime = 0;
    public static long _lanNetworkTime = 0;
    public static float _lanNetworkTimePercentage = 0;

    private Context _context;
    private boolean _cloud = true;
    private boolean _lan = true;

    public NetworkProfileTestCase(Context context) {
        super("Network Profile Test");
        _context = context;
    }

    @Override
    public View getConfigView() {
        LinearLayout root = new LinearLayout(_context);
        root.setOrientation(LinearLayout.VERTICAL);

        CheckBox cloud = new CheckBox(_context);
        cloud.setText(_context.getString(R.string.cloud));
        cloud.setChecked(_cloud);
        cloud.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                _cloud = checked;
            }
        });
        root.addView(cloud);

        CheckBox lan = new CheckBox(_context);
        lan.setText(_context.getString(R.string.lan));
        lan.setChecked(_lan);
        lan.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                _lan = checked;
            }
        });
        root.addView(lan);

        return root;
    }

    @Override
    public void run(TestSuite suite) {

        //Device service health check
        long deviceHealthCheck = doServiceHealthCheck(suite, DEVICE_PING);
        if(deviceHealthCheck == -1){
            suite.logMessage(LogEntry.LogType.Info, "Device service health check failed");
        } else{
            suite.logMessage(LogEntry.LogType.Info, "Device service health check success in " +
                    ""+deviceHealthCheck + "ms");
        }

        //User service healthcheck
        long userServiceHealth = doServiceHealthCheck(suite, USER_PING);
        if(userServiceHealth == -1){
            suite.logMessage(LogEntry.LogType.Info, "User service health check failed");
        } else{
            suite.logMessage(LogEntry.LogType.Info, "User service health check success in " +
                    ""+deviceHealthCheck + "ms");
        }

        AylaDevice device = suite.getDeviceUnderTest();

        AylaProperty<Integer> blueLED = device.getProperty("Blue_LED");
        if (blueLED == null) {
            suite.logMessage(LogEntry.LogType.Error, "Cannot find the blue LED property");
            fail();
            return;
        }

        if (suite.getCurrentIteration() == 1) {
            _cloudNetworkTime = 0;
            _cloudTotalTime = 0;
            _cloudNetworkTimePercentage = 0;
            _lanNetworkTime = 0;
            _lanTotalTime = 0;
            _lanNetworkTimePercentage = 0;
        }

        if (_cloud) {
            boolean on = blueLED.getValue() == 1;
            profileCloudCommand(suite, blueLED, on ? 0 : 1);

            on = blueLED.getValue() == 1;
            profileCloudCommand(suite, blueLED, on ? 0 : 1);
        }

        // Check to see if we're stopped
        if (!suite.isRunning()) {
            _testStatus = TestStatus.Stopped;
            suite.logMessage(LogEntry.LogType.Info, "Test suite was stopped");
            return;
        }

        if (_lan && device.isLanModeActive()) {
            boolean on = blueLED.getValue() == 1;
            profileLANCommand(suite, blueLED, on ? 0 : 1);

            on = blueLED.getValue() == 1;
            profileLANCommand(suite, blueLED, on ? 0 : 1);
        } else {
            suite.logMessage(LogEntry.LogType.Warning, "Skipping LAN command profile");
        }

        if (suite.getRemainingRuns() == 1) {
            int operationCount = suite.getCurrentIteration() * 2;

            if (_cloud) {
                suite.logMessage(LogEntry.LogType.Info, "Cloud Operation Total: " + _cloudTotalTime + "ms; Network Total: " +
                        _cloudNetworkTime + "ms");

                float cloudAvgTotal = (float) _cloudTotalTime / operationCount;
                float cloudAvgNetwork = (float) _cloudNetworkTime / operationCount;
                float cloudAvgNetworkPercentage = _cloudNetworkTimePercentage / operationCount;

                suite.logMessage(LogEntry.LogType.Info, "Cloud Operation Average: " + cloudAvgTotal + "ms; Network Average: " +
                        cloudAvgNetwork + "ms; Percentage Average: " + cloudAvgNetworkPercentage + "%");
            }

            if (_lan) {
                suite.logMessage(LogEntry.LogType.Info, "LAN Operation Total: " + _lanTotalTime + "ms; Network Total: " +
                        _lanNetworkTime + "ms");

                float lanAvgTotal = (float) _lanTotalTime / operationCount;
                float lanAvgNetwork = (float) _lanNetworkTime / operationCount;
                float lanAvgNetworkPercentage = _lanNetworkTimePercentage / operationCount;

                suite.logMessage(LogEntry.LogType.Info, "LAN Operation Average: " + lanAvgTotal + "ms; Network Average: " +
                        lanAvgNetwork + "ms; Percentage Average: " + lanAvgNetworkPercentage + "%");
            }
        }

        pass();
    }

    private void profileCloudCommand(TestSuite suite, AylaProperty<Integer> blueLED, int value) {
        RequestFuture<AylaDatapoint<Integer>> future = RequestFuture.newFuture();
        AylaAPIRequest<AylaDatapoint> request = blueLED.createDatapointCloud(value, null, future, future);
        AylaDatapoint dp;

        suite.logMessage(LogEntry.LogType.Info, "Turning blue LED " + (value == 1 ? "on" : "off") + " via Cloud");

        long startTime = System.currentTimeMillis();
        try {
            dp = future.get(blueLED.isAckEnabled() ? 10 * 1000 : 3000, TimeUnit.MILLISECONDS);
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

        long duration = System.currentTimeMillis() - startTime;

        if ((Integer) dp.getValue() != value) {
            suite.logMessage(LogEntry.LogType.Warning, "Returned datapoint value is " + dp.getValue());
        }

        if (blueLED.isAckEnabled()) {
            Date ackedAt = blueLED.getAckedAt();
            if (ackedAt != null) {
                long diff = ackedAt.getTime() - request.getNetworkResponseTimestamp();
                suite.logMessage(LogEntry.LogType.Info, "Property ack'd by device " +
                        Math.abs(diff) + "ms " +
                        (diff <= 0 ? "before" : "after") + " the server response");
            }
        }

        float percentage = ((float) request.getNetworkTimeMs() / (float) duration) * 100f;

        _cloudTotalTime += duration;
        _cloudNetworkTime += request.getNetworkTimeMs();
        _cloudNetworkTimePercentage += percentage;

        suite.logMessage(LogEntry.LogType.Info, "Cloud Operation Time: " + duration + "ms; Network: " +
                request.getNetworkTimeMs() + "ms; Percentage: " +  Float.toString(percentage) + "%");
    }

    private void profileLANCommand(TestSuite suite, AylaProperty<Integer> blueLED, int value) {
        RequestFuture<AylaDatapoint<Integer>> future = RequestFuture.newFuture();
        AylaAPIRequest<AylaDatapoint> request = blueLED.createDatapointLAN(value, null, future, future);
        AylaDatapoint dp;

        suite.logMessage(LogEntry.LogType.Info, "Turning blue LED " + (value == 1 ? "on" : "off") + " via LAN");

        long startTime = System.currentTimeMillis();
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
        long duration = System.currentTimeMillis() - startTime;

        if ((Integer) dp.getValue() != value) {
            suite.logMessage(LogEntry.LogType.Warning, "Returned datapoint value is " + dp.getValue());
        }

        if (blueLED.isAckEnabled()) {
            Date ackedAt = blueLED.getAckedAt();
            if (ackedAt != null) {
                long diff = ackedAt.getTime() - request.getNetworkResponseTimestamp();
                suite.logMessage(LogEntry.LogType.Info, "Property ack'd by device " +
                        Math.abs(diff) + "ms " +
                        (diff <= 0 ? "before" : "after") + " the server response");
            }
        }

        float percentage = ((float) request.getNetworkTimeMs() / (float) duration) * 100f;

        _lanTotalTime += duration;
        _lanNetworkTime += request.getNetworkTimeMs();
        _lanNetworkTimePercentage += percentage;

        suite.logMessage(LogEntry.LogType.Info, "LAN Operation Time: " + duration + "ms; Network: " +
                request.getNetworkTimeMs() + "ms; Percentage: " +  Float.toString(percentage) + "%");
    }

    private long doServiceHealthCheck(TestSuite suite, String pingUrl){
        long healthCheckTime = -1;
        RequestFuture<AylaAPIRequest.EmptyResponse> future = RequestFuture.newFuture();
        AylaDeviceManager deviceManager =  suite.getDeviceUnderTest().getDeviceManager();
        if(deviceManager != null){
            AylaAPIRequest<AylaAPIRequest.EmptyResponse> serviceHealthCheck = new AylaAPIRequest<>
                    (Request.Method.GET, deviceManager.deviceServiceUrl(pingUrl), null,
                            AylaAPIRequest.EmptyResponse.class, deviceManager.getSessionManager()
                            , future, future);
            deviceManager.sendDeviceServiceRequest(serviceHealthCheck);

            try {
                future.get(3000, TimeUnit.MILLISECONDS);

            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (TimeoutException e) {
                e.printStackTrace();
            }
            if(future.isDone()){
                AylaLog.d(LOG_TAG, "Healthcheck completed");
                healthCheckTime = serviceHealthCheck.getNetworkTimeMs();
            }
        }
        return healthCheckTime;

    }
}
