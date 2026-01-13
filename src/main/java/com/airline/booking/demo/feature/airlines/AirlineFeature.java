package com.airline.booking.demo.feature.airlines;

import com.airline.booking.demo.common.validation.RequestValidationHandler;
import com.airline.booking.demo.feature.airlines.dto.AirlineRequest;
import com.airline.booking.demo.feature.airlines.handler.AirlineHandler;
import com.google.inject.Inject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

public class AirlineFeature {

    private final AirlineHandler handler;

    private final RequestValidationHandler requestValidationHandler;

    @Inject
    public AirlineFeature(final AirlineHandler handler, final RequestValidationHandler requestValidationHandler) {
        this.handler = handler;
        this.requestValidationHandler = requestValidationHandler;
    }

    public void init(final Router router) {
        router.route("/airlines*").handler(BodyHandler.create());

        router.get("/airlines").handler(handler::getAll);
        router.get("/airlines/:id").handler(handler::getById);
        router.post("/airlines")
                .handler(requestValidationHandler.validate(AirlineRequest.class))
                .handler(handler::create);
    }
}
