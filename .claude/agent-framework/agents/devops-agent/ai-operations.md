# AI Operations

## Purpose

Specialized operational intelligence for AI workloads — monitoring model performance, tracking costs, detecting degradation, managing prompt lifecycle, and ensuring AI reliability.

---

## AI Operational Concerns

```
Standard Service Operations
    +
AI-Specific Operations
    │
    ├── Model Performance (accuracy, confidence, hallucination)
    ├── Token Economics (cost per request, budget consumption)
    ├── Prompt Lifecycle (versioning, A/B testing, rollback)
    ├── Agent Health (tool failures, escalation rate)
    └── Provider Reliability (rate limits, outages, latency)
```

---

## AI Performance Monitoring

### Key Metrics

```yaml
ai_performance_metrics:
  quality:
    - name: ai_confidence_score
      type: histogram
      description: "Confidence score returned by model"
      alert_low: "< 0.7 average over 1 hour → quality degradation"
    
    - name: ai_low_confidence_rate
      type: counter
      description: "Requests where confidence < threshold"
      alert: "> 20% of requests → model struggling with input type"
    
    - name: ai_human_escalation_rate
      type: counter
      description: "Requests escalated to human review"
      alert: "> 30% → model not handling workload appropriately"
    
    - name: ai_classification_accuracy
      type: gauge (from periodic evaluation)
      description: "Accuracy on labeled test set"
      alert: "drops > 5% from baseline → model drift or prompt issue"
  
  reliability:
    - name: ai_request_success_rate
      type: gauge
      description: "Successful AI responses / total requests"
      target: "> 99%"
      alert: "< 95% for 5 minutes → provider issue or prompt error"
    
    - name: ai_timeout_rate
      type: counter
      description: "Requests that timed out waiting for AI"
      alert: "> 5% → provider overloaded or prompt too complex"
    
    - name: ai_retry_rate
      type: counter
      description: "Requests that required retry"
      alert: "> 10% → provider throttling or transient errors"
    
    - name: ai_circuit_breaker_open
      type: gauge
      description: "Circuit breaker state (0=closed, 1=open)"
      alert: "open for > 2 minutes → escalate"
  
  cost:
    - name: ai_tokens_input_total
      type: counter
      labels: [model, service, prompt_version]
    
    - name: ai_tokens_output_total
      type: counter
      labels: [model, service, prompt_version]
    
    - name: ai_cost_per_request
      type: histogram
      alert: "> £0.10 average → investigate expensive requests"
    
    - name: ai_daily_cost_total
      type: gauge
      alert: "> daily_budget × 0.8 → budget warning"
```

---

## AI Anomaly Detection

### Reasoning Playbook

```yaml
ai_anomaly_detection:
  scenario_1_cost_spike:
    signal: "AI daily cost 35% above average"
    reasoning:
      step_1: "Check request volume — is traffic higher?"
        if_yes: "Cost proportional to traffic → normal"
        if_no: proceed
      step_2: "Check average tokens per request"
        if_higher: "Prompts are generating more tokens → investigate prompt change"
        if_normal: proceed
      step_3: "Check for retry storms"
        if_retries_high: "Failures causing retries → fix root cause"
        if_normal: proceed
      step_4: "Check for specific user/document causing loops"
        if_found: "Block/throttle the problematic input"
    resolution:
      - identified: "Large documents causing 10x token usage"
      - action: "Add document summarization pre-processing stage"
      - impact: "Reduce cost by 40% for large documents"
  
  scenario_2_quality_degradation:
    signal: "Confidence scores dropping, human escalation increasing"
    reasoning:
      step_1: "Check if prompt version changed recently"
        if_yes: "Rollback prompt to previous version → verify"
        if_no: proceed
      step_2: "Check input data distribution — new document types?"
        if_new_types: "Model needs examples for new type → update prompt"
        if_normal: proceed
      step_3: "Check model provider status"
        if_degraded: "Provider issue → wait or switch to fallback model"
        if_normal: proceed
      step_4: "Check if model version changed (provider-side)"
        if_changed: "Provider updated model → re-evaluate and adjust prompt"
    resolution:
      - identified: "New document type (invoices) not covered by prompt"
      - action: "Add invoice classification examples to prompt v1.2.0"
      - rollout: "Canary new prompt version"
  
  scenario_3_latency_increase:
    signal: "AI processing time p95 increased from 3s to 8s"
    reasoning:
      step_1: "Check provider status page"
        if_degraded: "Provider capacity issue → activate circuit breaker"
      step_2: "Check request payload sizes"
        if_larger: "Bigger documents → add chunking/summarization"
      step_3: "Check rate limit proximity"
        if_near_limit: "Throttling → reduce concurrent requests"
      step_4: "Check if prompt grew (more context/examples)"
        if_larger_prompt: "Optimize prompt length"
```

---

## Prompt Operations

### Prompt Health Monitoring

```yaml
prompt_monitoring:
  per_prompt_version:
    track:
      - Accuracy (on labeled samples)
      - Average confidence score
      - Token count (input + output)
      - Cost per invocation
      - Latency
      - Human escalation rate
      - Error rate
    
    compare:
      - Current version vs previous version
      - Current version vs baseline
    
    alert_conditions:
      - Accuracy drops > 5% from baseline
      - Cost increases > 20% from previous version
      - Escalation rate increases > 10%

  prompt_rollback:
    trigger:
      - Quality metrics below threshold for 1 hour
      - Cost exceeds budget by > 50%
      - Error rate > 10%
    action:
      - Revert to previous prompt version
      - Notify AI team
      - Record in decision memory
    autonomy: SEMI_AUTO (auto-revert + notify)
```

### A/B Testing Prompts

```yaml
prompt_ab_testing:
  setup:
    control: prompt v1.1.0 (current production)
    experiment: prompt v1.2.0 (candidate)
    traffic_split: 90/10 (control/experiment)
    duration: 7 days minimum
    sample_size: 1000 requests minimum
  
  metrics_to_compare:
    primary: accuracy (on labeled subset)
    secondary: [confidence, cost, latency, escalation_rate]
  
  decision_criteria:
    promote: "Experiment matches or exceeds control on primary metric, no regression on secondary"
    reject: "Experiment worse on primary metric, or significant regression on secondary"
    extend: "Inconclusive — need more data"
  
  automation:
    - Traffic routing via header/percentage
    - Metrics collection per version
    - Statistical significance calculation
    - Auto-promote if criteria met (with approval)
```

---

## AI Provider Operations

### Provider Health

```yaml
provider_health:
  aws_bedrock:
    monitoring:
      - API availability (InvokeModel success rate)
      - Latency (time to first token, total response time)
      - Rate limit consumption (% of account limit)
      - Throttling events
    
    rate_limit_management:
      account_limit: 100 requests/minute (Claude 3 Sonnet)
      application_limit: 80 requests/minute (80% safety margin)
      per_service_allocation:
        document-service: 50 req/min
        ai-worker: 30 req/min
      burst_handling: queue with backpressure
    
    fallback_strategy:
      primary: Claude 3 Sonnet
      fallback_1: Claude 3 Haiku (lower quality, available)
      fallback_2: queue for retry (delayed processing)
      fallback_3: escalate to human (if AI unavailable > 10 min)
    
    outage_response:
      detection: "3 consecutive failures or > 5% error rate in 1 minute"
      action:
        1. Activate circuit breaker (stop sending requests)
        2. Try fallback model
        3. If fallback fails: queue messages for retry
        4. Alert team
        5. Monitor provider status page
      recovery:
        - Half-open circuit breaker after 30 seconds
        - Send 1 test request
        - If successful: close circuit breaker (resume)
        - If failed: keep open, retry in 60 seconds
```

---

## AI Cost Operations

### Real-Time Cost Tracking

```yaml
cost_tracking:
  granularity: per-request
  
  calculation:
    per_request:
      input_cost: input_tokens × model_price_per_input_token
      output_cost: output_tokens × model_price_per_output_token
      total: input_cost + output_cost
    
    pricing (Claude 3 Sonnet via Bedrock):
      input: $3.00 / 1M tokens
      output: $15.00 / 1M tokens
  
  aggregation:
    - Per request (for anomaly detection)
    - Per hour (for trend analysis)
    - Per day (for budget tracking)
    - Per service (for accountability)
    - Per prompt version (for comparison)
  
  budget_enforcement:
    daily_budget: £100
    actions:
      at_60%: "Informational log"
      at_80%: "Warning alert to team"
      at_90%: "Throttle non-critical AI requests"
      at_100%: "Block new AI requests (except critical), alert team"
    
    critical_requests: "Always allowed regardless of budget (safety/compliance)"
    non_critical: "Can be deferred or downgraded to cheaper model"
```

---

## Agent Operations (Multi-Agent AI System)

```yaml
agent_operations:
  monitoring:
    per_agent:
      - Invocations per hour
      - Success rate
      - Average processing time
      - Tool call success rate
      - Human escalation rate
      - Cost per invocation
    
    system_level:
      - Agent-to-agent communication latency
      - Queue depth between agents
      - End-to-end workflow completion rate
      - Total system cost per processed document
  
  failure_modes:
    tool_failure:
      signal: "Agent tool calls failing > 5%"
      action: "Check tool availability, fix or disable tool"
    
    infinite_loop:
      signal: "Agent exceeding max iterations"
      action: "Kill execution, log for investigation"
      prevention: "Max iteration limit (10), timeout (60s)"
    
    hallucination:
      signal: "Output confidence low + doesn't match expected schema"
      action: "Reject output, retry with different prompt framing"
      tracking: "Log and review hallucination instances weekly"
    
    cascade_failure:
      signal: "Multiple agents failing simultaneously"
      action: "Circuit break all agents, fall back to queue + human"
```

---

## AI SLOs

```yaml
ai_slos:
  document_classification:
    accuracy: "> 95% (measured on weekly labeled sample)"
    latency: "p95 < 5 seconds"
    availability: "99.5% (accounts for provider outages)"
    cost_per_document: "< £0.02 average"
    human_escalation: "< 15% of documents"
  
  error_budget:
    availability: "0.5% = 3.6 hours/month of allowed AI downtime"
    quality: "5% = 1 in 20 documents can be wrong"
    policy: "If quality drops below SLO, pause new prompt experiments"
```
