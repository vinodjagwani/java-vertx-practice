package com.airline.booking.demo.feature.bookings;

import com.airline.booking.demo.common.validation.RequestValidationHandler;
import com.airline.booking.demo.feature.bookings.dto.BookingRequest;
import com.airline.booking.demo.feature.bookings.handler.BookingHandler;
import com.google.inject.Inject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

public class BookingFeature {

    private final BookingHandler handler;

    private final RequestValidationHandler requestValidationHandler;

    @Inject
    public BookingFeature(final BookingHandler handler, final RequestValidationHandler requestValidationHandler) {
        this.handler = handler;
        this.requestValidationHandler = requestValidationHandler;
    }

    public void init(final Router router) {
        router.route("/bookings*").handler(BodyHandler.create());

        router.post("/bookings")
                .handler(requestValidationHandler.validate(BookingRequest.class))
                .handler(handler::create);

        router.get("/bookings/:id").handler(handler::getById);
        router.delete("/bookings/:id").handler(handler::cancel);
    }
}
