# Observability Standards

> Version 1.0 | AI-DLC Engineering Handbook
> Purpose: Define monitoring, logging, and tracing for AWS deployments.

---

## 1. Three Pillars

| Pillar | Tool | Purpose |
|--------|------|---------|
| Logs | CloudWatch Logs | What happened |
| Metrics | CloudWatch Metrics + Prometheus | How the system is performing |
| Traces | OpenTelemetry + X-Ray | How requests flow across services |

---

## 2. Logging

### Structure

All logs in structured JSON format:

```json
{
  "timestamp": "2024-01-15T10:30:00.123Z",
  "level": "INFO",
  "service": "order-service",
  "traceId": "abc-123-def",
  "message": "Order created",
  "orderId": "ord-456",
  "customerId": "cust-789",
  "durationMs": 145
}
```

### CloudWatch Log Groups

```
/ecs/<environment>/<service-name>
/eks/<environment>/<namespace>/<service-name>
```

### Retention

| Environment | Retention |
|-------------|-----------|
| Production | 90 days |
| Staging | 30 days |
| Development | 7 days |

---

## 3. Metrics

### Golden Signals (Monitor These for Every Service)

| Signal | Metric | Alert Threshold |
|--------|--------|----------------|
| Latency | P99 response time | > 500ms |
| Traffic | Requests per second | Anomaly detection |
| Errors | 5xx error rate | > 1% |
| Saturation | CPU / Memory utilization | > 80% |

### Custom Metrics

```java
// Micrometer (Spring Boot)
@Timed(value = "order.creation.time", description = "Time to create an order")
public Order createOrder(CreateOrderCommand command) { ... }

Counter.builder("orders.created")
    .tag("status", order.getStatus().name())
    .register(meterRegistry)
    .increment();
```

---

## 4. Distributed Tracing

### OpenTelemetry Setup

- Auto-instrument Spring Boot applications
- Propagate trace context in all HTTP headers
- Export traces to AWS X-Ray or Jaeger
- 100% sampling in staging, 10% in production

### Trace Context Propagation

```
Service A → (traceId in header) → Service B → (traceId in header) → Service C
```

---

## 5. Alerting

### Rules

- Alert on symptoms, not causes
- Every alert must have a runbook
- No alert fatigue — remove noisy alerts
- PagerDuty/OpsGenie for critical alerts
- Slack for warnings

### Alert Levels

| Severity | Response | Example |
|----------|----------|---------|
| Critical (P1) | Immediate (pager) | Service down, data loss |
| High (P2) | 15 minutes | Error rate > 5%, latency spike |
| Medium (P3) | 1 hour | Disk > 80%, cert expiring |
| Low (P4) | Next business day | Non-critical warnings |

---

## 6. Dashboards

Every service must have:

1. **Overview dashboard**: Golden signals at a glance
2. **Detailed dashboard**: Per-endpoint metrics, DB queries, cache hit rates
3. **Infrastructure dashboard**: CPU, memory, network, disk

---

## 7. Checklist

- [ ] Structured JSON logging
- [ ] CloudWatch Log Groups with retention policies
- [ ] Golden signals monitored (latency, traffic, errors, saturation)
- [ ] Custom business metrics (Micrometer)
- [ ] OpenTelemetry tracing configured
- [ ] Trace context propagated across services
- [ ] Alerts defined with runbooks
- [ ] Dashboards per service
- [ ] Log correlation via traceId
