# Incident Management

## Purpose

Define incident response procedures, runbook standards, post-mortem process, and communication protocols.

---

## Incident Lifecycle

```
Detection → Triage → Mitigation → Resolution → Post-Mortem
    │          │          │            │             │
 Alerts    Severity    Contain     Root Cause     Learn &
 Users     Assess      Rollback    Fix            Prevent
 Monitoring            Failover    Deploy
```

---

## Incident Severity

| Severity | Impact | Examples | Response |
|----------|--------|----------|----------|
| SEV-1 | Service down, all users affected | Complete outage, data loss | All-hands, war room |
| SEV-2 | Major degradation, many users | Feature broken, high error rate | On-call + team lead |
| SEV-3 | Minor degradation, some users | Slow performance, intermittent errors | On-call investigates |
| SEV-4 | Minimal impact, cosmetic | UI glitch, non-critical alert | Next business day |

---

## Incident Response Process

### 1. Detection & Alert

```yaml
detection:
  automatic:
    - Monitoring alerts (Prometheus/CloudWatch)
    - Health check failures
    - Error rate spikes
  manual:
    - Customer reports
    - Team member observation
    - Dependency status pages
```

### 2. Triage (First 5 Minutes)

```yaml
triage_checklist:
  1. Acknowledge alert (stop escalation)
  2. Assess severity (SEV-1/2/3/4)
  3. Determine blast radius (which users/services affected)
  4. Open incident channel (#incident-YYYY-MM-DD-description)
  5. Declare incident commander (if SEV-1/2)
  6. Initial status update to stakeholders
```

### 3. Mitigation (Contain the Damage)

```yaml
mitigation_options:
  - Rollback to last known good version
  - Scale up resources
  - Enable circuit breaker / fallback
  - Redirect traffic (failover)
  - Disable problematic feature (feature flag)
  - Block malicious traffic (WAF)
  - Restart affected pods/services
```

### 4. Resolution

```yaml
resolution:
  1. Identify root cause
  2. Implement fix
  3. Test fix in staging
  4. Deploy fix to production
  5. Verify fix (monitoring, customer reports)
  6. Close incident
  7. Schedule post-mortem
```

---

## Communication Template

### Status Update (During Incident)

```
🔴 INCIDENT: [Service Name] - [Brief Description]
Severity: SEV-X
Status: Investigating / Mitigating / Resolved
Impact: [Who/what is affected]
Start Time: [HH:MM UTC]
Next Update: [HH:MM UTC]
Commander: [Name]

Current Actions:
- [What we're doing now]

Timeline:
- HH:MM - [Event]
- HH:MM - [Event]
```

---

## Runbook Standard

Every P1/P2 alert must have a linked runbook:

```markdown
# Runbook: [Alert Name]

## Overview
What this alert means and why it matters.

## Impact
What users experience when this fires.

## Quick Diagnosis
1. Check [specific metric/log]
2. Look for [specific pattern]
3. Verify [dependency status]

## Mitigation Steps
1. [Immediate action to reduce impact]
2. [Second action if first doesn't work]
3. [Escalation path]

## Root Cause Investigation
- Common causes: [list]
- Where to look: [logs, metrics, traces]
- Related services: [dependencies]

## Resolution
- How to fix permanently
- Required approvals
- Deployment steps

## Rollback
- Command: `helm rollback [service] [revision]`
- Verification: [how to confirm rollback worked]

## Escalation
- Primary: [team/person]
- Secondary: [backup]
- External: [AWS support, vendor]
```

---

## Post-Mortem Template

```markdown
# Post-Mortem: [Incident Title]

**Date:** YYYY-MM-DD
**Duration:** X hours Y minutes
**Severity:** SEV-X
**Commander:** [Name]
**Author:** [Name]

## Summary
One paragraph describing what happened.

## Impact
- Users affected: [number/percentage]
- Duration of impact: [time]
- Revenue impact: [if applicable]
- SLO budget consumed: [percentage]

## Timeline
| Time (UTC) | Event |
|------------|-------|
| HH:MM | First alert fired |
| HH:MM | On-call acknowledged |
| HH:MM | Root cause identified |
| HH:MM | Mitigation applied |
| HH:MM | Service fully recovered |

## Root Cause
Technical explanation of what went wrong.

## Contributing Factors
- Factor 1 (e.g., missing monitoring)
- Factor 2 (e.g., inadequate testing)

## What Went Well
- Quick detection
- Effective communication
- Successful rollback

## What Went Poorly
- Slow diagnosis
- Missing runbook
- Unclear ownership

## Action Items
| Action | Owner | Priority | Due Date |
|--------|-------|----------|----------|
| Add monitoring for X | @engineer | P1 | YYYY-MM-DD |
| Update runbook for Y | @engineer | P2 | YYYY-MM-DD |
| Add test coverage for Z | @engineer | P2 | YYYY-MM-DD |

## Lessons Learned
What we learned and how we prevent recurrence.
```

---

## Game Days

```yaml
game_days:
  frequency: quarterly
  purpose: Practice incident response in safe environment
  
  scenarios:
    - Database failover
    - Service crash loop
    - Network partition
    - Certificate expiry
    - Secret rotation failure
    - DDoS simulation
    - Region failover
  
  process:
    1. Announce game day (no surprise for first few)
    2. Inject failure in staging
    3. Team responds as if real
    4. Observe and note gaps
    5. Debrief and improve
```
