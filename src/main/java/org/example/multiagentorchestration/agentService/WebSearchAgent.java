package org.example.multiagentorchestration.agentService;

import org.example.multiagentorchestration.model.Finding;
import org.example.multiagentorchestration.service.ClaudeApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

@Service
public class WebSearchAgent {

    @Autowired
    private ClaudeApiService  claudeApiService;

    // Agent Definition
    private static final String NAME = "web_search_agent";
    private static final String DESCRIPTION = "Web Search the given Topic";
    private static final String SYSTEM_PROMPT =
                    "You are a web research agent. " +
                    "Research the given topic thoroughly. " +
                    "Return ONLY a JSON array of findings. No explanation, no markdown. " +
                    "Each finding must have these exact fields: " +
                    "claim, source_url, document_name, page_number, confidence, retrieved_by. " +
                    "confidence must be HIGH, MEDIUM, or LOW. " +
                    "retrieved_by must always be 'web_search_agent'. " +
                    "Example: [{\"claim\": \"AI is growing\", " +
                    "\"source_url\": \"https://example.com\", " +
                    "\"document_name\": \"AI Report 2024\", " +
                    "\"page_number\": 1, " +
                    "\"confidence\": \"HIGH\", " +
                    "\"retrieved_by\": \"web_search_agent\"}]";

    // Execute
    public List<Finding> execute(String topic) {
        // call Claude API
        String response = claudeApiService.sendSimpleMessage(SYSTEM_PROMPT, topic);
        ObjectMapper mapper = new ObjectMapper();
        try{
            return mapper.readValue(response, new TypeReference<>(){});
        } catch(Exception e) {
            throw new RuntimeException("Failed to parse findings: " + response, e);
        }
    }
}