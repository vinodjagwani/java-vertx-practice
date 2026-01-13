package com.airline.booking.demo.exception;

import com.airline.booking.demo.exception.dto.ErrorCodeEnum;
import com.airline.booking.demo.exception.dto.ErrorResponse;
import com.google.common.net.MediaType;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class GlobalErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalErrorHandler.class);

    public static void handle(final RoutingContext ctx) {

        Throwable failure = ctx.failure();
        if (failure == null) {
            failure = new BusinessServiceException(
                    ErrorCodeEnum.INTERNAL_ERROR,
                    "Unknown server error"
            );
        }

        final String correlationId = getKey(ctx, "correlationId", MDC.get("correlationId"));
        final String method = getKey(ctx, "method", MDC.get("method"));
        final String path = getKey(ctx, "path", MDC.get("path"));

        if (ctx.response().ended() || ctx.response().headWritten()) {
            log.error("Failure after response committed correlationId={} method={} path={} msg={}",
                    correlationId, method, path, failure.getMessage(), failure);
            return;
        }

        if (failure instanceof BusinessServiceException ex) {
            log.warn("Business error correlationId={} method={} path={} [{}]: {}",
                    correlationId, method, path, ex.getErrorEnum(), ex.getMessage());
            writeErrorResponse(ctx, ex.getErrorEnum(), ex.getMessage(), correlationId, method, path);
            return;
        }

        if (failure instanceof NumberFormatException) {
            writeErrorResponse(ctx, ErrorCodeEnum.INVALID_PARAM, "Invalid numeric parameter",
                    correlationId, method, path);
            return;
        }

        if (failure instanceof IllegalArgumentException || failure instanceof IllegalStateException) {
            writeErrorResponse(ctx, ErrorCodeEnum.INVALID_PARAM, failure.getMessage(),
                    correlationId, method, path);
            return;
        }

        log.error("Unhandled server error correlationId={} method={} path={} msg={}",
                correlationId, method, path, failure.getMessage(), failure);

        writeErrorResponse(ctx, ErrorCodeEnum.INTERNAL_ERROR, "Internal server error",
                correlationId, method, path);
    }

    private static void writeErrorResponse(
            final RoutingContext ctx,
            final ErrorPrinter code,
            final String message,
            final String correlationId,
            final String method,
            final String path
    ) {

        final String timestamp = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        final ErrorResponse error = ErrorResponse.builder()
                .code(code.getHttpStatus().code())
                .message(message)
                .errors(List.of(
                        ErrorResponse.ErrorInfo.builder()
                                .domain(code.getHttpStatus().reasonPhrase())
                                .reason("BUSINESS")
                                .message(message)
                                .build()
                ))
                .correlationId(correlationId)
                .path(path)
                .method(method)
                .timestamp(timestamp)
                .build();

        ctx.response()
                .setStatusCode(code.getHttpStatus().code())
                .putHeader("content-type", MediaType.JSON_UTF_8.toString())
                .end(Json.encode(error));
    }

    private static String getKey(final RoutingContext ctx, final String key, final String fallback) {
        final Object v = ctx.get(key);
        return v != null ? v.toString() : fallback;
    }
}
