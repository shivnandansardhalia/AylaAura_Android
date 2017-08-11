package com.aylanetworks.aura;

/*
 * Android_AylaSDK
 *
 * Copyright 2016 Ayla Networks, all rights reserved
 */

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.text.InputType;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.aylanetworks.aura.localdevice.GrillRightDevice;
import com.aylanetworks.aura.localdevice.GrillRightFragment;
import com.aylanetworks.aura.test.TestRunnerFragment;
import com.aylanetworks.aura.util.FilePropertyPreviewUtil;
import com.aylanetworks.aylasdk.AylaAPIRequest;
import com.aylanetworks.aylasdk.AylaContact;
import com.aylanetworks.aylasdk.AylaDatapoint;
import com.aylanetworks.aylasdk.AylaDatapointBlob;
import com.aylanetworks.aylasdk.AylaDevice;
import com.aylanetworks.aylasdk.AylaNetworks;
import com.aylanetworks.aylasdk.AylaProperty;
import com.aylanetworks.aylasdk.AylaPropertyTrigger;
import com.aylanetworks.aylasdk.AylaPropertyTriggerApp;
import com.aylanetworks.aylasdk.AylaServiceApp;
import com.aylanetworks.aylasdk.AylaSessionManager;
import com.aylanetworks.aylasdk.AylaSystemSettings;
import com.aylanetworks.aylasdk.AylaTimeZone;
import com.aylanetworks.aylasdk.AylaUser;
import com.aylanetworks.aylasdk.MultipartProgressListener;
import com.aylanetworks.aylasdk.change.Change;
import com.aylanetworks.aylasdk.error.AylaError;
import com.aylanetworks.aylasdk.error.ErrorListener;
import com.aylanetworks.aylasdk.error.PreconditionError;
import com.aylanetworks.aylasdk.error.RequestFuture;
import com.aylanetworks.aylasdk.util.TypeUtils;
import com.aylanetworks.aylasdk.localdevice.AylaLocalDevice;
import com.aylanetworks.aylasdk.localdevice.ble.AylaBLEDevice;

import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TimeZone;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link DeviceDetailFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DeviceDetailFragment extends Fragment implements AylaDevice.DeviceChangeListener {
    private final static String ARG_DSN = "dsn";
    private final static String DETAIL_TAG = "device_details";

    private AylaDevice _device;

    private TextView _productName;
    private TextView _deviceDSN;
    private TextView _connectionStatus;
    private ListView _propretiesListView;

    // Dynamic menu item IDs
    private static final int MENU_CONNECT_LOCAL = Menu.FIRST;

    public DeviceDetailFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param dsn DSN of the device to show details for
     * @return A new instance of fragment DeviceDetailFragment.
     */
    public static DeviceDetailFragment newInstance(String dsn) {
        DeviceDetailFragment fragment = new DeviceDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DSN, dsn);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        if (getArguments() != null) {
            String dsn = getArguments().getString(ARG_DSN);
            AylaSessionManager sm = MainActivity.getSession();
            if (sm != null) {
                _device = sm.getDeviceManager().deviceWithDSN(dsn);
                _device.addListener(this);
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.device_details, menu);

        if (_device instanceof GrillRightDevice) {
            inflater.inflate(R.menu.grill_right, menu);
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.removeItem(MENU_CONNECT_LOCAL);
        // TODO: BSK: Remove this
        if (_device instanceof AylaLocalDevice) {
            AylaLocalDevice localDevice = (AylaLocalDevice)_device;
            String menuText;
            if (localDevice.isConnectedLocal()) {
                menuText = MainActivity.sharedInstance().getString(R.string.disconnect_local);
            } else {
                menuText = MainActivity.sharedInstance().getString(R.string.connect_local);
            }

            menu.add(Menu.NONE, MENU_CONNECT_LOCAL, Menu.NONE, menuText);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()){
            case MENU_CONNECT_LOCAL:

                AylaLocalDevice localDevice = (AylaLocalDevice)_device;
                boolean isConnected = localDevice.isConnectedLocal();
                final String responseMessage = MainActivity.sharedInstance().getString(
                        isConnected ? R.string.disconnect_local_success :
                                R.string.connect_local_success);

                final String errorMessage = MainActivity.sharedInstance().getString(
                    isConnected ? R.string.disconnect_local_failure : R.string
                            .connect_local_failure);

                Response.Listener<AylaAPIRequest.EmptyResponse> successListener =
                        new Response.Listener<AylaAPIRequest.EmptyResponse>() {
                        @Override
                        public void onResponse(AylaAPIRequest.EmptyResponse response) {
                            Toast.makeText(MainActivity.sharedInstance(), responseMessage,
                                    Toast.LENGTH_LONG).show();
                        }
                    };

                ErrorListener errorListener = new ErrorListener() {
                        @Override
                        public void onErrorResponse(AylaError error) {
                            Toast.makeText(MainActivity.sharedInstance(), errorMessage,
                                    Toast.LENGTH_LONG).show();
                        }
                    };

                if (isConnected) {
                    localDevice.disconnectLocal(successListener, errorListener);
                } else {
                    localDevice.connectLocal(successListener, errorListener);
                }
                break;

            case R.id.action_unregister:
                new AlertDialog.Builder(getContext())
                        .setTitle(R.string.action_unregister)
                        .setMessage(R.string.unregister_confirm)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                final Snackbar sb = Snackbar.make(_propretiesListView,
                                        R.string.unregistering, Snackbar.LENGTH_INDEFINITE);
                                final AylaAPIRequest request = _device.unregister(
                                        new Response.Listener<AylaAPIRequest.EmptyResponse>() {
                                            @Override
                                            public void onResponse(AylaAPIRequest.EmptyResponse response) {
                                                sb.dismiss();
                                                MainActivity.sharedInstance().onBackPressed();
                                            }
                                        },
                                        new ErrorListener() {
                                            @Override
                                            public void onErrorResponse(AylaError error) {
                                                sb.dismiss();
                                                Snackbar.make(_propretiesListView, error.getMessage(),
                                                        Snackbar.LENGTH_LONG).show();
                                            }
                                        });
                                if (request != null) {
                                    sb.setAction(android.R.string.cancel, new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            request.cancel();
                                            sb.dismiss();
                                        }
                                    }).show();
                                }
                            }
                        })
                        .setNegativeButton(android.R.string.no, null)
                        .create().show();
                return true;

            case R.id.action_factory_reset:
                new AlertDialog.Builder(getContext())
                        .setTitle(R.string.action_factory_reset)
                        .setMessage(R.string.factory_reset_confirm)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                final Snackbar sb = Snackbar.make(_propretiesListView,
                                        R.string.resetting, Snackbar.LENGTH_INDEFINITE);
                                final AylaAPIRequest request = _device.factoryReset(
                                        new Response.Listener<AylaAPIRequest.EmptyResponse>() {
                                            @Override
                                            public void onResponse(AylaAPIRequest.EmptyResponse response) {
                                                sb.dismiss();
                                                MainActivity.sharedInstance().onBackPressed();
                                            }
                                        },
                                        new ErrorListener() {
                                            @Override
                                            public void onErrorResponse(AylaError error) {
                                                sb.dismiss();
                                                Snackbar.make(_propretiesListView, error.getMessage(),
                                                        Snackbar.LENGTH_LONG).show();
                                            }
                                        });
                                if (request != null) {
                                    sb.setAction(android.R.string.cancel, new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            if (request != null) {
                                                request.cancel();
                                            }
                                            sb.dismiss();
                                        }
                                    }).show();
                                }
                            }
                        })
                        .setNegativeButton(android.R.string.no, null)
                        .create().show();
                return true;

            case R.id.action_run_tests:
                TestRunnerFragment frag = TestRunnerFragment.newInstance(_device);
                MainActivity.sharedInstance().pushFragment(frag);
                return true;

            case R.id.action_timezone:
                changeTimeZone();
                return true;

            case R.id.action_rename_device:
                productNameClicked();
                return true;

            case R.id.action_device_details:
                showDeviceDetails();
                return true;

            case R.id.action_lan_ota:
                LanOTAFragment otaFragment = LanOTAFragment.newInstance(_deviceDSN.getText().toString());
                MainActivity.sharedInstance().pushFragment(otaFragment);
                return true;

            case R.id.action_fetch_all_properties:
                fetchAllProperties();
                return true;

            case R.id.action_grillright_ui:
                GrillRightFragment grillRightFrag = GrillRightFragment.newInstance(
                        (GrillRightDevice)_device);
                MainActivity.sharedInstance().pushFragment(grillRightFrag);
        }

        return super.onOptionsItemSelected(item);
    }

    private void showDeviceDetails() {
        DeviceDetailDialog dlg = DeviceDetailDialog.newInstance(_device);
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag(DETAIL_TAG);
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);
        dlg.show(ft, DETAIL_TAG);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (_device != null) {
            _device.removeListener(this);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_device_detail, container, false);

        _productName = (TextView) view.findViewById(R.id.product_name);
        _deviceDSN = (TextView) view.findViewById(R.id.dsn);
        _connectionStatus = (TextView) view.findViewById(R.id.connection_status);
        _propretiesListView = (ListView) view.findViewById(R.id.properties_listview);

        updateDeviceDetails();
        Button deviceNotificationButton = (Button) view.findViewById(R.id.
                device_notifications_button);
        deviceNotificationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                notificationsClicked();
            }
        });

        Button btnShare = (Button) view.findViewById(R.id.btn_share);
        btnShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareClicked();
            }
        });

        Button deviceSchedulesButton = (Button) view.findViewById(R.id.
                device_schedules_button);
        deviceSchedulesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                schedulesClicked();
            }
        });
        if (canSetSchedules())
            deviceSchedulesButton.setVisibility(View.VISIBLE);
        else
            deviceSchedulesButton.setVisibility(View.GONE);


        String [] propNames = getNotifiablePropertyNames();
        if(propNames == null)
            deviceNotificationButton.setVisibility(View.GONE);
        else
            deviceNotificationButton.setVisibility(View.VISIBLE);
        createPropertyTriggerForRedLed1();
        return view;
    }
    private void createPropertyTriggerForRedLed1() {
        final AylaProperty prop = _device.getProperty("Red_LED");
        prop.fetchTriggers(new Response.Listener<AylaPropertyTrigger[]>() {
                               @Override
                               public void onResponse(AylaPropertyTrigger[] response) {
                                   if (response != null && response.length ==0) {



                                       final AylaPropertyTrigger trigger = new AylaPropertyTrigger();

                                       trigger.setDeviceNickname("Red_LED_NICK_NAME");
                                       trigger.setBaseType(prop.getBaseType());
                                       trigger.setActive(true);
                                       trigger.setTriggerType(AylaPropertyTrigger.TriggerType.Absolute.stringValue());
                                       trigger.setCompareType("==");
                                       trigger.setValue("1");
                                       prop.createTrigger(trigger, new Response.Listener<AylaPropertyTrigger>() {
                                                   @Override
                                                   public void onResponse(AylaPropertyTrigger response) {


                                                       createAppNotifications(response, prop);
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

    private void createAppNotifications(final AylaPropertyTrigger trigger, final AylaProperty property) {

        createTriggers(trigger, property);
    }

    private void createTriggers(final AylaPropertyTrigger trigger, AylaProperty property){
        AylaNetworks.sharedInstance()
                .getSessionManager(MainActivity.SESSION_NAME)
                .fetchUserProfile(new Response.Listener<AylaUser>() {
                                      @Override
                                      public void onResponse(AylaUser response) {

                                          AylaContact contact = new AylaContact();
                                          contact.setFirstname(response.getFirstname());
                                          contact.setLastname(response.getLastname());
                                          contact.setDisplayName(response.getFirstname());
                                          contact.setEmail(response.getEmail());
                                          contact.setCountry(response.getCountry());
                                          contact.setPhoneCountryCode(response.getPhoneCountryCode());
                                          contact.setPhoneNumber(response.getPhone());
                                          contact.setPushType(AylaServiceApp.PushType.GooglePush);
                                          contact.setWantsEmailNotification(true);
                                          contact.setWantsSmsNotification(true);

                                          RequestFuture<AylaContact> future = RequestFuture.newFuture();
                                          MainActivity.getSession().createContact(contact, future, future);


                                          ///EMAIL
                                          AylaPropertyTriggerApp triggerApp = new AylaPropertyTriggerApp();
                                          triggerApp.setEmailAddress(contact.getEmail());
                                          //AylaEmailTemplate template = new AylaEmailTemplate();
                                          triggerApp.configureAsEmail(contact, "[[property_name]] [[property_value]]", null,
                                                  null);
                                          trigger.createApp(triggerApp,
                                                  new Response.Listener<AylaPropertyTriggerApp>() {
                                                      @Override
                                                      public void onResponse(AylaPropertyTriggerApp response) {

                                                      }
                                                  },
                                                  new ErrorListener() {
                                                      @Override
                                                      public void onErrorResponse(AylaError error) {

                                                          Toast.makeText(getContext(), error.toString(), Toast.LENGTH_LONG)
                                                                  .show();
                                                      }
                                                  });


                                          //SMS
                                          AylaPropertyTriggerApp triggerAppSMS = new AylaPropertyTriggerApp();
                                          triggerAppSMS.configureAsSMS(contact, "[[property_name]] [[property_value]]");

                                          trigger.createApp(triggerAppSMS,
                                                  new Response.Listener<AylaPropertyTriggerApp>() {
                                                      @Override
                                                      public void onResponse(AylaPropertyTriggerApp response) {
                                                      }
                                                  },
                                                  new ErrorListener() {
                                                      @Override
                                                      public void onErrorResponse(AylaError error) {
                                                          Toast.makeText(MainActivity.sharedInstance(), error.toString(), Toast.LENGTH_LONG)
                                                                  .show();
                                                      }
                                                  });


                                          //PUSH

                                          AylaPropertyTriggerApp triggerAppPUSH = new AylaPropertyTriggerApp();

                                          if (contact.getPushType() == null
                                                  || contact.getPushType() == AylaServiceApp.PushType.GooglePush) {
                                              String registrationId =PushNotification.registrationId;
                                              triggerAppPUSH.configureAsPushAndroid(registrationId
                                                      , "[[property_name]] " + "[[property_value]] for Google Push"
                                                      , "default"
                                                      , "Google Push meta data");
                                          } else if (contact.getPushType() == AylaServiceApp.PushType.BaiduPush) {

                                              Toast.makeText(MainActivity.sharedInstance(), "Baidu push is unavailable!", Toast.LENGTH_SHORT).show();
                                          }

                                          trigger.createApp(triggerAppPUSH,
                                                  new Response.Listener<AylaPropertyTriggerApp>() {
                                                      @Override
                                                      public void onResponse(AylaPropertyTriggerApp response) {

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
                                  },
                        new ErrorListener() {
                            @Override
                            public void onErrorResponse(AylaError error) {

                            }
                        });
    }
    private void schedulesClicked () {
        //check the model
        if(AuraDeviceDetailProvider.OEM_MODEL_EVB.equals(_device.getOemModel())) {
            MainActivity.sharedInstance().pushFragment(ScheduleFragment.newInstance
                    (_device, "schedule_in"));
        } else if(AuraDeviceDetailProvider.OEM_MODEL_PLUG.equals(_device.getOemModel())) {
            MainActivity.sharedInstance().pushFragment(ScheduleFragment.newInstance
                    (_device, "sched1"));
        }
        else {
            Toast.makeText(getActivity(),
                    "Schedules are not not supported for this device in Aura",
                    Toast.LENGTH_LONG).show();
        }
    }

    private boolean canSetSchedules() {
        return  (AuraDeviceDetailProvider.OEM_MODEL_EVB.equals(_device.getOemModel()) ||
                AuraDeviceDetailProvider.OEM_MODEL_PLUG.equals(_device.getOemModel()));
    }

    private void productNameClicked() {
        final EditText editText = new EditText(getActivity());
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        editText.setText(_device.getProductName());
        editText.setSelectAllOnFocus(true);
        editText.requestFocus();

        new AlertDialog.Builder(getActivity())
                .setIcon(R.drawable.log_info)
                .setTitle(R.string.rename_device_text)
                .setView(editText)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        changeDeviceName(editText.getText().toString());
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create().show();
        editText.requestFocus();
    }

    private void changeDeviceName(final String newDeviceName) {
        final Snackbar sb = Snackbar.make(_propretiesListView, R.string.renaming_device,
                Snackbar.LENGTH_INDEFINITE);
        final AylaAPIRequest request = _device.updateProductName(newDeviceName, new Response
                        .Listener<AylaAPIRequest.EmptyResponse>() {
                    @Override
                    public void onResponse(AylaAPIRequest.EmptyResponse response) {
                        updateDeviceDetails();
                        sb.dismiss();
                    }
                },
                new ErrorListener() {
                    @Override
                    public void onErrorResponse(AylaError error) {
                        sb.dismiss();
                        Toast.makeText(MainActivity.sharedInstance(), error.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });

        if (request != null) {
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

    private void fetchAllProperties() {
        final Snackbar sb = Snackbar.make(_propretiesListView, R.string.fetching_all_properties,
                Snackbar.LENGTH_INDEFINITE);
        final AylaAPIRequest request = _device.fetchPropertiesCloud(null, new Response.Listener<AylaProperty[]>() {
                    @Override
                    public void onResponse(AylaProperty[] response) {
                        // We should automatically update our device from the listener
                        sb.dismiss();
                    }
                },
                new ErrorListener() {
                    @Override
                    public void onErrorResponse(AylaError error) {
                        sb.dismiss();
                        Toast.makeText(MainActivity.sharedInstance(), error.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });

        if (request != null) {
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

    private void updateDeviceDetails() {
        if (_device != null && isAdded()) {
            SpannableString productName = new SpannableString(_device.getProductName());
            productName.setSpan(new UnderlineSpan(), 0, productName.length(), 0);
            _productName.setText(productName);

            String lan = getString(R.string.via_lan);
            if (_device instanceof AylaBLEDevice) {
                lan = getString(R.string.via_bluetooth);
            } else if (_device instanceof AylaLocalDevice) {
                lan = getString(R.string.via_local_connection);
            }

            String cloud = getString(R.string.via_cloud);

            _deviceDSN.setText(_device.getDsn());
            String connectionStatus = _device.getConnectionStatus().toString() + " (" +
                    (_device.isLanModeActive() ? lan : cloud) + ")";
            _connectionStatus.setText(connectionStatus);

            Activity activity = getActivity();
            if (activity != null) {
                activity.setTitle(_device.getProductName());
            }

            // Sort the list. Managed properties first, then umnanaged. Order is alphabetical by
            // property name at that point.
            List<AylaProperty> propertyList = _device.getProperties();
            Collections.sort(propertyList, new Comparator<AylaProperty>() {
                @Override
                public int compare(AylaProperty lhs, AylaProperty rhs) {
                    if (isManaged(lhs) && !isManaged(rhs)) {
                        return -1;
                    }
                    if (isManaged(rhs) && !isManaged(lhs)) {
                        return 1;
                    }

                    return lhs.getName().compareTo(rhs.getName());
                }
            });
            _propretiesListView.setAdapter(new PropertyAdapter(getContext(),
                    propertyList));
        }
    }

    @Override
    public void deviceChanged(AylaDevice device, Change change) {
        updateDeviceDetails();
    }

    @Override
    public void deviceError(AylaDevice device, AylaError error) {
        if (isVisible()) {
            Snackbar.make(_propretiesListView, error.getMessage(), Snackbar.LENGTH_SHORT).show();
        }
    }

    @Override
    public void deviceLanStateChanged(AylaDevice device, boolean lanModeEnabled) {
        updateDeviceDetails();
    }

    private void notificationsClicked () {
        MainActivity.sharedInstance().pushFragment(NotificationListFragment.newInstance
                (_device));
    }

    private void shareClicked () {
        MainActivity.sharedInstance().pushFragment(ShareFragment.newInstance
                (_device));
    }

    public class PropertyAdapter extends ArrayAdapter<AylaProperty> {
        private Context _context;
        private List<AylaProperty> _items;

        public PropertyAdapter(Context context, List<AylaProperty> items) {
            super(context, -1, items);
            _context = context;
            _items = items;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater)
                    _context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            View v = convertView;
            if (v == null) {
                v = inflater.inflate(R.layout.list_item_property, parent, false);
            }
            TextView name = (TextView) v.findViewById(R.id.property_name);
            TextView textviewValue = (TextView) v.findViewById(R.id.textview_property_value);
            final Switch switchValue = (Switch) v.findViewById(R.id.switch_property_value);
            switchValue.setChecked(false);
            final AylaProperty property = getItem(position);
            name.setText(property.getName());
            String value = String.valueOf(property.getValue());
            if(property.getBaseType().equals("boolean")){
                switchValue.setChecked(value.equals("1"));
                textviewValue.setVisibility(View.GONE);
                switchValue.setVisibility(View.VISIBLE);
            } else{
                textviewValue.setText(String.valueOf(property.getValue()));
                switchValue.setVisibility(View.GONE);
                textviewValue.setVisibility(View.VISIBLE);
            }
            // Is this a managed property?
            if (isManaged(property)) {
                name.setTextColor(ContextCompat.getColor(getContext(), R.color.managed_property));
            } else {
                name.setTextColor(ContextCompat.getColor(getContext(), R.color.unmanaged_property));
            }

            switchValue.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    onBooleanPropertyChanged(property, isChecked?1:0);
                    if(property.isReadOnly()){
                        switchValue.setChecked(!isChecked);
                    }
                }
            });
            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(!property.getBaseType().equals("boolean")){
                        onPropertyTapped(property);
                    }
                }
            });

            return v;
        }
    }

    private boolean isManaged(AylaProperty property) {
        String[] managedProps = AylaNetworks.sharedInstance().getSystemSettings()
                .deviceDetailProvider.getManagedPropertyNames(_device);
        if (managedProps == null) {
            return false;
        }

        for (String prop : managedProps) {
            if (TextUtils.equals(prop, property.getName())) {
                return true;
            }
        }
        return false;
    }

    private void onPropertyTapped(AylaProperty property) {

        if(!isAdded()){
            return;
        }
        if (property.isReadOnly()) {
            if (property.getBaseType().equals(AylaProperty.BASE_TYPE_FILE)) {
                filePropertyPreview(property);
            } else {
                Snackbar.make(_propretiesListView, R.string.property_read_only, Snackbar.LENGTH_SHORT)
                        .show();
            }
            return;
        }

        ErrorListener errorListener = new ErrorListener() {
            @Override
            public void onErrorResponse(AylaError error) {
                if (isAdded() && _propretiesListView.getContext() != null && isVisible()) {
                    Snackbar.make(_propretiesListView, error.getMessage(), Snackbar.LENGTH_SHORT)
                            .show();
                }
            }
        };

        String baseType = property.getBaseType();
        switch (baseType){
            case "boolean": {
                // Attempt to toggle the value
                Response.Listener<AylaDatapoint<Integer>> successListener = new Response
                        .Listener<AylaDatapoint<Integer>>() {
                    @Override
                    public void onResponse(AylaDatapoint response) {
                        updateDeviceDetails();
                    }
                };

                if (property.getValue() == null) {
                    displayEditValue(property, successListener, errorListener);
                } else {
                    try {
                        // We know this is an Integer-based property now
                        @SuppressWarnings("unchecked")
                        AylaProperty<Integer> booleanProperty = (AylaProperty<Integer>) property;
                        if (booleanProperty.getValue() == 1) {
                            booleanProperty.createDatapoint(0, null, successListener, errorListener);
                        } else {
                            booleanProperty.createDatapoint(1, null, successListener, errorListener);
                        }
                    } catch (ClassCastException ex) {
                        errorListener.onErrorResponse(new PreconditionError("property.getValue() " +
                                "error" + ex.getMessage()));
                    }
                }
                break;
            }
            case "string":
            case "integer":
            case "decimal": {
                Response.Listener<AylaDatapoint> successListener = new Response.Listener<AylaDatapoint>() {
                    @Override
                    public void onResponse(AylaDatapoint response) {
                        updateDeviceDetails();
                    }
                };

                displayEditValue(property, successListener, errorListener);
                break;
            }
            default:
                Snackbar.make(_propretiesListView, "Not supported in this version.", Snackbar
                        .LENGTH_SHORT).show();
        }
    }

    private void filePropertyPreview(AylaProperty property) {
        AylaDatapointBlob blob = new AylaDatapointBlob(property);

        File path = new File(Environment.getExternalStorageDirectory(), "aura");
        if (!path.exists()) {
            path.mkdirs();
        }
        final File file = new File(path, FilePropertyPreviewUtil.generateFileName(blob.getValue()));
        if (file.exists()) {
            preview(file);
        } else {
            final Snackbar snackbar = Snackbar.make(this._propretiesListView, "Loading...", Snackbar.LENGTH_INDEFINITE);
            final AylaAPIRequest request = blob.downloadToFile(file.getAbsolutePath(),
                    new MultipartProgressListener() {
                        @Override
                        public boolean isCanceled() {
                            snackbar.dismiss();
                            return false;
                        }

                        @Override
                        public void updateProgress(long sentOrRecvd, long total) {
                            snackbar.setText(String.format("Downloading:%d/%d", sentOrRecvd, total));
                        }
                    },
                    new Response.Listener<AylaAPIRequest.EmptyResponse>() {
                        @Override
                        public void onResponse(AylaAPIRequest.EmptyResponse response) {
                            preview(file);
                            snackbar.dismiss();
                        }
                    },
                    new ErrorListener() {
                        @Override
                        public void onErrorResponse(AylaError error) {
                            Snackbar.make(_propretiesListView, error.getMessage(), Snackbar.LENGTH_LONG).show();
                            file.delete();
                        }
                    });
            snackbar.setAction(android.R.string.cancel, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    request.cancel();
                    snackbar.dismiss();
                }
            });
            snackbar.show();
        }
    }

    private void preview(File file) {
        Intent intent = new Intent();
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(file), FilePropertyPreviewUtil.guessContentType(file));
        startActivity(intent);
    }

    private String[] getNotifiablePropertyNames(){
        AylaSystemSettings.DeviceDetailProvider provider =
                AylaNetworks.sharedInstance().getSystemSettings().deviceDetailProvider;

        if (provider != null) {
            return provider.getManagedPropertyNames(_device);
        }
        return null;
    }


    private void displayEditValue(final AylaProperty property,
                                  final Response.Listener successListener,
                                  final ErrorListener errorListener) {
        final EditText editText = new EditText(getActivity());
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        editText.setText(String.valueOf(property.getValue()));
        editText.setSelectAllOnFocus(true);
        editText.requestFocus();

        new AlertDialog.Builder(getActivity())
                .setIcon(R.drawable.log_info)
                .setTitle(String.format(getResources().getString(R.string.new_value_text), property
                        .getBaseType()))
                .setView(editText)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String newValue = editText.getText().toString();
                        Object value = TypeUtils.getTypeConvertedValue(property.getBaseType(),
                                newValue);
                        if(value == null){
                            Toast.makeText(getContext(), String.format("%s %s",
                                    getString(R.string.cannot_be_parsed), property.getBaseType()),
                                    Toast.LENGTH_SHORT).show();
                        } else{
                            property.createDatapoint(value, null, successListener, errorListener);
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create().show();
        editText.requestFocus();
    }

    private void showTimeZones(final AylaTimeZone timeZone) {
        android.app.AlertDialog.Builder builder =
                new android.app.AlertDialog.Builder(this.getContext());
        builder.setTitle(R.string.available_timezone);
        final String[] timezones = TimeZone.getAvailableIDs();
        int count = -1;
        for(String tzId: timezones){
            count++;
            if(tzId.equals(timeZone.tzId)){
                break;
            }
        }

        builder.setSingleChoiceItems(timezones, count, null);

        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ListView tzListView = ((AlertDialog)dialog).getListView();
                int selectedPos = tzListView.getCheckedItemPosition();
                if(selectedPos == -1){
                    return;
                }
                String tzId = timezones[selectedPos];
                _device.updateTimeZone(tzId, new Response.Listener<AylaTimeZone>() {
                    @Override
                    public void onResponse(AylaTimeZone response) {
                        Activity activity = getActivity();
                        if(activity != null){
                            View view = getActivity().findViewById(android.R.id
                                    .content);
                            Snackbar.make(view, R.string.time_zone_update_success,
                                    Snackbar.LENGTH_SHORT).show();
                        }

                    }
                }, new ErrorListener() {
                    @Override
                    public void onErrorResponse(AylaError error) {
                        Activity activity = getActivity();
                        if(activity != null){
                            View view = getActivity().findViewById(android.R.id
                                    .content);
                            Snackbar.make(view, R.string.time_zone_update_success,
                                    Snackbar.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });

        builder.setNegativeButton("Cancel", null );
        builder.show();
    }

    private void changeTimeZone(){
        _device.fetchTimeZone(new Response.Listener<AylaTimeZone>() {
            @Override
            public void onResponse(AylaTimeZone response) {
                showTimeZones(response);
            }
        }, new ErrorListener() {
            @Override
            public void onErrorResponse(AylaError error) {
                Activity activity = getActivity();
                if(activity != null){
                    View view = activity.findViewById(android.R.id.content);
                    Snackbar.make(view, "Error in fetching time zone: "+error.getLocalizedMessage(),
                            Snackbar.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void onBooleanPropertyChanged(AylaProperty property, int newValue){
        if(property.isReadOnly()){
            Snackbar.make(_propretiesListView, R.string.property_read_only, Snackbar.LENGTH_SHORT)
                    .show();
        } else{
            property.createDatapoint(newValue, null, new Response.Listener<AylaDatapoint>(){
                @Override
                public void onResponse(AylaDatapoint response) {

                }
            },new ErrorListener(){
                @Override
                public void onErrorResponse(AylaError error) {
                    if (isAdded() && _propretiesListView.getContext() != null && isVisible()) {
                        Snackbar.make(_propretiesListView, error.getMessage(), Snackbar.LENGTH_SHORT)
                                .show();
                    }
                }
            });
        }
    }
}
