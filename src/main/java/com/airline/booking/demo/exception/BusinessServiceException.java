package com.airline.booking.demo.exception;

import io.netty.handler.codec.http.HttpResponseStatus;
import java.io.Serial;
import java.io.Serializable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@EqualsAndHashCode(callSuper = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public final class BusinessServiceException extends RuntimeException implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String message;

    private final HttpResponseStatus httpStatus;

    private final ErrorPrinter errorEnum;

    public BusinessServiceException(final ErrorPrinter errorEnum, final String message) {
        super(message);
        this.message = message;
        this.errorEnum = errorEnum;
        this.httpStatus = errorEnum.getHttpStatus();
    }
}
