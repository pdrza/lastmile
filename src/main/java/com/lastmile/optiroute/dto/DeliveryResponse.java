package com.lastmile.optiroute.dto;

import com.lastmile.optiroute.domain.entity.Delivery;
import com.lastmile.optiroute.domain.enums.DeliveryStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record DeliveryResponse(
        UUID id,
        String customerName,
        String addressText,
        Double latitude,
        Double longitude,
        Integer deliveryOrder,
        DeliveryStatus status,
        LocalDateTime createdAt
) {
    public static DeliveryResponse from(Delivery d) {
        return new DeliveryResponse(
                d.getId(),
                d.getCustomerName(),
                d.getAddressText(),
                d.getLocation() != null ? d.getLocation().getY() : null,
                d.getLocation() != null ? d.getLocation().getX() : null,
                d.getDeliveryOrder(),
                d.getStatus(),
                d.getCreatedAt()
        );
    }
}
