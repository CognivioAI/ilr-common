# Non-Functional Requirement (NFR) Analyzer

> Architecture Agent — NFR Identification and Quantification

---

## Purpose

Most AI tools miss non-functional requirements. For every feature, this module
identifies, quantifies, and architecturally addresses NFRs. This ensures the
design handles real-world conditions, not just happy-path functionality.

---

## NFR Categories

For every feature, evaluate all ten categories:

| # | Category | Question |
|---|----------|----------|
| 1 | Availability | What uptime SLA is required? |
| 2 | Latency | What response time is acceptable? |
| 3 | Scalability | How many users/requests must it handle? |
| 4 | Security | What data sensitivity? What threats? |
| 5 | Compliance | GDPR, UK GDPR, FCA, PCI-DSS? |
| 6 | Cost | What's the budget constraint? |
| 7 | Maintainability | How easy to change and deploy? |
| 8 | Observability | How do we know it's working? |
| 9 | Disaster Recovery | What's the RPO and RTO? |
| 10 | Accessibility | WCAG level? Screen reader support? |

---

## NFR Template

For each feature, produce:

```markdown
## NFR Analysis: [Feature Name]

### Availability
- SLA: 99.9% (< 8.7 hours downtime/year)
- Multi-AZ: Required
- Failover: Automatic (RDS Multi-AZ, EKS multi-node)

### Latency
- User-facing API P99: < 500ms
- Document upload: < 3s (including S3 transfer)
- AI processing: < 30s (async, user notified on completion)
- Search: < 200ms

### Scalability
- Concurrent users: 500 (peak: 2000)
- Document uploads: 1000/day (peak: 5000/day)
- AI processing: Queue-based, auto-scaling workers
- Database: Read replicas if reads > 80% of traffic

### Security
- Data classification: Restricted (passport, financial data)
- Authentication: JWT (Cognito)
- Authorization: RBAC + resource ownership
- Encryption: AES-256 at rest, TLS 1.3 in transit
- PII fields: Encrypted at column level

### Compliance
- GDPR / UK GDPR: Right to erasure, data portability
- Data residency: UK/EU only (eu-west-2)
- Audit: All access logged immutably
- Retention: 7 years for completed applications

### Cost
- Monthly budget: $X
- AI cost per document: ~$0.22
- Infrastructure baseline: ~$Y/month
- Growth model: Linear with user count

### Maintainability
- Deployment frequency: Daily (CI/CD)
- Change lead time: < 4 hours (simple), < 2 days (complex)
- Rollback time: < 5 minutes
- Tech debt budget: 20% of sprint capacity

### Observability
- Metrics: Golden signals per service (Prometheus)
- Logs: Structured JSON, 90-day retention
- Tracing: OpenTelemetry, trace every request
- Dashboards: Per service + business metrics
- Alerts: PagerDuty for critical, Slack for warnings

### Disaster Recovery
- RPO (Recovery Point Objective): < 1 hour
- RTO (Recovery Time Objective): < 30 minutes
- Backup: Daily automated (RDS), continuous for S3
- DR strategy: Warm standby in secondary AZ

### Accessibility
- Standard: WCAG 2.1 AA
- Screen reader: Full support
- Keyboard navigation: Complete
- Color contrast: 4.5:1 minimum
```

---

## NFR Impact on Architecture

| NFR | Architectural Implication |
|-----|--------------------------|
| 99.9% availability | Multi-AZ, redundant instances, health checks |
| < 500ms latency | Caching, connection pooling, async offloading |
| 2000 concurrent users | HPA, load balancing, connection limits |
| PII / Restricted data | Encryption, access control, audit logging |
| GDPR compliance | Data residency, erasure APIs, consent tracking |
| < 30 min RTO | Multi-AZ, automated failover, tested DR plan |
| WCAG AA | Semantic HTML, ARIA, keyboard nav, testing |

---

## NFR-Driven Decisions

The agent uses NFRs to narrow architecture options:

```
IF availability > 99.9% → Multi-AZ mandatory
IF latency P99 < 200ms → Caching required
IF compliance = GDPR → Data residency constraint
IF scalability > 5000 req/sec → Event-driven mandatory
IF cost < $500/month → Serverless or minimal ECS
IF DR RTO < 15 min → Active-passive or active-active
```

---

## ILR Application Example

```
Feature: Upload Passport

NFRs Identified:
- Availability: 99.9%
- Max upload size: 10MB
- Latency: Upload acknowledgment < 3s, processing < 60s
- Encryption: At rest (S3 SSE-KMS), in transit (TLS 1.3)
- Audit: Every upload logged with userId, timestamp, IP
- GDPR: Document deletable on request
- PII: Passport number, photo — Restricted classification
- Retention: 7 years post-decision
- Accessibility: Upload form WCAG AA compliant

Architecture Impact:
- S3 presigned URL for direct upload (bypasses API for large files)
- SQS queue for async processing
- KMS encryption on S3 bucket
- Audit service receives DocumentUploadedEvent
- Lifecycle policy for retention
- Delete cascade when user exercises right to erasure
```

---

## Checklist

- [ ] All 10 NFR categories evaluated
- [ ] Each NFR quantified (numbers, not "fast" or "secure")
- [ ] Architectural implications documented
- [ ] NFRs drive technology selection (not the other way around)
- [ ] Compliance requirements identified and addressed
- [ ] Accessibility standard specified
- [ ] DR plan (RPO/RTO) defined
- [ ] Cost budget stated
