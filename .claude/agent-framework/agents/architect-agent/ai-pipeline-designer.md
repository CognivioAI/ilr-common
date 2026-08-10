# AI Pipeline Designer

> Architecture Agent — AI Processing Pipeline Design Discipline

---

## Purpose

Design multi-stage AI processing pipelines where each stage has defined
inputs, outputs, retry behaviour, timeouts, and confidence handling.
This replaces ad-hoc "call the LLM" thinking with structured AI workflows.

---

## Pipeline Design Template

For each stage, define:

| Field | Description |
|-------|-------------|
| Stage name | Human-readable identifier |
| Input | What data enters this stage |
| Output | What data exits (structured schema) |
| AI or Code | Is this AI-driven or deterministic code? |
| Model | Which AI model (if AI) |
| Confidence threshold | Auto-accept / confirm / manual |
| Timeout | Maximum processing time |
| Retry policy | Max attempts, backoff, DLQ |
| On failure | Fallback behaviour |

---

## ILR Document Processing Pipeline

```
Document Uploaded
    │
    ▼
┌──────────────────────────────────────────────────────────────────┐
│  Stage 1: Pre-processing                                         │
│  AI: No (deterministic)                                          │
│  Input: Raw file                                                 │
│  Output: Normalised file (PDF, JPEG/TIFF)                        │
│  Timeout: 10s                                                    │
│  Retry: 2 attempts                                               │
└────────────────────────────┬─────────────────────────────────────┘
                             │
                             ▼
┌──────────────────────────────────────────────────────────────────┐
│  Stage 2: OCR (Text Extraction)                                  │
│  AI: AWS Textract                                                │
│  Input: Normalised document                                      │
│  Output: Extracted text + bounding boxes (JSON)                  │
│  Confidence: Textract confidence per field                       │
│  Timeout: 30s (async) / 60s (async large docs)                  │
│  Retry: 3 attempts, exponential backoff                          │
│  On failure: Store raw file, flag for manual OCR                 │
└────────────────────────────┬─────────────────────────────────────┘
                             │
                             ▼
┌──────────────────────────────────────────────────────────────────┐
│  Stage 3: Classification                                         │
│  AI: Claude Haiku (fast, cheap)                                  │
│  Input: OCR text + document metadata                             │
│  Output: { type: "PASSPORT", confidence: 0.97, subtype: "UK" }  │
│  Auto-accept: ≥ 95%                                              │
│  Confirm: 70–94%                                                 │
│  Manual: < 70%                                                   │
│  Timeout: 10s                                                    │
│  Retry: 2 attempts                                               │
│  On failure: Flag as UNCLASSIFIED, route to manual               │
└────────────────────────────┬─────────────────────────────────────┘
                             │
                             ▼
┌──────────────────────────────────────────────────────────────────┐
│  Stage 4: Field Extraction                                       │
│  AI: Claude Sonnet (more capable, structured output)             │
│  Input: OCR text + document type                                 │
│  Output: { surname, given_names, nationality, dob, expiry,       │
│            document_number, mrz }                                │
│  Auto-accept: ≥ 90%                                              │
│  Confirm: 70–89%                                                 │
│  Manual: < 70%                                                   │
│  Timeout: 20s                                                    │
│  Retry: 2 attempts, exponential backoff                          │
│  On failure: Store partial extraction, flag missing fields        │
└────────────────────────────┬─────────────────────────────────────┘
                             │
                             ▼
┌──────────────────────────────────────────────────────────────────┐
│  Stage 5: Validation                                             │
│  AI: Hybrid (Rules + Claude for semantic checks)                 │
│  Input: Extracted fields + application data                      │
│  Output: { valid: bool, issues: [...], confidence: 0.88 }        │
│  Rules: Expiry date, MRZ checksum, format checks (deterministic) │
│  AI: Document not expired relative to application date           │
│  Semantic: Name consistency across documents                     │
│  Auto-accept: ≥ 95%                                              │
│  Confirm: 70–94%                                                 │
│  Manual: < 70% OR critical field mismatch                        │
│  Timeout: 15s                                                    │
└────────────────────────────┬─────────────────────────────────────┘
                             │
                             ▼
┌──────────────────────────────────────────────────────────────────┐
│  Stage 6: Checklist Update                                       │
│  AI: Mostly No (rules-based, deterministic)                      │
│  Input: Validated extraction + current checklist                 │
│  Output: Updated checklist items { satisfied: bool, notes: str } │
│  Logic: Rule engine (not LLM — auditable, deterministic)         │
│  Timeout: 5s                                                     │
└────────────────────────────┬─────────────────────────────────────┘
                             │
                             ▼
┌──────────────────────────────────────────────────────────────────┐
│  Stage 7: Recommendation Generation                              │
│  AI: Claude Sonnet                                               │
│  Input: Full application state + knowledge base (RAG)            │
│  Output: Human-readable guidance (advisory only, never binding)  │
│  Auto-apply: Never (advisory)                                    │
│  Human confirms: Always                                          │
│  Timeout: 30s                                                    │
│  On failure: Skip recommendation, proceed without it             │
└────────────────────────────┬─────────────────────────────────────┘
                             │
                             ▼
┌──────────────────────────────────────────────────────────────────┐
│  Stage 8: Summary Generation                                     │
│  AI: Claude Haiku (fast, cost-effective)                         │
│  Input: All stage outputs                                        │
│  Output: Structured processing summary + status                  │
│  Publish: DocumentProcessedEvent                                 │
└──────────────────────────────────────────────────────────────────┘
```

---

## Pipeline Orchestration

```java
@Service
public class DocumentPipelineOrchestrator {

    public PipelineResult execute(PipelineContext context) {
        try {
            context = preprocessingStage.execute(context);
            context = ocrStage.execute(context);

            var classification = classificationStage.execute(context);
            if (classification.requiresManualReview()) {
                return PipelineResult.pendingReview(context.documentId(), classification);
            }
            context = context.withClassification(classification);

            var extraction = extractionStage.execute(context);
            if (extraction.requiresConfirmation()) {
                return PipelineResult.pendingConfirmation(context.documentId(), extraction);
            }
            context = context.withExtraction(extraction);

            context = validationStage.execute(context);
            context = checklistStage.execute(context);

            // Recommendation is optional — non-blocking
            try {
                context = recommendationStage.execute(context);
            } catch (Exception e) {
                log.warn("Recommendation stage failed, continuing documentId={}", context.documentId());
            }

            context = summaryStage.execute(context);
            return PipelineResult.complete(context);

        } catch (StageFailureException e) {
            return PipelineResult.failed(context.documentId(), e.getStage(), e.getMessage());
        }
    }
}
```

---

## Stage Design Contract

Every pipeline stage implements:

```java
public interface PipelineStage {
    String name();
    PipelineContext execute(PipelineContext context) throws StageFailureException;
    boolean isRecoverable();     // Can we skip and continue?
    Duration timeout();
    RetryPolicy retryPolicy();
}
```

---

## Failure Modes

| Failure | Behaviour |
|---------|-----------|
| OCR unavailable | Queue for retry (max 3), then manual |
| Classification low confidence | Route to manual review queue |
| Extraction partial | Store partial, flag missing fields |
| Validation fails | Notify user, explain which rule failed |
| Recommendation fails | Continue without it (non-critical) |
| Full pipeline failure | Store last successful stage, resume from there |

---

## Checklist

- [ ] All pipeline stages defined with input/output schemas
- [ ] AI vs deterministic decision made per stage
- [ ] Confidence thresholds set per stage
- [ ] Timeout defined per stage
- [ ] Retry policy defined per stage
- [ ] Failure behaviour defined (fail safe, not fail silent)
- [ ] Non-critical stages (recommendation) are optional
- [ ] Pipeline state persisted for resumability
- [ ] AI decisions audited at each stage
