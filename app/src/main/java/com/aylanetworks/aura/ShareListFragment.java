package com.aylanetworks.aura;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import com.android.volley.Response;
import com.aylanetworks.aylasdk.AylaAPIRequest;
import com.aylanetworks.aylasdk.AylaLog;
import com.aylanetworks.aylasdk.AylaShare;
import com.aylanetworks.aylasdk.error.AylaError;
import com.aylanetworks.aylasdk.error.ErrorListener;


/**
 * Fragment to display owned and received shares of the user.
 */
public class ShareListFragment extends Fragment {

    private static final String LOG_TAG = "SHARE_LIST_FRAGMENT";

    private AylaShare[] _ownedShares;
    private AylaShare[] _receivedShares;
    private ShareListAdapter _ownedShareAdapter;
    private ShareListAdapter _receivedShareAdapter;


    public ShareListFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment ShareListFragment.
     */
    public static ShareListFragment newInstance() {
        ShareListFragment fragment = new ShareListFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        _ownedShareAdapter = new ShareListAdapter(getContext(), R.layout.share_list_item,
                R.id.txtview_share_item);
        _receivedShareAdapter = new ShareListAdapter(getContext(), R.layout.share_list_item,
                R.id.txtview_share_item);
        fetchShares();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_share_list, container, false);
        ListView ownedShareList = (ListView) view.findViewById(R.id.listview_owned_shares);
        ListView receivedShareList = (ListView) view.findViewById(R.id.listview_received_shares);
        ownedShareList.setAdapter(_ownedShareAdapter);
        receivedShareList.setAdapter(_receivedShareAdapter);

        ownedShareList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                AylaShare selectedShare = _ownedShares[position];
                displayShareDetails(selectedShare);
            }
        });

        receivedShareList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                AylaShare selectedShare = _receivedShares[position];
               displayShareDetails(selectedShare);
            }
        });

        return view;
    }

    private void fetchShares(){
        MainActivity.getSession().fetchOwnedShares(new Response.Listener<AylaShare[]>() {
            @Override
            public void onResponse(AylaShare[] response) {
                _ownedShares = response;
                _ownedShareAdapter.clear();
                _ownedShareAdapter.addAll(getNames(_ownedShares));
            }
        }, new ErrorListener() {
            @Override
            public void onErrorResponse(AylaError error) {
                AylaLog.e(LOG_TAG, "Error in fetching shares "+ error.getLocalizedMessage());
            }
        });

        MainActivity.getSession().fetchReceivedShares(new Response.Listener<AylaShare[]>() {
            @Override
            public void onResponse(AylaShare[] response) {
                _receivedShares = response;
                _receivedShareAdapter.clear();
                _receivedShareAdapter.addAll(getNames(_receivedShares));
            }
        }, new ErrorListener() {
            @Override
            public void onErrorResponse(AylaError error) {
                AylaLog.e(LOG_TAG, "Error in fetching shares "+ error.getLocalizedMessage());
            }
        });

        _ownedShareAdapter.notifyDataSetChanged();
        _receivedShareAdapter.notifyDataSetChanged();
    }

    private String[] getNames(AylaShare[] shares){
        String[] sharedDevices = new String[shares.length];
        for(int i=0; i<shares.length; i++){
            sharedDevices[i] = shares[i].getResourceId();
        }
        return sharedDevices;
    }

    private void displayShareDetails(final AylaShare selectedShare){
        final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(selectedShare.getResourceId());
        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context
                .LAYOUT_INFLATER_SERVICE);
        final View shareDescriptionView = inflater.inflate(R.layout.share_description, null);
        TextView txtResourceType = (TextView) shareDescriptionView.findViewById(R.id.share_type);
        txtResourceType.setText(selectedShare.getResourceName());

        TextView txtEmail = (TextView) shareDescriptionView.findViewById(R.id.share_email);
        txtEmail.setText(selectedShare.getUserEmail());

        TextView txtAccess = (TextView) shareDescriptionView.findViewById(R.id.share_access);
        txtAccess.setText(selectedShare.getOperation());

        TextView txtRole = (TextView) shareDescriptionView.findViewById(R.id.share_role);
        txtRole.setText(selectedShare.getRoleName());

        TextView txtSartDate = (TextView) shareDescriptionView.findViewById(R.id.share_start_date);
        txtSartDate.setText(selectedShare.getStartDateAt());

        TextView txtEndDate = (TextView) shareDescriptionView.findViewById(R.id.share_end_date);
        txtEndDate.setText(selectedShare.getEndDateAt());

        builder.setPositiveButton(R.string.delete_share, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                MainActivity.getSession().deleteShare(selectedShare.getId(), new Response.Listener<AylaAPIRequest.EmptyResponse>() {
                    @Override
                    public void onResponse(AylaAPIRequest.EmptyResponse response) {
                        Activity activity = getActivity();
                        if(activity != null){
                            View view = activity.findViewById(android.R.id.content);
                            Snackbar.make(view, "Deleted share", Snackbar.LENGTH_SHORT).show();
                            fetchShares();
                        }
                    }
                }, new ErrorListener() {
                    @Override
                    public void onErrorResponse(AylaError error) {
                        Activity activity = getActivity();
                        if(activity != null){
                            View view = activity.findViewById(android.R.id.content);
                            Snackbar.make(view, "Delete failed", Snackbar.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
        builder.setView(shareDescriptionView);
        builder.show();
    }
}
