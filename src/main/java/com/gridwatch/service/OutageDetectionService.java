package com.gridwatch.service;

import com.gridwatch.model.ElectricityReport;
import com.gridwatch.model.ElectricityStatus;
import com.gridwatch.model.Outage;
import com.gridwatch.model.OutageStatus;
import com.gridwatch.repository.ElectricityReportRepository;
import com.gridwatch.repository.OutageRepository;
import com.gridwatch.util.GeoUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Rule-based outage detection (Phase 3).
 * Call {@link #evaluate} after a report is saved; the scheduled job calls {@link #expireStale}.
 */
@Service
public class OutageDetectionService {

    private final ElectricityReportRepository reports;
    private final OutageRepository outages;

    private final double clusterRadiusMeters;
    private final int windowMinutes;
    private final int minReportsPossible;
    private final int minReportsLikely;
    private final int minReportsConfirmed;
    private final int minRestoredReports;
    private final long staleAfterMinutes;

    public OutageDetectionService(
            ElectricityReportRepository reports,
            OutageRepository outages,
            @Value("${gridwatch.detection.radius-meters:1000}") double clusterRadiusMeters,
            @Value("${gridwatch.detection.window-minutes:30}") int windowMinutes,
            @Value("${gridwatch.detection.min-possible:2}") int minReportsPossible,
            @Value("${gridwatch.detection.min-likely:3}") int minReportsLikely,
            @Value("${gridwatch.detection.min-confirmed:5}") int minReportsConfirmed,
            @Value("${gridwatch.detection.min-restored:2}") int minRestoredReports,
            @Value("${gridwatch.expiry.stale-after-minutes:180}") long staleAfterMinutes) {
        this.reports = reports;
        this.outages = outages;
        this.clusterRadiusMeters = clusterRadiusMeters;
        this.windowMinutes = windowMinutes;
        this.minReportsPossible = minReportsPossible;
        this.minReportsLikely = minReportsLikely;
        this.minReportsConfirmed = minReportsConfirmed;
        this.minRestoredReports = minRestoredReports;
        this.staleAfterMinutes = staleAfterMinutes;
    }

    /** Looks at the area around a new report and creates, updates or closes an outage. */
    @Transactional
    public Optional<Outage> evaluate(ElectricityReport report) {
        return switch (report.getStatus()) {
            case OFF, UNSTABLE -> handleOutageReport(report);
            case RESTORED, ON -> handleRestoredReport(report);
        };
    }

    /** Marks outages with no new activity for a while as restored. Returns how many were closed. */
    @Transactional
    public int expireStale(Instant now) {
        Instant cutoff = now.minus(Duration.ofMinutes(staleAfterMinutes));
        int closed = 0;
        for (Outage o : outages.findByStatusNot(OutageStatus.RESTORED)) {
            if (o.getUpdatedAt().isBefore(cutoff)) {
                o.markRestored();
                closed++;
            }
        }
        return closed;
    }

    // ---- OFF / UNSTABLE ----

    private Optional<Outage> handleOutageReport(ElectricityReport report) {
        Optional<Instant> lastRestored = lastRestorationNear(report);
        List<ElectricityReport> cluster = nearbyReports(report,
                ElectricityStatus.OFF, ElectricityStatus.UNSTABLE).stream()
                .filter(r -> lastRestored
                        .map(t -> r.getCreatedAt() != null && r.getCreatedAt().isAfter(t))
                        .orElse(true))
                .toList();
        if (cluster.size() < minReportsPossible) {
            return Optional.empty();
        }

        double lat = cluster.stream().mapToDouble(ElectricityReport::getLatitude).average().orElseThrow();
        double lon = cluster.stream().mapToDouble(ElectricityReport::getLongitude).average().orElseThrow();
        double radius = Math.max(clusterRadiusMeters / 2, cluster.stream()
                .mapToDouble(r -> GeoUtils.haversineMeters(lat, lon, r.getLatitude(), r.getLongitude()))
                .max().orElse(0));
        int count = cluster.size();
        double confidence = confidence(count);
        OutageStatus status = classify(count);

        Outage outage = findActiveOutageNear(report)
                .map(existing -> { existing.update(lat, lon, radius, count, confidence, status); return existing; })
                .orElseGet(() -> new Outage(lat, lon, radius, count, confidence, status));
        return Optional.of(outages.save(outage));
    }

    // ---- RESTORED / ON ----

    private Optional<Outage> handleRestoredReport(ElectricityReport report) {
        Optional<Outage> active = findActiveOutageNear(report);
        if (active.isEmpty()) {
            return Optional.empty();
        }
        List<ElectricityReport> restored = nearbyReports(report,
                ElectricityStatus.RESTORED, ElectricityStatus.ON);
        if (restored.size() >= minRestoredReports) {
            Outage outage = active.get();
            outage.markRestored();
            return Optional.of(outages.save(outage));
        }
        return Optional.empty();
    }

    // ---- helpers ----

    private List<ElectricityReport> nearbyReports(ElectricityReport centre, ElectricityStatus... statuses) {
        Instant since = Instant.now().minus(Duration.ofMinutes(windowMinutes));
        var box = GeoUtils.boundingBox(centre.getLatitude(), centre.getLongitude(), clusterRadiusMeters);
        List<ElectricityStatus> wanted = List.of(statuses);
        return reports.findInBoundingBox(since, box.minLat(), box.maxLat(), box.minLon(), box.maxLon())
                .stream()
                .filter(r -> wanted.contains(r.getStatus()))
                .filter(r -> GeoUtils.haversineMeters(centre.getLatitude(), centre.getLongitude(),
                        r.getLatitude(), r.getLongitude()) <= clusterRadiusMeters)
                .toList();
    }

    private Optional<Outage> findActiveOutageNear(ElectricityReport report) {
        return outages.findByStatusNot(OutageStatus.RESTORED).stream()
                .filter(o -> GeoUtils.haversineMeters(report.getLatitude(), report.getLongitude(),
                        o.getCenterLatitude(), o.getCenterLongitude()) <= o.getRadiusMeters() + clusterRadiusMeters)
                .findFirst();
    }

    /** Time of the most recent restoration near this report, inside the detection window. */
    private Optional<Instant> lastRestorationNear(ElectricityReport report) {
        Instant since = Instant.now().minus(Duration.ofMinutes(windowMinutes));
        return outages.findByStatusAndRestoredAtAfter(OutageStatus.RESTORED, since).stream()
                .filter(o -> GeoUtils.haversineMeters(report.getLatitude(), report.getLongitude(),
                        o.getCenterLatitude(), o.getCenterLongitude())
                        <= o.getRadiusMeters() + clusterRadiusMeters)
                .map(Outage::getRestoredAt)
                .max(Comparator.naturalOrder());
    }

    private OutageStatus classify(int count) {
        if (count >= minReportsConfirmed) return OutageStatus.CONFIRMED;
        if (count >= minReportsLikely) return OutageStatus.LIKELY;
        return OutageStatus.POSSIBLE;
    }

    /** Simple score: grows with report count and reaches 1.0 at the "confirmed" threshold. */
    private double confidence(int count) {
        return Math.min(1.0, (double) count / minReportsConfirmed);
    }
}