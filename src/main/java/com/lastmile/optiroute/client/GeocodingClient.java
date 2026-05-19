package com.lastmile.optiroute.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

// Responsável por chamar a API do OpenRouteService para converter endereço em coordenadas
@Component
public class GeocodingClient {

    private final RestClient restClient;
    private final String apiKey;

    public GeocodingClient(
            RestClient.Builder builder,
            @Value("${app.geocoding.base-url}") String baseUrl,
            @Value("${app.geocoding.api-key}") String apiKey) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.apiKey = apiKey;
    }

    // recebe um endereço em texto e retorna [longitude, latitude]
    public double[] geocode(String address) {
        GeocodingResponse response = restClient.get()
                // manda o endereço pra API e pede só 1 resultado
                .uri("/geocode/search?api_key={key}&text={text}&size=1", apiKey, address)
                .retrieve()
                .body(GeocodingResponse.class);

        if (response == null || response.features() == null || response.features().isEmpty()) {
            throw new IllegalArgumentException("Não foi possível encontrar coordenadas para o endereço: " + address);
        }

        // a ORS retorna as coordenadas como [longitude, latitude] (nessa ordem)
        List<Double> coords = response.features().get(0).geometry().coordinates();
        return new double[]{coords.get(0), coords.get(1)};
    }

    // estrutura da resposta que a API do ORS devolve
    record GeocodingResponse(List<Feature> features) {}
    record Feature(Geometry geometry) {}
    record Geometry(List<Double> coordinates) {}
}
