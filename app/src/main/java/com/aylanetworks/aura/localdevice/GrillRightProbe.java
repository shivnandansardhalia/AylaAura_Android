package com.aylanetworks.aura.localdevice;

import android.app.Notification;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.RingtoneManager;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.NotificationCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.aylanetworks.aura.R;
import com.aylanetworks.aylasdk.AylaDevice;
import com.aylanetworks.aylasdk.AylaLog;
import com.aylanetworks.aylasdk.AylaProperty;
import com.aylanetworks.aylasdk.change.Change;
import com.aylanetworks.aylasdk.error.AylaError;
import com.aylanetworks.aylasdk.error.ErrorListener;

import java.util.Locale;

import static com.aylanetworks.aura.localdevice.GrillRightDevice.ControlMode;
import static com.aylanetworks.aura.localdevice.GrillRightDevice.*;

/**
 * View class for the display of probe information for one GrillRight probe
 */

public class GrillRightProbe extends RelativeLayout implements AylaDevice.DeviceChangeListener {
    private static final String LOG_TAG = "GrillRightProbe";
    private GrillRightDevice _device;
    private int _probeNumber;
    private ControlMode _currentControlMode = ControlMode.None;
    private int _sensor;
    private APIListener _apiListener;

    private TextView _currentTemp;
    private Button _startStopButton;

    private TextView _cookingModeText;

    // Timer mode controls
    private TextView _timerResetTime;
    private TextView _timerCurrentTime;

    private ViewGroup _controlContainer;
    private ViewGroup _timerViewGroup;
    private ViewGroup _meatViewGroup;
    private ViewGroup _tempViewGroup;
    private ViewGroup _modeBarViewGroup;

    private ImageButton _timerButton;
    private ImageButton _meatButton;
    private ImageButton _tempButton;

    private Handler _cookTimerHandler = new Handler(Looper.getMainLooper());
    private AlarmState _alarmTriggered = AlarmState.None;

    public GrillRightProbe(Context context, AttributeSet attrs) {
        super(context, attrs);

        _apiListener = new APIListener();

        View.inflate(context, R.layout.grill_right_probe, this);

        // Digital font for our display
        Typeface tf = Typeface.createFromAsset(context.getAssets(), "fonts/digital_counter_7.ttf");
        Typeface tf2 = Typeface.createFromAsset(context.getAssets(), "fonts/LEDSimulator.ttf");

        // Cook mode buttons

        _meatButton = (ImageButton)findViewById(R.id.mode_meat);
        _meatButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setCookMode(ControlMode.Meat);
            }
        });
        _meatButton.setColorFilter(ContextCompat.getColor(context, R.color.gr_button_normal));

        _tempButton = (ImageButton)findViewById(R.id.mode_temp);
        _tempButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setCookMode(ControlMode.Temp);
            }
        });
        _tempButton.setColorFilter(ContextCompat.getColor(context, R.color.gr_button_normal));

        _timerButton = (ImageButton)findViewById(R.id.mode_timer);
        _timerButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setCookMode(ControlMode.Time);
            }
        });
        _timerButton.setColorFilter(ContextCompat.getColor(context, R.color.gr_button_normal));


        // Start / stop button
        _startStopButton = (Button)findViewById(R.id.start_stop);
        _startStopButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startStopClicked();
            }
        });

        _cookingModeText = (TextView)findViewById(R.id.cooking_mode_text);
        _cookingModeText.setTypeface(tf2);
        _cookingModeText.setTextSize(32f);

        // Current temperature display
        _currentTemp = (TextView)findViewById(R.id.probe_temp);
        _currentTemp.setTypeface(tf);
        _currentTemp.setTextSize(64f);

        // View groups
        _controlContainer = (ViewGroup)findViewById(R.id.control_container);
        _timerViewGroup = (ViewGroup)findViewById(R.id.control_layout_timer);
        _meatViewGroup = (ViewGroup)findViewById(R.id.control_layout_meat);
        _tempViewGroup = (ViewGroup)findViewById(R.id.control_layout_temp);
        _modeBarViewGroup = (ViewGroup)findViewById(R.id.mode_bar);


        // Timer controls
        _timerResetTime = (TextView)findViewById(R.id.timer_reset_time);
        _timerCurrentTime = (TextView)findViewById(R.id.timer_current_time);
        _timerResetTime.setTypeface(tf);
        _timerResetTime.setTextSize(32f);
        _timerResetTime.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setTimerClicked();
            }
        });

        _timerCurrentTime.setTypeface(tf);
        _timerCurrentTime.setTextSize(56f);

        updateUI();
    }

    private void setTimerClicked() {
        if (isCooking()) {
            Toast.makeText(getContext(), R.string.stop_before_modifying, Toast.LENGTH_LONG).show();
            return;
        }

        final String propName = _probeNumber == 1 ? PROP_SENSOR1_TARGET_TIME :
                PROP_SENSOR2_TARGET_TIME;
        String currentTime = (String)_device.getProperty(propName).getValue();
        final EditText editText = new EditText(getContext());
        editText.setText(currentTime);
        new AlertDialog.Builder(getContext())
                .setTitle(R.string.choose_time)
                .setView(editText)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String newTime = editText.getText().toString();
                        // Check the format
                        String[] parts = newTime.split(":");
                        try {
                            if (parts.length != 3 ||
                                    Integer.parseInt(parts[0]) > 9 ||
                                    Integer.parseInt(parts[1]) > 59 ||
                                    Integer.parseInt(parts[2]) > 59) {
                                Toast.makeText(getContext(), R.string.time_format_text, Toast
                                        .LENGTH_LONG).show();
                                return;
                            }
                        } catch (NumberFormatException e) {
                            Toast.makeText(getContext(), R.string.time_format_text, Toast
                                    .LENGTH_LONG).show();
                            return;
                        }

                        _device.getProperty(propName).createDatapoint(newTime, null, _apiListener,
                                _apiListener);
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create().show();
    }

    private void startStopClicked() {
        String propName = _probeNumber == 1 ? PROP_SENSOR1_COOKING : PROP_SENSOR2_COOKING;
        AylaProperty<Integer> cookingProp = _device.getProperty(propName);
        int isCooking = cookingProp.getValue();
        if (isCooking == 0) {
            // Start cooking with the selected mode
            _alarmTriggered = AlarmState.None;
            cookingProp.createDatapoint(_currentControlMode.getIndex(), null,
                    _apiListener, _apiListener);
            if (_currentControlMode == ControlMode.Time) {
                startTimer();
            }
        } else {
            // Stop cooking
            cookingProp.createDatapoint(0, null, _apiListener, _apiListener);
            stopTimer();
        }

        // Disable until we get an update
        _startStopButton.setEnabled(false);
    }

    private void startTimer() {
        // Set our timer
        stopTimer();
        _cookTimerHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                timerTick();
                _cookTimerHandler.postDelayed(this, 1000);
            }
        }, 1000);
    }

    private void stopTimer() {
        _cookTimerHandler.removeCallbacksAndMessages(null);
    }

    private void timerTick() {
        // Count down our timer
        String timerText = _timerCurrentTime.getText().toString();
        String[] components = timerText.split(":");
        int s = Integer.parseInt(components[2]);
        int m = Integer.parseInt(components[1]);
        int h = Integer.parseInt(components[0]);

        s--;
        if (s < 0) {
            s = 0;
            m--;
        }

        if (m < 0) {
            m = 0;
            h--;
        }

        if (h < 0) {
            h = 0;
        }

        String newText = String.format(Locale.US, "%d:%02d:%02d", h, m, s);
        _timerCurrentTime.setText(newText);
    }

    public void setDevice(GrillRightDevice device, int probeNumber) {
        if (_device != null) {
            _device.removeListener(this);
        }

        _device = device;
        _probeNumber = probeNumber;
        if (_device != null) {
            _device.addListener(this);
        }
        updateUI();
    }

    private void setCookMode(GrillRightDevice.ControlMode controlMode) {
        AylaLog.d(LOG_TAG, "Control mode: " + controlMode);
        _currentControlMode = controlMode;
        updateUI();
    }

    private void updateUI() {
        if (_device == null) {
            return;
        }

        String propName = _probeNumber == 1 ? PROP_SENSOR1_TEMP :
                PROP_SENSOR2_TEMP;
        String tempText = "---";
        // Update current temp
        int temp = (int) _device.getProperty(propName).getValue();
        float currentTemp = temp / 10f;
        if (temp != GrillRightDevice.NO_SENSOR) {
            tempText = String.format(Locale.US, "%.1f F", currentTemp);
        }
        _currentTemp.setText(tempText);

        // Set the color of the temperature display based on the relative temperature
        float hotness = Math.max(((Math.min(currentTemp, 470f) - 70f) / 400f), 0f);

        AylaLog.d(LOG_TAG, "Hotness: " + hotness);
        int color = Color.rgb((int)(240 * hotness), 240 - (int)(240 * hotness), 0);
        _currentTemp.setTextColor(color);

        propName = _probeNumber == 1 ? PROP_SENSOR1_CONTROL_MODE : PROP_SENSOR2_CONTROL_MODE;
        ControlMode deviceMode = ControlMode.fromIndex((int) _device.getProperty(propName).getValue());
        boolean updateControlMode = (_currentControlMode == ControlMode.None) ||
                (isCooking() && deviceMode != _currentControlMode);

        // Switch the UI based on the current control mode, if we're cooking.
        if (updateControlMode) {
            _currentControlMode = deviceMode;
        }

        AylaLog.d(LOG_TAG, "Current control mode[" + _probeNumber + "]: " + _currentControlMode);


        // Hide everything
        ViewGroup activeViewGroup = null;
        switch(_currentControlMode){
            case Meat:
                activeViewGroup = _meatViewGroup;
                _meatViewGroup.setVisibility(View.VISIBLE);
                _timerViewGroup.setVisibility(View.GONE);
                _tempViewGroup.setVisibility(View.GONE);
                _cookingModeText.setText(R.string.cooking_mode_meat);
                _meatButton.setColorFilter(ContextCompat.getColor(getContext(),
                        R.color.gr_button_selected));
                _tempButton.setColorFilter(ContextCompat.getColor(getContext(),
                        R.color.gr_button_normal));
                _timerButton.setColorFilter(ContextCompat.getColor(getContext(),
                        R.color.gr_button_normal));
                break;

            case Temp:
                activeViewGroup = _tempViewGroup;
                _tempViewGroup.setVisibility(View.VISIBLE);
                _timerViewGroup.setVisibility(View.GONE);
                _meatViewGroup.setVisibility(View.GONE);
                _cookingModeText.setText(R.string.cooking_mode_temp);
                _meatButton.setColorFilter(ContextCompat.getColor(getContext(),
                        R.color.gr_button_normal));
                _tempButton.setColorFilter(ContextCompat.getColor(getContext(),
                        R.color.gr_button_selected));
                _timerButton.setColorFilter(ContextCompat.getColor(getContext(),
                        R.color.gr_button_normal));
                break;

            case Time:
                activeViewGroup = _timerViewGroup;
                _timerViewGroup.setVisibility(View.VISIBLE);
                _meatViewGroup.setVisibility(View.GONE);
                _tempViewGroup.setVisibility(View.GONE);
                _cookingModeText.setText(R.string.cooking_mode_time);
                _meatButton.setColorFilter(ContextCompat.getColor(getContext(),
                        R.color.gr_button_normal));
                _tempButton.setColorFilter(ContextCompat.getColor(getContext(),
                        R.color.gr_button_normal));
                _timerButton.setColorFilter(ContextCompat.getColor(getContext(),
                        R.color.gr_button_selected));
                if (isCooking()) {
                    startTimer();
                } else {
                    stopTimer();
                }
                break;
        }

        // Timer values
        propName = _probeNumber == 1 ? PROP_SENSOR1_TIME : PROP_SENSOR2_TIME;
        String currentTime = (String)_device.getProperty(propName).getValue();
        _timerCurrentTime.setText(currentTime);

        propName = _probeNumber == 1 ? PROP_SENSOR1_TARGET_TIME : PROP_SENSOR2_TARGET_TIME;
        currentTime = (String)_device.getProperty(propName).getValue();
        _timerResetTime.setText(currentTime);

        // Are we cooking?
        boolean isCooking = isCooking();
        if (isCooking) {
            _startStopButton.setText(R.string.stop_button_text);
            setBackgroundColor(ContextCompat.getColor(getContext(), R.color.gr_active_bg));
        } else {
            _startStopButton.setText(R.string.start_button_text);
            setBackgroundColor(ContextCompat.getColor(getContext(), R.color.gr_bg));
        }

        // Alarm state?
        propName = _probeNumber == 1 ? PROP_SENSOR1_ALARM : PROP_SENSOR2_ALARM;
        Object aStateObj = _device.getProperty(propName).getValue();
        if (aStateObj != null) {
            int alarmState = (int)aStateObj;
            if (alarmState != 0) {
                if (_alarmTriggered == AlarmState.None) {
                    _alarmTriggered = AlarmState.fromIndex(alarmState);
                    showAlarmNotification();
                }
                setBackgroundColor(ContextCompat.getColor(getContext(), R.color.gr_bg_alarm));
            }
        }

        enableInteraction(_modeBarViewGroup, !isCooking);
        if (activeViewGroup != null) {
            enableInteraction(activeViewGroup, !isCooking);
        }

        // Always keep the start / stop button enabled
        _startStopButton.setEnabled(true);
    }

    private boolean isCooking() {
        String propName = _probeNumber == 1 ? PROP_SENSOR1_COOKING : PROP_SENSOR2_COOKING;
        Object cookStateObj = _device.getProperty(propName).getValue();
        if (cookStateObj != null) {
            return ((int)cookStateObj != 0);
        } else {
            return false;
        }
    }
    /**
     * Helper method to enable or disable a view and its subviews
     * @param view Root view to enable or disable
     * @param enable true to enable, false to disable
     */
    private void enableInteraction(View view, boolean enable) {
        view.setEnabled(enable);
        if (view instanceof ViewGroup) {
            for (int i=0; i < ((ViewGroup) view).getChildCount(); i++) {
                View child = ((ViewGroup) view).getChildAt(i);
                enableInteraction(child, enable);
            }
        }
    }

    private void showAlarmNotification() {
        String alertTitle;
        String alertMessage;
        Context context = getContext();
        if (_alarmTriggered == AlarmState.AlmostDone) {
            alertTitle = context.getString(R.string.almost_done_title, _sensor);
            alertMessage = context.getString(R.string.almost_done_message, _sensor);
        } else {
            alertTitle = context.getString(R.string.overdone_title, _sensor);
            alertMessage = context.getString(R.string.overdone_message, _sensor);
        }
        new NotificationCompat.Builder(getContext())
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(alertTitle)
                .setCategory(Notification.CATEGORY_ALARM)
                .setContentTitle(alertMessage)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM));
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (_device != null) {
            _device.removeListener(this);
            stopTimer();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (_device != null) {
            _device.addListener(this);
        }
    }

    @Override
    public void deviceChanged(AylaDevice device, Change change) {
        updateUI();
    }

    @Override
    public void deviceError(AylaDevice device, AylaError error) {
        AylaLog.e(LOG_TAG, "Device error: " + error);
    }

    @Override
    public void deviceLanStateChanged(AylaDevice device, boolean lanModeEnabled) {

    }

    private class APIListener implements Response.Listener, ErrorListener {

        @Override
        public void onResponse(Object response) {
            updateUI();
        }

        @Override
        public void onErrorResponse(AylaError error) {
            Snackbar.make(getRootView(), error.getMessage(), Snackbar.LENGTH_LONG).show();
        }
    }
}
