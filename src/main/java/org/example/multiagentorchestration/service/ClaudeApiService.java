package org.example.multiagentorchestration.service;

import org.example.multiagentorchestration.model.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ClaudeApiService {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${claude.api.key}")
    private String apiKey;

    @Value("${claude.api.model:claude-sonnet-4-20250514}")
    private String model;

    @Value("${claude.api.version:2023-06-01}")
    private String apiVersion;

    @Value("${claude.api.max-tokens:4000}")
    private int maxTokens;

    @PostConstruct
    public void validateApiKey() {
        if (apiKey == null || apiKey.isBlank() || apiKey.equals("${claude.api.key}")) {
            throw new IllegalStateException(
                "claude.api.key must be set in application.properties. " +
                "Copy application.properties.template to application.properties and add your API key."
            );
        }
        if (!apiKey.startsWith("sk-ant-")) {
            throw new IllegalStateException(
                "Invalid claude.api.key format. API key should start with 'sk-ant-'"
            );
        }
    }

    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 1000;


    public String sendSimpleMessage(String systemPrompt, String userMessage){
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("max_tokens", maxTokens);
        requestBody.put("system", systemPrompt);
        List<Message> messages = new ArrayList<>();
        Message message = new Message("user", userMessage);
        messages.add(message);
        requestBody.put("messages", messages);

        HttpHeaders headers = new HttpHeaders();
        headers.set("x-api-key" , apiKey);
        headers.set("anthropic-version", apiVersion);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String,Object>> requestEntity = new HttpEntity<>(requestBody,headers);

        int attempt = 0;
        Exception lastException = null;

        while (attempt < MAX_RETRIES) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> response = restTemplate.postForObject(API_URL, requestEntity, Map.class);

                // Validate response structure
                if (response == null) {
                    throw new RuntimeException("Received null response from Claude API");
                }

                // Check for API error in response
                if (response.containsKey("error")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> error = (Map<String, Object>) response.get("error");
                    String errorType = (String) error.get("type");
                    String errorMessage = (String) error.get("message");
                    throw new RuntimeException("Claude API error [" + errorType + "]: " + errorMessage);
                }

                // Validate content exists
                if (!response.containsKey("content")) {
                    throw new RuntimeException("Response missing 'content' field: " + response);
                }

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> contentList = (List<Map<String, Object>>) response.get("content");

                if (contentList == null || contentList.isEmpty()) {
                    throw new RuntimeException("Response content list is empty or null");
                }

                Map<String, Object> firstContent = contentList.get(0);
                if (!firstContent.containsKey("text")) {
                    throw new RuntimeException("First content block missing 'text' field: " + firstContent);
                }

                return firstContent.get("text").toString();

            } catch (HttpClientErrorException e) {
                // 4xx errors - don't retry (authentication, invalid request, etc.)
                throw new RuntimeException("Claude API client error [" + e.getStatusCode() + "]: " + e.getResponseBodyAsString(), e);

            } catch (HttpServerErrorException e) {
                // 5xx errors - retry
                lastException = e;
                System.err.println("Claude API server error [" + e.getStatusCode() + "] on attempt " + (attempt + 1) + "/" + MAX_RETRIES + ": " + e.getResponseBodyAsString());

            } catch (ResourceAccessException e) {
                // Network/timeout errors - retry
                lastException = e;
                System.err.println("Network error on attempt " + (attempt + 1) + "/" + MAX_RETRIES + ": " + e.getMessage());

            } catch (RuntimeException e) {
                // Response validation errors - don't retry
                throw e;
            }

            attempt++;
            if (attempt < MAX_RETRIES) {
                try {
                    Thread.sleep(RETRY_DELAY_MS * attempt); // Exponential backoff
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted during retry delay", ie);
                }
            }
        }

        // All retries exhausted
        throw new RuntimeException("Failed to get response from Claude API after " + MAX_RETRIES + " attempts", lastException);
    }

}
