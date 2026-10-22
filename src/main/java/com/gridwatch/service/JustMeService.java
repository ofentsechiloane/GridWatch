package com.gridwatch.service;

import com.gridwatch.dto.JustMeResponse;
import com.gridwatch.dto.JustMeResponse.Verdict;
import com.gridwatch.model.ElectricityReport;
import com.gridwatch.model.ElectricityStatus;
import com.gridwatch.model.Outage;
import com.gridwatch.model.OutageStatus;
import com.gridwatch.repository.ElectricityReportRepository;
import com.gridwatch.repository.OutageRepository;
import com.gridwatch.util.GeoUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Answers "is this just my house, or is the whole area down?" for a location. */
@Service
public class JustMeService {

    private final ElectricityReportRepository reports;
    private final OutageRepository outages;

    public JustMeService(ElectricityReportRepository reports, OutageRepository outages) {
        this.reports = reports;
        this.outages = outages;
    }

    @Transactional(readOnly = true)
    public JustMeResponse check(double lat, double lon, double radiusMeters, int minutes) {
        Instant since = Instant.now().minus(Duration.ofMinutes(minutes));
        var box = GeoUtils.boundingBox(lat, lon, radiusMeters);

        List<ElectricityReport> nearby = reports
                .findInBoundingBox(since, box.minLat(), box.maxLat(), box.minLon(), box.maxLon())
                .stream()
                .filter(r -> GeoUtils.haversineMeters(lat, lon, r.getLatitude(), r.getLongitude()) <= radiusMeters)
                .toList();

        long off = nearby.stream()
                .filter(r -> r.getStatus() == ElectricityStatus.OFF || r.getStatus() == ElectricityStatus.UNSTABLE)
                .count();
        long on = nearby.stream()
                .filter(r -> r.getStatus() == ElectricityStatus.ON || r.getStatus() == ElectricityStatus.RESTORED)
                .count();

        Optional<Outage> outage = outages.findByStatusNot(OutageStatus.RESTORED).stream()
                .filter(o -> GeoUtils.haversineMeters(lat, lon, o.getCenterLatitude(), o.getCenterLongitude())
                        <= o.getRadiusMeters() + radiusMeters)
                .findFirst();

        if (outage.isPresent()) {
            return new JustMeResponse(Verdict.OUTAGE_IN_AREA,
                    "Your neighbours are reporting an outage. It is not just you.",
                    off, on, radiusMeters, minutes, outage.get().getId());
        }
        if (on > 0) {
            return new JustMeResponse(Verdict.LIKELY_JUST_YOU,
                    "Nearby households report power is on. The problem is probably at your property.",
                    off, on, radiusMeters, minutes, null);
        }
        return new JustMeResponse(Verdict.NOT_ENOUGH_DATA,
                "Not enough reports nearby to tell. Submit a report to help your area.",
                off, on, radiusMeters, minutes, null);
    }
}