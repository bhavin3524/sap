package com.sap;

import com.sap.exception.BadRequestException;
import com.sap.exception.CityNotFoundException;
import com.sap.exception.ForbiddenException;
import com.sap.exception.UnknownTransportMethodException;
import com.sap.model.dto.Co2CalculateRequestDTO;
import com.sap.service.Co2CalculatorService;
import com.sap.utility.AppConstants;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
@TestPropertySource(properties = {
        "OPEN_ROUTE_API_GEOCODE=https://api.openrouteservice.org/geocode/search",
        "OPEN_ROUTE_API_MATRIX=https://api.openrouteservice.org/v2/matrix/driving-car",
        "ORS_TOKEN=5b3ce3597851110001cf624822f9170a3be2483e8681d85f65f928ce"
})
@MockitoSettings(strictness = Strictness.LENIENT)
class Co2CalculatorIntegrationTest {

    @Autowired
    private Co2CalculatorService co2Service;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private MessageSource messageSource;

    private void setupMessageSource() {
        lenient().when(messageSource.getMessage(anyString(), any(), anyString(), any()))
                .thenAnswer(invocation -> invocation.getArgument(2));
    }

    @Test
    void testFullFlow() throws Exception {
        setupMessageSource();

        String geoBody = """
                {
                  "features":[{"geometry":{"coordinates":[13.4,52.5]}}]
                }
                """;

        when(restTemplate.getForEntity(contains("Berlin"), eq(String.class)))
                .thenReturn(ResponseEntity.ok(geoBody));
        when(restTemplate.getForEntity(contains("Hamburg"), eq(String.class)))
                .thenReturn(ResponseEntity.ok(geoBody));

        String matrixBody = """
                {
                  "distances": [[0, 36000], [36000, 0]]
                }
                """;

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok(matrixBody));

        Co2CalculateRequestDTO request = new Co2CalculateRequestDTO();
        request.setStart("Berlin");
        request.setEnd("Hamburg");
        request.setTransportationMethod(AppConstants.DIESEL_CAR_SMALL);

        double distanceKm = co2Service.getDistanceKm(request.getStart(), request.getEnd());
        double co2 = co2Service.calculateCo2Kg(distanceKm, request.getTransportationMethod());

        System.out.printf("Distance: %.2f km, CO2: %.2f kg%n", distanceKm, co2);

        assertTrue(distanceKm > 0);
        assertTrue(co2 > 0);
    }

    @Test
    void testInvalidTransportMethod() {
        setupMessageSource();
        assertThrows(UnknownTransportMethodException.class,
                () -> co2Service.calculateCo2Kg(100, "UNKNOWN_TRANSPORT"));
    }

    @Test
    void testCityNotFound() {
        setupMessageSource();
        String body = "{\"features\":[]}";
        when(restTemplate.getForEntity(anyString(), eq(String.class)))
                .thenReturn(ResponseEntity.ok(body));

        assertThrows(CityNotFoundException.class,
                () -> co2Service.getDistanceKm("UnknownCity", "UnknownCity"));
    }

    @Test
    void testForbiddenException() {
        setupMessageSource();
        RestClientResponseException ex = new RestClientResponseException(
                "Forbidden", 403, "Forbidden", new HttpHeaders(),
                "{\"error\":\"Access denied\"}".getBytes(), StandardCharsets.UTF_8);

        when(restTemplate.getForEntity(anyString(), eq(String.class))).thenThrow(ex);

        assertThrows(ForbiddenException.class,
                () -> co2Service.getDistanceKm("Berlin", "Munich"));
    }

    @Test
    void testCoordinateInvalidRequestPayload() {
        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> co2Service.getDistanceKm("Berlin", StringUtils.EMPTY)
        );

        assertEquals("City name must not be blank.", exception.getMessage());
    }
}