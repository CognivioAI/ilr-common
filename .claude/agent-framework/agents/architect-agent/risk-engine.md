# Risk Engine

> Architecture Agent — Architectural Risk Analysis

---

## Purpose

Every design has risks. The Architecture Agent identifies, scores, and produces
mitigation strategies for every significant risk before implementation begins.
Unmitigated risks become production incidents.

---

## Risk Scoring

Each risk is scored on two dimensions:

| Dimension | Scale |
|-----------|-------|
| Impact | 1 (Low) → 5 (Critical) |
| Likelihood | 1 (Rare) → 5 (Very Likely) |
| **Risk Score** | Impact × Likelihood |

| Score | Level | Action |
|-------|-------|--------|
| 1–5 | Low | Monitor |
| 6–12 | Medium | Mitigate in next sprint |
| 13–19 | High | Mitigate before go-live |
| 20–25 | Critical | Block release until mitigated |

---

## Risk Register Template

```markdown
## Risk Register: [Feature / System Name]

| # | Risk | Impact | Likelihood | Score | Mitigation | Owner |
|---|------|--------|-----------|-------|-----------|-------|
| R1 | AI service (Bedrock) unavailable | 4 | 2 | 8 | Fallback: queue for retry + manual review | AI Agent |
| R2 | OCR misreads document | 3 | 3 | 9 | Confidence threshold, human review for low scores | AI Agent |
| R3 | Database connection exhaustion | 5 | 2 | 10 | HikariCP pool, connection monitoring, circuit breaker | Java Agent |
| R4 | S3 upload failure | 4 | 2 | 8 | Retry with exponential backoff, DLQ | DevOps Agent |
| R5 | Prompt injection attack | 4 | 2 | 8 | Input sanitisation, prompt boundaries, audit | Security |
| R6 | PII data breach | 5 | 1 | 5 | Encryption, access control, DPIA | Compliance |
```

---

## Risk Categories

### Infrastructure Risks

| Risk | Impact | Likelihood | Mitigation |
|------|--------|-----------|-----------|
| AWS region outage | 5 | 1 | Multi-AZ, DR runbook, RTO target |
| Database failure | 5 | 2 | Multi-AZ RDS, automated failover |
| Container crash loop | 4 | 3 | Health probes, PDB, HPA |
| S3 unavailable | 4 | 1 | Retry, circuit breaker |
| EKS node exhaustion | 3 | 2 | Karpenter auto-provisioning |

### AI Risks

| Risk | Impact | Likelihood | Mitigation |
|------|--------|-----------|-----------|
| AI model unavailable | 4 | 2 | Retry → DLQ → manual queue |
| AI hallucination | 4 | 3 | Confidence threshold, structured output, human review |
| Prompt injection | 4 | 2 | Input sanitisation, prompt isolation |
| Model deprecation | 3 | 2 | Abstraction layer, model swap test |
| AI cost spike | 3 | 3 | Budget alerts, cost per call limit |
| Bias in classification | 4 | 2 | Diverse test dataset, fairness monitoring |

### Security Risks

| Risk | Impact | Likelihood | Mitigation |
|------|--------|-----------|-----------|
| PII data breach | 5 | 1 | Encryption, access control, audit |
| Unauthorised access | 5 | 2 | JWT + RBAC + resource ownership |
| SQL injection | 4 | 1 | Parameterised queries (Spring Data) |
| Compromised dependency | 4 | 2 | OWASP check in CI, Dependabot |
| Stolen JWT token | 4 | 2 | Short expiry (15 min), refresh rotation |

### Compliance Risks

| Risk | Impact | Likelihood | Mitigation |
|------|--------|-----------|-----------|
| GDPR violation | 5 | 2 | Privacy by design, erasure flow, audit |
| Data residency breach | 5 | 2 | Region lock, VPC policy, audit |
| Retention violation | 3 | 2 | Automated lifecycle policies |
| AI decision not explainable | 4 | 3 | Explainability output, audit log, human override |

### Business Continuity Risks

| Risk | Impact | Likelihood | Mitigation |
|------|--------|-----------|-----------|
| Key developer unavailable | 3 | 3 | Knowledge base, documentation, pair programming |
| Third-party API breakdown | 4 | 2 | Circuit breaker, fallback, abstraction layer |
| Data corruption | 5 | 1 | Backups, point-in-time recovery, data validation |
| Performance degradation | 3 | 3 | Load testing, APM, autoscaling |

---

## Risk Response Strategies

| Strategy | When |
|----------|------|
| **Avoid** | Change design to eliminate risk entirely |
| **Mitigate** | Reduce likelihood or impact through controls |
| **Transfer** | Insurance, SLA with provider |
| **Accept** | Low score, cost of mitigation > cost of risk |

---

## Fallback Design (AI Risks)

```
AI service unavailable
    │
    ├── Retry (3 attempts, exponential backoff: 2s, 4s, 8s)
    │       │ fails
    │       ▼
    ├── Degrade gracefully (return partial result)
    │       │ not possible
    │       ▼
    ├── Queue for retry (SQS DLQ, process when available)
    │       │ > 1 hour wait
    │       ▼
    └── Route to manual review queue, notify caseworker
```

---

## Checklist

- [ ] Risk register created for each major feature
- [ ] All risks scored (Impact × Likelihood)
- [ ] Critical risks (score ≥ 20) have mitigation before go-live
- [ ] AI risks addressed (unavailability, hallucination, bias)
- [ ] Security risks addressed
- [ ] Compliance risks addressed
- [ ] Fallback designs documented
- [ ] Risk owner assigned per risk
- [ ] Risk register reviewed before each release
