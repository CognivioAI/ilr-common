# Decision Memory

## Purpose

The DevOps Agent's persistent knowledge store — recording past decisions, outcomes, patterns, and lessons learned. This enables the agent to improve over time and avoid repeating mistakes.

---

## Memory Architecture

```
Decision Memory
    │
    ├── Decision Log (every decision with outcome)
    ├── Pattern Library (recurring successful patterns)
    ├── Incident History (failures and root causes)
    ├── Cost Actuals (predicted vs actual costs)
    ├── Infrastructure Preferences (team/org preferences)
    └── Anti-Patterns (decisions that failed)
```

---

## Decision Log Schema

```yaml
decision_log:
  - id: "DM-2026-001"
    timestamp: "2026-01-15T10:30:00Z"
    category: compute_selection
    context:
      service: document-service
      workload_type: api
      runtime: java-21
      team_size: 2
      budget: £500/month
      nfrs: {availability: 99.9%, latency_p95: 500ms}
    
    decision:
      selected: ECS Fargate
      alternatives_rejected: [EKS, Lambda, App Runner]
      reasoning: "Best cost/ops ratio for small team, meets all NFRs"
      confidence: HIGH
      scoring:
        ecs_fargate: 7.5
        eks: 6.2
        app_runner: 7.0
    
    outcome:
      status: SUCCESS
      measured_at: "2026-02-15"
      actual_cost: £235/month (estimated: £240)
      actual_availability: 99.95%
      actual_latency_p95: 120ms
      issues_encountered: none
      human_overrides: none
    
    lessons:
      - "ECS Fargate well-suited for small Java APIs"
      - "Cost estimate was accurate (within 3%)"
      - "Startup time ~25s acceptable with startup probe"
    
    reuse_conditions:
      - "Java API service"
      - "Team < 5 people"
      - "No Kubernetes-specific requirements"
      - "Budget < £500/month for compute"
```

---

## Pattern Library

### Successful Patterns (Reusable)

```yaml
patterns:
  - id: "PAT-001"
    name: "Java API on ECS Fargate"
    description: "Standard deployment pattern for Spring Boot APIs"
    applicable_when:
      - workload_type: api
      - runtime: java-21
      - team_size: < 5
      - kubernetes_required: false
    stack:
      compute: ECS Fargate (0.5 vCPU, 1GB)
      database: RDS PostgreSQL (db.t4g.medium)
      deployment: Rolling update
      monitoring: Prometheus + Grafana
      ci_cd: GitHub Actions
    success_count: 3
    failure_count: 0
    confidence: HIGH
    monthly_cost_range: £200-£300
    last_used: "2026-06-01"
  
  - id: "PAT-002"
    name: "Queue Worker on ECS Fargate"
    description: "SQS consumer with queue-based auto-scaling"
    applicable_when:
      - workload_type: worker
      - queue_type: sqs
      - processing_time: < 5 minutes per message
    stack:
      compute: ECS Fargate (1 vCPU, 2GB)
      queue: SQS + DLQ
      scaling: KEDA or custom metric (queue depth)
      deployment: Rolling update
    success_count: 2
    failure_count: 0
    confidence: HIGH
    monthly_cost_range: £50-£150
  
  - id: "PAT-003"
    name: "AI Worker with Bedrock"
    description: "Async AI processing via queue + Bedrock"
    applicable_when:
      - workload_type: ai_worker
      - ai_provider: bedrock
      - processing: async (queue-based)
    stack:
      compute: ECS Fargate (0.5 vCPU, 1GB)
      queue: SQS (visibility timeout: 300s)
      ai: Bedrock (Claude 3 Sonnet)
      scaling: Queue depth based
      circuit_breaker: enabled (Bedrock rate limits)
    success_count: 1
    failure_count: 0
    confidence: MEDIUM
    monthly_cost_range: £100-£2000 (AI cost variable)
    notes: "AI token cost dominates — optimize prompts first"
```

---

## Incident History

```yaml
incident_history:
  - id: "INC-2026-001"
    timestamp: "2026-03-10T14:22:00Z"
    service: document-service
    severity: P2
    symptoms: [high_latency, timeout_errors]
    root_cause: "Database connection pool exhausted (max=10, needed=30)"
    resolution: "Increased pool size to 30, added connection pool monitoring"
    time_to_detect: "3 minutes"
    time_to_resolve: "12 minutes"
    prevention:
      - "Always size connection pool based on max replicas × connections_per_pod"
      - "Add connection pool utilization alert (> 80% warning)"
    related_decision: "DM-2026-001"
    
  - id: "INC-2026-002"
    timestamp: "2026-04-05T09:15:00Z"
    service: ai-worker
    severity: P3
    symptoms: [queue_backlog_growing, ai_timeout_errors]
    root_cause: "Bedrock rate limit hit (100 req/min exceeded)"
    resolution: "Added rate limiting in application, implemented backoff"
    time_to_detect: "5 minutes"
    time_to_resolve: "45 minutes (code change + deploy)"
    prevention:
      - "Always implement client-side rate limiting below provider limit"
      - "Add Bedrock rate limit usage metric (% of limit consumed)"
      - "Pre-calculate expected load vs limit before deployment"
```

---

## Cost Actuals (Estimation Accuracy)

```yaml
cost_tracking:
  - decision_id: "DM-2026-001"
    service: document-service
    estimated_monthly: £240
    actual_monthly:
      month_1: £235
      month_2: £248
      month_3: £242
    accuracy: 97% (within 3%)
    variance_cause: "Slight traffic growth in month 2"
  
  - decision_id: "DM-2026-003"
    service: ai-worker
    estimated_monthly: £500
    actual_monthly:
      month_1: £780
      month_2: £650 (after optimization)
      month_3: £520 (after caching)
    accuracy: 64% (initially underestimated)
    variance_cause: "AI token usage higher than projected"
    lesson: "Add 50% buffer to AI cost estimates. Actual usage always exceeds synthetic test projections."
    
  estimation_model_updates:
    - "AI costs: multiply estimate by 1.5x safety factor"
    - "Database costs: estimates consistently accurate (within 10%)"
    - "Networking costs: add £10-20 for VPC endpoints overlooked"
```

---

## Infrastructure Preferences

```yaml
preferences:
  organization:
    cloud: AWS (eu-west-2)
    iac: Terraform
    containers: Docker (multi-stage, Alpine-based)
    orchestration: ECS Fargate (preferred until > 5 services)
    ci_cd: GitHub Actions
    monitoring: Prometheus + Grafana
    secrets: AWS Secrets Manager
    gitops: ArgoCD (when K8s adopted)
  
  team:
    kubernetes_experience: basic
    terraform_experience: intermediate
    aws_experience: intermediate
    preferred_languages: [java, typescript]
    deployment_comfort: rolling updates (canary = learning)
  
  constraints:
    budget: £3000/month total
    compliance: GDPR, UK GDPR
    data_residency: eu-west-2 only
    working_hours: UK business hours (deploy during)
    change_freeze: Fridays after 15:00, bank holidays
```

---

## Anti-Patterns (Decisions That Failed)

```yaml
anti_patterns:
  - id: "AP-001"
    description: "Undersized database connection pool"
    context: "Default pool size (10) with 5 replicas"
    failure: "Pool exhaustion under load"
    lesson: "Pool size = max_replicas × 5 (minimum)"
    prevention: "Validate pool config in deployment-validator"
  
  - id: "AP-002"
    description: "No AI rate limiting"
    context: "Direct Bedrock calls without client-side throttle"
    failure: "Hit API rate limit → cascade failure"
    lesson: "Always implement rate limiting at 80% of provider limit"
    prevention: "Check rate limiting in AI deployment review"
  
  - id: "AP-003"
    description: "Terraform state lock not configured"
    context: "DynamoDB lock table missing in new environment"
    failure: "Concurrent applies corrupted state"
    lesson: "Lock table creation is part of bootstrap, never skip"
    prevention: "Validate lock configuration in IaC pipeline"
```

---

## Memory Query Interface

```yaml
query_examples:
  find_similar_decision:
    input: {service_type: api, runtime: java, team_size: 3}
    result: "DM-2026-001 — ECS Fargate, SUCCESS, confidence HIGH"
  
  find_relevant_incidents:
    input: {symptom: high_latency, service_type: api}
    result: "INC-2026-001 — connection pool exhaustion"
  
  get_cost_estimate_accuracy:
    input: {category: ai_services}
    result: "Historical accuracy: 64%. Apply 1.5x multiplier."
  
  check_anti_patterns:
    input: {action: deploy_ai_worker}
    result: "AP-002 — ensure rate limiting configured"
  
  find_reusable_pattern:
    input: {workload: java_api, team_size: 2}
    result: "PAT-001 — Java API on ECS Fargate (3 successes, 0 failures)"
```

---

## Memory Maintenance

```yaml
maintenance:
  retention:
    decision_log: indefinite (compressed after 1 year)
    incident_history: indefinite
    cost_actuals: 2 years (then aggregated)
    patterns: indefinite (updated with each use)
    anti_patterns: indefinite
  
  refresh:
    patterns: re-evaluate confidence after each use
    cost_models: update quarterly with actuals
    preferences: review monthly with team
  
  cleanup:
    - Remove patterns with 0 uses in 6 months
    - Archive decisions older than 2 years
    - Consolidate similar incidents into single pattern
```
