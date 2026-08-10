# Reference Architecture: AI Platform

> Multi-agent AI with orchestration, confidence routing, and human-in-the-loop.

---

## When to Use

- AI is core to the value proposition
- Multiple AI models needed for different tasks
- Human review required for low-confidence results
- Processing is async (seconds to minutes)
- Audit trail on AI decisions mandatory

---

## Architecture

```
Client (React SPA)
    │
    ▼
API Gateway (ALB + WAF)
    │
    ├── Application Service (user-facing CRUD)
    │       └── PostgreSQL (users, applications, status)
    │
    ├── Document Service (file management)
    │       ├── S3 (original files)
    │       └── PostgreSQL (metadata)
    │
    └── AI Orchestrator (pipeline coordinator)
            ├── SQS (input queue)
            ├── Classification Agent → Bedrock (Claude Haiku)
            ├── Extraction Agent → Bedrock (Claude Sonnet)
            ├── Validation Agent → Bedrock + Rules Engine
            ├── Knowledge Service → pgvector (RAG)
            ├── SQS (stage queues)
            └── PostgreSQL (pipeline state, audit)
```

---

## Event Flow

```
document.uploaded → SQS → AI Orchestrator
    → document.ocr.completed
    → document.classified
    → document.extracted
    → document.validated
    → application.checklist.updated
    → notification.sent
```

---

## Key Decisions

| Decision | Choice | ADR |
|----------|--------|-----|
| AI model | Bedrock (data stays in account) | ADR-001 |
| Pipeline orchestration | Custom (Spring Boot) | ADR-002 |
| Event infrastructure | SQS/SNS | ADR-003 |
| Confidence routing | Threshold-based | ADR-004 |
| Knowledge base | pgvector + RAG | ADR-005 |

---

## Scaling Model

- AI Orchestrator scales on SQS queue depth
- Each agent stage is independently scalable
- Human review queue monitored for depth
- Cost scales linearly with document volume

---

## Risks

- AI model latency spikes → Circuit breaker + DLQ
- Hallucination → Confidence threshold + human review
- Cost spike → Budget alerts + per-document cost cap
- Model deprecation → Abstraction layer, swap-test process
