package com.gridwatch.dto;

import com.gridwatch.model.ElectricityReport;
import com.gridwatch.model.ElectricityStatus;
import java.time.Instant;

public record ReportResponse(Long id, ElectricityStatus status, double latitude, double longitude,
                             String description, Instant createdAt, Double distanceMeters) {
    public static ReportResponse from(ElectricityReport r, Double distanceMeters) {
        return new ReportResponse(r.getId(), r.getStatus(), r.getLatitude(), r.getLongitude(),
                r.getDescription(), r.getCreatedAt(), distanceMeters);
    }
}