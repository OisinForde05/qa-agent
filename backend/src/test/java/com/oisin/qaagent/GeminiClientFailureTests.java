package com.oisin.qaagent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

class GeminiClientFailureTests {

    @Test
    void reportsServiceUnavailableWithoutBlamingTheApiKey() {
        var failure = GeminiClient.httpFailure(503);

        assertEquals(
                HttpStatus.SERVICE_UNAVAILABLE,
                failure.getStatusCode()
        );

        assertTrue(failure.getReason().contains("temporarily unavailable"));
        assertFalse(failure.getReason().contains("API key"));
    }

    @Test
    void distinguishesQuotaLimitsFromServiceUnavailability() {
        var failure = GeminiClient.httpFailure(429);

        assertEquals(
                HttpStatus.TOO_MANY_REQUESTS,
                failure.getStatusCode()
        );

        assertTrue(failure.getReason().contains("quota"));
    }

    @Test
    void reportsUpstreamAuthenticationErrorsAsBackendIntegrationFailures() {
        for (int upstreamStatus : new int[]{401, 403}) {
            var failure = GeminiClient.httpFailure(upstreamStatus);

            assertEquals(HttpStatus.BAD_GATEWAY, failure.getStatusCode());
            assertTrue(failure.getReason().contains("authentication or access"));
        }
    }

    @Test
    void reportsUpstreamTimeouts() {
        for (int upstreamStatus : new int[]{408, 504}) {
            var failure = GeminiClient.httpFailure(upstreamStatus);

            assertEquals(HttpStatus.GATEWAY_TIMEOUT, failure.getStatusCode());
        }
    }

    @Test
    void detectsTimeoutInsideWrappedExceptions() {
        var failure = new RestClientException(
                "Response could not be read",
                new ResourceAccessException(
                        "Read failed",
                        new SocketTimeoutException("Read timed out")
                )
        );

        assertTrue(GeminiClient.hasTimeoutCause(failure));
    }

    @Test
    void doesNotTreatOtherConnectionFailuresAsTimeouts() {
        var failure = new ResourceAccessException(
                "Connection failed",
                new UnknownHostException("example.invalid")
        );

        assertFalse(GeminiClient.hasTimeoutCause(failure));
    }
}
