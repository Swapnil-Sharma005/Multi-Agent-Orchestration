package org.example.multiagentorchestration.agentService;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.multiagentorchestration.model.Finding;
import org.example.multiagentorchestration.service.ClaudeApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DocumentAnalysisAgent {

    @Autowired
    private ClaudeApiService  claudeApiService;

    // Agent Definition
    private static final String NAME = "document_Analysis_agent";
    private static final String DESCRIPTION = "Analysis the Given Topic";
    private static final String SYSTEM_PROMPT =
            "You are a document analysis expert. " +
                    "Analyze the given topic thoroughly. " +
                    "Return ONLY a JSON array of findings. No explanation, no markdown. " +
                    "Each finding must have these exact fields: " +
                    "claim, source_url, document_name, page_number, confidence, retrieved_by. " +
                    "confidence must be HIGH, MEDIUM, or LOW. " +
                    "retrieved_by must always be 'document_analysis_agent'. " +
                    "source_url can be null for document analysis. " +
                    "Example: [{\"claim\": \"AI improves diagnosis\", " +
                    "\"source_url\": null, " +
                    "\"document_name\": \"Medical AI Study\", " +
                    "\"page_number\": 5, " +
                    "\"confidence\": \"HIGH\", " +
                    "\"retrieved_by\": \"document_analysis_agent\"}]";

    // Execute
    public List<Finding> execute(String topic) {
        // call Claude API
        String response = claudeApiService.sendSimpleMessage(SYSTEM_PROMPT, topic);
        ObjectMapper mapper = new ObjectMapper();
        try{
            return mapper.readValue(response, new TypeReference<>() {});
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse findings: " + response, e);
        }
    }
}