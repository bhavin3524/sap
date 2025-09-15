package com.sap.validation;

import com.sap.model.TransportMethod;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.StringUtils;

public class TransportationMethodValidator implements ConstraintValidator<ValidTransportationMethod, String> {

    @Override
    public boolean isValid(String transportationMethod, ConstraintValidatorContext context) {
        if (StringUtils.isBlank(transportationMethod)) {
            setCustomMessage(context);
            return Boolean.FALSE;
        }

        if (!TransportMethod.isValidMethod(transportationMethod.trim())) {
            setCustomMessage(context);
            return Boolean.FALSE;
        }

        return Boolean.TRUE;
    }

    private void setCustomMessage(ConstraintValidatorContext context) {
        context.disableDefaultConstraintViolation();
        String validMethods = String.join(", ", TransportMethod.getAllMethods());
        context.buildConstraintViolationWithTemplate(
                "Invalid transportation method. Available methods: [" + validMethods + "] "
        ).addConstraintViolation();
    }
}