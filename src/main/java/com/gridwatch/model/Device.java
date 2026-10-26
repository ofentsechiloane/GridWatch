package com.gridwatch.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "devices", indexes = {
        @Index(name = "idx_devices_key_hash", columnList = "apiKeyHash", unique = true)
})
public class Device {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100) private String name;
    @Column(nullable = false) private double latitude;
    @Column(nullable = false) private double longitude;

    @Column(nullable = false, length = 64) private String apiKeyHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeviceStatus status = DeviceStatus.ACTIVE;

    @Column(nullable = false, updatable = false) private Instant createdAt;
    private Instant lastSeenAt;

    protected Device() { }

    public Device(String name, double latitude, double longitude, String apiKeyHash) {
        this.name = name; this.latitude = latitude;
        this.longitude = longitude; this.apiKeyHash = apiKeyHash;
    }

    @PrePersist
    void onCreate() { if (createdAt == null) createdAt = Instant.now(); }

    public void touch() { this.lastSeenAt = Instant.now(); }
    public void disable() { this.status = DeviceStatus.DISABLED; }

    public Long getId() { return id; }
    public String getName() { return name; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getApiKeyHash() { return apiKeyHash; }
    public DeviceStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastSeenAt() { return lastSeenAt; }
}