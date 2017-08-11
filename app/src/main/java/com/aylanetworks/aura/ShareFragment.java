package com.aylanetworks.aura;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.aylanetworks.aylasdk.AylaDevice;
import com.aylanetworks.aylasdk.AylaDeviceManager;
import com.aylanetworks.aylasdk.AylaLog;
import com.aylanetworks.aylasdk.AylaShare;
import com.aylanetworks.aylasdk.error.AylaError;
import com.aylanetworks.aylasdk.error.ErrorListener;

import org.w3c.dom.Text;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import static com.aylanetworks.aura.MainActivity.getDeviceManager;


/**
 * Fragment to display share UI
 */
public class ShareFragment extends Fragment {

    private static final String ARG_DSN = "dsn";
    private static final String LOG_TAG = "AURA_SHARES";
    private static final String OPERATION_WRITE = "write";
    private static final String OPERATION_READ = "read";

    private String _dsn;
    private AylaDevice _device;
    private TextView _txtviewDeviceName;
    private EditText _editTextEmail;
    private EditText _editTextRole;
    private TextView _textviewStartDate;
    private TextView _textviewEndDate;
    private String _startDate;
    private String _endDate;
    private TabLayout _sharePermissionTab;
    private String _operation;

    public ShareFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param device device to be shared.
     * @return A new instance of fragment ShareFragment.
     */
    public static ShareFragment newInstance(AylaDevice device) {
        ShareFragment fragment = new ShareFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DSN, device.getDsn());
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            _dsn = (String) getArguments().get(ARG_DSN);
            AylaDeviceManager deviceManager = getDeviceManager();
            if( deviceManager != null){
                _device = deviceManager.deviceWithDSN(_dsn);
            }
            _operation = OPERATION_WRITE;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_share_device, container, false);
        _txtviewDeviceName = (TextView) view.findViewById(R.id.textview_share_devicename);
        _editTextEmail = (EditText) view.findViewById(R.id.edittext_user_email);
        _editTextRole = (EditText) view.findViewById(R.id.edittext_role_name);
        _textviewStartDate = (TextView) view.findViewById(R.id.share_start_date);
        _textviewEndDate = (TextView) view.findViewById(R.id.share_end_date);
        _txtviewDeviceName.setText(String.format(getString(R.string.share_device_name),
                _device.getFriendlyName()));
        _sharePermissionTab = (TabLayout) view.findViewById(R.id.tablayout_share);

        _textviewStartDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               getDate(v.getId());
            }
        });

        _textviewEndDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               getDate(v.getId());
            }
        });

        Button btnShare = (Button) view.findViewById(R.id.btn_share_device);
        btnShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = _editTextEmail.getText().toString();
                if(email == null || email.isEmpty()){
                    return;
                }
                String role = _editTextRole.getText().toString();
                if(role.isEmpty()){
                    role = null;
                }
                if(role == null && _operation == null){
                    Toast.makeText(getContext(), getString(R.string.share_specify_role), Toast
                            .LENGTH_SHORT).show();
                } else{
                    shareDevice(email, role, _operation, _startDate, _endDate );
                }
                _startDate = null;
                _endDate = null;
            }
        });

        _sharePermissionTab.setSelectedTabIndicatorColor(getResources().getColor(
                R.color.ayla_green, null));
        _sharePermissionTab.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch(tab.getPosition()){
                    case 0:
                        _operation = OPERATION_WRITE;
                        break;
                    case 1:
                        _operation = OPERATION_READ;
                        break;
                    case 2:
                        _operation = null;

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

    private void shareDevice(final String email, final String role, final String operation,
                             String startDate, String endDate) {

        View view = getView();
        InputMethodManager inputMethodManager = (InputMethodManager) getContext().getSystemService(Context
                .INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromInputMethod(view.getWindowToken(), 0);
        if(_device == null){
            AylaLog.e(LOG_TAG, "shareDevice: device is null");
            return;
        }
        AylaShare share = _device.shareWithEmail(email, operation, role, startDate, endDate );
        MainActivity.getSession().createShare(share, null,
                new Response.Listener<AylaShare>(){
                    @Override
                    public void onResponse(AylaShare response) {
                        Activity activity = getActivity();
                        if(activity != null){
                            View view = activity.findViewById(android.R.id.content);
                            Snackbar.make(view, R.string.share_success, Snackbar
                                    .LENGTH_SHORT).show();
                        }
                    }
                },
                new ErrorListener() {
                    @Override
                    public void onErrorResponse(AylaError error) {
                        Activity activity = getActivity();
                        if(activity != null){
                            View view = activity.findViewById(android.R.id.content);
                            Snackbar.make(view, error.getLocalizedMessage(), Snackbar
                                    .LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void getDate(final int viewId){
        final SimpleDateFormat dateFormat = new SimpleDateFormat
                ("yyyy-MM-dd'T'HH:mm:ss'Z'");
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        Date startDate = calendar.getTime();
        if(_startDate != null && viewId == R.id.share_end_date){
            try {
                startDate = dateFormat.parse(_startDate) ;
            } catch (ParseException e) {
                AylaLog.d(LOG_TAG, "DateParseException while parsing start date");
            }
        }
        calendar.setTime(startDate);
        DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(), new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                Calendar cal = Calendar.getInstance();
                cal.set(year, monthOfYear, dayOfMonth);
                switch (viewId){
                    case R.id.share_start_date:
                        _startDate = dateFormat.format(cal.getTime());
                        _textviewStartDate.setText(_startDate);
                        break;
                    case R.id.share_end_date:
                        _endDate = dateFormat.format(cal.getTime());
                        _textviewEndDate.setText(_endDate);
                        break;
                }

            }
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }
}
