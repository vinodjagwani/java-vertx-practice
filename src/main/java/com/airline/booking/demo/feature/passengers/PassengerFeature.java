package com.airline.booking.demo.feature.passengers;

import com.airline.booking.demo.common.validation.RequestValidationHandler;
import com.airline.booking.demo.feature.passengers.dto.PassengerRequest;
import com.airline.booking.demo.feature.passengers.handler.PassengerHandler;
import com.google.inject.Inject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

public class PassengerFeature {

    private final PassengerHandler handler;

    private final RequestValidationHandler requestValidationHandler;

    @Inject
    public PassengerFeature(final PassengerHandler handler, final RequestValidationHandler requestValidationHandler) {
        this.handler = handler;
        this.requestValidationHandler = requestValidationHandler;
    }

    public void init(final Router router) {
        router.route("/passengers*").handler(BodyHandler.create());

        router.post("/passengers")
                .handler(requestValidationHandler.validate(PassengerRequest.class))
                .handler(handler::create);

        router.get("/passengers/:id/bookings").handler(handler::getBookings);
    }
}
