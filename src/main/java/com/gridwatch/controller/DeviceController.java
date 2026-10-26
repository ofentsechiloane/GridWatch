package com.gridwatch.controller;

import com.gridwatch.dto.DeviceRequest;
import com.gridwatch.dto.DeviceResponse;
import com.gridwatch.service.DeviceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    private final DeviceService service;

    public DeviceController(DeviceService service) {
        this.service = service;
    }

    /** Register a device. The response contains its API key, shown only this once. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DeviceResponse register(@Valid @RequestBody DeviceRequest request) {
        return service.register(request);
    }

    @GetMapping
    public List<DeviceResponse> list() {
        return service.list();
    }
}