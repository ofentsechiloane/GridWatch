package com.gridwatch.controller;

import com.gridwatch.config.SecurityConfig;
import com.gridwatch.dto.JustMeResponse;
import com.gridwatch.dto.JustMeResponse.Verdict;
import com.gridwatch.service.JustMeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(JustMeController.class)
@Import(SecurityConfig.class)
class JustMeControllerTest {

    @Autowired MockMvc mvc;
    @MockitoBean JustMeService service;

    @Test
    void usesDefaultRadiusAndMinutes() throws Exception {
        when(service.check(-26.2041, 28.0473, 1000.0, 30))
                .thenReturn(new JustMeResponse(Verdict.NOT_ENOUGH_DATA, "Not enough reports",
                        0, 0, 1000.0, 30, null));

        mvc.perform(get("/api/outages/just-me").param("lat", "-26.2041").param("lon", "28.0473"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("NOT_ENOUGH_DATA"))
                .andExpect(jsonPath("$.radiusMeters").value(1000.0))
                .andExpect(jsonPath("$.minutes").value(30));

        verify(service).check(-26.2041, 28.0473, 1000.0, 30);
    }

    @Test
    void returnsOutageInAreaWithOutageId() throws Exception {
        when(service.check(-26.2041, 28.0473, 1000.0, 30))
                .thenReturn(new JustMeResponse(Verdict.OUTAGE_IN_AREA, "It is not just you.",
                        3, 0, 1000.0, 30, 7L));

        mvc.perform(get("/api/outages/just-me").param("lat", "-26.2041").param("lon", "28.0473"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("OUTAGE_IN_AREA"))
                .andExpect(jsonPath("$.offReports").value(3))
                .andExpect(jsonPath("$.outageId").value(7));
    }

    @Test
    void passesCustomRadiusAndMinutes() throws Exception {
        when(service.check(-26.2041, 28.0473, 5000.0, 120))
                .thenReturn(new JustMeResponse(Verdict.LIKELY_JUST_YOU, "Probably just you.",
                        0, 2, 5000.0, 120, null));

        mvc.perform(get("/api/outages/just-me")
                        .param("lat", "-26.2041").param("lon", "28.0473")
                        .param("radius", "5000").param("minutes", "120"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("LIKELY_JUST_YOU"));

        verify(service).check(-26.2041, 28.0473, 5000.0, 120);
    }

    @Test
    void rejectsOutOfRangeLatitude() throws Exception {
        mvc.perform(get("/api/outages/just-me").param("lat", "999").param("lon", "28"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.lat").exists());
    }

    @Test
    void rejectsOversizedRadius() throws Exception {
        mvc.perform(get("/api/outages/just-me")
                        .param("lat", "-26.2").param("lon", "28.0").param("radius", "50001"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.radius").exists());
    }

    @Test
    void rejectsZeroMinutes() throws Exception {
        mvc.perform(get("/api/outages/just-me")
                        .param("lat", "-26.2").param("lon", "28.0").param("minutes", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.minutes").exists());
    }

    @Test
    void rejectsMissingLat() throws Exception {
        mvc.perform(get("/api/outages/just-me").param("lon", "28"))
                .andExpect(status().isBadRequest());
    }
}