# Multi-Agent Orchestration — Java Spring Boot

A Java Spring Boot implementation of Hub-and-Spoke multi-agent orchestration with structured metadata context passing using the Anthropic Claude API.
Built as part of the **Claude Certified Architect — Foundations** exam preparation.

---

## What This Project Demonstrates

This project implements core multi-agent orchestration patterns tested in **Domain 1 (27%)** of the Claude Certified Architect exam:

- Hub-and-spoke architecture — all communication flows through coordinator
- Subagent isolation — every piece of context explicitly passed, no shared memory
- Broad task decomposition — avoids narrow decomposition failure pattern
- Parallel subagent spawning — independent agents run simultaneously
- Iterative refinement loop — coordinator detects and fills coverage gaps
- Structured metadata — separates content from source attribution
- Attribution preservation — full Finding metadata passed through entire pipeline

---

## Architecture

```
User Input: "Research renewable energy technologies"
                    ↓
           CoordinatorAgent (Hub)
                    ↓
           taskDecomposition()
    ["solar", "wind", "geothermal", "tidal", "biomass", "fusion"]
                    ↓
    ┌───────────────┼───────────────┐
    ↓               ↓               ↓
subtopic1       subtopic2       subtopic3      (all parallel)
web+doc         web+doc         web+doc
    └───────────────┼───────────────┘
                    ↓
           List<Finding> allFindings
           (with full metadata preserved)
                    ↓
           SynthesisAgent
           (cites every claim with source_url + page_number)
                    ↓
           evaluateCoverage()
                    ↓
         ┌──────────────────────┐
         │  coverageComplete?   │
         │  NO → re-delegate    │
         │       missing only   │
         │  YES → return report │
         └──────────────────────┘
```

---

## Project Structure

```
src/main/java/org/example/multiagentorchestration/
├── model/
│   ├── Message.java                    # Message POJO with role and content
│   └── Finding.java                    # Structured finding with metadata
├── agentService/
│   ├── CoordinatorAgent.java           # Hub - orchestrates all agents
│   ├── WebSearchAgent.java             # Spoke 1 - web research
│   ├── DocumentAnalysisAgent.java      # Spoke 2 - document analysis
│   └── SynthesisAgent.java             # Spoke 3 - synthesizes findings
├── service/
│   └── ClaudeApiService.java           # Claude API client
├── AgentRunner.java                    # CommandLineRunner entry point
└── resources/
    └── application.properties          # API key + configuration
```

---

## Finding — Structured Metadata Model

The core of this exercise — separating content from source attribution:

```java
public class Finding {

    public enum Confidence { HIGH, MEDIUM, LOW }

    @JsonProperty("claim")
    private String claim;           // the actual content

    @JsonProperty("source_url")
    private String sourceUrl;       // where it came from

    @JsonProperty("document_name")
    private String documentName;    // document title

    @JsonProperty("page_number")
    private Integer pageNumber;     // page reference

    @JsonProperty("confidence")
    private Confidence confidence;  // HIGH, MEDIUM, LOW

    @JsonProperty("retrieved_by")
    private String retrievedBy;     // which agent found it
}
```

---

## Agent Definitions

Each agent has a clear definition with name, description, system prompt and allowed tools:

### CoordinatorAgent (Hub)
```java
NAME = "coordinator_agent"
DESCRIPTION = "Orchestrates research by delegating to specialized subagents"
ALLOWED_TOOLS = ["web_search_agent", "document_analysis_agent", "synthesis_agent"]
```

### WebSearchAgent (Spoke 1)
```java
NAME = "web_search_agent"
DESCRIPTION = "Web Search the given Topic"
SYSTEM_PROMPT = "You are a web research agent...
                 Return ONLY a JSON array of findings...
                 retrieved_by must always be 'web_search_agent'"
```

### DocumentAnalysisAgent (Spoke 2)
```java
NAME = "document_analysis_agent"
DESCRIPTION = "Analysis the Given Topic"
SYSTEM_PROMPT = "You are a document analysis expert...
                 Return ONLY a JSON array of findings...
                 retrieved_by must always be 'document_analysis_agent'"
```

### SynthesisAgent (Spoke 3)
```java
NAME = "synthesis_agent"
DESCRIPTION = "Research synthesis specialist"
SYSTEM_PROMPT = "...IMPORTANT: Every claim MUST include citation 
                 with source_url and page_number.
                 Never make unsourced claims."
```

---

## Context Passing — Critical Pattern

### Attribution Failure Pattern (WRONG)
```java
// strips metadata — synthesis agent cannot cite sources
private String execute(String webResults, String docResults, String topic)
```

### Correct Pattern — Preserve Full Metadata
```java
// passes complete structured findings — attribution preserved
public String execute(List<Finding> allFindings, String topic)

// coordinator collects ALL findings with metadata intact
List<Finding> allFindings = new ArrayList<>();
webFutures.forEach(f -> allFindings.addAll(f.join()));
docFutures.forEach(f -> allFindings.addAll(f.join()));

// synthesis receives full metadata
return synthesisAgent.execute(allFindings, topic);
```

**Exam rule:** If synthesis agent produces unsourced claims, trace back to whether metadata was passed — do not blame the synthesis agent prompt.

---

## Parallel Subagent Spawning

All subtopics run simultaneously using `CompletableFuture`:

```java
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
```

---

## Iterative Refinement Loop

```java
while (iteration < maxIterations) {
    String coverageJson = evaluateCoverage(report, subTopics);

    if (coverageComplete) break;

    // re-delegate ONLY missing subtopics
    List<String> missingTopics = coverageMap.get("missing");
    String missingReport = runAgentsAndSynthesis(missingTopics, topic);
    report = report + "\n\n" + missingReport;

    iteration++;
}
```

---

## Key Exam Concepts

### allowedTools — Binary Requirement
In Claude Agent SDK, `Agent/Task` must be in `allowedTools` for coordinator to spawn subagents. Without it, spawning fails at SDK level. In Java, `@Autowired` enforces the same constraint structurally.

### fork_session vs Parallel Agent Invocation

| | fork_session | Parallel Agent Invocation |
|---|---|---|
| Purpose | Divergent exploration from shared baseline | Independent tasks running simultaneously |
| Use when | Trying different strategies from same starting point | Tasks are independent, want speed |
| Java equivalent | N/A | CompletableFuture |

### Narrow Decomposition Failure Pattern
```
WRONG: ["solar", "wind"]           ← misses geothermal, tidal, biomass
CORRECT: ["solar", "wind", "geothermal", "tidal", "biomass", "fusion"]
```
Root cause is always coordinator decomposition — not subagents.

---

## Configuration

```properties
# application.properties
claude.api.key=sk-ant-your-key-here
research.coordinator.max-iterations=3
```

---

## Setup

### Prerequisites
- Java 17+
- Maven
- Anthropic API key from [console.anthropic.com](https://console.anthropic.com)

### Run
```bash
mvn spring-boot:run
```

---

## Related Exam Domains

**Domain 1: Agentic Architecture & Orchestration — 27% of exam**

Task Statements covered:
- 1.2 Orchestrate multi-agent systems with coordinator-subagent patterns
- 1.3 Configure subagent invocation, context passing, and spawning
- 1.6 Design task decomposition strategies for complex workflows

---

## Tech Stack

- Java 17
- Spring Boot 3.2.4
- CompletableFuture (parallel execution)
- Jackson ObjectMapper (JSON parsing + serialization)
- RestTemplate (synchronous HTTP)
- Anthropic Claude API (`claude-sonnet-4-20250514`)