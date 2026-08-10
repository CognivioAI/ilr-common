# Decision Framework

## Purpose

Define how the DevOps Agent makes infrastructure and operational decisions — weighted criteria, scoring methodology, and decision documentation.

---

## Decision Categories

```yaml
decision_types:
  compute:
    question: "Which compute service for this workload?"
    options: [ECS Fargate, EKS, Lambda, App Runner, EC2]
  
  storage:
    question: "Which storage for this data pattern?"
    options: [S3, EFS, EBS, DynamoDB, RDS, Aurora]
  
  messaging:
    question: "Which messaging service for this integration?"
    options: [SQS, SNS, EventBridge, Kinesis, MSK]
  
  deployment:
    question: "Which deployment strategy for this release?"
    options: [Rolling, Blue/Green, Canary]
  
  scaling:
    question: "How should this service scale?"
    options: [HPA CPU, HPA Memory, HPA Custom, KEDA Queue, Scheduled]
  
  database:
    question: "Which database for this access pattern?"
    options: [RDS PostgreSQL, Aurora PostgreSQL, Aurora Serverless, DynamoDB]
  
  observability:
    question: "Which monitoring approach?"
    options: [CloudWatch, Prometheus+Grafana, Hybrid]
```

---

## Scoring Criteria

### Universal Decision Matrix

Every decision is scored against these weighted criteria:

| Criterion | Weight | Description |
|-----------|--------|-------------|
| Cost | 25% | Monthly cost, scaling cost, hidden costs |
| Operational Complexity | 20% | Team effort to run, maintain, troubleshoot |
| Scalability | 15% | Can it handle 10x growth? |
| Security | 15% | Attack surface, compliance, isolation |
| Reliability | 15% | Availability, failure modes, recovery |
| Developer Experience | 10% | How easy for developers to work with? |

### Scoring Scale

```
1 — Poor (significant issues, avoid if possible)
3 — Acceptable (works but has notable drawbacks)
5 — Good (solid choice, minor limitations)
7 — Very Good (strong choice, few concerns)
9 — Excellent (ideal fit for this scenario)
```

---

## Compute Decision Engine

### Input Parameters

```yaml
compute_input:
  workload_type: api | worker | batch | event-driven | frontend
  traffic_pattern: steady | bursty | scheduled | unpredictable
  team_size: 1-3 | 4-10 | 10+
  kubernetes_required: true | false
  startup_time_requirement: fast (<5s) | moderate (<30s) | slow_ok (>30s)
  execution_duration: short (<15min) | long (>15min) | continuous
  state: stateless | stateful
  monthly_budget: £X
```

### Decision Logic

```yaml
compute_decision_tree:
  # Path 1: Event-driven, short duration
  if workload_type == "event-driven" AND execution_duration == "short":
    recommend: Lambda
    score:
      cost: 9 (zero idle cost)
      ops_complexity: 9 (fully managed)
      scalability: 9 (automatic)
      security: 7 (shared responsibility)
      reliability: 8 (AWS managed)
      dx: 7 (cold starts, constraints)
    confidence: HIGH
    
  # Path 2: Simple API, small team, no K8s requirement
  elif workload_type == "api" AND team_size <= 3 AND kubernetes_required == false:
    recommend: ECS Fargate
    score:
      cost: 7 (pay per use, no cluster overhead)
      ops_complexity: 8 (no node management)
      scalability: 7 (auto-scaling available)
      security: 8 (task-level isolation)
      reliability: 8 (multi-AZ, AWS managed)
      dx: 7 (Docker familiar, simple deploy)
    confidence: HIGH
    
  # Path 3: Complex orchestration, multiple services, K8s features needed
  elif kubernetes_required == true OR team_size > 5:
    recommend: EKS
    score:
      cost: 5 (cluster overhead, node costs)
      ops_complexity: 4 (significant ops burden)
      scalability: 9 (full K8s ecosystem)
      security: 8 (network policies, RBAC)
      reliability: 8 (self-healing, multi-AZ)
      dx: 6 (steep learning curve)
    confidence: MEDIUM
    caveat: "Only if team has Kubernetes experience"
    
  # Path 4: Simple API, fastest path, no special needs
  elif workload_type == "api" AND traffic_pattern == "steady":
    recommend: App Runner
    score:
      cost: 7 (simple pricing, auto-scale)
      ops_complexity: 9 (minimal management)
      scalability: 7 (limited customization)
      security: 7 (managed platform)
      reliability: 7 (AWS managed)
      dx: 9 (fastest to production)
    confidence: MEDIUM
    caveat: "Limited customization options"
```

---

## Database Decision Engine

```yaml
database_decision_tree:
  # Standard relational workload
  if access_pattern == "relational" AND scale == "moderate":
    recommend: RDS PostgreSQL
    reasoning: "Standard RDBMS, predictable cost, Multi-AZ available"
    
  # High availability, auto-scaling reads
  elif access_pattern == "relational" AND scale == "high" AND read_heavy:
    recommend: Aurora PostgreSQL
    reasoning: "Auto-scaling reads, faster failover, storage auto-grows"
    
  # Variable/unpredictable load, cost-sensitive
  elif access_pattern == "relational" AND traffic_pattern == "bursty":
    recommend: Aurora Serverless v2
    reasoning: "Scales to zero, pay per ACU, handles spikes"
    
  # Key-value, high throughput, simple queries
  elif access_pattern == "key-value" AND scale == "high":
    recommend: DynamoDB
    reasoning: "Single-digit ms latency, infinite scale, pay per request"
    
  # Document search, full-text
  elif access_pattern == "search":
    recommend: OpenSearch
    reasoning: "Full-text search, analytics, log aggregation"
```

---

## Deployment Strategy Decision Engine

```yaml
deployment_decision_tree:
  inputs:
    change_type: feature | bugfix | hotfix | ai_model | breaking_change | infra
    risk_level: low | medium | high | critical
    data_migration: true | false
    user_impact: none | low | high | all_users
    rollback_complexity: simple | complex | impossible
  
  decision:
    if risk_level == "low" AND change_type in [feature, bugfix]:
      strategy: rolling_update
      confidence: HIGH
      reasoning: "Low risk, simple rollback via kubectl rollout undo"
    
    elif change_type == "breaking_change" OR data_migration == true:
      strategy: blue_green
      confidence: HIGH
      reasoning: "Instant rollback needed, validate before traffic switch"
    
    elif change_type == "ai_model" OR risk_level == "high":
      strategy: canary
      confidence: HIGH
      reasoning: "Gradual exposure, real-user validation, auto-rollback on metrics"
    
    elif change_type == "hotfix" AND risk_level == "low":
      strategy: rolling_update_fast
      confidence: HIGH
      reasoning: "Fast deployment, maxSurge=50%, minimal validation"
```

---

## Decision Documentation (ADR)

Every significant DevOps decision generates an ADR:

```markdown
# ADR-DEVOPS-001: Compute Selection for Document Service

## Status: Accepted
## Date: 2025-01-15

## Context
Document Service is a Spring Boot API processing document uploads. 
Team size: 3 engineers. No Kubernetes experience. MVP phase.

## Decision
Use ECS Fargate

## Scoring
| Criterion | ECS Fargate | EKS | Lambda |
|-----------|------------|-----|--------|
| Cost (25%) | 7 | 5 | 8 |
| Ops Complexity (20%) | 8 | 4 | 9 |
| Scalability (15%) | 7 | 9 | 7 |
| Security (15%) | 8 | 8 | 7 |
| Reliability (15%) | 8 | 8 | 7 |
| DX (10%) | 7 | 5 | 6 |
| **Weighted Total** | **7.4** | **6.1** | **7.5** |

## Rationale
Despite Lambda scoring marginally higher, ECS Fargate is selected because:
- Long-running Spring Boot service (not suited for Lambda cold starts)
- Team familiar with Docker (natural fit)
- Good growth path to EKS if needed later

## Consequences
- Monthly cost: ~£40-60 (Fargate tasks)
- Ops burden: Low (no cluster management)
- Migration path: Containerized → easy EKS migration later

## Trigger to Revisit
- > 10 services deployed (consider EKS)
- Event-driven workloads needed (consider Lambda)
- Cost > £200/month for compute (consider reserved/EKS)
```

---

## Conflict Resolution

When decisions conflict (e.g., architecture wants EKS but cost says ECS):

```yaml
conflict_resolution:
  process:
    1. Document both positions with scoring
    2. Identify the constraint causing conflict
    3. Present options with clear trade-offs
    4. Recommend based on current phase (MVP vs Scale)
    5. Define trigger conditions to revisit
  
  priority_order:
    1. Security (non-negotiable)
    2. Reliability (business critical)
    3. Cost (budget constrained)
    4. Scalability (future growth)
    5. Developer Experience (team productivity)
    6. Operational Complexity (team capacity)
  
  example:
    architecture_wants: EKS (full Kubernetes orchestration)
    devops_recommends: ECS Fargate (simpler, cheaper for MVP)
    resolution:
      decision: ECS Fargate now
      condition_to_upgrade: "> 8 services OR K8s features needed"
      documentation: ADR-DEVOPS-XXX
```
