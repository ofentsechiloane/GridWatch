package com.gridwatch.dto;

import com.gridwatch.model.ElectricityStatus;
import jakarta.validation.constraints.NotNull;

public record DeviceEventRequest(@NotNull ElectricityStatus status) { }