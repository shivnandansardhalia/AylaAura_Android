package com.aylanetworks.aura;
/*
 * AylaSDK
 *
 * Copyright 2015 Ayla Networks, all rights reserved
 */

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.android.volley.Response;
import com.aylanetworks.aylasdk.AylaAPIRequest;
import com.aylanetworks.aylasdk.AylaDevice;
import com.aylanetworks.aylasdk.AylaSchedule;
import com.aylanetworks.aylasdk.AylaTimeZone;
import com.aylanetworks.aylasdk.error.AylaError;
import com.aylanetworks.aylasdk.error.ErrorListener;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

public class ScheduleFragment extends Fragment {
    private final static String LOG_TAG = "ScheduleFragment";

    private final static String ARG_DEVICE_DSN = "deviceDSN";
    private final static String ARG_SCHEDULE_NAME = "scheduleName";

    private AylaDevice _device;
    private String _scheduleName;
    private AylaSchedule _schedule;
    private TimeZone _tz;
    private EditText _scheduleTitleEditText;
    private Switch _scheduleEnabledSwitch;
    private RadioGroup _scheduleTypeRadioGroup;
    private LinearLayout _fullScheduleLayout;
    private RelativeLayout _scheduleDetailsLayout;
    private LinearLayout _timerScheduleLayout;
    private TimePicker _scheduleTimePicker;
    private TimePicker _timerTimePicker;
    private Button _saveScheduleButton;
    private TextView _timeZoneSchedule;

    // On / off time buttons for the repeating schedule
    private Button _scheduleOnTimeButton;
    private Button _scheduleOffTimeButton;

    // On off time buttons for the timer
    private Button _timerTurnOnButton;
    private Button _timerTurnOffButton;

    private int _timerOnDuration;
    private int _timerOffDuration;

    private boolean _updatingUI;
    private SimpleDateFormat _dateFormatYMD = null;

    private final DateFormat _dateFormatHMS;
    private static final int[] _weekdayButtonIDs = {
            R.id.button_sunday,
            R.id.button_monday,
            R.id.button_tuesday,
            R.id.button_wednesday,
            R.id.button_thursday,
            R.id.button_friday,
            R.id.button_saturday
    };

    public static ScheduleFragment newInstance(AylaDevice device, String scheduleName) {
        Bundle args = new Bundle();
        args.putString(ARG_DEVICE_DSN, device.getDsn());
        args.putString(ARG_SCHEDULE_NAME, scheduleName);
        ScheduleFragment frag = new ScheduleFragment();
        frag.setArguments(args);
        return frag;
    }

    public ScheduleFragment() {
        _timerOnDuration = 60;
        _timerOffDuration = 120;
        _dateFormatYMD = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        _dateFormatHMS = new SimpleDateFormat("HH:mm:ss", Locale.US);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View root = inflater.inflate(R.layout.fragment_schedule, container, false);
        // Get our device argument
        if (getArguments() != null) {
            String dsn = getArguments().getString(ARG_DEVICE_DSN);
            _device = MainActivity.getSession().getDeviceManager().deviceWithDSN(dsn);
            _scheduleName = getArguments().getString(ARG_SCHEDULE_NAME);
            _device.fetchSchedules(
                    new Response.Listener<AylaSchedule[]>() {
                        @Override
                        public void onResponse(AylaSchedule[] response) {
                            if(!isAdded()){
                                return;
                            }
                            if (response != null && response.length > 0) {
                                for (AylaSchedule schedule : response) {
                                    if (_scheduleName.equals(schedule.getName())) {
                                        _schedule = schedule;
                                        initialize(root);
                                        setFetchedTimers();
                                        break;
                                    }
                                }
                            }

                            if (_schedule == null) {
                                Toast.makeText(getActivity(),
                                        "Schedules are not available on this device",
                                        Toast.LENGTH_LONG).show();
                                getActivity().getSupportFragmentManager().popBackStack();
                            }
                        }
                    },
                    new ErrorListener() {
                        @Override
                        public void onErrorResponse(AylaError error) {
                            if(isAdded()){
                                Toast.makeText(MainActivity.sharedInstance(), error.toString(),
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                    });

        }

        // Get our views set up
        _scheduleTitleEditText = (EditText) root.findViewById(R.id.schedule_title_edittext);
        _scheduleEnabledSwitch = (Switch) root.findViewById(R.id.schedule_enabled_switch);
        _scheduleTypeRadioGroup = (RadioGroup) root.findViewById(R.id.schedule_type_radio_group);
        _fullScheduleLayout = (LinearLayout) root.findViewById(R.id.complex_schedule_layout);
        _timerScheduleLayout = (LinearLayout) root.findViewById(R.id.schedule_timer_layout);
        _scheduleDetailsLayout = (RelativeLayout) root.findViewById(R.id.schedule_details_layout);
        _scheduleTimePicker = (TimePicker) root.findViewById(R.id.time_on_off_picker);
        _timerTimePicker = (TimePicker) root.findViewById(R.id.timer_duration_picker);
        _scheduleOnTimeButton = (Button) root.findViewById(R.id.button_turn_on);
        _scheduleOffTimeButton = (Button) root.findViewById(R.id.button_turn_off);

        _timerTurnOnButton = (Button) root.findViewById(R.id.timer_turn_on_button);
        _timerTurnOffButton = (Button) root.findViewById(R.id.timer_turn_off_button);

        _saveScheduleButton = (Button) root.findViewById(R.id.save_schedule);
        _timeZoneSchedule = (TextView) root.findViewById(R.id.time_zone_for_schedule);

        return root;
    }

    private void initialize(View root) {
        // Control configuration / setup
        _scheduleEnabledSwitch.setChecked(_schedule.isActive());
        _scheduleEnabledSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                scheduleEnabledChanged(isChecked);
            }
        });
        scheduleEnabledChanged(_scheduleEnabledSwitch.isChecked());


        _scheduleTypeRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (_updatingUI) {
                    return;
                }
                setIsTimer(checkedId == R.id.radio_timer);
                updateUI();
            }
        });

        _scheduleTitleEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                _schedule.setName(s.toString());
            }
        });

        _scheduleTitleEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_NULL) {
                    InputMethodManager imm = (InputMethodManager) MainActivity.sharedInstance().
                            getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(_scheduleTitleEditText.getWindowToken(), 0);
                    return true;
                }
                return false;
            }
        });

        _scheduleOnTimeButton.setSelected(true);
        _scheduleOnTimeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                _scheduleOnTimeButton.setSelected(true);
                _scheduleOffTimeButton.setSelected(false);
                updateUI();
            }
        });

        _scheduleOffTimeButton.setSelected(false);
        _scheduleOffTimeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                _scheduleOnTimeButton.setSelected(false);
                _scheduleOffTimeButton.setSelected(true);
                updateUI();
            }
        });

        _scheduleTimePicker.setIs24HourView(false);
        _scheduleTimePicker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
            @Override
            public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
                scheduleTimeChanged(hourOfDay, minute);
            }
        });

        _timerTimePicker.setIs24HourView(true);
        _timerTimePicker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
            @Override
            public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
                timerTimeChanged(hourOfDay, minute);
            }
        });
        _timerTurnOnButton.setSelected(true);
        _timerTurnOnButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                _timerTurnOnButton.setSelected(true);
                _timerTurnOffButton.setSelected(false);
                updateUI();
            }
        });
        _timerTurnOffButton.setSelected(false);
        _timerTurnOffButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                _timerTurnOnButton.setSelected(false);
                _timerTurnOffButton.setSelected(true);
                updateUI();
            }
        });

        // Set up the buttons for weekdays
        int day = 1;
        for (int id : _weekdayButtonIDs) {
            final Button b = (Button) root.findViewById(id);
            b.setTag(day++);
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Set<Integer> daysSet = new HashSet<>(Arrays.asList((Integer[]) convertIntToInteger
                            (_schedule.getDaysOfWeek())));
                    Integer tag = (Integer) v.getTag();
                    if (daysSet.contains((tag))) {
                        b.setSelected(false);
                        daysSet.remove(v.getTag());
                        setDaysofweek(daysSet);
                    } else {
                        b.setSelected(true);
                        daysSet.add((Integer) v.getTag());
                        setDaysofweek(daysSet);
                    }
                }
            });
        }
        _saveScheduleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveSchedule();
            }
        });
    }

    private void setDaysofweek(Set<Integer> daysOfWeek) {
        int[] arrayWeek = new int[daysOfWeek.size()];
        Iterator<Integer> iter = daysOfWeek.iterator();
        int idx = 0;
        while (iter.hasNext()) {
            Integer integerVal = iter.next();
            arrayWeek[idx] = integerVal;
            idx++;
        }
        _schedule.setDaysOfWeek(arrayWeek);
    }

    private static Integer[] convertIntToInteger(int[] ids) {

        Integer[] newArray = new Integer[ids.length];
        for (int i = 0; i < ids.length; i++) {
            newArray[i] = ids[i];
        }
        return newArray;
    }

    private void getTimeZone() {
        final View view = getActivity().findViewById(R.id.schedule_title_edittext);
        final Snackbar sb = Snackbar.make(view, "Getting TimeZone...",
                Snackbar.LENGTH_INDEFINITE);
        final AylaAPIRequest request = _device.fetchTimeZone(
                new Response.Listener<AylaTimeZone>() {
                    @Override
                    public void onResponse(AylaTimeZone response) {
                        if (response.tzId != null) {
                            _tz = TimeZone.getTimeZone(response.tzId);
                        } else {
                            _tz = TimeZone.getTimeZone("UTC");
                        }
                        _timeZoneSchedule.setText(_tz.getDisplayName());
                        sb.dismiss();
                    }
                },
                new ErrorListener() {
                    @Override
                    public void onErrorResponse(AylaError error) {
                        sb.dismiss();
                        Toast.makeText(MainActivity.sharedInstance(), "Error while getting " +
                                "TimeZone:"+error.toString(), Toast.LENGTH_LONG).show();
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

    private void scheduleTimeChanged(int hourOfDay, int minute) {
        if (_updatingUI) {
            return;
        }
        //This is very rare case. The time zone is already set in onCreateView when fetchSchedules
        //method succeeds
        if(_tz == null) {
            Toast.makeText(MainActivity.sharedInstance(),"TimeZone is null for this Device."
                    , Toast.LENGTH_LONG).show();
            return;
        }

        Calendar cal = Calendar.getInstance();
        cal.setTimeZone(_tz);
        cal.set(Calendar.HOUR_OF_DAY, hourOfDay);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, 0);

        if (_scheduleOnTimeButton.isSelected()) {
            setStartTimeEachDay(cal);
        } else {
            setEndTimeEachDay(cal);
        }

    }

    private void timerTimeChanged(int hourOfDay, int minute) {
        if (_updatingUI) {
            return;
        }

        if (_timerTurnOnButton.isSelected()) {
            _timerOnDuration = hourOfDay * 60 + minute;
        } else {
            _timerOffDuration = hourOfDay * 60 + minute;
        }
    }

    private void scheduleEnabledChanged(boolean isChecked) {
        _schedule.setActive(isChecked);
        _scheduleDetailsLayout.setVisibility((isChecked ? View.VISIBLE : View.GONE));
        updateUI();
    }

    private void saveSchedule() {
        //This is very rare case. The time zone is already set in onCreateView when fetchSchedules
        //method succeeds
        if(_tz == null) {
            Toast.makeText(MainActivity.sharedInstance(),"TimeZone is null for this Device."
                    , Toast.LENGTH_LONG).show();
            return;
        }

        // Start date is always "right now".
        Calendar cal = Calendar.getInstance();
        cal.setTimeZone(_tz);
        _schedule.setStartDate(_dateFormatYMD.format(cal.getTime()));


        if (isTimer()) {
            //For Timers the time is in UTC
            _schedule.setUtc(true);
            // Set up the schedule
            setTimer(_timerOnDuration, _timerOffDuration);
        }
        else {

            //For Schedules the time is in device time zone. Only for Timers the time is in UTC
            _schedule.setUtc(false);
            _schedule.setEndDate(null);
        }

        // Save the updated schedule
        final View view = getActivity().findViewById(R.id.schedule_title_edittext);
        final Snackbar sb = Snackbar.make(view, "Saving Schedule...",
                Snackbar.LENGTH_INDEFINITE);
        final AylaAPIRequest request = _device.updateSchedule(_schedule,
                new Response.Listener<AylaSchedule>() {
                    @Override
                    public void onResponse(final AylaSchedule response) {
                        _schedule = response;
                        sb.dismiss();
                        new AlertDialog.Builder(getActivity())
                                .setMessage(R.string.save_schedule_msg)
                                .setTitle(android.R.string.dialog_alert_title)
                                .setCancelable(false)
                                .setPositiveButton(android.R.string.ok,
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog,
                                                                int id) {
                                                MainActivity.sharedInstance().pushFragment(
                                                        ScheduleActionFragment.newInstance
                                                                (_device, response));
                                            }
                                        })
                                .setNegativeButton(android.R.string.no,
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog,
                                                                int id) {
                                                MainActivity.sharedInstance().pushFragment(
                                                        DeviceDetailFragment.newInstance
                                                                (_device.getDsn()));
                                            }
                                        })
                                .create().show();
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

    private void updateUI() {
        // Make the UI reflect the schedule for this device
        if (_schedule != null) {
            _scheduleTitleEditText.setText(_schedule.getName());
        }

        if (_schedule == null || !_schedule.isActive()) {
            // Nothing shown except for the switch
            return;
        }

        _updatingUI = true;

        int checkedId = isTimer() ? R.id.radio_timer : R.id.radio_repeating;
        _scheduleTypeRadioGroup.check(checkedId);

        if (checkedId == R.id.radio_timer) {
            _timerScheduleLayout.setVisibility(View.VISIBLE);
            _fullScheduleLayout.setVisibility(View.GONE);
        } else {
            _timerScheduleLayout.setVisibility(View.GONE);
            _fullScheduleLayout.setVisibility(View.VISIBLE);
        }

        // Update the selected days of week
        for (int i = 0; i < 7; i++) {
            Button b = (Button) _fullScheduleLayout.findViewById(_weekdayButtonIDs[i]);
            if (_schedule.getDaysOfWeek() == null) {
                break;
            }
            Integer[] arrayDaysOfWeek = toObject(_schedule.getDaysOfWeek());
             Set<Integer> days = new HashSet<>(Arrays.asList(arrayDaysOfWeek));
            b.setSelected(days.contains(i + 1));
        }

        // Update the pickers
        Calendar now = Calendar.getInstance();
        now.setTimeZone(TimeZone.getTimeZone("UTC"));

        if (isTimer()) {
            int hour, minute;
            if (_timerTurnOnButton.isSelected()) {
                hour = (_timerOnDuration / 60);
                minute = (_timerOnDuration % 60);
            } else {
                hour = (_timerOffDuration / 60);
                minute = (_timerOffDuration % 60);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                _timerTimePicker.setHour(hour);
            } else {
                _timerTimePicker.setCurrentHour(hour);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                _timerTimePicker.setMinute(minute);
            } else {
                _timerTimePicker.setCurrentMinute(minute);
            }
        } else {
            // The schedule picker
            // Set to the on or off time if set, or the current time if not set
            if (_scheduleOnTimeButton.isSelected()) {
                setHourAndMinute(_schedule.getStartTimeEachDay(), _scheduleTimePicker);
            } else {
                setHourAndMinute(_schedule.getEndTimeEachDay(), _scheduleTimePicker);
            }
        }

        _updatingUI = false;
    }

    private boolean isTimer() {
        return _schedule.getEndDate() != null && !_schedule.getEndDate().isEmpty();

    }

    private void setIsTimer(boolean isTimer) {
        if (isTimer) {
            _schedule.setEndDate(_dateFormatYMD.format(today().getTime()));
        } else {
            _schedule.setEndDate(null);
        }
    }

    private Calendar today() {
        return Calendar.getInstance();
    }

    private void setTimer(int onMinutes, int offMinutes) {
        setIsTimer(true);
        _schedule.setEndTimeEachDay("");
        _schedule.setDaysOfWeek(new int[]{1, 2, 3, 4, 5, 6, 7});

        Calendar scheduleStartTime= Calendar.getInstance();
        scheduleStartTime.set(Calendar.HOUR_OF_DAY, 0);
        scheduleStartTime.set(Calendar.MINUTE, 0);
        scheduleStartTime.set(Calendar.SECOND, 0);

        Calendar scheduleEndTime = Calendar.getInstance();
        scheduleEndTime.set(Calendar.HOUR_OF_DAY, 0);
        scheduleEndTime.set(Calendar.MINUTE, 0);
        scheduleEndTime.set(Calendar.SECOND, 0);

        int duration = Math.abs(onMinutes - offMinutes) * 60;

        if (onMinutes > offMinutes) {
            // We turn off first. That will be the schedule start.
            scheduleStartTime.add(Calendar.MINUTE, offMinutes);
            scheduleEndTime.add(Calendar.MINUTE, onMinutes);
        } else {
            // We turn on first. That will be the schedule start.
            scheduleStartTime.add(Calendar.MINUTE, onMinutes);
            scheduleEndTime.add(Calendar.MINUTE, offMinutes);
        }
         SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss", Locale.US);
        _schedule.setStartDate(_dateFormatYMD.format(scheduleStartTime.getTime()));
        _schedule.setStartTimeEachDay(dateFormat.format(scheduleStartTime.getTime()));
        _schedule.setEndTimeEachDay(dateFormat.format(scheduleEndTime.getTime()));
        _schedule.setDuration(duration);

        scheduleStartTime.add(Calendar.SECOND, duration);
        _schedule.setEndDate(_dateFormatYMD.format(scheduleStartTime.getTime()));
    }

    private void setHourAndMinute(String timeStamp, TimePicker timePicker) {
        try {
            int index1 = timeStamp.indexOf(":");
            int hour, minute;
            if (index1 > 0) {
                hour = Integer.parseInt(timeStamp.substring(0, index1));
                int index2 = timeStamp.indexOf(":", index1 + 1);
                if (index2 > 0) {
                    String minuteString = timeStamp.substring(index1 + 1, index2);
                    minute = Integer.parseInt(minuteString);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        timePicker.setHour(hour);
                        timePicker.setMinute(minute);
                    } else {
                        timePicker.setCurrentHour(hour);
                        timePicker.setCurrentMinute(minute);
                    }
                }
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
            Toast.makeText(MainActivity.sharedInstance(),"Exception while setting hour and minute"
                    +e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    private void setTimerDuration(String timeStamp, boolean isTimerOn) {
        try {
            int index1 = timeStamp.indexOf(":");
            int hour, minute;
            if (index1 > 0) {
                hour = Integer.parseInt(timeStamp.substring(0, index1));
                int index2 = timeStamp.indexOf(":", index1 + 1);
                if (index2 > 0) {
                    String minuteString = timeStamp.substring(index1 + 1, index2);
                    minute = Integer.parseInt(minuteString);
                    if (isTimerOn) {
                        _timerOnDuration = hour * 60 + minute;
                    } else {
                        _timerOffDuration = hour * 60 + minute;
                    }
                }
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
            Toast.makeText(MainActivity.sharedInstance(), "Exception while setting Timer Duration"
                            +e.toString(),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void setFetchedTimers() {
        getTimeZone();
        String strStartTime = _schedule.getStartTimeEachDay();
        String strEndTime = _schedule.getEndTimeEachDay();
        setTimerDuration(strStartTime, true);
        setTimerDuration(strEndTime, false);
        updateUI();
    }

    private Integer[] toObject(int[] intArray) {

        Integer[] result = new Integer[intArray.length];
        for (int i = 0; i < intArray.length; i++) {
            result[i] = intArray[i];
        }
        return result;

    }

    private void setStartTimeEachDay(Calendar startTime) {
        setIsTimer(false);
        if (startTime == null) {
            _schedule.setStartTimeEachDay("");
        } else {
            try {
                Date date = startTime.getTime();
                _dateFormatHMS.setTimeZone(startTime.getTimeZone());
                _schedule.setStartTimeEachDay(_dateFormatHMS.format(date));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void setEndTimeEachDay(Calendar endTime) {
        setIsTimer(false);
        if (endTime == null) {
            _schedule.setEndTimeEachDay("");
        } else {
            _dateFormatHMS.setTimeZone(endTime.getTimeZone());
            _schedule.setEndTimeEachDay(_dateFormatHMS.format(endTime.getTime()));
        }
    }

}
