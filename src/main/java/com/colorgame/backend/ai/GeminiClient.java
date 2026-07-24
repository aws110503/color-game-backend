// This tells the computer where this file lives in the project's folder structure
package com.colorgame.backend.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Map;

// @Component tells the app: "This file is a Worker Bee that sends prompts to Gemini
// and hands back its raw text reply."
@Component
public class GeminiClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.api.url:}")
    private String apiUrl;

    // RETRY CONFIG: Gemini occasionally returns 503 ("model overloaded") under load —
    // this is transient, not a real error, so we retry a few times with increasing
    // delays before giving up, instead of failing the player's round on the first blip.
    private static final int MAX_RETRIES = 3;
    private static final Duration INITIAL_BACKOFF = Duration.ofSeconds(2);

    public GeminiClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    /**
     * Envoie un prompt texte à Gemini et retourne la réponse texte brute.
     * Réessaie automatiquement en cas d'erreur 503 (service temporairement surchargé).
     */
    public String generateContent(String prompt) {
        Map<String, Object> requestBody = Map.of(
                "contents", new Object[]{
                        Map.of("parts", new Object[]{
                                Map.of("text", prompt)
                        })
                }
        );

        String rawResponse = webClient.post()
                .uri(apiUrl + "?key=" + apiKey)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                // Only retry on 503 — a genuine 4xx (bad request, invalid key) retrying
                // wouldn't help and would just waste time before failing anyway.
                .retryWhen(Retry.backoff(MAX_RETRIES, INITIAL_BACKOFF)
                        .filter(this::isRetryableError)
                        .onRetryExhaustedThrow((spec, signal) -> new RuntimeException(
                                "Gemini indisponible après " + MAX_RETRIES + " tentatives : "
                                        + signal.failure().getMessage(), signal.failure())))
                .block();

        return extractTextFromResponse(rawResponse);
    }

    // HELPER: only 503 (Service Unavailable) is worth retrying — it means Gemini's
    // servers are temporarily overloaded, not that something is wrong with our request.
    private boolean isRetryableError(Throwable throwable) {
        if (throwable instanceof WebClientResponseException wcre) {
            return wcre.getStatusCode().value() == 503;
        }
        return false;
    }

    private String extractTextFromResponse(String rawResponse) {
        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            return root
                    .path("candidates").get(0)
                    .path("content")
                    .path("parts").get(0)
                    .path("text")
                    .asText();
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la lecture de la réponse Gemini : " + e.getMessage(), e);
        }
    }
}