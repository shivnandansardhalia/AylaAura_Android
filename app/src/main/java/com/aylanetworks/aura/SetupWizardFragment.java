package com.aylanetworks.aura;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.text.InputType;
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
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.android.internal.util.Predicate;
import com.android.volley.Response;
import com.aylanetworks.aylasdk.AylaAPIRequest;
import com.aylanetworks.aylasdk.AylaDevice;
import com.aylanetworks.aylasdk.AylaDeviceManager;
import com.aylanetworks.aylasdk.AylaLog;
import com.aylanetworks.aylasdk.AylaNetworks;
import com.aylanetworks.aylasdk.error.AppPermissionError;
import com.aylanetworks.aylasdk.error.AylaError;
import com.aylanetworks.aylasdk.error.ErrorListener;
import com.aylanetworks.aylasdk.error.InternalError;
import com.aylanetworks.aylasdk.setup.AylaRegistration;
import com.aylanetworks.aylasdk.setup.AylaRegistrationCandidate;
import com.aylanetworks.aylasdk.setup.AylaSetup;
import com.aylanetworks.aylasdk.setup.AylaSetupDevice;
import com.aylanetworks.aylasdk.setup.AylaWifiScanResults;
import com.aylanetworks.aylasdk.setup.AylaWifiStatus;
import com.aylanetworks.aylasdk.util.ObjectUtils;
import com.aylanetworks.aylasdk.util.PermissionUtils;

import java.util.ArrayList;
import java.util.List;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static com.aylanetworks.aylasdk.AylaDevice.RegistrationType;


/**
 * SetupFragment provides easy Wifi-setup and registration.
 */
public class SetupWizardFragment extends Fragment implements AdapterView.OnItemClickListener,
        AylaSetup.DeviceWifiStateChangeListener{

    private static final String LOG_TAG = "SETUP_WIZARD";
    private static final int REQUEST_LOCATION = 0;

    // The mobile app waits for WIFI_SCAN_DELAY milliseconds between starting a new
    // wifi scan and getting the latest scan results. This allows the device to do an uninterrupted
    // scan and return the latest scan results.

    private static final int WIFI_SCAN_DELAY = 5000;
    private static final String[] REG_TYPES = {"Same-LAN", "Button-Push", "AP-Mode", "Display",
            "DSN", "Node"};
    private Button _button;
    private EditText _regexEditText;
    private EditText _passwordEditText;
    private TextView _textViewSSID;
    private String _scanRegex = "Ayla-[0-9a-zA-Z]{12}";
    private AylaSetup _setup;
    private AylaSetupDevice _setupDevice;
    private Snackbar _currentSnackbar;
    private TextView _messageTextView;
    private ListView _listView;
    private LinearLayout _passwordLayout;
    private LinearLayout _regTokenLayout;
    private EditText _regtokenEditText;
    private State _state;
    private String _regType;
    private String _regToken;
    private String _setupToken;
    private Spinner _regtypeSpinner;
    private boolean _checkWifiStatus;
    private int _retryCount;
    private int _reconnectCount;

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        _listView.setItemChecked(position, true);
        switch (_state) {
            case ChooseDevice:
                chooseDeviceClicked();
                break;
            case ConnectToAp:
                AylaWifiScanResults.Result result = (AylaWifiScanResults.Result)
                        _listView.getAdapter().getItem(position);
                _button.setText(String.format(getResources().getString(R.string.connect_device) ,
                        result.ssid));
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

    @Override
    public void wifiStateChanged(String currentState) {
        println("Device state: "+currentState);
    }

    enum State {
        Uninitialized,
        Initialized,
        ChooseDevice,
        Connected,
        ConnectToAp,
        StopAP,
        Reconnect,
        ConfirmDeviceConnection,
        Register
    }

    public SetupWizardFragment() {
        // Required empty public constructor
    }

    @Override
    public void onStart() {
        super.onStart();
        getActivity().setTitle(R.string.setup_wizard);
    }

    private void setState(State state) {
        _state = state;
        switch (state) {
            case Uninitialized:
                _checkWifiStatus = false;
                _retryCount = 0;
                _reconnectCount = 0;
                _messageTextView.setText("");
                _passwordLayout.setVisibility(View.GONE);
                _listView.setAdapter(null);
                _button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startSetupClicked();
                    }
                });
                break;

            case Initialized:
                _listView.setAdapter(null);
                _button.setText(R.string.scan_devices);
                scanForDevices();
                break;

            case Connected:
                _listView.setAdapter(null);
                startDeviceScanForAPs();
                break;

            case ConnectToAp:
                _button.setText(R.string.connect_to_service);
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
                _listView.setAdapter(null);
                reconnectToOriginalNetwork();
                break;

            case ConfirmDeviceConnection:
                confirmDeviceConnection();
                break;

            case Register:
                if(_setupDevice.getRegistrationType().equals("Display")){
                    _regTokenLayout.setVisibility(View.VISIBLE);

                } else{
                    _regTokenLayout.setVisibility(View.GONE);
                }

                if(_regType.equals("Button-Push")){
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle(R.string.push_button);
                    builder.setPositiveButton(R.string.ok, null);
                    builder.show();
                }
                _button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        _regToken = _regtokenEditText.getText().toString();
                        if(_setupDevice.getRegistrationType().equals("Display")){
                            if(_regToken == null || _regToken.isEmpty())  {
                                Toast.makeText(getActivity(), R.string.enter_regtoken, Toast
                                        .LENGTH_SHORT).show();
                            } else{
                                register();
                            }
                        } else{
                            register();
                        }
                    }
                });

        }
    }
    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     * @return A new instance of fragment SetupWizardFragment.
     */
    public static SetupWizardFragment newInstance() {
        return new SetupWizardFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Make sure we have all required permissions
        AppPermissionError error = null;
        error = PermissionUtils.checkPermissions(getActivity(),
                AylaSetup.SETUP_REQUIRED_PERMISSIONS);
        if (error != null) {
            ActivityCompat.requestPermissions(getActivity(), AylaSetup.SETUP_REQUIRED_PERMISSIONS,
                    0);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if(_setup != null){
            dismissCancelableMessage();
            exitSetup();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.fragment_setup_wizard, container, false);
        _button = (Button) view.findViewById(R.id.button_setup);
        _regexEditText = (EditText) view.findViewById(R.id.ap_regex_edit);
        _regexEditText.setText(_scanRegex);
        _textViewSSID = (TextView) view.findViewById(R.id.textview_ssid);
        _messageTextView = (TextView) view.findViewById(R.id.textview);
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
        _passwordLayout = (LinearLayout) view.findViewById(R.id.password_layout);
        _regTokenLayout = (LinearLayout) view.findViewById(R.id.regtoken_layout);
        _listView = (ListView) view.findViewById(R.id.listview);
        _listView.setOnItemClickListener(this);
        _regtypeSpinner = (Spinner) view.findViewById(R.id.regtype_spinner);
        _regtypeSpinner.setAdapter(new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_spinner_item, REG_TYPES));
        _regtokenEditText = (EditText) view.findViewById(R.id.regtoken_edit_text);
        _regTokenLayout.setVisibility(View.GONE);
        setState(State.Uninitialized);

        return view;
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
            _regType = (String)_regtypeSpinner.getSelectedItem();
            if(_regType.equals("Display")){
                _regTokenLayout.setVisibility(View.VISIBLE);
            } else{
                _regTokenLayout.setVisibility(View.GONE);
            }
            _setup = new AylaSetup(getActivity(), MainActivity.getSession());
            _setup.addListener(this);
            _messageTextView.setText("");
            setState(State.Initialized);
        } catch (AylaError aylaError) {
            _setup = null;
            Toast.makeText(getActivity(), R.string.setup_permission_message,
                    Toast.LENGTH_LONG).show();
            MainActivity.sharedInstance().navigateHome();
        }
    }

    private void scanForDevices() {
        _scanRegex = _regexEditText.getText().toString();

        AylaAPIRequest request = _setup.scanForAccessPoints(15, new Predicate<ScanResult>() {
            @Override
            public boolean apply(ScanResult scanResult) {
                return scanResult.SSID.matches(_scanRegex);

            }
        }, new Response.Listener<ScanResult[]>() {
            @Override
            public void onResponse(ScanResult[] response) {
                dismissCancelableMessage();
                _listView.setAdapter(new ScanResultAdapter(getActivity(), response));
                if (response.length == 0) {
                    Snackbar.make(_messageTextView, R.string.no_AP_found,
                            Snackbar.LENGTH_SHORT).show();
                    setState(State.Initialized);
                } else {
                    setState(State.ChooseDevice);
                }
                _listView.deferNotifyDataSetChanged();
            }
        }, new ErrorListener() {
            @Override
            public void onErrorResponse(AylaError error) {
                dismissCancelableMessage();
                if(isAdded() && !isRemoving()){
                    Toast.makeText(getActivity(), getString(R.string.error_scan_devices) +error,
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        displayCancelableMessage(getString(R.string.scanning_devices), request);
    }

    private void displayCancelableMessage(String message, final AylaAPIRequest requestToCancel) {
        dismissCancelableMessage();
        if(requestToCancel != null){
            _currentSnackbar = Snackbar.make(_messageTextView, message, Snackbar.LENGTH_INDEFINITE);
            _currentSnackbar.setAction(android.R.string.cancel, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    requestToCancel.cancel();
                    if(_currentSnackbar != null){
                        _currentSnackbar.dismiss();
                        _currentSnackbar = null;
                    }

                }
            });
            _currentSnackbar.show();
        }
    }
    private void dismissCancelableMessage() {
        if (_currentSnackbar != null) {
            _currentSnackbar.dismiss();
            _currentSnackbar = null;
        }
    }

    private void chooseDeviceClicked() {
        // Get the scan info
        int selectedPos = _listView.getCheckedItemPosition();
        if (selectedPos == ListView.INVALID_POSITION) {
            Toast.makeText(getActivity(), getString(R.string.select_device),
                    Toast.LENGTH_LONG).show();
            return;
        }

        final ScanResult result = (ScanResult) _listView.getAdapter().getItem(selectedPos);
        AylaLog.d(LOG_TAG, "Setting up " + result.SSID + " capabilitites "+ result.capabilities);
        boolean isOpen = false;

        if(!result.capabilities.contains("WPA") && (!result.capabilities.contains("WPA2") &&
                !result.capabilities.contains("WEP"))){
            isOpen = true;
        }

        if(isOpen){
            AylaAPIRequest request = sendConnectToDeviceRequest(result.SSID, null,
                    AylaSetup.WifiSecurityType.NONE );
            displayCancelableMessage(String.format(getString(R.string.connecting_to_device),
                    result.SSID), request);
        }
        else{
            //display dialog to get security type and password
            displayPasswordUI(result.SSID);
        }

    }

    private void startDeviceScanForAPs() {
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
                        AylaLog.d(LOG_TAG, "startDeviceScanForAccessPoints success");
                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                _setup.fetchDeviceAccessPoints(filter,
                                        new Response.Listener<AylaWifiScanResults>() {
                                            @Override
                                            public void onResponse(AylaWifiScanResults response) {
                                                if(isAdded()){
                                                    dismissCancelableMessage();
                                                    if(response.results.length == 0){
                                                        Toast.makeText(getActivity(), getString(
                                                                R.string.device_scan_empty_results),
                                                                Toast.LENGTH_SHORT).show();
                                                        setState(State.Connected);
                                                    } else{
                                                        _listView.setAdapter(new DeviceScanAdapter(getActivity(),
                                                                response.results));
                                                        setState(State.ConnectToAp);
                                                    }

                                                }
                                            }
                                        },
                                        new ErrorListener() {
                                            @Override
                                            public void onErrorResponse(AylaError error) {
                                                if(isAdded() && !isRemoving()){
                                                    Toast.makeText(getActivity(), R.string.error_fetching_AP,
                                                            Toast.LENGTH_SHORT).show();
                                                    dismissCancelableMessage();
                                                }
                                            }
                                        });
                            }
                        }, WIFI_SCAN_DELAY);
                    }
                }, new ErrorListener() {
                    @Override
                    public void onErrorResponse(AylaError error) {
                        AylaLog.d(LOG_TAG, "startDeviceScanForAccessPoints failed with error "+error);
                        if(isAdded()){
                            Toast.makeText(getActivity(), error.getLocalizedMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                        dismissCancelableMessage();
                    }
                });
        displayCancelableMessage(getString(R.string.scanning_APs), request);
    }

    private void connectDeviceToAPClicked() {
        int checked = _listView.getCheckedItemPosition();
        if (checked == ListView.INVALID_POSITION) {
            Toast.makeText(getActivity(), R.string.select_AP,
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
        //Dsiplay setup token to the user if device uses AP-mode registration.
        if(_regType.equals(RegistrationType.APMode.stringValue())){
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(String.format(getString(R.string.setup_token_is), _setupToken));
            builder.setPositiveButton(R.string.ok, null);
            builder.show();
        }

        AylaAPIRequest request = _setup.connectDeviceToService(result.ssid, wifiPassword,
                _setupToken, null, null, 60,
                new Response.Listener<AylaWifiStatus>() {
                    @Override
                    public void onResponse(AylaWifiStatus response) {
                        if(isAdded()){
                            println(getString(R.string.connect_wifi_success));
                            _passwordLayout.setVisibility(View.GONE);
                            setState(State.StopAP);;
                        }
                        dismissCancelableMessage();
                    }
                },
                new ErrorListener() {
                    @Override
                    public void onErrorResponse(AylaError error) {
                        dismissCancelableMessage();
                        //Nexus 6P will mnost likely recieve this error because the device
                        // disconnects when it joins the AP, and polling for wifi status fails.
                        // When this occurs reconnect to original network and check with Ayla cloud
                        // if the device got connected. If device is still not connected, go back
                        // to scanning for APs and continue from there.
                        if(error.getErrorType() == (AylaError.ErrorType.NetworkError)){
                            AylaLog.d(LOG_TAG, "connectDeviceToService() failed with error "+error);
                            _checkWifiStatus = true;
                            _passwordLayout.setVisibility(View.GONE);
                            setState(State.Reconnect);
                        } else{
                            if(isAdded() && !isRemoving()){
                                println(getString(R.string.error_connecting_service)+error);
                            }
                        }

                    }
                });
        displayCancelableMessage(getString(R.string.connecting_to_service), request);
    }

    private void disconnectAPClicked(){
        AylaAPIRequest request = _setup.disconnectAPMode(new Response.Listener<AylaAPIRequest
                .EmptyResponse>() {
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
        if(isAdded()){
            displayCancelableMessage(getString(R.string.stop_ap), request);
        }
    }

    private void reconnectToOriginalNetwork() {
        final String originalSSID = _setup.getCurrentNetworkInfo() == null?
                null:_setup.getCurrentNetworkInfo().getSSID();
        AylaAPIRequest request = _setup.reconnectToOriginalNetwork(15, new Response
                .Listener<AylaAPIRequest.EmptyResponse>() {
            @Override
            public void onResponse(AylaAPIRequest.EmptyResponse response) {
                dismissCancelableMessage();
                _textViewSSID.setText(originalSSID);
                setState(State.ConfirmDeviceConnection);
            }
        }, new ErrorListener() {
            @Override
            public void onErrorResponse(AylaError error) {
                if(isAdded()){
                    dismissCancelableMessage();
                    if(error.getErrorType() == AylaError.ErrorType.Timeout){
                        if(_reconnectCount < 1) {
                            _reconnectCount++;
                            reconnectToOriginalNetwork();
                        } else{
                            println(getString(R.string.error_reconnecting) +error);
                        }
                    } else{
                        println(getString(R.string.error_reconnecting) +error);
                    }
                }

            }
        });

        if(isAdded()){
             displayCancelableMessage(getString(R.string.reconnecting), request);
        }
    }

    private void confirmDeviceConnection() {
        AylaAPIRequest request = _setup.confirmDeviceConnected(30, _setupDevice.getDsn(),
                _setupToken, new Response.Listener<AylaSetupDevice>() {
                    @Override
                    public void onResponse(AylaSetupDevice response) {
                        if(isAdded()) {
                            println(getString(R.string.connect_success));
                        }
                        _setupDevice.setRegistrationType(response.getRegistrationType());
                        _setupDevice.setLanIp(response.getLanIp());
                        dismissCancelableMessage();

                        //Exit setup so LAN mode can resume
                        exitSetup();
                        AylaDeviceManager deviceManager = AylaNetworks.sharedInstance()
                                .getSessionManager(MainActivity.SESSION_NAME).getDeviceManager();
                        AylaDevice device = deviceManager.deviceWithDSN(_setupDevice.getDsn());
                        if(device != null){
                            println(getString(R.string.device_registered));
                            _button.setVisibility(View.GONE);
                        }
                        else{
                            _button.setText(R.string.action_register);
                            setState(State.Register);
                        }

                    }
                },
                new ErrorListener() {
                    @Override
                    public void onErrorResponse(AylaError error) {
                        if(_checkWifiStatus){
                            // Scan for devices again and attempt setup again
                            if(_retryCount < 2){
                                _retryCount++;
                                scanForDevices();
                            } else{
                                if(isAdded()) {
                                    println(getString(R.string.setup_failed));
                                }
                                exitSetup();
                            }

                        }
                        dismissCancelableMessage();
                        if(isAdded() && !isRemoving()){
                            Toast.makeText(getActivity(), R.string.confirm_connection_error +
                                    "" + error, Toast.LENGTH_LONG).show();
                        }
                    }
                });

        if (request != null) {
            if(isAdded()){
                displayCancelableMessage(getString(R.string.confirming_connection), request);
            }
        }
    }

    private void exitSetup() {
            _setup.removeListener(this);
            AylaAPIRequest request = _setup.exitSetup(
                    new Response.Listener<AylaAPIRequest.EmptyResponse>() {
                @Override
                public void onResponse(AylaAPIRequest.EmptyResponse response) {
                    _setup = null;
                }
            }, new ErrorListener() {
                @Override
                public void onErrorResponse(AylaError error) {
                    dismissCancelableMessage();
                }
            });
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

    public void register(){
        Snackbar.make(_messageTextView, String.format(getString(R.string.registration_type),
                _setupDevice.getRegistrationType()), Snackbar.LENGTH_SHORT).show();

        AylaRegistration registration = AylaNetworks.sharedInstance().getSessionManager
                (MainActivity.SESSION_NAME).getDeviceManager().getAylaRegistration();
        AylaRegistrationCandidate candidate = new AylaRegistrationCandidate();
        candidate.setDsn(_setupDevice.getDsn());
        candidate.setLanIp(_setupDevice.getLanIp());
        candidate.setRegistrationToken(_regToken);
        candidate.setSetupToken(_setupToken);
        candidate.setRegistrationType(getRegistrationType(_setupDevice.getRegistrationType()));
        //Optional parameters for sending location during registration
        if (ContextCompat.checkSelfPermission(getActivity(),
                android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {
            LocationManager locationManager =
                    (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
            Criteria criteria = new Criteria();
            criteria.setAccuracy(Criteria.ACCURACY_COARSE);
            Location currentLocation;
            List<String> locationProviders = locationManager.getAllProviders();
            for(String provider: locationProviders){
                currentLocation = locationManager.getLastKnownLocation(provider);
                if(currentLocation != null){
                    candidate.setLatitude(String.valueOf(currentLocation.getLatitude()));
                    candidate.setLongitude(String.valueOf(currentLocation.getLongitude()));
                    break;
                }
            }

        } else {
            requestPermissions(new String[]{ACCESS_COARSE_LOCATION}, REQUEST_LOCATION);
        }
        println(getString(R.string.registering));
        final AylaAPIRequest request = registration.registerCandidate(candidate,
                new Response.Listener<AylaDevice>() {
                    @Override
                    public void onResponse(AylaDevice response) {
                        println(getString(R.string.register_success) + response.toString());
                    }
                },
                new ErrorListener() {
                    @Override
                    public void onErrorResponse(AylaError error) {
                       if(isAdded() && !isRemoving()){
                           println(getString(R.string.register_error)+error);
                       }
                    }
                });
    }

    private RegistrationType getRegistrationType(String regType){
        RegistrationType type = RegistrationType.None;
        switch (regType){
            case "Same-LAN":
                type = RegistrationType.SameLan;
                break;
            case "Button-Push":
                type = RegistrationType.ButtonPush;
                break;
            case "AP-Mode":
                type = RegistrationType.APMode;
                break;
            case "Display":
                type = RegistrationType.Display;
                break;
            case "Dsn":
                type = RegistrationType.DSN;
                break;

        }
        return type;
    }

    private List getSecurityTypes(){
        List<String> list = new ArrayList<>(3);
        list.add(0, AylaSetup.WifiSecurityType.WPA2.stringValue());
        list.add(1, AylaSetup.WifiSecurityType.WPA.stringValue());
        list.add(2, AylaSetup.WifiSecurityType.WEP.stringValue());
        return list;
    }

    private void displayPasswordUI(final String ssid){
        final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(getString(R.string.select_security));
        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context
                .LAYOUT_INFLATER_SERVICE);
        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(),
                android.R.layout.simple_spinner_dropdown_item, getSecurityTypes());
        final View view = inflater.inflate(R.layout.device_security, null);

        final Spinner spinnerSecurity = (Spinner) view.findViewById(R.id.spinner_security);
        spinnerSecurity.setAdapter(adapter);
        final EditText edittextPassword = (EditText) view.findViewById(R.id.edittext_password);
        CheckBox cb = (CheckBox)view.findViewById(R.id.checkbox_show_password);
        cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    edittextPassword.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
                } else {
                    edittextPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                }
            }
        });



        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String password = edittextPassword.getText().toString();
                int pos = spinnerSecurity.getSelectedItemPosition();
                AylaSetup.WifiSecurityType securityType = AylaSetup.WifiSecurityType.NONE;
                switch (pos){
                    case 0:
                        securityType = AylaSetup.WifiSecurityType.WPA2;
                        break;
                    case 1:
                        securityType = AylaSetup.WifiSecurityType.WPA;
                        break;
                    case 2:
                        securityType = AylaSetup.WifiSecurityType.WEP;
                        break;
                }
                if(password == null || password.isEmpty()){
                    Snackbar.make(_messageTextView, R.string.please_enter_password,
                            Snackbar.LENGTH_SHORT).show();
                } else{
                    AylaAPIRequest request = sendConnectToDeviceRequest(ssid, password, securityType);
                    displayCancelableMessage(String.format(getString(R.string.connecting_to_device),
                            ssid), request);
                }

            }
        });
        builder.setView(view);
        builder.show();

    }


    private AylaAPIRequest sendConnectToDeviceRequest(final String ssid, String password,
                                                      AylaSetup.WifiSecurityType securityType){

        AylaAPIRequest request = _setup.connectToNewDevice(ssid, password, securityType,  10,
                new Response.Listener<AylaSetupDevice>() {
                    @Override
                    public void onResponse(AylaSetupDevice response) {
                        _textViewSSID.setText(ssid);
                        dismissCancelableMessage();
                        _setupDevice = response;
                        setState(State.Connected);
                    }
                },
                new ErrorListener() {
                    @Override
                    public void onErrorResponse(AylaError error) {
                        dismissCancelableMessage();
                        if(isAdded() && !isRemoving()){
                            if(error instanceof InternalError){
                                Toast.makeText(getActivity(), String.format("%s. %s",error
                                                .getMessage() , getString(R.string.retry_setup)),
                                        Toast.LENGTH_SHORT).show();
                            } else{
                                Toast.makeText(getActivity(), error.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            }

                        }
                    }
                });
        return request;
    }

}
