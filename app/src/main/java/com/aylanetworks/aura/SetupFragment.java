package com.aylanetworks.aura;


import android.content.Context;
import android.graphics.Color;
import java.util.TimeZone;
import android.net.wifi.ScanResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.util.Predicate;
import com.android.volley.Response;
import com.aylanetworks.aylasdk.AylaAPIRequest;
import com.aylanetworks.aylasdk.AylaDevice;
import com.aylanetworks.aylasdk.AylaDevice.RegistrationType;
import com.aylanetworks.aylasdk.AylaDeviceManager;
import com.aylanetworks.aylasdk.AylaLog;
import com.aylanetworks.aylasdk.AylaNetworks;
import com.aylanetworks.aylasdk.AylaTimeZone;
import com.aylanetworks.aylasdk.error.AppPermissionError;
import com.aylanetworks.aylasdk.error.AylaError;
import com.aylanetworks.aylasdk.error.ErrorListener;
import com.aylanetworks.aylasdk.setup.AylaRegistration;
import com.aylanetworks.aylasdk.setup.AylaRegistrationCandidate;
import com.aylanetworks.aylasdk.setup.AylaSetup;
import com.aylanetworks.aylasdk.setup.AylaSetupDevice;
import com.aylanetworks.aylasdk.setup.AylaWifiScanResults;
import com.aylanetworks.aylasdk.setup.AylaWifiStatus;
import com.aylanetworks.aylasdk.util.ObjectUtils;
import com.aylanetworks.aylasdk.util.PermissionUtils;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SetupFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SetupFragment extends Fragment implements  AylaSetup.DeviceWifiStateChangeListener {

    private static String setupDeviceType;
    private static String LOG_TAG = "SETUP_FRAGMENT";
    private String _scanRegex = "Ayla-[0-9a-zA-Z]{12}";
    private State _state;
    private String _setupToken;
    private AylaSetup _setup;
    private AylaSetupDevice _setupDevice;
    private ProgressBar _progressBar;
    private LinearLayout _layoutDeviceList;
    private LinearLayout _layoutPlugInEvb;
    private LinearLayout _layoutEVBList;
    private LinearLayout _layoutWifiScanList;
    private LinearLayout _layoutOtherWifi;
    private LinearLayout _layoutJoinWifi;
    private LinearLayout _layoutResetEvb;
    private LinearLayout _layoutPersonalizeEvb;
    private ListView _listViewDevices;
    private ListView _listViewDeviceType;
    private ListView _listViewAdvancedMenu;
    private ListView _listViewSavedNetworks;
    private ListView _listViewDiscoveredNetworks;
    private TextView _txtviewOther;
    private TextView _txtviewPersonalizeEVB;
    private EditText _editTextOtherSSID;
    private EditText _editTextOtherPassword;
    private EditText _editTextPassword;
    private TextView _textViewEnterPassword;
    private Button _btnSetup;
    private Spinner _timeZoneSpinner;
    private EditText _editTextDeviceName;
    private Snackbar _currentSnackbar;
    private TextView _messageTextView;
    private static final int WIFI_SCAN_DELAY = 5000;
    private String _selectedSSID;
    private int _reconnectCount;
    private static SetupFragment _mFragment;
    private String _timeZone;
    private CheckBox _checkboxWifiPassword;
    private CheckBox _checkboxOtherWifiPassword;

    @Override
    public void wifiStateChanged(String currentState) {
        AylaLog.d(LOG_TAG, "device state changed to "+currentState);
    }

    enum State {
        Uninitialized,
        Initialized,
        ScanDevices,
        ChooseDevice,
        Connected,
        OtherNetwork,
        ConnectToAp,
        StopAP,
        Reconnect,
        ConfirmDeviceConnection,
        Register,
        Registered,
        Reset
    }

    enum BGColors{
        Green,
        Gray
    }

    public SetupFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment SetupFragment.
     */
    public static SetupFragment newInstance() {
        _mFragment = new SetupFragment();
        return _mFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Make sure we have all required permissions
        checkAppPermissions();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_setup, container, false);
        _progressBar = (ProgressBar) view.findViewById(R.id.setup_progress);
        _layoutPlugInEvb = (LinearLayout) view.findViewById(R.id.evb_image_layout);
        _layoutDeviceList = (LinearLayout) view.findViewById(R.id.device_list_layout);
        _layoutEVBList = (LinearLayout) view.findViewById(R.id.evb_scan_layout);
        _layoutWifiScanList = (LinearLayout) view.findViewById(R.id.wifi_scan_results_layout);
        _listViewDeviceType = (ListView) view.findViewById(R.id.device_type_listview);
        _listViewAdvancedMenu = (ListView) view.findViewById(R.id.advanced_options_list);
        _listViewDevices = (ListView) view.findViewById(R.id.device_scan_listview);
        _listViewDiscoveredNetworks = (ListView) view.findViewById(R.id.discovered_networks_list);
        _layoutOtherWifi = (LinearLayout) view.findViewById(R.id.other_wifi__layout);
        _layoutJoinWifi = (LinearLayout) view.findViewById(R.id.join_wifi_layout);
        _layoutResetEvb = (LinearLayout) view.findViewById(R.id.reset_evb_layout);
        _layoutPersonalizeEvb = (LinearLayout) view.findViewById(R.id.personalize_evb_layout);
        _messageTextView = (TextView) view.findViewById(R.id.snackbar_textview);
        _txtviewOther= (TextView) view.findViewById(R.id.textview_other);
        _txtviewPersonalizeEVB = (TextView) view.findViewById(R.id.textview_register_success);
        _editTextOtherSSID = (EditText) view.findViewById(R.id.editText_network_name);
        _editTextOtherPassword = (EditText) view.findViewById(R.id.editText_network_password);
        _editTextPassword = (EditText) view.findViewById(R.id.editText_wifi_password);
        _textViewEnterPassword = (TextView) view.findViewById(R.id.textView_enter_password);
        _timeZoneSpinner = (Spinner) view.findViewById(R.id.timezone_spinner);
        _editTextDeviceName = (EditText) view.findViewById(R.id.edittext_product_name);
        _checkboxWifiPassword = (CheckBox)view.findViewById(R.id.wifi_show_password);
        _checkboxOtherWifiPassword = (CheckBox)view.findViewById(R.id.other_wifi_show_password);
        _btnSetup = (Button) view.findViewById(R.id.btn_setup);
        setState(State.Uninitialized);

        _btnSetup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (_state){
                    case Uninitialized:
                    case Initialized:
                    case ChooseDevice:
                        setState(State.ScanDevices);
                        break;
                    case Connected:
                        setState(State.ConnectToAp);
                        break;
                    case OtherNetwork:
                        String ssid = _editTextOtherSSID.getText().toString();
                        _selectedSSID = ssid;
                        String password = _editTextOtherPassword.getText().toString();
                        joinWifiClicked(ssid,password);
                        //Todo: Save this network according to user input
                        break;
                    case Reset:
                        exitSetup();
                        setState(State.Uninitialized);
                        break;
                    case ConnectToAp:
                        if(_editTextPassword.getVisibility() == View.VISIBLE){
                            String wifiPassword = _editTextPassword.getText().toString();
                            if(wifiPassword.length() < 8)
                            {
                                Toast.makeText(getContext(), getString(R.string.password_error),
                                        Toast.LENGTH_SHORT).show();
                            } else{
                                joinWifiClicked(_selectedSSID, wifiPassword);
                            }
                        } else{
                            joinWifiClicked(_selectedSSID, null);
                        }
                        break;
                    case Registered:
                        updateDeviceName();
                        updateTimeZone();
                        exitSetup();
                        getActivity().getSupportFragmentManager().popBackStack();
                        break;


                }
            }
        });

        _txtviewOther.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setState(State.OtherNetwork);
            }
        });

        _listViewDeviceType.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(position == 0){
                    setState(State.Initialized);
                } else{
                    Toast.makeText(getContext(), getString(R.string.not_available),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        _listViewAdvancedMenu.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                AdapterItem[] items = getAdvancedMenuItems();
                Fragment fragment;
                switch (items[position].getStringResId()){
                    case R.string.registration:
                        fragment = RegistrationFragment.newInstance();
                        MainActivity.sharedInstance().pushFragment(fragment);
                        break;
                    case R.string.wifi_setup:
                        fragment = SetupWizardFragment.newInstance();
                        MainActivity.sharedInstance().pushFragment(fragment);
                        break;
                    case R.string.step_by_step_setup:
                        fragment = WifiSetupFragment.newInstance();
                        MainActivity.sharedInstance().pushFragment(fragment);
                        break;
                }
            }
        });

        _listViewDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch (_state){
                    case ChooseDevice:
                        chooseDeviceClicked();
                        break;

                }
            }
        });

        _listViewDiscoveredNetworks.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                AylaWifiScanResults.Result selectedItem = (AylaWifiScanResults.Result)
                        _listViewDiscoveredNetworks.getAdapter().getItem(position);
                AylaLog.d(LOG_TAG, "Selected SSID "+selectedItem.ssid);
                _selectedSSID = selectedItem.ssid;
                if(selectedItem.isSecurityOpen()){
                    _editTextPassword.setVisibility(View.GONE);
                } else{
                    _editTextPassword.setVisibility(View.VISIBLE);
                }
                setState(State.ConnectToAp);
            }
        });

        _checkboxWifiPassword.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    _editTextPassword.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
                } else {
                    _editTextPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                }
            }
        });

        _checkboxOtherWifiPassword.setOnCheckedChangeListener(new CompoundButton
                .OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    _editTextOtherPassword.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
                } else {
                    _editTextOtherPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                }
            }
        });

        return view;

    }

    @Override
    public void onResume() {
        super.onResume();
        getView().setFocusableInTouchMode(true);
        getView().requestFocus();
        getView().setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if(event.getAction() == KeyEvent.ACTION_DOWN){
                    AylaLog.d(LOG_TAG, "BackButton hit _state "+_state);
                    switch (keyCode){
                        case KeyEvent.KEYCODE_BACK:
                            switch (_state){
                                case Initialized:
                                case ChooseDevice:
                                    setState(State.Uninitialized);
                                    break;
                                case OtherNetwork:
                                case ConnectToAp:
                                    setState(State.Connected);
                                    break;
                                case Uninitialized:
                                case Registered:
                                case Reset:
                                    exitSetup();
                                    getActivity().getSupportFragmentManager().popBackStack();
                                    break;
                                case Connected:
                                    setState(State.ScanDevices);
                                    break;

                            }
                    }
                    return true;
                }
               return false;
            }
        });
    }

    private void setPageTitle(String title){
        getActivity().setTitle(title);
    }

    private void scanForDevices() {

        _setup.addListener(this);
        AylaAPIRequest request = _setup.scanForAccessPoints(15, new Predicate<ScanResult>() {
            @Override
            public boolean apply(ScanResult scanResult) {
                return scanResult.SSID.matches(_scanRegex);

            }
        }, new Response.Listener<ScanResult[]>() {
            @Override
            public void onResponse(ScanResult[] response) {
                AylaLog.d(LOG_TAG, "scan response length "+response.length);
                dismissProgressBar();
                setState(State.ChooseDevice);
                _listViewDevices.setAdapter(new ScanResultAdapter(getActivity(), response));
                _listViewDevices.deferNotifyDataSetChanged();
            }
        }, new ErrorListener() {
            @Override
            public void onErrorResponse(AylaError error) {
                if(isAdded() && !isRemoving()){
                    Toast.makeText(getActivity(), error.getLocalizedMessage(),
                            Toast.LENGTH_SHORT).show();
                }
                dismissProgressBar();
                setState(State.Uninitialized);
            }
        });

    }
    private void chooseDeviceClicked() {
        // Get the scan info
        int selectedPos = _listViewDevices.getCheckedItemPosition();
        if (selectedPos == ListView.INVALID_POSITION) {
            Toast.makeText(getActivity(), getString(R.string.select_device),
                    Toast.LENGTH_LONG).show();
            return;
        }

        final ScanResult result = (ScanResult) _listViewDevices.getAdapter().getItem(selectedPos);
        AylaLog.d(LOG_TAG, "Setting up " + result.SSID);

        AylaAPIRequest request = _setup.connectToNewDevice(result.SSID, 10,
                new Response.Listener<AylaSetupDevice>() {
                    @Override
                    public void onResponse(AylaSetupDevice response) {
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
                            Toast.makeText(getActivity(), error.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                            setState(State.Reset);

                        }
                    }
                });

        displayCancelableMessage(String.format(getString(R.string.connecting_to_device),
                result.SSID), request);
    }


    public static class ScanResultAdapter extends ArrayAdapter<ScanResult> {
        public ScanResultAdapter(Context context, ScanResult[] objects) {
            super(context, R.layout.setup_scan_results_item, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ScanResult result = getItem(position);
            View view = convertView;
            if (view == null) {
                LayoutInflater inflater = (LayoutInflater) getContext()
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(R.layout.setup_scan_results_item, parent, false);
            }

            TextView textView = (TextView) view.findViewById(R.id.textview_scan_result);
            textView.setText(result.SSID);
            return view;
        }
    }

    public static class DeviceTypeAdapter extends ArrayAdapter<AdapterItem> {
        public DeviceTypeAdapter(Context context, AdapterItem[] objects) {
            super(context, R.layout.device_list_item, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            AdapterItem result = getItem(position);
            View view = convertView;
            if (view == null) {
                LayoutInflater inflater = (LayoutInflater) getContext()
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(R.layout.device_list_item, parent, false);
            }

            TextView textView = (TextView) view.findViewById(R.id.textview_device_type);
            textView.setText(result.getName());
            ImageView imgView = (ImageView) view.findViewById(R.id.device_type_icon);
            imgView.setImageResource(result.getImageResourceId());
            return view;
        }
    }

    public static class AdvancedMenuAdapter extends ArrayAdapter<AdapterItem> {
        public AdvancedMenuAdapter(Context context, AdapterItem[] objects) {
            super(context, R.layout.device_list_item, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            AdapterItem result = getItem(position);
            View view = convertView;
            if (view == null) {
                LayoutInflater inflater = (LayoutInflater) getContext()
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(R.layout.device_list_item, parent, false);
            }

            TextView textView = (TextView) view.findViewById(R.id.textview_device_type);
            textView.setText(result.getName());
            ImageView imgView = (ImageView) view.findViewById(R.id.device_type_icon);
            imgView.setImageResource(result.getImageResourceId());
            return view;
        }
    }


    public static class DeviceScanAdapter extends ArrayAdapter<AylaWifiScanResults.Result> {

        public DeviceScanAdapter(Context context, AylaWifiScanResults.Result[] items) {
            super(context, R.layout.wifi_network_scanresult_item, items);

        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            AylaWifiScanResults.Result result = getItem(position);
            View view = convertView;
            if (view == null) {
                LayoutInflater inflater = (LayoutInflater) getContext()
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(R.layout.wifi_network_scanresult_item, parent, false);
            }

            TextView textView = (TextView) view.findViewById(R.id.textview_wifi_scan_result);
            textView.setText(result.ssid);
            ImageView imgViewSecurity = (ImageView) view.findViewById(R.id.img_icon_lock);
            if(result.isSecurityOpen()){
                imgViewSecurity.setVisibility(View.GONE);
            } else{
                imgViewSecurity.setVisibility(View.VISIBLE);
            }

            return view;
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

        if(_setup != null){
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
                                                            _layoutEVBList.setVisibility(View.GONE);
                                                            setButtonTheme(BGColors.Gray);
                                                            _btnSetup.setText(getString(R.string.rescan_wifi));
                                                            _layoutWifiScanList.setVisibility(View.VISIBLE);
                                                            _listViewDiscoveredNetworks.setAdapter(new DeviceScanAdapter
                                                                    (getActivity(), response.results));
                                                            _listViewDiscoveredNetworks.deferNotifyDataSetChanged();
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
            displayCancelableMessage(getString(R.string.discovering_networks), request);
        }
    }


    private void joinWifiClicked(String ssid, String password) {
        AylaLog.d(LOG_TAG, "joinWifiClicked "+ssid+ " , "+password);
        if(password != null && password.isEmpty()){
            password = null;
        }
        _setupToken = ObjectUtils.generateRandomToken(8);
        AylaLog.d(LOG_TAG, "_setupToken "+_setupToken);
        AylaAPIRequest request = _setup.connectDeviceToService(ssid, password,
                _setupToken, null, null, 60,
                new Response.Listener<AylaWifiStatus>() {
                    @Override
                    public void onResponse(AylaWifiStatus response) {
                        if(isAdded()){
                            setState(State.StopAP);
                        }
                        dismissCancelableMessage();
                    }
                },
                new ErrorListener() {
                    @Override
                    public void onErrorResponse(AylaError error) {
                        dismissCancelableMessage();
                        if(error.getErrorType() == (AylaError.ErrorType.NetworkError)){
                            AylaLog.d(LOG_TAG, "connectDeviceToService() failed with error "+error);
                            setState(State.Reconnect);
                        } else{
                            setState(State.Reset);
                        }

                    }
                });
        displayCancelableMessage(String.format(getString(R.string.connecting_evb), ssid), request);
    }

    private void setState(State state){
        _state = state;
        switch (state){
            case Uninitialized:
                _layoutDeviceList.setVisibility(View.VISIBLE);
                _layoutPlugInEvb.setVisibility(View.GONE);
                _layoutEVBList.setVisibility(View.GONE);
                _layoutResetEvb.setVisibility(View.GONE);
                _layoutPersonalizeEvb.setVisibility(View.GONE);
                _layoutWifiScanList.setVisibility(View.GONE);
                _layoutJoinWifi.setVisibility(View.GONE);
                _layoutOtherWifi.setVisibility(View.GONE);
                _btnSetup.setVisibility(View.GONE);
                _listViewDeviceType.setAdapter(new DeviceTypeAdapter(getContext(), getSetupDeviceTypes()));
                _listViewAdvancedMenu.setAdapter(new AdvancedMenuAdapter(getContext(), getAdvancedMenuItems()));
                setPageTitle(getString(R.string.add_device));
                break;
            case Initialized:
                _layoutPlugInEvb.setVisibility(View.VISIBLE);
                _layoutDeviceList.setVisibility(View.GONE);
                _layoutEVBList.setVisibility(View.GONE);
                _layoutResetEvb.setVisibility(View.GONE);
                _layoutPersonalizeEvb.setVisibility(View.GONE);
                _layoutWifiScanList.setVisibility(View.GONE);
                _layoutJoinWifi.setVisibility(View.GONE);
                _layoutOtherWifi.setVisibility(View.GONE);
                _btnSetup.setText(getString(R.string.powered_evb));
                _btnSetup.setVisibility(View.VISIBLE);
                setButtonTheme(BGColors.Green);
                setPageTitle(getString(R.string.plug_in_evb));
                _reconnectCount = 0;
                break;
            case ScanDevices:
                try {
                    if(_setup == null){
                        _setup = new AylaSetup(getActivity(), MainActivity.getSession());
                    }
                } catch (AylaError aylaError) {
                    aylaError.printStackTrace();
                    if(aylaError instanceof AppPermissionError){
                        Toast.makeText(getContext(), getString(R.string.missing_permissions),
                                Toast.LENGTH_SHORT).show();
                        checkAppPermissions();
                        setState(State.Initialized);
                        return;
                    }
                }
                _progressBar.setVisibility(View.VISIBLE);
                _listViewDevices.setAdapter(null);
                _layoutPlugInEvb.setVisibility(View.GONE);
                _layoutWifiScanList.setVisibility(View.GONE);
                _layoutDeviceList.setVisibility(View.GONE);
                scanForDevices();
                break;
            case ChooseDevice:
                setPageTitle(getString(R.string.select_evb_title));
                _layoutEVBList.setVisibility(View.VISIBLE);
                _layoutDeviceList.setVisibility(View.GONE);
                _layoutPlugInEvb.setVisibility(View.GONE);
                _layoutPersonalizeEvb.setVisibility(View.GONE);
                _layoutWifiScanList.setVisibility(View.GONE);
                _btnSetup.setText(getString(R.string.rescan_evb));
                setButtonTheme(BGColors.Gray);
                break;
            case Connected:
                _layoutJoinWifi.setVisibility(View.GONE);
                _layoutOtherWifi.setVisibility(View.GONE);
                setPageTitle(getString(R.string.select_wifi));
                startDeviceScanForAPs();
                break;
            case OtherNetwork:
                setPageTitle(getString(R.string.other_wifi));
                _layoutWifiScanList.setVisibility(View.GONE);
                _layoutOtherWifi.setVisibility(View.VISIBLE);
                setButtonTheme(BGColors.Green);
                _btnSetup.setText(R.string.join_wifi);
                _textViewEnterPassword.setText(
                        String.format(getString(R.string.enter_wifi_password_text), _selectedSSID));
                break;
            case ConnectToAp:
                setPageTitle(getString(R.string.enter_wifi_password));
                _textViewEnterPassword.setText(String.format(getString(
                        R.string.enter_wifi_password_text), _selectedSSID));
                _layoutWifiScanList.setVisibility(View.GONE);
                _layoutJoinWifi.setVisibility(View.VISIBLE);
                setButtonTheme(BGColors.Green);
                _btnSetup.setText(R.string.join_wifi);
                break;
            case StopAP:
                disconnectAP();
                break;
            case Reconnect:
                reconnectToOriginalNetwork();
                break;
            case Reset:
                setPageTitle(getString(R.string.reset_evb_title));
                _layoutResetEvb.setVisibility(View.VISIBLE);
                _layoutEVBList.setVisibility(View.GONE);
                _layoutWifiScanList.setVisibility(View.GONE);
                _layoutOtherWifi.setVisibility(View.GONE);
                _layoutJoinWifi.setVisibility(View.GONE);
                setButtonTheme(BGColors.Green);
                _btnSetup.setText(R.string.repeat_setup);
                break;
            case ConfirmDeviceConnection:
                confirmDeviceConnection();
                break;
            case Register:
                register();
                break;
            case Registered:
                setPageTitle(getString(R.string.personalize_evb));
                _txtviewPersonalizeEVB.setText(String.format(getString(R.string
                        .successfully_registered), _selectedSSID));
                _layoutOtherWifi.setVisibility(View.GONE);
                _layoutPersonalizeEvb.setVisibility(View.VISIBLE);
                _layoutJoinWifi.setVisibility(View.GONE);
                _timeZoneSpinner.setAdapter(new ArrayAdapter<>(getActivity(), android.R.layout
                        .simple_list_item_1, TimeZone.getAvailableIDs()));
                setDeviceTimeZone();
                setButtonTheme(BGColors.Green);
                _btnSetup.setText(getString(R.string.return_to_devices));
        }
    }


    private void setButtonTheme(BGColors bgColor){
        switch(bgColor){
            case Green:
                _btnSetup.setBackground(getResources().getDrawable(R.drawable
                        .setup_screen_button_green));
                _btnSetup.setTextColor(Color.WHITE);
                break;
            case Gray:
                _btnSetup.setBackground(getResources().getDrawable(R.drawable
                        .setup_screen_button_grey));
                _btnSetup.setTextColor(getResources().getColor(R.color.ayla_green));
        }

    }

    private void dismissProgressBar(){
        if(_progressBar != null){
            _progressBar.setVisibility(View.GONE);
        }
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

    @Override
    public void onDetach() {
        super.onDetach();
        if(_setup != null){
            dismissCancelableMessage();
            exitSetup();
        }
    }

    private void exitSetup() {
        if(_setup != null){
            _setup.removeListener(this);
            AylaAPIRequest request = _setup.exitSetup(
                    new Response.Listener<AylaAPIRequest.EmptyResponse>() {
                        @Override
                        public void onResponse(AylaAPIRequest.EmptyResponse response) {
                        }
                    }, new ErrorListener() {
                        @Override
                        public void onErrorResponse(AylaError error) {
                            dismissCancelableMessage();
                        }
                    });
        }
        _setup = null;
    }

    private void disconnectAP(){
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
                            AylaLog.d(LOG_TAG, getString(R.string.error_reconnecting) +error);
                        }
                    } else{
                        AylaLog.d(LOG_TAG, getString(R.string.error_reconnecting) +error);
                    }
                }

            }
        });
        displayCancelableMessage(getString(R.string.reconnecting), request);

    }

    private void confirmDeviceConnection() {
        AylaAPIRequest request = _setup.confirmDeviceConnected(30, _setupDevice.getDsn(),
                _setupToken, new Response.Listener<AylaSetupDevice>() {
                    @Override
                    public void onResponse(AylaSetupDevice response) {
                        _setupDevice.setRegistrationType(response.getRegistrationType());
                        _setupDevice.setLanIp(response.getLanIp());
                        dismissCancelableMessage();

                        //Exit setup so LAN mode can resume
                        exitSetup();
                        AylaDeviceManager deviceManager = AylaNetworks.sharedInstance()
                                .getSessionManager(MainActivity.SESSION_NAME).getDeviceManager();
                        setState(State.Register);
                    }
                },
                new ErrorListener() {
                    @Override
                    public void onErrorResponse(AylaError error) {
                        dismissCancelableMessage();
                        if(isAdded() && !isRemoving()){
                            Toast.makeText(getActivity(), R.string.confirm_connection_error +
                                    "" + error, Toast.LENGTH_LONG).show();
                        }
                        exitSetup();
                    }
                });

        if (request != null) {
            if(isAdded()){
                displayCancelableMessage(getString(R.string.confirming_connection_status), request);
            }
        }
    }

    public void register(){
        AylaRegistration registration = AylaNetworks.sharedInstance().getSessionManager
                (MainActivity.SESSION_NAME).getDeviceManager().getAylaRegistration();
        AylaRegistrationCandidate candidate = new AylaRegistrationCandidate();
        candidate.setDsn(_setupDevice.getDsn());
        candidate.setLanIp(_setupDevice.getLanIp());
        candidate.setSetupToken(_setupToken);
        candidate.setRegistrationType(getRegistrationType(_setupDevice.getRegistrationType()));
        final AylaAPIRequest request = registration.registerCandidate(candidate,
                new Response.Listener<AylaDevice>() {
                    @Override
                    public void onResponse(AylaDevice response) {
                        dismissCancelableMessage();
                        setState(State.Registered);
                    }
                },
                new ErrorListener() {
                    @Override
                    public void onErrorResponse(AylaError error) {
                        if(isAdded() && !isRemoving()){
                            dismissCancelableMessage();
                            Toast.makeText(getContext(), "Error adding device to your account",
                                    Toast.LENGTH_SHORT).show();
                            setState(State.Reset);
                        }
                    }
                });

        displayCancelableMessage(getString(R.string.adding_evb), request);
    }

    private void setDeviceTimeZone() {
        AylaDeviceManager deviceManager = AylaNetworks.sharedInstance()
                .getSessionManager(MainActivity.SESSION_NAME).getDeviceManager();
        AylaDevice device = deviceManager.deviceWithDSN(_setupDevice.getDsn());
        device.fetchTimeZone(new Response.Listener<AylaTimeZone>() {
            @Override
            public void onResponse(AylaTimeZone response) {
                _timeZone = response.tzId;
                String[] timezones = TimeZone.getAvailableIDs();
                int count = -1;
                for (String tzId : timezones) {
                    count++;
                    if (tzId.equals(response.tzId)) {
                        break;
                    }
                }
                _timeZoneSpinner.setSelection(count);
            }
        }, new ErrorListener() {
            @Override
            public void onErrorResponse(AylaError error) {
            }
        });
    }

    private void updateTimeZone(){
        int selecetdPos = _timeZoneSpinner.getSelectedItemPosition();
        String[] timezones = TimeZone.getAvailableIDs();
        final String selectedTimezone = timezones[selecetdPos];
        if(!selectedTimezone.equals(_timeZone)){
            AylaDeviceManager deviceManager = AylaNetworks.sharedInstance()
                    .getSessionManager(MainActivity.SESSION_NAME).getDeviceManager();
            AylaDevice device = deviceManager.deviceWithDSN(_setupDevice.getDsn());
            device.updateTimeZone(selectedTimezone, new Response.Listener<AylaTimeZone>() {
                @Override
                public void onResponse(AylaTimeZone response) {
                    AylaLog.d(LOG_TAG, "Device timezone updated to "+selectedTimezone);
                }
            }, new ErrorListener() {
                @Override
                public void onErrorResponse(AylaError error) {
                    AylaLog.d(LOG_TAG, "Device timezone update failed");
                }
            });
        }
    }

    private void updateDeviceName(){
        final String deviceName = _editTextDeviceName.getText().toString();
        if(deviceName != null){
            AylaDeviceManager deviceManager = AylaNetworks.sharedInstance()
                    .getSessionManager(MainActivity.SESSION_NAME).getDeviceManager();
            AylaDevice device = deviceManager.deviceWithDSN(_setupDevice.getDsn());
            device.updateProductName(deviceName, new Response.Listener<AylaAPIRequest.EmptyResponse>() {
                @Override
                public void onResponse(AylaAPIRequest.EmptyResponse response) {
                    AylaLog.d(LOG_TAG, "Device name updated to "+deviceName);
                }
            }, new ErrorListener() {
                @Override
                public void onErrorResponse(AylaError error) {

                }
            });
        }
    }

    private void checkAppPermissions(){
        AppPermissionError error = null;
        error = PermissionUtils.checkPermissions(getActivity(),
                AylaSetup.SETUP_REQUIRED_PERMISSIONS);
        if (error != null) {
            ActivityCompat.requestPermissions(getActivity(), AylaSetup.SETUP_REQUIRED_PERMISSIONS,
                    0);
        }
    }

    class AdapterItem{
        private int stringResId;
        private String name;
        private int imageResourceId;

        public AdapterItem(int stringResId, String name, int imageResourceId) {
            this.stringResId = stringResId;
            this.name = name;
            this.imageResourceId = imageResourceId;
        }

        public String getName() {
            return name;
        }

        public int getStringResId() {
            return stringResId;
        }

        public int getImageResourceId() {
            return imageResourceId;
        }
    }

    //Add new types of devices here.
    private AdapterItem[] getSetupDeviceTypes(){
        AdapterItem[] itemsList = new AdapterItem[]{
                new AdapterItem(R.string.ayla_evb, getString(R.string.ayla_evb),
                        R.drawable.icon_device_evb),
                new AdapterItem(R.string.smart_plug, getString(R.string.smart_plug),
                        R.drawable.icon_device_plug),
                new AdapterItem(R.string.gateway, getString(R.string.gateway),
                        R.drawable.icon_device_gateway),
                new AdapterItem(R.string.node, getString(R.string.node),
                        R.drawable.icon_device_node)};

        return itemsList;

    }

    private AdapterItem[] getAdvancedMenuItems() {
        AdapterItem[] items = new AdapterItem[]{
                new AdapterItem(R.string.registration, getString(R.string.registration),
                        R.drawable.ic_add_circle_black_48dp),
                new AdapterItem(R.string.wifi_setup, getString(R.string.wifi_setup),
                        R.drawable.icon_wifi),
                new AdapterItem(R.string.step_by_step_setup,
                        getString(R.string.step_by_step_setup), R.drawable.icon_wifi)};
        return items;
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
}
