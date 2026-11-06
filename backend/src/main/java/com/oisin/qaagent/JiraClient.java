package com.oisin.qaagent;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import tools.jackson.databind.JsonNode;

@Service
public class JiraClient {

    private final RestClient client;
    private final boolean configured;

    public JiraClient(
        @Value("${jira.base-url:}") String baseUrl,
        @Value("${jira.email:}") String email,
        @Value("${jira.api-token:}") String apiToken) {

        String url = baseUrl.strip().replaceAll("/+$", "");
        String username = email.strip();
        String token = apiToken.strip();

        configured = !url.isBlank()
                && !username.isBlank()
                && !token.isBlank();

        SimpleClientHttpRequestFactory requestFactory =
                new SimpleClientHttpRequestFactory();

        requestFactory.setConnectTimeout(5000);
        requestFactory.setReadTimeout(20000);

        RestClient.Builder jiraBuilder = RestClient.builder()
        .requestFactory(requestFactory);

        if (configured) {
            jiraBuilder.baseUrl(url);
            jiraBuilder.defaultHeaders(headers ->
                    headers.setBasicAuth(
                            username, token, StandardCharsets.UTF_8));
        }

        client = jiraBuilder.build();
    }

    public JsonNode getIssue(String ticketKey) {
        String key = ticketKey == null
                ? ""
                : ticketKey.strip().toUpperCase(Locale.ROOT);

        if (!key.matches("[A-Z][A-Z0-9_]*-[1-9][0-9]*")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Enter a Jira ticket key such as QA-1.");
        }

        if (!configured) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Jira is not configured. Set the Jira connection settings.");
        }

        try {
            JsonNode issue = client.get()
                    .uri("/rest/api/3/issue/{key}", key)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(JsonNode.class);

            if (issue == null
                    || !issue.path("key").isTextual()
                    || !issue.path("fields").isObject()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Jira returned an invalid ticket response.");
            }

            return issue;

        } catch (RestClientResponseException exception) {
            throw mapError(exception.getStatusCode().value());

        } catch (ResourceAccessException exception) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Could not contact Jira. Check the connection and try again.");

        } catch (RestClientException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Could not read the Jira response.");
        }
    }

    private ResponseStatusException mapError(int status) {
        return switch (status) {
            case 401, 403 -> new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Jira denied access. Check the configured credentials and permissions.");

            case 404 -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "The Jira ticket was not found or is not accessible.");

            case 429 -> new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Jira's request limit was reached. Try again later.");

            default -> new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Jira could not complete the request.");
        };
    }
}