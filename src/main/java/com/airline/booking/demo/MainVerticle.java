package com.airline.booking.demo;

import com.airline.booking.demo.bootstrap.AppModule;
import com.airline.booking.demo.bootstrap.DatabaseBootstrap;
import com.airline.booking.demo.common.logging.HttpLoggerHandler;
import com.airline.booking.demo.config.ConfigProvider;
import com.airline.booking.demo.config.JsonConfig;
import com.airline.booking.demo.exception.GlobalErrorHandler;
import com.airline.booking.demo.feature.airlines.AirlineFeature;
import com.airline.booking.demo.feature.bookings.BookingFeature;
import com.airline.booking.demo.feature.flights.FlightFeature;
import com.airline.booking.demo.feature.passengers.PassengerFeature;
import com.google.inject.Guice;
import com.google.inject.Injector;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainVerticle extends AbstractVerticle {

    private static final Logger log = LoggerFactory.getLogger(MainVerticle.class);

    private Injector injector;

    @Override
    public void start(final Promise<Void> startPromise) {
        log.info("=== Bootstrapping Airline Booking System ===");

        JsonConfig.register();

        initConfig()
                .compose(this::initDI)
                .compose(this::initDatabaseIfRequired)
                .compose(this::initHttpServer)
                .onSuccess(v -> {
                    log.info("=== Application Started Successfully ===");
                    startPromise.complete();
                })
                .onFailure(err -> {
                    log.error("Application failed to start", err);
                    startPromise.fail(err);
                });
    }

    private Future<JsonObject> initConfig() {
        log.info("Loading configuration...");
        return ConfigProvider.load(vertx)
                .onSuccess(cfg -> log.info("Configuration loaded: activeProfile={}", resolveProfile(cfg)));
    }

    private Future<JsonObject> initDI(final JsonObject config) {
        this.injector = Guice.createInjector(new AppModule(vertx, config));
        log.info("Dependency Injection initialized");
        return Future.succeededFuture(config);
    }

    private Future<JsonObject> initDatabaseIfRequired(final JsonObject config) {
        final String profile = resolveProfile(config);

        if ("prod".equals(profile)) {
            log.info("Skipping DB bootstrap for production profile={}", profile);
            return Future.succeededFuture(config);
        }

        log.info("Running DB bootstrap for profile={}", profile);
        final var dbBootstrap = injector.getInstance(DatabaseBootstrap.class);

        return dbBootstrap.bootstrap()
                .onSuccess(v -> log.info("DB bootstrap completed"))
                .onFailure(err -> log.error("DB bootstrap failed", err))
                .map(config);
    }

    private Future<Void> initHttpServer(final JsonObject config) {
        final Router router = Router.router(vertx);

        registerHttpLoggerRoutes(router);

        router.route().handler(BodyHandler.create());

        registerCoreRoutes(router);
        registerFeatureModules(router);
        registerFailureRoutes(router);

        return startHttpServer(router, config);
    }

    private void registerCoreRoutes(final Router router) {
        router.get("/").handler(ctx -> ctx.response().end("Welcome to Airline Booking API"));
        router.get("/health").handler(ctx -> ctx.response().end("OK"));
        router.get("/ready").handler(ctx -> ctx.response().end("READY"));
    }

    private void registerFailureRoutes(final Router router) {
        router.route().failureHandler(GlobalErrorHandler::handle);
    }

    private void registerHttpLoggerRoutes(final Router router) {
        router.route().handler(new HttpLoggerHandler());
    }


    private void registerFeatureModules(final Router router) {
        injector.getInstance(AirlineFeature.class).init(router);
        injector.getInstance(FlightFeature.class).init(router);
        injector.getInstance(PassengerFeature.class).init(router);
        injector.getInstance(BookingFeature.class).init(router);
        log.info("Feature modules registered");
    }

    private Future<Void> startHttpServer(final Router router, final JsonObject config) {
        final Promise<Void> promise = Promise.promise();

        final JsonObject serverCfg = config.getJsonObject("server", new JsonObject());
        final int port = serverCfg.getInteger("port", 8080);
        final String host = serverCfg.getString("host", "0.0.0.0");

        vertx.createHttpServer()
                .requestHandler(router)
                .listen(port, host)
                .onSuccess(server -> {
                    log.info("HTTP server listening on {}:{}", host, server.actualPort());
                    promise.complete();
                })
                .onFailure(err -> {
                    log.error("Failed to start HTTP server", err);
                    promise.fail(err);
                });

        return promise.future();
    }

    private String resolveProfile(final JsonObject config) {
        return config.getString("profile",
                System.getProperty("profile",
                        System.getenv().getOrDefault("APP_PROFILE", "dev")));
    }
}
