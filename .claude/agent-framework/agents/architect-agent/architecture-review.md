# Architecture Review Board (ARB)

> Architecture Agent — Self-Critique, Quality Scoring, and Governance

---

## Purpose

Before handing off a design to implementation agents, the Architecture Agent
runs its own design through a virtual Architecture Review Board. This catches
issues before code is written — when changes are cheapest.

---

## Review Process

```
Architecture Design Complete
    ↓
┌─────────────────────────────────────────┐
│      Virtual Architecture Review Board  │
│                                         │
│  1. Security Review                     │
│  2. Performance Review                  │
│  3. Cost Review                         │
│  4. Operations Review                   │
│  5. AI Governance Review                │
│  6. Compliance Review                   │
│  7. Self-Critique (adversarial)         │
│                                         │
│  → Generate Quality Score              │
│  → Identify Improvements               │
│  → Approve or Revise                   │
└─────────────────────────────────────────┘
    ↓
Architecture Revised (if needed)
    ↓
Final Architecture Package → Implementation Agents
```

---

## Review Dimensions

### 1. Security Review

| Check | Question |
|-------|---------|
| Authentication | Is every endpoint protected? |
| Authorization | Is resource-level access enforced? |
| Encryption | Is all PII encrypted at rest and in transit? |
| Secrets | Are secrets in Secrets Manager (not code)? |
| Input validation | Is all user input validated? |
| Audit | Are sensitive actions logged immutably? |
| Threats | Is a threat model documented? |

### 2. Performance Review

| Check | Question |
|-------|---------|
| Latency budget | Is P99 latency defined and achievable? |
| Scaling | Can the system handle 10x traffic? |
| Caching | Is cache-worthy data identified? |
| Pagination | Are all list endpoints paginated? |
| Async | Are long operations offloaded from request thread? |
| Connection pools | Are pools sized correctly? |
| N+1 queries | Are data access patterns efficient? |

### 3. Cost Review

| Check | Question |
|-------|---------|
| Budget fit | Does estimated cost fit the budget? |
| AI costs | Are per-request AI costs estimated? |
| Right-sizing | Are resources matched to actual needs? |
| Scaling costs | What does 10x traffic cost? |
| Waste | Are there always-on resources that could be on-demand? |

### 4. Operations Review

| Check | Question |
|-------|---------|
| Observability | Are golden signals monitored? |
| Alerting | Are alerts defined with runbooks? |
| DR | Is RPO/RTO defined and achievable? |
| Rollback | Can we revert within 5 minutes? |
| Runbook | Do ops teams know what to do when things fail? |
| On-call | Is the system manageable by the team size? |

### 5. AI Governance Review

| Check | Question |
|-------|---------|
| Hallucination | Are high-risk tasks protected by confidence thresholds? |
| Human review | Are irreversible AI decisions human-confirmed? |
| Prompt versioning | Are prompts tracked and testable? |
| Model fallback | What happens when AI is unavailable? |
| Audit trail | Are all AI decisions logged with model version? |
| Fairness | Is the system tested for bias? |

### 6. Compliance Review

| Check | Question |
|-------|---------|
| GDPR | Is right to erasure implemented? |
| Data residency | Is data staying in the correct region? |
| Retention | Are retention policies automated? |
| Consent | Is consent collected and tracked? |
| PII | Is PII classified and protected? |

---

## Self-Critique (Adversarial Review)

The Architecture Agent asks itself adversarial questions:

```
1. "What's the weakest part of this design?"
2. "What happens if the most critical service fails?"
3. "Where could data be lost?"
4. "What's the single point of failure?"
5. "Is this over-engineered for the current phase?"
6. "Is this under-engineered for the next 12 months?"
7. "Would a new developer understand this in 2 hours?"
8. "What would I change if the budget were halved?"
9. "What would I change if traffic doubled overnight?"
10. "Am I using AI where deterministic code would be better?"
```

---

## Architecture Quality Score

After review, produce a quantified quality score:

```markdown
## Architecture Quality Report

| Dimension | Score (1-10) | Notes |
|-----------|-------------|-------|
| Scalability | 9.0 | HPA configured, queue-based processing |
| Security | 9.5 | Encryption, RBAC, audit, PII controls |
| Maintainability | 8.5 | Clean Architecture, SOLID, clear modules |
| Cost Efficiency | 7.5 | AI costs could be optimised (Haiku for classification) |
| Operational Complexity | 7.0 | EKS adds ops burden for small team |
| AI Readiness | 9.5 | Multi-agent, confidence routing, governance |
| Observability | 8.0 | Prometheus + Grafana, needs business dashboards |
| Testability | 9.0 | Hexagonal architecture, dependency injection |
| Compliance | 9.0 | GDPR controls, audit logging, data residency |
| Resilience | 8.5 | Circuit breakers, DLQ, retry policies |

**Overall Score: 8.6 / 10**

### Strengths
- AI architecture is well-designed with confidence routing
- Security and compliance are thorough
- Event-driven design provides good decoupling

### Improvement Opportunities
- Consider ECS over EKS to reduce ops complexity for current team size
- Add business-level metrics dashboard
- AI cost could be reduced by using Haiku for classification stage

### Risk Flags
- None blocking. Proceed to implementation.
```

---

## Quality Score Thresholds

| Score | Decision |
|-------|----------|
| 9.0–10.0 | Excellent. Approve immediately |
| 8.0–8.9 | Good. Approve with noted improvements |
| 7.0–7.9 | Acceptable. Address improvements before production |
| 6.0–6.9 | Needs revision. Fix weak dimensions before implementation |
| < 6.0 | Reject. Fundamental issues — redesign required |

---

## Trade-off Documentation

Every architecture recommendation explicitly states trade-offs:

```markdown
## Decision: Use EKS over ECS

### Benefits
- Full Kubernetes ecosystem (Helm, ArgoCD, HPA, RBAC)
- Multi-team scalability
- Portability (not locked to AWS)

### Drawbacks
- Higher operational complexity
- Requires Kubernetes expertise
- Higher base cost (~$72/month control plane)

### Operational Cost
- Team needs K8s training (2 weeks learning curve)
- On-call complexity increases

### Migration Difficulty
- From ECS: Medium (rewrite task definitions → Helm charts)
- From EKS to elsewhere: Low (standard K8s manifests)

### Long-term Risk
- If team stays small (< 3), EKS may be over-engineered
- Trigger to reconsider: If ops burden exceeds 20% of dev time

### Verdict
Recommended IF team ≥ 4 AND services ≥ 5. Otherwise, use ECS.
```

---

## Future Readiness Assessment

For every design, answer:

| Scenario | Impact on Architecture | Changes Needed |
|----------|----------------------|----------------|
| Users increase 100× | Worker auto-scaling, read replicas | HPA tuning, DB scaling |
| New country added | New document types, different rules | Configuration, not code change |
| Different AI provider | Switch Bedrock model | Abstraction layer (already in place) |
| Multi-region required | RDS cross-region, CloudFront | Infrastructure change, not code |
| Offline processing needed | Queue + batch worker | Already async (minimal change) |

---

## Engineering KPIs (Acceptance Criteria)

Every architecture defines measurable targets:

| KPI | Target | Measurement |
|-----|--------|-------------|
| Availability | 99.9% | CloudWatch uptime |
| API Latency (P99) | < 500ms | Prometheus histogram |
| Upload Success Rate | 99.99% | Custom metric |
| AI Accuracy | > 92% | Accuracy tracking dashboard |
| AI Processing Time | < 30s per document | Pipeline duration metric |
| Error Rate | < 0.1% | 5xx count / total requests |
| Deployment Success | > 95% | CI/CD success rate |
| Mean Time to Recovery | < 30 min | Incident tracking |

These become the fitness functions that the Testing Agent validates.

---

## Checklist

- [ ] All 6 review dimensions evaluated
- [ ] Self-critique questions answered honestly
- [ ] Quality score generated (≥ 8.0 to proceed)
- [ ] Trade-offs documented for every major decision
- [ ] Future readiness scenarios assessed
- [ ] Engineering KPIs defined
- [ ] Improvement opportunities noted (non-blocking)
- [ ] Risk flags raised (blocking if critical)
