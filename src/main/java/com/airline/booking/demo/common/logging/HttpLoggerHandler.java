package com.airline.booking.demo.common.logging;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class HttpLoggerHandler implements Handler<RoutingContext> {

    private static final Logger log = LoggerFactory.getLogger(HttpLoggerHandler.class);

    private static final String HDR_CORRELATION_ID = "X-Correlation-Id";
    private static final String HDR_FORWARDED = "X-Forwarded-For";

    private static final String MDC_CORRELATION_ID = "correlationId";
    private static final String MDC_METHOD = "method";
    private static final String MDC_PATH = "path";
    private static final String MDC_IP = "ip";

    @Override
    public void handle(final RoutingContext ctx) {

        final long startTime = System.currentTimeMillis();
        final HttpServerRequest req = ctx.request();

        String correlationId = req.getHeader(HDR_CORRELATION_ID);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        String ip = req.getHeader(HDR_FORWARDED);
        if (ip == null || ip.isBlank()) {
            ip = req.remoteAddress() != null ? req.remoteAddress().host() : "unknown";
        }

        final String method = req.method().name();
        final String path = req.path();

        MDC.put(MDC_CORRELATION_ID, correlationId);
        MDC.put(MDC_METHOD, method);
        MDC.put(MDC_PATH, path);
        MDC.put(MDC_IP, ip);

        ctx.response().putHeader(HDR_CORRELATION_ID, correlationId);

        log.info("HTTP_START");

        ctx.response().endHandler(v -> {
            final int status = ctx.response().getStatusCode();
            final long elapsed = System.currentTimeMillis() - startTime;

            log.info("HTTP_END status={} durationMs={}", status, elapsed);

            MDC.clear();
        });

        ctx.response().exceptionHandler(err -> {
            final long elapsed = System.currentTimeMillis() - startTime;

            log.error("HTTP_ERROR status={} durationMs={} message=\"{}\"",
                    ctx.response().getStatusCode(), elapsed, err.getMessage(), err);

            MDC.clear();
        });

        ctx.next();
    }
}

