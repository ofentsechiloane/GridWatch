package com.gridwatch.controller;

import com.gridwatch.dto.DeviceEventRequest;
import com.gridwatch.dto.DeviceRegistrationResponse;
import com.gridwatch.dto.DeviceRequest;
import com.gridwatch.dto.DeviceResponse;
import com.gridwatch.dto.ReportResponse;
import com.gridwatch.service.DeviceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    private final DeviceService service;

    public DeviceController(DeviceService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DeviceRegistrationResponse register(@Valid @RequestBody DeviceRequest request) {
        return service.register(request);
    }

    @PostMapping("/events")
    @ResponseStatus(HttpStatus.CREATED)
    public ReportResponse event(@RequestHeader(value = "X-API-Key", required = false) String apiKey,
                                @Valid @RequestBody DeviceEventRequest request) {
        return service.recordEvent(apiKey, request.status());
    }

    @GetMapping
    public List<DeviceResponse> list() {
        return service.list();
    }
}