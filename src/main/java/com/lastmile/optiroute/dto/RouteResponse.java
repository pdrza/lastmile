package com.lastmile.optiroute.dto;

import com.lastmile.optiroute.domain.entity.Route;
import com.lastmile.optiroute.domain.enums.RouteStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record RouteResponse(
        UUID id,
        Integer totalDistanceMeters,
        Integer totalTimeSeconds,
        RouteStatus status,
        List<DeliveryResponse> deliveries,
        LocalDateTime createdAt
) {
    public static RouteResponse from(Route r) {
        return new RouteResponse(
                r.getId(),
                r.getTotalDistanceMeters(),
                r.getTotalTimeSeconds(),
                r.getStatus(),
                r.getDeliveries().stream().map(DeliveryResponse::from).toList(),
                r.getCreatedAt()
        );
    }
}
