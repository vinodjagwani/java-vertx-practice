package com.airline.booking.demo;

import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Launcher {

    private static final Logger log = LoggerFactory.getLogger(Launcher.class);

    public static void main(final String... args) {
        final Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new MainVerticle())
                .onSuccess(id -> log.info("MainVerticle deployed!"))
                .onFailure(err -> {
                    log.error("Failed to start {]", err);
                    System.exit(1);
                });
    }
}
