package com.sap.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.exception.BadRequestException;
import com.sap.exception.CityNotFoundException;
import com.sap.exception.ForbiddenException;
import com.sap.exception.InternalServerErrorException;
import com.sap.exception.NetworkException;
import com.sap.model.TransportMethod;
import com.sap.model.dto.MatrixRequest;
import com.sap.service.Co2CalculatorService;
import com.sap.utility.AppConstants;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Locale;

@Service
public class Co2CalculatorServiceImpl implements Co2CalculatorService {

    private final RestTemplate restTemplate;
    private final ObjectMapper mapper;
    private final String ORS_TOKEN;
    private final String GEO_CODE_API;
    private final String MATRIX_API;
    private final MessageSource messageSource;

    public Co2CalculatorServiceImpl(RestTemplate restTemplate,
                                    ObjectMapper mapper,
                                    @Value("${ORS_TOKEN}") String ORS_TOKEN,
                                    @Value("${OPEN_ROUTE_API_GEOCODE}") String GEO_CODE_API,
                                    @Value("${OPEN_ROUTE_API_MATRIX}") String MATRIX_API,
                                    MessageSource messageSource) {
        this.restTemplate = restTemplate;
        this.mapper = mapper;
        this.ORS_TOKEN = ORS_TOKEN;
        this.GEO_CODE_API = GEO_CODE_API;
        this.MATRIX_API = MATRIX_API;
        this.messageSource = messageSource;
    }

    @Override
    public double getDistanceKm(String cityStart, String cityEnd) {
        double[] startCoords = getCoordinates(cityStart);
        double[] endCoords = getCoordinates(cityEnd);

        try {
            MatrixRequest request = new MatrixRequest(List.of(startCoords, endCoords));
            String body = mapper.writeValueAsString(request);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set(HttpHeaders.AUTHORIZATION, ORS_TOKEN);

            HttpEntity<String> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(MATRIX_API, HttpMethod.POST, entity, String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new BadRequestException(buildErrorMessage(
                        "matrix.client", cityStart, cityEnd, response.getStatusCode(), response.getBody()));
            }

            JsonNode res = mapper.readTree(response.getBody());
            JsonNode distances = res.get(AppConstants.DISTANCES);

            if (distances == null || distances.get(0) == null || distances.get(0).get(1) == null) {
                throw new InternalServerErrorException(buildErrorMessage("matrix.empty", cityStart, cityEnd));
            }

            return distances.get(0).get(1).asDouble() / 1000.0;

        } catch (RestClientResponseException e) {
            handleMatrixException(cityStart, cityEnd, e);
            throw new IllegalStateException("Unreachable code after handleMatrixException");
        } catch (ResourceAccessException e) {
            throw new NetworkException(buildErrorMessage("matrix.network", cityStart, cityEnd, e.getMessage()), e);
        } catch (Exception e) {
            throw new InternalServerErrorException(buildErrorMessage("matrix.server", cityStart, cityEnd, e.getMessage()));
        }
    }

    @Override
    public double calculateCo2Kg(double distanceKm, String transportMethod) {
        Integer rate = TransportMethod.getEmissionRate(transportMethod);
        if (rate == null) {
            throw new BadRequestException(buildErrorMessage("transport.unknown", transportMethod));
        }
        return Math.round((distanceKm * rate / 1000.0) * 10.0) / 10.0;
    }

    public double[] getCoordinates(String city) {

        if (StringUtils.isBlank(city)) {
            throw new BadRequestException(buildErrorMessage("city.blank", city));
        }

        try {

            String url = GEO_CODE_API + "?api_key=" + ORS_TOKEN + "&text=" + city + "&layers=locality";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new BadRequestException(buildErrorMessage("geo.client", city,
                        response.getStatusCode(), response.getBody()));
            }

            JsonNode root = mapper.readTree(response.getBody());
            JsonNode features = root.get(AppConstants.FEATURES);

            if (features == null || features.isEmpty()) {
                throw new CityNotFoundException(buildErrorMessage("geo.notfound", city));
            }

            JsonNode geometry = features.get(0).get(AppConstants.GEOMETRY);
            if (geometry == null || geometry.get(AppConstants.COORDINATES) == null || geometry.get(AppConstants.COORDINATES).size() < 2) {
                throw new InternalServerErrorException(buildErrorMessage("geo.invalid", city));
            }

            JsonNode coords = geometry.get(AppConstants.COORDINATES);
            return new double[]{coords.get(0).asDouble(), coords.get(1).asDouble()};

        } catch (CityNotFoundException e) {
            throw e; // do not wrap business exception
        } catch (RestClientResponseException e) {
            handleGeoException(city, e);
            throw new IllegalStateException("Unreachable code after handleGeoException");
        } catch (ResourceAccessException e) {
            throw new NetworkException(buildErrorMessage("geo.network", city, e.getMessage()), e);
        } catch (Exception e) {
            throw new InternalServerErrorException(buildErrorMessage("geo.server", city, e.getMessage()));
        }
    }

    private String buildErrorMessage(String key, Object... args) {
        Locale locale = LocaleContextHolder.getLocale();
        String defaultMsg = "An error occurred [" + key + "]";
        return messageSource.getMessage("error." + key, args, defaultMsg, locale);
    }


    private void handleGeoException(String city, RestClientResponseException e) {
        var statusCode = e.getStatusCode();
        if (statusCode.isSameCodeAs(HttpStatus.FORBIDDEN)) {
            throw new ForbiddenException(buildErrorMessage("geo.forbidden", city, e.getResponseBodyAsString()));
        } else if (statusCode.is4xxClientError()) {
            throw new BadRequestException(buildErrorMessage("geo.client", city, statusCode.value(), e.getResponseBodyAsString()));
        } else if (statusCode.is5xxServerError()) {
            throw new InternalServerErrorException(buildErrorMessage("geo.server", city, statusCode.value(), e.getResponseBodyAsString()));
        } else {
            throw new BadRequestException(buildErrorMessage("geo.client", city, statusCode.value(), e.getResponseBodyAsString()));
        }
    }

    private void handleMatrixException(String cityStart, String cityEnd, RestClientResponseException e) {
        var statusCode = e.getStatusCode();
        if (statusCode.isSameCodeAs(HttpStatus.FORBIDDEN)) {
            throw new ForbiddenException(buildErrorMessage("matrix.forbidden", cityStart, cityEnd, e.getResponseBodyAsString()));
        } else if (statusCode.is4xxClientError()) {
            throw new BadRequestException(buildErrorMessage("matrix.client", cityStart, cityEnd, statusCode.value(), e.getResponseBodyAsString()));
        } else if (statusCode.is5xxServerError()) {
            throw new InternalServerErrorException(buildErrorMessage("matrix.server", cityStart, cityEnd, statusCode.value(), e.getResponseBodyAsString()));
        } else {
            throw new BadRequestException(buildErrorMessage("matrix.client", cityStart, cityEnd, statusCode.value(), e.getResponseBodyAsString()));
        }
    }
}