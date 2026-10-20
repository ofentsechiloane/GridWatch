package com.gridwatch.service;

import com.gridwatch.model.ElectricityReport;
import com.gridwatch.model.ElectricityStatus;
import com.gridwatch.model.Outage;
import com.gridwatch.model.OutageStatus;
import com.gridwatch.repository.ElectricityReportRepository;
import com.gridwatch.repository.OutageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OutageDetectionServiceTest {

    private static final double LAT = -26.2041;
    private static final double LON = 28.0473;

    private ElectricityReportRepository reports;
    private OutageRepository outages;
    private OutageDetectionService service;

    @BeforeEach
    void setUp() {
        reports = mock(ElectricityReportRepository.class);
        outages = mock(OutageRepository.class);
        when(outages.findByStatusNot(OutageStatus.RESTORED)).thenReturn(List.of());
        when(outages.save(any(Outage.class))).thenAnswer(i -> i.getArgument(0));
        // radius 1000m, window 30min, possible 2, likely 3, confirmed 5, restored 2, stale 180min
        service = new OutageDetectionService(reports, outages, 1000, 30, 2, 3, 5, 2, 180);
    }

    private List<ElectricityReport> reportsWith(ElectricityStatus status, int n) {
        List<ElectricityReport> list = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            list.add(new ElectricityReport(status, LAT, LON, null));
        }
        return list;
    }

    private void dbReturns(List<ElectricityReport> list) {
        when(reports.findInBoundingBox(any(Instant.class),
                anyDouble(), anyDouble(), anyDouble(), anyDouble())).thenReturn(list);
    }

    @Test
    void singleReport_createsNoOutage() {
        List<ElectricityReport> list = reportsWith(ElectricityStatus.OFF, 1);
        dbReturns(list);

        assertTrue(service.evaluate(list.get(0)).isEmpty());
    }

    @Test
    void twoReports_createsPossibleOutage() {
        List<ElectricityReport> list = reportsWith(ElectricityStatus.OFF, 2);
        dbReturns(list);

        Outage o = service.evaluate(list.get(0)).orElseThrow();
        assertEquals(OutageStatus.POSSIBLE, o.getStatus());
        assertEquals(2, o.getReportCount());
    }

    @Test
    void threeReports_createsLikelyOutage() {
        List<ElectricityReport> list = reportsWith(ElectricityStatus.OFF, 3);
        dbReturns(list);

        Outage o = service.evaluate(list.get(0)).orElseThrow();
        assertEquals(OutageStatus.LIKELY, o.getStatus());
        assertEquals(0.6, o.getConfidence(), 1e-9);
    }

    @Test
    void fiveReports_createsConfirmedOutageWithFullConfidence() {
        List<ElectricityReport> list = reportsWith(ElectricityStatus.OFF, 5);
        dbReturns(list);

        Outage o = service.evaluate(list.get(0)).orElseThrow();
        assertEquals(OutageStatus.CONFIRMED, o.getStatus());
        assertEquals(1.0, o.getConfidence(), 1e-9);
    }

    @Test
    void unstableReportsCountTowardsOutage() {
        List<ElectricityReport> list = reportsWith(ElectricityStatus.UNSTABLE, 2);
        dbReturns(list);

        assertTrue(service.evaluate(list.get(0)).isPresent());
    }

    @Test
    void enoughRestoredReports_closeActiveOutage() {
        Outage active = new Outage(LAT, LON, 500, 3, 0.6, OutageStatus.LIKELY);
        when(outages.findByStatusNot(OutageStatus.RESTORED)).thenReturn(List.of(active));
        List<ElectricityReport> list = reportsWith(ElectricityStatus.RESTORED, 2);
        dbReturns(list);

        Optional<Outage> result = service.evaluate(list.get(0));

        assertTrue(result.isPresent());
        assertEquals(OutageStatus.RESTORED, result.get().getStatus());
        assertNotNull(result.get().getRestoredAt());
    }

    @Test
    void singleRestoredReport_doesNotCloseOutage() {
        Outage active = new Outage(LAT, LON, 500, 3, 0.6, OutageStatus.LIKELY);
        when(outages.findByStatusNot(OutageStatus.RESTORED)).thenReturn(List.of(active));
        List<ElectricityReport> list = reportsWith(ElectricityStatus.RESTORED, 1);
        dbReturns(list);

        assertTrue(service.evaluate(list.get(0)).isEmpty());
        assertEquals(OutageStatus.LIKELY, active.getStatus());
    }

    @Test
    void restoredReportWithNoActiveOutage_doesNothing() {
        List<ElectricityReport> list = reportsWith(ElectricityStatus.RESTORED, 2);
        dbReturns(list);

        assertTrue(service.evaluate(list.get(0)).isEmpty());
    }

    @Test
    void expireStale_closesOnlyOutagesWithNoRecentActivity() {
        Outage active = new Outage(LAT, LON, 500, 3, 0.6, OutageStatus.LIKELY);
        when(outages.findByStatusNot(OutageStatus.RESTORED)).thenReturn(List.of(active));

        assertEquals(0, service.expireStale(Instant.now()));
        assertEquals(OutageStatus.LIKELY, active.getStatus());

        assertEquals(1, service.expireStale(Instant.now().plus(Duration.ofMinutes(181))));
        assertEquals(OutageStatus.RESTORED, active.getStatus());
    }
}