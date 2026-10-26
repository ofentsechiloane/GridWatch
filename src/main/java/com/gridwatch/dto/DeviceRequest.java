package com.gridwatch.dto;

import jakarta.validation.constraints.*;

public record DeviceRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude
) { }