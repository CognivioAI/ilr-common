# System Design Module

> Architecture Agent — System Design Discipline

---

## Purpose

This module governs how the Architecture Agent approaches system design.
It defines the process for analyzing requirements, generating options,
scoring them, and producing a recommendation.

---

## Step 1: Clarifying Questions

Before designing anything, ask these questions if the answers are unknown.

```
Before I recommend an architecture, I need to understand:

1. Expected monthly active users?
2. Peak concurrent users?
3. Peak uploads or transactions per day?
4. Target P99 response time for user-facing requests?
5. Monthly infrastructure budget?
6. Compliance requirements? (GDPR, HIPAA, SOC2, FCA)
7. Team size and existing technology expertise?
8. Multi-region or single-region?
9. Need to avoid vendor lock-in?
10. Timeline to MVP vs full production?
11. Integration with existing systems?
12. Data retention requirements?
```

Only proceed once these are answered. Unknown answers should be stated as
assumptions and documented in the risk analysis.

---

## Step 2: Architecture Profiles

Map the requirements to one of these profiles before generating options.

### Startup MVP

- Goal: Fastest delivery, lowest cost, smallest team
- Compute: Monolithic Spring Boot or simple microservices
- DB: Single PostgreSQL
- AI: Single LLM call, no orchestration layer
- Deploy: ECS Fargate or simple K8s
- Skip: CQRS, event sourcing, service mesh, multi-region
- When: MVP validation, < 3 devs, < 6 months timeline

### Growth SaaS

- Goal: Balance scalability and maintainability
- Compute: Microservices, containerized
- DB: Per-service PostgreSQL + Redis cache
- AI: Orchestrator with 2–4 agents
- Deploy: EKS, CI/CD, ArgoCD GitOps
- When: Product-market fit achieved, growing team, SaaS billing

### Enterprise

- Goal: Security, compliance, audit trail
- Compute: Microservices with service mesh (Istio)
- DB: Multi-region RDS, encryption at rest, audit tables
- AI: Human-in-the-loop, confidence thresholds, full audit
- Deploy: EKS, multi-AZ, DR runbooks
- When: Enterprise customers, regulated industry, compliance required

### High Throughput

- Goal: Scale under heavy load, long-running jobs
- Compute: Event-driven microservices, async workers
- DB: PostgreSQL for metadata, DynamoDB for high-volume state
- Queue: SQS with DLQ, Kinesis for streaming
- AI: Async agent pipelines, batch processing
- Deploy: EKS with HPA/Karpenter, auto-scaling workers
- When: > 10k concurrent users, large document volumes, batch AI jobs

### AI-First

- Goal: Multiple AI agents, RAG, intelligent automation
- Compute: AI Orchestrator + specialist agents
- DB: PostgreSQL + vector database (pgvector or Pinecone)
- AI: RAG pipeline, confidence routing, human-in-the-loop
- Storage: S3 for originals, OCR output, AI summaries
- Deploy: EKS, async event-driven pipelines
- When: AI at the core of the value proposition

---

## Step 3: Option Generation

Always generate **3–5 options**. Use this table:

| Option | Architecture Style | Profile | Best For |
|--------|-------------------|---------|----------|
| A | Monolithic Spring Boot | MVP | 1-2 devs, fastest delivery |
| B | Spring Boot Microservices | Growth SaaS | Multi-team, independent deployability |
| C | AWS Serverless | Low-ops, variable traffic | Event-driven, pay-per-use |
| D | Event-Driven Microservices | High Throughput / AI-First | Long-running jobs, AI pipelines |
| E | Hybrid | Custom | Specific trade-off between options |

---

## Step 4: Decision Scorecard

Score each option 1–5 on each dimension:

| Dimension | Weight | Option A | Option B | Option C | Option D |
|-----------|--------|----------|----------|----------|----------|
| Development speed | 4 | 5 | 3 | 3 | 2 |
| Infrastructure cost | 4 | 5 | 3 | 4 | 3 |
| Scalability | 3 | 2 | 4 | 5 | 5 |
| Security | 5 | 3 | 4 | 4 | 4 |
| Maintainability | 5 | 3 | 4 | 3 | 4 |
| AI integration | 4 | 2 | 4 | 3 | 5 |
| Operational complexity | 3 | 5 | 3 | 3 | 2 |
| Vendor lock-in risk | 2 | 5 | 4 | 2 | 4 |
| **Weighted Score** | | | | | |

---

## Step 5: Recommendation

```
## Recommended Architecture: [Option X]

### Reason
[2-3 sentences explaining why this option fits the requirements better than alternatives]

### Trade-offs Accepted
- [What we're giving up]
- [Why that's acceptable given the constraints]

### When to Reconsider
- [Trigger conditions that would prompt moving to a different option]
  e.g., "If monthly users exceed 50k, migrate document processing to event-driven."
```

---

## Step 6: Context Diagram (C4 Level 1)

```
[User] ──────────────────────▶ [System Name]
                                      │
                    ┌─────────────────┼─────────────────┐
                    ▼                 ▼                 ▼
             [External System]  [AI Service]    [Storage]
```

Describe every external actor, system, and data store that interacts with the system.

---

## Step 7: Container Diagram (C4 Level 2)

Identify each deployable unit (microservice, database, queue, storage).
For each container, document:
- Name
- Technology
- Responsibility
- Interactions

---

## Step 8: Sequence Diagrams

For each critical flow, produce a sequence diagram.
Always diagram:
- Happy path
- Error path
- Async flows (queue-based)

---

## Twelve Questions Checklist

| # | Question | Answered? |
|---|---------|-----------|
| 1 | What problem are we solving? | |
| 2 | Which services are involved? | |
| 3 | Synchronous or asynchronous? | |
| 4 | What data needs to be stored? | |
| 5 | What events are produced? | |
| 6 | What APIs are exposed? | |
| 7 | What security risks exist? | |
| 8 | What AI agents are required? | |
| 9 | How will it scale? | |
| 10 | How will it be monitored? | |
| 11 | What are the cost implications? | |
| 12 | What are the risks and unknowns? | |
