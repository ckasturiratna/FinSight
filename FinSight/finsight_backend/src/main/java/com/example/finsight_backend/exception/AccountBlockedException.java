package com.example.finsight_backend.exception;

public class AccountBlockedException extends RuntimeException {
    
    private final long remainingMinutes;
    
    public AccountBlockedException(String message, long remainingMinutes) {
        super(message);
        this.remainingMinutes = remainingMinutes;
    }
    
    public AccountBlockedException(String message) {
        super(message);
        this.remainingMinutes = 0;
    }
    
    public long getRemainingMinutes() {
        return remainingMinutes;
    }
}


