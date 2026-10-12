package com.gridwatch.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "electricity_reports", indexes = {
        @Index(name = "idx_reports_created_at", columnList = "createdAt"),
        @Index(name = "idx_reports_lat_lon", columnList = "latitude, longitude")
})
public class ElectricityReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ElectricityStatus status;

    @Column(nullable = false) private double latitude;
    @Column(nullable = false) private double longitude;
    @Column(length = 500) private String description;
    @Column(nullable = false, updatable = false) private Instant createdAt;

    protected ElectricityReport() { }

    public ElectricityReport(ElectricityStatus status, double latitude, double longitude, String description) {
        this.status = status; this.latitude = latitude;
        this.longitude = longitude; this.description = description;
    }

    @PrePersist
    void onCreate() { if (createdAt == null) createdAt = Instant.now(); }

    public Long getId() { return id; }
    public ElectricityStatus getStatus() { return status; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getDescription() { return description; }
    public Instant getCreatedAt() { return createdAt; }
}
