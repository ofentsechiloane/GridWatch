package com.gridwatch.dto;

import com.gridwatch.model.ElectricityStatus;
import jakarta.validation.constraints.*;

public record ReportRequest(
        @NotNull ElectricityStatus status,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
        @Size(max = 500) String description
) { }