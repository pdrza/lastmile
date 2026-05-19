package com.lastmile.optiroute.dto;

import jakarta.validation.constraints.NotBlank;

public record DeliveryRequest(
        @NotBlank String customerName,
        @NotBlank String address
) {}
