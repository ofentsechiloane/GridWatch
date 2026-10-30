package com.gridwatch.service;

import com.gridwatch.dto.ReportRequest;
import com.gridwatch.exception.UnauthorizedException;
import com.gridwatch.model.Device;
import com.gridwatch.model.ElectricityStatus;
import com.gridwatch.repository.DeviceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceServiceTest {

    @Mock DeviceRepository repository;
    @Mock ReportService reportService;
    @InjectMocks DeviceService service;

    @Test
    void validKeyCreatesReportAtDeviceLocationAndMarksSeen() {
        Device device = new Device("Test", -26.2, 28.04, DeviceService.hash("gw_key"));
        when(repository.findByApiKeyHash(DeviceService.hash("gw_key"))).thenReturn(Optional.of(device));

        service.recordEvent("gw_key", ElectricityStatus.OFF);

        verify(reportService).create(new ReportRequest(ElectricityStatus.OFF, -26.2, 28.04, "device:Test"));
        assertNotNull(device.getLastSeenAt());
    }

    @Test
    void unknownKeyIsRejected() {
        when(repository.findByApiKeyHash(any())).thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class,
                () -> service.recordEvent("gw_bad", ElectricityStatus.OFF));
        verifyNoInteractions(reportService);
    }

    @Test
    void missingKeyIsRejected() {
        assertThrows(UnauthorizedException.class,
                () -> service.recordEvent(null, ElectricityStatus.OFF));
        verifyNoInteractions(repository, reportService);
    }
}