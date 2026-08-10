# Kubernetes Monitoring Standards

> Version 1.0 | AI-DLC Engineering Handbook
> Purpose: Define monitoring and observability for Kubernetes workloads.

---

## 1. Stack

| Layer | Tool | Purpose |
|-------|------|---------|
| Metrics | Prometheus + Grafana | Time-series metrics and dashboards |
| Logs | Fluentd/Fluent Bit → CloudWatch | Log aggregation |
| Traces | OpenTelemetry → X-Ray/Jaeger | Distributed tracing |
| Alerts | Alertmanager → PagerDuty/Slack | Incident notification |

---

## 2. Prometheus ServiceMonitor

Every service must expose metrics and have a ServiceMonitor:

```yaml
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: order-service-monitor
  labels:
    release: prometheus
spec:
  selector:
    matchLabels:
      app.kubernetes.io/name: order-service
  endpoints:
    - port: http
      path: /actuator/prometheus
      interval: 30s
```

---

## 3. Key Metrics

### Infrastructure Metrics (Node/Pod)

| Metric | Alert Threshold |
|--------|----------------|
| CPU usage | > 80% sustained (5m) |
| Memory usage | > 85% |
| Pod restarts | > 3 in 15 minutes |
| Pod pending | > 0 for 5 minutes |
| Node not ready | Any node |

### Application Metrics (Spring Boot)

| Metric | Alert Threshold |
|--------|----------------|
| HTTP request rate | Anomaly detection |
| HTTP error rate (5xx) | > 1% |
| HTTP latency P99 | > 500ms |
| JVM heap usage | > 80% |
| Connection pool active | > 80% of max |
| Circuit breaker open | Any instance |

---

## 4. Logging

### Container Logging

- Applications log to stdout (JSON structured)
- Fluent Bit collects and forwards to CloudWatch
- Logs enriched with Kubernetes metadata (pod, namespace, labels)

### Log Format

```json
{
  "timestamp": "2024-01-15T10:30:00Z",
  "level": "INFO",
  "service": "order-service",
  "pod": "order-service-abc123",
  "namespace": "production",
  "traceId": "abc-123",
  "message": "Order created",
  "orderId": "ord-456"
}
```

---

## 5. Alerting Rules

```yaml
apiVersion: monitoring.coreos.com/v1
kind: PrometheusRule
metadata:
  name: order-service-alerts
spec:
  groups:
    - name: order-service
      rules:
        - alert: HighErrorRate
          expr: |
            rate(http_server_requests_seconds_count{status=~"5..", service="order-service"}[5m])
            / rate(http_server_requests_seconds_count{service="order-service"}[5m]) > 0.01
          for: 5m
          labels:
            severity: critical
          annotations:
            summary: "High error rate on order-service"
            runbook: "https://wiki.company.com/runbooks/order-service-errors"

        - alert: HighLatency
          expr: |
            histogram_quantile(0.99, 
              rate(http_server_requests_seconds_bucket{service="order-service"}[5m])
            ) > 0.5
          for: 5m
          labels:
            severity: warning
          annotations:
            summary: "P99 latency above 500ms"
```

---

## 6. Dashboards

Every service needs these Grafana dashboards:

1. **Overview**: Request rate, error rate, latency (RED method)
2. **JVM**: Heap, GC, threads, CPU
3. **Database**: Query time, connection pool, active queries
4. **Infrastructure**: Pod CPU/memory, restarts, replicas

---

## 7. Anti-Patterns

- ❌ No metrics endpoint exposed
- ❌ Alerts without runbooks
- ❌ Too many alerts (alert fatigue)
- ❌ No dashboards for service teams
- ❌ Logging PII or secrets
- ❌ Missing traceId in logs

---

## 8. Checklist

- [ ] Prometheus metrics exposed (`/actuator/prometheus`)
- [ ] ServiceMonitor created
- [ ] Golden signal alerts (latency, traffic, errors, saturation)
- [ ] Alerting rules with runbook links
- [ ] Grafana dashboards per service
- [ ] Structured JSON logging to stdout
- [ ] Fluent Bit forwarding to CloudWatch
- [ ] OpenTelemetry tracing configured
- [ ] Pod restart alerts
- [ ] Circuit breaker state alerts
