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

@Service
public class AuthService {

    private final StoreRepository storeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final GeocodingCacheService geocodingCacheService;

    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326); // WGS84

    public AuthService(StoreRepository storeRepository, PasswordEncoder passwordEncoder,
                       JwtService jwtService, GeocodingCacheService geocodingCacheService) {
        this.storeRepository = storeRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.geocodingCacheService = geocodingCacheService;
    }

    public LoginResponse register(RegisterRequest request) {
        if (storeRepository.findByEmail(request.email()).isPresent()) {
            throw new IllegalArgumentException("Email já cadastrado: " + request.email());
        }

        double[] coords = geocodingCacheService.getCoordinates(request.address());

        Store store = Store.builder()
                .name(request.name())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .addressText(request.address())
                .location(geometryFactory.createPoint(new Coordinate(coords[0], coords[1])))
                .build();

        Store saved = storeRepository.save(store);

        return new LoginResponse(jwtService.generateToken(saved));
    }

    public LoginResponse login(LoginRequest request) {
        Store store = storeRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Credenciais inválidas"));

        if (!passwordEncoder.matches(request.password(), store.getPassword())) {
            throw new BadCredentialsException("Credenciais inválidas");
        }

        return new LoginResponse(jwtService.generateToken(store));
    }
}
