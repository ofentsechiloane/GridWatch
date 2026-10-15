package com.gridwatch.controller;

import com.gridwatch.dto.ReportRequest;
import com.gridwatch.dto.ReportResponse;
import com.gridwatch.service.ReportService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService service;

    public ReportController(ReportService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReportResponse create(@Valid @RequestBody ReportRequest request) {
        return service.create(request);
    }

    @GetMapping("/nearby")
    public List<ReportResponse> nearby(
            @RequestParam @DecimalMin("-90.0") @DecimalMax("90.0") double lat,
            @RequestParam @DecimalMin("-180.0") @DecimalMax("180.0") double lon,
            @RequestParam(defaultValue = "2000") @Min(1) @Max(50000) double radius,
            @RequestParam(defaultValue = "60") @Min(1) @Max(1440) int minutes) {
        return service.findNearby(lat, lon, radius, minutes);
    }
}
