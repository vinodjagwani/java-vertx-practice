package com.airline.booking.demo.exception;

import com.airline.booking.demo.exception.dto.ErrorCodeEnum;
import io.vertx.pgclient.PgException;

public final class PgErrorMapper {

    private PgErrorMapper() {
    }

    public static BusinessServiceException map(final Throwable err, final String ctx) {
        final Throwable cause = err.getCause() != null ? err.getCause() : err;
        final String message = cause.getMessage() != null ? cause.getMessage() : "";

        if (cause instanceof BusinessServiceException be) {
            return be;
        }

        if (cause instanceof PgException pg) {
            final String code = pg.getSqlState();
            final String constraint = pg.getConstraint() != null ? pg.getConstraint() : "unknown";

            return switch (code) {
                case "23505" -> new BusinessServiceException(
                        ErrorCodeEnum.CONFLICT,
                        "Duplicate value violates unique constraint: " + constraint
                );
                case "23503" -> new BusinessServiceException(
                        ErrorCodeEnum.INVALID_PARAM,
                        "Operation violates foreign key constraint: " + constraint
                );
                case "23502" -> new BusinessServiceException(
                        ErrorCodeEnum.INVALID_PARAM,
                        "Required field is missing (NOT NULL constraint)"
                );
                case "22001" -> new BusinessServiceException(
                        ErrorCodeEnum.INVALID_PARAM,
                        "Value too long for column (string overflow)"
                );
                default -> new BusinessServiceException(
                        ErrorCodeEnum.DATABASE_ERROR,
                        ctx + ": SQL Error [" + code + "] " + pg.getMessage()
                );
            };
        }

        if (message.contains("23505") || message.toLowerCase().contains("unique")) {
            return new BusinessServiceException(
                    ErrorCodeEnum.CONFLICT,
                    "Duplicate value violates unique constraint"
            );
        }

        if (message.contains("23503")) {
            return new BusinessServiceException(
                    ErrorCodeEnum.INVALID_PARAM,
                    "Operation violates foreign key constraint"
            );
        }

        if (message.contains("23502")) {
            return new BusinessServiceException(
                    ErrorCodeEnum.INVALID_PARAM,
                    "Required field missing (NOT NULL constraint)"
            );
        }

        if (message.contains("22001")) {
            return new BusinessServiceException(
                    ErrorCodeEnum.INVALID_PARAM,
                    "Value too long for column (string overflow)"
            );
        }

        return new BusinessServiceException(
                ErrorCodeEnum.DATABASE_ERROR,
                ctx + ": " + message
        );
    }
}
