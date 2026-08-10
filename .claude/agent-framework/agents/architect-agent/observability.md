# Observability Module

> Architecture Agent — Observability Design Discipline

---

## Purpose

Define how the Architecture Agent designs monitoring, alerting, and
observability into every feature from the start.

---

## Observability Requirements Per Feature

For every feature, define:

| Question | Answer Required |
|----------|----------------|
| What metrics indicate success? | Business + technical metrics |
| What does failure look like? | Error conditions + thresholds |
| How do we know it's slow? | Latency SLOs |
| How do we trace a request? | Correlation ID strategy |
| How do we alert? | Who, when, severity |
| What dashboard is needed? | Key visualizations |

---

## SLO Definition

The Architecture Agent defines SLOs (Service Level Objectives) for every service:

| Service | SLO Type | Target |
|---------|----------|--------|
| Document upload | Availability | 99.9% |
| Document upload | Latency (P99) | < 3s |
| AI classification | Latency (P95) | < 10s |
| AI classification | Accuracy | > 92% |
| Checklist update | Freshness | Within 60s of document processing |
| API overall | Error rate | < 0.1% |

---

## Monitoring Architecture

```
Application → Prometheus Metrics → Grafana Dashboards
    │                                      │
    ├→ Structured Logs → CloudWatch → Alerts
    │                                      │
    └→ OpenTelemetry Traces → X-Ray → Trace Explorer
```

---

## Dashboard Design Per Service

### Overview Dashboard (Golden Signals)

- Request rate (per endpoint)
- Error rate (per endpoint)
- Latency P50 / P95 / P99
- Saturation (CPU, memory, connections)

### AI-Specific Dashboard

- AI calls per minute (by agent type)
- AI latency (P50, P95, P99)
- Confidence score distribution
- Manual review queue depth
- Model version in use
- AI cost per hour / per day

### Business Dashboard (ILR Example)

- Documents uploaded per hour
- Processing completion rate
- Average processing time
- Checklist completion rate
- Applications in progress vs completed
- AI accuracy (verified vs auto-accepted)

---

## Alerting Design

| Alert | Condition | Severity | Action |
|-------|-----------|----------|--------|
| High error rate | 5xx > 1% for 5 min | Critical | PagerDuty |
| High latency | P99 > 1s for 5 min | Warning | Slack |
| AI queue depth | DLQ > 0 | Warning | Slack |
| AI manual review pile-up | Queue > 50 for 1 hour | High | Slack + email |
| Pod restarts | > 3 in 15 min | Critical | PagerDuty |
| DB connections exhausted | Active > 90% pool | Critical | PagerDuty |
| S3 upload failures | Any failure | Warning | Slack |
| Certificate expiring | < 14 days | Warning | Slack |

---

## Tracing Strategy

- Every incoming request gets a `traceId`
- `traceId` propagated to all downstream services
- `traceId` propagated into SQS messages
- `traceId` logged in every log line
- `traceId` returned in error responses to user
- AI calls include `traceId` for correlation

---

## Checklist

- [ ] SLOs defined per service
- [ ] Golden signals monitored
- [ ] Dashboards designed (overview + AI + business)
- [ ] Alerting rules with severity and routing
- [ ] Trace context propagated across all services
- [ ] AI-specific monitoring (cost, latency, accuracy)
- [ ] DLQ monitoring and alerts
- [ ] Runbook linked to every critical alert
