package com.lastmile.optiroute.service;

import com.lastmile.optiroute.domain.entity.Delivery;
import com.lastmile.optiroute.domain.entity.Store;
import com.lastmile.optiroute.domain.enums.DeliveryStatus;
import com.lastmile.optiroute.dto.DeliveryRequest;
import com.lastmile.optiroute.dto.DeliveryResponse;
import com.lastmile.optiroute.dto.StatusUpdateRequest;
import com.lastmile.optiroute.exception.ResourceNotFoundException;
import com.lastmile.optiroute.repository.DeliveryRepository;
import com.lastmile.optiroute.repository.StoreRepository;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final StoreRepository storeRepository;
    private final GeocodingCacheService geocodingCacheService;

    // SRID 4326 = sistema de coordenadas WGS84 (o mesmo do GPS)
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    public DeliveryService(DeliveryRepository deliveryRepository, StoreRepository storeRepository,
                           GeocodingCacheService geocodingCacheService) {
        this.deliveryRepository = deliveryRepository;
        this.storeRepository = storeRepository;
        this.geocodingCacheService = geocodingCacheService;
    }

    public DeliveryResponse create(UUID storeId, DeliveryRequest request) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Loja não encontrada"));

        double[] coords = geocodingCacheService.getCoordinates(request.address());

        Delivery delivery = Delivery.builder()
                .store(store)
                .customerName(request.customerName())
                .addressText(request.address())
                .location(geometryFactory.createPoint(new Coordinate(coords[0], coords[1])))
                .build(); // status padrão já é PENDING (definido na entidade)

        return DeliveryResponse.from(deliveryRepository.save(delivery));
    }

    public List<DeliveryResponse> list(UUID storeId, DeliveryStatus status) {
        List<Delivery> deliveries = status != null
                ? deliveryRepository.findByStoreIdAndStatus(storeId, status)
                : deliveryRepository.findByStoreId(storeId);
        return deliveries.stream().map(DeliveryResponse::from).toList();
    }

    // findByIdAndStoreId: uma loja não acessa entregas de outra
    public DeliveryResponse updateStatus(UUID storeId, UUID deliveryId, StatusUpdateRequest request) {
        Delivery delivery = deliveryRepository.findByIdAndStoreId(deliveryId, storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Entrega não encontrada"));

        delivery.setStatus(request.status());
        return DeliveryResponse.from(deliveryRepository.save(delivery));
    }
}
