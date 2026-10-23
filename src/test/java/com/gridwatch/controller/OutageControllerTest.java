package com.gridwatch.controller;

import com.gridwatch.config.SecurityConfig;
import com.gridwatch.dto.OutageResponse;
import com.gridwatch.exception.ResourceNotFoundException;
import com.gridwatch.model.OutageStatus;
import com.gridwatch.service.OutageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OutageController.class)
@Import(SecurityConfig.class)
class OutageControllerTest {

    @Autowired MockMvc mvc;
    @MockitoBean OutageService service;

    private static OutageResponse sample() {
        return new OutageResponse(7L, OutageStatus.LIKELY, -26.2041, 28.0473,
                500.0, 3, 0.6, Instant.now(), Instant.now(), null);
    }

    @Test
    void activeReturnsOutages() throws Exception {
        when(service.findActive()).thenReturn(List.of(sample()));

        mvc.perform(get("/api/outages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].status").value("LIKELY"))
                .andExpect(jsonPath("$[0].reportCount").value(3));
    }

    @Test
    void activeReturnsEmptyListWhenNoOutages() throws Exception {
        when(service.findActive()).thenReturn(List.of());

        mvc.perform(get("/api/outages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getByIdReturnsOutage() throws Exception {
        when(service.findById(7L)).thenReturn(sample());

        mvc.perform(get("/api/outages/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.confidence").value(0.6));
    }

    @Test
    void getByIdReturns404ForUnknownOutage() throws Exception {
        when(service.findById(99L)).thenThrow(new ResourceNotFoundException("Outage 99 not found"));

        mvc.perform(get("/api/outages/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Outage 99 not found"));
    }

    @Test
    void getByIdRejectsNonNumericId() throws Exception {
        mvc.perform(get("/api/outages/abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nearbyUsesDefaultRadius() throws Exception {
        when(service.findNearby(-26.2041, 28.0473, 2000.0)).thenReturn(List.of(sample()));

        mvc.perform(get("/api/outages/nearby").param("lat", "-26.2041").param("lon", "28.0473"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(7));

        verify(service).findNearby(-26.2041, 28.0473, 2000.0);
    }

    @Test
    void nearbyRejectsOutOfRangeLat() throws Exception {
        mvc.perform(get("/api/outages/nearby").param("lat", "999").param("lon", "28"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.lat").exists());
    }

    @Test
    void nearbyRejectsZeroRadius() throws Exception {
        mvc.perform(get("/api/outages/nearby")
                        .param("lat", "-26.2").param("lon", "28.0").param("radius", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.radius").exists());
    }

    @Test
    void nearbyRejectsMissingLon() throws Exception {
        mvc.perform(get("/api/outages/nearby").param("lat", "-26.2"))
                .andExpect(status().isBadRequest());
    }
}
