# Production Readiness Review

## Purpose

A structured gate before any service enters production. Similar to the Architecture Review Board, this ensures operational excellence standards are met.

---

## Review Process

```
Service Ready for Production
    │
    ▼
Production Readiness Review (DevOps Agent)
    │
    ├── Security Assessment
    ├── Reliability Assessment
    ├── Observability Assessment
    ├── Backup & Recovery Assessment
    ├── Cost Assessment
    ├── Scalability Assessment
    ├── Deployment Assessment
    ├── Compliance Assessment
    │
    ▼
Score: X/10
    │
    ├── ≥ 8.5  → ✅ APPROVED — Ready for production
    ├── 7.0-8.4 → ⚠️ CONDITIONAL — Minor issues, fix within 1 week
    ├── 5.0-6.9 → 🟡 BLOCKED — Significant issues, must fix before deploy
    └── < 5.0   → ❌ REJECTED — Major issues, redesign required
```

---

## Assessment Categories

### 1. Security (Weight: 20%)

```yaml
security_assessment:
  checks:
    container_security:
      - Image scanned (no CRITICAL/HIGH vulnerabilities)
      - Base image from approved registry
      - Running as non-root
      - Read-only root filesystem
      - No privilege escalation
      score_if_all_pass: 10
      blocking_if_fail: true
    
    application_security:
      - SAST scan passed (no critical findings)
      - Dependency scan passed
      - Secret detection passed (no committed secrets)
      - Input validation implemented
      score_if_all_pass: 10
      blocking_if_fail: true
    
    infrastructure_security:
      - IAM role follows least privilege
      - Network policies defined
      - Security groups restrict access
      - Encryption at rest and transit
      - No public endpoints without auth
      score_if_all_pass: 10
      blocking_if_fail: true
    
    authentication_authorization:
      - Auth mechanism implemented and tested
      - RBAC configured
      - Token expiry configured
      score_if_all_pass: 10
  
  scoring:
    10: All checks pass, no findings
    8-9: Minor findings (medium severity), fix planned
    5-7: High findings, must fix
    0-4: Critical findings, BLOCKED
```

### 2. Reliability (Weight: 15%)

```yaml
reliability_assessment:
  checks:
    health_checks:
      - Liveness probe configured
      - Readiness probe configured
      - Startup probe configured (if slow start)
      - Health checks test real dependencies
    
    fault_tolerance:
      - Graceful shutdown implemented (SIGTERM handling)
      - Circuit breakers for external dependencies
      - Retry with exponential backoff
      - Timeout configuration for all outbound calls
      - Dead letter queue for async processing
    
    availability:
      - Multi-AZ deployment
      - Pod Disruption Budget defined
      - Topology spread constraints
      - Minimum replicas ≥ 2 (production)
    
    data_integrity:
      - Database transactions where needed
      - Idempotent operations
      - Optimistic locking where appropriate
  
  scoring:
    10: All fault tolerance patterns implemented
    8-9: Minor gaps (e.g., no circuit breaker for non-critical dependency)
    5-7: Missing health checks or single-AZ
    0-4: No fault tolerance, single point of failure
```

### 3. Observability (Weight: 15%)

```yaml
observability_assessment:
  checks:
    metrics:
      - Prometheus metrics endpoint exposed
      - Request rate/error/duration metrics (RED)
      - Business metrics defined
      - Custom metrics for key operations
    
    logging:
      - Structured logging (JSON)
      - Correlation ID propagated
      - Trace ID included
      - Appropriate log levels
      - No PII/secrets in logs
    
    tracing:
      - OpenTelemetry/X-Ray integrated
      - Spans for key operations
      - Trace propagation across services
    
    dashboards_alerts:
      - Grafana dashboard created
      - Alert rules defined for P1/P2 scenarios
      - Runbooks linked to alerts
      - On-call rotation configured
    
    slos:
      - SLOs defined (availability, latency)
      - Error budget tracking enabled
  
  scoring:
    10: Full observability stack, SLOs defined, all alerts have runbooks
    8-9: Metrics and logging complete, missing some dashboards/runbooks
    5-7: Basic metrics only, no alerts or dashboards
    0-4: No observability
```

### 4. Backup & Recovery (Weight: 10%)

```yaml
backup_recovery_assessment:
  checks:
    data_protection:
      - Database backups configured
      - Point-in-time recovery enabled
      - Backup retention meets policy
      - Cross-region backup (if required)
    
    recovery_validation:
      - Backup restore tested (within last quarter)
      - RTO documented and achievable
      - RPO documented and achievable
      - Recovery runbook exists
    
    state_management:
      - Stateless service design (or state externalized)
      - Configuration in ConfigMap/Secrets (not local)
      - No local disk dependency (or ephemeral)
  
  scoring:
    10: Backups configured, tested, RTO/RPO met in drill
    8-9: Backups configured, not recently tested
    5-7: Backups exist but no recovery testing
    0-4: No backups for stateful data, BLOCKED
```

### 5. Cost (Weight: 10%)

```yaml
cost_assessment:
  checks:
    estimation:
      - Monthly cost estimated
      - Cost breakdown by component
      - Cost within allocated budget
    
    optimization:
      - Resources right-sized (not over-provisioned)
      - Auto-scaling configured (scale down during low traffic)
      - No unnecessary resources provisioned
      - Spot/Savings Plans considered (if applicable)
    
    governance:
      - All resources tagged (Environment, Service, Owner)
      - Budget alerts configured
      - Cost tracking per service enabled
  
  scoring:
    10: Cost estimated, within budget, optimized, tagged
    8-9: Cost estimated, within budget, minor optimization opportunities
    5-7: Cost estimated but exceeds budget, or untagged resources
    0-4: No cost estimate, significantly over budget
```

### 6. Scalability (Weight: 10%)

```yaml
scalability_assessment:
  checks:
    horizontal_scaling:
      - HPA configured with appropriate metrics
      - Minimum and maximum replicas defined
      - Scale-down stabilization configured
      - Load tested to validate scaling behavior
    
    dependencies:
      - Database connection pool sized for max replicas
      - External API rate limits understood
      - Queue consumer scaling independent of producers
    
    performance:
      - Load test completed (target: 2x expected peak)
      - P95 latency within SLO at peak load
      - No resource exhaustion at peak
  
  scoring:
    10: Load tested to 5x, auto-scaling validated, all dependencies scaled
    8-9: Load tested to 2x, HPA configured
    5-7: HPA configured but not load tested
    0-4: No auto-scaling, single replica
```

### 7. Deployment (Weight: 10%)

```yaml
deployment_assessment:
  checks:
    pipeline:
      - CI/CD pipeline complete (build → test → scan → deploy)
      - All quality gates enforced
      - Pipeline success rate > 95%
    
    strategy:
      - Deployment strategy defined (rolling/canary/blue-green)
      - Zero-downtime deployment validated
      - Database migrations backward-compatible
    
    rollback:
      - Rollback procedure documented
      - Rollback tested in staging
      - Automatic rollback on failure (--atomic or circuit breaker)
      - Rollback time < 5 minutes
    
    feature_flags:
      - High-risk features behind flags
      - Kill switch available for new functionality
  
  scoring:
    10: Full CI/CD, canary deployed, rollback tested, feature flagged
    8-9: Full CI/CD, rolling update, rollback documented
    5-7: CI/CD exists but incomplete quality gates
    0-4: Manual deployment process
```

### 8. Compliance (Weight: 10%)

```yaml
compliance_assessment:
  checks:
    data_handling:
      - Data classification documented
      - PII handling follows GDPR requirements
      - Data retention policies applied
      - Encryption at rest and in transit
    
    audit:
      - Audit logging enabled for sensitive operations
      - Access logs configured
      - Audit log retention meets policy
    
    access_control:
      - Service account with minimal permissions
      - No shared credentials
      - Access review documented
  
  scoring:
    10: Full compliance, audit logging, data handling documented
    8-9: Mostly compliant, minor documentation gaps
    5-7: Partial compliance, missing audit logging
    0-4: Non-compliant, BLOCKED for sensitive data services
```

---

## Review Output

### Report Template

```
╔══════════════════════════════════════════════════════════════╗
║         PRODUCTION READINESS REVIEW                         ║
║         Service: document-service v1.2.0                    ║
║         Date: 2025-01-15                                    ║
║         Reviewer: DevOps Agent                              ║
╠══════════════════════════════════════════════════════════════╣
║                                                             ║
║  Category              Score    Status                      ║
║  ─────────────────────────────────────────                  ║
║  Security              9/10     ✅ PASS                      ║
║  Reliability           8/10     ✅ PASS                      ║
║  Observability         7/10     ⚠️ Minor gaps                ║
║  Backup & Recovery     9/10     ✅ PASS                      ║
║  Cost                  8/10     ✅ PASS                      ║
║  Scalability           8/10     ✅ PASS                      ║
║  Deployment            9/10     ✅ PASS                      ║
║  Compliance            9/10     ✅ PASS                      ║
║                                                             ║
║  ─────────────────────────────────────────                  ║
║  OVERALL SCORE:        8.4/10                               ║
║  DECISION:             ⚠️ CONDITIONAL APPROVAL               ║
║                                                             ║
║  Blocking Issues: None                                      ║
║                                                             ║
║  Required Actions (fix within 1 week):                      ║
║  1. Add runbooks for HighMemory and QueueBacklog alerts     ║
║  2. Complete SLO dashboard in Grafana                       ║
║                                                             ║
║  Recommendations (non-blocking):                            ║
║  - Consider ElastiCache for document metadata (latency)     ║
║  - Schedule load test to 5x after 2 weeks of production     ║
║                                                             ║
╚══════════════════════════════════════════════════════════════╝
```

---

## Automated vs Manual Checks

| Check Type | Automation | Manual Review Needed |
|-----------|-----------|---------------------|
| Container scan | Fully automated (CI) | No |
| SAST scan | Fully automated (CI) | No |
| Health check existence | Automated (K8s validation) | No |
| Dashboard exists | Semi-automated (check Grafana API) | Quick verify |
| Runbook quality | Cannot automate | Yes (human review) |
| Load test results | Automated execution | Interpret results |
| Cost estimate accuracy | Automated calculation | Validate assumptions |
| Architecture fit | Cannot automate | Architecture Agent review |

---

## Review Cadence

```yaml
review_cadence:
  new_service: Full review before first production deploy
  major_version: Full review (breaking changes, new dependencies)
  minor_version: Abbreviated review (security + deployment checks)
  patch_version: Automated checks only (no manual review)
  quarterly: Re-review all production services (standards evolve)
```
