package com.aylanetworks.aura.test;
/*
 * AylaSDK
 *
 * Copyright 2015 Ayla Networks, all rights reserved
 */

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.android.volley.Request;
import com.android.volley.Response;
import com.aylanetworks.aylasdk.AylaAPIRequest;
import com.aylanetworks.aylasdk.AylaDevice;
import com.aylanetworks.aylasdk.AylaDeviceManager;
import com.aylanetworks.aylasdk.AylaJsonRequest;
import com.aylanetworks.aylasdk.AylaSessionManager;
import com.aylanetworks.aylasdk.error.ErrorListener;
import com.aylanetworks.aylasdk.error.JsonError;
import com.aylanetworks.aylasdk.error.PreconditionError;
import com.aylanetworks.aylasdk.ota.AylaLanOTADevice;
import com.aylanetworks.aylasdk.ota.AylaOTAImageInfo;
import com.aylanetworks.aylasdk.error.RequestFuture;
import com.google.gson.annotations.Expose;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import fi.iki.elonen.NanoHTTPD;

public class LanOTAUpdateTestCase extends TestCase implements AylaLanOTADevice.LanOTAListener {

    final private Object _statusObj = new Object();
    private TestSuite _suite;
    private String _dsn;

    private String _groupID1;
    private String _imageID1;
    private String _imageID2;
    private Context _context;

    private final String URL_JOB= "https://staging-ais.ayladev.com/";
    private static final String OTA_JOB_ID1 = "ota_jobid_one";
    private static final String OTA_JOB_ID2 = "ota_jobid_two";
    private static final String KEY_GROUP_ID1 = "ota_groupid_one";
    private static final String KEY_IMAGE_ID1 = "ota_imageid_one";
    private static final String KEY_IMAGE_ID2 = "ota_imageid_two";

    public LanOTAUpdateTestCase(Context context) {
        super("LAN OTA Update Test");
        _context = context;
    }

    @Override
    public View getConfigView() {
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(_context);

        LinearLayout root = new LinearLayout(_context);
        root.setOrientation(LinearLayout.VERTICAL);

        EditText groupID1Input = new EditText(_context);
        groupID1Input.setInputType(InputType.TYPE_CLASS_TEXT);
        groupID1Input.setHint("Group ID 1");
        _groupID1 = sharedPreferences.getString(KEY_GROUP_ID1, "");
        groupID1Input.setText(_groupID1);
        groupID1Input.setSelection(_groupID1.length());
        groupID1Input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                _groupID1 = editable.toString();
                sharedPreferences.edit().putString(KEY_GROUP_ID1, _groupID1).apply();
            }
        });
        root.addView(groupID1Input);

        EditText imageID1Input = new EditText(_context);
        imageID1Input.setInputType(InputType.TYPE_CLASS_TEXT);
        imageID1Input.setHint("Image ID 1");
        _imageID1 = sharedPreferences.getString(KEY_IMAGE_ID1, "");
        imageID1Input.setText(_imageID1);
        imageID1Input.setSelection(_imageID1.length());
        imageID1Input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                _imageID1 = editable.toString();
                sharedPreferences.edit().putString(KEY_IMAGE_ID1, _imageID1).apply();
            }
        });
        root.addView(imageID1Input);


        EditText imageID2Input = new EditText(_context);
        imageID2Input.setInputType(InputType.TYPE_CLASS_TEXT);
        imageID2Input.setHint("Image ID 2");
        _imageID2 = sharedPreferences.getString(KEY_IMAGE_ID2, "");
        imageID2Input.setText(_imageID2);
        imageID2Input.setSelection(_imageID2.length());
        imageID2Input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                _imageID2 = editable.toString();
                sharedPreferences.edit().putString(KEY_IMAGE_ID2, _imageID2).apply();
            }
        });
        root.addView(imageID2Input);

        return root;
    }

    public void run(TestSuite suite) {
        super.run(suite);
        _suite = suite;

        AylaDevice device = suite.getDeviceUnderTest();
        if (!device.isLanModeActive()) {
            suite.logMessage(LogEntry.LogType.Error, "Device is not in LAN mode");
            fail();
            return;
        }
        _dsn = device.getDsn();
        RequestFuture<AylaOTAImageInfo> future = RequestFuture.newFuture();
        AylaLanOTADevice aylaLanOTA = new AylaLanOTADevice(device.getDeviceManager(),_dsn,
                null);
        aylaLanOTA.fetchOTAImageInfo(future,future);
        AylaOTAImageInfo imageInfo;
        suite.logMessage(LogEntry.LogType.Info, "Calling fetchOTAImageInfo");
        try {
            imageInfo=future.get(20000, TimeUnit.MILLISECONDS);
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

        // Check to see if we're stopped
        if (!suite.isRunning()) {
            _testStatus = TestStatus.Stopped;
            suite.logMessage(LogEntry.LogType.Info, "Test suite was stopped");
            return;
        }

        StringBuilder path = new StringBuilder(Environment.getExternalStorageDirectory().toString());
        path.append("/");
        path.append( _dsn);
        path.append(".image");

        suite.logMessage(LogEntry.LogType.Info, "Download Image to path " +path);

        RequestFuture<AylaAPIRequest.EmptyResponse> futureFetchFile = RequestFuture.newFuture();
        aylaLanOTA.fetchOTAImageFile(imageInfo,path.toString(),null,futureFetchFile,futureFetchFile);
        suite.logMessage(LogEntry.LogType.Info, "Calling fetchOTAImageFile");
        try {
            futureFetchFile.get(20000, TimeUnit.MILLISECONDS);
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
        suite.logMessage(LogEntry.LogType.Info, "Image size is:"+imageInfo.getSize());
        suite.logMessage(LogEntry.LogType.Info, "Image version is:"+imageInfo.getVersion());

        // Check to see if we're stopped
        if (!suite.isRunning()) {
            _testStatus = TestStatus.Stopped;
            suite.logMessage(LogEntry.LogType.Info, "Test suite was stopped");
            return;
        }

        suite.logMessage(LogEntry.LogType.Info, "Calling pushOTAImageToDevice");
        RequestFuture<AylaAPIRequest.EmptyResponse> futurePushImage = RequestFuture.newFuture();
        aylaLanOTA.pushOTAImageToDevice(futurePushImage, futurePushImage);
        aylaLanOTA.setLanOTAListener(this);
        try {
            int API_TIMEOUT_MS = 90000;
            futurePushImage.get(API_TIMEOUT_MS, TimeUnit.MILLISECONDS);
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

        try {
            synchronized (_statusObj) {
                _statusObj.wait(300000);//5 minutes. It should not take 5 minutes but this is
                // like max wait time
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        boolean isDeleted = aylaLanOTA.deleteOTAFile();
        if(isDeleted) {
            suite.logMessage(LogEntry.LogType.Info, "deleted OTAFile");
        }
        else {
            fail();
            suite.logMessage(LogEntry.LogType.Error, "Unable to delete OTA File");
        }
        suite.logMessage(LogEntry.LogType.Info, "Calling updateOTADownloadStatus");
        RequestFuture<AylaAPIRequest.EmptyResponse> futureConnect = RequestFuture.newFuture();
        aylaLanOTA.updateOTADownloadStatus(futureConnect,futureConnect);
        try {
            int API_TIMEOUT_MS = 30000;
            futureConnect.get(API_TIMEOUT_MS, TimeUnit.MILLISECONDS);
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
        long jobID;
        //First check if we already created an OTA Job. In that case we just need to start the job
        int iterationNumber = _suite.getCurrentIteration();
        final SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(_context);
        if(iterationNumber == 1 || iterationNumber == 2) {
            suite.logMessage(LogEntry.LogType.Info, "OTA Image Update is success, Creating an OTA JOB" +
                    " for the next Image update cycle");
            String strGroupId= _groupID1;
            String strImageId = _imageID1;
            String imageName = "OTA Update1";
            if(iterationNumber ==2) {
                strImageId = _imageID2;
                imageName= "OTA Update2";
            }
            long groupId = Long.parseLong(strGroupId);
            long imageId = Long.parseLong(strImageId);

            RequestFuture<OTAJob> futureOTAJob = RequestFuture.newFuture();
            createOTAJob(device,groupId,imageId,imageName,futureOTAJob,futureOTAJob);
            OTAJob otaJob;
            try {
                int API_TIMEOUT_MS = 30000;
                otaJob=futureOTAJob.get(API_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                jobID =otaJob.getId();
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
            suite.logMessage(LogEntry.LogType.Info, "Created OTA JOB. Now starting the job");

            if(iterationNumber == 1) {
                sharedPreferences.edit().putLong(OTA_JOB_ID1, jobID).apply();
            } else {
                sharedPreferences.edit().putLong(OTA_JOB_ID2, jobID).apply();
            }
        }
        else {
            if(iterationNumber %2 ==1) {
                jobID = sharedPreferences.getLong(OTA_JOB_ID1,-1);
            } else {
                jobID = sharedPreferences.getLong(OTA_JOB_ID2,-1);
            }
        }
        if(jobID == -1){
            suite.logMessage(LogEntry.LogType.Error, "Unable to get the Job ID");
            fail();
        }
        suite.logMessage(LogEntry.LogType.Info, "Calling OTA Start Job");
        RequestFuture<AylaAPIRequest.EmptyResponse> futureStartJob = RequestFuture.newFuture();
        startOTAJob(device,jobID,futureStartJob,futureStartJob);
        try {
            int API_TIMEOUT_MS = 30000;
            futureStartJob.get(API_TIMEOUT_MS, TimeUnit.MILLISECONDS);
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

        pass();
    }

    public void updateStatus(int statusCode, String error) {
        synchronized (_statusObj) {
            _statusObj.notify();
        }
        String msg;
        if(NanoHTTPD.Response.Status.OK.getRequestStatus()==statusCode) {
            msg= "Updated image for: " + _dsn;
            _suite.logMessage(LogEntry.LogType.Info, msg);
        }
        else {
            StringBuilder sb = new StringBuilder("Update image failed for: ");
            sb.append(_dsn);
            sb.append(" status code is " +statusCode);
            sb.append(" and error is " +error);
            msg = sb.toString();
            _suite.logMessage(LogEntry.LogType.Error, msg);
        }
    }

    //We are Creating an OTA job and on Success we start an OTA job. These methods are added here
    // and not in SDK as they are just part of testing the LAN OTA.
    private AylaAPIRequest createOTAJob(final AylaDevice device,
                                         final long groupID,
                                         final long imageID,
                                         final String jobName,
                                         final Response.Listener<OTAJob>
                                                 successListener,
                                         final ErrorListener errorListener) {
        final AylaDeviceManager deviceManager = device.getDeviceManager();
        if (deviceManager == null) {
            if (errorListener != null) {
                errorListener.onErrorResponse(new PreconditionError("No device manager is " +
                        "available"));
            }
            return null;
        }

        AylaSessionManager sessionManager = deviceManager.getSessionManager();
        if (sessionManager == null) {
            if (errorListener != null) {
                errorListener.onErrorResponse(new PreconditionError("No session is active"));
            }
            return null;
        }

        String url = URL_JOB+"apiv1/job.json?env=ha_staging";

        final JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("group_id", groupID);
            jsonBody.put("image_id", imageID);
            jsonBody.put("ignore_invalid", true);
            jsonBody.put("name",jobName);
        } catch (JSONException e) {
            if (errorListener != null) {
                errorListener.onErrorResponse(new JsonError(null, "JSONException trying to create "
                        + "updateOTADownloadStatus", e));
            }
            return null;
        }
        AylaAPIRequest<OTAJob.Wrapper> request = new AylaJsonRequest<>(
                Request.Method.POST,
                url,
                jsonBody.toString(),
                null,
                OTAJob.Wrapper.class,
                sessionManager,
                new Response.Listener<OTAJob.Wrapper>() {
                    @Override
                    public void onResponse(OTAJob.Wrapper response) {
                        OTAJob otajob = response.job;
                        successListener.onResponse(otajob);
                    }
                },
                errorListener);
        deviceManager.sendDeviceServiceRequest(request);
        return request;
    }
    private AylaAPIRequest startOTAJob(final AylaDevice device,
                        final long jobID,
                             Response.Listener<AylaAPIRequest.EmptyResponse> successListener,
                        final ErrorListener errorListener) {
        final AylaDeviceManager deviceManager = device.getDeviceManager();
        if (deviceManager == null) {
            if (errorListener != null) {
                errorListener.onErrorResponse(new PreconditionError("No device manager is " +
                        "available"));
            }
            return null;
        }

        AylaSessionManager sessionManager = deviceManager.getSessionManager();
        if (sessionManager == null) {
            if (errorListener != null) {
                errorListener.onErrorResponse(new PreconditionError("No session is active"));
            }
            return null;
        }

        String url =URL_JOB+"apiv1/job/"+jobID+"/start";
        AylaAPIRequest<AylaAPIRequest.EmptyResponse> request = new AylaAPIRequest<>(
                Request.Method.POST,
                url,
                null,
                AylaAPIRequest.EmptyResponse.class,
                sessionManager,
                successListener,
                errorListener);

        deviceManager.sendDeviceServiceRequest(request);
        return request;
    }

    class OTAJob {
        @Expose
        private long id;
        @Expose
        private String name;

        public long getId() { return id; }

        public String getName() { return name; }

        public class Wrapper {
            @Expose
            public OTAJob job;
        }
    }
}
