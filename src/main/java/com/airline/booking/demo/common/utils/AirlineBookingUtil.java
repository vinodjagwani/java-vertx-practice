package com.airline.booking.demo.common.utils;

import io.vertx.sqlclient.Row;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public final class AirlineBookingUtil {

    private AirlineBookingUtil() {
        // Empty Constructor
    }


    public static OffsetDateTime convertSqlDateTimeToOffset(final Row row, final String name) {
        return switch (row.getValue(name)) {
            case OffsetDateTime odt -> odt;
            case LocalDateTime ldt -> ldt.atOffset(ZoneOffset.UTC);
            default -> null;
        };
    }

}
