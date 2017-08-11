package com.aylanetworks.aura;
/*
 * Android_AylaSDK
 *
 * Copyright 2016 Ayla Networks, all rights reserved
 */

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.util.Predicate;
import com.android.volley.Response;
import com.aylanetworks.aura.localdevice.AuraLocalDeviceManager;
import com.aylanetworks.aylasdk.AylaAPIRequest;
import com.aylanetworks.aylasdk.AylaDevice;
import com.aylanetworks.aylasdk.AylaDeviceGateway;
import com.aylanetworks.aylasdk.AylaDeviceManager;
import com.aylanetworks.aylasdk.AylaLog;
import com.aylanetworks.aylasdk.AylaNetworks;
import com.aylanetworks.aylasdk.change.Change;
import com.aylanetworks.aylasdk.error.AppPermissionError;
import com.aylanetworks.aylasdk.error.AylaError;
import com.aylanetworks.aylasdk.error.ErrorListener;
import com.aylanetworks.aylasdk.setup.AylaRegistration;
import com.aylanetworks.aylasdk.setup.AylaRegistrationCandidate;
import com.aylanetworks.aylasdk.setup.AylaSetup;
import com.aylanetworks.aylasdk.util.EmptyListener;
import com.aylanetworks.aylasdk.util.PermissionUtils;
import com.aylanetworks.aylasdk.localdevice.AylaLocalDevice;
import com.aylanetworks.aylasdk.localdevice.AylaLocalDeviceManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;


/**
 * Registration methods are exercised in this fragment.
 */
public class RegistrationFragment extends Fragment {
    public static final String AYLA_OEM_ID = "0dfc7900";

    private static final String LOG_TAG = "Registration";
    private static final String[] REG_TYPES = {"SameLan", "ButtonPush", "APMode", "Display",
            "DSN", "Node"};
    private static final int REQUEST_LOCATION = 0;
    private EditText _dsnEditText;
    private EditText _regtokenEditText;

    private Spinner _regtypeSpinner;
    private TextView _messageTextView;

    private TextView _dsnLabel;
    private Spinner _gatewaySpinner;

    private AylaRegistrationCandidate _fetchedCandidate;
    private AylaRegistrationCandidate[] _fetchedNodeCandidates;

    private AylaLocalDevice[] _discoveredLocalDevices;

    public RegistrationFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment RegistrationFragment.
     */
    public static RegistrationFragment newInstance() {
        RegistrationFragment fragment = new RegistrationFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_registration, container, false);

        _dsnEditText = (EditText) view.findViewById(R.id.dsn_edit_text);
        _regtokenEditText = (EditText) view.findViewById(R.id.regtoken_edit_text);

        _dsnLabel = (TextView) view.findViewById(R.id.dsn_label);
        _gatewaySpinner = (Spinner) view.findViewById(R.id.dsn_spinner);

        // Get a list of gateways registered to this account
        AylaDeviceManager dm = MainActivity.getDeviceManager();
        if (dm != null) {
            List<AylaDevice> gatewayList = dm.getDevices(new Predicate<AylaDevice>() {
                @Override
                public boolean apply(AylaDevice device) {
                    return device.isGateway();
                }
            });
            String[] gatewayDSNs = new String[gatewayList.size()];
            int i = 0;
            for (AylaDevice gw : gatewayList) {
                gatewayDSNs[i++] = gw.getDsn();
            }
            _gatewaySpinner.setAdapter(new ArrayAdapter<>(getContext(),
                    android.R.layout.simple_spinner_item, gatewayDSNs));
        }


        _regtypeSpinner = (Spinner) view.findViewById(R.id.regtype_spinner);

        // See if we have the local device manager present
        AylaLocalDeviceManager ldm = (AylaLocalDeviceManager)AylaNetworks.sharedInstance()
                .getPlugin(AylaLocalDeviceManager.PLUGIN_ID_LOCAL_DEVICE);
        List<String> regTypes = new ArrayList<>(Arrays.asList(REG_TYPES));
        if (ldm != null) {
            regTypes.add(AylaDevice.RegistrationType.Local.stringValue());
        }

        _regtypeSpinner.setAdapter(new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, regTypes));
        _regtypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                _fetchedNodeCandidates = null;
                if (AylaDevice.RegistrationType.valueOf(
                        _regtypeSpinner.getSelectedItem().toString()) ==
                        AylaDevice.RegistrationType.Node) {
                    _gatewaySpinner.setVisibility(View.VISIBLE);
                    _dsnEditText.setVisibility(View.GONE);
                    _dsnLabel.setText(R.string.dsn_gateway_label);
                } else {
                    _gatewaySpinner.setVisibility(View.GONE);
                    _dsnEditText.setVisibility(View.VISIBLE);
                    _dsnLabel.setText(R.string.dsn_label);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                _gatewaySpinner.setVisibility(View.GONE);
                _dsnEditText.setVisibility(View.VISIBLE);
                _dsnLabel.setText(R.string.dsn_label);
            }
        });

        _messageTextView = (TextView) view.findViewById(R.id.textview);

        Button registerButton = (Button) view.findViewById(R.id.register_button);
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerClicked();
            }
        });

        Button fetchCandidates = (Button) view.findViewById(R.id.fetch_candidates_button);
        fetchCandidates.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fetchCandidateClicked();
            }
        });

        return view;
    }

    private void promptRegisterNodes() {
        // Show the list of nodes to register and prompt the user for which ones
        final String[] dsns = new String[_fetchedNodeCandidates.length];
        final boolean[] checked = new boolean[_fetchedNodeCandidates.length];
        int i = 0;
        for (AylaRegistrationCandidate candidate : _fetchedNodeCandidates) {
            checked[i] = true;
            dsns[i++] = candidate.getProductName() + " [" + candidate.getDsn() + "]";
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.sharedInstance());
        builder.setTitle(R.string.choose_nodes)
                .setMultiChoiceItems(dsns, checked,
                        new DialogInterface.OnMultiChoiceClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                                Log.d(LOG_TAG, "Click: " + dsns[which]);
                            }
                        })
                .setPositiveButton(R.string.register, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Get a list of the selected nodes and register them
                        List<AylaRegistrationCandidate> regCandidates = new ArrayList<>();
                        int i = 0;
                        for (AylaRegistrationCandidate candidate : _fetchedNodeCandidates) {
                            if (checked[i++]) {
                                regCandidates.add(candidate);
                            }
                        }
                        if (regCandidates.size() > 0) {
                            View v = getView();
                            if (v != null) {
                                Snackbar sb = Snackbar.make(v, R.string.registering,
                                        Snackbar.LENGTH_INDEFINITE);
                                sb.show();
                                registerNodes(regCandidates, sb);
                            }
                        }
                    }
                }).show();
    }

    private void registerNodes(final List<AylaRegistrationCandidate> regCandidates,
                               final Snackbar sb) {
        if (regCandidates.size() == 0) {
            // We're done
            if (sb != null) {
                sb.dismiss();
            }
            View v = getView();
            if (v != null) {
                Snackbar.make(v, MainActivity.sharedInstance().getString(R.string
                        .register_success), Snackbar.LENGTH_LONG).show();
            }

            _fetchedNodeCandidates = null;
            return;
        }

        AylaRegistrationCandidate candidate = regCandidates.remove(0);
        if (sb != null) {
            String regText = MainActivity.sharedInstance().getResources().getString(R.string
                    .registering_device, candidate.getDsn());
            sb.setText(regText);
        }

        AylaRegistration registration = AylaNetworks.sharedInstance().getSessionManager
                (MainActivity.SESSION_NAME).getDeviceManager().getAylaRegistration();
        registration.registerCandidate(candidate, new Response.Listener<AylaDevice>() {
                    @Override
                    public void onResponse(AylaDevice response) {
                        registerNodes(regCandidates, sb);
                    }
                },
                new ErrorListener() {
                    @Override
                    public void onErrorResponse(AylaError error) {
                        if (sb != null) {
                            sb.dismiss();
                        }
                        View v = getView();
                        if (v != null) {
                            Snackbar.make(v, error.getMessage(), Snackbar.LENGTH_LONG).show();
                        }
                        _fetchedNodeCandidates = null;
                    }
                });
    }

    private void registerClicked() {
        if (_fetchedNodeCandidates != null) {
            promptRegisterNodes();
            return;
        }

        String dsn = _dsnEditText.getText().toString();
        String regtoken = _regtokenEditText.getText().toString();

        AylaRegistration registration = AylaNetworks.sharedInstance().getSessionManager
                (MainActivity.SESSION_NAME).getDeviceManager().getAylaRegistration();
        final AylaRegistrationCandidate candidate;
        if (_fetchedCandidate != null) {
            candidate = _fetchedCandidate;
        } else {
            candidate = new AylaRegistrationCandidate();
        }

        if (!TextUtils.isEmpty(dsn)) {
            candidate.setDsn(dsn);
        }
        if (!TextUtils.isEmpty(regtoken)) {
            candidate.setRegistrationToken(regtoken);
        }

        //Optional parameters for sending location during registration
        if (ContextCompat.checkSelfPermission(getContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationManager locationManager =
                    (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
            Criteria criteria = new Criteria();
            criteria.setAccuracy(Criteria.ACCURACY_COARSE);
            Location currentLocation;
            List<String> locationProviders = locationManager.getAllProviders();
            for (String provider : locationProviders) {
                currentLocation = locationManager.getLastKnownLocation(provider);
                if (currentLocation != null) {
                    candidate.setLatitude(String.valueOf(currentLocation.getLatitude()));
                    candidate.setLongitude(String.valueOf(currentLocation.getLongitude()));
                    break;
                }
            }

        } else {
            requestPermissions(new String[]{ACCESS_COARSE_LOCATION}, REQUEST_LOCATION);
        }

        candidate.setRegistrationType(AylaDevice.RegistrationType.valueOf(
                (String) _regtypeSpinner.getSelectedItem()));

        View v = getView();
        if (v == null) {
            return;
        }

        final Snackbar snackbar = Snackbar.make(v, "Registering device...",
                Snackbar.LENGTH_INDEFINITE);

        final AylaAPIRequest request = registration.registerCandidate(candidate,
                new Response.Listener<AylaDevice>() {
                    @Override
                    public void onResponse(AylaDevice response) {
                        logMessage("Successfuly registered device: " + response.toString());
                        snackbar.dismiss();
                        View v = getView();
                        if (v != null) {
                            Snackbar.make(v, "Success! The device was registered.",
                                    Snackbar.LENGTH_SHORT).show();
                        }
                    }
                },
                new ErrorListener() {
                    @Override
                    public void onErrorResponse(AylaError error) {
                        logError(error);
                        snackbar.dismiss();
                        View v = getView();
                        if (v != null) {
                            Snackbar.make(v, error.getMessage(), Snackbar.LENGTH_SHORT).show();
                        }
                    }
                });

        if (request != null) {
            snackbar.setAction("Cancel", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    request.cancel();
                    snackbar.dismiss();
                    logMessage("Canceled");
                }
            });
            snackbar.show();
        }
    }

    private AylaDeviceGateway selectedGateway() {
        String dsn = _gatewaySpinner.getSelectedItem().toString();
        AylaDeviceManager dm = MainActivity.getDeviceManager();
        if (dm == null) {
            return null;
        }

        return (AylaDeviceGateway) dm.deviceWithDSN(dsn);
    }

    private void fetchNodeCandidates() {
        _fetchedNodeCandidates = null;

        View v = getView();
        AylaDeviceGateway gateway = selectedGateway();
        if (gateway != null && v != null) {
            final Snackbar snackbar = Snackbar.make(v, "Fetching candidate...",
                    Snackbar.LENGTH_INDEFINITE);

            final AylaAPIRequest request = gateway.fetchRegistrationCandidates(
                    new Response.Listener<AylaRegistrationCandidate[]>() {
                        @Override
                        public void onResponse(AylaRegistrationCandidate[] response) {
                            snackbar.dismiss();
                            logMessage("Received " + response.length + " node candidates");
                            _fetchedNodeCandidates = response;
                            for (AylaRegistrationCandidate candidate : _fetchedNodeCandidates) {
                                candidate.setRegistrationType(AylaDevice.RegistrationType.Node);
                            }

                            // Press the button for them
                            registerClicked();
                        }
                    },
                    new ErrorListener() {
                        @Override
                        public void onErrorResponse(AylaError error) {
                            snackbar.dismiss();
                            logError(error);
                        }
                    });

            if (request != null) {
                snackbar.setAction("Cancel", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        request.cancel();
                        snackbar.dismiss();
                        logMessage("Canceled");
                    }
                });
                snackbar.show();
            }
        }
    }

    private void fetchCandidateClicked() {
        AylaRegistration registration = AylaNetworks.sharedInstance().getSessionManager
                (MainActivity.SESSION_NAME).getDeviceManager().getAylaRegistration();

        AylaDevice.RegistrationType regType = AylaDevice.RegistrationType.valueOf(
                (String) _regtypeSpinner.getSelectedItem());

        if (regType == AylaDevice.RegistrationType.Local) {
            // Make sure we have all required permissions
            AppPermissionError error = null;
            error = PermissionUtils.checkPermissions(getContext(),
                    AylaSetup.SETUP_REQUIRED_PERMISSIONS);
            if (error != null) {
                ActivityCompat.requestPermissions(getActivity(), AylaSetup.SETUP_REQUIRED_PERMISSIONS,
                        0);
                return;
            }

            // Do a local scan
            AuraLocalDeviceManager ldm = (AuraLocalDeviceManager)AylaNetworks.sharedInstance()
                    .getPlugin(AylaLocalDeviceManager.PLUGIN_ID_LOCAL_DEVICE);

            final Snackbar sb = Snackbar.make(getView(), "Scanning for BLE devices...", Snackbar
                    .LENGTH_INDEFINITE);

            final AylaAPIRequest request = ldm.findLocalDevices(5000, new Response
                    .Listener<AylaLocalDevice[]>() {
                @Override
                public void onResponse(AylaLocalDevice[] response) {
                    AylaLog.d(LOG_TAG, "Local devices: " + response.length);
                    _discoveredLocalDevices = response;
                    sb.dismiss();
                    promptRegisterLocal();
                }
            }, new ErrorListener() {
                @Override
                public void onErrorResponse(AylaError error) {
                    AylaLog.e(LOG_TAG, "Scan error: " + error);
                    logMessage("Scan error: " +  error);
                }
            });

            if (request != null) {
                sb.setAction(android.R.string.cancel, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        request.cancel();
                    }
                });
                sb.show();
            }
            return;
        }

        String dsn;
        if (regType == AylaDevice.RegistrationType.Node) {
            fetchNodeCandidates();
            return;
        } else {
            dsn = _dsnEditText.getText().toString();
        }

        if (dsn.length() == 0) {
            dsn = null;
        }

        View v = getView();
        if (v == null) {
            return;
        }

        final Snackbar snackbar = Snackbar.make(v, "Fetching candidate...",
                Snackbar.LENGTH_INDEFINITE);

        _fetchedCandidate = null;

        final AylaAPIRequest request = registration.fetchCandidate(dsn, regType,
                new Response.Listener<AylaRegistrationCandidate>() {
                    @Override
                    public void onResponse(AylaRegistrationCandidate response) {
                        _fetchedCandidate = response;
                        snackbar.dismiss();
                        _dsnEditText.setText(response.getDsn());
                        int selectedPos = -1;
                        SpinnerAdapter adapter = _regtypeSpinner.getAdapter();
                        for (int i = 0; i < adapter.getCount(); i++) {
                            String regTypeString = (String) adapter.getItem(i);
                            if (TextUtils.equals(regTypeString,
                                    response.getRegistrationType().stringValue())) {
                                selectedPos = i;
                                break;
                            }
                        }
                        if (selectedPos != -1) {
                            _regtypeSpinner.setSelection(selectedPos);
                        }
                        _regtokenEditText.setText(response.getRegistrationToken());
                        logMessage("Registration candidate received: [" + response.getDsn() +
                                "] " + response.getLan_ip() + " regtoken: " +
                                response.getRegistrationToken());

                    }
                },
                new ErrorListener() {
                    @Override
                    public void onErrorResponse(AylaError error) {
                        snackbar.dismiss();
                        logError(error);
                    }
                });

        if (request != null) {
            snackbar.setAction("Cancel", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    request.cancel();
                    snackbar.dismiss();
                    logMessage("Canceled");
                }
            });
            snackbar.show();
        }
    }

    private void promptRegisterLocal() {
        if (_discoveredLocalDevices == null || _discoveredLocalDevices.length == 0) {
            return;
        }

        // Show the list of nodes to register and prompt the user for which ones
        final String[] deviceNames = new String[_discoveredLocalDevices.length];
        final boolean[] checked = new boolean[_discoveredLocalDevices.length];
        int i = 0;
        for (AylaLocalDevice localDevice : _discoveredLocalDevices) {
            checked[i] = true;
            deviceNames[i++] = localDevice.getProductName() + " [" + localDevice.getHardwareAddress() + "]";
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.sharedInstance());
        builder.setTitle(R.string.choose_local_devices)
                .setMultiChoiceItems(deviceNames, checked,
                        new DialogInterface.OnMultiChoiceClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                                Log.d(LOG_TAG, "Click: " + deviceNames[which]);
                            }
                        })
                .setPositiveButton(R.string.register, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Get a list of the selected local devices and register them
                        List<AylaLocalDevice> devicesToRegister = new ArrayList<>();
                        int i = 0;
                        for (AylaLocalDevice device : _discoveredLocalDevices) {
                            if (checked[i++]) {
                                devicesToRegister.add(device);
                            }
                        }
                        if (devicesToRegister.size() > 0) {
                            registerLocalDevices(devicesToRegister);
                        }
                    }
                }).show();
    }

    private void registerLocalDevices(final List<AylaLocalDevice> devicesToRegister) {
        if (devicesToRegister == null || devicesToRegister.size() == 0) {
            AylaLog.d(LOG_TAG, "Finished registering local devices");
            return;
        }

        final AylaLocalDevice deviceToRegister = devicesToRegister.remove(0);

        Snackbar sb = null;
        View view = getView();
        if (view != null) {
            sb = Snackbar.make(view, getString(R.string.connecting_to_device,
                    deviceToRegister.toString()), Snackbar.LENGTH_INDEFINITE);
        }

        final Snackbar snackbar = sb;

        // First connect to the device
        final AylaAPIRequest req = deviceToRegister.connectLocal(
                new Response.Listener<AylaAPIRequest.EmptyResponse>() {
            @Override
            public void onResponse(AylaAPIRequest.EmptyResponse response) {
                AylaLog.d(LOG_TAG, "Connected!");

                    AuraLocalDeviceManager ldm = (AuraLocalDeviceManager)AylaNetworks.sharedInstance()
                            .getPlugin(AylaLocalDeviceManager.PLUGIN_ID_LOCAL_DEVICE);
                    ldm.registerLocalDevice(
                            MainActivity.sharedInstance().getSession(),
                            deviceToRegister,
                            AYLA_OEM_ID,
                            new Response.Listener<AylaLocalDevice>() {
                                @Override
                                public void onResponse(AylaLocalDevice response) {
                                    AylaLog.i(LOG_TAG, "Registration complete for " + deviceToRegister);
                                    snackbar.dismiss();
                                    Toast.makeText(MainActivity.sharedInstance(),
                                            deviceToRegister.getProductName() + " registered!",
                                            Toast.LENGTH_LONG).show();
                                    registerLocalDevices(devicesToRegister);
                                }
                            },
                            new ErrorListener() {
                                @Override
                                public void onErrorResponse(AylaError error) {
                                    snackbar.dismiss();
                                    Toast.makeText(MainActivity.sharedInstance(), "Error " +
                                                    "registering " + deviceToRegister
                                                    .getProductName() + ": " + error .getMessage(),
                                            Toast.LENGTH_LONG).show();
                                    registerLocalDevices(devicesToRegister);
                                }
                            });
                }
        }, new ErrorListener() {
            @Override
            public void onErrorResponse(AylaError error) {
                EmptyListener<AylaAPIRequest.EmptyResponse> emptyListener = new EmptyListener<>();
                deviceToRegister.disconnectLocal(emptyListener, emptyListener);
                snackbar.dismiss();
                AylaLog.e(LOG_TAG, "Error connecting to " + deviceToRegister.getProductName() +
                        ": " + error.getMessage());
            }
        });

        if (req != null) {
            snackbar.setAction(android.R.string.cancel, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    req.cancel();
                    snackbar.dismiss();
                }
            });
            snackbar.show();
        }
    }

    private void fetchRegtokenClicked() {
        if (_fetchedCandidate == null) {
            Snackbar.make(getView(), "Fetch a candidate first", Snackbar.LENGTH_SHORT).show();
            return;
        }

        AylaRegistration registration = AylaNetworks.sharedInstance().getSessionManager
                (MainActivity.SESSION_NAME).getDeviceManager().getAylaRegistration();

    }

    private void logMessage(String message) {
        _messageTextView.setText(_messageTextView.getText() + "\n" + message);
    }

    private void logError(AylaError error) {
        String messageText = error.getMessage();
        CharSequence existingText = _messageTextView.getText();
        if (messageText != null && messageText.length() > 0) {
            Spannable spannable = new SpannableString(existingText + "\n" + messageText);
            Context context = getContext();
            if (context != null) {
                spannable.setSpan(new ForegroundColorSpan(
                                ContextCompat.getColor(context, R.color.colorWarning)),
                        existingText.length() + 1, messageText.length() + existingText.length(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            _messageTextView.setText(spannable);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        getActivity().setTitle("Device Registration");
    }
}
