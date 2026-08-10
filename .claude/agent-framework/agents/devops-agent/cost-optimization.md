# Cost Optimization

## Purpose

The DevOps Agent acts as a cost guardian — challenging architecture decisions, estimating costs, and continuously optimizing spend.

---

## Cost Challenge Role

The DevOps Agent **must challenge** architecture decisions when cheaper alternatives exist:

```yaml
example_challenge:
  architecture_says: "Run EKS cluster"
  devops_challenges:
    cost_estimate:
      eks: "£180/month minimum (control plane + nodes)"
    alternative:
      ecs_fargate: "£40/month (for MVP workload)"
    recommendation: "Use ECS Fargate initially. Migrate to EKS when complexity justifies cost."
    trigger_to_upgrade: "> 10 services OR Kubernetes-specific features needed"
```

---

## Cost Estimation Framework

### Per Service Template

```yaml
cost_estimate:
  service: document-service
  environment: production
  
  compute:
    type: ECS Fargate
    vcpu: 0.5
    memory: 1GB
    tasks: 3
    monthly: £45
  
  database:
    type: RDS PostgreSQL
    instance: db.t4g.medium
    storage: 100GB
    multi_az: true
    monthly: £120
  
  storage:
    type: S3
    estimated_size: 50GB
    requests: 100k/month
    monthly: £5
  
  queue:
    type: SQS
    messages: 1M/month
    monthly: £1
  
  networking:
    nat_gateway: £30
    alb: £20
    data_transfer: £10
    monthly: £60
  
  monitoring:
    cloudwatch: £15
    monthly: £15
  
  total_monthly: £246
  total_annual: £2,952
```

---

## FinOps Practices

### Right-Sizing

```yaml
right_sizing:
  frequency: monthly
  tool: AWS Compute Optimizer
  
  rules:
    cpu_underutilized:
      threshold: average < 20% for 14 days
      action: downsize instance/task
    
    memory_underutilized:
      threshold: average < 30% for 14 days
      action: reduce memory allocation
    
    over_provisioned_db:
      threshold: CPU < 15% average
      action: downsize RDS instance class
    
    storage_unused:
      threshold: < 50% utilized after 30 days
      action: reduce provisioned storage
```

### Reserved Capacity (After Stabilization)

```yaml
reserved_capacity:
  when: after 3 months of stable production usage
  
  strategy:
    compute:
      type: Savings Plans (Compute)
      term: 1 year
      payment: No Upfront or Partial Upfront
      savings: ~30-40%
    
    database:
      type: RDS Reserved Instances
      term: 1 year
      payment: Partial Upfront
      savings: ~30-40%
  
  never_reserve:
    - Dev environment resources
    - Burst/spike capacity
    - Services < 3 months old
```

### Spot Instances (Non-Critical)

```yaml
spot_instances:
  use_for:
    - CI/CD build runners
    - Batch processing
    - Development environments
    - Load testing
  
  never_for:
    - Production API services
    - Databases
    - Stateful services
  
  savings: 60-90% vs on-demand
```

---

## Cost Alerts

```yaml
cost_alerts:
  daily_budget_exceeded:
    threshold: daily spend > (monthly_budget / 30) * 1.5
    action: Notify team lead
  
  monthly_forecast:
    threshold: projected spend > monthly_budget * 0.8
    action: Review and optimize
  
  anomaly_detection:
    tool: AWS Cost Anomaly Detection
    threshold: 25% above normal
    action: Investigate immediately
  
  ai_cost_spike:
    threshold: AI spend > daily average * 3x
    action: Review prompts, check for loops
```

---

## Cost Optimization Checklist

### Quick Wins

| Action | Savings | Effort |
|--------|---------|--------|
| Delete unused EBS volumes | £10-50/month | Low |
| Release unattached Elastic IPs | £4/month each | Low |
| Use S3 lifecycle policies | 20-60% storage | Low |
| Enable VPC endpoints (reduce NAT) | £10-30/month | Medium |
| Right-size RDS instances | 20-40% | Medium |
| Implement auto-scaling (scale down off-hours) | 30-50% | Medium |

### Scheduled Scaling

```yaml
scheduled_scaling:
  dev_environment:
    business_hours: 1 replica (08:00-18:00 Mon-Fri)
    off_hours: 0 replicas (scale to zero)
    savings: ~70%
  
  staging_environment:
    business_hours: 2 replicas
    off_hours: 1 replica
    savings: ~40%
  
  production:
    peak_hours: auto-scale (3-10)
    off_peak: minimum (3)
```

---

## AI Cost Management

```yaml
ai_costs:
  tracking:
    - Token usage per service
    - Token usage per endpoint
    - Cost per request
    - Daily/weekly/monthly trends
  
  optimization:
    - Prompt optimization (fewer tokens)
    - Caching common responses
    - Model selection (cheaper model for simple tasks)
    - Batch processing where possible
    - Rate limiting AI calls
  
  budget_guardrails:
    daily_limit: £50
    monthly_limit: £1000
    per_request_alert: £0.50
    action_on_exceed: throttle + notify
  
  example:
    model: Claude 3 Sonnet (via Bedrock)
    input_tokens: $3/1M tokens
    output_tokens: $15/1M tokens
    average_request: 1000 input + 500 output = £0.01
    daily_volume: 5000 requests = £50/day
```

---

## Monthly Cost Review

```yaml
monthly_review:
  cadence: First Monday of each month
  attendees: Platform team + Engineering lead
  
  agenda:
    1. Total spend vs budget
    2. Cost by service breakdown
    3. Biggest cost drivers
    4. Optimization opportunities
    5. Forecast for next month
    6. Action items
  
  output:
    - Cost report (shared with team)
    - Action items (with owners and deadlines)
    - Updated forecasts
```
