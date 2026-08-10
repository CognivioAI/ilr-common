# AI Agent Design Module

> Architecture Agent — AI Decision Architecture Discipline

---

## Purpose

Define which tasks use AI, which tasks use deterministic code, and how AI agents
are orchestrated within the system. This prevents overuse of LLMs and ensures
human-in-the-loop controls are in place.

---

## AI Decision Matrix

Before assigning a task to an AI agent, classify it:

| Capability | Use AI? | Why |
|-----------|---------|-----|
| OCR / text extraction | Yes (or OCR service) | Pattern recognition, variable layouts |
| File upload | No | Deterministic I/O |
| User authentication | No | Security-critical, deterministic |
| Document classification | Yes | Semantic understanding required |
| Checklist calculation | Mostly No | Rules-based logic, auditable |
| PDF generation | No | Templated output |
| Recommendation generation | Yes | Context-dependent reasoning |
| Data validation (format) | No | Regex / Jakarta Validation |
| Data validation (semantic) | Yes | Understanding intent |
| Status transitions | No | State machine logic |
| Search (keyword) | No | SQL / Elasticsearch |
| Search (semantic) | Yes | Vector similarity |
| Compliance checking | Hybrid | Rules + AI for ambiguity |

**Default rule:** Use deterministic code. Use AI only when deterministic code cannot
solve the problem or would be brittle to maintain.

---

## AI Agent Architecture

Instead of a single large AI service, use specialized agents:

```
AI Orchestrator
│
├── Classification Agent    → Classify document type
├── OCR Agent               → Extract text from images/PDFs
├── Extraction Agent        → Extract structured fields from text
├── Validation Agent        → Validate extracted data semantically
├── Checklist Agent         → Determine which items are satisfied
└── Recommendation Agent    → Generate human-readable guidance
```

### Each Agent Is Responsible For

| Agent | Input | Output | AI Model |
|-------|-------|--------|----------|
| Classification | Raw document | Document type + confidence | Bedrock / Claude |
| OCR | PDF / image | Extracted text + positions | AWS Textract |
| Extraction | Text + document type | Structured JSON fields | Bedrock / Claude |
| Validation | Extracted fields | Validation result + issues | Bedrock / Claude |
| Checklist | Application state | Checklist item status | Rules + Bedrock |
| Recommendation | Application state + history | Guidance text | Bedrock / Claude |

---

## Confidence Routing

Every AI output includes a confidence score.
Route based on confidence:

```
Confidence ≥ 95%
    → Auto-accept result, proceed

Confidence 70–94%
    → Present to user with "please confirm" prompt
    → User confirms or corrects

Confidence < 70%
    → Flag for manual review
    → Notify caseworker
    → AI result stored as suggestion, not applied

AI unavailable / timeout
    → Graceful degradation: store raw input
    → Queue for retry or manual processing
    → Never block the user journey
```

### Implementation

```java
public record AgentResult<T>(
    T data,
    double confidence,
    ConfidenceLevel level,  // AUTO_ACCEPT, CONFIRM_REQUIRED, MANUAL_REVIEW
    String reasoning,
    String modelVersion
) {
    public enum ConfidenceLevel {
        AUTO_ACCEPT,       // ≥ 95%
        CONFIRM_REQUIRED,  // 70–94%
        MANUAL_REVIEW      // < 70%
    }
}
```

---

## Human-in-the-Loop Design

For every AI decision, define:

| Decision | Human Involvement | Trigger |
|----------|------------------|---------|
| Document classification | Review if confidence < 70% | Low confidence |
| Field extraction | Confirm if confidence < 95% | Moderate confidence |
| Compliance check | Always for high-risk findings | Risk level |
| Recommendation | User reads and acts | Always (advisory only) |

Rules:
- AI never makes irreversible decisions without human confirmation
- AI results are stored as `suggestions`, not `facts`, until reviewed
- All AI decisions are logged with model version, confidence, and input hash

---

## AI Orchestrator Design

```java
@Service
public class DocumentProcessingOrchestrator {

    public ProcessingResult process(Document document) {
        // Step 1: Classify
        var classification = classificationAgent.classify(document);
        if (classification.level() == MANUAL_REVIEW) {
            return ProcessingResult.pendingReview(document.id(), classification);
        }

        // Step 2: OCR (if image-based)
        var text = ocrAgent.extract(document);

        // Step 3: Extract fields
        var extraction = extractionAgent.extract(text, classification.data());
        if (extraction.level() == MANUAL_REVIEW) {
            return ProcessingResult.pendingReview(document.id(), extraction);
        }

        // Step 4: Validate
        var validation = validationAgent.validate(extraction.data());

        // Step 5: Update checklist
        checklistAgent.update(document.applicationId(), extraction.data(), validation.data());

        // Publish event
        eventPublisher.publish(new DocumentProcessedEvent(document.id(), extraction.data()));

        return ProcessingResult.complete(document.id());
    }
}
```

---

## Model Selection

| Use Case | Recommended | Alternative | Avoid |
|----------|-------------|-------------|-------|
| OCR | AWS Textract | Google Vision | LLM for pure OCR |
| Text classification | Claude Haiku | Bedrock Titan | GPT (vendor lock) |
| Field extraction | Claude Sonnet | Bedrock Nova | Large model overkill |
| Semantic search | pgvector + Claude | Pinecone | Proprietary only |
| Document generation | Template + data | Claude for summaries | Pure LLM hallucination risk |

---

## Failure Handling

```
AI call fails / times out
    → Log with context
    → Return ProcessingResult.degraded()
    → Queue document for retry (SQS)
    → Notify user: "Processing in progress"
    → Never return error to user for AI transient failure

Retry policy:
    - Max 3 attempts
    - Exponential backoff (2s, 4s, 8s)
    - After 3 failures: route to manual review queue
```

---

## AI Audit Trail

Every AI decision must be auditable:

```java
public record AiDecisionLog(
    String requestId,
    String agentName,
    String modelId,
    String modelVersion,
    String inputHash,      // Hash of input — not the raw PII
    double confidence,
    String decision,
    String reasoning,
    Instant timestamp,
    String reviewedBy      // null if auto-accepted
) {}
```

---

## ADR Questions for AI Architecture

When writing an AI-related ADR, answer:

1. Which tasks are AI vs deterministic code?
2. Which AI model and why?
3. What are the confidence thresholds and routing?
4. Where is human-in-the-loop required?
5. How are AI decisions audited?
6. What happens when AI is unavailable?
7. How is model versioning managed?
8. How is PII handled in prompts?
9. What is the estimated AI inference cost?
10. How is AI performance monitored (accuracy, latency, cost)?
