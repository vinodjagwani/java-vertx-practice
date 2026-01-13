package com.airline.booking.demo.feature.passengers.handler;

import com.airline.booking.demo.exception.BusinessServiceException;
import com.airline.booking.demo.exception.dto.ErrorCodeEnum;
import com.airline.booking.demo.feature.bookings.service.BookingService;
import com.airline.booking.demo.feature.passengers.dto.PassengerRequest;
import com.airline.booking.demo.feature.passengers.mapper.PassengerMapper;
import com.airline.booking.demo.feature.passengers.service.PassengerService;
import com.google.common.net.MediaType;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;

@Singleton
public class PassengerHandler {

    private final BookingService bookingService;
    private final PassengerService passengerService;

    @Inject
    public PassengerHandler(final BookingService bookingService, final PassengerService passengerService) {
        this.bookingService = bookingService;
        this.passengerService = passengerService;
    }

    public void getBookings(final RoutingContext ctx) {
        try {
            final long id = Long.parseLong(ctx.pathParam("id"));

            passengerService.getById(id)
                    .compose(v -> bookingService.findByPassenger(id))
                    .onSuccess(ctx::json)
                    .onFailure(ctx::fail);

        } catch (NumberFormatException e) {
            ctx.fail(new BusinessServiceException(
                    ErrorCodeEnum.INVALID_PARAM, "Invalid passenger id"
            ));
        } catch (Exception e) {
            ctx.fail(e);
        }
    }

    public void create(final RoutingContext ctx) {
        try {
            final PassengerRequest req = ctx.get("validated_body");

            passengerService.create(req)
                    .map(PassengerMapper::toResponse)
                    .onSuccess(resp -> {
                        final var response = ctx.response();
                        response.setStatusCode(HttpResponseStatus.CREATED.code());
                        response.putHeader("content-type", MediaType.JSON_UTF_8.toString());
                        response.end(Json.encode(resp));
                    })
                    .onFailure(ctx::fail);

        } catch (Exception e) {
            ctx.fail(e);
        }
    }
}
