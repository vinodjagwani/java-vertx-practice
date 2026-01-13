package com.airline.booking.demo.bootstrap;

import com.airline.booking.demo.common.db.ReactiveTx;
import com.airline.booking.demo.common.validation.RequestValidationHandler;
import com.airline.booking.demo.common.validation.ValidationService;
import com.airline.booking.demo.common.validation.ValidatorProvider;
import com.airline.booking.demo.config.DbPoolProvider;
import com.airline.booking.demo.feature.airlines.repository.AirlineRepository;
import com.airline.booking.demo.feature.airlines.repository.impl.AirlineRepositoryImpl;
import com.airline.booking.demo.feature.bookings.repository.BookingRepository;
import com.airline.booking.demo.feature.bookings.repository.impl.BookingRepositoryImpl;
import com.airline.booking.demo.feature.flights.repository.FlightRepository;
import com.airline.booking.demo.feature.flights.repository.impl.FlightRepositoryImpl;
import com.airline.booking.demo.feature.passengers.repository.PassengerRepository;
import com.airline.booking.demo.feature.passengers.repository.impl.PassengerRepositoryImpl;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Pool;

public class AppModule extends AbstractModule {

    private final Vertx vertx;
    private final JsonObject config;

    public AppModule(final Vertx vertx, final JsonObject config) {
        this.vertx = vertx;
        this.config = config;
    }

    @Override
    protected void configure() {
        bind(Vertx.class).toInstance(vertx);

        bind(ReactiveTx.class).in(Singleton.class);

        // Repositories
        bind(AirlineRepository.class).to(AirlineRepositoryImpl.class).in(Singleton.class);
        bind(FlightRepository.class).to(FlightRepositoryImpl.class).in(Singleton.class);
        bind(PassengerRepository.class).to(PassengerRepositoryImpl.class).in(Singleton.class);
        bind(BookingRepository.class).to(BookingRepositoryImpl.class).in(Singleton.class);

        // Validator
        bind(ValidatorProvider.class).in(Singleton.class);
        bind(ValidationService.class).in(Singleton.class);
        bind(RequestValidationHandler.class).in(Singleton.class);

        // DB bootstrap
        bind(DatabaseBootstrap.class).in(Singleton.class);
    }

    @Provides
    @Singleton
    @Named("app-config")
    JsonObject provideConfig() {
        return config;
    }

    @Provides
    @Singleton
    public Pool providePool(final Vertx vertx, @Named("app-config") final JsonObject config) {
        return DbPoolProvider.createPool(vertx, config);
    }
}
