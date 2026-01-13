package com.airline.booking.demo.feature.flights.handler;

import com.airline.booking.demo.exception.BusinessServiceException;
import com.airline.booking.demo.exception.dto.ErrorCodeEnum;
import com.airline.booking.demo.feature.flights.dto.FlightRequest;
import com.airline.booking.demo.feature.flights.mapper.FlightMapper;
import com.airline.booking.demo.feature.flights.service.FlightService;
import com.google.common.net.MediaType;
import com.google.inject.Inject;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;

public class FlightHandler {

    private final FlightService flightService;

    @Inject
    public FlightHandler(final FlightService flightService) {
        this.flightService = flightService;
    }

    public void getById(final RoutingContext ctx) {
        try {
            final long id = Long.parseLong(ctx.pathParam("id"));

            flightService.getById(id)
                    .onSuccess(f -> ctx.json(FlightMapper.toResponse(f)))
                    .onFailure(ctx::fail);

        } catch (NumberFormatException e) {
            ctx.fail(new BusinessServiceException(
                    ErrorCodeEnum.INVALID_PARAM,
                    "Invalid flight id"
            ));
        } catch (Exception e) {
            ctx.fail(e);
        }
    }

    public void search(final RoutingContext ctx) {
        final String from = ctx.queryParam("from").stream().findFirst().orElse(null);
        final String to = ctx.queryParam("to").stream().findFirst().orElse(null);

        if (from == null || to == null) {
            ctx.fail(new BusinessServiceException(
                    ErrorCodeEnum.INVALID_PARAM,
                    "from/to required"
            ));
            return;
        }

        flightService.search(from, to)
                .onSuccess(list -> ctx.json(
                        list.stream().map(FlightMapper::toResponse).toList()
                ))
                .onFailure(ctx::fail);
    }

    public void create(final RoutingContext ctx) {
        try {
            final FlightRequest req = ctx.get("validated_body");

            flightService.create(req)
                    .map(FlightMapper::toResponse)
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
