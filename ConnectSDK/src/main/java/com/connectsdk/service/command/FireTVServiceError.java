package com.connectsdk.service.command;

/**
 * This class implements an exception for FireTVService
 */
public class FireTVServiceError extends ServiceCommandError {

    public FireTVServiceError(String message) {
        super(message);
    }

    public FireTVServiceError(String message, Throwable e) {
        super(message);
        this.payload = e;
    }
}
