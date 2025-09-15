package com.sap.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.model.TransportMethod;
import com.sap.model.dto.MatrixRequest;
import com.sap.service.Co2CalculatorService;
import com.sap.utility.AppConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Objects;

@Service
public class Co2CalculatorServiceImpl implements Co2CalculatorService {

    private final RestTemplate restTemplate;
    private final ObjectMapper mapper;
    private final String ORS_TOKEN;
    private final String GEO_CODE_API;
    private final String MATRIX_API;

    @Autowired
    public Co2CalculatorServiceImpl(
            RestTemplate restTemplate,
            ObjectMapper mapper,
            @Value("${OPEN_ROUTE_API_TOKEN}") String ORS_TOKEN,
            @Value("${OPEN_ROUTE_API_GEOCODE}") String GEO_CODE_API,
            @Value("${OPEN_ROUTE_API_MATRIX}") String MATRIX_API) {
        this.restTemplate = restTemplate;
        this.mapper = mapper;
        this.ORS_TOKEN = ORS_TOKEN;
        this.GEO_CODE_API = GEO_CODE_API;
        this.MATRIX_API = MATRIX_API;
    }


    @Override
    public double getDistanceKm(String cityStart, String cityEnd) throws Exception {
        double[] startCoords = getCoordinates(cityStart);
        double[] endCoords = getCoordinates(cityEnd);

        MatrixRequest request = new MatrixRequest(List.of(startCoords, endCoords));
        String body = mapper.writeValueAsString(request);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.AUTHORIZATION, ORS_TOKEN);

        HttpEntity<String> entity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.exchange(
                MATRIX_API, HttpMethod.POST, entity, String.class);

        JsonNode res = mapper.readTree(response.getBody());
        double distanceMeters = res.get(AppConstants.DISTANCES).get(0).get(1).asDouble();
        return distanceMeters / 1000.0;
    }

    @Override
    public double calculateCo2Kg(double distanceKm, String transportMethod) {
        Integer rate = TransportMethod.getEmissionRate(transportMethod);
        if (rate == null)
            throw new IllegalArgumentException("Unknown transportation method: " + transportMethod);
        return Math.round((distanceKm * rate / 1000.0) * 10.0) / 10.0;
    }

    private double[] getCoordinates(String city) throws Exception {
        String URL = GEO_CODE_API + "?api_key=" + ORS_TOKEN + "&text=" + city + "&layers=locality";
        ResponseEntity<String> response = restTemplate.getForEntity(URL, String.class);

        JsonNode root = mapper.readTree(response.getBody());
        JsonNode features = root.get("features");

        if (Objects.isNull(features) || features.isEmpty()) {
            throw new Exception("City not found: " + city);
        }

        JsonNode coords = features.get(0).get("geometry").get("coordinates");
        return new double[]{coords.get(0).asDouble(), coords.get(1).asDouble()};
    }
}
