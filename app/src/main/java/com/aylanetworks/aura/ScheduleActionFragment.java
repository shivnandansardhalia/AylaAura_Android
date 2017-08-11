package com.aylanetworks.aura;
/*
 * AylaSDK
 *
 * Copyright 2015 Ayla Networks, all rights reserved
 */

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
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
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.aylanetworks.aylasdk.AylaAPIRequest;
import com.aylanetworks.aylasdk.AylaDevice;
import com.aylanetworks.aylasdk.AylaProperty;
import com.aylanetworks.aylasdk.AylaSchedule;
import com.aylanetworks.aylasdk.AylaScheduleAction;
import com.aylanetworks.aylasdk.error.AylaError;
import com.aylanetworks.aylasdk.error.ErrorListener;

import org.honorato.multistatetogglebutton.MultiStateToggleButton;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class ScheduleActionFragment extends Fragment {
    private final static String LOG_TAG = "ScheduleFragment";

    private final static String ARG_DEVICE_DSN = "deviceDSN";

    private AylaDevice _device;
    private AylaSchedule _schedule;
    private LinearLayout _propertySelectionCheckboxLayout;
    private AylaScheduleAction[] _scheduleactions;
    private Set<String> _actionProperties;
    private Switch _scheduleEnabledSwitch;
    private EditText _scheduleActionValueEditText;
    private Button _saveActionButton;
    private Button _removeActionButton;
    private MultiStateToggleButton _executeToggleButton;
    private Spinner _scheduleCreationSpinner;
    private TextView _scheduleActionDescription;
    private String _scheduleCreationProperty;

    public static ScheduleActionFragment newInstance(AylaDevice device, AylaSchedule schedule) {
        Bundle args = new Bundle();
        args.putString(ARG_DEVICE_DSN, device.getDsn());
        ScheduleActionFragment frag = new ScheduleActionFragment();
        frag.setSchedule(schedule);
        frag.setArguments(args);
        return frag;
    }

    private void setSchedule(AylaSchedule schedule) {
        _schedule = schedule;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View root = inflater.inflate(R.layout.fragment_schedule_action, container, false);
        // Get our device argument
        if (getArguments() != null) {
            String dsn = getArguments().getString(ARG_DEVICE_DSN);
            _device = MainActivity.getSession().getDeviceManager().deviceWithDSN(dsn);

            try {
                _schedule.fetchActions(
                        new Response.Listener<AylaScheduleAction[]>() {
                            @Override
                            public void onResponse(AylaScheduleAction[] response) {
                                _scheduleactions = response;
                                initialize();

                            }
                        },
                        new ErrorListener() {
                            @Override
                            public void onErrorResponse(AylaError error) {
                                Toast.makeText(MainActivity.sharedInstance(), error.toString(),
                                        Toast.LENGTH_LONG).show();
                            }
                        });
            } catch (Exception e) {
                e.printStackTrace();
                //MainActivity.sharedInstance().popBackstackToRoot();
                Toast.makeText(getActivity(), R.string.unknown_error, Toast.LENGTH_LONG).show();
                return new View(getActivity());
            }
        }

        // Get our views set up
        _propertySelectionCheckboxLayout = (LinearLayout) root.findViewById
                (R.id.property_selection_checkbox_layout);
        _scheduleEnabledSwitch = (Switch) root.findViewById(R.id.schedule_action_enabled_switch);
        _scheduleActionValueEditText = (EditText) root.findViewById(R.id.schedule_action_value_text);
        _saveActionButton = (Button) root.findViewById(R.id.button_action_save);
        _removeActionButton = (Button) root.findViewById(R.id.button_action_remove);
        _executeToggleButton = (MultiStateToggleButton) root.findViewById(R.id.mstb_multi_id);
        _scheduleCreationSpinner = (Spinner) root.findViewById(R.id.location_spinner);
        _scheduleActionDescription = (TextView)root.findViewById(R.id
                .schedule_properties_action_descripton);

        _scheduleEnabledSwitch.setChecked(true);

        return root;
    }

    private void initialize() {
        _actionProperties = new HashSet<>();

        _saveActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveScheduleActions();
            }
        });
        _removeActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeAllScheduleActions();
            }
        });
        if(_schedule.hasFixedActions()) {
            _propertySelectionCheckboxLayout.setVisibility(View.VISIBLE);
            _scheduleCreationSpinner.setVisibility(View.GONE);
            _scheduleActionDescription.setText(R.string.schedule_properties_update_description);
            setupPropertySelection();

        } else {
            _propertySelectionCheckboxLayout.setVisibility(View.GONE);
            _scheduleCreationSpinner.setVisibility(View.VISIBLE);
            _scheduleActionDescription.setText(R.string.schedule_properties_create_description);
            setPropertiesForSpinner();
        }
        //Set it to default position
        _executeToggleButton.setValue(0);
    }

    private void setPropertiesForSpinner() {
        List<String> list = getPropertiesScheduleList();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout
                .simple_spinner_dropdown_item, list);
        _scheduleCreationSpinner.setAdapter(adapter);
        _scheduleCreationSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                _scheduleCreationProperty = (String)_scheduleCreationSpinner.getItemAtPosition
                        (position);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void removeAllScheduleActions() {
        final View view = getActivity().findViewById(R.id.property_selection_layout);
        final Snackbar sb = Snackbar.make(view, "Removing All Schedule actions...",
                Snackbar.LENGTH_INDEFINITE);

        final AylaAPIRequest request = _schedule.deleteAllActions(
                new Response.Listener<AylaAPIRequest.EmptyResponse>() {
                    @Override
                    public void onResponse(final AylaAPIRequest.EmptyResponse response) {
                        sb.dismiss();
                        Log.d(LOG_TAG, "Deleted all schedules for: " + _schedule.getName());
                        Toast.makeText(MainActivity.sharedInstance(), R.string.deleted_all_actions,
                                Toast.LENGTH_LONG).show();
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
        if(request != null) {
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

    private void saveScheduleActions() {
        if (!_schedule.hasFixedActions())
            createScheduleAction();
        else
            updateScheduleActions();
    }

    private void createScheduleAction() {
        final View view = getActivity().findViewById(R.id.property_selection_layout);
        String valueAction = _scheduleActionValueEditText.getText().toString();

        //Get the value that user selected from the Toggle Button. The 3 options are AtStart,InRange
        //and AtEnd. The values are 0 for AtStart, 1 for InRange, and 2 for AtEnd
        int executeValue = _executeToggleButton.getValue();
        AylaScheduleAction.AylaScheduleActionFirePoint firePoint = AylaScheduleAction.
                AylaScheduleActionFirePoint.AtStart;
        if (executeValue == 1) {
            firePoint = AylaScheduleAction.AylaScheduleActionFirePoint.InRange;
        } else if (executeValue == 2) {
            firePoint = AylaScheduleAction.AylaScheduleActionFirePoint.AtEnd;
        }
        final Snackbar sb = Snackbar.make(view, "Saving Schedule action...",
                Snackbar.LENGTH_INDEFINITE);

        boolean isChecked = _scheduleEnabledSwitch.isChecked();
        AylaScheduleAction aylaScheduleAction = new AylaScheduleAction();
        aylaScheduleAction.setName(_scheduleCreationProperty);

        aylaScheduleAction.setValue(valueAction);
        aylaScheduleAction.setScheduleActionFirePoint(firePoint);
        aylaScheduleAction.setType("SchedulePropertyAction");
        aylaScheduleAction.setBaseType("boolean");
        aylaScheduleAction.setActive(isChecked);


        final AylaAPIRequest request = _schedule.createAction(aylaScheduleAction,
                new Response.Listener<AylaScheduleAction>() {
                    @Override
                    public void onResponse(final AylaScheduleAction response) {
                        Log.d(LOG_TAG, "Update Actions saved for:" + _schedule.getName());
                        sb.dismiss();
                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                        builder.setMessage(R.string.more_schedule_action_msg)
                                .setTitle(android.R.string.dialog_alert_title)
                                .setCancelable(false)
                                .setPositiveButton(android.R.string.ok,
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog,
                                                                int id) {
                                                Log.d(LOG_TAG, "Configure More Schedule Actions for: " +
                                                        _schedule.getName());
                                            }
                                        })
                                .setNegativeButton(android.R.string.no,
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog,
                                                                int id) {
                                                MainActivity.sharedInstance().pushFragment(
                                                        DeviceDetailFragment.newInstance(
                                                                _device.getDsn()));
                                            }
                                        });
                        AlertDialog alert = builder.create();
                        alert.show();
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
        if(request != null) {
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

    private void updateScheduleActions() {
        final View view = getActivity().findViewById(R.id.property_selection_layout);
        String valueAction = _scheduleActionValueEditText.getText().toString();

        //Get the value that user selected from the Toggle Button. The 3 options are AtStart,InRange
        //and AtEnd. The values are 0 for AtStart, 1 for InRange, and 2 for AtEnd
        int executeValue = _executeToggleButton.getValue();
        AylaScheduleAction.AylaScheduleActionFirePoint firePoint = AylaScheduleAction.
                AylaScheduleActionFirePoint.AtStart;

        if (executeValue ==  1) {
            firePoint = AylaScheduleAction.AylaScheduleActionFirePoint.InRange;
        } else if (executeValue == 2) {
            firePoint = AylaScheduleAction.AylaScheduleActionFirePoint.AtEnd;
        }
        final Snackbar sb = Snackbar.make(view, "Saving Schedule action...",
                Snackbar.LENGTH_INDEFINITE);

        boolean isChecked = _scheduleEnabledSwitch.isChecked();
        final List<AylaScheduleAction> listScheduleActions = new ArrayList<>();

        for (String propertyName : _actionProperties) {
            for (AylaScheduleAction scheduleAction : _scheduleactions) {
                if (scheduleAction.getName().equals(propertyName)) {
                    scheduleAction.setValue(valueAction);
                    scheduleAction.setScheduleActionFirePoint(firePoint);
                    scheduleAction.setActive(isChecked);
                    listScheduleActions.add(scheduleAction);
                }
            }
        }
        AylaScheduleAction[] arrayActions = listScheduleActions.toArray(
                new AylaScheduleAction[listScheduleActions.size()]);
        final int responseSize= listScheduleActions.size();
        final AylaAPIRequest request = _schedule.updateActions(arrayActions,
                new Response.Listener<AylaScheduleAction[]>() {
                    @Override
                    public void onResponse(final AylaScheduleAction[] response) {
                        if(responseSize == response.length) {
                            Log.d(LOG_TAG, "Update Actions saved for:" + _schedule.getName());
                            sb.dismiss();
                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                            builder.setMessage(R.string.updated_all_actions)
                                    .setTitle(android.R.string.dialog_alert_title)
                                    .setCancelable(false)
                                    .setPositiveButton(android.R.string.ok,
                                            new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog,
                                                                    int id) {
                                                    Log.d(LOG_TAG, "Configure More Schedule Actions for: " +
                                                            _schedule.getName());
                                                }
                                            })
                                    .setNegativeButton(android.R.string.no,
                                            new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog,
                                                                    int id) {
                                                    getFragmentManager().popBackStack();
                                                }
                                            });
                            AlertDialog alert = builder.create();
                            alert.show();
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
        if(request != null) {
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

    private void setupPropertySelection() {
        _propertySelectionCheckboxLayout.removeAllViewsInLayout();

        int nSelected = 0;
        CheckBox firstCheckBox = null;
        List<String> list = getPropertiesScheduleList();
        if (list.isEmpty()) {
            Toast.makeText(MainActivity.sharedInstance(), R.string.no_actions_msg,
                    Toast.LENGTH_LONG).show();
            getActivity().getSupportFragmentManager().popBackStack();
            return;
        }

        for (String propertyName : list) {
            CheckBox cb = new CheckBox(getActivity());
            cb.setText(propertyName);
            cb.setTag(propertyName);
            if (isPropertyActive(propertyName)) {
                cb.setChecked(true);
                addAction(propertyName);
                nSelected++;
            }
            cb.setBackgroundColor(ContextCompat.getColor(getContext(), R.color
                    .app_theme_accent));
            cb.setTextColor(ContextCompat.getColor(getContext(), R.color
                    .colorWarningFG));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                cb.setTextAppearance(android.R.style.TextAppearance_Medium);
            } else {
                cb.setTextAppearance(getContext(),android.R.style.TextAppearance_Medium);
            }
            cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    propertySelectionChanged((CheckBox) buttonView, isChecked);
                }
            });
            if (firstCheckBox == null) {
                firstCheckBox = cb;
            }
            _propertySelectionCheckboxLayout.addView(cb);
        }

        if (nSelected == 0 && firstCheckBox != null) {
            // Check the first box
            firstCheckBox.setChecked(true);
        }
    }

    private void propertySelectionChanged(CheckBox cb, boolean isChecked) {
        String propertyName = (String) cb.getTag();
        Log.d(LOG_TAG, "Property selection changed: " + propertyName);

        if (isChecked) {
            addAction(propertyName);
        } else {
            removeAction(propertyName);
        }
    }

    private List<String> getPropertiesScheduleList() {
        String[] propertyNames = getSchedulablePropertyNames();
        HashSet<String> setProperties = new HashSet<>( );

        for(String propertyName:propertyNames) {
            setProperties.add(propertyName);
        }
        List<String> list = new ArrayList<>();
        List <AylaProperty>aylaProperties = _device.getProperties();
        for(AylaProperty property:aylaProperties){
            if(setProperties.contains(property.getName())) {
                list.add(property.getName());
            }
        }
        return list;
    }


    private String[] getSchedulablePropertyNames() {
        if (AuraDeviceDetailProvider.OEM_MODEL_EVB.equals(_device.getOemModel())) {
            return AuraDeviceDetailProvider.EVB_SCHEDULE_PROPERTIES;
        } else if (AuraDeviceDetailProvider.OEM_MODEL_PLUG.equals(_device.getOemModel())) {
            return AuraDeviceDetailProvider.PLUG_SCHEDULE_PROPERTIES;
        }
        return null;
    }

    private boolean isPropertyActive(String propertyName) {
        if (_scheduleactions == null) {
            return false;
        }

        for (AylaScheduleAction action : _scheduleactions) {
            if (action.getName().equals(propertyName) && action.isActive()) {
                return true;
            }
        }
        return false;
    }

    private void addAction(String propertyName) {
        _actionProperties.add(propertyName);
    }

    private void removeAction(String propertyName) {
        _actionProperties.remove(propertyName);
    }
}
