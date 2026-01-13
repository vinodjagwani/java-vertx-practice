package com.airline.booking.demo.config;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigProvider {

    private static final Logger log = LoggerFactory.getLogger(ConfigProvider.class);

    public static Future<JsonObject> load(final Vertx vertx) {
        final String profile = System.getProperty("profile",
                System.getenv().getOrDefault("APP_PROFILE", "dev"));

        log.info("Active config profile: {}", profile);

        final Future<JsonObject> baseFuture = readJson(vertx, "application.json");
        final Future<JsonObject> profileFuture = readJson(vertx, "application-" + profile + ".json")
                .recover(err -> Future.succeededFuture(new JsonObject()));

        return baseFuture.compose(base -> profileFuture.map(override -> {
            final JsonObject merged = deepMerge(base, override);
            log.info("Final merged config: \n{}", merged.encodePrettily());
            return merged;
        }));
    }

    private static Future<JsonObject> readJson(final Vertx vertx, final String path) {
        return vertx.fileSystem().exists(path).compose(found -> {
            if (!found) {
                return Future.succeededFuture(new JsonObject());
            }
            return vertx.fileSystem().readFile(path).map(Buffer::toJsonObject);
        });
    }

    private static JsonObject deepMerge(final JsonObject target, final JsonObject source) {
        source.forEach(entry -> {
            final var key = entry.getKey();
            final var value = entry.getValue();
            final Object targetVal = target.getValue(key);
            if (value instanceof JsonObject sourceObj && targetVal instanceof JsonObject targetObj) {
                deepMerge(targetObj, sourceObj);
            } else {
                target.put(key, value);
            }
        });
        return target;
    }
}
