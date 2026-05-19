package com.lastmile.optiroute.dto;

import com.lastmile.optiroute.domain.enums.DeliveryStatus;
import jakarta.validation.constraints.NotNull;

public record StatusUpdateRequest(@NotNull DeliveryStatus status) {}
