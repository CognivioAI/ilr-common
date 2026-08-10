# Observability

## Purpose

Define the unified observability stack — metrics, logs, and traces — to provide complete visibility into system behavior.

---

## Three Pillars

```
┌─────────────────────────────────────────────────────────────────┐
│                    OBSERVABILITY                                  │
│                                                                  │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐  │
│  │   METRICS    │  │    LOGS      │  │       TRACES         │  │
│  │              │  │              │  │                      │  │
│  │ - Latency    │  │ - Structured │  │ - Request flow       │  │
│  │ - Throughput │  │ - JSON       │  │ - Service-to-service │  │
│  │ - Error rate │  │ - Correlated │  │ - Latency breakdown  │  │
│  │ - Saturation │  │ - Searchable │  │ - Root cause         │  │
│  └──────────────┘  └──────────────┘  └──────────────────────┘  │
│                                                                  │
│  Tool: Prometheus     Tool: CloudWatch    Tool: AWS X-Ray /     │
│        + Grafana            Logs / ELK          OpenTelemetry   │
└─────────────────────────────────────────────────────────────────┘
```

---

## Metrics

### Key Metrics (RED Method)

| Metric | Description | Target |
|--------|-------------|--------|
| **R**ate | Requests per second | Track trend |
| **E**rrors | Error rate (% of 5xx) | < 0.1% |
| **D**uration | Latency (p50, p95, p99) | p95 < 500ms |

### Service Metrics

```yaml
mandatory_metrics:
  api_services:
    - http_requests_total (by method, status, endpoint)
    - http_request_duration_seconds (histogram)
    - http_requests_in_flight
  
  workers:
    - messages_processed_total
    - message_processing_duration_seconds
    - queue_depth
    - dlq_messages_total
  
  ai_services:
    - ai_requests_total (by model, status)
    - ai_request_duration_seconds
    - ai_tokens_used_total (input, output)
    - ai_cost_total
    - ai_confidence_score (histogram)
  
  infrastructure:
    - container_cpu_usage_seconds_total
    - container_memory_usage_bytes
    - container_network_receive_bytes_total
    - kube_pod_status_phase
```

### Prometheus Configuration

```yaml
# Spring Boot - expose Prometheus metrics
management:
  endpoints:
    web:
      exposure:
        include: health,prometheus,info
  metrics:
    tags:
      application: ${spring.application.name}
      environment: ${ENVIRONMENT:dev}
    distribution:
      percentiles-histogram:
        http.server.requests: true
      sla:
        http.server.requests: 100ms,250ms,500ms,1000ms
```

---

## Logs

### Structured Logging Standard

```json
{
  "timestamp": "2025-01-15T10:30:00.123Z",
  "level": "INFO",
  "service": "document-service",
  "traceId": "abc123def456",
  "spanId": "span789",
  "correlationId": "req-uuid-123",
  "userId": "user-456",
  "message": "Document processed successfully",
  "context": {
    "documentId": "doc-789",
    "processingTimeMs": 150,
    "documentType": "PDF"
  }
}
```

### Logging Rules

| Rule | Rationale |
|------|-----------|
| Always JSON format | Machine-parseable, searchable |
| Always include traceId | Correlate across services |
| Always include correlationId | Link related operations |
| Never log PII | GDPR compliance |
| Never log secrets | Security |
| Log at appropriate level | INFO for business, DEBUG for technical |
| Include context object | Structured metadata for filtering |

### Log Levels

| Level | When to Use | Example |
|-------|------------|---------|
| ERROR | Something failed, needs attention | Database connection failed |
| WARN | Unexpected but handled | Retry attempt, fallback used |
| INFO | Business events, state changes | Document processed, user logged in |
| DEBUG | Technical details (dev/troubleshooting) | Query executed, cache hit/miss |
| TRACE | Very verbose (never in production) | Full request/response bodies |

### Spring Boot Configuration

```yaml
logging:
  level:
    root: INFO
    com.ilr: INFO
    org.springframework.web: WARN
  pattern:
    console: '{"timestamp":"%d","level":"%p","service":"${spring.application.name}","traceId":"%X{traceId}","message":"%m"}%n'
```

---

## Traces

### Distributed Tracing Flow

```
React App (Browser)
    │ TraceId: abc123
    ▼
API Gateway
    │ TraceId: abc123, SpanId: span-1
    ▼
Document Service (Spring Boot)
    │ TraceId: abc123, SpanId: span-2
    ├──▶ Database Query (SpanId: span-3)
    ├──▶ S3 Upload (SpanId: span-4)
    └──▶ SQS Publish (SpanId: span-5)
              │
              ▼
AI Worker
    │ TraceId: abc123, SpanId: span-6
    └──▶ Bedrock API Call (SpanId: span-7)
```

### OpenTelemetry Integration

```xml
<!-- Spring Boot - OpenTelemetry dependency -->
<dependency>
    <groupId>io.opentelemetry.instrumentation</groupId>
    <artifactId>opentelemetry-spring-boot-starter</artifactId>
</dependency>
```

```yaml
# OTEL configuration
otel:
  service:
    name: document-service
  exporter:
    otlp:
      endpoint: http://otel-collector:4317
  traces:
    sampler:
      type: parentbased_traceidratio
      arg: 0.1  # Sample 10% in production
```

---

## Dashboards

### Service Dashboard (Per Service)

```yaml
panels:
  row_1_overview:
    - Request rate (req/s)
    - Error rate (%)
    - p95 Latency (ms)
    - Active pods
  
  row_2_details:
    - Request duration histogram
    - Error breakdown by status code
    - Top 10 slowest endpoints
  
  row_3_resources:
    - CPU usage vs limit
    - Memory usage vs limit
    - Network I/O
    - Disk I/O
  
  row_4_dependencies:
    - Database connection pool
    - Queue depth
    - External API latency
    - Cache hit rate
```

### Platform Dashboard (Global)

```yaml
panels:
  - All services health status
  - Total request rate
  - Error rate across all services
  - Infrastructure costs (daily)
  - Deployment frequency
  - Change failure rate
```

---

## Correlation IDs

```yaml
correlation_strategy:
  header: X-Correlation-ID
  generation: at API Gateway (first entry point)
  propagation: all downstream services
  logging: included in every log line
  tracing: mapped to trace context
  
  flow:
    Browser → X-Correlation-ID: uuid-123
    API Gateway → propagates uuid-123
    Document Service → logs with uuid-123
    AI Worker → logs with uuid-123
    Response → X-Correlation-ID: uuid-123
```
