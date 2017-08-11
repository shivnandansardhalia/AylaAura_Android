package com.aylanetworks.aura;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.aylanetworks.aura.util.AuraConfig;
import com.aylanetworks.aylasdk.util.ObjectUtils;

public class DeveloperOptionsActivity extends AppCompatActivity {
    private static int RC_EDIT_CURRENT_CONFIG = 1;

    private TextView _appId;
    private TextView _appSecret;
    private TextView _serviceType;
    private TextView _serviceLocation;
    private TextView _allowDSS;
    private TextView _allowOfflineUse;
    private TextView _networkTimeout;
    private TextView _dssSubscriptionTypes;
    private Spinner _configSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.developer_options);

        Intent intent = getIntent();
        Uri data = intent.getData();
        if (data != null) {
            if (AuraConfig.importData(data)) {
                Toast.makeText(this, R.string.added_config, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, R.string.add_config_fail, Toast.LENGTH_LONG).show();
            }
        }

        _appId = (TextView)findViewById(R.id.app_id);
        _appSecret = (TextView)findViewById(R.id.app_secret);
        _serviceType = (TextView)findViewById(R.id.service_type);
        _serviceLocation = (TextView)findViewById(R.id.service_location);
        _allowDSS = (TextView)findViewById(R.id.allow_dss);
        _allowOfflineUse = (TextView)findViewById(R.id.allow_offline_use);
        _networkTimeout = (TextView)findViewById(R.id.network_timeout);
        _dssSubscriptionTypes = (TextView) findViewById(R.id.subscription_types);
        _configSpinner = (Spinner)findViewById(R.id.config_spinner);

        setupSpinner();

        Button b = (Button)findViewById(R.id.button);
        if (b != null) {
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Let the main activity know we changed the configuration
                    if(MainActivity.sharedInstance() != null){
                        MainActivity.sharedInstance().configChanged();
                    }
                    finish();
                }
            });
        }

        b = (Button)findViewById(R.id.edit_config);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editConfigClicked();
            }
        });

        b = (Button)findViewById(R.id.copy_config);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyConfigClicked();
            }
        });

        b = (Button)findViewById(R.id.delete_config);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteConfig();
            }
        });

        b = (Button)findViewById(R.id.restore_configs);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                restoreConfigs();
            }
        });
    }

    private void deleteConfig() {
        if (AuraConfig.getAvailableConfigurations().size() == 1) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.cant_remove_last_config_title)
                    .setMessage(R.string.cant_remove_last_config_msg)
                    .setPositiveButton(android.R.string.ok, null)
                    .create().show();
            return;
        }

        final AuraConfig config = AuraConfig.getSelectedConfiguration();
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_config_prompt)
                .setMessage(config.name)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        AuraConfig.removeConfig(config);
                        setupSpinner();
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .create().show();
    }

    private void restoreConfigs() {
        final AuraConfig config = AuraConfig.getSelectedConfiguration();
        new AlertDialog.Builder(this)
                .setTitle(R.string.restore_default_config_title)
                .setMessage(R.string.restore_default_config_message)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        AuraConfig.restoreDefaults();
                        setupSpinner();
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .create().show();
    }

    private void setupSpinner() {
        final ArrayAdapter<AuraConfig> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, AuraConfig.getAvailableConfigurations
                ());

        AuraConfig selectedConfig = AuraConfig.getSelectedConfiguration();

        if (_configSpinner != null) {
            _configSpinner.setAdapter(adapter);
            for (int i = 0; i < adapter.getCount(); i++) {
                if (selectedConfig == adapter.getItem(i)) {
                    _configSpinner.setSelection(i);
                    break;
                }
            }

            _configSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    AuraConfig config = adapter.getItem(position);
                    onConfigChanged(config);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    onConfigChanged(null);
                }
            });
        }

        onConfigChanged(selectedConfig);
    }

    private void editConfigClicked() {
        Intent intent = new Intent(this, AuraConfigEditor.class);
        startActivityForResult(intent, RC_EDIT_CURRENT_CONFIG);
    }

    private void copyConfigClicked() {
        final AuraConfig currentConfig = AuraConfig.getSelectedConfiguration();
        String copyName = currentConfig.name + getString(R.string.config_copy_modifier);

        final EditText et = new EditText(this);
        et.setText(copyName);

        AlertDialog.Builder ad = new AlertDialog.Builder(this);
        ad.setView(et)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        AuraConfig newConfig = new AuraConfig(currentConfig);
                        newConfig.name = et.getText().toString();
                        AuraConfig.addConfig(newConfig);
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .setTitle(R.string.copy_config_prompt_title)
                .create().show();
    }

    private void onConfigChanged(AuraConfig config) {
        if (config != null) {
            AuraConfig.setSelectedConfiguration(config);
            _appId.setText(config.appId);
            _appSecret.setText(config.appSecret);
            _serviceType.setText(config.serviceType);
            _serviceLocation.setText(config.serviceLocation);
            _allowOfflineUse.setText(config.allowOfflineUse ? "YES" : "NO");
            _allowDSS.setText(config.allowDSS ? "YES" : "NO");
            _networkTimeout.setText(String.valueOf(config.defaultNetworkTimeoutMs));
            _dssSubscriptionTypes.setText(ObjectUtils.getDelimitedString(
                    config.dssSubscriptionTypeList, ","));
        } else {
            _appId.setText("---");
            _appSecret.setText("---");
            _serviceType.setText("---");
            _serviceLocation.setText("---");
            _allowOfflineUse.setText("---");
            _allowDSS.setText("---");
            _networkTimeout.setText("---");
            _dssSubscriptionTypes.setText("---");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            setupSpinner();
        }
    }
}
