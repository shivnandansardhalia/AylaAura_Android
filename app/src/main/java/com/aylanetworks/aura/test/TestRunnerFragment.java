package com.aylanetworks.aura.test;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.aylanetworks.aura.MainActivity;
import com.aylanetworks.aura.R;
import com.aylanetworks.aylasdk.AylaDevice;
import com.aylanetworks.aylasdk.AylaDeviceManager;
import com.aylanetworks.aylasdk.change.Change;
import com.aylanetworks.aylasdk.error.AylaError;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link TestRunnerFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class TestRunnerFragment extends Fragment implements TestSuite.TestSuiteDelegate, AylaDevice.DeviceChangeListener {

    public final static String ARG_DEVICE_DSN = "DeviceDSN";

    private AylaDevice _device;
    private RecyclerView _messageView;
    private Button _startStopButton;
    private EditText _iterationsEditText;
    private EditText _maxErrorEditText;
    private Spinner _testSelector;

    private List<TestSuite> _testSuites;
    private TestSuite _runningSuite;
    private int _errorCount;
    private int _maxErrorCount;
    private int _totalRunCount;

    public TestRunnerFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param deviceToTest Device used to perform tests
     * @return A new instance of fragment TestRunnerFragment.
     */
    public static TestRunnerFragment newInstance(AylaDevice deviceToTest) {
        TestRunnerFragment fragment = new TestRunnerFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DEVICE_DSN, deviceToTest.getDsn());
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            String dsn = getArguments().getString(ARG_DEVICE_DSN);
            if (dsn != null) {
                AylaDeviceManager dm = MainActivity.getDeviceManager();
                if (dm != null) {
                    _device = dm.deviceWithDSN(dsn);
                    if (_device != null) {
                        _device.addListener(this);
                    }
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (_runningSuite != null) {
            _runningSuite.stopTests();
            _runningSuite = null;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_test_runner, container, false);

        _messageView = (RecyclerView) root.findViewById(R.id.messages_recycler);
        LinearLayoutManager llm = new LinearLayoutManager(getContext());
        llm.setReverseLayout(true);
        _messageView.setLayoutManager(llm);
        _messageView.setAdapter(new TestRunnerLogAdapter(null));

        _maxErrorEditText = (EditText)root.findViewById(R.id.stop_after_error_edit_text);

        _iterationsEditText = (EditText) root.findViewById(R.id.iterations_edit_text);

        Button clearButton = (Button) root.findViewById(R.id.clear_button);
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearButtonClicked();
            }
        });

        _startStopButton = (Button) root.findViewById(R.id.start_stop_button);
        _startStopButton.setText(R.string.start_button_text);
        _startStopButton.setEnabled(true);
        _startStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startButtonClicked();
            }
        });

        _testSelector = (Spinner) root.findViewById(R.id.test_selector);

        TextView deviceNameTextView = (TextView) root.findViewById(R.id.device_name_textview);
        if (_device != null) {
            deviceNameTextView.setText(_device.getFriendlyName());
            initTestSuites();
        }

        MainActivity.sharedInstance().setTitle(R.string.test_runner_title);
        return root;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        MainActivity.sharedInstance().setTitle(R.string.test_runner_title);
        MainActivity.sharedInstance().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        MainActivity.sharedInstance().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    // TestSuiteDelegate methods
    @Override
    public void logMessage(LogEntry.LogType logType, String message) {
        TestRunnerLogAdapter adapter = (TestRunnerLogAdapter) _messageView.getAdapter();
        adapter.addEntry(new LogEntry(new Date(), logType, message));
    }

    @Override
    public void testSuiteStartedCase(TestSuite testSuite, TestCase testCase) {
    }

    @Override
    public void testSuiteFinishedCase(TestSuite testSuite, TestCase testCase) {
        _totalRunCount++;
        if (testCase.getTestStatus() != TestCase.TestStatus.Passed) {
            _errorCount++;
            if (_maxErrorCount != 0 && _errorCount >= _maxErrorCount) {
                logMessage(LogEntry.LogType.Error, "Maximium error count reached at " + _errorCount);
                if (_runningSuite != null) {
                    _runningSuite.stopTests();
                }
            }
        }
    }

    @Override
    public void testSuiteComplete(TestSuite testSuite, TestCase.TestStatus status) {
        logMessage(LogEntry.LogType.Info, "Test suite complete: " + testSuite.getName());
        _startStopButton.setText(R.string.start_button_text);
        _startStopButton.setEnabled(true);
        _startStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startButtonClicked();
            }
        });
        _runningSuite = null;
    }

    private void initTestSuites() {
        _testSuites = new ArrayList<>();
        List<TestCase> allCases = new ArrayList<>();

        List<TestCase> testCases = new ArrayList<>();
        testCases.add(new LanDatapointTestCase());
        allCases.addAll(testCases);
        _testSuites.add(new TestSuite(_device, "LAN Mode Tests", testCases, this));

        testCases = new ArrayList<>();
        testCases.add(new CloudDatapointTestCase());
        allCases.addAll(testCases);
        _testSuites.add(new TestSuite(_device, "Cloud mode tests", testCases, this));

        testCases = new ArrayList<>();
        testCases.add(new NetworkProfileTestCase(getContext()));
        allCases.addAll(testCases);
        _testSuites.add(new TestSuite(_device, "Network profile tests", testCases, this));

        testCases = new ArrayList<>();
        testCases.add(new SetupAndRegistrationTestCase(getContext()));
        allCases.addAll(testCases);
        _testSuites.add(new TestSuite(_device, "Setup and registration tests", testCases, this));

        testCases = new ArrayList<>();
        testCases.add(new LanOTAUpdateTestCase(getContext()));
        allCases.addAll(testCases);
        _testSuites.add(new TestSuite(_device, "LAN OTA Tests", testCases, this));

        _testSuites.add(new TestSuite(_device, "All Tests", allCases, this));

        final List<String> testNames = new ArrayList<>(testCases.size());

        for (TestSuite suite : _testSuites) {
            testNames.add(suite.getName());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(MainActivity.sharedInstance(),
                android.R.layout.simple_spinner_item, testNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        _testSelector.setAdapter(adapter);
    }

    private void startButtonClicked() {
        // Find the selected test suite
        int index = _testSelector.getSelectedItemPosition();

        int iterations = Integer.parseInt(_iterationsEditText.getText().toString());

        // Cap iterations at 100 and don't let it run forever
        if (iterations <= 0 || iterations > 100) {
            iterations = 100;
            _iterationsEditText.setText(String.format(Locale.getDefault(), "%d", iterations));
        }

        _maxErrorCount = Integer.parseInt(_maxErrorEditText.getText().toString());
        _errorCount = 0;
        _totalRunCount = 0;

        _runningSuite = _testSuites.get(index);
        _runningSuite.setNumberOfRuns(iterations);

        final int runIterations = iterations;

        // Let TestSuite handle presenting the config views of each test (if any) to the user.
        // After the user is done with configuration and presses run, the callback will fire
        // and then we can begin the tests
        _runningSuite.configTestsAndRun(getContext(), new TestRunnerDelegate() {
            @Override
            public void runTests() {
                logMessage(LogEntry.LogType.Info, "Running suite " + _runningSuite.getName() +
                        " for " + runIterations + " iteration" +
                        (runIterations == 1 ? "" : "s"));

                _startStopButton.setText(R.string.stop_button_text);
                _startStopButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        stopButtonClicked();
                    }
                });

                _runningSuite.runTests();

            }
        });
    }

    private void stopButtonClicked() {
        if (_runningSuite != null) {
            logMessage(LogEntry.LogType.Info, "Stopping tests.");
            logMessage(LogEntry.LogType.Info, _errorCount + " errors out of " + _totalRunCount +
                    " test runs");
            _runningSuite.stopTests();
            _runningSuite = null;
            _startStopButton.setEnabled(false);
        }
    }

    private void clearButtonClicked() {
        TestRunnerLogAdapter adapter = (TestRunnerLogAdapter) _messageView.getAdapter();
        adapter.clear();
    }

    @Override
    public void deviceChanged(AylaDevice device, Change change) {

    }

    @Override
    public void deviceError(AylaDevice device, AylaError error) {
        logMessage(LogEntry.LogType.Error, "DeviceListener.deviceError (" +
                error.getClass().getSimpleName() + "): " + error.getMessage());
    }

    @Override
    public void deviceLanStateChanged(AylaDevice device, boolean lanModeEnabled) {

    }

    public class TestRunnerDelegate {
        public void runTests() {
        }
    }
}
