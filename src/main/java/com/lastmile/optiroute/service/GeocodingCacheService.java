package com.lastmile.optiroute.service;

import com.lastmile.optiroute.client.GeocodingClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class GeocodingCacheService {

    private static final String CACHE_PREFIX = "geocode:";
    private static final Duration TTL = Duration.ofDays(30);

    private final StringRedisTemplate redis;
    private final GeocodingClient geocodingClient;

    public GeocodingCacheService(StringRedisTemplate redis, GeocodingClient geocodingClient) {
        this.redis = redis;
        this.geocodingClient = geocodingClient;
    }

    // retorna [longitude, latitude]
    public double[] getCoordinates(String address) {
        // trim+lowercase para "Rua X" e "rua x" baterem na mesma chave
        String key = CACHE_PREFIX + address.trim().toLowerCase();
        String cached = redis.opsForValue().get(key);

        if (cached != null) {
            try {
                String[] parts = cached.split(",");
                return new double[]{Double.parseDouble(parts[0]), Double.parseDouble(parts[1])};
            } catch (Exception e) {
                redis.delete(key);
            }
        }

        double[] coords = geocodingClient.geocode(address);
        redis.opsForValue().set(key, coords[0] + "," + coords[1], TTL);
        return coords;
    }
}
