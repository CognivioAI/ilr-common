# Monitoring & Alerting

## Purpose

Define dashboards, alert rules, escalation paths, and on-call practices for proactive incident detection.

---

## Alert Severity Levels

| Severity | Response Time | Action | Notification |
|----------|--------------|--------|--------------|
| P1 - Critical | 5 minutes | Wake on-call, immediate fix | PagerDuty + Slack + Phone |
| P2 - High | 15 minutes | On-call investigates | PagerDuty + Slack |
| P3 - Medium | 1 hour | Fix during business hours | Slack channel |
| P4 - Low | Next sprint | Track and plan | Ticket created |

---

## Alert Rules

### Service Health

```yaml
alerts:
  - name: HighErrorRate
    severity: P1
    condition: error_rate > 5% for 5 minutes
    message: "Service {{ $labels.service }} error rate above 5%"
    runbook: runbooks/high-error-rate.md
    action: page on-call

  - name: HighLatency
    severity: P2
    condition: p95_latency > 1000ms for 5 minutes
    message: "Service {{ $labels.service }} p95 latency above 1s"
    runbook: runbooks/high-latency.md
    action: notify team

  - name: PodCrashLooping
    severity: P1
    condition: kube_pod_container_status_restarts_total increase > 3 in 10m
    message: "Pod {{ $labels.pod }} crash looping"
    runbook: runbooks/pod-crash-loop.md
    action: page on-call

  - name: PodNotReady
    severity: P2
    condition: kube_pod_status_ready == 0 for 5 minutes
    message: "Pod {{ $labels.pod }} not ready"
    action: notify team
```

### Infrastructure

```yaml
  - name: HighCPU
    severity: P3
    condition: cpu_usage > 80% for 15 minutes
    message: "Service {{ $labels.service }} CPU above 80%"
    action: auto-scale or notify

  - name: HighMemory
    severity: P2
    condition: memory_usage > 85% for 10 minutes
    message: "Service {{ $labels.service }} memory above 85% — potential OOM"
    action: notify team, investigate leak

  - name: DiskSpaceLow
    severity: P2
    condition: disk_usage > 80%
    message: "Disk space on {{ $labels.instance }} above 80%"
    action: notify, plan cleanup

  - name: DatabaseConnectionPoolExhausted
    severity: P1
    condition: active_connections / max_connections > 0.9
    message: "Database connection pool near exhaustion"
    action: page on-call
```

### AI Workloads

```yaml
  - name: AIHighLatency
    severity: P2
    condition: ai_request_duration_p95 > 10s for 5 minutes
    message: "AI processing latency above 10s"
    action: check Bedrock status, notify team

  - name: AIHighCost
    severity: P3
    condition: ai_daily_cost > budget_threshold
    message: "AI cost exceeding daily budget"
    action: notify team, review usage

  - name: AIErrorRate
    severity: P2
    condition: ai_error_rate > 10% for 5 minutes
    message: "AI service error rate above 10%"
    action: check model availability, fallback

  - name: DLQGrowing
    severity: P2
    condition: dlq_message_count > 100
    message: "Dead letter queue growing — messages failing"
    action: investigate processing failures
```

---

## Prometheus Alert Rules

```yaml
# prometheus-rules.yaml
apiVersion: monitoring.coreos.com/v1
kind: PrometheusRule
metadata:
  name: ilr-service-alerts
spec:
  groups:
    - name: service.rules
      rules:
        - alert: HighErrorRate
          expr: |
            sum(rate(http_requests_total{status=~"5.."}[5m])) by (service)
            /
            sum(rate(http_requests_total[5m])) by (service)
            > 0.05
          for: 5m
          labels:
            severity: critical
          annotations:
            summary: "High error rate on {{ $labels.service }}"
            runbook_url: "https://wiki.ilr.com/runbooks/high-error-rate"

        - alert: HighP95Latency
          expr: |
            histogram_quantile(0.95,
              sum(rate(http_request_duration_seconds_bucket[5m])) by (le, service)
            ) > 1.0
          for: 5m
          labels:
            severity: warning
          annotations:
            summary: "P95 latency above 1s on {{ $labels.service }}"
```

---

## Grafana Dashboards

### Service Health Dashboard

```yaml
dashboard: Service Health
rows:
  - title: Traffic
    panels:
      - type: stat
        title: Requests/sec
        query: sum(rate(http_requests_total[5m]))
      - type: stat
        title: Error Rate
        query: sum(rate(http_requests_total{status=~"5.."}[5m])) / sum(rate(http_requests_total[5m]))
      - type: stat
        title: P95 Latency
        query: histogram_quantile(0.95, sum(rate(http_request_duration_seconds_bucket[5m])) by (le))
      - type: stat
        title: Active Pods
        query: count(kube_pod_status_phase{phase="Running", namespace="ilr-production"})

  - title: Latency Distribution
    panels:
      - type: heatmap
        title: Request Duration
        query: sum(rate(http_request_duration_seconds_bucket[5m])) by (le)

  - title: Errors
    panels:
      - type: timeseries
        title: Error Rate by Status
        query: sum(rate(http_requests_total{status=~"[45].."}[5m])) by (status)
```

---

## Escalation Matrix

```yaml
escalation:
  P1_critical:
    0_min: Alert fires → PagerDuty → on-call engineer
    5_min: If not acknowledged → escalate to backup on-call
    15_min: If not resolved → notify engineering manager
    30_min: If not resolved → incident commander engaged
    60_min: If not resolved → executive notification
  
  P2_high:
    0_min: Alert fires → Slack #alerts channel
    15_min: On-call investigates
    60_min: If not resolved → escalate to P1
  
  P3_medium:
    0_min: Alert fires → Slack #monitoring channel
    business_hours: Team investigates
    end_of_day: If not resolved → create ticket
```

---

## On-Call Practices

```yaml
on_call:
  rotation: weekly
  schedule:
    primary: 1 engineer
    secondary: 1 backup engineer
  
  tools:
    paging: PagerDuty / Opsgenie
    communication: Slack #incidents
    documentation: Incident notebook
  
  expectations:
    acknowledge: within 5 minutes
    begin_investigation: within 15 minutes
    status_update: every 30 minutes during incident
    post_mortem: within 48 hours of resolution
  
  support:
    runbooks: documented for every P1/P2 alert
    access: pre-provisioned production access
    training: quarterly game days
```

---

## Health Check Endpoints

```yaml
health_checks:
  /actuator/health:
    purpose: Overall service health
    includes: database, disk, custom checks
  
  /actuator/health/liveness:
    purpose: Is the process alive? (K8s liveness probe)
    checks: process is running
    restart_on_failure: yes
  
  /actuator/health/readiness:
    purpose: Can it accept traffic? (K8s readiness probe)
    checks: database connected, dependencies available
    remove_from_lb_on_failure: yes
```
