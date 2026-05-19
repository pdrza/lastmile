package com.lastmile.optiroute.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Point;

import java.time.LocalDateTime;
import java.util.UUID;

// Representa uma loja cadastrada no sistema (cada loja é um "tenant")
@Entity
@Table(name = "stores")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password; // armazenado como hash bcrypt, nunca em texto puro

    @Column(name = "address_text", nullable = false)
    private String addressText;

    // ponto geográfico no mapa — SRID 4326 = sistema de coordenadas do GPS (WGS84)
    @Column(columnDefinition = "geometry(Point,4326)")
    private Point location;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // preenche a data automaticamente antes de salvar no banco pela primeira vez
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
