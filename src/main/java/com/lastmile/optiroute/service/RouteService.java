package com.lastmile.optiroute.service;

import com.lastmile.optiroute.client.OptimizationClient;
import com.lastmile.optiroute.domain.entity.Delivery;
import com.lastmile.optiroute.domain.entity.Route;
import com.lastmile.optiroute.domain.entity.Store;
import com.lastmile.optiroute.domain.enums.DeliveryStatus;
import com.lastmile.optiroute.dto.DeliveryResponse;
import com.lastmile.optiroute.dto.RouteResponse;
import com.lastmile.optiroute.exception.ResourceNotFoundException;
import com.lastmile.optiroute.repository.DeliveryRepository;
import com.lastmile.optiroute.repository.RouteRepository;
import com.lastmile.optiroute.repository.StoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// Responsável por calcular a rota otimizada para as entregas pendentes
@Service
public class RouteService {

    private final RouteRepository routeRepository;
    private final DeliveryRepository deliveryRepository;
    private final StoreRepository storeRepository;
    private final OptimizationClient optimizationClient;

    public RouteService(RouteRepository routeRepository, DeliveryRepository deliveryRepository,
                        StoreRepository storeRepository, OptimizationClient optimizationClient) {
        this.routeRepository = routeRepository;
        this.deliveryRepository = deliveryRepository;
        this.storeRepository = storeRepository;
        this.optimizationClient = optimizationClient;
    }

    // calcula a rota mais eficiente para todas as entregas PENDING da loja
    // tudo dentro de uma transação: se qualquer coisa falhar, desfaz tudo
    @Transactional
    public RouteResponse optimize(UUID storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Loja não encontrada"));

        // busca só as entregas que ainda não foram despachadas
        List<Delivery> pending = deliveryRepository.findByStoreIdAndStatus(storeId, DeliveryStatus.PENDING);
        if (pending.isEmpty()) {
            throw new IllegalStateException("Não há entregas pendentes para otimizar");
        }

        // ponto de origem = localização da loja (longitude, latitude)
        double[] origin = {store.getLocation().getX(), store.getLocation().getY()};

        // monta a lista de jobs para a API de otimização
        // o índice (i) é usado como id do job para poder mapear de volta depois
        List<OptimizationClient.JobRequest> jobs = new ArrayList<>();
        for (int i = 0; i < pending.size(); i++) {
            Delivery d = pending.get(i);
            // getY() = latitude, getX() = longitude (convenção do JTS)
            jobs.add(new OptimizationClient.JobRequest(i, d.getLocation().getY(), d.getLocation().getX()));
        }

        // chama a API externa e recebe a sequência ideal de entregas
        OptimizationClient.OptimizationResult result = optimizationClient.optimize(origin, jobs);

        // salva a rota no banco com distância e tempo totais
        Route route = Route.builder()
                .store(store)
                .totalDistanceMeters(result.totalDistanceMeters())
                .totalTimeSeconds(result.totalTimeSeconds())
                .build();
        Route savedRoute = routeRepository.save(route);

        // atualiza cada entrega com a ordem de parada e muda status para IN_TRANSIT
        List<Delivery> entregasOrdenadas = new ArrayList<>();
        for (int order = 0; order < result.orderedJobIds().size(); order++) {
            int jobIndex = result.orderedJobIds().get(order).intValue();
            Delivery delivery = pending.get(jobIndex);
            delivery.setRoute(savedRoute);
            delivery.setStatus(DeliveryStatus.IN_TRANSIT);
            delivery.setDeliveryOrder(order + 1); // começa em 1 (não em 0)
            deliveryRepository.save(delivery);
            entregasOrdenadas.add(delivery);
        }

        // monta a resposta com os dados já carregados em memória
        // evita re-buscar no banco e ter problema de cache do Hibernate
        return new RouteResponse(
                savedRoute.getId(),
                savedRoute.getTotalDistanceMeters(),
                savedRoute.getTotalTimeSeconds(),
                savedRoute.getStatus(),
                entregasOrdenadas.stream().map(DeliveryResponse::from).toList(),
                savedRoute.getCreatedAt()
        );
    }

    // busca uma rota específica da loja
    @Transactional(readOnly = true)
    public RouteResponse getById(UUID storeId, UUID routeId) {
        Route route = routeRepository.findByIdAndStoreIdWithDeliveries(routeId, storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Rota não encontrada"));
        return RouteResponse.from(route);
    }
}
