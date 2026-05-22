package com.lastmile.optiroute.dto;

import com.lastmile.optiroute.domain.entity.Store;

public record StoreResponse(
        String name,
        String addressText,
        double latitude,
        double longitude
) {
    public static StoreResponse from(Store store) {
        return new StoreResponse(
                store.getName(),
                store.getAddressText(),
                store.getLocation().getY(),
                store.getLocation().getX()
        );
    }
}
