package com.gridwatch.service;

import com.gridwatch.dto.ReportRequest;
import com.gridwatch.dto.ReportResponse;
import com.gridwatch.model.ElectricityReport;
import com.gridwatch.model.ElectricityStatus;
import com.gridwatch.repository.ElectricityReportRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    private static final double LAT = -26.2041;
    private static final double LON = 28.0473;

    @Mock ElectricityReportRepository repository;
    @Mock OutageDetectionService detectionService;
    @InjectMocks ReportService service;

    @Test
    void createSavesAndReturnsResponse() {
        when(repository.save(any(ElectricityReport.class))).thenAnswer(i -> i.getArgument(0));

        ReportResponse res = service.create(new ReportRequest(ElectricityStatus.OFF, LAT, LON, "out"));

        assertThat(res.status()).isEqualTo(ElectricityStatus.OFF);
        assertThat(res.latitude()).isEqualTo(LAT);
        assertThat(res.distanceMeters()).isNull();
        verify(detectionService).evaluate(any(ElectricityReport.class));
    }

    @Test
    void historyReturnsRequestedPageWithoutDistance() {
        ElectricityReport r = new ElectricityReport(ElectricityStatus.OFF, LAT, LON, "out");
        when(repository.findAllByOrderByCreatedAtDesc(PageRequest.of(1, 10)))
                .thenReturn(new PageImpl<>(List.of(r)));

        List<ReportResponse> result = service.history(1, 10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).status()).isEqualTo(ElectricityStatus.OFF);
        assertThat(result.get(0).distanceMeters()).isNull();
        verify(repository).findAllByOrderByCreatedAtDesc(PageRequest.of(1, 10));
    }

    @Test
    void historyReturnsEmptyListWhenNoReports() {
        when(repository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of()));

        assertThat(service.history(0, 20)).isEmpty();
    }

    @Test
    void findNearbyFiltersByExactRadius() {
        ElectricityReport same = new ElectricityReport(ElectricityStatus.OFF, LAT, LON, null);
        ElectricityReport near = new ElectricityReport(ElectricityStatus.OFF, LAT, LON + 0.01, null);  // ~998 m
        ElectricityReport far = new ElectricityReport(ElectricityStatus.OFF, LAT, LON + 0.05, null);   // ~4.99 km
        when(repository.findInBoundingBox(any(Instant.class), anyD(), anyD(), anyD(), anyD()))
                .thenReturn(List.of(same, near, far));

        List<ReportResponse> result = service.findNearby(LAT, LON, 2000, 60);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).distanceMeters()).isZero();
        assertThat(result.get(1).distanceMeters()).isCloseTo(998, within(5.0));
    }

    @Test
    void findNearbyBuildsBoundingBoxAroundPoint() {
        when(repository.findInBoundingBox(any(Instant.class), anyD(), anyD(), anyD(), anyD()))
                .thenReturn(List.of());

        service.findNearby(LAT, LON, 2000, 60);

        ArgumentCaptor<Double> minLat = ArgumentCaptor.forClass(Double.class);
        ArgumentCaptor<Double> maxLat = ArgumentCaptor.forClass(Double.class);
        ArgumentCaptor<Double> minLon = ArgumentCaptor.forClass(Double.class);
        ArgumentCaptor<Double> maxLon = ArgumentCaptor.forClass(Double.class);
        verify(repository).findInBoundingBox(any(Instant.class),
                minLat.capture(), maxLat.capture(), minLon.capture(), maxLon.capture());

        double latDelta = 2000 / 111_195.0;
        assertThat(minLat.getValue()).isCloseTo(LAT - latDelta, within(1e-4));
        assertThat(maxLat.getValue()).isCloseTo(LAT + latDelta, within(1e-4));
        assertThat(minLon.getValue()).isLessThan(LON);
        assertThat(maxLon.getValue()).isGreaterThan(LON);
    }

    private static double anyD() {
        return org.mockito.ArgumentMatchers.anyDouble();
    }
}