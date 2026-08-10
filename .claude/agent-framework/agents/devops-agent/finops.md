# FinOps

## Purpose

Cost is a first-class architectural constraint. The DevOps Agent acts as a FinOps practitioner — estimating, tracking, optimizing, and challenging cost decisions throughout the lifecycle.

---

## FinOps Principles

```yaml
principles:
  1_visibility: "Every team sees what they spend"
  2_optimization: "Continuously right-size and eliminate waste"
  3_accountability: "Services own their costs"
  4_challenge: "DevOps Agent challenges expensive architecture decisions"
  5_forecast: "Predict before provision"
```

---

## Cost Challenge Function

The DevOps Agent **must challenge** architecture proposals with cost analysis:

### Challenge Template

```yaml
cost_challenge:
  trigger: "Architecture proposes infrastructure"
  
  process:
    1. Estimate proposed architecture cost
    2. Identify cheaper alternatives
    3. Calculate savings vs trade-offs
    4. Present recommendation
  
  output:
    proposed:
      description: "EKS + RDS Multi-AZ + OpenSearch + ElastiCache"
      monthly_cost: £650
      breakdown:
        eks_control_plane: £60
        eks_nodes: £180
        rds_multi_az: £120
        opensearch: £150
        elasticache: £80
        networking: £60
    
    alternative:
      description: "ECS Fargate + Aurora Serverless + PostgreSQL FTS"
      monthly_cost: £220
      breakdown:
        ecs_fargate: £60
        aurora_serverless: £80
        s3: £5
        sqs: £2
        networking: £50
        monitoring: £23
      
    savings: £430/month (66% reduction)
    
    trade_offs:
      - No Kubernetes (but not needed for 3 services)
      - No dedicated search (but PostgreSQL FTS handles current volume)
      - Aurora Serverless may have cold start on scale-to-zero
    
    recommendation: "Start with alternative. Upgrade components as needed."
    trigger_to_upgrade:
      eks: "> 5 services OR complex scheduling needed"
      opensearch: "> 1M documents OR complex search queries"
      dedicated_cache: "> 1000 req/s to same endpoints"
```

---

## Cost Estimation Engine

### Per-Service Cost Template

```yaml
cost_estimate:
  service: document-service
  environment: production
  assumptions:
    traffic: 100 req/s peak, 30 req/s average
    storage_growth: 10GB/month
    ai_requests: 5000/day
  
  compute:
    service: ECS Fargate
    configuration:
      vcpu: 0.5
      memory: 1GB
      tasks: 3 (average), 10 (peak)
    calculation:
      base: 3 tasks × 0.5 vCPU × 720 hrs × £0.04/hr = £43
      peak: estimated 20% time at 10 tasks = £14 extra
    monthly: £57
  
  database:
    service: RDS PostgreSQL
    configuration:
      instance: db.t4g.medium (2 vCPU, 4GB RAM)
      storage: 100GB gp3
      multi_az: true
    calculation:
      instance: £0.082/hr × 720 = £59
      multi_az: 2x = £118
      storage: 100GB × £0.115/GB = £12
    monthly: £130
  
  storage:
    service: S3
    configuration:
      volume: 50GB (current) + 10GB/month growth
      requests: 100K PUT, 500K GET per month
    calculation:
      storage: 50GB × £0.023/GB = £1.15
      requests: negligible
    monthly: £2
  
  messaging:
    service: SQS
    configuration:
      messages: 500K/month
    calculation:
      first_1M_free: £0
    monthly: £0
  
  ai:
    service: Bedrock (Claude 3 Sonnet)
    configuration:
      requests: 5000/day
      avg_input_tokens: 1500
      avg_output_tokens: 500
    calculation:
      input: 5000 × 1500 × 30 × £0.003/1K = £675
      output: 5000 × 500 × 30 × £0.015/1K = £1125
    monthly: £1800
    note: "⚠️ AI is largest cost — optimize prompts"
  
  networking:
    nat_gateway: £30
    alb: £20
    data_transfer: £10
    monthly: £60
  
  monitoring:
    cloudwatch: £15
    monthly: £15
  
  total_monthly: £2,064
  total_annual: £24,768
  
  cost_drivers:
    1: "AI inference (87% of total cost)"
    2: "Database (6%)"
    3: "Networking (3%)"
    4: "Compute (3%)"
  
  optimization_opportunities:
    - Prompt optimization (reduce tokens 30% → save £540/month)
    - Response caching (cache 20% of AI calls → save £360/month)
    - Use cheaper model for simple tasks (Haiku for classification)
    - Reserved RDS instance after 3 months (save 30% → £39/month)
```

---

## Budget Management

### Budget Structure

```yaml
budgets:
  total_platform:
    monthly: £3000
    annual: £36000
  
  per_environment:
    dev:
      budget: £200/month
      alert_at: [70%, 90%, 100%]
      action_at_100%: notify + review
    
    staging:
      budget: £400/month
      alert_at: [70%, 90%, 100%]
      action_at_100%: notify + review
    
    production:
      budget: £2400/month
      alert_at: [60%, 80%, 90%, 100%]
      action_at_80%: review optimization opportunities
      action_at_100%: emergency review + throttle non-essential
  
  per_category:
    compute: £300/month
    database: £200/month
    ai_inference: £1500/month
    networking: £100/month
    storage: £50/month
    monitoring: £50/month
```

### Budget Alerts (Terraform)

```hcl
resource "aws_budgets_budget" "monthly" {
  name         = "ilr-monthly-budget"
  budget_type  = "COST"
  limit_amount = "3000"
  limit_unit   = "USD"
  time_unit    = "MONTHLY"

  notification {
    comparison_operator       = "GREATER_THAN"
    threshold                 = 80
    threshold_type            = "PERCENTAGE"
    notification_type         = "ACTUAL"
    subscriber_email_addresses = ["platform-team@ilr.com"]
  }
}
```

---

## Cost Optimization Strategies

### Immediate (Week 1)

| Action | Effort | Savings |
|--------|--------|---------|
| Delete unattached EBS volumes | 5 min | £5-50/month |
| Release unused Elastic IPs | 5 min | £4/each |
| S3 lifecycle policies | 30 min | 20-60% storage |
| Schedule dev env shutdown (nights/weekends) | 2 hrs | 60-70% dev costs |
| Right-size over-provisioned instances | 1 hr | 20-40% compute |

### Short-Term (Month 1)

| Action | Effort | Savings |
|--------|--------|---------|
| VPC endpoints (reduce NAT costs) | 2 hrs | £10-30/month |
| AI prompt optimization | 1 day | 20-40% AI costs |
| AI response caching | 2 days | 10-30% AI costs |
| Spot instances for CI/CD | 4 hrs | 60-80% CI costs |
| Auto-scaling policies tuned | 4 hrs | 10-20% compute |

### Medium-Term (Quarter 1)

| Action | Effort | Savings |
|--------|--------|---------|
| Reserved instances (compute) | 1 hr | 30-40% compute |
| Reserved RDS | 1 hr | 30-40% database |
| Savings Plans | 1 hr | 30% broadly |
| Architecture optimization (cheaper AI model for simple tasks) | 1 week | 30-50% AI |
| Graviton instances (ARM) | 2 hrs | 20% compute |

---

## AI Cost Management (Special Focus)

```yaml
ai_cost_management:
  tracking:
    granularity: per-request
    dimensions:
      - model
      - prompt_version
      - service
      - endpoint
      - user (anonymized)
  
  optimization_techniques:
    1_prompt_engineering:
      action: "Reduce prompt token count"
      technique: "Concise system prompts, few-shot → zero-shot where possible"
      potential_savings: "20-40%"
    
    2_response_caching:
      action: "Cache identical requests"
      tool: "ElastiCache / application-level cache"
      ttl: "1 hour for classification, 24 hours for static content"
      potential_savings: "10-30% (depends on repeat rate)"
    
    3_model_tiering:
      action: "Use cheaper model for simple tasks"
      mapping:
        simple_classification: "Claude 3 Haiku (10x cheaper)"
        summarization: "Claude 3 Sonnet"
        complex_analysis: "Claude 3 Opus"
      potential_savings: "40-60% for mixed workloads"
    
    4_batch_processing:
      action: "Batch similar requests"
      technique: "Group documents by type, process in batches"
      potential_savings: "10-20%"
    
    5_early_exit:
      action: "Skip AI if rule-based logic can handle"
      technique: "Check if document matches known pattern first"
      potential_savings: "20-50% of AI calls eliminated"
  
  guardrails:
    per_request_max: £0.50
    daily_max: £100
    monthly_max: £2000
    action_on_exceed: throttle + alert
```

---

## Monthly Cost Review Report

```yaml
monthly_report:
  format:
    summary:
      total_spend: £X
      budget: £Y
      variance: +/- Z%
      trend: increasing / decreasing / stable
    
    breakdown_by_service:
      - service: document-service
        cost: £X
        change: +/-Y% vs last month
    
    breakdown_by_category:
      - compute: £X
      - database: £Y
      - ai: £Z
      - networking: £W
    
    top_cost_drivers:
      - "AI inference: 65% of total"
      - "Database: 15% of total"
    
    optimization_actions_taken:
      - "Implemented prompt caching: saved £X"
      - "Right-sized RDS: saved £Y"
    
    recommended_actions:
      - action: "Switch classification to Haiku"
        estimated_savings: £X/month
        effort: "2 days"
      - action: "Reserved RDS instance"
        estimated_savings: £Y/month
        effort: "1 hour"
    
    forecast:
      next_month: £X (based on growth trend)
      quarter: £Y
```

---

## Cost Governance Rules

```yaml
governance:
  rules:
    - Every new service must include cost estimate before approval
    - AI cost must be tracked per-request and per-service
    - Any resource > £100/month needs explicit approval
    - Dev environments must auto-shutdown off-hours
    - Untagged resources flagged for deletion after 7 days
    - Monthly cost review is mandatory (not optional)
  
  enforcement:
    - AWS Config: tag compliance
    - Budget alerts: automatic notifications
    - Terraform: cost estimation in plan output
    - CI/CD: infracost comment on PRs
```
