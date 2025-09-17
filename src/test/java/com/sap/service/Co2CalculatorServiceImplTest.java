package com.sap.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.exception.CityNotFoundException;
import com.sap.exception.ForbiddenException;
import com.sap.exception.InternalServerErrorException;
import com.sap.exception.NetworkException;
import com.sap.exception.UnknownTransportMethodException;
import com.sap.service.impl.Co2CalculatorServiceImpl;
import com.sap.utility.AppConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


//@Disabled("Temporarily disabling all tests in this class")
@SpringBootTest
@ExtendWith(MockitoExtension.class)
@TestPropertySource("classpath:application-test.properties")
class Co2CalculatorServiceImplTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private MessageSource messageSource;

    private Co2CalculatorServiceImpl service;

    private ObjectMapper mapper;

    @Value("${ORS_TOKEN}")
    private String token;

    @Value("${OPEN_ROUTE_API_GEOCODE}")
    private String geoApi;

    @Value("${OPEN_ROUTE_API_MATRIX}")
    private String matrixApi;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();

        lenient().when(messageSource.getMessage(anyString(), any(), anyString(), any(Locale.class)))
                .thenAnswer(invocation -> invocation.getArgument(2));

        service = new Co2CalculatorServiceImpl(
                restTemplate,
                mapper,
                token,
                geoApi,
                matrixApi,
                messageSource
        );

        Mockito.reset(restTemplate);
    }

    @Test
    void testCalculateCo2Kg_validTransport() {
        double result = service.calculateCo2Kg(100, AppConstants.DIESEL_CAR_SMALL);
        assertTrue(result > 0);
    }

    @Test
    void testCalculateCo2Kg_unknownTransport() {
        assertThrows(UnknownTransportMethodException.class,
                () -> service.calculateCo2Kg(10, AppConstants.UNKNOWN_TRANSPORT));
    }

    @Test
    void testGetCoordinates_success() throws Exception {
        String city = "Berlin";
        String geoBody = """
                {
                  "features":[{"geometry":{"coordinates":[13.4,52.5]}}]
                }
                """;

        when(restTemplate.getForEntity(anyString(), eq(String.class)))
                .thenReturn(ResponseEntity.ok(geoBody));

        double[] coords = service.getCoordinates(city);

        assertNotNull(coords);
        assertEquals(13.4, coords[0]);
        assertEquals(52.5, coords[1]);
    }

    @Test
    void testGetCoordinates_cityNotFound() {
        String geoBody = "{\"features\":[]}";
        when(restTemplate.getForEntity(anyString(), eq(String.class)))
                .thenReturn(ResponseEntity.ok(geoBody));

        assertThrows(CityNotFoundException.class,
                () -> service.getCoordinates("UnknownCity"));
    }

    @Test
    void testGetDistanceKm_success() throws Exception {
        double[] coords = new double[]{13.4, 52.5};
        Co2CalculatorServiceImpl spyService = spy(service);

        doReturn(coords).when(spyService).getCoordinates("Berlin");
        doReturn(coords).when(spyService).getCoordinates("Munich");

        String matrixBody = """
                {
                  "distances": [[0, 36000], [36000, 0]]
                }
                """;

        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok(matrixBody));

        double distance = spyService.getDistanceKm("Berlin", "Munich");
        assertEquals(36.0, distance, 0.01); // 36000 meters = 36 km
    }

    @Test
    void testGetDistanceKm_matrixForbidden() throws Exception {
        double[] coords = new double[]{13.4, 52.5};
        Co2CalculatorServiceImpl spyService = spy(service);

        doReturn(coords).when(spyService).getCoordinates("Berlin");
        doReturn(coords).when(spyService).getCoordinates("Munich");

        RestClientResponseException ex = new RestClientResponseException(
                "Forbidden", 403, "Forbidden", new HttpHeaders(), null, StandardCharsets.UTF_8);

        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class)))
                .thenThrow(ex);

        assertThrows(ForbiddenException.class, () -> spyService.getDistanceKm("Berlin", "Munich"));
    }

    @Test
    void testGetDistanceKm_networkException() throws Exception {
        double[] coords = new double[]{13.4, 52.5};
        Co2CalculatorServiceImpl spyService = spy(service);

        doReturn(coords).when(spyService).getCoordinates("Berlin");
        doReturn(coords).when(spyService).getCoordinates("Munich");

        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new ResourceAccessException("Connection refused"));

        assertThrows(NetworkException.class, () -> spyService.getDistanceKm("Berlin", "Munich"));
    }

    @Test
    void testGetDistanceKm_matrixInvalidResponse() throws Exception {
        double[] coords = new double[]{13.4, 52.5};
        Co2CalculatorServiceImpl spyService = spy(service);

        doReturn(coords).when(spyService).getCoordinates("Berlin");
        doReturn(coords).when(spyService).getCoordinates("Ahmedabad");

        String matrixBody = "{\"distances\": []}";

        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok(matrixBody));

        assertThrows(InternalServerErrorException.class, () -> spyService.getDistanceKm("Berlin", "Ahmedabad"));
    }
}