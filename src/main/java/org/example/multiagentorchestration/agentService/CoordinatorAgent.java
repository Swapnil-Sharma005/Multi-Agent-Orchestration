package org.example.multiagentorchestration.agentService;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.multiagentorchestration.model.Finding;
import org.example.multiagentorchestration.service.ClaudeApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

@Service
public class CoordinatorAgent {

    @Autowired
    private ClaudeApiService claudeApiService;

    @Autowired
    private WebSearchAgent webSearchAgent;

    @Autowired
    private DocumentAnalysisAgent documentAnalysisAgent;

    @Autowired
    private SynthesisAgent synthesisAgent;

    @Value("${research.coordinator.max-iterations:3}")
    private int maxIterations;

    private static final String TASK_DECOMPOSITION_PROMPT =
            "You are given a topic. " +
                    "Divide this topic into subtopics. " +
                    "List all the subtopics that can come under the given topic. " +
                    "Return ONLY a JSON array. No explanation, no preamble, no markdown backticks. " +
                    "Example output: [\"subtopic1\", \"subtopic2\", \"subtopic3\"]";

    private static final String COVERAGE_EVALUATION_PROMPT =
            "You are a coverage evaluation expert. " +
                    "You will receive a report and the subtopics. " +
                    "Evaluate if the report covers all the subtopics. " +
                    "Return ONLY a JSON object. No explanation, no preamble, no markdown backticks. " +
                    "Example: {\"wellCovered\": [\"solar\"], " +
                    "\"partiallyCovered\": [\"wind\"], " +
                    "\"missing\": [\"geothermal\"], " +
                    "\"coverageComplete\": false}";

    public String researchCoordinator(String topic) {
        List<String> subTopics = taskDecomposition(topic);
        String report = runAgentsAndSynthesis(subTopics, topic);
        int iteration = 0;

        while(iteration < maxIterations){

            String coverageJson = evaluateCoverage(report,subTopics);
            try {
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> coverageMap = mapper.readValue(coverageJson,
                        new TypeReference<>() {});
                if (!(boolean) coverageMap.get("coverageComplete")) {
                    @SuppressWarnings("unchecked")
                    List<String> listOfMissingTopics = (List<String>) coverageMap.get("missing");
                    String missingTopicsReport = runAgentsAndSynthesis(listOfMissingTopics, topic);
                    report = report + "\n\n" + missingTopicsReport;
                } else break;
            }
            catch(Exception e){
                System.out.println(e.getMessage());
                break;
            }
            iteration++;
        }
        return report;
    }

    private String runAgentsAndSynthesis(List<String> subTopics, String topic){
        // spawn ALL subtopics in parallel simultaneously
        List<CompletableFuture<List<Finding>>> webFutures = subTopics.stream()
                .map(t -> CompletableFuture.supplyAsync(() -> webSearchAgent.execute(t)))
                .toList();

        List<CompletableFuture<List<Finding>>> docFutures = subTopics.stream()
                .map(t -> CompletableFuture.supplyAsync(() -> documentAnalysisAgent.execute(t)))
                .toList();

        CompletableFuture.allOf(
                Stream.concat(webFutures.stream(), docFutures.stream())
                        .toArray(CompletableFuture[]::new)
        ).join();

        List<Finding> allFindings = new ArrayList<>();

        webFutures.forEach(f -> allFindings.addAll(f.join()));
        docFutures.forEach(f -> allFindings.addAll(f.join()));

        return synthesisAgent.execute(allFindings, topic);
    }

    private List<String> taskDecomposition(String topic){
        String response = claudeApiService.sendSimpleMessage(TASK_DECOMPOSITION_PROMPT, topic);
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(response, new TypeReference<>() {});
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse subtopics: " + response, e);
        }
    }
    
    private String evaluateCoverage(String report, List<String> subTopics){
        String message = """
                SubTopics
                %s
                
                Report
                %s
                Evaluate Coverage of all topics
                """.formatted(subTopics,report);
        return claudeApiService.sendSimpleMessage(COVERAGE_EVALUATION_PROMPT, message);
    }
}
