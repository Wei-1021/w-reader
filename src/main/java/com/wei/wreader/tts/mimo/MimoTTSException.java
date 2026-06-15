package com.wei.wreader.tts.mimo;

/**
 * MiMo TTS v2.5 异常
 */
public class MimoTTSException extends Exception {

    private final int statusCode;
    private final String errorCode;

    public MimoTTSException(String message) {
        super(message);
        this.statusCode = -1;
        this.errorCode = null;
    }

    public MimoTTSException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
        this.errorCode = null;
    }

    public MimoTTSException(String message, int statusCode, String errorCode) {
        super(message);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
    }

    public MimoTTSException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = -1;
        this.errorCode = null;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    @Override
    public String toString() {
        if (errorCode != null) {
            return String.format("MimoTTSException{message='%s', statusCode=%d, errorCode='%s'}",
                    getMessage(), statusCode, errorCode);
        }
        return String.format("MimoTTSException{message='%s', statusCode=%d}", getMessage(), statusCode);
    }
}
