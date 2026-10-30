package com.gridwatch.dto;

import com.gridwatch.model.Device;
import java.time.Instant;

public record DeviceResponse(Long id, String name, double latitude, double longitude,
                             Instant createdAt, Instant lastSeenAt) {
    public static DeviceResponse from(Device d) {
        return new DeviceResponse(d.getId(), d.getName(), d.getLatitude(), d.getLongitude(),
                d.getCreatedAt(), d.getLastSeenAt());
    }
}