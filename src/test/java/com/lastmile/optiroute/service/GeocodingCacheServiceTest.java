package com.lastmile.optiroute.service;

import com.lastmile.optiroute.client.GeocodingClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// Testa a lógica de cache — a parte mais importante é garantir que a API externa
// só seja chamada quando o endereço não estiver no cache (economiza requisições)
@ExtendWith(MockitoExtension.class)
class GeocodingCacheServiceTest {

    @Mock
    private StringRedisTemplate redis;

    @Mock
    private GeocodingClient geocodingClient;

    // ValueOperations é o objeto retornado por redis.opsForValue() — precisa ser mockado também
    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private GeocodingCacheService geocodingCacheService;

    @Test
    @DisplayName("Cache hit: deve retornar coordenadas do Redis sem chamar a API externa")
    void buscarCoordenadas_deveRetornarDoCacheQuandoEnderecoJaFoiGeocodificado() {
        // ARRANGE — simula que o endereço já está no Redis
        when(redis.opsForValue()).thenReturn(valueOperations);
        // Redis retorna "longitude,latitude" como string
        when(valueOperations.get("geocode:rua das flores, 100, curitiba")).thenReturn("-43.100,-22.900");

        // ACT
        double[] resultado = geocodingCacheService.getCoordinates("Rua das Flores, 100, Curitiba");

        // ASSERT — coordenadas devem ser as do cache
        assertThat(resultado[0]).isEqualTo(-43.100); // longitude
        assertThat(resultado[1]).isEqualTo(-22.900); // latitude

        // a API externa jamais deve ser chamada — esse é o ponto central do cache
        verify(geocodingClient, never()).geocode(anyString());
    }

    @Test
    @DisplayName("Cache miss: deve chamar a API externa e guardar resultado no Redis")
    void buscarCoordenadas_deveChamarAPIEGuardarNoCacheQuandoNaoEncontrado() {
        // ARRANGE — Redis não tem o endereço (cache miss)
        when(redis.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);

        // API de geocoding retorna as coordenadas reais
        when(geocodingClient.geocode("Avenida Brasil, 500, Curitiba"))
                .thenReturn(new double[]{-43.2, -22.8});

        // ACT
        double[] resultado = geocodingCacheService.getCoordinates("Avenida Brasil, 500, Curitiba");

        // ASSERT — resultado vem da API
        assertThat(resultado[0]).isEqualTo(-43.2);
        assertThat(resultado[1]).isEqualTo(-22.8);

        // API foi chamada exatamente uma vez
        verify(geocodingClient, times(1)).geocode("Avenida Brasil, 500, Curitiba");

        // resultado foi salvo no Redis para as próximas chamadas (TTL definido no service)
        verify(valueOperations, times(1)).set(
                eq("geocode:avenida brasil, 500, curitiba"),
                eq("-43.2,-22.8"),
                any() // TTL (Duration.ofDays(30))
        );
    }

    @Test
    @DisplayName("Normalização: endereços em maiúsculo e minúsculo devem usar a mesma chave no cache")
    void buscarCoordenadas_deveNormalizarChaveDoCacheParaMinusculo() {
        // ARRANGE — cache tem a chave em minúsculo
        when(redis.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("geocode:rua das flores, 100")).thenReturn("-43.1,-22.9");

        // ACT — chamada com endereço em maiúsculo
        double[] resultado = geocodingCacheService.getCoordinates("RUA DAS FLORES, 100");

        // ASSERT — deve encontrar no cache mesmo com caixa diferente
        assertThat(resultado).isNotNull();
        assertThat(resultado[0]).isEqualTo(-43.1);

        // API externa não foi chamada — normalização funcionou
        verify(geocodingClient, never()).geocode(anyString());
    }

    @Test
    @DisplayName("Cache miss com espaços extras: deve normalizar o endereço antes de buscar")
    void buscarCoordenadas_deveRemoverEspacosExtraDoEndereco() {
        // endereços com espaço antes/depois devem usar a mesma chave no cache
        when(redis.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("geocode:rua x, 10")).thenReturn("-43.0,-22.0");

        double[] resultado = geocodingCacheService.getCoordinates("  Rua X, 10  ");

        assertThat(resultado[0]).isEqualTo(-43.0);
        verify(geocodingClient, never()).geocode(anyString());
    }
}
