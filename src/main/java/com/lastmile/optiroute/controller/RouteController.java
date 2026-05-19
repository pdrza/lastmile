package com.lastmile.optiroute.controller;

import com.lastmile.optiroute.dto.RouteResponse;
import com.lastmile.optiroute.service.RouteService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

// Endpoints de rotas — todos exigem token JWT
@RestController
@RequestMapping("/api/routes")
public class RouteController {

    private final RouteService routeService;

    public RouteController(RouteService routeService) {
        this.routeService = routeService;
    }

    // POST /api/routes/optimize — calcula a rota otimizada para as entregas pendentes
    @PostMapping("/optimize")
    @ResponseStatus(HttpStatus.CREATED)
    public RouteResponse optimize(@AuthenticationPrincipal UserDetails userDetails) {
        return routeService.optimize(UUID.fromString(userDetails.getUsername()));
    }

    // GET /api/routes/{id} — busca os detalhes de uma rota com as entregas em ordem
    @GetMapping("/{id}")
    public RouteResponse getById(@AuthenticationPrincipal UserDetails userDetails,
                                 @PathVariable UUID id) {
        return routeService.getById(UUID.fromString(userDetails.getUsername()), id);
    }
}
