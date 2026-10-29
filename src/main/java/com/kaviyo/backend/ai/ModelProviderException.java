package com.kaviyo.backend.ai;

public final class ModelProviderException extends RuntimeException {
    private final String code;
    private final int httpStatus;

    public ModelProviderException(String code, String message, int httpStatus, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public String code() {
        return code;
    }

    public int httpStatus() {
        return httpStatus;
    }
}
