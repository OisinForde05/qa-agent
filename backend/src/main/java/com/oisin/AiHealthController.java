package com.oisin.qaagent;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AiHealthController {

    private final AiServiceClient aiServiceClient;

    public AiHealthController(AiServiceClient aiServiceClient) {
        this.aiServiceClient = aiServiceClient;
    }

    @GetMapping("/api/ai/health")
    public ResponseEntity<Map<String, String>> health() {
        if (aiServiceClient.isHealthy()) {
            return ResponseEntity.ok(Map.of("status", "UP"));
        }

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "status", "DOWN",
                        "message", "AI service is unavailable or unhealthy"
                ));
    }
}