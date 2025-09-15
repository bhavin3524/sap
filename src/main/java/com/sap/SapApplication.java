package com.sap;

import com.sap.model.dto.Co2CalculateRequestDTO;
import com.sap.service.Co2CalculatorService;
import com.sap.utility.AppConstants;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Set;

@SpringBootApplication
public class SapApplication implements CommandLineRunner {

    @Autowired
    private Co2CalculatorService co2Service;

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(SapApplication.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        app.run(args);
    }

    @Override
    public void run(String... args) throws Exception {
        Co2CalculateRequestDTO request = parseArgs(args);

        // Validate request
        Set<ConstraintViolation<Co2CalculateRequestDTO>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            violations.forEach(v -> System.err.println(v.getMessage()));
            System.err.println("Usage: --start <City> --end <City> --transportation-method <method>");
            return;
        }

        double distanceKm = co2Service.getDistanceKm(request.getStart(), request.getEnd());
        double co2Kg = co2Service.calculateCo2Kg(distanceKm, request.getTransportationMethod());

        System.out.printf("Your trip caused %.1fkg of CO2-equivalent.%n", co2Kg);
    }

    private Co2CalculateRequestDTO parseArgs(String[] args) {
        Co2CalculateRequestDTO request = new Co2CalculateRequestDTO();
        for (int i = 0; i < args.length; i++) {
            String[] parts = args[i].split("=", 2);
            String key = parts[0].replace("--", StringUtils.EMPTY).trim();
            String value = parts.length > 1 ? parts[1] : (i + 1 < args.length ? args[i + 1] : null);

            switch (key) {
                case AppConstants.START -> request.setStart(value);
                case AppConstants.END -> request.setEnd(value);
                case AppConstants.TRANSPORTATION_METHOD -> request.setTransportationMethod(value);
            }
        }
        return request;
    }

}
