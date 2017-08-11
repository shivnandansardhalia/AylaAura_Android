package com.aylanetworks.aura.test;
/*
 * Aura_Android
 *
 * Copyright 2015 Ayla Networks, all rights reserved
 */

import android.view.View;

public abstract class TestCase {

    protected String _name;
    protected TestStatus _testStatus;

    public TestCase(String name) {
        _name = name;
        _testStatus = TestStatus.Waiting;
    }

    public String getName() {
        return _name;
    }

    /**
     * Override this method to provide a custom configuration view for the test.
     * NOTE: It is REQUIRED for each widget to have a listener which will receive and process
     * updates in real-time. (ex. OnTextChangedListener for EditText and OnCheckChangedListener for
     * CheckBox etc.). There are NO callbacks available to inform the test when the user is done
     * with configuration and when the test should save the configured values.
     * @return The configuration view of the test. Or null if none is required.
     */
    public View getConfigView() {
        return null;
    }

    public TestStatus getTestStatus() {
        return _testStatus;
    }

    public void pass() {
        _testStatus = TestStatus.Passed;
    }

    public void fail() {
        _testStatus = TestStatus.Failed;
    }

    public boolean isFinished() {
        return _testStatus != TestStatus.Waiting && _testStatus != TestStatus.Running;
    }

    public void run(TestSuite suite) {
        _testStatus = TestStatus.Running;
    }

    public void stop() {
        _testStatus = TestStatus.Stopped;
    }

    public enum TestStatus {
        Waiting,
        Running,
        Stopped,
        Passed,
        Failed,
    }
}
