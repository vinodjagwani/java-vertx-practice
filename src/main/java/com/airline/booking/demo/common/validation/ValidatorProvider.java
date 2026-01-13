package com.airline.booking.demo.common.validation;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import lombok.Getter;

@Getter
@Singleton
public class ValidatorProvider {

    private final Validator validator;
    private final ValidatorFactory factory;

    @Inject
    public ValidatorProvider() {
        this.factory = Validation.buildDefaultValidatorFactory();
        this.validator = factory.getValidator();
        Runtime.getRuntime().addShutdownHook(new Thread(factory::close));
    }

}
