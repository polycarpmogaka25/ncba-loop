package com.integration.ncba.test.models.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CountryRequest {

    @NotBlank(message = "Country name is required")
    private String name;
}