package com.airline.booking.demo.bootstrap;

import com.airline.booking.demo.exception.BusinessServiceException;
import com.airline.booking.demo.exception.dto.ErrorCodeEnum;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import org.h2.tools.RunScript;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class DatabaseBootstrap {

    private static final Logger log = LoggerFactory.getLogger(DatabaseBootstrap.class);

    private final Vertx vertx;
    private final String h2Url;
    private final String h2User;
    private final String h2Password;

    @Inject
    public DatabaseBootstrap(final Vertx vertx, @Named("app-config") final JsonObject config) {
        this.vertx = vertx;
        final JsonObject db = config.getJsonObject("database", new JsonObject());

        this.h2Url = db.getString("url", "jdbc:h2:mem:dev_airline;DB_CLOSE_DELAY=-1;CASE_INSENSITIVE_IDENTIFIERS=TRUE");
        this.h2User = db.getString("user", "sa");
        this.h2Password = db.getString("password", "");
    }

    public Future<Void> bootstrap() {
        log.info("Starting H2 Database Bootstrap sequence...");
        return runScriptOnH2("db/schema.sql")
                .compose(v -> {
                    log.info("Schema applied. Starting data seeding...");
                    return runScriptOnH2("db/data.sql");
                })
                .onSuccess(v -> log.info("H2 Bootstrap Complete. Tables are ready."));
    }

    private Future<Void> runScriptOnH2(final String resourcePath) {
        return vertx.executeBlocking(() -> {
            try (final var is = DatabaseBootstrap.class.getClassLoader().getResourceAsStream(resourcePath)) {
                if (is == null) {
                    throw new IllegalStateException("SQL resource not found: " + resourcePath);
                }

                log.info("Executing JDBC Script: {} on {}", resourcePath, h2Url);

                try (final Connection conn = DriverManager.getConnection(h2Url, h2User, h2Password)) {
                    RunScript.execute(conn, new InputStreamReader(is, StandardCharsets.UTF_8));
                }
                return null;
            } catch (Exception e) {
                log.error("Bootstrap failure at {}: ", resourcePath, e);
                throw new BusinessServiceException(ErrorCodeEnum.DATABASE_ERROR, "Failed to initialize db script");
            }
        });
    }
}
