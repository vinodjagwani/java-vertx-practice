package com.airline.booking.demo.feature.airlines.handler;

import com.airline.booking.demo.feature.airlines.dto.AirlineRequest;
import com.airline.booking.demo.feature.airlines.mapper.AirlineMapper;
import com.airline.booking.demo.feature.airlines.service.AirlineService;
import com.google.common.net.MediaType;
import com.google.inject.Inject;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;

public class AirlineHandler {

    private final AirlineService airlineService;

    @Inject
    public AirlineHandler(final AirlineService airlineService) {
        this.airlineService = airlineService;
    }

    public void getAll(final RoutingContext ctx) {
        airlineService.getAll()
                .map(list -> list.stream().map(AirlineMapper::toResponse).toList())
                .onSuccess(ctx::json)
                .onFailure(ctx::fail);
    }

    public void getById(final RoutingContext ctx) {
        final Long id = Long.valueOf(ctx.pathParam("id"));

        airlineService.getById(id)
                .map(AirlineMapper::toResponse)
                .onSuccess(ctx::json)
                .onFailure(ctx::fail);
    }

    public void create(final RoutingContext ctx) {
        final AirlineRequest req = ctx.get("validated_body");

        airlineService.create(req)
                .map(AirlineMapper::toResponse)
                .onSuccess(resp -> {
                    final var response = ctx.response();
                    response.setStatusCode(HttpResponseStatus.CREATED.code());
                    response.putHeader("content-type", MediaType.JSON_UTF_8.toString());
                    response.end(Json.encode(resp));
                })
                .onFailure(ctx::fail);
    }
}
