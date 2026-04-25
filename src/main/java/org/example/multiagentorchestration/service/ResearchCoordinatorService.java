package org.example.multiagentorchestration.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ResearchCoordinatorService {

    @Autowired
    private ClaudeApiService claudeApiService;

    //Hub Coordinator
    public String researchCordinator(String topic) {
        List<String> subTopics = taskDecomposition(topic);
        String report = runAgentsAndSynthesis(subTopics, topic);
        int maxIteration = 3;
        int iteration = 0;

        while(iteration<maxIteration){

            String coverageJson = evaluateCoverage(report,subTopics);
            try {
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> coverageMap = mapper.readValue(coverageJson, Map.class);
                if (!(boolean) coverageMap.get("coverageComplete")) {
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
        List<CompletableFuture<String>> webFutures = subTopics.stream()
                .map(t -> CompletableFuture.supplyAsync(() -> webSearchAgent(t)))
                .collect(Collectors.toList());

        List<CompletableFuture<String>> docFutures = subTopics.stream()
                .map(t -> CompletableFuture.supplyAsync(() -> documentAnalysisAgent(t)))
                .collect(Collectors.toList());

        CompletableFuture.allOf(
                Stream.concat(webFutures.stream(), docFutures.stream())
                        .toArray(CompletableFuture[]::new)
        ).join();

        // collect results
        StringBuilder webResult = new StringBuilder();
        StringBuilder docResult = new StringBuilder();

        for (int i = 0; i < subTopics.size(); i++) {
            webResult.append("Subtopic: ").append(subTopics.get(i))
                    .append("\n").append(webFutures.get(i).join()).append("\n\n");
            docResult.append("Subtopic: ").append(subTopics.get(i))
                    .append("\n").append(docFutures.get(i).join()).append("\n\n");
        }
        return synthesisAgent(webResult.toString(), docResult.toString(), topic);
    }

    private List<String> taskDecomposition(String topic){
        String systemPrompt = "you are give a topic." +
                "you have to divide this topic into subtopics" +
                "list all the subtopics that can come under given topic" +
                "Return ONLY a JSON array. No explanation, no preamble, no markdown backticks" +
                "Example output: [\"subtopic1\", \"subtopic2\", \"subtopic3\"]";
        String response = claudeApiService.sendSimpleMessage(systemPrompt, topic);
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(response, List.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse subtopics: " + response, e);
        }
    }

    //spoke 1 web Search Agent
    private String webSearchAgent(String topic){
        String systemPrompt = "you are a web research agent. " +
                "you job to research the given topic." +
                "give clear and direct summary with bullet points." +
                "focus on recent developments, facts." +
                "do not analyze";
        return claudeApiService.sendSimpleMessage(systemPrompt, topic);
    }

    private String documentAnalysisAgent(String topic){
        String systemPrompt = "you are a document analysis expert" +
                "Analysis the given topic thoroughly" +
                "format: structured paragraphs with clear headings" +
                "do not search the web analysis based on you training knowledge";
        return claudeApiService.sendSimpleMessage(systemPrompt, topic);
    }

    private String synthesisAgent(String webResults, String docResults, String topic){
        String systemPrompt = "You are a research synthesis specialist." +
                " You will receive web research results and document analysis." +
                " Your job is to combine them into a comprehensive," +
                " well-structured final report." +
                " Format: executive summary, key findings, conclusion." +
                " Preserve source context — distinguish web findings from analysis";

        String userMessage = """
                Topic: %s
                
                Web Research Results:
                %s
                
                Document Analysis Results:
                %s
                
                Please synthesize these into a comprehensive report.
                """.formatted(topic, webResults, docResults);
        return claudeApiService.sendSimpleMessage(systemPrompt, userMessage);
    }

    private String evaluateCoverage(String report, List<String> subTopics){
        String systemPrompt = "you are a coverage evaluation expert. Take a report and the subTopics" +
                "evaluate if the report covers all the subtopics" +
                "Return ONLY a JSON object. No explanation, no preamble, no markdown backticks." +
                "Example: {\"wellCovered\": [\"solar\"], " +
                "\"partiallyCovered\": [\"wind\"], " +
                "\"missing\": [\"geothermal\"], " +
                "\"coverageComplete\": false}";
        String message = """
                SubTopics
                %s
                
                Report
                %s
                Evaluate Coverage of all topics
                """.formatted(subTopics,report);
        return claudeApiService.sendSimpleMessage(systemPrompt,message);
    }
}
