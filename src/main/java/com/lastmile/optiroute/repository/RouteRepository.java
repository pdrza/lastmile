package com.lastmile.optiroute.repository;

import com.lastmile.optiroute.domain.entity.Route;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

// Interface de acesso ao banco para as rotas
public interface RouteRepository extends JpaRepository<Route, UUID> {

    // busca uma rota garantindo que ela pertence à loja correta
    Optional<Route> findByIdAndStoreId(UUID id, UUID storeId);

    // mesma busca mas já carrega as entregas em um único SQL (evita lazy loading vazio)
    @Query("SELECT r FROM Route r LEFT JOIN FETCH r.deliveries WHERE r.id = :id AND r.store.id = :storeId")
    Optional<Route> findByIdAndStoreIdWithDeliveries(@Param("id") UUID id, @Param("storeId") UUID storeId);

    java.util.List<Route> findByStoreId(UUID storeId);
}
