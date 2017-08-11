package com.aylanetworks.aura;
/*
 * AylaSDK
 *
 * Copyright 2015 Ayla Networks, all rights reserved
 */
import android.content.Context;
import android.graphics.Color;
import android.net.wifi.ScanResult;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.util.Predicate;
import com.android.volley.Response;
import com.aylanetworks.aylasdk.AylaAPIRequest;
import com.aylanetworks.aylasdk.error.AppPermissionError;
import com.aylanetworks.aylasdk.ota.AylaLanOTADevice;
import com.aylanetworks.aylasdk.AylaLog;
import com.aylanetworks.aylasdk.ota.AylaOTAImageInfo;
import com.aylanetworks.aylasdk.MultipartProgressListener;
import com.aylanetworks.aylasdk.error.AylaError;
import com.aylanetworks.aylasdk.error.ErrorListener;
import com.aylanetworks.aylasdk.setup.AylaSetup;
import com.aylanetworks.aylasdk.util.PermissionUtils;
import fi.iki.elonen.NanoHTTPD;

public class LanOTAFragment extends Fragment implements AylaLanOTADevice.LanOTAListener,
        AdapterView.OnItemClickListener {
    private static final String ARG_DSN = "dsn";
    private static final String LOG_TAG = "LanOTAFragment";
    private EditText _deviceDSN;
    private EditText _deviceSSID;
    private TextView _status;
    private int _updatedStatusCode;
    private AylaOTAImageInfo _otaImageInfo;
    private AylaLanOTADevice _aylaLanOTA;
    private final String _scanRegex = "Ayla-[0-9a-zA-Z]{12}";
    private ListView _listView;
    private AylaSetup _setup;
    private LinearLayout _deviceSSIDLayout;
    private String _dsn;
    private boolean _validateSSID=true;
    private long _totalBytes;

    public static LanOTAFragment newInstance() {
        return new LanOTAFragment();
    }

    public static LanOTAFragment newInstance(String dsn) {
        LanOTAFragment fragment = new LanOTAFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DSN, dsn);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            _dsn = getArguments().getString(ARG_DSN);
        }
        // Make sure we have all required permissions
        AppPermissionError error;
        error = PermissionUtils.checkPermissions(getContext(),
                AylaSetup.SETUP_REQUIRED_PERMISSIONS);
        if (error != null) {
            ActivityCompat.requestPermissions(getActivity(), AylaSetup.SETUP_REQUIRED_PERMISSIONS,
                    0);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_lan_ota, container, false);
        _deviceDSN = (EditText) view.findViewById(R.id.dsn);
        _deviceSSID = (EditText) view.findViewById(R.id.device_ssid);
        _status = (TextView) view.findViewById(R.id.ota_update_info);
        _deviceSSIDLayout = (LinearLayout) view.findViewById(R.id.device_ssid_layout);

        if(_dsn != null) {
            // In this case we can hide the _deviceSSID field as this OTA Device is not in AP Mode
            // and we can get the lan ip directly from device
            _deviceDSN.setText(_dsn);
            _deviceSSIDLayout.setVisibility(View.GONE);
            _validateSSID=false;
        }
        else {
            _deviceSSIDLayout.setVisibility(View.VISIBLE);
            _validateSSID=true;
        }

        final Button checkOTAInfoButton = (Button) view.findViewById(R.id.check_ota_info);
        checkOTAInfoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkOTAInfoClicked(checkOTAInfoButton);
            }
        });
        final Button downloadOTAButton = (Button) view.findViewById(R.id.download_ota_image);
        downloadOTAButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downloadOTAImage(downloadOTAButton);
            }
        });
        final Button pushOTAButton = (Button) view.findViewById(R.id.push_ota_image);
        pushOTAButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pushOTAImageClicked(pushOTAButton);
            }
        });
        Button scanSSIDButton = (Button) view.findViewById(R.id.scan_ssid);
        scanSSIDButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanForDevices();
            }
        });
        _listView = (ListView) view.findViewById(R.id.listview);
        _listView.setOnItemClickListener(this);
        _deviceSSID.setText(_scanRegex);
        getActivity().setTitle(R.string.action_lan_ota);
        return view;
    }

    private void checkOTAInfoClicked(final Button button) {
        if(validateFields(_validateSSID)) {
            return;
        }

        final View view = getActivity().findViewById(R.id.push_ota_image);
        final Snackbar sb = Snackbar.make(view, "Checking OTA Info...",
                Snackbar.LENGTH_INDEFINITE);
        _aylaLanOTA = new AylaLanOTADevice(MainActivity.getDeviceManager(),
                _deviceDSN.getText().toString(), _deviceSSID.getText().toString());

        final AylaAPIRequest request = _aylaLanOTA.fetchOTAImageInfo(
                new Response.Listener<AylaOTAImageInfo>() {
                    @Override
                    public void onResponse(final AylaOTAImageInfo response) {
                        _otaImageInfo = response;
                        sb.dismiss();
                        String successMessage = "OTA image Info succeeded for: " + _deviceDSN
                                .getText().toString();
                        Log.d(LOG_TAG, successMessage);
                        _status.setText(successMessage);
                        button.setBackgroundColor(Color.GREEN);
                    }
                },
                new ErrorListener() {
                    @Override
                    public void onErrorResponse(AylaError error) {
                        sb.dismiss();
                        Snackbar.make(view, error.getMessage(), Snackbar.LENGTH_LONG)
                                .show();
                        button.setBackgroundColor(Color.RED);
                    }
                });
        if (request != null) {
            sb.setAction(android.R.string.cancel, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    request.cancel();
                    sb.dismiss();
                }
            });
            sb.show();
        }
    }

    private void downloadOTAImage(final Button button) {
        if(validateFields(_validateSSID)) {
            return;
        }
        final View view = getActivity().findViewById(R.id.push_ota_image);
        final Snackbar sb = Snackbar.make(view, "Downloading OTA Message...",
                Snackbar.LENGTH_INDEFINITE);
        if(_aylaLanOTA == null) {
            String warnMessage = "Check OTA Info first before clicking on Downloading OTA Image";
            _status.setText(warnMessage);
            return;
        }

        StringBuilder path = new StringBuilder(Environment.getExternalStorageDirectory().toString());
        path.append("/");
        path.append( _deviceDSN.getText().toString());
        path.append(".image");
        final DownloadImageProgress imageProgress = new DownloadImageProgress();

        _aylaLanOTA.fetchOTAImageFile(_otaImageInfo, path.toString(), imageProgress,
                new Response.Listener<AylaAPIRequest.EmptyResponse>() {
                    @Override
                    public void onResponse(final AylaAPIRequest.EmptyResponse response) {
                        sb.dismiss();
                        StringBuilder msg = new StringBuilder("OTA image download succeeded for: ");
                        msg.append(_deviceDSN.getText().toString());
                        msg.append(" and Image size in Bytes is ");
                        msg.append(_totalBytes);
                        Log.d(LOG_TAG, msg.toString());
                        _status.setText(msg.toString());
                        button.setBackgroundColor(Color.GREEN);
                    }
                },

                new ErrorListener() {
                    @Override
                    public void onErrorResponse(AylaError error) {
                        sb.dismiss();
                        Snackbar.make(view, error.getMessage(), Snackbar.LENGTH_LONG)
                                .show();
                        button.setBackgroundColor(Color.RED);
                    }
                });
        sb.setAction(android.R.string.cancel, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imageProgress.cancelDownload();
                sb.dismiss();
            }
        });
        sb.show();
    }


    private void pushOTAImageClicked(final Button button) {
        if(validateFields(_validateSSID)) {
            return;
        }
        final String msg ="Updating OTA image to device...";
        _updatedStatusCode =-1;
        final View view = getActivity().findViewById(R.id.push_ota_image);
        final Snackbar sb = Snackbar.make(view, msg,
                Snackbar.LENGTH_INDEFINITE);

        if(_aylaLanOTA == null) {
            String warnMessage = "Download OTA Image first before clicking on Push OTA Image";
            _status.setText(warnMessage);
            return;
        }
        _aylaLanOTA.setLanOTAListener(this);


        final AylaAPIRequest request = _aylaLanOTA.pushOTAImageToDevice(
                new Response.Listener<AylaAPIRequest.EmptyResponse>() {
                    @Override
                    public void onResponse(final AylaAPIRequest.EmptyResponse response) {
                        do {
                            try {
                                Thread.sleep(5000);
                            } catch (InterruptedException e) {
                                Log.d(LOG_TAG, "InterruptedException while Updating Image: "
                                        + e.getMessage());
                            }
                        } while (_updatedStatusCode == -1);
                        _aylaLanOTA.deleteOTAFile();
                        //We need to connect back to Original network only if we connected to Device
                        //in AP Mode. In our case this check is based on if we are scanning ssid and
                        //connecting to device in AP mode.


                        if(_validateSSID) {
                            _aylaLanOTA.reconnectToOriginalNetwork(15000,
                                    new Response.Listener<AylaAPIRequest.EmptyResponse>() {
                                        @Override
                                        public void onResponse(AylaAPIRequest.EmptyResponse response) {
                                            updateStatusToCloud(sb,view,true);
                                        }
                                    },
                                    new ErrorListener() {
                                        @Override
                                        public void onErrorResponse(AylaError error) {
                                            sb.dismiss();
                                            Snackbar.make(view, error.getMessage(), Snackbar.LENGTH_LONG)
                                                    .show();
                                        }
                                    });
                        } else {
                            //This is the case where it is an existing device from the Device
                            // List and we connected to it in LAN Mode. We just need to update the
                            // Cloud about the status of LAN OTA
                           updateStatusToCloud(sb,view,false);
                        }
                        button.setBackgroundColor(Color.GREEN);
                    }
                },

                new ErrorListener() {
                    @Override
                    public void onErrorResponse(AylaError error) {
                        sb.dismiss();
                        Snackbar.make(view, error.getMessage(), Snackbar.LENGTH_LONG)
                                .show();
                        button.setBackgroundColor(Color.RED);
                    }
                });
        if (request != null) {
            sb.setAction(android.R.string.cancel, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    request.cancel();
                    sb.dismiss();
                }
            });
            sb.show();
        }
    }

    private void updateStatusToCloud(final Snackbar sb, final View view, final boolean reconnectedToNetwork) {
        _aylaLanOTA.updateOTADownloadStatus(new Response.Listener<AylaAPIRequest.EmptyResponse>() {
                                                @Override
                                                public void onResponse(AylaAPIRequest.EmptyResponse response) {
                                                    sb.dismiss();
                                                    //We want to show reconnect success message only when
                                                    //update of Image is success else we want to keep the
                                                    // error message from module shown in status
                                                    if (NanoHTTPD.Response.Status.OK.getRequestStatus()
                                                            == _updatedStatusCode) {
                                                        String successMessage = "Updated the Image status to Cloud";
                                                        if(reconnectedToNetwork) {
                                                            successMessage = "Successfully reconnected to WiFi" +
                                                                    " network and Updated the Image status to Cloud";
                                                        }
                                                        _status.setText(successMessage);
                                                    }
                                                }
                                            },
                new ErrorListener() {
                    @Override
                    public void onErrorResponse(AylaError error) {
                        sb.dismiss();
                        Snackbar.make(view, error.getMessage(), Snackbar.LENGTH_LONG)
                                .show();
                    }
                });

    }

    public void updateStatus(int statusCode, String error) {
        final String msg;
        _updatedStatusCode = statusCode;
        if (NanoHTTPD.Response.Status.OK.getRequestStatus() == statusCode) {
            msg = "Updated image for: " + _deviceDSN.getText().toString();
        } else {
            StringBuilder sb = new StringBuilder("Update image failed for: ");
            sb.append(_deviceDSN.getText().toString());
            sb.append(" and error is " + error);
            msg = sb.toString();
        }
        Log.d(LOG_TAG, msg);
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                _status.setText(msg);
            }
        });

    }

    private class DownloadImageProgress implements MultipartProgressListener {
        private boolean isCanceled = false;

        public void updateProgress(long downloaded, long total) {
            _totalBytes = downloaded;
            String progressStatus = "downloaded ota image:" + downloaded + " of " + total;
            AylaLog.d(LOG_TAG, progressStatus);
            _status.setText(progressStatus);
        }

        public boolean isCanceled() {
            return isCanceled;
        }

        public void cancelDownload() {
            isCanceled = true;
        }
    }

    private void scanForDevices() {
        //In case keyboard is open. just hide it
        InputMethodManager imm = (InputMethodManager) MainActivity.sharedInstance().
                getSystemService(Context.INPUT_METHOD_SERVICE);
        if(getActivity() == null || getActivity().getCurrentFocus() == null)
            return;
        imm.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);

        final View view = getActivity().findViewById(R.id.push_ota_image);
        final Snackbar sb = Snackbar.make(view, "Scanning ...",
                Snackbar.LENGTH_INDEFINITE);
        try {
            _setup = new AylaSetup(getContext(), MainActivity.getSession());
        } catch (AylaError aylaError) {
            _setup = null;
            Toast.makeText(getContext(), R.string.setup_permission_message,
                    Toast.LENGTH_LONG).show();
            MainActivity.sharedInstance().navigateHome();
        }


        final AylaAPIRequest request = _setup.scanForAccessPoints(10, new Predicate<ScanResult>() {
            @Override
            public boolean apply(ScanResult scanResult) {
                return scanResult.SSID.matches(_scanRegex);

            }
        }, new Response.Listener<ScanResult[]>() {
            @Override
            public void onResponse(ScanResult[] response) {
                sb.dismiss();
                _listView.setAdapter(new ScanResultAdapter(getContext(), response));
                if (response.length == 0) {
                    Snackbar.make(view, R.string.no_AP_found,
                            Snackbar.LENGTH_SHORT).show();
                }
                _listView.deferNotifyDataSetChanged();
                exitSetup();
            }
        }, new ErrorListener() {
            @Override
            public void onErrorResponse(AylaError error) {
                sb.dismiss();
                exitSetup();
                if (isAdded() && !isRemoving()) {
                    Toast.makeText(getContext(), getString(R.string.error_scan_devices) + error,
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        if (request != null) {
            sb.setAction(android.R.string.cancel, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    request.cancel();
                    sb.dismiss();
                }
            });
            sb.show();
        }
    }

    public static class ScanResultAdapter extends ArrayAdapter<ScanResult> {
        public ScanResultAdapter(Context context, ScanResult[] objects) {
            super(context, R.layout.scan_result_item, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ScanResult result = getItem(position);
            View view = convertView;
            if (view == null) {
                LayoutInflater inflater = (LayoutInflater) getContext()
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(R.layout.scan_result_item, parent, false);
            }

            TextView textView = (TextView) view.findViewById(R.id.textview);
            textView.setText(result.SSID);
            return view;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        _listView.setItemChecked(position, true);
        final ScanResult result = (ScanResult) _listView.getAdapter().getItem(position);
        _deviceSSID.setText(result.SSID);
        _listView.setAdapter(null);
    }

    private void exitSetup() {
        _setup.exitSetup(
                new Response.Listener<AylaAPIRequest.EmptyResponse>() {
                    @Override
                    public void onResponse(AylaAPIRequest.EmptyResponse response) {
                        _setup = null;
                    }
                }, new ErrorListener() {
                    @Override
                    public void onErrorResponse(AylaError error) {
                        Toast.makeText(getContext(), getString(R.string.error_scan_devices) + error,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private boolean validateFields(boolean ssidCheck) {
        String dsn = _deviceDSN.getText().toString();
        if (TextUtils.isEmpty(dsn)) {
            Toast.makeText(getContext(), getString(R.string.valid_dsn),
                    Toast.LENGTH_LONG).show();
            return true;
        }
        if(ssidCheck) {
            String ssid = _deviceSSID.getText().toString();
            if (TextUtils.isEmpty(ssid) || _scanRegex.equals(ssid)) {
                Toast.makeText(getContext(), getString(R.string.valid_ssid),
                        Toast.LENGTH_LONG).show();
                return true;
            }
        }
        return false;
    }
}
