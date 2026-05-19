package com.lastmile.optiroute.service;

import com.lastmile.optiroute.client.OptimizationClient;
import com.lastmile.optiroute.domain.entity.Delivery;
import com.lastmile.optiroute.domain.entity.Route;
import com.lastmile.optiroute.domain.entity.Store;
import com.lastmile.optiroute.domain.enums.DeliveryStatus;
import com.lastmile.optiroute.dto.RouteResponse;
import com.lastmile.optiroute.exception.ResourceNotFoundException;
import com.lastmile.optiroute.repository.DeliveryRepository;
import com.lastmile.optiroute.repository.RouteRepository;
import com.lastmile.optiroute.repository.StoreRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// Testa as regras de negócio do motor de otimização de rotas
@ExtendWith(MockitoExtension.class)
class RouteServiceTest {

    @Mock
    private RouteRepository routeRepository;
    @Mock
    private DeliveryRepository deliveryRepository;
    @Mock
    private StoreRepository storeRepository;
    @Mock
    private OptimizationClient optimizationClient;

    @InjectMocks
    private RouteService routeService;

    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    @Test
    @DisplayName("Otimizar: deve lançar exceção quando não há entregas pendentes")
    void otimizar_deveLancarExcecaoQuandoSemEntregasPendentes() {
        UUID storeId = UUID.randomUUID();
        Store loja = criarLojaTeste(storeId);

        when(storeRepository.findById(storeId)).thenReturn(Optional.of(loja));
        // lista vazia = nenhuma entrega pendente
        when(deliveryRepository.findByStoreIdAndStatus(storeId, DeliveryStatus.PENDING))
                .thenReturn(List.of());

        assertThatThrownBy(() -> routeService.optimize(storeId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("pendentes");

        // a API de otimização jamais deve ser chamada se não há o que otimizar
        verify(optimizationClient, never()).optimize(any(), any());
        verify(routeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Otimizar: deve criar rota com entregas na ordem retornada pela API")
    void otimizar_deveCriarRotaComOrdemCorreta() {
        // ARRANGE
        UUID storeId = UUID.randomUUID();
        Store loja = criarLojaTeste(storeId);

        // 2 entregas pendentes — são passadas para a API com índice 0 e 1
        Delivery entrega0 = criarEntregaTeste(loja, "Ana", -43.1, -22.9);
        Delivery entrega1 = criarEntregaTeste(loja, "Bruno", -43.2, -22.8);

        when(storeRepository.findById(storeId)).thenReturn(Optional.of(loja));
        when(deliveryRepository.findByStoreIdAndStatus(storeId, DeliveryStatus.PENDING))
                .thenReturn(List.of(entrega0, entrega1));

        // API retorna: primeiro entrega no índice 1 (Bruno), depois índice 0 (Ana)
        // ou seja: a rota otimizada vai até Bruno primeiro, depois Ana
        var resultadoORS = new OptimizationClient.OptimizationResult(
                List.of(1L, 0L), // ordem otimizada: índice 1 (Bruno) → índice 0 (Ana)
                300,              // tempo total estimado em segundos
                5000              // distância total em metros
        );
        when(optimizationClient.optimize(any(), any())).thenReturn(resultadoORS);

        // quando salvar a rota, retorna com ID e totais preenchidos
        Route rotaSalva = Route.builder()
                .id(UUID.randomUUID())
                .store(loja)
                .totalDistanceMeters(5000)
                .totalTimeSeconds(300)
                .build();
        when(routeRepository.save(any(Route.class))).thenReturn(rotaSalva);

        // quando salvar uma entrega, retorna o mesmo objeto (com status e ordem já atualizados)
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(inv -> inv.getArgument(0));

        // ACT
        RouteResponse resposta = routeService.optimize(storeId);

        // ASSERT — a resposta deve ter as 2 entregas
        assertThat(resposta.deliveries()).hasSize(2);

        // Bruno (índice 1 da lista original) deve ser a PRIMEIRA parada (deliveryOrder = 1)
        assertThat(resposta.deliveries().get(0).customerName()).isEqualTo("Bruno");
        assertThat(resposta.deliveries().get(0).deliveryOrder()).isEqualTo(1);

        // Ana (índice 0 da lista original) deve ser a SEGUNDA parada (deliveryOrder = 2)
        assertThat(resposta.deliveries().get(1).customerName()).isEqualTo("Ana");
        assertThat(resposta.deliveries().get(1).deliveryOrder()).isEqualTo(2);

        // verifica totais
        assertThat(resposta.totalDistanceMeters()).isEqualTo(5000);
        assertThat(resposta.totalTimeSeconds()).isEqualTo(300);

        // verifica que as entregas foram salvas (2 entregas = 2 saves)
        verify(deliveryRepository, times(2)).save(any(Delivery.class));
    }

    @Test
    @DisplayName("Otimizar: deve mudar o status das entregas de PENDING para IN_TRANSIT")
    void otimizar_deveMudarStatusDasEntregasParaInTransit() {
        // ARRANGE
        UUID storeId = UUID.randomUUID();
        Store loja = criarLojaTeste(storeId);

        Delivery entrega = criarEntregaTeste(loja, "Carlos", -43.1, -22.9);
        // confirma que começa como PENDING
        assertThat(entrega.getStatus()).isEqualTo(DeliveryStatus.PENDING);

        when(storeRepository.findById(storeId)).thenReturn(Optional.of(loja));
        when(deliveryRepository.findByStoreIdAndStatus(storeId, DeliveryStatus.PENDING))
                .thenReturn(List.of(entrega));

        // API com uma única entrega
        var resultadoORS = new OptimizationClient.OptimizationResult(List.of(0L), 120, 2000);
        when(optimizationClient.optimize(any(), any())).thenReturn(resultadoORS);

        Route rotaSalva = Route.builder().id(UUID.randomUUID()).store(loja).build();
        when(routeRepository.save(any(Route.class))).thenReturn(rotaSalva);
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(inv -> inv.getArgument(0));

        // ACT
        routeService.optimize(storeId);

        // ASSERT — após a otimização, a entrega deve estar IN_TRANSIT
        // (o motoboy já sabe a rota, entrega está a caminho)
        assertThat(entrega.getStatus()).isEqualTo(DeliveryStatus.IN_TRANSIT);
        assertThat(entrega.getDeliveryOrder()).isEqualTo(1);
        assertThat(entrega.getRoute()).isEqualTo(rotaSalva);
    }

    @Test
    @DisplayName("Otimizar: deve lançar 404 quando loja não existe")
    void otimizar_deveLancarExcecaoQuandoLojaNaoExiste() {
        UUID storeId = UUID.randomUUID();
        when(storeRepository.findById(storeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> routeService.optimize(storeId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Buscar rota: deve lançar 404 quando rota não existe ou não pertence à loja")
    void buscarPorId_deveLancarExcecaoQuandoRotaNaoEncontrada() {
        UUID storeId = UUID.randomUUID();
        UUID routeId = UUID.randomUUID();

        // findByIdAndStoreIdWithDeliveries retorna empty = rota não existe ou pertence a outra loja
        when(routeRepository.findByIdAndStoreIdWithDeliveries(routeId, storeId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> routeService.getById(storeId, routeId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- métodos auxiliares para criar objetos de teste ---

    private Store criarLojaTeste(UUID storeId) {
        return Store.builder()
                .id(storeId)
                .name("Loja Teste")
                .email("loja@teste.com")
                // ponto geográfico da loja (origem das rotas)
                .location(geometryFactory.createPoint(new Coordinate(-43.3, -23.0)))
                .build();
    }

    private Delivery criarEntregaTeste(Store loja, String nomeCliente, double lon, double lat) {
        return Delivery.builder()
                .id(UUID.randomUUID())
                .store(loja)
                .customerName(nomeCliente)
                .addressText("Rua Teste, " + nomeCliente)
                .location(geometryFactory.createPoint(new Coordinate(lon, lat)))
                .status(DeliveryStatus.PENDING)
                .build();
    }
}
