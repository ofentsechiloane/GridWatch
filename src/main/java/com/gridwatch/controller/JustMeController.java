package com.gridwatch.controller;

import com.gridwatch.dto.JustMeResponse;
import com.gridwatch.service.JustMeService;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/outages")
public class JustMeController {

    private final JustMeService service;

    public JustMeController(JustMeService service) {
        this.service = service;
    }

    @GetMapping("/just-me")
    public JustMeResponse justMe(
            @RequestParam @DecimalMin("-90.0") @DecimalMax("90.0") double lat,
            @RequestParam @DecimalMin("-180.0") @DecimalMax("180.0") double lon,
            @RequestParam(defaultValue = "1000") @Min(1) @Max(50000) double radius,
            @RequestParam(defaultValue = "30") @Min(1) @Max(1440) int minutes) {
        return service.check(lat, lon, radius, minutes);
    }
}