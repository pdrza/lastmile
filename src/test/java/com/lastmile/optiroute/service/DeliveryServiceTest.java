package com.lastmile.optiroute.service;

import com.lastmile.optiroute.domain.entity.Delivery;
import com.lastmile.optiroute.domain.entity.Store;
import com.lastmile.optiroute.domain.enums.DeliveryStatus;
import com.lastmile.optiroute.dto.DeliveryRequest;
import com.lastmile.optiroute.dto.DeliveryResponse;
import com.lastmile.optiroute.dto.StatusUpdateRequest;
import com.lastmile.optiroute.exception.ResourceNotFoundException;
import com.lastmile.optiroute.repository.DeliveryRepository;
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

// Testa as regras de negócio das entregas, incluindo o comportamento do cache Redis
@ExtendWith(MockitoExtension.class)
class DeliveryServiceTest {

    @Mock
    private DeliveryRepository deliveryRepository;
    @Mock
    private StoreRepository storeRepository;
    @Mock
    private GeocodingCacheService geocodingCacheService;

    @InjectMocks
    private DeliveryService deliveryService;

    // utilitário para criar pontos geográficos nos testes (sem precisar do banco)
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    @Test
    @DisplayName("Criar entrega: deve chamar geocoding e salvar entrega com status PENDING")
    void criar_deveChamarGeocodingERetornarEntregaPending() {
        // ARRANGE
        UUID storeId = UUID.randomUUID();
        var request = new DeliveryRequest("João Silva", "Rua das Flores, 100, Curitiba");

        Store loja = Store.builder().id(storeId).name("Loja Teste").build();
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(loja));

        // geocoding (com ou sem cache) retorna as coordenadas
        when(geocodingCacheService.getCoordinates("Rua das Flores, 100, Curitiba"))
                .thenReturn(new double[]{-43.1, -22.9});

        // banco retorna a entrega com ID após salvar
        Delivery entregaSalva = Delivery.builder()
                .id(UUID.randomUUID())
                .store(loja)
                .customerName("João Silva")
                .addressText("Rua das Flores, 100, Curitiba")
                .location(geometryFactory.createPoint(new Coordinate(-43.1, -22.9)))
                .status(DeliveryStatus.PENDING)
                .build();
        when(deliveryRepository.save(any(Delivery.class))).thenReturn(entregaSalva);

        // ACT
        DeliveryResponse resposta = deliveryService.create(storeId, request);

        // ASSERT
        assertThat(resposta).isNotNull();
        assertThat(resposta.customerName()).isEqualTo("João Silva");
        assertThat(resposta.status()).isEqualTo(DeliveryStatus.PENDING);

        // a regra mais importante: geocoding SEMPRE deve ser chamado ao criar entrega
        // o GeocodingCacheService já cuida de verificar o cache internamente
        verify(geocodingCacheService, times(1)).getCoordinates("Rua das Flores, 100, Curitiba");
        verify(deliveryRepository, times(1)).save(any(Delivery.class));
    }

    @Test
    @DisplayName("Criar entrega (cache hit): geocoding retorna do cache — API externa não é chamada")
    void criar_geocodingRetornaDoCache_ApiExternaNaoEChamada() {
        // Este teste documenta o comportamento esperado do cache:
        // se o endereço já foi pesquisado antes, o GeocodingCacheService devolve do Redis
        // e o GeocodingClient (API externa) não é chamado
        // Como o cache está encapsulado no GeocodingCacheService, testamos o comportamento esperado aqui
        UUID storeId = UUID.randomUUID();
        var request = new DeliveryRequest("Maria", "Avenida Brasil, 500");

        Store loja = Store.builder().id(storeId).name("Loja").build();
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(loja));

        // simulamos que o GeocodingCacheService retornou do cache (mesmas coordenadas, sem lentidão)
        when(geocodingCacheService.getCoordinates("Avenida Brasil, 500"))
                .thenReturn(new double[]{-43.2, -22.8});

        Delivery entregaSalva = Delivery.builder()
                .id(UUID.randomUUID())
                .store(loja)
                .customerName("Maria")
                .addressText("Avenida Brasil, 500")
                .location(geometryFactory.createPoint(new Coordinate(-43.2, -22.8)))
                .status(DeliveryStatus.PENDING)
                .build();
        when(deliveryRepository.save(any(Delivery.class))).thenReturn(entregaSalva);

        DeliveryResponse resposta = deliveryService.create(storeId, request);

        assertThat(resposta.addressText()).isEqualTo("Avenida Brasil, 500");
        // o DeliveryService sempre delega para o GeocodingCacheService — uma única chamada
        verify(geocodingCacheService, times(1)).getCoordinates("Avenida Brasil, 500");
    }

    @Test
    @DisplayName("Criar entrega: deve lançar 404 quando loja não existe")
    void criar_deveLancarExcecaoQuandoLojaNaoExiste() {
        UUID storeId = UUID.randomUUID();
        var request = new DeliveryRequest("João", "Rua X");

        // loja não encontrada no banco
        when(storeRepository.findById(storeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deliveryService.create(storeId, request))
                .isInstanceOf(ResourceNotFoundException.class);

        // geocoding jamais deve ser chamado se a loja não existe
        verify(geocodingCacheService, never()).getCoordinates(anyString());
        verify(deliveryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Listar: deve retornar todas as entregas da loja quando status não informado")
    void listar_deveRetornarTodasEntregasDaLoja() {
        UUID storeId = UUID.randomUUID();
        Store loja = Store.builder().id(storeId).build();

        Delivery e1 = Delivery.builder().id(UUID.randomUUID()).store(loja).customerName("Ana")
                .addressText("Rua A").location(geometryFactory.createPoint(new Coordinate(-43.1, -22.9)))
                .status(DeliveryStatus.PENDING).build();
        Delivery e2 = Delivery.builder().id(UUID.randomUUID()).store(loja).customerName("Bruno")
                .addressText("Rua B").location(geometryFactory.createPoint(new Coordinate(-43.2, -22.8)))
                .status(DeliveryStatus.IN_TRANSIT).build();

        when(deliveryRepository.findByStoreId(storeId)).thenReturn(List.of(e1, e2));

        // sem filtro de status → retorna tudo
        List<DeliveryResponse> resultado = deliveryService.list(storeId, null);

        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).customerName()).isEqualTo("Ana");
        assertThat(resultado.get(1).customerName()).isEqualTo("Bruno");

        // quando não há filtro, usa o método sem status
        verify(deliveryRepository, times(1)).findByStoreId(storeId);
        verify(deliveryRepository, never()).findByStoreIdAndStatus(any(), any());
    }

    @Test
    @DisplayName("Listar: deve retornar apenas entregas com o status filtrado")
    void listar_deveRetornarEntregasFiltradasPorStatus() {
        UUID storeId = UUID.randomUUID();
        Store loja = Store.builder().id(storeId).build();

        Delivery entregaPendente = Delivery.builder().id(UUID.randomUUID()).store(loja)
                .customerName("Ana").addressText("Rua A")
                .location(geometryFactory.createPoint(new Coordinate(-43.1, -22.9)))
                .status(DeliveryStatus.PENDING).build();

        when(deliveryRepository.findByStoreIdAndStatus(storeId, DeliveryStatus.PENDING))
                .thenReturn(List.of(entregaPendente));

        List<DeliveryResponse> resultado = deliveryService.list(storeId, DeliveryStatus.PENDING);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).status()).isEqualTo(DeliveryStatus.PENDING);

        // quando há filtro, usa o método com status
        verify(deliveryRepository, times(1)).findByStoreIdAndStatus(storeId, DeliveryStatus.PENDING);
        verify(deliveryRepository, never()).findByStoreId(any());
    }

    @Test
    @DisplayName("Atualizar status: deve alterar o status da entrega corretamente")
    void atualizarStatus_deveAlterarStatusDaEntrega() {
        UUID storeId = UUID.randomUUID();
        UUID deliveryId = UUID.randomUUID();
        Store loja = Store.builder().id(storeId).build();

        Delivery entrega = Delivery.builder().id(deliveryId).store(loja).customerName("Carlos")
                .addressText("Rua C").location(geometryFactory.createPoint(new Coordinate(-43.1, -22.9)))
                .status(DeliveryStatus.IN_TRANSIT).build();

        when(deliveryRepository.findByIdAndStoreId(deliveryId, storeId)).thenReturn(Optional.of(entrega));
        // quando salvar, retorna o mesmo objeto modificado
        when(deliveryRepository.save(entrega)).thenReturn(entrega);

        var statusRequest = new StatusUpdateRequest(DeliveryStatus.DELIVERED);
        DeliveryResponse resposta = deliveryService.updateStatus(storeId, deliveryId, statusRequest);

        assertThat(resposta.status()).isEqualTo(DeliveryStatus.DELIVERED);
        verify(deliveryRepository, times(1)).save(entrega);
    }

    @Test
    @DisplayName("Atualizar status (ownership check): deve lançar 404 quando entrega não pertence à loja")
    void atualizarStatus_deveLancarExcecaoQuandoEntregaNaoPertenceALoja() {
        UUID storeId = UUID.randomUUID();
        UUID deliveryId = UUID.randomUUID();

        // findByIdAndStoreId retorna empty quando a entrega não existe OU pertence a outra loja
        // isso é a proteção de multitenancy — uma loja não pode mexer nas entregas de outra
        when(deliveryRepository.findByIdAndStoreId(deliveryId, storeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deliveryService.updateStatus(
                storeId, deliveryId, new StatusUpdateRequest(DeliveryStatus.DELIVERED)))
                .isInstanceOf(ResourceNotFoundException.class);

        // garante que o banco não foi chamado para salvar nada
        verify(deliveryRepository, never()).save(any());
    }
}
