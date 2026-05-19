package com.lastmile.optiroute.controller;

import com.lastmile.optiroute.domain.enums.DeliveryStatus;
import com.lastmile.optiroute.dto.DeliveryRequest;
import com.lastmile.optiroute.dto.DeliveryResponse;
import com.lastmile.optiroute.dto.StatusUpdateRequest;
import com.lastmile.optiroute.service.DeliveryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

// Endpoints de entregas — todos exigem token JWT
@RestController
@RequestMapping("/api/deliveries")
public class DeliveryController {

    private final DeliveryService deliveryService;

    public DeliveryController(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    // POST /api/deliveries — cadastra uma nova entrega para a loja logada
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DeliveryResponse create(@AuthenticationPrincipal UserDetails userDetails,
                                   @Valid @RequestBody DeliveryRequest request) {
        // pega o id da loja direto do token JWT (via userDetails.getUsername())
        return deliveryService.create(UUID.fromString(userDetails.getUsername()), request);
    }

    // GET /api/deliveries?status=PENDING — lista entregas, com filtro opcional por status
    @GetMapping
    public List<DeliveryResponse> list(@AuthenticationPrincipal UserDetails userDetails,
                                       @RequestParam(required = false) DeliveryStatus status) {
        return deliveryService.list(UUID.fromString(userDetails.getUsername()), status);
    }

    // PATCH /api/deliveries/{id}/status — atualiza o status de uma entrega específica
    @PatchMapping("/{id}/status")
    public DeliveryResponse updateStatus(@AuthenticationPrincipal UserDetails userDetails,
                                         @PathVariable UUID id,
                                         @Valid @RequestBody StatusUpdateRequest request) {
        return deliveryService.updateStatus(UUID.fromString(userDetails.getUsername()), id, request);
    }
}
