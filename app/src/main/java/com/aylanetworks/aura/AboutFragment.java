package com.aylanetworks.aura;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.TextViewCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.aylanetworks.aura.util.AuraConfig;
import com.aylanetworks.aylasdk.AylaNetworks;
import com.aylanetworks.aylasdk.util.SystemInfoUtils;


public class AboutFragment extends Fragment {
    public AboutFragment() {
        // Required empty public constructor
    }

    /**
     * Create a new instance of this fragment
     * @return A new instance of fragment AboutFragment.
     */
    public static AboutFragment newInstance() {
        AboutFragment fragment = new AboutFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_about, container, false);
        TextView txtView = (TextView) view.findViewById(R.id.about_text);
        StringBuilder strBuilder = new StringBuilder(200);
        strBuilder.append("\n\nOS Version: " + SystemInfoUtils.getOSVersion());
        strBuilder.append("\nSDK Version: " + SystemInfoUtils.getSDKVersion());
        strBuilder.append("\nCountry: " + SystemInfoUtils.getCountry());
        strBuilder.append("\nLanguage: " + SystemInfoUtils.getLanguage());
        strBuilder.append("\nNetwork Operator: " + SystemInfoUtils.getNetworkOperator());
        strBuilder.append("\nAyla SDK version: " + AylaNetworks.getVersion());
        strBuilder.append("\nAura app version: " + getAppVersion());
        strBuilder.append("\nAyla Service: "+ AuraConfig.getSelectedConfiguration().toString());
        txtView.setText(strBuilder.toString());

        getActivity().setTitle(getString(R.string.about));
        return view;
    }

    public String getAppVersion() {
        try {
            PackageInfo pi = getActivity().getPackageManager().getPackageInfo(
                    getActivity().getPackageName(), 0);
            return pi.versionName;

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return "Error in getting app version "+e.getLocalizedMessage();
        }

    }
}
