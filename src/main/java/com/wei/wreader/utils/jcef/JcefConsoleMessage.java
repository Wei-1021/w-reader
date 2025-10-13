package com.wei.wreader.utils.jcef;

import org.cef.CefSettings;

public class JcefConsoleMessage {
    private final CefSettings.LogSeverity level;
    private final String message;
    private final String source;
    private final int line;

    public JcefConsoleMessage(CefSettings.LogSeverity level, String message, String source, int line) {
        this.level = level;
        this.message = message;
        this.source = source;
        this.line = line;
    }

    public CefSettings.LogSeverity getLevel() {
        return level;
    }

    public String getMessage() {
        return message;
    }

    public String getSource() {
        return source;
    }

    public int getLine() {
        return line;
    }
}