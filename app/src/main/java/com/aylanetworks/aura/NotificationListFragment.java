package com.aylanetworks.aura;
/*
 * AylaSDK
 *
 * Copyright 2015 Ayla Networks, all rights reserved
 */
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.android.volley.Response;
import com.aylanetworks.aura.util.ContactHelper;
import com.aylanetworks.aylasdk.AylaAPIRequest;
import com.aylanetworks.aylasdk.AylaContact;
import com.aylanetworks.aylasdk.AylaLog;
import com.aylanetworks.aylasdk.AylaNetworks;
import com.aylanetworks.aylasdk.AylaDevice;
import com.aylanetworks.aylasdk.AylaProperty;
import com.aylanetworks.aylasdk.AylaPropertyTrigger;
import com.aylanetworks.aylasdk.AylaSessionManager;
import com.aylanetworks.aylasdk.AylaSystemSettings;
import com.aylanetworks.aylasdk.error.AylaError;
import com.aylanetworks.aylasdk.error.ErrorListener;

public class NotificationListFragment extends Fragment implements View.OnClickListener {

    private static final String ARG_DSN = "dsn";
    private static final String LOG_TAG = "NotificationListFragment";

    private RecyclerView _recyclerView;
    private TextView _emptyView;
    private AylaDevice _device;
    private List<AylaPropertyTrigger> _propertyTriggers;

    public static NotificationListFragment newInstance(AylaDevice device) {
        NotificationListFragment frag = new NotificationListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DSN, device.getDsn());
        frag.setArguments(args);
        return frag;
    }

    public NotificationListFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            String dsn = getArguments().getString(ARG_DSN);
            _device = MainActivity.getSession().getDeviceManager().deviceWithDSN(dsn);
        }
        getActivity().setTitle(getString(R.string.device_notifications));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notification_list, container, false);
        _emptyView = (TextView) view.findViewById(R.id.empty);

        // Set up the list view
        _recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        _recyclerView.setHasFixedSize(true);

        _recyclerView.setVisibility(View.GONE);
        _emptyView.setVisibility(View.VISIBLE);
        //_emptyView.setText(R.string.fetching_notifications);

        RecyclerView.LayoutManager lm  = new LinearLayoutManager(getActivity());
        _recyclerView.setLayoutManager(lm);

        ImageButton b = (ImageButton) view.findViewById(R.id.add_button);
        b.setOnClickListener(this);

        if (_device != null) {
            _propertyTriggers = new ArrayList<>();
            fetchTriggers();
            updateTriggerList();
        }

        return view;
    }

    private void updateTriggerList() {
        if (_propertyTriggers.isEmpty() ) {
            _recyclerView.setVisibility(View.GONE);
            _emptyView.setVisibility(View.VISIBLE);
            //_emptyView.setText(R.string.no_triggers_found);
        } else {
            _recyclerView.setVisibility(View.VISIBLE);
            _emptyView.setVisibility(View.GONE);
        }
        _recyclerView.setAdapter(new TriggerAdapter(this, _device, _propertyTriggers));
    }

    @Override
    public void onClick(View v) {
        // Add button tapped
        PropertyNotificationFragment frag = PropertyNotificationFragment.newInstance(_device, null);
        MainActivity.sharedInstance().pushFragment(frag);
    }

    private void onLongClick(final int index) {
        AylaPropertyTrigger trigger = _propertyTriggers.get(index);
        new AlertDialog.Builder(getActivity())
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(R.string.delete_notification_title)
                .setMessage(getActivity().getResources().getString(R.string
                        .delete_notification_message, trigger.getDeviceNickname()))
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        AylaPropertyTrigger trigger = _propertyTriggers.get(index);
                        String propNickName = trigger.getPropertyNickname();
                        AylaProperty property = _device.getProperty(propNickName);
                        property.deleteTrigger(trigger, new Response.Listener<AylaAPIRequest
                                        .EmptyResponse>() {
                                    @Override
                                    public void onResponse(AylaAPIRequest.EmptyResponse response) {
                                        AylaLog.d(LOG_TAG, "Successfully Deleted the old trigger");
                                        _propertyTriggers.remove(index);
                                        NotificationListFragment.this.updateTriggerList();
                                    }
                                },
                                new ErrorListener() {
                                    @Override
                                    public void onErrorResponse(AylaError error) {
                                        Toast.makeText(getContext(), error.toString(),
                                                Toast.LENGTH_LONG).show();
                                    }
                                });
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .create().show();
    }

    private static class TriggerAdapter extends RecyclerView.Adapter {
        private final WeakReference<NotificationListFragment> _frag;
        private final List<AylaPropertyTrigger>_propertyTriggers;
        private final AylaDevice _device;

        public TriggerAdapter(NotificationListFragment fragment, AylaDevice device,
                              List<AylaPropertyTrigger> propertyTriggers) {
            _frag = new WeakReference<>(fragment);
            _device = device;
            _propertyTriggers = propertyTriggers;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.trigger_card_view,
                    parent, false);
            return new TriggerViewHolder(v);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
            final AylaPropertyTrigger trigger = _propertyTriggers.get(position);
            TriggerViewHolder h = (TriggerViewHolder)holder;
            h._triggerName.setText(trigger.getDeviceNickname());
            h._propertyName.setText(trigger.getPropertyNickname());
            h.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MainActivity.sharedInstance().pushFragment(PropertyNotificationFragment
                            .newInstance(_device, trigger));
                }
            });
            h.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    _frag.get().onLongClick(position);
                    return true;
                }
            });
        }

        @Override
        public int getItemCount() {
            return _propertyTriggers.size();
        }
    }

    private static class TriggerViewHolder extends RecyclerView.ViewHolder {
        private final TextView _triggerName;
        private final TextView _propertyName;

        public TriggerViewHolder(View v) {
            super(v);
            _triggerName = (TextView)v.findViewById(R.id.trigger_name);
            _propertyName = (TextView)v.findViewById(R.id.trigger_property);
        }
    }
    private void fetchTriggers() {
        String[] propertyNames = null;
        AylaSystemSettings.DeviceDetailProvider provider =
                AylaNetworks.sharedInstance().getSystemSettings().deviceDetailProvider;

        if (provider != null) {
            propertyNames = provider.getManagedPropertyNames(_device);
        }
        if(propertyNames == null) {
            return;
        }
        for (String propName :propertyNames){
            AylaProperty aylaProperty= _device.getProperty(propName);
            if (aylaProperty == null) {
                AylaLog.e(LOG_TAG, "No property returned for " + propName);
                continue;
            }

            aylaProperty.fetchTriggers(new Response.Listener<AylaPropertyTrigger[]>() {
                @Override
                public void onResponse(AylaPropertyTrigger[] response) {
                    if(response !=null && response.length>0) {
                        _propertyTriggers.addAll(Arrays.asList(response));
                        updateTriggerList();
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
        AylaSessionManager sessionManager = AylaNetworks.sharedInstance()
                .getSessionManager(MainActivity.SESSION_NAME);
        sessionManager.fetchContacts(new Response.Listener<AylaContact[]>() {
                                         @Override
                                         public void onResponse(AylaContact[] response) {
                                             if (response != null && response.length > 0) {
                                                 ContactHelper.setContactList(Arrays.asList(response));
                                             }
                                         }
                                     },
                new ErrorListener() {
                    @Override
                    public void onErrorResponse(AylaError error) {
//                            Toast.makeText(_context, error.toString(), Toast.LENGTH_LONG)
//                                    .show();

                    }
                });
    }
}

