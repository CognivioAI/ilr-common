# AI Governance Module

> Architecture Agent — AI Governance and Safety Discipline

---

## Purpose

Most AI applications ignore governance. This module ensures every AI decision
is auditable, explainable, and reversible. For an ILR platform, where AI
influences immigration decisions, governance is a legal and ethical obligation.

---

## Governance Pillars

| Pillar | Question |
|--------|---------|
| Reliability | Can AI hallucinate on this task? |
| Confidence | How is accuracy measured? |
| Human oversight | When must a human review? |
| Prompt versioning | How are prompts tracked and tested? |
| Model lifecycle | How are model changes tested and deployed? |
| Explainability | Can we explain why AI reached a decision? |
| Fairness | Can AI produce biased outcomes? |
| Cost control | Is AI spend tracked and capped? |

---

## Hallucination Risk Assessment

Before assigning a task to AI, assess hallucination risk:

| Task | Hallucination Risk | Strategy |
|------|--------------------|----------|
| Document classification | Low (known types) | Confidence threshold + structured output |
| Field extraction (dates, numbers) | Medium | Structured output + regex validation |
| Legal compliance checking | High | Human review mandatory |
| Generating recommendations | High | Advisory only, never auto-applied |
| Checking visa eligibility | Very High | AI assists, human decides |

### Hallucination Mitigation

```
Structured output (JSON schema) → reduces free-form errors
Confidence scoring → routes low confidence to human
Input validation → catches garbage-in scenarios
Cross-validation → second agent checks first agent's output
Human-in-the-loop → mandatory for high-stakes decisions
```

---

## Confidence Measurement

Every AI agent must return a structured result with confidence:

```java
public record AgentResult<T>(
    T data,
    double confidence,           // 0.0–1.0
    ConfidenceLevel level,       // AUTO_ACCEPT, CONFIRM_REQUIRED, MANUAL_REVIEW
    List<String> reasoning,      // Why the AI reached this conclusion
    String modelId,              // Model identifier
    String promptVersion,        // Which prompt was used
    Instant timestamp
) {}
```

### Confidence Thresholds (Configurable Per Task)

| Task | Auto-Accept | Confirm | Manual |
|------|------------|---------|--------|
| Document classification | ≥ 95% | 70–94% | < 70% |
| Field extraction | ≥ 90% | 70–89% | < 70% |
| Compliance check | Never auto | Always confirm | < 80% → senior review |
| Recommendation | Never auto | Always advisory | — |

---

## Prompt Governance

### Prompt Versioning

Every prompt is versioned like code:

```
prompts/
├── classification/
│   ├── v1.0.0.txt
│   ├── v1.1.0.txt     ← current production
│   └── v1.2.0.txt     ← in testing
├── extraction/
│   └── v2.0.0.txt
└── validation/
    └── v1.0.0.txt
```

### Rules

- Prompts are stored in version control
- Production prompts are tagged with semantic version
- Every prompt change goes through: dev → staging → A/B test → production
- Prompt version is stored with every AI output for audit
- Rollback to previous prompt version must be possible in < 5 minutes

### Prompt Change Management

```
Change proposed
    ↓
Regression test suite (test cases with known outputs)
    ↓
Manual review (does new prompt produce correct output?)
    ↓
A/B test in staging (old vs new prompt, compare accuracy)
    ↓
Gradual rollout (10% → 50% → 100%)
    ↓
Monitor accuracy and confidence distribution
    ↓
Full promotion or rollback
```

---

## Model Lifecycle Management

### Model Version Tracking

```java
public record ModelConfig(
    String modelId,           // "anthropic.claude-3-haiku-20240307-v1:0"
    String modelVersion,      // "1.0"
    String promptVersion,     // "v1.1.0"
    Map<String, Double> confidenceThresholds,
    boolean productionActive
) {}
```

### Model Change Process

1. New model available (e.g., Claude upgrade)
2. Run regression tests against golden dataset
3. Compare accuracy, latency, cost vs current model
4. If accuracy drops > 2%: reject
5. If accuracy within acceptable range: shadow deploy (run both, compare)
6. Gradual traffic shift: 10% → 25% → 50% → 100%
7. Monitor confidence distribution and error rates
8. Retain ability to switch back within 1 deployment

---

## Explainability

For every AI decision, the system must be able to explain:
- What input was provided
- What the AI concluded
- Why (key factors / reasoning)
- What alternatives were considered
- What confidence level was assigned

```java
public record ExplainableDecision(
    String decision,
    double confidence,
    List<String> supportingEvidence,  // "Expiry date present", "Photo matches ID type"
    List<String> uncertainties,       // "Signature partially obscured"
    String humanReadableSummary
) {}
```

---

## Fairness and Bias

For an ILR platform:

| Risk | Mitigation |
|------|-----------|
| Nationality bias in document classification | Test against diverse document samples |
| Language bias in text extraction | Test with non-English documents |
| Format bias in field validation | Test with international formats |

### Fairness Testing

- Maintain test dataset covering all document types and nationalities
- Track accuracy by document type and country of issue
- Alert if accuracy drops for any demographic group

---

## AI Audit Trail

Every AI decision is immutably logged:

```sql
CREATE TABLE ai_decision_logs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    agent_name      VARCHAR(50) NOT NULL,
    entity_type     VARCHAR(50) NOT NULL,
    entity_id       UUID NOT NULL,
    model_id        VARCHAR(100) NOT NULL,
    prompt_version  VARCHAR(20) NOT NULL,
    input_hash      VARCHAR(64) NOT NULL,  -- SHA-256 of input (not raw PII)
    decision        JSONB NOT NULL,
    confidence      DECIMAL(4,3) NOT NULL,
    reasoning       JSONB,
    auto_applied    BOOLEAN NOT NULL,
    reviewed_by     VARCHAR(36),           -- NULL if auto-applied
    reviewed_at     TIMESTAMP,
    timestamp       TIMESTAMP NOT NULL DEFAULT NOW()
);
```

---

## Human-in-the-Loop Design

```
AI Result
    ↓
Confidence level check
    │
    ├── AUTO_ACCEPT → Apply immediately, log decision
    │
    ├── CONFIRM_REQUIRED → Present to user:
    │       "Our system classified this as [X] with [Y]% confidence.
    │        Please confirm or correct."
    │       → User accepts or corrects → Log outcome
    │
    └── MANUAL_REVIEW → Route to caseworker queue:
            → Caseworker reviews AI suggestion
            → Caseworker accepts or overrides
            → Override reason logged
            → Feedback used to improve model
```

### Human Override is Always Possible

- AI decisions are always suggestions until human-confirmed or threshold-accepted
- Caseworkers can override any AI decision
- All overrides are logged for model retraining signal

---

## Governance Checklist

- [ ] Hallucination risk assessed per task
- [ ] Confidence thresholds defined per task type
- [ ] Prompts versioned in source control
- [ ] Prompt change process followed
- [ ] Model version tracked with every AI output
- [ ] Model change regression tests pass
- [ ] Explainability output included in AI results
- [ ] Human-in-the-loop for all high-stakes decisions
- [ ] AI audit log records all decisions
- [ ] Fairness test dataset maintained
- [ ] AI cost monitoring configured
- [ ] Rollback procedure documented
