package com.com4energy.recordsapi.response;

import java.time.Instant;

public record ApiError( int status, String message, String timestamp) {

    public ApiError(int status, String message) {
        this(status, message, Instant.now().toString());
    }

}
