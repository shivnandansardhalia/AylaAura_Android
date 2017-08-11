package com.aylanetworks.aura.test;
/*
 * Aura_Android
 *
 * Copyright 2015 Ayla Networks, all rights reserved
 */

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.aylanetworks.aylasdk.AylaDevice;

import java.util.List;

public class TestSuite {
    private String _suiteName;
    private List<TestCase> _testCases;
    private TestSuiteDelegate _delegate;
    private int _remainingRuns;                 // -1 means run forever
    private int _initialRuns = 1;               // 0 means run forever
    private int _currentIteration = 0;
    private AylaDevice _deviceUnderTest;

    private boolean _running;

    public TestSuite(AylaDevice testDevice, String suiteName, List<TestCase> testCases,
                     TestSuiteDelegate delegate) {
        _deviceUnderTest = testDevice;
        _suiteName = suiteName;
        _testCases = testCases;
        _delegate = delegate;
    }

    public List<TestCase> getTestCases() {
        return _testCases;
    }

    /**
     * Sets the number of times this suite should be run. Set to 0 to run indefinitely
     * @param numberOfRuns the number of times the test should be run, or 0 to run indefinitely
     */
    public void setNumberOfRuns(int numberOfRuns) {
        _initialRuns = numberOfRuns;
    }

    public String getName() {
        return _suiteName;
    }

    public AylaDevice getDeviceUnderTest() {
        return _deviceUnderTest;
    }

    public void runTests() {
        if (!_running) {
            _currentIteration = 1;
            _running = true;
            _remainingRuns = _initialRuns;
            if (_remainingRuns == 0) {
                _remainingRuns = -1;
            }
            new Thread(new TestRunner()).start();
        }
    }

    public void stopTests() {
        _running = false;
    }

    public boolean isRunning() {
        return _running;
    }

    public void logMessage(LogEntry.LogType logType, String message) {
        _delegate.logMessage(logType, message);
    }

    public interface TestSuiteDelegate {
        void logMessage(LogEntry.LogType logType, String message);
        void testSuiteStartedCase(TestSuite testSuite, TestCase testCase);
        void testSuiteFinishedCase(TestSuite testSuite, TestCase testCase);
        void testSuiteComplete(TestSuite testSuite, TestCase.TestStatus status);
    }

    private void testRunnerFinished() {
        _running = false;

        logMessage(LogEntry.LogType.Info, "Test suite " + getName() + " completed.");

        // See how we did
        int nPass = 0;
        int nFail = 0;
        for (TestCase testCase : _testCases) {
            if (testCase.getTestStatus() != TestCase.TestStatus.Passed) {
                nFail++;
                logMessage(LogEntry.LogType.Fail, testCase.getName() + " failed");
            } else {
                nPass++;
                logMessage(LogEntry.LogType.Pass, testCase.getName() + " passed");
            }
        }

        logMessage(LogEntry.LogType.Info, getName() + " results: " + nPass + " pass, " +
                nFail + " fail");

        final TestCase.TestStatus suiteStatus = nFail == 0 ?
                        TestCase.TestStatus.Passed : TestCase.TestStatus.Failed;
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                _delegate.testSuiteComplete(TestSuite.this, suiteStatus);
            }
        });
    }

    /**
     * The method collects the configuration view (if any) from each of the tests and presents them
     * to the user in a list via a dialog. After the user confirms the configurations, the callback
     * will allow TestRunnerFragment to begin the tests
     * @param context Activity context for displaying the dialog
     * @param delegate Delegate containing the callback to invoke after configuration
     */
    public void configTestsAndRun(Context context, final TestRunnerFragment.TestRunnerDelegate delegate) {
        ScrollView root = new ScrollView(context);
        LinearLayout ll = new LinearLayout(context);
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.setGravity(Gravity.CENTER);
        root.addView(ll);

        for (TestCase test : getTestCases()) {
            View configView = test.getConfigView();
            if (configView != null) {
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                params.setMargins(15, 15, 15, 15);

                TextView testName = new TextView(context);
                testName.setText(test.getName());
                testName.setGravity(Gravity.CENTER);
                testName.setLayoutParams(params);
                ll.addView(testName);

                configView.setLayoutParams(params);
                ll.addView(configView);
            }
        }

        if (ll.getChildCount() == 0) {
            delegate.runTests();
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage("Test Configurations");
            builder.setView(root);
            builder.setPositiveButton("Run", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    delegate.runTests();
                }
            });
            builder.setNegativeButton("Cancel", null);
            builder.show();
        }
    }

    public int getRemainingRuns() {
        return _remainingRuns;
    }

    public int getCurrentIteration() {
        return _currentIteration;
    }

    class TestRunner implements Runnable {
        @Override
        public void run() {
            while (_running && (_remainingRuns == -1 || _remainingRuns > 0)) {
                for (TestCase testCase : _testCases) {
                    if (!_running) break;
                    _delegate.logMessage(LogEntry.LogType.Info, "Running test case " +
                            testCase.getName() + " run #" + _currentIteration);
                    _delegate.testSuiteStartedCase(TestSuite.this, testCase);

                    testCase.run(TestSuite.this);
                    _delegate.testSuiteFinishedCase(TestSuite.this, testCase);
                }

                _currentIteration++;
                if (_remainingRuns != -1) {
                    _remainingRuns--;
                }
            }

            testRunnerFinished();
        }
    }
}
