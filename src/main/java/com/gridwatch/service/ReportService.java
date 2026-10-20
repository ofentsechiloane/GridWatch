package com.gridwatch.service;

import com.gridwatch.dto.ReportRequest;
import com.gridwatch.dto.ReportResponse;
import com.gridwatch.model.ElectricityReport;
import com.gridwatch.repository.ElectricityReportRepository;
import com.gridwatch.util.GeoUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class ReportService {

    private final ElectricityReportRepository repository;
    private final OutageDetectionService detectionService;

    public ReportService(ElectricityReportRepository repository,
                         OutageDetectionService detectionService) {
        this.repository = repository;
        this.detectionService = detectionService;
    }

    @Transactional
    public ReportResponse create(ReportRequest req) {
        ElectricityReport saved = repository.save(new ElectricityReport(
                req.status(), req.latitude(), req.longitude(), req.description()));
        detectionService.evaluate(saved);
        return ReportResponse.from(saved, null);
    }

    @Transactional(readOnly = true)
    public List<ReportResponse> findNearby(double lat, double lon, double radiusMeters, int sinceMinutes) {
        double latDelta = Math.toDegrees(radiusMeters / GeoUtils.EARTH_RADIUS_M);
        double lonDelta = latDelta / Math.max(Math.cos(Math.toRadians(lat)), 0.01);
        Instant since = Instant.now().minus(sinceMinutes, ChronoUnit.MINUTES);

        return repository.findInBoundingBox(since,
                        lat - latDelta, lat + latDelta, lon - lonDelta, lon + lonDelta)
                .stream()
                .map(r -> ReportResponse.from(r,
                        GeoUtils.haversineMeters(lat, lon, r.getLatitude(), r.getLongitude())))
                .filter(r -> r.distanceMeters() <= radiusMeters)
                .toList();
    }
}