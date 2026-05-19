package com.lastmile.optiroute.controller;

import com.lastmile.optiroute.dto.LoginRequest;
import com.lastmile.optiroute.dto.LoginResponse;
import com.lastmile.optiroute.dto.RegisterRequest;
import com.lastmile.optiroute.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

// Endpoints de autenticação — cadastro e login das lojas
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // POST /api/auth/register — cadastra a loja e já retorna o token
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public LoginResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    // POST /api/auth/login — faz login e retorna o token JWT
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
