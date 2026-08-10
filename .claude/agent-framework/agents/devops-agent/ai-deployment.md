# AI Deployment

## Purpose

Define AI workload deployment patterns, model/prompt lifecycle management, AI cost monitoring, and agent deployment orchestration for the ILR platform.

---

## AI Workload Architecture

```
┌──────────────────────────────────────────────────────────┐
│                    AI Platform                             │
│                                                          │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐  │
│  │  API Layer  │───▶│  Queue      │───▶│  AI Worker  │  │
│  │  (Spring    │    │  (SQS)      │    │  (Spring    │  │
│  │   Boot)     │    │             │    │   Boot)     │  │
│  └─────────────┘    └─────────────┘    └──────┬──────┘  │
│                                               │          │
│                                               ▼          │
│                                        ┌─────────────┐   │
│                                        │  AI Provider│   │
│                                        │  (Bedrock / │   │
│                                        │   Claude)   │   │
│                                        └─────────────┘   │
└──────────────────────────────────────────────────────────┘
```

---

## Model Lifecycle Management

### Prompt Versioning

```
Prompt Version
    │
    ▼
Development (testing with sample data)
    │
    ▼
Evaluation (automated quality checks)
    │
    ▼
Staging (real-like data, human review)
    │
    ▼
Approval (team lead sign-off)
    │
    ▼
Production (canary rollout)
    │
    ▼
Monitoring (quality metrics)
```

### Prompt Version Storage

```yaml
prompt_versioning:
  storage: Git (alongside application code)
  structure:
    prompts/
    ├── document-classification/
    │   ├── v1.0.0.txt
    │   ├── v1.1.0.txt
    │   └── current.txt → v1.1.0.txt (symlink)
    ├── summarization/
    │   ├── v1.0.0.txt
    │   └── current.txt → v1.0.0.txt
    └── metadata.yaml
  
  metadata:
    document-classification:
      current_version: "1.1.0"
      model: "anthropic.claude-3-sonnet"
      last_updated: "2025-01-10"
      owner: "ai-team"
      evaluation_score: 0.92
```

### Prompt Deployment Strategy

```yaml
prompt_deployment:
  strategy: canary (always)
  
  stages:
    1. Deploy new prompt version (5% traffic)
    2. Compare quality metrics vs current version
    3. If quality maintained/improved → increase to 25%
    4. Wait 24 hours → full rollout
  
  rollback_trigger:
    - Quality score drops > 10%
    - Error rate increases > 5%
    - Hallucination rate increases
    - Cost per request increases > 50%
  
  a_b_testing:
    enabled: true
    framework: custom (header-based routing)
    metrics_comparison:
      - accuracy
      - latency
      - cost
      - user_satisfaction
```

---

## AI Monitoring

### Key Metrics

```yaml
ai_metrics:
  performance:
    - ai_request_duration_seconds (histogram)
    - ai_requests_total (by model, status, prompt_version)
    - ai_queue_depth
    - ai_processing_backlog_age
  
  quality:
    - ai_confidence_score (histogram)
    - ai_low_confidence_total (< threshold)
    - ai_fallback_to_human_total
    - ai_retry_total
  
  cost:
    - ai_tokens_input_total (by model, service)
    - ai_tokens_output_total (by model, service)
    - ai_cost_dollars_total (by model, service)
    - ai_cost_per_request_average
  
  reliability:
    - ai_provider_errors_total (by provider, error_type)
    - ai_timeout_total
    - ai_circuit_breaker_open_total
    - ai_rate_limit_hit_total
```

### AI Dashboard

```yaml
ai_dashboard:
  row_1_overview:
    - Total AI requests/minute
    - Average latency
    - Error rate
    - Daily cost
  
  row_2_quality:
    - Confidence score distribution
    - Low-confidence rate (needs human review)
    - Prompt version comparison
  
  row_3_cost:
    - Token usage (input vs output)
    - Cost per service
    - Cost trend (daily/weekly)
    - Budget remaining
  
  row_4_model_health:
    - Provider availability
    - Rate limit usage (% of limit)
    - Retry rate
    - Circuit breaker status
```

---

## AI Worker Deployment

### Kubernetes Configuration

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ai-worker
  namespace: ilr-ai
spec:
  replicas: 2
  strategy:
    type: RollingUpdate
  template:
    spec:
      containers:
        - name: ai-worker
          image: 123456789.dkr.ecr.eu-west-2.amazonaws.com/ai-worker:1.0.0
          resources:
            requests:
              cpu: 500m
              memory: 1Gi
            limits:
              cpu: "2"
              memory: 2Gi
          env:
            - name: AI_MODEL
              value: "anthropic.claude-3-sonnet-20240229-v1:0"
            - name: AI_MAX_TOKENS
              value: "4096"
            - name: AI_TEMPERATURE
              value: "0.1"
            - name: QUEUE_URL
              valueFrom:
                configMapKeyRef:
                  name: ai-worker-config
                  key: queue-url
            - name: PROMPT_VERSION
              value: "1.1.0"
```

### Auto-Scaling (Queue-Based)

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: ai-worker
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: ai-worker
  minReplicas: 2
  maxReplicas: 8
  metrics:
    - type: External
      external:
        metric:
          name: sqs_queue_depth
          selector:
            matchLabels:
              queue: ai-processing-queue
        target:
          type: AverageValue
          averageValue: "50"  # Scale up when > 50 messages per worker
```

---

## AI Cost Guardrails

```yaml
cost_guardrails:
  per_request:
    max_tokens: 4096
    max_retries: 2
    timeout: 30s
    alert_if_cost_above: £0.50
  
  daily:
    budget: £50
    action_at_80%: notify team
    action_at_100%: throttle non-critical requests
  
  monthly:
    budget: £1000
    alert_thresholds: [50%, 75%, 90%, 100%]
  
  optimization:
    - Cache identical requests (TTL: 1 hour)
    - Use cheaper model for simple tasks
    - Batch requests where possible
    - Truncate context to minimum needed
    - Monitor and optimize prompt length
```

---

## AI Resilience Patterns

### Circuit Breaker

```yaml
circuit_breaker:
  service: bedrock-client
  failure_threshold: 5 failures in 60 seconds
  recovery_timeout: 30 seconds
  fallback:
    - Return cached result (if available)
    - Queue for retry
    - Fall back to simpler model
    - Escalate to human review
```

### Retry Strategy

```yaml
retry:
  max_attempts: 3
  backoff: exponential (1s, 2s, 4s)
  retryable_errors:
    - ThrottlingException
    - ServiceUnavailableException
    - TimeoutException
  non_retryable:
    - ValidationException
    - AccessDeniedException
    - ContentFilterException
```

### Rate Limiting

```yaml
rate_limiting:
  bedrock:
    requests_per_minute: 100 (account limit)
    tokens_per_minute: 100000 (model limit)
  
  application:
    per_user: 10 AI requests per minute
    per_service: 50 AI requests per minute
    global: 80% of account limit (safety margin)
```

---

## AI Observability

### Tracing AI Requests

```
User Request
    │ TraceId: abc123
    ▼
API Service
    │ SpanId: span-1 (validate, enqueue)
    ▼
SQS Queue
    │ SpanId: span-2 (message in queue: 200ms)
    ▼
AI Worker
    │ SpanId: span-3 (dequeue, prepare prompt)
    ▼
Bedrock API Call
    │ SpanId: span-4 (AI inference: 3000ms)
    │   ├── Input tokens: 1500
    │   ├── Output tokens: 500
    │   └── Cost: £0.008
    ▼
Post-processing
    │ SpanId: span-5 (parse, validate, store: 50ms)
    ▼
Response
```

### Logging AI Interactions

```json
{
  "timestamp": "2025-01-15T10:30:00Z",
  "level": "INFO",
  "service": "ai-worker",
  "event": "AI_REQUEST_COMPLETED",
  "traceId": "abc123",
  "context": {
    "model": "claude-3-sonnet",
    "promptVersion": "1.1.0",
    "inputTokens": 1500,
    "outputTokens": 500,
    "latencyMs": 3200,
    "costUsd": 0.008,
    "confidenceScore": 0.95,
    "classification": "INVOICE"
  }
}
```
