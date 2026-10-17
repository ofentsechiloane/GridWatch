package com.gridwatch.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "outages")
public class Outage {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutageStatus status;

    @Column(nullable = false) private double centerLatitude;
    @Column(nullable = false) private double centerLongitude;
    @Column(nullable = false) private double radiusMeters;
    @Column(nullable = false) private int reportCount;
    @Column(nullable = false) private double confidence;
    @Column(nullable = false, updatable = false) private Instant startedAt;
    @Column(nullable = false) private Instant updatedAt;
    private Instant restoredAt;

    protected Outage() { }

    public Outage(double lat, double lon, double radius, int count, double confidence, OutageStatus status) {
        this.centerLatitude = lat; this.centerLongitude = lon; this.radiusMeters = radius;
        this.reportCount = count; this.confidence = confidence; this.status = status;
        this.startedAt = this.updatedAt = Instant.now();
    }

    public void update(double lat, double lon, double radius, int count, double confidence, OutageStatus status) {
        this.centerLatitude = lat; this.centerLongitude = lon; this.radiusMeters = radius;
        this.reportCount = count; this.confidence = confidence; this.status = status;
        this.updatedAt = Instant.now();
    }

    public void markRestored() {
        this.status = OutageStatus.RESTORED;
        this.restoredAt = this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public OutageStatus getStatus() { return status; }
    public double getCenterLatitude() { return centerLatitude; }
    public double getCenterLongitude() { return centerLongitude; }
    public double getRadiusMeters() { return radiusMeters; }
    public int getReportCount() { return reportCount; }
    public double getConfidence() { return confidence; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getRestoredAt() { return restoredAt; }
}