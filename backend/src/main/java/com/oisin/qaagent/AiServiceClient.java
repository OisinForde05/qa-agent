package com.oisin.qaagent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class AiServiceClient {

    private static final Logger logger =
            LoggerFactory.getLogger(AiServiceClient.class);

    private final RestClient restClient;

    public AiServiceClient(@Value("${ai.service.url}") String baseUrl) {
        SimpleClientHttpRequestFactory requestFactory =
                new SimpleClientHttpRequestFactory();

        requestFactory.setConnectTimeout(3000);
        requestFactory.setReadTimeout(3000);

        restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    public boolean isHealthy() {
        try {
            HealthResponse response = restClient.get()
                    .uri("/health")
                    .retrieve()
                    .body(HealthResponse.class);

            return response != null && "UP".equals(response.status());
        } catch (RestClientException exception) {
            logger.warn("AI service health check failed: {}",
                    exception.getMessage());

            return false;
        }
    }

    public record HealthResponse(String status) {
    }
}