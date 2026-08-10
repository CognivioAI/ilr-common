# Cloud Decision Engine

## Purpose

Not "Use AWS" — but "Which AWS service is the right choice for this specific workload?" An autonomous decision engine that evaluates options and recommends with reasoning.

---

## Decision Process

```
Workload Requirements
    │
    ▼
1. Classify workload (type, pattern, constraints)
    │
    ▼
2. Identify candidate services (2-4 options)
    │
    ▼
3. Score each against criteria
    │
    ▼
4. Recommend with reasoning
    │
    ▼
5. Document alternatives and trigger conditions
```

---

## Compute Decision Engine

### Input

```yaml
compute_input:
  service_name: "document-service"
  workload_type: api | worker | batch | event-driven | frontend
  runtime: java-21 | node-20 | python-3.12
  traffic_pattern: steady | bursty | scheduled | unpredictable
  execution_duration: <15min | >15min | continuous
  startup_time: <5s | <30s | <60s | >60s
  state: stateless | stateful
  team_kubernetes_experience: none | basic | advanced
  monthly_budget: £X
  scale_requirement: low (<10 rps) | medium (<100 rps) | high (>100 rps)
```

### Decision Matrix

```yaml
# Input: Java Spring Boot API, steady traffic, small team, no K8s experience
compute_evaluation:
  candidate_1:
    service: Lambda
    verdict: ❌ REJECTED
    reasons:
      - Java cold start: 5-15 seconds (unacceptable for API)
      - 15-minute execution limit
      - Not designed for always-on APIs
    score: 3/10
  
  candidate_2:
    service: EKS (Kubernetes)
    verdict: ❌ NOT RECOMMENDED (for now)
    reasons:
      - Team has no K8s experience
      - Control plane cost: £60/month (overhead for 1 service)
      - Operational complexity high
    score: 5/10
    revisit_when: "> 5 services OR team gains K8s skills"
  
  candidate_3:
    service: ECS Fargate
    verdict: ✅ RECOMMENDED
    reasons:
      - No cluster management
      - Docker-native (team familiarity)
      - Auto-scaling available
      - Cost-effective for steady workloads
      - Natural upgrade path to EKS later
    score: 8/10
  
  candidate_4:
    service: App Runner
    verdict: ⚠️ ALTERNATIVE
    reasons:
      - Simplest deployment model
      - Limited customization (networking, sidecars)
      - Less mature than ECS
    score: 7/10
    best_for: "Quick MVP with minimal infra concern"

  recommendation:
    selected: ECS Fargate
    confidence: HIGH
    monthly_cost: ~£40-60
    trigger_to_revisit:
      - Team gains Kubernetes experience
      - Service count exceeds 5
      - Need for complex scheduling/sidecars
```

---

## Database Decision Engine

### Input

```yaml
database_input:
  access_pattern: relational | key-value | document | search | time-series
  query_complexity: simple | moderate | complex (joins, aggregations)
  data_volume: small (<10GB) | medium (<100GB) | large (>100GB)
  read_write_ratio: read-heavy | write-heavy | balanced
  consistency: strong | eventual
  traffic_pattern: steady | bursty | unpredictable
  availability_requirement: 99.9% | 99.99%
  budget_sensitivity: low | medium | high
```

### Decision Matrix

```yaml
# Input: Relational, complex queries, medium volume, steady traffic
database_evaluation:
  candidate_1:
    service: RDS PostgreSQL
    verdict: ✅ RECOMMENDED
    reasons:
      - Full SQL support, complex queries
      - Predictable cost (instance-based)
      - Multi-AZ available
      - Team PostgreSQL experience
      - Backup/restore well-documented
    score: 8/10
    monthly_cost: "£60-120 (depending on instance class)"
  
  candidate_2:
    service: Aurora PostgreSQL
    verdict: ⚠️ CONSIDER FOR GROWTH
    reasons:
      - Better failover (30s vs 60-120s)
      - Auto-scaling storage
      - Up to 15 read replicas
      - Higher base cost
    score: 7/10
    monthly_cost: "£120-200"
    best_when: "High availability critical OR read-heavy with replicas"
  
  candidate_3:
    service: Aurora Serverless v2
    verdict: ⚠️ CONSIDER FOR VARIABLE LOAD
    reasons:
      - Scales to zero (cost saving in dev)
      - Handles traffic spikes automatically
      - Higher per-ACU cost at steady state
    score: 6/10
    monthly_cost: "Variable: £20-200"
    best_when: "Unpredictable traffic OR dev environments"
  
  candidate_4:
    service: DynamoDB
    verdict: ❌ NOT SUITABLE
    reasons:
      - Complex relational queries not supported
      - Requires data model redesign
      - Higher development effort
    score: 3/10
    best_when: "Simple key-value access, massive scale"

  recommendation:
    selected: RDS PostgreSQL
    instance_class: db.t4g.medium (production), db.t4g.small (staging)
    confidence: HIGH
    upgrade_path: "Aurora PostgreSQL when availability requirement increases"
```

---

## Messaging Decision Engine

```yaml
# Input: Async communication, point-to-point, reliable delivery needed
messaging_evaluation:
  candidate_1:
    service: SQS
    verdict: ✅ RECOMMENDED
    reasons:
      - Simple, reliable, cost-effective
      - Built-in DLQ
      - Automatic scaling
      - At-least-once delivery
      - Near-zero cost at low volume
    score: 9/10
    monthly_cost: "£1-5 (at moderate volume)"
  
  candidate_2:
    service: EventBridge
    verdict: ⚠️ BETTER FOR EVENT ROUTING
    reasons:
      - Rule-based routing
      - Schema registry
      - Multiple targets
      - Higher cost per event
    score: 6/10
    best_when: "Multiple consumers need different events"
  
  candidate_3:
    service: SNS + SQS
    verdict: ⚠️ BETTER FOR FAN-OUT
    reasons:
      - One event → multiple queues
      - Each consumer independent
      - Adds complexity
    score: 5/10
    best_when: "Same event needs to go to 3+ consumers"
  
  candidate_4:
    service: Kinesis
    verdict: ❌ OVERKILL
    reasons:
      - Designed for streaming (thousands/sec)
      - Higher cost
      - More operational complexity
      - Ordering guarantees not needed here
    score: 3/10
    best_when: "Real-time streaming, ordering required, >1000 events/sec"
```

---

## AI/ML Service Decision Engine

```yaml
# Input: LLM inference, document classification, moderate volume
ai_evaluation:
  candidate_1:
    service: AWS Bedrock (Claude)
    verdict: ✅ RECOMMENDED
    reasons:
      - Fully managed, no infrastructure
      - Pay per token (no idle cost)
      - Multiple models available
      - Built-in guardrails
      - UK region available
    score: 9/10
    monthly_cost: "Variable: £50-500 (based on volume)"
  
  candidate_2:
    service: SageMaker (hosted model)
    verdict: ❌ OVERKILL
    reasons:
      - Requires instance management
      - Higher base cost (always-on endpoint)
      - More operational burden
      - Needed for custom model training (not our case)
    score: 4/10
    best_when: "Custom model training, fine-tuning, specific hardware needs"
  
  candidate_3:
    service: Direct API (Anthropic/OpenAI)
    verdict: ❌ NOT RECOMMENDED
    reasons:
      - Data leaves AWS (compliance concern)
      - No VPC integration
      - Separate billing management
      - Credential management overhead
    score: 3/10
    best_when: "Non-sensitive data, model not available on Bedrock"
```

---

## Decision Documentation

Every decision produces:

```yaml
decision_record:
  id: CLOUD-001
  date: 2025-01-15
  service: document-service
  category: compute
  
  selected: ECS Fargate
  confidence: HIGH
  
  alternatives_considered:
    - service: EKS
      rejected_because: "Team lacks K8s experience, overkill for 2 services"
    - service: Lambda
      rejected_because: "Java cold start too slow for API service"
    - service: App Runner
      rejected_because: "Limited networking customization"
  
  monthly_cost_estimate: £45
  
  trigger_to_revisit:
    - condition: "Service count > 5"
      consider: EKS
    - condition: "Monthly compute cost > £300"
      consider: "Reserved capacity or EKS with spot nodes"
    - condition: "Team completes K8s training"
      consider: EKS migration
  
  risk:
    - "Fargate task size limits (4 vCPU max)"
    - "No GPU support (if needed for AI later)"
```

---

## Anti-Patterns (Decisions to Challenge)

| Proposed | Challenge | Alternative |
|----------|-----------|-------------|
| EKS for 1-2 services | "£60/month control plane for 2 services?" | ECS Fargate |
| Multi-region for MVP | "Do you need 99.99% uptime day one?" | Single region + backup |
| OpenSearch for 10K docs | "PostgreSQL full-text handles this volume" | PostgreSQL FTS |
| Kinesis for 100 events/day | "SQS handles this at 1% of the cost" | SQS |
| Aurora for dev environment | "Dev doesn't need HA" | RDS or Aurora Serverless |
| NAT Gateway per AZ for dev | "Dev can use single NAT" | Single NAT Gateway |
