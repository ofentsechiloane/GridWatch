package com.gridwatch.service;

import com.gridwatch.dto.JustMeResponse;
import com.gridwatch.dto.JustMeResponse.Verdict;
import com.gridwatch.model.ElectricityReport;
import com.gridwatch.model.ElectricityStatus;
import com.gridwatch.model.Outage;
import com.gridwatch.model.OutageStatus;
import com.gridwatch.repository.ElectricityReportRepository;
import com.gridwatch.repository.OutageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JustMeServiceTest {

    private static final double LAT = -26.2041;
    private static final double LON = 28.0473;

    private ElectricityReportRepository reports;
    private OutageRepository outages;
    private JustMeService service;

    @BeforeEach
    void setUp() {
        reports = mock(ElectricityReportRepository.class);
        outages = mock(OutageRepository.class);
        when(outages.findByStatusNot(OutageStatus.RESTORED)).thenReturn(List.of());
        service = new JustMeService(reports, outages);
    }

    private void dbReturns(List<ElectricityReport> list) {
        when(reports.findInBoundingBox(any(Instant.class),
                anyDouble(), anyDouble(), anyDouble(), anyDouble())).thenReturn(list);
    }

    @Test
    void activeOutageNearby_saysOutageInArea() {
        when(outages.findByStatusNot(OutageStatus.RESTORED))
                .thenReturn(List.of(new Outage(LAT, LON, 500, 3, 0.6, OutageStatus.LIKELY)));
        dbReturns(List.of());

        JustMeResponse r = service.check(LAT, LON, 1000, 30);
        assertEquals(Verdict.OUTAGE_IN_AREA, r.verdict());
    }

    @Test
    void neighboursHavePower_saysLikelyJustYou() {
        dbReturns(List.of(
                new ElectricityReport(ElectricityStatus.ON, LAT, LON, null),
                new ElectricityReport(ElectricityStatus.ON, LAT, LON, null)));

        JustMeResponse r = service.check(LAT, LON, 1000, 30);
        assertEquals(Verdict.LIKELY_JUST_YOU, r.verdict());
        assertEquals(2, r.onReports());
    }

    @Test
    void noReports_saysNotEnoughData() {
        dbReturns(List.of());

        JustMeResponse r = service.check(LAT, LON, 1000, 30);
        assertEquals(Verdict.NOT_ENOUGH_DATA, r.verdict());
    }
}