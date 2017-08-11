package com.aylanetworks.aura;

/*
 * Android_AylaSDK
 *
 * Copyright 2016 Ayla Networks, all rights reserved
 */

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.aylanetworks.aura.localdevice.AuraLocalDeviceManager;
import com.aylanetworks.aylasdk.AylaAPIRequest;
import com.aylanetworks.aylasdk.AylaDevice;
import com.aylanetworks.aylasdk.AylaDeviceManager;
import com.aylanetworks.aylasdk.AylaDeviceNode;
import com.aylanetworks.aylasdk.AylaLog;
import com.aylanetworks.aylasdk.AylaNetworks;
import com.aylanetworks.aylasdk.AylaSessionManager;
import com.aylanetworks.aylasdk.change.Change;
import com.aylanetworks.aylasdk.change.ListChange;
import com.aylanetworks.aylasdk.error.AppPermissionError;
import com.aylanetworks.aylasdk.error.AylaError;
import com.aylanetworks.aylasdk.error.ErrorListener;
import com.aylanetworks.aylasdk.setup.AylaSetup;
import com.aylanetworks.aylasdk.util.EmptyListener;
import com.aylanetworks.aylasdk.util.PermissionUtils;
import com.aylanetworks.aylasdk.localdevice.AylaLocalDevice;
import com.aylanetworks.aylasdk.localdevice.AylaLocalDeviceManager;
import com.aylanetworks.aylasdk.localdevice.ble.AylaBLEDevice;

import java.util.Map;



/**
 * A simple {@link Fragment} subclass.
 * Use the {@link DeviceListFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DeviceListFragment extends Fragment
        implements AylaDeviceManager.DeviceManagerListener, AylaDevice.DeviceChangeListener {
    private static final String LOG_TAG = "DeviceList";
    RecyclerView _recyclerView;
    ProgressBar _mProgressBar;
    ImageButton _addDeviceButton;
    TextView _txtviewNoDevices;
    TextView _txtviewClickToAdd;

    public DeviceListFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment DeviceListFragment.
     */
    public static DeviceListFragment newInstance() {
        return new DeviceListFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        AylaDeviceManager dm = getDeviceManager();
        if (dm != null) {
            dm.removeListener(this);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_device_list, container, false);
        _recyclerView = (RecyclerView) view.findViewById(R.id.recycler);
        _recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        _mProgressBar = (ProgressBar) view.findViewById(R.id.device_list_progress);
        _addDeviceButton = (ImageButton) view.findViewById(R.id.add_button);
        _txtviewClickToAdd = (TextView) view.findViewById(R.id.textview_click_to_add);
        _txtviewClickToAdd.setVisibility(View.GONE);
        _txtviewNoDevices = (TextView) view.findViewById(R.id.textview_no_devices);
        _txtviewNoDevices.setVisibility(View.GONE);
        _recyclerView.setAdapter(new DeviceAdapter());
        _mProgressBar.setVisibility(View.VISIBLE);
        _addDeviceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SetupFragment setupFragment = SetupFragment.newInstance();
                MainActivity.sharedInstance().pushFragment(setupFragment);
            }
        });
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        getActivity().setTitle("Device List");
        AylaDeviceManager dm = getDeviceManager();
        if (dm != null) {
            dm.addListener(this);
            for ( AylaDevice d : dm.getDevices() ) {
                d.addListener(this);
            }
        }
        _recyclerView.getAdapter().notifyDataSetChanged();
    }

    @Override
    public void onStop() {
        super.onStop();
        AylaDeviceManager dm = getDeviceManager();
        if (dm != null) {
            dm.removeListener(this);
            for ( AylaDevice d : dm.getDevices() ) {
                d.removeListener(this);
            }
        }
    }

    void onItemSelected(AylaDevice device) {
        AylaLog.d(LOG_TAG, "Item selected: " + device);
        // If we have a local device that needs to be set up, do that here.
        if (device instanceof AylaBLEDevice) {
            AylaBLEDevice bleDevice = (AylaBLEDevice)device;
            if (bleDevice.requiresLocalConfiguration()) {
                promptConfigureLocalDevice(bleDevice);
                return;
            }
        }

        Fragment frag;
        frag = DeviceDetailFragment.newInstance(device.getDsn());
        MainActivity.sharedInstance().pushFragment(frag);
    }

    private void promptConfigureLocalDevice(final AylaBLEDevice bleDevice) {
        // Prompt the user to configure the device
        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.configure_local_title)
                .setMessage(R.string.configure_local_message)
                .setIcon(android.R.drawable.stat_sys_data_bluetooth)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        configureLocalDevice(bleDevice);
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Take them through to the details page
                        MainActivity.sharedInstance().pushFragment(DeviceDetailFragment
                                .newInstance(bleDevice.getDsn()));
                    }
                })
                .create().show();
    }

    private void configureLocalDevice(final AylaBLEDevice bleDevice) {
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

        String snackbarMessage = getActivity().getString(R.string.searching_for_local_device,
                bleDevice.getFriendlyName());
        final Snackbar sb = Snackbar.make(getView(), snackbarMessage, Snackbar
                .LENGTH_INDEFINITE);

        final EmptyListener<AylaAPIRequest.EmptyResponse> emptyListener = new EmptyListener<>();

        final AylaAPIRequest request = ldm.findLocalDevices(5000, new Response.Listener<AylaLocalDevice[]>() {
            @Override
            public void onResponse(AylaLocalDevice[] response) {
                AylaLog.d(LOG_TAG, "Local devices: " + response.length);
                sb.dismiss();
                if (response.length == 0) {
                    Snackbar.make(getView(), R.string.local_device_not_found, Snackbar.LENGTH_LONG)
                            .show();
                } else if (response.length == 1) {
                    // Assume this is the device.
                    final AylaBLEDevice foundDevice = (AylaBLEDevice)response[0];

                    foundDevice.connectLocal(
                            new Response.Listener<AylaAPIRequest.EmptyResponse>() {
                                @Override
                                public void onResponse(AylaAPIRequest.EmptyResponse response) {
                                    if (TextUtils.equals(foundDevice.getHardwareAddress(),
                                            bleDevice.getHardwareAddress())) {
                                        sb.dismiss();
                                        bleDevice.mapBluetoothAddress(foundDevice.getBluetoothAddress());
                                        Toast.makeText(MainActivity.sharedInstance(),
                                                R.string.local_device_connected,
                                                Toast.LENGTH_LONG).show();
                                    } else {
                                        // This is not the device we are looking for
                                        foundDevice.disconnectLocal(emptyListener, emptyListener);
                                        Toast.makeText(MainActivity.sharedInstance(),
                                                R.string.local_device_doesnt_match,
                                                Toast.LENGTH_LONG).show();
                                    }
                                }
                            },
                            new ErrorListener() {
                                @Override
                                public void onErrorResponse(AylaError error) {
                                    foundDevice.disconnectLocal(emptyListener, emptyListener);
                                    sb.dismiss();
                                    Toast.makeText(MainActivity.sharedInstance(),
                                            R.string.local_device_connect_fail,
                                            Toast.LENGTH_LONG).show();
                                }
                            });
                } else {
                    sb.dismiss();
                    disambiguateLocalDevices(bleDevice, response);
                }
            }
        }, new ErrorListener() {
            @Override
            public void onErrorResponse(AylaError error) {
                AylaLog.e(LOG_TAG, "Scan error: " + error);
                sb.setText(error.getMessage());
                sb.setDuration(Snackbar.LENGTH_LONG);
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
    }

    private void disambiguateLocalDevices(final AylaBLEDevice bleDevice, final AylaLocalDevice[]
            scannedDevices) {
        ListView lv = new ListView(MainActivity.sharedInstance());
        ArrayAdapter<AylaLocalDevice> adapter = new ArrayAdapter<>(MainActivity.sharedInstance(),
                android.R.layout.simple_list_item_1, scannedDevices);
        lv.setAdapter(adapter);
        final AlertDialog dlg = new AlertDialog.Builder(MainActivity.sharedInstance())
                .setTitle(R.string.choose_local_device)
                .setView(lv)
                .setNegativeButton(android.R.string.cancel, null)
                .create();

        final EmptyListener<AylaAPIRequest.EmptyResponse> emptyListener = new EmptyListener<>();

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final AylaBLEDevice selectedDevice = (AylaBLEDevice)scannedDevices[position];
                selectedDevice.connectLocal(new Response.Listener<AylaAPIRequest.EmptyResponse>() {
                    @Override
                    public void onResponse(AylaAPIRequest.EmptyResponse response) {
                        if (TextUtils.equals(selectedDevice.getHardwareAddress(),
                                bleDevice.getHardwareAddress())) {
                            bleDevice.mapBluetoothAddress(selectedDevice.getBluetoothAddress());
                            Toast.makeText(MainActivity.sharedInstance(),
                                    R.string.local_device_connected,
                                    Toast.LENGTH_LONG).show();
                        } else {
                            // This is not the device we are looking for
                            selectedDevice.disconnectLocal(emptyListener, emptyListener);
                            Toast.makeText(MainActivity.sharedInstance(),
                                    R.string.local_device_doesnt_match,
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                }, new ErrorListener() {
                    @Override
                    public void onErrorResponse(AylaError error) {
                        selectedDevice.disconnectLocal(emptyListener, emptyListener);
                        Toast.makeText(MainActivity.sharedInstance(),
                                R.string.local_device_connect_fail, Toast.LENGTH_LONG).show();
                    }
                });
                Toast.makeText(MainActivity.sharedInstance(), R.string.connecting_to_ble_device,
                        Toast.LENGTH_LONG).show();
                dlg.dismiss();
            }
        });

        dlg.show();
    }

    private AylaDeviceManager getDeviceManager() {
        AylaSessionManager sm = AylaNetworks.sharedInstance()
                .getSessionManager(MainActivity.SESSION_NAME);
        if (sm == null) {
            return null;
        }

        return sm.getDeviceManager();
    }

    @Override
    public void deviceManagerInitComplete(Map<String, AylaError> deviceFailures) {
        _recyclerView.getAdapter().notifyDataSetChanged();
        _mProgressBar.setVisibility(View.GONE);
        displayNoDeviceUI();
    }

    @Override
    public void deviceManagerInitFailure(AylaError error, AylaDeviceManager.DeviceManagerState failureState) {
        _mProgressBar.setVisibility(View.GONE);
    }

    // DeviceManagerListener methods

    @Override
    public void deviceListChanged(ListChange change) {
        _mProgressBar.setVisibility(View.GONE);
        AylaLog.d(LOG_TAG, "Device list changed: " + change);
        // Make sure we are listeners to all devices
        AylaDeviceManager dm = getDeviceManager();
        if ( dm != null) {
            for (AylaDevice d : getDeviceManager().getDevices()) {
                d.addListener(this);
            }
        }
        _recyclerView.getAdapter().notifyDataSetChanged();
        displayNoDeviceUI();
    }

    @Override
    public void deviceManagerError(AylaError error) {
        _mProgressBar.setVisibility(View.GONE);
        AylaLog.e(LOG_TAG, "Device manager error: " + error);
        View view = getView();
        if ( view != null) {
            Snackbar.make(view, "Device manager: " + error, Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    public void deviceManagerStateChanged(AylaDeviceManager.DeviceManagerState oldState, AylaDeviceManager.DeviceManagerState newState) {
        AylaLog.d(LOG_TAG, "DeviceManager " + oldState + " --> " + newState);
        _recyclerView.getAdapter().notifyDataSetChanged();
    }

    @Override
    public void deviceChanged(AylaDevice device, Change change) {
        AylaLog.d(LOG_TAG, "Device " + device.getDsn() + " changed: " + change);
        _recyclerView.getAdapter().notifyDataSetChanged();
    }

    @Override
    public void deviceError(AylaDevice device, AylaError error) {
        AylaLog.e(LOG_TAG, "Device " + device.getDsn() + " encountered an error: " + error);
        View view = getView();
        if ( view != null ) {
            Snackbar.make(view, "Device " + device.getDsn() + " encountered an error:\n" +
                    error.getMessage(), Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    public void deviceLanStateChanged(AylaDevice device, boolean lanModeEnabled) {
        AylaLog.d(LOG_TAG, "Device " + device.getDsn() + " changed LAN state: " + lanModeEnabled);
        _recyclerView.getAdapter().notifyDataSetChanged();
    }

    class DeviceAdapter extends RecyclerView.Adapter<DeviceHolder> {

        @Override
        public DeviceHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = (LayoutInflater) MainActivity.sharedInstance()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View v = inflater.inflate(R.layout.device_cardview, parent, false);
            return new DeviceHolder(v);
        }

        @Override
        public void onBindViewHolder(DeviceHolder holder, int position) {
            AylaDeviceManager dm = AylaNetworks.sharedInstance()
                    .getSessionManager(MainActivity.SESSION_NAME)
                    .getDeviceManager();
            _mProgressBar.setVisibility(View.GONE);
            final AylaDevice d = dm.getDevices().get(position);
            holder.deviceName.setText(d.getFriendlyName());
            String ipField;
            if (d.isNode()) {
                AylaDeviceNode node = (AylaDeviceNode)d;
                ipField = "GW " + node.getGatewayDsn();
            } else {
                ipField = d.getLanIp();
            }
            String subtext = d.getDsn() + "    " + ipField;
            holder.deviceDsn.setText(subtext);
            Context context = getContext();

            // See if we're dealing with a local device
            AylaLocalDevice localDevice = null;
            if (d instanceof AylaLocalDevice) {
                localDevice = (AylaLocalDevice)d;
            }

            if ( context != null ) {
                Drawable image = ContextCompat.getDrawable(context,
                        R.drawable.ic_cloud_off_black_24dp);
                int imageColor = ContextCompat.getColor(context, R.color.colorIconError);

                if (localDevice != null) {
                    if (localDevice.requiresLocalConfiguration()) {
                        image = ContextCompat.getDrawable(context,
                                android.R.drawable.stat_sys_warning);
                        imageColor = ContextCompat.getColor(context, R.color.color_icon_warning);
                    } else  {
                        image = ContextCompat.getDrawable(context,
                                android.R.drawable.stat_sys_data_bluetooth);
                        imageColor = localDevice.isConnectedLocal() ?
                                ContextCompat.getColor(context, R.color.colorBluetooth) :
                                ContextCompat.getColor(context, R.color.disabled_text);
                    }
                } else if (d.getConnectionStatus().equals(AylaDevice.ConnectionStatus.Online)) {
                    imageColor = ContextCompat.getColor(context, R.color.colorIcon);
                    if (d.isLanModeActive()) {
                            image = ContextCompat.getDrawable(context,
                                    R.drawable.ic_signal_wifi_4_bar_black_24dp);
                    } else {
                        image = ContextCompat.getDrawable(context,
                                R.drawable.ic_cloud_circle_black_24dp);
                    }
                }

                holder.connectionImage.setImageDrawable(image);
                holder.connectionImage.setColorFilter(imageColor, PorterDuff.Mode.SRC_ATOP);
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onItemSelected(d);
                    }
                });
            }
        }

        @Override
        public int getItemCount() {
            AylaDeviceManager dm = getDeviceManager();
            if (dm == null) {
                AylaLog.d(LOG_TAG, "Holder: dm is null");
                return 0;
            }


            return dm.getDevices().size();
        }
    }

    class DeviceHolder extends RecyclerView.ViewHolder {
        TextView deviceName;
        TextView deviceDsn;
        ImageView connectionImage;

        public DeviceHolder(View itemView) {
            super(itemView);
            deviceName = (TextView) itemView.findViewById(R.id.device_name);
            deviceDsn = (TextView) itemView.findViewById(R.id.device_dsn);
            connectionImage = (ImageView) itemView.findViewById(R.id.connection_state_image);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if(getDeviceManager() != null && getDeviceManager().getState() == AylaDeviceManager
                .DeviceManagerState.Ready){
            _mProgressBar.setVisibility(View.GONE);
            displayNoDeviceUI();
        }
    }

    private void displayNoDeviceUI(){
        AylaDeviceManager deviceManager = getDeviceManager();
        if(deviceManager.getDevices().isEmpty()){
            _recyclerView.setVisibility(View.GONE);
            _txtviewNoDevices.setVisibility(View.VISIBLE);
            _txtviewClickToAdd.setVisibility(View.VISIBLE);

        } else{
            _recyclerView.setVisibility(View.VISIBLE);
            _txtviewNoDevices.setVisibility(View.GONE);
            _txtviewClickToAdd.setVisibility(View.GONE);
        }
    }
}
