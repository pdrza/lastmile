package com.lastmile.optiroute.domain.entity;

import com.lastmile.optiroute.domain.enums.RouteStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// Representa uma rota calculada — contém a sequência otimizada de entregas
@Entity
@Table(name = "routes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Route {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // toda rota pertence a uma loja
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    // distância total da rota em metros
    @Column(name = "total_distance_meters")
    private Integer totalDistanceMeters;

    // tempo estimado total em segundos
    @Column(name = "total_time_seconds")
    private Integer totalTimeSeconds;

    // ACTIVE = motoboy ainda entregando | COMPLETED = todas as entregas finalizadas
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private RouteStatus status = RouteStatus.ACTIVE;

    // lista de entregas já ordenadas pelo deliveryOrder (1, 2, 3...)
    @OneToMany(mappedBy = "route", fetch = FetchType.LAZY)
    @OrderBy("deliveryOrder ASC")
    @Builder.Default
    private List<Delivery> deliveries = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
