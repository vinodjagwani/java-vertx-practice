package com.airline.booking.demo.common.validation;

import com.airline.booking.demo.exception.BusinessServiceException;
import com.airline.booking.demo.exception.dto.ErrorCodeEnum;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Set;


@Singleton
public class ValidationService {

    private final Validator validator;

    @Inject
    public ValidationService(final ValidatorProvider provider) {
        this.validator = provider.getValidator();
    }

    public <T> void validate(final T target) {
        final Set<ConstraintViolation<T>> violations = validator.validate(target);

        if (!violations.isEmpty()) {
            final String msg = violations.stream()
                    .map(v -> v.getPropertyPath() + " " + v.getMessage())
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("Validation failed");

            throw new BusinessServiceException(ErrorCodeEnum.INVALID_PARAM, msg);
        }
    }
}
