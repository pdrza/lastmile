package com.lastmile.optiroute.controller;

import com.lastmile.optiroute.domain.enums.DeliveryStatus;
import com.lastmile.optiroute.repository.DeliveryRepository;
import com.lastmile.optiroute.repository.RouteRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

// Endpoints auxiliares para facilitar testes — não existem em produção.
// @Profile("!docker"): o bean NÃO é registrado quando a aplicação roda no
// pacote do lojista (perfil "docker"), então DELETE /api/test/reset some.
@RestController
@RequestMapping("/api/test")
@Profile("!docker")
public class TestController {

    private final DeliveryRepository deliveryRepository;
    private final RouteRepository routeRepository;

    public TestController(DeliveryRepository deliveryRepository, RouteRepository routeRepository) {
        this.deliveryRepository = deliveryRepository;
        this.routeRepository = routeRepository;
    }

    // reseta todas as entregas da loja para PENDING e apaga as rotas
    // útil para testar o optimize várias vezes sem precisar recadastrar entregas
    @DeleteMapping("/reset")
    @Transactional
    public Map<String, String> reset(@AuthenticationPrincipal UserDetails userDetails) {
        UUID storeId = UUID.fromString(userDetails.getUsername());

        // desvincula as entregas das rotas e volta para PENDING
        deliveryRepository.findByStoreId(storeId).forEach(d -> {
            d.setRoute(null);
            d.setDeliveryOrder(null);
            d.setStatus(DeliveryStatus.PENDING);
            deliveryRepository.save(d);
        });

        // apaga todas as rotas da loja
        routeRepository.findByStoreId(storeId)
                .forEach(routeRepository::delete);

        return Map.of("message", "Entregas resetadas para PENDING. Rotas apagadas.");
    }
}
