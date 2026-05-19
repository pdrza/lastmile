package com.lastmile.optiroute.service;

import com.lastmile.optiroute.client.GeocodingClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

// Verifica o cache antes de chamar a API de geocoding
// evita gastar requisições da API para endereços que já foram pesquisados antes
@Service
public class GeocodingCacheService {

    private static final String CACHE_PREFIX = "geocode:";
    private static final Duration TTL = Duration.ofDays(30); // endereço fica 30 dias no cache

    private final StringRedisTemplate redis;
    private final GeocodingClient geocodingClient;

    public GeocodingCacheService(StringRedisTemplate redis, GeocodingClient geocodingClient) {
        this.redis = redis;
        this.geocodingClient = geocodingClient;
    }

    // retorna [longitude, latitude] — primeiro verifica no Redis, se não tiver vai na API
    public double[] getCoordinates(String address) {
        // normaliza o endereço para que "Rua X" e "rua x" apontem para o mesmo cache
        String key = CACHE_PREFIX + address.trim().toLowerCase();
        String cached = redis.opsForValue().get(key);

        // cache hit: já temos as coordenadas salvas, não precisa chamar a API
        if (cached != null) {
            String[] parts = cached.split(",");
            return new double[]{Double.parseDouble(parts[0]), Double.parseDouble(parts[1])};
        }

        // cache miss: busca na API e salva no Redis para as próximas vezes
        double[] coords = geocodingClient.geocode(address);
        redis.opsForValue().set(key, coords[0] + "," + coords[1], TTL);
        return coords;
    }
}
