package com.lastmile.optiroute.repository;

import com.lastmile.optiroute.domain.entity.Delivery;
import com.lastmile.optiroute.domain.enums.DeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Interface de acesso ao banco para as entregas
public interface DeliveryRepository extends JpaRepository<Delivery, UUID> {

    // busca todas as entregas de uma loja
    List<Delivery> findByStoreId(UUID storeId);

    // busca entregas de uma loja filtradas por status (ex: só as PENDING)
    List<Delivery> findByStoreIdAndStatus(UUID storeId, DeliveryStatus status);

    // busca uma entrega específica garantindo que ela pertence à loja correta
    // evita que uma loja acesse entrega de outra loja
    Optional<Delivery> findByIdAndStoreId(UUID id, UUID storeId);
}
