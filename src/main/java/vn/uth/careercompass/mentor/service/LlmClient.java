package vn.uth.careercompass.mentor.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class LlmClient {

    private final RestClient restClient;

    @Value("${llm.api-key}")
    private String apiKey;

    @Value("${llm.model:gemini-2.5-flash}")
    private String model;

    public LlmClient() {
        this.restClient = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com/v1beta")
                .build();
    }

    public String ask(String prompt) {
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt)
                        ))
                )
        );

        // Gemini free-tier hay trả 503 (quá tải) / 429 (rate limit) tạm thời -> retry vài lần.
        RuntimeException lastError = null;
        for (int attempt = 1; attempt <= 4; attempt++) {
            try {
                Map<String, Object> response = restClient.post()
                        .uri("/models/{model}:generateContent", model)
                        .header("x-goog-api-key", apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(requestBody)
                        .retrieve()
                        .body(Map.class);

                var candidates = (List<Map<String, Object>>) response.get("candidates");
                var content = (Map<String, Object>) candidates.get(0).get("content");
                var parts = (List<Map<String, Object>>) content.get("parts");
                return (String) parts.get(0).get("text");
            } catch (HttpServerErrorException | HttpClientErrorException.TooManyRequests e) {
                lastError = e;
                sleepQuietly(1200L * attempt);   // backoff tăng dần (1.2s, 2.4s, 3.6s)
            }
        }
        throw lastError;   // MentorService/PortfolioService bắt -> fallback
    }

    private void sleepQuietly(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}