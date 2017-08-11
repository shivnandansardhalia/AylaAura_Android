package com.aylanetworks.aura.localdevice;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.aylanetworks.aura.R;
import com.aylanetworks.aylasdk.AylaDevice;
import com.aylanetworks.aylasdk.AylaNetworks;
import com.aylanetworks.aylasdk.AylaSessionManager;
import com.aylanetworks.aylasdk.change.Change;
import com.aylanetworks.aylasdk.error.AylaError;

/**
 * Fragment used to display and control a GrillRightDevice
 */
public class GrillRightFragment extends Fragment implements AylaDevice.DeviceChangeListener {

    private static final String ARG_DSN = "dsn";
    private static final String ARG_SESSION_NAME = "SessionName";

    private GrillRightDevice _device;
    private GrillRightProbe _probe1;
    private GrillRightProbe _probe2;
    private TextView _emptyView;


    public GrillRightFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param device The GrillRightDevice to control
     * @return A new instance of fragment GrillRightFragment.
     */
    public static GrillRightFragment newInstance(GrillRightDevice device) {
        GrillRightFragment fragment = new GrillRightFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DSN, device.getDsn());
        args.putString(ARG_SESSION_NAME, device.getSessionManager().getSessionName());
        fragment.setArguments(args);
        return fragment;
    }

    private void showEmptyView(boolean show) {
        _emptyView.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            String sessionName = args.getString(ARG_SESSION_NAME);
            String dsn = args.getString(ARG_DSN);
            if (sessionName == null || dsn == null) {
                throw new RuntimeException("Invalid arguments in bundle");
            }

            AylaSessionManager sm = AylaNetworks.sharedInstance().getSessionManager(sessionName);
            _device = (GrillRightDevice)sm.getDeviceManager().deviceWithDSN(dsn);
            _device.addListener(this);
        }

        getActivity().setTitle("GrillRight BBQ");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_grill_right, container, false);
        _probe1 = (GrillRightProbe)v.findViewById(R.id.probe1);
        _probe2 = (GrillRightProbe)v.findViewById(R.id.probe2);
        _probe1.setDevice(_device, 1);
        _probe2.setDevice(_device, 2);
        _emptyView = (TextView)v.findViewById(R.id.gr_disconnected);
        showEmptyView(_device == null || !_device.isConnectedLocal());
        return v;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (_probe1 != null && _probe2 != null) {
            _probe1.setDevice(_device, 1);
            _probe2.setDevice(_device, 2);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (_probe1 != null && _probe2 != null) {
            _probe1.setDevice(null, 1);
            _probe2.setDevice(null, 2);
        }
    }

    @Override
    public void deviceChanged(AylaDevice device, Change change) {
        showEmptyView(!_device.isConnectedLocal());
    }

    @Override
    public void deviceError(AylaDevice device, AylaError error) {

    }

    @Override
    public void deviceLanStateChanged(AylaDevice device, boolean lanModeEnabled) {
        showEmptyView(!_device.isConnectedLocal());
    }
}
