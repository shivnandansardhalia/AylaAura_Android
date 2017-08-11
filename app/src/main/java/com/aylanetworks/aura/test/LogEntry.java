package com.aylanetworks.aura.test;
/*
 * Aura_Android
 *
 * Copyright 2015 Ayla Networks, all rights reserved
 */

import java.util.Date;

public class LogEntry {
    private String _message;
    private Date _timestamp;
    private LogType _logType;

    public LogEntry(Date timestamp, LogType logType, String message) {
        _timestamp = timestamp;
        _logType = logType;
        _message = message;
    }

    public String getMessage() {
        return _message;
    }

    public Date getTimestamp() {
        return _timestamp;
    }

    public LogType getLogType() {
        return _logType;
    }

    public enum LogType {
        Message,
        Info,
        Warning,
        Error,
        Pass,
        Fail
    }
}
