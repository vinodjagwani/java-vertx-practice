package com.airline.booking.demo.common.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.net.MediaType;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import jakarta.validation.Validator;

@Singleton
public class RequestValidationHandler {

    private final Validator validator;
    private final ObjectMapper mapper;

    @Inject
    public RequestValidationHandler(final ValidatorProvider provider) {
        this.validator = provider.getValidator();
        this.mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public <T> Handler<RoutingContext> validate(final Class<T> clazz) {
        return ctx -> {
            try {
                final byte[] bytes = ctx.body().buffer().getBytes();
                final T body = mapper.readValue(bytes, clazz);

                final var violations = validator.validate(body);

                if (!violations.isEmpty()) {
                    final var errors = violations.stream()
                            .map(v -> v.getPropertyPath() + " " + v.getMessage())
                            .toList();

                    ctx.response()
                            .setStatusCode(HttpResponseStatus.BAD_REQUEST.code())
                            .putHeader("content-type", MediaType.JSON_UTF_8.toString())
                            .end(new JsonObject()
                                    .put("code", HttpResponseStatus.BAD_REQUEST.code())
                                    .put("errors", errors)
                                    .encode());
                    return;
                }

                ctx.put("validated_body", body);
                ctx.next();

            } catch (Exception e) {
                ctx.response()
                        .setStatusCode(HttpResponseStatus.BAD_REQUEST.code())
                        .putHeader("content-type", MediaType.JSON_UTF_8.toString())
                        .end(new JsonObject()
                                .put("code", HttpResponseStatus.BAD_REQUEST.code())
                                .put("message", "Invalid JSON body")
                                .encode());
            }
        };
    }
}
