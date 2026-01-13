package com.airline.booking.demo.config;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.jdbcclient.JDBCConnectOptions;
import io.vertx.jdbcclient.JDBCPool;
import io.vertx.pgclient.PgBuilder;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DbPoolProvider {

    private static final Logger log = LoggerFactory.getLogger(DbPoolProvider.class);

    private DbPoolProvider() {
    }

    public static Pool createPool(final Vertx vertx, final JsonObject config) {
        final JsonObject db = config.getJsonObject("database", new JsonObject());
        final String profile = config.getString("profile", "dev");

        final String h2Url = db.getString("url",
                "jdbc:h2:mem:dev_airline;DB_CLOSE_DELAY=-1;CASE_INSENSITIVE_IDENTIFIERS=TRUE");

        if ("prod".equalsIgnoreCase(profile)) {
            log.info("Production Mode: Creating Pure Reactive PG Pool");
            PgConnectOptions pgOptions = new PgConnectOptions()
                    .setPort(db.getInteger("port", 5432))
                    .setHost(db.getString("host", "localhost"))
                    .setDatabase(db.getString("database"))
                    .setUser(db.getString("user"))
                    .setPassword(db.getString("password"))
                    .setPipeliningLimit(256);

            return PgBuilder.pool()
                    .connectingTo(pgOptions)
                    .with(new PoolOptions().setMaxSize(db.getInteger("maxPoolSize", 10)))
                    .using(vertx)
                    .build();
        }

        log.info("Dev Mode: Creating Reactive JDBC Pool for H2. URL: {}", h2Url);
        JDBCConnectOptions jdbcOptions = new JDBCConnectOptions()
                .setJdbcUrl(h2Url)
                .setUser(db.getString("user", "sa"))
                .setPassword(db.getString("password", ""));

        return JDBCPool.pool(vertx, jdbcOptions, new PoolOptions().setMaxSize(5));
    }
}
