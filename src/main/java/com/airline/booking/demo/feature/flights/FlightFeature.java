package com.airline.booking.demo.feature.flights;

import com.airline.booking.demo.common.validation.RequestValidationHandler;
import com.airline.booking.demo.feature.flights.dto.FlightRequest;
import com.airline.booking.demo.feature.flights.handler.FlightHandler;
import com.google.inject.Inject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

public class FlightFeature {

    private final FlightHandler handler;

    private final RequestValidationHandler requestValidationHandler;

    @Inject
    public FlightFeature(final FlightHandler handler, final RequestValidationHandler requestValidationHandler) {
        this.handler = handler;
        this.requestValidationHandler = requestValidationHandler;
    }

    public void init(final Router router) {
        router.route("/flights*").handler(BodyHandler.create());

        router.get("/flights/search").handler(handler::search);
        router.get("/flights/:id").handler(handler::getById);

        router.post("/flights")
                .handler(requestValidationHandler.validate(FlightRequest.class))
                .handler(handler::create);
    }
}
