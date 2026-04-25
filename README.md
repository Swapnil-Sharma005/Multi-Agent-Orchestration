# Multi-Agent Research Coordinator — Java Spring Boot

A Java Spring Boot implementation of a Hub-and-Spoke multi-agent orchestration system using the Anthropic Claude API.
Built as part of the **Claude Certified Architect — Foundations** exam preparation.

---

## What This Project Demonstrates

This project implements the core multi-agent orchestration patterns tested in **Domain 1 (27%)** of the Claude Certified Architect exam:

- Hub-and-spoke architecture — all communication flows through the coordinator
- Subagent isolation — every piece of context explicitly passed, no shared memory
- Broad task decomposition — avoids narrow decomposition failure pattern
- Parallel subagent spawning — independent agents run simultaneously
- Iterative refinement loop — coordinator detects and fills coverage gaps
- Targeted re-delegation — only missing subtopics re-researched, not full restart

---

## Architecture

```
User Input: "Research renewable energy technologies"
                    ↓
         ResearchCoordinator (Hub)
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
            synthesisAgent()
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
│   └── Message.java                    # Message POJO with role and content
├── service/
│   ├── ClaudeApiService.java           # Claude API client, HTTP calls
│   └── ResearchCoordinatorService.java # Hub + all spokes + refinement loop
├── AgentRunner.java                    # CommandLineRunner entry point
└── resources/
    └── application.properties          # API key configuration
```

---

## Key Components

### Hub — ResearchCoordinator
The central orchestrator that:
1. Decomposes the topic into 5+ subtopics using Claude
2. Spawns all subagents in parallel
3. Aggregates results and evaluates coverage
4. Re-delegates only missing subtopics if gaps found
5. Returns final comprehensive report

### Spokes — Subagents
Each subagent is isolated with its own focused system prompt:

| Subagent | Role | Focus |
|---|---|---|
| `webSearchAgent` | Web research specialist | Recent facts, statistics, developments |
| `documentAnalysisAgent` | Document analysis expert | Deep analysis, technical details |
| `synthesisAgent` | Research synthesis specialist | Combines all results into final report |

---

## Subagent Isolation — Critical Pattern

Subagents have **no shared memory** and **no inherited context**.
Every piece of information they need must be explicitly passed in their prompt.

```java
// WRONG - subagent has no context
private String synthesisAgent(String topic) {
    return claudeApiService.sendSimpleMessage(systemPrompt, topic);
}

// CORRECT - all context explicitly passed
private String synthesisAgent(String webResults, String docResults, String topic) {
    String userMessage = """
            Topic: %s
            Web Research Results: %s
            Document Analysis Results: %s
            Please synthesize these into a comprehensive report.
            """.formatted(topic, webResults, docResults);
    return claudeApiService.sendSimpleMessage(systemPrompt, userMessage);
}
```

---

## Parallel Subagent Spawning

All subtopics run simultaneously using `CompletableFuture`:

```java
// spawn ALL subtopics in parallel
List<CompletableFuture<String>> webFutures = subTopics.stream()
        .map(t -> CompletableFuture.supplyAsync(() -> webSearchAgent(t)))
        .collect(Collectors.toList());

List<CompletableFuture<String>> docFutures = subTopics.stream()
        .map(t -> CompletableFuture.supplyAsync(() -> documentAnalysisAgent(t)))
        .collect(Collectors.toList());

// wait for ALL to complete
CompletableFuture.allOf(
        Stream.concat(webFutures.stream(), docFutures.stream())
                .toArray(CompletableFuture[]::new)
).join();
```

**Why parallel matters:**
- Sequential: subtopic1 → wait → subtopic2 → wait → subtopic3
- Parallel: subtopic1 + subtopic2 + subtopic3 → all simultaneously → much faster

---

## Iterative Refinement Loop

```java
while (iteration < maxIterations) {
    String coverageJson = evaluateCoverage(report, subTopics);
    
    if (coverageComplete) {
        break; // sufficient coverage achieved
    }
    
    // re-delegate ONLY missing subtopics
    List<String> missingTopics = coverageMap.get("missing");
    String missingReport = runAgentsAndSynthesis(missingTopics, topic);
    report = report + "\n\n" + missingReport;
    
    iteration++;
}
```

**Key exam concept:** Re-delegate only missing subtopics — not full restart.
Targeted refinement is more efficient and avoids duplicate coverage.

---

## Coverage Evaluation

The coordinator asks Claude to evaluate coverage and return structured JSON:

```json
{
  "wellCovered": ["solar", "wind"],
  "partiallyCovered": ["geothermal"],
  "missing": ["tidal", "biomass", "fusion"],
  "coverageComplete": false
}
```

This structured output drives the refinement decision — no guessing, no natural language parsing.

---

## Narrow Decomposition — Exam Failure Pattern

The exam specifically tests whether you recognise this failure:

```
Topic: "Renewable Energy Technologies"

WRONG decomposition (too narrow):
["solar", "wind"]  ← misses geothermal, tidal, biomass, fusion

CORRECT decomposition (broad):
["solar", "wind", "geothermal", "tidal", "biomass", "fusion", "hydrogen"]
```

**Diagnostic:** If the final report only covers solar and wind, the root cause is
the **coordinator decomposition** — not the subagents. Subagents only research
what they are assigned. This is the exact diagnostic the exam expects.

---

## Setup

### Prerequisites
- Java 17+
- Maven
- Anthropic API key (`sk-ant-...`) from [console.anthropic.com](https://console.anthropic.com)

### Configuration

Add your API key to `src/main/resources/application.properties`:
```properties
claude.api.key=sk-ant-your-key-here
```

### Run
```bash
mvn spring-boot:run
```

---

## Expected Output

For topic `"renewable energy technologies"`:

```
Coverage gaps found: [tidal, biomass, fusion]
Coverage complete after 1 refinements!

=== FINAL RESEARCH REPORT ===

Executive Summary:
Renewable energy technologies encompass a diverse range of solutions...

Key Findings:
Solar: ...
Wind: ...
Geothermal: ...
Tidal: ...
Biomass: ...
Fusion: ...

Conclusion:
...
```

---

## Key Exam Concepts Demonstrated

| Exam Concept | Implementation |
|---|---|
| Hub-and-spoke architecture | `researchCoordinator` + private spokes |
| Subagent isolation | Explicit context in every prompt |
| Broad decomposition | Claude-driven dynamic subtopic generation |
| Parallel spawning | `CompletableFuture` streams |
| Iterative refinement | while loop with coverage evaluation |
| Targeted re-delegation | Missing subtopics only |
| Safety cap | `maxIterations = 3` |
| Structured output | JSON coverage assessment |

---

## Related Exam Domain

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
- Jackson ObjectMapper (JSON parsing)
- RestTemplate (synchronous HTTP)
- Anthropic Claude API (`claude-sonnet-4-20250514`)