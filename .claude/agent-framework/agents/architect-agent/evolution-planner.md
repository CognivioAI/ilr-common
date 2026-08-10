# Evolution Planner

> Architecture Agent — Architecture Evolution and Migration Strategy

---

## Purpose

Instead of prescribing a final architecture on day one, the Architecture Agent
plans a phased evolution that matches team capability, budget, and traffic.
It answers: "What do we build now, and how do we get to the target?"

---

## Evolution Principle

```
Start simple. Evolve when justified by evidence.
```

Never build microservices on day one unless:
- Multiple teams need independent deployment
- Services have clearly different scaling needs
- Domain boundaries are well understood

---

## ILR Platform Evolution Plan

### Phase 1: MVP (Month 1–3)

**Goal**: Validate the product. Get to users fast.

```
Architecture: Modular Monolith (Spring Boot)
Deploy: ECS Fargate (single service)
Database: Single PostgreSQL (RDS)
AI: Direct Bedrock API calls (in-process)
Frontend: React SPA (S3 + CloudFront)
```

| What You Get | What You Skip |
|-------------|---------------|
| Fast development | Microservices complexity |
| Single codebase | Event-driven messaging |
| Simple deployment | Kubernetes |
| Low cost (~$200/month) | Multi-region DR |

**Exit criteria**: Product validated, > 50 active users, revenue signal

---

### Phase 2: Extract Document Service (Month 3–6)

**Goal**: Separate document processing for independent scaling.

```
Before:
  Monolith (handles everything)

After:
  Application Service (users, checklist, UI backend)
      ↓ (API)
  Document Service (upload, OCR, classification)
      ↓ (SQS)
  AI Worker (extraction, validation, recommendations)
```

**Trigger**: Document processing load affects user experience
**Effort**: 2–4 weeks
**Risk**: Low (clear boundary, simple extraction)

---

### Phase 3: Event-Driven Architecture (Month 6–9)

**Goal**: Decouple services, support multiple consumers.

```
Before:
  Service A → REST → Service B

After:
  Service A → SNS → SQS → Service B
                        → Service C (new consumer, zero change to A)
```

**Trigger**: New consumers needed, latency on sync calls unacceptable
**Effort**: 4–6 weeks (add messaging, convert critical paths)
**Risk**: Medium (eventual consistency, idempotency required)

---

### Phase 4: AI Agent Orchestration (Month 9–12)

**Goal**: Multi-agent AI with confidence routing and human-in-the-loop.

```
Before:
  Document Service → Bedrock (single call)

After:
  Document Service → AI Orchestrator
                        ├── Classification Agent
                        ├── OCR Agent
                        ├── Extraction Agent
                        ├── Validation Agent
                        └── Recommendation Agent
```

**Trigger**: AI accuracy needs specialisation, different models per task
**Effort**: 6–8 weeks
**Risk**: Medium (pipeline complexity, monitoring needed)

---

### Phase 5: Platform Scale (Month 12–18)

**Goal**: Multi-team, independently deployable services, full observability.

```
Before:
  ECS services, shared CI/CD

After:
  EKS cluster
  ArgoCD (GitOps)
  Per-service CI/CD
  Full observability stack (Prometheus, Grafana, Alertmanager)
  Service mesh (if > 10 services)
```

**Trigger**: Team > 5 engineers, > 5 services, need advanced deployments
**Effort**: 8–12 weeks
**Risk**: High (operational complexity, team training)

---

### Phase 6: Global SaaS (Month 18+)

**Goal**: Multi-region, enterprise features, compliance at scale.

```
Multi-region deployment (eu-west-2 + us-east-1)
Active-passive DR
Multi-tenancy (shared infra, isolated data)
SOC 2 compliance
Enterprise SSO integration
Advanced RAG (knowledge base per customer)
```

**Trigger**: International customers, enterprise contracts, compliance requirements
**Effort**: 3–6 months
**Risk**: High (data residency, multi-tenancy complexity)

---

## Evolution Decision Framework

| Question | If YES → Evolve | If NO → Stay |
|----------|-----------------|-------------|
| Is current architecture causing production issues? | Split service | Keep monolith |
| Are developers blocked waiting for each other? | Independent services | Keep monolith |
| Do components scale differently? | Separate and scale | Keep together |
| Is AI processing impacting user experience? | Extract AI service | Keep in-process |
| Are event consumers multiplying? | Add event bus | Keep REST |
| Is operational complexity manageable by team? | Advance to next phase | Stay and stabilise |

---

## Phase Transition ADR

Each phase transition requires an ADR:

```markdown
# ADR-{n}: Transition from Phase X to Phase Y

## Trigger
What evidence justified this transition?

## Before Architecture
[Describe current]

## After Architecture
[Describe target]

## Effort Estimate
[Weeks + team size]

## Risks
[What could go wrong during migration]

## Rollback Plan
[How to revert if the migration fails]
```

---

## Checklist

- [ ] Starting architecture matches team size and phase
- [ ] Evolution phases defined (3–6 month increments)
- [ ] Trigger conditions defined per phase
- [ ] Effort estimated per phase
- [ ] Risks identified per phase
- [ ] Rollback strategy per phase transition
- [ ] ADR written for each phase transition
- [ ] Team never over-architects for current phase
