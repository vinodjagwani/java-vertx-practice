package com.airline.booking.demo.exception.dto;

import com.airline.booking.demo.exception.ErrorPrinter;
import io.netty.handler.codec.http.HttpResponseStatus;


public enum ErrorCodeEnum implements ErrorPrinter {

    INTERNAL_ERROR(HttpResponseStatus.INTERNAL_SERVER_ERROR),
    DATABASE_ERROR(HttpResponseStatus.BAD_REQUEST),
    ENTITY_NOT_FOUND(HttpResponseStatus.NOT_FOUND),
    INVALID_PARAM(HttpResponseStatus.BAD_REQUEST),
    CONFLICT(HttpResponseStatus.CONFLICT);

    private final HttpResponseStatus httpStatus;

    ErrorCodeEnum(final HttpResponseStatus status) {
        this.httpStatus = status;
    }

    @Override
    public HttpResponseStatus getHttpStatus() {
        return httpStatus;
    }
}

