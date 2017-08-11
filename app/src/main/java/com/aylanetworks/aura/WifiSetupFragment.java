package com.aylanetworks.aura;
/*
 * Aura
 *
 * Copyright 2015 Ayla Networks, all rights reserved
 */

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.util.Predicate;
import com.android.volley.Response;
import com.aylanetworks.aylasdk.AylaAPIRequest;
import com.aylanetworks.aylasdk.AylaLog;
import com.aylanetworks.aylasdk.error.AppPermissionError;
import com.aylanetworks.aylasdk.error.AylaError;
import com.aylanetworks.aylasdk.error.ErrorListener;
import com.aylanetworks.aylasdk.setup.AylaSetup;
import com.aylanetworks.aylasdk.setup.AylaSetupDevice;
import com.aylanetworks.aylasdk.setup.AylaWifiScanResults;
import com.aylanetworks.aylasdk.setup.AylaWifiStatus;
import com.aylanetworks.aylasdk.util.EmptyListener;
import com.aylanetworks.aylasdk.util.ObjectUtils;
import com.aylanetworks.aylasdk.util.PermissionUtils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;


public class WifiSetupFragment extends Fragment implements AdapterView.OnItemClickListener {
    private static final String LOG_TAG = "WifiSetup";

    private ListView _listView;
    private TextView _messageTextView;
    private Button _button;
    private LinearLayout _passwordLayout;
    private EditText _passwordEditText;
    private EditText _regexEditText;
    private String _scanRegex = "Ayla-[0-9a-zA-Z]{12}";
    private String _setupToken;
    private Snackbar _currentSnackbar;
    private AylaSetup _setup;
    private AylaSetupDevice _setupDevice;

    private State _state;

    public static WifiSetupFragment newInstance() {
        return new WifiSetupFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_wifi_setup, container, false);
        _listView = (ListView) view.findViewById(R.id.listview);
        _listView.setOnItemClickListener(this);
        _messageTextView = (TextView) view.findViewById(R.id.textview);
        _passwordLayout = (LinearLayout) view.findViewById(R.id.password_layout);
        _passwordEditText = (EditText) view.findViewById(R.id.password_entry);
        CheckBox cb = (CheckBox)view.findViewById(R.id.wifi_show_password);
        cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    _passwordEditText.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
                } else {
                    _passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                }
            }
        });
        _button = (Button) view.findViewById(R.id.button);
        _regexEditText = (EditText) view.findViewById(R.id.ap_regex_edit);
        _regexEditText.setText(_scanRegex);

        Button exitButton = (Button) view.findViewById(R.id.exit_setup);
        exitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exitSetupClicked();
            }
        });

        setState(State.Uninitialized);

        if (_setup != null) {
            println("Setup object created. Session has been paused.");
        }

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        getActivity().setTitle("WiFi Setup");
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Make sure we have all required permissions
        AppPermissionError error = null;
        error = PermissionUtils.checkPermissions(getContext(),
                AylaSetup.SETUP_REQUIRED_PERMISSIONS);
        if (error != null) {
            ActivityCompat.requestPermissions(getActivity(), AylaSetup.SETUP_REQUIRED_PERMISSIONS,
                    0);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (_setup != null) {
            EmptyListener<AylaAPIRequest.EmptyResponse> el =
                    new EmptyListener<AylaAPIRequest.EmptyResponse>();
            _setup.exitSetup(el, el);
        }
    }

    private void println(final String line) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                _messageTextView.setText(_messageTextView.getText() + "\n" + line);
            }
        });
    }

    private void startSetupClicked() {
        try {
            _setup = new AylaSetup(getContext(), MainActivity.getSession());
            _messageTextView.setText("");
            println("Setup object created");
            setState(State.Initialized);
        } catch (AylaError aylaError) {
            _setup = null;
            Toast.makeText(getContext(), "Could not create setup object- do we have permission?",
                    Toast.LENGTH_LONG).show();
            MainActivity.sharedInstance().navigateHome();
        }
    }

    private void exitSetupClicked() {
        if (_setup == null) {
            println("Setup has not been started");
        } else {
            AylaAPIRequest request = _setup.exitSetup(new Response.Listener<AylaAPIRequest.EmptyResponse>() {
                @Override
                public void onResponse(AylaAPIRequest.EmptyResponse response) {
                    _setup = null;
                    setState(State.Uninitialized);
                    dismissCancelableMessage();
                }
            }, new ErrorListener() {
                @Override
                public void onErrorResponse(AylaError error) {
                    dismissCancelableMessage();
                    displayError(error);
                }
            });
            if (request != null) {
                displayCancelableMessage("Exiting Setup", request);
            }
        }
    }

    private void scanForDevicesClicked() {
        _scanRegex = _regexEditText.getText().toString();

        try{
            Pattern.compile(_scanRegex, Pattern.CASE_INSENSITIVE);
        } catch(PatternSyntaxException e){
            Toast.makeText(getContext(), getString(R.string.invalid_regex), Toast.LENGTH_SHORT).show();
            return;
        }
        AylaAPIRequest request = _setup.scanForAccessPoints(10, new Predicate<ScanResult>() {
            @Override
            public boolean apply(ScanResult scanResult) {
                return scanResult.SSID.matches(_scanRegex);

            }
        }, new Response.Listener<ScanResult[]>() {
            @Override
            public void onResponse(ScanResult[] response) {
                dismissCancelableMessage();
                println("Received " + response.length + " access points.");
                _listView.setAdapter(new ScanResultAdapter(getContext(), response));
                if (response.length == 0) {
                    Snackbar.make(_messageTextView, "No device APs found",
                            Snackbar.LENGTH_SHORT).show();
                    setState(State.Initialized);
                } else {
                    setState(State.ChooseDevice);
                }
            }
        }, new ErrorListener() {
            @Override
            public void onErrorResponse(AylaError error) {
                dismissCancelableMessage();
                displayError(error);
            }
        });

        displayCancelableMessage("Scanning for devices...", request);
    }

    private void chooseDeviceClicked() {
        // Get the scan info
        int selectedPos = _listView.getCheckedItemPosition();
        if (selectedPos == ListView.INVALID_POSITION) {
            Toast.makeText(getContext(), "Select a device to set up", Toast.LENGTH_LONG).show();
            return;
        }

        ScanResult result = (ScanResult) _listView.getAdapter().getItem(selectedPos);
        AylaLog.d(LOG_TAG, "Setting up " + result.SSID);

        AylaAPIRequest request = _setup.connectToNewDevice(result.SSID, 10,
                new Response.Listener<AylaSetupDevice>() {
                    @Override
                    public void onResponse(AylaSetupDevice response) {
                        dismissCancelableMessage();
                        println("Connected to " + response.getDsn());
                        _setupDevice = response;
                        setState(State.Connected);
                        //showNetworkInfo();
                    }
                },
                new ErrorListener() {
                    @Override
                    public void onErrorResponse(AylaError error) {
                        dismissCancelableMessage();
                        displayError(error);
                        //showNetworkInfo();
                    }
                });

        displayCancelableMessage("Connecting to " + result.SSID + "...", request);
    }

    private void deviceScanForAPsClicked() {
        // Set up a filter for the returned results to ignore SSIDs that start with "Ayla-"
        final Predicate<AylaWifiScanResults.Result> filter =
                new Predicate<AylaWifiScanResults.Result>() {
            @Override
            public boolean apply(AylaWifiScanResults.Result result) {
                return result.ssid != null && !result.ssid.startsWith("Ayla-");
            }
        };

        AylaAPIRequest request = _setup.startDeviceScanForAccessPoints(
                new Response.Listener<AylaAPIRequest.EmptyResponse>() {
                    @Override
                    public void onResponse(AylaAPIRequest.EmptyResponse response) {
                        _setup.fetchDeviceAccessPoints(filter,
                                new Response.Listener<AylaWifiScanResults>() {
                                    @Override
                                    public void onResponse(AylaWifiScanResults response) {
                                        println("Received " + response.results.length + " APs");
                                        dismissCancelableMessage();
                                        if(isAdded()){
                                            if(response.results.length == 0){
                                                Toast.makeText(getActivity(), getString(
                                                        R.string.retry_scan),
                                                        Toast.LENGTH_SHORT).show();
                                                setState(State.Connected);
                                            } else{
                                                _listView.setAdapter(new DeviceScanAdapter(getContext(),
                                                        response.results));
                                                setState(State.ConnectToAp);
                                            }
                                        }
                                    }
                                },
                                new ErrorListener() {
                                    @Override
                                    public void onErrorResponse(AylaError error) {
                                        dismissCancelableMessage();
                                        displayError(error);
                                        //showNetworkInfo();
                                    }
                                });
                    }
                }, new ErrorListener() {
                    @Override
                    public void onErrorResponse(AylaError error) {
                        dismissCancelableMessage();
                        displayError(error);
                        //showNetworkInfo();
                    }
                });
        displayCancelableMessage("Device is scanning for APs...", request);
    }

    private void disconnectAPClicked(){
        _setup.disconnectAPMode(new Response.Listener<AylaAPIRequest.EmptyResponse>() {
            @Override
            public void onResponse(AylaAPIRequest.EmptyResponse response) {
                setState(State.Reconnect);
            }
        }, new ErrorListener() {
            @Override
            public void onErrorResponse(AylaError error) {
                AylaLog.d(LOG_TAG, "Error in disconnecting from AP. "+error.getLocalizedMessage());
                setState(State.Reconnect);
            }
        });
    }

    private void reconnectToOriginalNetworkClicked() {
        AylaAPIRequest request = _setup.reconnectToOriginalNetwork(10, new Response.Listener<AylaAPIRequest.EmptyResponse>() {
            @Override
            public void onResponse(AylaAPIRequest.EmptyResponse response) {
                println("Success! Tap Exit Setup to start over.");
                dismissCancelableMessage();
                setState(State.ConfirmDeviceConnection);
            }
        }, new ErrorListener() {
            @Override
            public void onErrorResponse(AylaError error) {
                dismissCancelableMessage();
                displayError(error);
            }
        });

        if (request != null) {
            displayCancelableMessage("Connecting back to original network...", request);
        }
    }

    private void confirmDeviceConnectionClicked() {
        println("Confirming device is connected to the Ayla service...");
        AylaAPIRequest request = _setup.confirmDeviceConnected(10, _setupDevice.getDsn(),

                _setupToken, new Response.Listener<AylaSetupDevice>() {
                    @Override
                    public void onResponse(AylaSetupDevice response) {
                        println("Device has connected to the Ayla service");
                        dismissCancelableMessage();
                        _button.setText("Exit Setup");
                        _button.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                exitSetupClicked();
                            }
                        });
                    }
                },
                new ErrorListener() {
                    @Override
                    public void onErrorResponse(AylaError error) {
                        dismissCancelableMessage();
                        displayError(error);
                    }
                });

        if (request != null) {
            displayCancelableMessage("Confirming device connection to service...", request);
        }
    }

    private void connectDeviceToAPClicked() {
        int checked = _listView.getCheckedItemPosition();
        if (checked == ListView.INVALID_POSITION) {
            Toast.makeText(getActivity(), "Please select an AP to connect the device to",
                    Toast.LENGTH_LONG).show();
            return;
        }

        AylaWifiScanResults.Result result =
                (AylaWifiScanResults.Result) _listView.getAdapter().getItem(checked);


        String wifiPassword = null;
        if (!result.isSecurityOpen()) {
            // Open wifi- no password required.
            wifiPassword = _passwordEditText.getText().toString();
        }
        _setupToken = ObjectUtils.generateRandomToken(8);
        AylaAPIRequest request = _setup.connectDeviceToService(result.ssid, wifiPassword, _setupToken,
                null, null, 60,
                new Response.Listener<AylaWifiStatus>() {
                    @Override
                    public void onResponse(AylaWifiStatus response) {
                        dismissCancelableMessage();
                        println("Device has connected to the WiFi network");
                        println("Last wifi status: " + response);
                        setState(State.StopAP);
                    }
                },
                new ErrorListener() {
                    @Override
                    public void onErrorResponse(AylaError error) {
                        dismissCancelableMessage();
                        displayError(error);
                        // Nexus 6P and 5X devices gets disconnected from the device after
                        // the device connects to the AP. When this happens polling for wifi
                        // status will fail.
                        if(error.getErrorType() == AylaError.ErrorType.NetworkError){
                            println("A network error occurred while checking wifi status " +
                                    "of device. Reconnect to original network to check" +
                                    " if the device connected to Ayla service.");
                            setState(State.Reconnect);
                        }

                    }
                });
        if ( request != null ) {
            displayCancelableMessage("Connecting device to service...", request);
        } else {

        }
    }

    private void showNetworkInfo() {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                ConnectivityManager cm = (ConnectivityManager)getContext()
                        .getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo ni = cm.getActiveNetworkInfo();
                println("Network info:");
                println("Detailed state: " + ni.getDetailedState().name());
                Enumeration<NetworkInterface> interfaces;
                try {
                    interfaces = NetworkInterface.getNetworkInterfaces();
                } catch (SocketException e) {
                    println("Exception trying to enum interfaces: " + e.getMessage());
                    return;
                }
                while (interfaces.hasMoreElements()) {
                    NetworkInterface nif = interfaces.nextElement();
                    String name = nif.getDisplayName();
                    Enumeration<InetAddress> netAddrs = nif.getInetAddresses();
                    println("Interface " + name);
                    while (netAddrs.hasMoreElements()) {
                        InetAddress addr = netAddrs.nextElement();
                        println(addr.getHostName() + ":   " + addr.getHostAddress());
                    }
                    println("");
                }
                println("");

            }
        };

        Thread t = new Thread(r);
        t.start();
    }

    private void displayError(AylaError error) {
        String messageText = error.getMessage();
        CharSequence existingText = _messageTextView.getText();
        if (messageText != null && messageText.length() > 0) {
            Spannable spannable = new SpannableString(existingText + "\n" + messageText);
            Context context = getContext();
            if ( context != null ) {
                spannable.setSpan(new ForegroundColorSpan(
                                ContextCompat.getColor(context, R.color.colorWarning)),
                        existingText.length() + 1, messageText.length() + existingText.length(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            _messageTextView.setText(spannable);
        }
    }

    private void dismissCancelableMessage() {
        if (_currentSnackbar != null) {
            _currentSnackbar.dismiss();
            _currentSnackbar = null;
        }
    }


    private void displayCancelableMessage(String message, final AylaAPIRequest requestToCancel) {
        dismissCancelableMessage();

        _currentSnackbar = Snackbar.make(_messageTextView, message, Snackbar.LENGTH_INDEFINITE);
        if (requestToCancel != null) {
            _currentSnackbar.setAction(android.R.string.cancel, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    requestToCancel.cancel();
                    _currentSnackbar.dismiss();
                    _currentSnackbar = null;
                }
            });
        }
        _currentSnackbar.show();
    }
    private void setState(State state) {
        println("State set to " + state);
        _state = state;
        switch (state) {
            case Uninitialized:
                _messageTextView.setText("");
                _passwordLayout.setVisibility(View.GONE);
                println("Uninitialized");
                _listView.setAdapter(null);
                _listView.setVisibility(View.VISIBLE);
                _button.setText("Start Setup");
                _button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startSetupClicked();
                    }
                });
                break;

            case Initialized:
                _listView.setAdapter(null);
                _button.setText("Scan for devices");
                _button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        scanForDevicesClicked();
                    }
                });
                break;

            case ChooseDevice:
                _button.setText("Choose device to set up");
                _button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        chooseDeviceClicked();
                    }
                });
                break;

            case Connected:
                _listView.setAdapter(null);
                _button.setText("Device Scan for APs");
                _button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        deviceScanForAPsClicked();
                    }
                });
                break;

            case ConnectToAp:
                _button.setText("Connect device to AP");
                _button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        connectDeviceToAPClicked();
                    }
                });
                break;

            case StopAP:
                _listView.setAdapter(null);
                _button.setText(R.string.disconnect_AP);
                _button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        disconnectAPClicked();
                    }
                });
                break;

            case Reconnect:
                _listView.setVisibility(View.GONE);
                _passwordLayout.setVisibility(View.GONE);
                _button.setText("Re-connect to original network");
                _button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        reconnectToOriginalNetworkClicked();
                    }
                });
                break;

            case ConfirmDeviceConnection:
                _button.setText("Confirm Device Connection");
                _button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        confirmDeviceConnectionClicked();
                    }
                });
                break;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        _listView.setItemChecked(position, true);
        switch (_state) {
            case ChooseDevice:
                ScanResult selectedResult = (ScanResult) _listView.getAdapter().getItem(position);
                _button.setText("Set up " + selectedResult.SSID);
                break;
            case ConnectToAp:
                AylaWifiScanResults.Result result = (AylaWifiScanResults.Result)
                        _listView.getAdapter().getItem(position);
                _button.setText("Connect device to " + result.ssid);
                if (result.isSecurityOpen()) {
                    _passwordLayout.setVisibility(View.GONE);
                } else {
                    _passwordLayout.setVisibility(View.VISIBLE);
                    _passwordEditText.requestFocus();
                }
            default:
                AylaLog.d(LOG_TAG, "List item click in state " + _state);
                break;
        }
    }

    enum State {
        Uninitialized,
        Initialized,
        ChooseDevice,
        Connected,
        ScanForAps,
        ConnectToAp,
        StopAP,
        Reconnect,
        ConfirmDeviceConnection
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

    public static class DeviceScanAdapter extends ArrayAdapter<AylaWifiScanResults.Result> {

        public DeviceScanAdapter(Context context, AylaWifiScanResults.Result[] items) {
            super(context, R.layout.scan_result_item, items);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            AylaWifiScanResults.Result result = getItem(position);
            View view = convertView;
            if (view == null) {
                LayoutInflater inflater = (LayoutInflater) getContext()
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(R.layout.scan_result_item, parent, false);
            }

            TextView textView = (TextView) view.findViewById(R.id.textview);
            textView.setText(result.ssid);

            return view;
        }
    }
}
