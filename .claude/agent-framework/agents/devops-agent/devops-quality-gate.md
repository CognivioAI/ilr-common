# DevOps Quality Gate

## Purpose

Score every DevOps artifact before handoff — ensuring infrastructure, deployment, and operational artifacts meet quality standards before execution or delivery to other agents.

---

## Quality Gate Architecture

```
DevOps Agent Generates Artifacts
    │
    ▼
┌─────────────────────────────────────┐
│         QUALITY GATE                 │
│                                     │
│  Infrastructure Quality    ─── /10  │
│  Security Quality          ─── /10  │
│  Observability Quality     ─── /10  │
│  Cost Quality              ─── /10  │
│  Deployment Quality        ─── /10  │
│  Documentation Quality     ─── /10  │
│                                     │
│  OVERALL: X/10                      │
│                                     │
│  ≥ 8.0  → ✅ Ready for handoff     │
│  6.0-7.9 → ⚠️ Review required      │
│  < 6.0  → ❌ Blocked (rework)      │
└─────────────────────────────────────┘
    │
    ▼
Handoff to Execution / Other Agents
```

---

## Scoring Categories

### 1. Infrastructure Quality (Weight: 20%)

```yaml
infrastructure_quality:
  checks:
    modularity:
      description: "Terraform uses reusable modules, not monoliths"
      scoring:
        10: "Fully modular, documented, versioned modules"
        7: "Mostly modular, some inline resources"
        4: "Large monolithic files"
        1: "Single file with everything"
    
    state_management:
      description: "Remote state with locking, environment separation"
      scoring:
        10: "Remote state, locking, per-env separation, encrypted"
        7: "Remote state with locking"
        4: "Remote state without locking"
        1: "Local state"
    
    idempotency:
      description: "Running twice produces same result"
      scoring:
        10: "All resources idempotent, lifecycle rules correct"
        7: "Mostly idempotent, minor issues"
        4: "Some resources not idempotent"
    
    naming_conventions:
      description: "Consistent naming, follows standards"
      scoring:
        10: "All resources follow naming pattern, tagged correctly"
        7: "Mostly consistent"
        4: "Inconsistent naming"
    
    validation:
      description: "terraform validate + security scan passes"
      scoring:
        10: "Zero findings from validate + tfsec + checkov"
        7: "Minor warnings only"
        4: "Medium severity findings"
        1: "Critical findings — BLOCKING"
```

### 2. Security Quality (Weight: 25%)

```yaml
security_quality:
  checks:
    least_privilege:
      description: "IAM roles scoped to minimum needed"
      scoring:
        10: "Resource-specific ARNs, no wildcards, documented"
        7: "Mostly scoped, minor wildcards in non-sensitive areas"
        4: "Some broad permissions"
        1: "Wildcard actions or resources — BLOCKING"
    
    encryption:
      description: "All data encrypted at rest and in transit"
      scoring:
        10: "All storage encrypted, TLS 1.3, CMK where needed"
        7: "All encrypted, default keys"
        4: "Some resources not encrypted"
        1: "Production data unencrypted — BLOCKING"
    
    network_isolation:
      description: "Services properly segmented"
      scoring:
        10: "Private subnets, SG references, network policies, no public DB"
        7: "Good isolation, minor gaps"
        4: "Some services exposed unnecessarily"
        1: "Database publicly accessible — BLOCKING"
    
    secrets:
      description: "No plaintext secrets anywhere"
      scoring:
        10: "All secrets in Secrets Manager, rotation configured"
        7: "Secrets in SM, rotation not configured"
        4: "Some secrets in env vars (but not committed)"
        1: "Secrets in code/config — BLOCKING"
    
    container_security:
      description: "Images scanned, non-root, minimal"
      scoring:
        10: "No vulns, non-root, read-only FS, minimal base"
        7: "No critical/high vulns, non-root"
        4: "Medium vulns present"
        1: "Critical vulns or running as root — BLOCKING"
```

### 3. Observability Quality (Weight: 15%)

```yaml
observability_quality:
  checks:
    metrics:
      description: "Prometheus metrics exposed, RED method covered"
      scoring:
        10: "Request/error/duration + custom business metrics"
        7: "Request/error/duration metrics"
        4: "Basic metrics only (CPU/memory)"
        1: "No metrics exposed"
    
    logging:
      description: "Structured JSON, correlation IDs, appropriate levels"
      scoring:
        10: "JSON, trace/correlation IDs, no PII, right levels"
        7: "JSON, correlation IDs"
        4: "Structured but missing correlation"
        1: "Unstructured logging"
    
    alerting:
      description: "Alerts defined with runbooks"
      scoring:
        10: "P1/P2 alerts defined, all linked to runbooks"
        7: "Key alerts defined, some missing runbooks"
        4: "Basic alerts only"
        1: "No alerts configured"
    
    dashboards:
      description: "Grafana dashboard with key panels"
      scoring:
        10: "Full dashboard with traffic, errors, latency, resources"
        7: "Dashboard with key metrics"
        4: "Basic dashboard"
        1: "No dashboard"
    
    slos:
      description: "SLOs defined with error budget tracking"
      scoring:
        10: "SLOs defined, error budget tracked, burn rate alerts"
        7: "SLOs defined"
        4: "Informal targets only"
        1: "No availability targets"
```

### 4. Cost Quality (Weight: 15%)

```yaml
cost_quality:
  checks:
    estimation:
      description: "Cost estimated and within budget"
      scoring:
        10: "Detailed estimate, within budget, alternatives documented"
        7: "Estimate provided, within budget"
        4: "Rough estimate, near budget limit"
        1: "No estimate or over budget"
    
    optimization:
      description: "Resources right-sized, no waste"
      scoring:
        10: "Right-sized, auto-scaling, scheduled scaling, spot where safe"
        7: "Right-sized, auto-scaling configured"
        4: "Some over-provisioning"
        1: "Significantly over-provisioned"
    
    tagging:
      description: "All resources tagged for cost tracking"
      scoring:
        10: "All resources tagged (Environment, Service, Owner, CostCenter)"
        7: "Most resources tagged"
        4: "Inconsistent tagging"
        1: "No tags"
    
    budget_alerts:
      description: "Budget alerts configured"
      scoring:
        10: "Multi-threshold alerts, per-service tracking"
        7: "Budget alerts configured"
        4: "Single threshold alert"
        1: "No budget monitoring"
```

### 5. Deployment Quality (Weight: 15%)

```yaml
deployment_quality:
  checks:
    strategy:
      description: "Appropriate deployment strategy selected"
      scoring:
        10: "Strategy matches risk level, validated in staging"
        7: "Appropriate strategy selected"
        4: "Default strategy without risk assessment"
    
    rollback:
      description: "Rollback plan documented and tested"
      scoring:
        10: "Automated rollback, tested in staging, documented"
        7: "Rollback procedure documented"
        4: "Manual rollback possible but untested"
        1: "No rollback plan — BLOCKING"
    
    pipeline:
      description: "CI/CD pipeline complete with quality gates"
      scoring:
        10: "Full pipeline: build→test→scan→deploy with gates"
        7: "Pipeline exists, most gates present"
        4: "Basic pipeline, missing security scanning"
        1: "No automated pipeline"
    
    zero_downtime:
      description: "Deployment causes no user-facing downtime"
      scoring:
        10: "Proven zero-downtime with health checks and draining"
        7: "Rolling update configured"
        4: "Brief downtime possible"
        1: "Deployment requires maintenance window"
```

### 6. Documentation Quality (Weight: 10%)

```yaml
documentation_quality:
  checks:
    runbooks:
      description: "Operational runbooks for key scenarios"
      scoring:
        10: "Runbooks for all P1/P2 alerts, tested"
        7: "Runbooks for P1 alerts"
        4: "Partial runbooks"
        1: "No operational documentation"
    
    architecture_decision:
      description: "ADRs for significant choices"
      scoring:
        10: "ADR for every major decision with alternatives"
        7: "ADRs for most decisions"
        4: "Some decisions documented"
        1: "No decision documentation"
    
    developer_guide:
      description: "Clear setup and contribution guide"
      scoring:
        10: "Complete onboarding guide, Makefile, docker-compose"
        7: "README with setup instructions"
        4: "Basic README"
        1: "No documentation"
```

---

## Quality Gate Output

```
╔══════════════════════════════════════════════════════════════╗
║           DEVOPS QUALITY GATE                               ║
║           Service: document-service                          ║
║           Artifacts: Terraform + Helm + CI/CD               ║
║           Date: 2026-07-08                                  ║
╠══════════════════════════════════════════════════════════════╣
║                                                             ║
║  Category              Score   Weight   Weighted            ║
║  ─────────────────────────────────────────────              ║
║  Infrastructure         8.5     20%      1.70               ║
║  Security               9.0     25%      2.25               ║
║  Observability          7.0     15%      1.05               ║
║  Cost                   8.5     15%      1.28               ║
║  Deployment             9.0     15%      1.35               ║
║  Documentation          7.5     10%      0.75               ║
║                                                             ║
║  ─────────────────────────────────────────────              ║
║  OVERALL SCORE:         8.4/10                              ║
║  VERDICT:               ✅ READY FOR HANDOFF                ║
║                                                             ║
║  Blocking Issues: 0                                         ║
║  Warnings: 2                                                ║
║    - Observability: Add runbooks for QueueBacklog alert     ║
║    - Documentation: Complete developer onboarding guide     ║
║                                                             ║
║  Strengths:                                                 ║
║    - Excellent security posture (9.0)                       ║
║    - Solid deployment strategy with auto-rollback           ║
║    - Cost well-optimized with right-sizing                  ║
║                                                             ║
╚══════════════════════════════════════════════════════════════╝
```

---

## Gate Rules

```yaml
gate_rules:
  pass (≥ 8.0):
    action: "Proceed to execution / handoff"
    notification: none
  
  conditional (6.0-7.9):
    action: "Proceed with documented improvement plan"
    notification: "Team lead notified"
    deadline: "Address warnings within 1 sprint"
  
  blocked (< 6.0):
    action: "Rework required before proceeding"
    notification: "Team notified, improvement plan required"
    deadline: "Fix before next release"
  
  auto_block_triggers:
    - Any security check scored 1 (critical)
    - No rollback plan
    - Critical vulnerabilities in container
    - Production secrets in code
    - No monitoring configured for production
```

---

## Continuous Quality Tracking

```yaml
quality_tracking:
  trend:
    - Track quality score over time per service
    - Identify improving or degrading trends
    - Compare across services (team benchmarking)
  
  targets:
    - All services ≥ 8.0 by end of quarter
    - Security never below 8.0
    - Zero blocking issues in production services
  
  improvement:
    - Monthly review of lowest-scoring categories
    - Action items for platform team (tooling improvements)
    - Celebrate quality improvements
```
