package org.mj.trip.common.service;// SmolagentClient.java
import org.mj.trip.pointrecommendation.dto.AiRecommendationDto;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.core.ParameterizedTypeReference;

@Component
public class AiRequestClient {

    private final RestClient restClient;

    public AiRequestClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.baseUrl("http://localhost:8000").build();
    }

    public List<AiRecommendationDto> getRecommendations(String requestText) {
        return restClient.post()
                .uri("/api/recommend")
                .body(Map.of("prompt", requestText))
                .retrieve()
                .body(new ParameterizedTypeReference<List<AiRecommendationDto>>() {});
    }

    public AsyncRecommendationService.AiRouteRecommendationDto getRoutePlan(
            String requestText,
            LocalDate startDate,
            LocalDate endDate,
            String region,
            BigDecimal budget) {

        Map<String, Object> requestBody = Map.of(
                "prompt", requestText,
                "startDate", startDate.toString(),
                "endDate", endDate.toString(),
                "region", region,
                "budget", budget
        );

        return restClient.post()
                .uri("/api/route-plan")
                .body(requestBody)
                .retrieve()
                .body(AsyncRecommendationService.AiRouteRecommendationDto.class);
    }
}
