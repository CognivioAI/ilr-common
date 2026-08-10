# SRE Practices

## Purpose

Define Site Reliability Engineering practices — SLOs, error budgets, toil reduction, and reliability culture.

---

## Service Level Objectives (SLOs)

### Definition Framework

```yaml
slo_template:
  service: document-service
  slos:
    - name: Availability
      sli: percentage of successful requests (non-5xx)
      target: 99.9%
      window: 30 days (rolling)
      
    - name: Latency
      sli: percentage of requests served within threshold
      target: 95% of requests < 500ms, 99% < 1000ms
      window: 30 days (rolling)
      
    - name: Throughput
      sli: successful requests processed per second
      target: sustain 100 req/s without degradation
      window: peak hours
```

### SLO Targets Per Service

| Service | Availability | Latency (p95) | Error Budget/Month |
|---------|-------------|----------------|-------------------|
| API Gateway | 99.95% | < 200ms | 22 minutes |
| Document Service | 99.9% | < 500ms | 43 minutes |
| AI Worker | 99.5% | < 10s | 3.6 hours |
| React Frontend | 99.9% | < 2s (page load) | 43 minutes |
| Database | 99.99% | < 50ms | 4.3 minutes |

---

## Error Budgets

### Concept

```
Error Budget = 1 - SLO Target

Example:
  SLO = 99.9% availability
  Error Budget = 0.1% = 43 minutes/month of allowed downtime
```

### Error Budget Policy

```yaml
error_budget_policy:
  budget_remaining > 50%:
    action: Normal development velocity
    releases: Standard process
    
  budget_remaining 25-50%:
    action: Increased caution
    releases: Extra review, canary deployments mandatory
    
  budget_remaining < 25%:
    action: Reliability focus
    releases: Only critical fixes and reliability improvements
    new_features: Paused until budget recovers
    
  budget_exhausted:
    action: Freeze
    releases: Emergency fixes only
    team: 100% focused on reliability
    duration: Until budget recovers above 25%
```

### Tracking

```yaml
error_budget_tracking:
  dashboard: Grafana - SLO Dashboard
  metrics:
    - Budget consumed this window (%)
    - Burn rate (how fast are we consuming?)
    - Projected budget at end of window
  
  alerts:
    - Budget consumed > 50%: Notify team lead
    - Budget consumed > 75%: Notify engineering manager
    - Budget consumed > 90%: Engineering-wide notification
    - Burn rate > 14x: Page on-call (will exhaust in hours)
```

---

## Toil Reduction

### Toil Definition

Toil is manual, repetitive, automatable work that scales linearly with service growth and provides no lasting value.

### Toil Identification

| Activity | Is It Toil? | Automation Plan |
|----------|-------------|----------------|
| Manual deployments | ✅ Yes | CI/CD pipeline |
| Restarting crashed pods | ✅ Yes | Auto-restart + fix root cause |
| Certificate renewal | ✅ Yes | cert-manager auto-renewal |
| Secret rotation | ✅ Yes | Automated rotation (Lambda) |
| Scaling for traffic | ✅ Yes | HPA auto-scaling |
| Investigating alerts | ❌ No | (Requires judgment) |
| Capacity planning | ❌ No | (Requires analysis) |
| Post-mortem writing | ❌ No | (Requires reflection) |

### Toil Budget

```yaml
toil_budget:
  target: < 50% of SRE time spent on toil
  current: track weekly
  
  reduction_goals:
    q1: Automate deployments (save 5 hrs/week)
    q2: Automate scaling (save 3 hrs/week)
    q3: Self-healing services (save 4 hrs/week)
    q4: Eliminate manual rollbacks (save 2 hrs/week)
```

---

## Reliability Practices

### Chaos Engineering

```yaml
chaos_engineering:
  tool: Litmus Chaos / AWS Fault Injection Simulator
  
  experiments:
    - name: Pod Kill
      target: Random pod in service
      frequency: Weekly (staging), Monthly (production)
      expected: Service recovers within 30s
    
    - name: Network Latency
      target: Add 500ms to database calls
      frequency: Monthly (staging)
      expected: Circuit breaker activates, graceful degradation
    
    - name: AZ Failure
      target: Simulate AZ outage
      frequency: Quarterly (staging)
      expected: Traffic shifts to surviving AZ
    
    - name: CPU Stress
      target: Consume 90% CPU on service
      frequency: Monthly (staging)
      expected: HPA scales up, performance maintained
  
  rules:
    - Start in staging, graduate to production
    - Always have a kill switch
    - Run during business hours initially
    - Document findings and improvements
```

### Capacity Planning

```yaml
capacity_planning:
  review_frequency: monthly
  
  metrics_to_track:
    - Current utilization (CPU, memory, storage)
    - Growth rate (request volume trend)
    - Headroom (how much spare capacity)
    - Lead time (how long to provision more)
  
  thresholds:
    - 50% utilization: Plan for growth
    - 70% utilization: Begin scaling action
    - 80% utilization: Urgent scaling needed
  
  forecasting:
    method: Linear regression on 90-day trend
    planning_horizon: 3 months
    safety_margin: 30% headroom
```

---

## Reliability Review

### Monthly Reliability Report

```yaml
monthly_report:
  sections:
    - SLO status (all services)
    - Error budget consumption
    - Incidents (count, severity, MTTR)
    - Top toil items
    - Reliability improvements completed
    - Planned reliability work (next month)
  
  metrics:
    - MTTR: Mean Time to Recovery (target: < 30 min)
    - MTTD: Mean Time to Detect (target: < 5 min)
    - Change Failure Rate (target: < 15%)
    - Deployment Frequency (target: daily)
```

---

## Production Readiness Checklist

Before any new service goes to production:

```yaml
production_readiness:
  observability:
    ✓ Metrics exposed (Prometheus)
    ✓ Structured logging (JSON)
    ✓ Distributed tracing enabled
    ✓ Dashboards created
    ✓ Alerts configured (with runbooks)
  
  reliability:
    ✓ SLOs defined
    ✓ Error budget established
    ✓ Health checks implemented
    ✓ Graceful shutdown configured
    ✓ Circuit breakers for dependencies
    ✓ Retry with exponential backoff
    ✓ Rate limiting configured
  
  operations:
    ✓ Runbooks written
    ✓ On-call rotation set up
    ✓ Rollback procedure tested
    ✓ Backup and recovery tested
    ✓ Load testing completed
    ✓ Chaos experiment run (staging)
  
  security:
    ✓ Security scan passed
    ✓ Secrets in Secrets Manager
    ✓ Network policies defined
    ✓ IAM least-privilege verified
```
