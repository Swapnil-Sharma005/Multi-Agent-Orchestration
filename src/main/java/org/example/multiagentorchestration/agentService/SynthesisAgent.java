package org.example.multiagentorchestration.agentService;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.multiagentorchestration.model.Finding;
import org.example.multiagentorchestration.service.ClaudeApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SynthesisAgent {

    @Autowired
    private ClaudeApiService claudeApiService;

    //Agentic Definition
    private static final String NAME = "synthesis_agent";
    private static final String DESCRIPTION = "research synthesis";
    private static final String SYSTEM_PROMPT =
            "You are a research synthesis specialist. " +
                    "You will receive web research results and document analysis. " +
                    "Your job is to combine them into a comprehensive, well-structured final report. " +
                    "Format: executive summary, key findings, conclusion. " +
                    "Preserve source context — distinguish web findings from analysis." +
                    "IMPORTANT: Every claim MUST include citation with source_url and page_number." +
                    "Never make unsourced claims.";


    public String execute(List<Finding> allFindings, String topic){
        ObjectMapper mapper = new ObjectMapper();
        String findingsJSON = "";
        try {
            findingsJSON = mapper.writeValueAsString(allFindings);
        }catch(Exception e){
            throw new RuntimeException("Failed to parse: ", e);
        }
        String userMessage = """
                Topic: %s
                
                Research Findings with MetaData
                %s
                
                Please synthesize these into a comprehensive report.
                """.formatted(topic, findingsJSON);
        return claudeApiService.sendSimpleMessage(SYSTEM_PROMPT, userMessage);
    }
}