package com.lastmile.optiroute.domain.entity;

import com.lastmile.optiroute.domain.enums.DeliveryStatus;
import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Point;

import java.time.LocalDateTime;
import java.util.UUID;

// Representa uma entrega (pacote a ser entregue para um cliente)
@Entity
@Table(name = "deliveries")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Delivery {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // toda entrega pertence a uma loja
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    // rota é nula enquanto a entrega está pendente — preenchida quando a rota é calculada
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id")
    private Route route;

    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @Column(name = "address_text", nullable = false)
    private String addressText;

    // coordenadas do endereço de destino (geradas pelo geocoding)
    @Column(columnDefinition = "geometry(Point,4326)")
    private Point location;

    // posição na rota otimizada: 1 = primeira entrega, 2 = segunda, etc.
    @Column(name = "delivery_order")
    private Integer deliveryOrder;

    // PENDING → IN_TRANSIT → DELIVERED (ou FAILED)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private DeliveryStatus status = DeliveryStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
