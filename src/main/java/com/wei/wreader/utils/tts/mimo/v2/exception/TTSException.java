package com.wei.wreader.utils.tts.mimo.v2.exception;

/**
 * MiMo TTS API 异常
 */
public class TTSException extends Exception {

    private final int statusCode;
    private final String errorCode;

    public TTSException(String message) {
        super(message);
        this.statusCode = -1;
        this.errorCode = null;
    }

    public TTSException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
        this.errorCode = null;
    }

    public TTSException(String message, int statusCode, String errorCode) {
        super(message);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
    }

    public TTSException(String message, Throwable cause) {
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
            return String.format("TTSException{message='%s', statusCode=%d, errorCode='%s'}",
                    getMessage(), statusCode, errorCode);
        }
        return String.format("TTSException{message='%s', statusCode=%d}", getMessage(), statusCode);
    }
}
