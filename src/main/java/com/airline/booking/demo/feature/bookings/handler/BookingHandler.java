package com.airline.booking.demo.feature.bookings.handler;

import com.airline.booking.demo.feature.bookings.dto.BookingRequest;
import com.airline.booking.demo.feature.bookings.mapper.BookingMapper;
import com.airline.booking.demo.feature.bookings.service.BookingService;
import com.google.common.net.MediaType;
import com.google.inject.Inject;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;

public class BookingHandler {

    private final BookingService bookingService;

    @Inject
    public BookingHandler(final BookingService bookingService) {
        this.bookingService = bookingService;
    }

    public void getById(final RoutingContext ctx) {
        final Long id = Long.valueOf(ctx.pathParam("id"));
        bookingService.getById(id)
                .map(BookingMapper::toResponse)
                .onSuccess(ctx::json)
                .onFailure(ctx::fail);
    }

    public void cancel(final RoutingContext ctx) {
        final Long id = Long.valueOf(ctx.pathParam("id"));
        bookingService.cancel(id)
                .onSuccess(v -> ctx.response()
                        .setStatusCode(HttpResponseStatus.NO_CONTENT.code())
                        .end())
                .onFailure(ctx::fail);
    }

    public void create(final RoutingContext ctx) {
        final BookingRequest req = ctx.get("validated_body");

        bookingService.create(req)
                .map(BookingMapper::toResponse)
                .onSuccess(resp -> {
                    final var response = ctx.response();
                    response.setStatusCode(HttpResponseStatus.CREATED.code());
                    response.putHeader("content-type", MediaType.JSON_UTF_8.toString());
                    response.end(Json.encode(resp));
                })
                .onFailure(ctx::fail);

    }
}
