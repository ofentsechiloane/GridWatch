package com.gridwatch.controller;

import com.gridwatch.dto.OutageResponse;
import com.gridwatch.service.OutageService;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/outages")
public class OutageController {

    private final OutageService service;

    public OutageController(OutageService service) {
        this.service = service;
    }

    @GetMapping
    public List<OutageResponse> active() {
        return service.findActive();
    }

    @GetMapping("/{id}")
    public OutageResponse get(@PathVariable Long id) {
        return service.findById(id);
    }

    @GetMapping("/nearby")
    public List<OutageResponse> nearby(
            @RequestParam @DecimalMin("-90.0") @DecimalMax("90.0") double lat,
            @RequestParam @DecimalMin("-180.0") @DecimalMax("180.0") double lon,
            @RequestParam(defaultValue = "2000") @Min(1) @Max(50000) double radius) {
        return service.findNearby(lat, lon, radius);
    }
}