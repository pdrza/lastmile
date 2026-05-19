package com.lastmile.optiroute.service;

import com.lastmile.optiroute.domain.entity.Store;
import com.lastmile.optiroute.dto.LoginRequest;
import com.lastmile.optiroute.dto.LoginResponse;
import com.lastmile.optiroute.dto.RegisterRequest;
import com.lastmile.optiroute.repository.StoreRepository;
import com.lastmile.optiroute.security.JwtService;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

// Lógica de cadastro e login das lojas
@Service
public class AuthService {

    private final StoreRepository storeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final GeocodingCacheService geocodingCacheService;

    // usado para criar o ponto geográfico (PostGIS) com SRID 4326 (sistema de coordenadas do GPS)
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    public AuthService(StoreRepository storeRepository, PasswordEncoder passwordEncoder,
                       JwtService jwtService, GeocodingCacheService geocodingCacheService) {
        this.storeRepository = storeRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.geocodingCacheService = geocodingCacheService;
    }

    // cadastra uma loja nova e já retorna o token JWT
    public LoginResponse register(RegisterRequest request) {
        // não deixa cadastrar dois emails iguais
        if (storeRepository.findByEmail(request.email()).isPresent()) {
            throw new IllegalArgumentException("Email já cadastrado: " + request.email());
        }

        // converte o endereço da loja em coordenadas geográficas
        double[] coords = geocodingCacheService.getCoordinates(request.address());

        Store store = Store.builder()
                .name(request.name())
                .email(request.email())
                .password(passwordEncoder.encode(request.password())) // nunca salva senha em texto puro
                .addressText(request.address())
                .location(geometryFactory.createPoint(new Coordinate(coords[0], coords[1])))
                .build();

        Store saved = storeRepository.save(store);

        // já retorna o token para a loja já ficar logada após o cadastro
        return new LoginResponse(jwtService.generateToken(saved));
    }

    // faz login e retorna o token JWT
    public LoginResponse login(LoginRequest request) {
        Store store = storeRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Credenciais inválidas"));

        // compara a senha digitada com o hash salvo no banco
        if (!passwordEncoder.matches(request.password(), store.getPassword())) {
            throw new BadCredentialsException("Credenciais inválidas");
        }

        return new LoginResponse(jwtService.generateToken(store));
    }
}
