package com.sap.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;


@Documented
@Constraint(validatedBy = TransportationMethodValidator.class)
@Target({FIELD})
@Retention(RUNTIME)
public @interface ValidTransportationMethod {
    String message() default "Invalid transportation method. Please choose from a valid list.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
