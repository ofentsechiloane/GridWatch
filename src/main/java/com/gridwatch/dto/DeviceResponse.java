package com.gridwatch.dto;

import com.gridwatch.model.Device;
import com.gridwatch.model.DeviceStatus;
import java.time.Instant;

public record DeviceResponse(Long id, String name, double latitude, double longitude,
                             DeviceStatus status, Instant createdAt, Instant lastSeenAt,
                             String apiKey) {
    public static DeviceResponse from(Device d, String apiKey) {
        return new DeviceResponse(d.getId(), d.getName(), d.getLatitude(), d.getLongitude(),
                d.getStatus(), d.getCreatedAt(), d.getLastSeenAt(), apiKey);
    }
}