package com.gridwatch.dto;

import com.gridwatch.model.Outage;
import com.gridwatch.model.OutageStatus;
import java.time.Instant;

public record OutageResponse(Long id, OutageStatus status, double centerLatitude,
                             double centerLongitude, double radiusMeters, int reportCount,
                             double confidence, Instant startedAt, Instant updatedAt,
                             Instant restoredAt) {
    public static OutageResponse from(Outage o) {
        return new OutageResponse(o.getId(), o.getStatus(), o.getCenterLatitude(),
                o.getCenterLongitude(), o.getRadiusMeters(), o.getReportCount(),
                o.getConfidence(), o.getStartedAt(), o.getUpdatedAt(), o.getRestoredAt());
    }
}