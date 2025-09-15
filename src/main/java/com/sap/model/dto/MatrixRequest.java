package com.sap.model.dto;

import com.sap.utility.AppConstants;

import java.util.List;

public class MatrixRequest {
    public List<double[]> locations;
    public List<String> metrics = List.of(AppConstants.DISTANCE);

    public MatrixRequest(List<double[]> locations) {
        this.locations = locations;
    }
}
