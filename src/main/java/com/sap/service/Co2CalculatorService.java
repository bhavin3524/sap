package com.sap.service;

public interface Co2CalculatorService {
    double getDistanceKm(String start, String end) throws Exception;

    double calculateCo2Kg(double distanceKm, String transportMethod);
}
