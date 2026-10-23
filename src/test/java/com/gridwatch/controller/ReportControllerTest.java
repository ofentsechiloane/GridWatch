package com.gridwatch.controller;

import com.gridwatch.config.SecurityConfig;
import com.gridwatch.dto.ReportRequest;
import com.gridwatch.dto.ReportResponse;
import com.gridwatch.model.ElectricityStatus;
import com.gridwatch.service.ReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReportController.class)
@Import(SecurityConfig.class)
class ReportControllerTest {

    @Autowired MockMvc mvc;
    @MockitoBean ReportService service;

    private static ReportResponse sample() {
        return new ReportResponse(1L, ElectricityStatus.OFF, -26.2041, 28.0473, "out", Instant.now(), null);
    }

    @Test
    void createReturns201() throws Exception {
        when(service.create(any(ReportRequest.class))).thenReturn(sample());

        mvc.perform(post("/api/reports").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"OFF\",\"latitude\":-26.2041,\"longitude\":28.0473}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void createRejectsOutOfRangeLatitude() throws Exception {
        mvc.perform(post("/api/reports").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"OFF\",\"latitude\":999,\"longitude\":28.0473}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.latitude").exists());
    }

    @Test
    void createRejectsMissingStatus() throws Exception {
        mvc.perform(post("/api/reports").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"latitude\":-26.2,\"longitude\":28.0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.status").exists());
    }

    @Test
    void createRejectsUnknownStatus() throws Exception {
        mvc.perform(post("/api/reports").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"BANANA\",\"latitude\":-26.2,\"longitude\":28.0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed request body"));
    }

    @Test
    void historyUsesDefaults() throws Exception {
        when(service.history(0, 20)).thenReturn(List.of(sample()));

        mvc.perform(get("/api/reports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));

        verify(service).history(0, 20);
    }

    @Test
    void historyPassesPageAndSize() throws Exception {
        when(service.history(2, 50)).thenReturn(List.of());

        mvc.perform(get("/api/reports").param("page", "2").param("size", "50"))
                .andExpect(status().isOk());

        verify(service).history(2, 50);
    }

    @Test
    void historyRejectsNegativePage() throws Exception {
        mvc.perform(get("/api/reports").param("page", "-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void historyRejectsOversizedPage() throws Exception {
        mvc.perform(get("/api/reports").param("size", "101"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nearbyUsesDefaults() throws Exception {
        when(service.findNearby(-26.2041, 28.0473, 2000.0, 60)).thenReturn(List.of(sample()));

        mvc.perform(get("/api/reports/nearby").param("lat", "-26.2041").param("lon", "28.0473"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));

        verify(service).findNearby(-26.2041, 28.0473, 2000.0, 60);
    }

    @Test
    void nearbyRejectsOutOfRangeLat() throws Exception {
        mvc.perform(get("/api/reports/nearby").param("lat", "999").param("lon", "28"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.lat").exists());
    }

    @Test
    void nearbyRejectsMissingLat() throws Exception {
        mvc.perform(get("/api/reports/nearby").param("lon", "28"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nearbyRejectsNonNumericLat() throws Exception {
        mvc.perform(get("/api/reports/nearby").param("lat", "abc").param("lon", "28"))
                .andExpect(status().isBadRequest());
    }
}