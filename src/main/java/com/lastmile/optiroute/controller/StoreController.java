package com.lastmile.optiroute.controller;

import com.lastmile.optiroute.dto.StoreResponse;
import com.lastmile.optiroute.exception.ResourceNotFoundException;
import com.lastmile.optiroute.repository.StoreRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/stores")
public class StoreController {

    private final StoreRepository storeRepository;

    public StoreController(StoreRepository storeRepository) {
        this.storeRepository = storeRepository;
    }

    @GetMapping("/me")
    public StoreResponse me(@AuthenticationPrincipal UserDetails userDetails) {
        UUID storeId = UUID.fromString(userDetails.getUsername());
        return storeRepository.findById(storeId)
                .map(StoreResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Loja não encontrada"));
    }
}
