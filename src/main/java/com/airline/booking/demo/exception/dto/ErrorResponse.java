package com.airline.booking.demo.exception.dto;

import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ErrorResponse {

    int code;
    String message;
    List<ErrorInfo> errors;

    String correlationId;
    String path;
    String method;
    String timestamp;

    @Value
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ErrorInfo {

        String domain;
        String reason;
        String message;
    }
}
