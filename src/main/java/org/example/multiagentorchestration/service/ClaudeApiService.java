package org.example.multiagentorchestration.service;

import org.example.multiagentorchestration.model.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

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

    //without tools for subagents
    public String sendSimpleMessage(String systemPrompt, String userMessage){
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "claude-sonnet-4-20250514");
        requestBody.put("max_tokens", 1000);
        requestBody.put("system", systemPrompt);
        List<Message> messages = new ArrayList<>();
        Message message = new Message("user", userMessage);
        messages.add(message);
        requestBody.put("messages", messages);

        HttpHeaders headers = new HttpHeaders();
        headers.set("x-api-key" , apiKey);
        headers.set("anthropic-version", "2023-06-01");
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String,Object>> requestEntity = new HttpEntity<>(requestBody,headers);

        Map<String, Object> response = restTemplate.postForObject("https://api.anthropic.com/v1/messages",requestEntity,Map.class);
        List<Map<String, Object>> contentList =
                (List<Map<String, Object>>) response.get("content");

        return contentList.get(0).get("text").toString();
    }

}
