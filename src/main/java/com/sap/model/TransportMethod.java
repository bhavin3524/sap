package com.sap.model;


import com.sap.utility.AppConstants;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TransportMethod {
    private static final Map<String, Integer> EMISSION_MAP = new HashMap<>();

    static {
        EMISSION_MAP.put(AppConstants.DIESEL_CAR_SMALL, 142);
        EMISSION_MAP.put(AppConstants.PETROL_CAR_SMALL, 154);
        EMISSION_MAP.put(AppConstants.PLUGIN_HYBRID_CAR_SMALL, 73);
        EMISSION_MAP.put(AppConstants.ELECTRIC_CAR_SMALL, 50);
        EMISSION_MAP.put(AppConstants.DIESEL_CAR_MEDIUM, 171);
        EMISSION_MAP.put(AppConstants.PETROL_CAR_MEDIUM, 192);
        EMISSION_MAP.put(AppConstants.PLUGIN_HYBRID_CAR_MEDIUM, 110);
        EMISSION_MAP.put(AppConstants.ELECTRIC_CAR_MEDIUM, 58);
        EMISSION_MAP.put(AppConstants.DIESEL_CAR_LARGE, 209);
        EMISSION_MAP.put(AppConstants.PETROL_CAR_LARGE, 282);
        EMISSION_MAP.put(AppConstants.PLUGIN_HYBRID_CAR_LARGE, 126);
        EMISSION_MAP.put(AppConstants.ELECTRIC_CAR_LARGE, 73);
        EMISSION_MAP.put(AppConstants.BUS_DEFAULT, 27);
        EMISSION_MAP.put(AppConstants.TRAIN_DEFAULT, 6);
    }

    public static Integer getEmissionRate(String method) {
        return EMISSION_MAP.get(method);
    }

    public static boolean isValidMethod(String method) {
        return EMISSION_MAP.containsKey(method);
    }

    public static Set<String> getAllMethods() {
        return EMISSION_MAP.keySet();
    }
}