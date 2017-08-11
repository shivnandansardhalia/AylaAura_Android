package com.aylanetworks.aura;
/*
 * Aura_Android
 *
 * Copyright 2015 Ayla Networks, all rights reserved
 */

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.aylanetworks.aylasdk.AylaDevice;

public class DeviceDetailDialog extends DialogFragment {
    AylaDevice _device;
    private TextView _productName;
    private TextView _deviceDSN;
    private TextView _connectedAt;
    private TextView _connectionStatus;
    private TextView _lanEnabled;
    private TextView _lanIP;
    private TextView _location;
    private TextView _mac;
    private TextView _model;
    private TextView _oemModel;
    private TextView _productClass;
    private TextView _templateId;

    static DeviceDetailDialog newInstance(AylaDevice device) {
        DeviceDetailDialog dlg = new DeviceDetailDialog();
        dlg._device = device;
        return dlg;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_device_details, container, false);
        _productName = (TextView) view.findViewById(R.id.product_name);
        _deviceDSN = (TextView) view.findViewById(R.id.dsn);
        _connectedAt = (TextView) view.findViewById(R.id.connected_at);
        _connectionStatus = (TextView) view.findViewById(R.id.connection_status);
        _lanEnabled = (TextView) view.findViewById(R.id.lan_enabled);
        _lanIP = (TextView) view.findViewById(R.id.lan_ip);
        _location = (TextView) view.findViewById(R.id.location);
        _mac = (TextView) view.findViewById(R.id.mac);
        _model = (TextView) view.findViewById(R.id.model);
        _oemModel = (TextView) view.findViewById(R.id.oem_model);
        _productClass = (TextView) view.findViewById(R.id.product_class);
        _templateId = (TextView) view.findViewById(R.id.template_id);

        Button okButton = (Button)view.findViewById(R.id.ok_button);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        initViews();
        return view;
    }

    private void initViews() {
        if (_device == null) {
            return;
        }

        _productName.setText(_device.getProductName());
        _deviceDSN.setText(_device.getDsn());
        String lan = getString(R.string.via_lan);
        String cloud = getString(R.string.via_cloud);

        String connectionStatus = _device.getConnectionStatus().toString() + " (" +
                (_device.isLanModeActive() ? lan : cloud) + ")";
        _connectionStatus.setText(connectionStatus);

        _deviceDSN.setText(_device.getDsn());
        _lanEnabled.setText(_device.isLanEnabled() ? "true" : "false");
        _lanIP.setText(_device.getLanIp());
        String location = _device.getLat() + " / " + _device.getLng();
        _location.setText(location);
        _mac.setText(_device.getMac());
        _model.setText(_device.getModel());
        _oemModel.setText(_device.getOemModel());
        _productClass.setText(_device.getProductClass());
        if (_device.getConnectedAt() != null) {
            _connectedAt.setText(_device.getConnectedAt().toString());
        } else {
            _connectedAt.setText(R.string.not_connected);
        }
        if (_device.getTemplateId() != null) {
            _templateId.setText(_device.getTemplateId().toString());
        } else {
            _templateId.setText(R.string.text_null);
        }
    }
}
