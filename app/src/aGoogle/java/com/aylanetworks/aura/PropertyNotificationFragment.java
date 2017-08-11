package com.aylanetworks.aura;
/*
 * AylaSDK
 *
 * Copyright 2015 Ayla Networks, all rights reserved
 */

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.android.volley.Response;
import com.aylanetworks.aura.util.ContactHelper;
import com.aylanetworks.aylasdk.AylaAPIRequest;
import com.aylanetworks.aylasdk.AylaContact;
import com.aylanetworks.aylasdk.AylaLog;
import com.aylanetworks.aylasdk.AylaNetworks;
import com.aylanetworks.aylasdk.AylaDevice;
import com.aylanetworks.aylasdk.AylaProperty;
import com.aylanetworks.aylasdk.AylaPropertyTrigger;
import com.aylanetworks.aylasdk.AylaPropertyTriggerApp;
import com.aylanetworks.aylasdk.AylaServiceApp;
import com.aylanetworks.aylasdk.AylaSystemSettings;
import com.aylanetworks.aylasdk.error.AylaError;
import com.aylanetworks.aylasdk.error.ErrorListener;

import java.util.ArrayList;
import java.util.List;

import static com.aylanetworks.aylasdk.AylaPropertyTrigger.*;

public class PropertyNotificationFragment extends Fragment implements
        ContactListAdapter.ContactCardListener, View.OnClickListener,
        AdapterView.OnItemSelectedListener {
    private static final String ARG_DSN = "dsn";
    private static final String ARG_TRIGGER = "trigger";
    private static final String TRIGGER_COMPARE_ABSOLUTE = "compare_absolute";
    private static final String TRIGGER_ALWAYS = "always";
    private static final String TRIGGER_CHANGE = "on_change";
    private static final String LOG_TAG = "PropNotifFrag";
    private final List<AylaContact> _emailContacts;
    private final List<AylaContact> _smsContacts;
    private Spinner _propertySpinner;
    private EditText _nameEditText;
    private TabLayout _tabLayoutNotifications;
    private TabLayout _tabLayoutComparator;
    private EditText _edittextValue;
    private RelativeLayout _relativeLayoutComparators;
    private AylaPropertyTrigger _trigger;
    private String _originalTriggerName;
    private AylaPropertyTrigger _originalTrigger;
    private CheckBox _sendPushCheckbox;
    private boolean _pushToDevice = false;


    public static PropertyNotificationFragment newInstance(AylaDevice device, AylaPropertyTrigger
            triggerToEdit) {
        PropertyNotificationFragment frag = new PropertyNotificationFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DSN, device.getDsn());
        if ( triggerToEdit != null ) {
            args.putString(ARG_TRIGGER, triggerToEdit.getDeviceNickname());
        }
        frag.setArguments(args);
        return frag;
    }

    // Default constructor
    public PropertyNotificationFragment() {
        _emailContacts = new ArrayList<>();
        _smsContacts = new ArrayList<>();
    }

    private AylaDevice _device;
    private RecyclerView _recyclerView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
        _device = null;
        _trigger = new AylaPropertyTrigger();
        if (getArguments() != null ) {
            String dsn = getArguments().getString(ARG_DSN);
            _device = MainActivity.getSession().getDeviceManager().deviceWithDSN(dsn);
            Log.d(LOG_TAG, "My device: " + _device);

            _originalTriggerName = getArguments().getString(ARG_TRIGGER);
            if (_originalTriggerName != null) {
                // Try to find the trigger
                for (AylaProperty prop : _device.getProperties()) {
                    prop.fetchTriggers(
                            new Response.Listener<AylaPropertyTrigger[]>() {
                                @Override
                                public void onResponse(AylaPropertyTrigger[] response) {
                                    if (response != null && response.length > 0) {
                                        for (AylaPropertyTrigger trigger : response) {
                                            if (_originalTriggerName.equals
                                                    (trigger.getDeviceNickname())) {
                                                _originalTrigger = trigger;
                                                _trigger = trigger;
                                                updateUI();
                                                break;
                                            }
                                        }
                                    }
                                }
                            },
                            new ErrorListener() {
                                @Override
                                public void onErrorResponse(AylaError error) {
                                    Toast.makeText(MainActivity.sharedInstance(), error.toString(),
                                            Toast.LENGTH_LONG).show();
                                }
                            });
                }
            } else{
                Log.e(LOG_TAG, "Unable to find original trigger!");
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if(_originalTriggerName == null){
            _tabLayoutNotifications.getTabAt(0).select();
            _trigger.setTriggerType(TriggerType.Change.stringValue());
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_property_notification, container, false);
        RecyclerView.LayoutManager _layoutManager;
        _recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        _layoutManager = new LinearLayoutManager(getActivity());
        _recyclerView.setLayoutManager(_layoutManager);
        _nameEditText = (EditText)view.findViewById(R.id.notification_name);
        _propertySpinner = (Spinner)view.findViewById(R.id.property_spinner);
        _tabLayoutNotifications = (TabLayout) view.findViewById(R.id.tablayout_notifications);
        _tabLayoutComparator = (TabLayout) view.findViewById(R.id.tablayout_comparators);
        _relativeLayoutComparators = (RelativeLayout) view.findViewById(R.id.layout_comparators);
        _edittextValue = (EditText) view.findViewById(R.id.edittext_value);
        _propertySpinner.setAdapter(new ArrayAdapter<>(getActivity(), android.R.layout
                .simple_list_item_1, getNotifiablePropertyNames()));
        _propertySpinner.setOnItemSelectedListener(this);
        view.findViewById(R.id.save_notifications).setOnClickListener(this);
        _recyclerView.setAdapter(new ContactListAdapter(PropertyNotificationFragment.this));
        _recyclerView.setVisibility(View.VISIBLE);
        if ( _propertySpinner.getCount() > 0 ) {
            _propertySpinner.setSelection(0);
        }

        if ( _originalTrigger != null ) {
            updateUI();
        }
        _sendPushCheckbox = (CheckBox) view.findViewById(R.id.send_push_notifications);
        _sendPushCheckbox.setChecked(false);
        _sendPushCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    _pushToDevice =true;
                } else {
                    _pushToDevice =false;
                }
            }
        });
        getActivity().setTitle(getString(R.string.property_notification));
        _relativeLayoutComparators.setVisibility(View.GONE);

        _tabLayoutNotifications.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()){
                    case 0:
                        _trigger.setTriggerType(TriggerType.Change.stringValue());
                        _relativeLayoutComparators.setVisibility(View.GONE);
                        break;
                    case 1:
                        _trigger.setTriggerType(TriggerType.Absolute.stringValue());
                        _relativeLayoutComparators.setVisibility(View.VISIBLE);
                        break;
                    case 2:
                        _trigger.setTriggerType(TriggerType.Always.stringValue());
                        _relativeLayoutComparators.setVisibility(View.GONE);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        _tabLayoutComparator.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch(tab.getPosition()){
                    case 0:
                        _trigger.setCompareType("==");
                        break;
                    case 1:
                        _trigger.setCompareType(">");
                        break;
                    case 2:
                        _trigger.setCompareType("<");
                        break;
                    case 3:
                        _trigger.setCompareType(">=");
                        break;
                    case 4:
                        _trigger.setCompareType("<=");
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });



        return view;
    }

    private void updateUI() {
        if ( _originalTrigger == null ) {
            Log.e(LOG_TAG, "trigger: No _originalTrigger");
            return;
        }

        if ( _originalTrigger.getPropertyNickname() == null ) {
            Log.e(LOG_TAG, "trigger: No property nickname");
        } else {
            // Select the property in our list
            String[] props = getNotifiablePropertyNames();
            for (int i = 0; i < props.length; i++) {
                if (props[i].equals(_originalTrigger.getPropertyNickname())) {
                    _propertySpinner.setSelection(i);
                    break;
                }
            }
        }

        switch(_originalTrigger.getTriggerType()){
            case TRIGGER_CHANGE:
                _tabLayoutNotifications.getTabAt(0).select();
                break;
            case TRIGGER_COMPARE_ABSOLUTE:
                _tabLayoutNotifications.getTabAt(1).select();
                if(_originalTrigger.getCompareType() != null){
                    switch(_originalTrigger.getCompareType()){
                        case "==":
                            _tabLayoutComparator.getTabAt(0).select();
                            break;
                        case ">":
                            _tabLayoutComparator.getTabAt(1).select();
                            break;
                        case "<":
                            _tabLayoutComparator.getTabAt(2).select();
                            break;
                        case ">=":
                            _tabLayoutComparator.getTabAt(3).select();
                            break;
                        case "<=":
                            _tabLayoutComparator.getTabAt(4).select();
                            break;
                    }
                }
                _edittextValue.setText(_originalTrigger.getValue());
                break;
            case TRIGGER_ALWAYS:
                _tabLayoutNotifications.getTabAt(2).select();
                break;

        }

        _nameEditText.setText(_originalTrigger.getDeviceNickname());

        _originalTrigger.fetchApps(
                new Response.Listener<AylaPropertyTriggerApp[]>() {
                    @Override
                    public void onResponse(AylaPropertyTriggerApp[] response) {
                        for (AylaPropertyTriggerApp propertyTriggerApp : response) {
                            AylaServiceApp.NotificationType notificationType= propertyTriggerApp
                                    .getNotificationType();

                            if(AylaServiceApp.NotificationType.GooglePush.equals(notificationType)
                                    || AylaServiceApp.NotificationType.BaiduPush.equals(notificationType)) {
                                //Now check if the registration id matches
                                    if(TextUtils.equals(PushNotification.registrationId,
                                            propertyTriggerApp.getRegistrationId())) {
                                        _sendPushCheckbox.setChecked(true);
                                    }
                            }
                            String contactId = propertyTriggerApp.getContactId();
                            if (contactId == null) {
                                continue;
                            }
                            AylaContact contact= ContactHelper.getContactByID(Integer.parseInt
                                    (contactId));
                            if(contact == null) {
                                return;
                            }
                            switch (propertyTriggerApp.getNotificationType()) {
                                case SMS:
                                    _smsContacts.add(contact);
                                    break;
                                case EMail:
                                    _emailContacts.add(contact);
                                    break;
                            }
                        }
                        _recyclerView.getAdapter().notifyDataSetChanged();
                    }
                },
                new ErrorListener() {
                    @Override
                    public void onErrorResponse(AylaError error) {
                        Toast.makeText(MainActivity.sharedInstance(), error.toString(), Toast.LENGTH_LONG)
                                .show();
                    }
                });
    }

    @Override
    public void emailTapped(AylaContact contact) {
        Log.d(LOG_TAG, "Email tapped: " + contact);
        if (TextUtils.isEmpty(contact.getEmail())) {
            Toast.makeText(getActivity(), R.string.contact_email_required,
                    Toast.LENGTH_SHORT).show();
            return;
        }
        if ( _emailContacts.contains(contact) ) {
            _emailContacts.remove(contact);
            Toast.makeText(MainActivity.sharedInstance(), "Email Notifications removed for: " +
                    contact.getDisplayName(), Toast.LENGTH_LONG).show();
        } else {
            _emailContacts.add(contact);
            Toast.makeText(MainActivity.sharedInstance(), "Email Notifications added for: " +
                    contact.getDisplayName(), Toast.LENGTH_LONG).show();
        }
        _recyclerView.getAdapter().notifyDataSetChanged();
    }

    @Override
    public void smsTapped(AylaContact contact) {
        Log.d(LOG_TAG, "SMS tapped: " + contact);
        if (TextUtils.isEmpty(contact.getPhoneNumber())) {
            Toast.makeText(getActivity(), R.string.contact_phone_required,
                    Toast.LENGTH_SHORT).show();
            return;
        }
        if ( _smsContacts.contains(contact) ) {
            _smsContacts.remove(contact);
            Toast.makeText(MainActivity.sharedInstance(), "SMS Notifications removed for: " +
                    contact.getDisplayName(), Toast.LENGTH_LONG).show();
        } else {
            _smsContacts.add(contact);
            Toast.makeText(MainActivity.sharedInstance(), "SMS Notifications added for: " +
                    contact.getDisplayName(), Toast.LENGTH_LONG).show();
        }
        _recyclerView.getAdapter().notifyDataSetChanged();
    }

    @Override
    public void contactTapped(AylaContact contact) {
        Log.d(LOG_TAG, "Contact tapped: " + contact);
        if ( _smsContacts.contains(contact) || _emailContacts.contains(contact)  ) {
            _smsContacts.remove(contact);
            _emailContacts.remove(contact);
        }
        _recyclerView.getAdapter().notifyDataSetChanged();
    }

    @Override
    public void contactLongTapped(AylaContact contact) {
        Log.d(LOG_TAG, "Contact long tapped: " + contact);
    }

    @Override
    public int colorForIcon(AylaContact contact, IconType iconType) {
        switch ( iconType ) {
            case ICON_SMS:
                if ( _smsContacts.contains(contact) ) {
                    return ContextCompat.getColor(this.getContext(), R.color.app_theme_accent);
                } else {
                    return ContextCompat.getColor(this.getContext(), R.color.disabled_text);
                }

            case ICON_EMAIL:
                if ( _emailContacts.contains(contact) ) {
                    return ContextCompat.getColor(this.getContext(), R.color.app_theme_accent);
                } else {
                    return ContextCompat.getColor(this.getContext(), R.color.disabled_text);
                }
        }
        return ContextCompat.getColor(this.getContext(), R.color.disabled_text);
    }

    @Override
    public void onClick(View v) {
        // Save notifications
        Log.d(LOG_TAG, "Save Notifications");

        // Make sure things are set up right
        if ( _nameEditText.getText().toString().isEmpty() ) {
            Toast.makeText(getActivity(), R.string.choose_name,
                    Toast.LENGTH_LONG).show();
            _nameEditText.requestFocus();
            return;
        }

        String propName = getNotifiablePropertyNames()[_propertySpinner.getSelectedItemPosition()];
        final AylaProperty prop = _device.getProperty(propName);
        if ( prop == null ) {
            Toast.makeText(getActivity(), R.string.unknown_error, Toast.LENGTH_LONG).show();
            return;
        }

        // Make sure somebody is selected
        if ( _emailContacts.size() + _smsContacts.size() == 0 && !_pushToDevice) {
            Toast.makeText(getActivity(), R.string.no_contacts_selected,
                    Toast.LENGTH_LONG).show();
            return;
        }

        // Now we should be set to create the trigger and trigger apps.

        final AylaPropertyTrigger trigger = new AylaPropertyTrigger();
        trigger.setDeviceNickname(_nameEditText.getText().toString());
        trigger.setBaseType(prop.getBaseType());
        trigger.setActive(true);
        trigger.setTriggerType(_trigger.getTriggerType());
        if(_trigger != null && _trigger.getTriggerType().equals(
                TriggerType.Absolute.stringValue())){
            trigger.setCompareType(_trigger.getCompareType());
            String value = _edittextValue.getText().toString();
            if(value == null || value.isEmpty()){
                Toast.makeText(getContext(), getString(R.string.error_value), Toast.LENGTH_SHORT).show();
                return;
            } else{
                trigger.setValue(value);
            }
        }
        prop.createTrigger(trigger, new Response.Listener<AylaPropertyTrigger>() {
                    @Override
                    public void onResponse(AylaPropertyTrigger response) {
                        createAppNotifications(response,prop);
                    }
                },
                new ErrorListener() {
                    @Override
                    public void onErrorResponse(AylaError error) {
                        Toast.makeText(getContext(), error.toString(), Toast.LENGTH_LONG)
                                .show();
                    }
                });

    }

    private void createAppNotifications(final AylaPropertyTrigger trigger, final AylaProperty
            property){
        final List<AylaError> aylaErrorList= new ArrayList<>();
        for (AylaContact emailContact:_emailContacts){
            AylaPropertyTriggerApp triggerApp = new AylaPropertyTriggerApp();
            triggerApp.setEmailAddress(emailContact.getEmail());
            //AylaEmailTemplate template = new AylaEmailTemplate();
            triggerApp.configureAsEmail(emailContact, "[[property_name]] [[property_value]]", null,
                    null);
            trigger.createApp(triggerApp,
                    new Response.Listener<AylaPropertyTriggerApp>() {
                        @Override
                        public void onResponse(AylaPropertyTriggerApp response) {
                            AylaLog.d(LOG_TAG, "Successfully created Trigger App for "+
                                    response.getEmailAddress());
                        }
                    },
                    new ErrorListener() {
                        @Override
                        public void onErrorResponse(AylaError error) {
                            aylaErrorList.add(error);
                            Toast.makeText(getContext(), error.toString(), Toast.LENGTH_LONG)
                                    .show();
                        }
                    });
        }
        for (AylaContact smsContact:_smsContacts){
            AylaPropertyTriggerApp triggerApp = new AylaPropertyTriggerApp();
            triggerApp.configureAsSMS(smsContact, "[[property_name]] [[property_value]]");

            trigger.createApp(triggerApp,
                    new Response.Listener<AylaPropertyTriggerApp>() {
                        @Override
                        public void onResponse(AylaPropertyTriggerApp response) {
                            AylaLog.d(LOG_TAG, "Successfully created Trigger App for "+
                                    response.getPhoneNumber());
                        }
                    },
                    new ErrorListener() {
                        @Override
                        public void onErrorResponse(AylaError error) {
                            aylaErrorList.add(error);
                            Toast.makeText(MainActivity.sharedInstance(), error.toString(), Toast.LENGTH_LONG)
                                    .show();
                        }
                    });
        }
        if(_pushToDevice) {
            AylaPropertyTriggerApp triggerApp = new AylaPropertyTriggerApp();
            String registrationId =PushNotification.registrationId;
            triggerApp.configureAsPushAndroid(registrationId
                    , "[[property_name]] " + "[[property_value]] for Google Push"
                    , "default"
                    , "Google Push meta data");

            trigger.createApp(triggerApp,
                    new Response.Listener<AylaPropertyTriggerApp>() {
                        @Override
                        public void onResponse(AylaPropertyTriggerApp response) {
                            AylaLog.d(LOG_TAG, "Successfully created Trigger App for Push");
                        }
                    },
                    new ErrorListener() {
                        @Override
                        public void onErrorResponse(AylaError error) {
                            aylaErrorList.add(error);
                            Toast.makeText(MainActivity.sharedInstance(), error.toString(), Toast.LENGTH_LONG)
                                    .show();
                        }
                    });
        }
        if(aylaErrorList.isEmpty()) {
            //There are no errors in creating the apps
            Toast.makeText(getActivity(), R.string.notification_created,
                    Toast.LENGTH_LONG).show();
            if (_originalTrigger != null) {
                property.deleteTrigger(_originalTrigger, new Response.Listener<
                                AylaAPIRequest.EmptyResponse>() {
                            @Override
                            public void onResponse(AylaAPIRequest.EmptyResponse response) {
                                AylaLog.d(LOG_TAG, "Successfully Deleted the old trigger");
                                getFragmentManager().popBackStack();
                            }
                        },
                        new ErrorListener() {
                            @Override
                            public void onErrorResponse(AylaError error) {
                                Toast.makeText(MainActivity.sharedInstance(), error.toString(), Toast.LENGTH_LONG)
                                        .show();
                            }
                        });
            }
            else {
                getFragmentManager().popBackStack();
            }
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        String propertyName = getNotifiablePropertyNames()[position];
        Log.d(LOG_TAG, "onItemSelected: "+ _propertySpinner.getSelectedItem() + " == " +
                propertyName);
        AylaProperty prop = _device.getProperty(propertyName);
        if ( prop == null ) {
            Log.e(LOG_TAG, "Failed to get property: " + propertyName);
            return;
        }

        Log.d(LOG_TAG, "Property " + propertyName + " base type: " + prop.getBaseType());
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        Log.e(LOG_TAG, "Nothing selected!");
    }
    private String friendlyNameForPropertyName(AylaProperty prop) {
        if (prop.getDisplayName() != null) {
            return prop.getDisplayName();
        }
        return prop.getName();
    }
    private String[] getNotifiablePropertyNames(){
        AylaSystemSettings.DeviceDetailProvider provider =
                AylaNetworks.sharedInstance().getSystemSettings().deviceDetailProvider;

        if (provider != null) {
            return provider.getManagedPropertyNames(_device);
        }
        return new String[0];
    }

}

