package com.lastmile.optiroute.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

// Chama a API do OpenRouteService para calcular a rota mais eficiente
// usa o formato VROOM (padrão de otimização de rotas com veículos)
@Component
public class OptimizationClient {

    private final RestClient restClient;
    private final String apiKey;

    public OptimizationClient(
            RestClient.Builder builder,
            @Value("${app.geocoding.base-url}") String baseUrl,
            @Value("${app.geocoding.api-key}") String apiKey) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.apiKey = apiKey;
    }

    // recebe a origem (loja) e lista de entregas, devolve a ordem otimizada
    public OptimizationResult optimize(double[] origin, List<JobRequest> jobs) {

        // define o veículo (motoboy): começa e termina na loja
        // "driving-car" é o perfil de rota de carro/moto exigido pela ORS
        var vehicle = new VroomVehicle(1, "driving-car",
                List.of(origin[0], origin[1]),
                List.of(origin[0], origin[1]));

        // converte as entregas para o formato que a API espera
        var vroomJobs = jobs.stream()
                .map(j -> new VroomJob(j.id(), List.of(j.longitude(), j.latitude())))
                .toList();

        var payload = new VroomRequest(vroomJobs, List.of(vehicle));

        VroomResponse response = restClient.post()
                .uri("/optimization")
                .header("Authorization", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(VroomResponse.class);

        if (response == null || response.routes() == null || response.routes().isEmpty()) {
            throw new IllegalStateException("A API de otimização não retornou nenhuma rota");
        }

        VroomRoute route = response.routes().get(0);

        // filtra só as paradas do tipo "job" (ignora start e end) e pega os ids na ordem certa
        List<Long> orderedJobIds = route.steps().stream()
                .filter(s -> "job".equals(s.type()))
                .map(VroomStep::id)
                .toList();

        return new OptimizationResult(orderedJobIds, route.duration(), route.distance());
    }

    // entrada: cada entrega com seu índice e coordenadas
    public record JobRequest(long id, double latitude, double longitude) {}

    // saída: ids dos jobs na ordem otimizada + tempo e distância totais
    public record OptimizationResult(List<Long> orderedJobIds, int totalTimeSeconds, int totalDistanceMeters) {}

    // estruturas internas do formato VROOM
    record VroomRequest(List<VroomJob> jobs, List<VroomVehicle> vehicles) {}
    record VroomJob(long id, List<Double> location) {}
    record VroomVehicle(int id, String profile, List<Double> start, List<Double> end) {}
    record VroomResponse(List<VroomRoute> routes) {}
    record VroomRoute(List<VroomStep> steps, int duration, int distance) {}
    record VroomStep(String type, Long id) {}
}
