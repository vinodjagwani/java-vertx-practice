package com.airline.booking.demo.exception;


import io.netty.handler.codec.http.HttpResponseStatus;

public interface ErrorPrinter {

    HttpResponseStatus getHttpStatus();

}
