package com.sap.model.dto;

import com.sap.validation.ValidTransportationMethod;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Co2CalculateRequestDTO {

    @NotBlank(message = "Start city cannot be blank")
    private String start;

    @NotBlank(message = "End city cannot be blank")
    private String end;

    @ValidTransportationMethod
    @NotBlank(message = "Transportation method cannot be blank")
    private String transportationMethod;

}
