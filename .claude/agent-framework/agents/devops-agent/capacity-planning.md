# Capacity Planning

## Purpose

Define scaling strategy, load projections, resource right-sizing, and proactive capacity management.

---

## Capacity Model

```yaml
capacity_dimensions:
  compute:
    metric: CPU and memory utilization
    headroom: 30% spare capacity at peak
    scaling: HPA (horizontal pod autoscaler)
  
  storage:
    metric: Disk usage growth rate
    headroom: 50% spare at current growth rate for 6 months
    scaling: Auto-expand (EBS, S3 unlimited)
  
  database:
    metric: Connection count, IOPS, storage
    headroom: 40% spare connections, 30% spare IOPS
    scaling: Vertical (instance class) + read replicas
  
  network:
    metric: Bandwidth, connection count
    headroom: 50% spare at peak
    scaling: Multi-AZ, additional NAT gateways
  
  queue:
    metric: Queue depth, message age
    headroom: Process 2x normal rate
    scaling: Add consumers (HPA on queue depth)
```

---

## Scaling Strategy Per Service

| Service | Min | Max | Scale Trigger | Cool Down |
|---------|-----|-----|--------------|-----------|
| API Gateway | 2 | 10 | CPU > 70% | 5 min |
| Document Service | 3 | 10 | CPU > 70% OR queue depth > 100 | 5 min |
| AI Worker | 2 | 8 | Queue depth > 50 | 3 min |
| React Frontend | 2 | 5 | CPU > 60% | 5 min |

---

## Load Projections

### Estimation Model

```yaml
load_projection:
  current:
    daily_users: 500
    peak_rps: 50
    avg_rps: 20
    storage_growth: 10GB/month
  
  6_month_projection:
    daily_users: 2000
    peak_rps: 200
    avg_rps: 80
    storage_growth: 40GB/month
  
  scaling_plan:
    compute: 4x current capacity
    database: Upgrade to db.r6g.large + read replica
    cache: Enable ElastiCache cluster mode
    queue: No change (SQS scales automatically)
    storage: No change (S3 unlimited)
```

### Traffic Patterns

```yaml
traffic_patterns:
  daily:
    peak: 09:00-11:00, 14:00-16:00 (UK business hours)
    low: 22:00-06:00
    pattern: bell curve
  
  weekly:
    peak: Tuesday-Thursday
    low: Saturday-Sunday
  
  seasonal:
    peak: Q1 (January - new year planning)
    low: August (summer holidays)
  
  scaling_response:
    peak: auto-scale to max
    low: scale to minimum (cost saving)
    unexpected_spike: scale aggressively (short stabilization window)
```

---

## Database Capacity

```yaml
database_capacity:
  connections:
    current_max: 100
    per_pod_pool: 10
    max_pods: 10
    headroom: 100 - (10 * 10) = 0 (UPGRADE NEEDED at max scale)
    plan: Upgrade to instance with 200 connections, or use RDS Proxy
  
  storage:
    current: 50GB
    growth_rate: 10GB/month
    provisioned: 100GB
    months_until_full: 5
    action: Monitor, auto-expand enabled
  
  iops:
    current_usage: 500 IOPS avg, 2000 peak
    provisioned: 3000 IOPS
    headroom: 33% at peak
    action: Adequate for 6 months
```

---

## Capacity Alerts

```yaml
capacity_alerts:
  compute:
    - name: HighUtilization
      condition: avg CPU > 70% for 30 minutes
      action: Verify HPA is scaling, check max replicas
    
    - name: NearMaxScale
      condition: replicas >= 80% of max
      action: Notify team, consider increasing max
  
  database:
    - name: ConnectionPoolNearFull
      condition: active_connections > 80% max
      action: Scale consumers or upgrade instance
    
    - name: StorageNearFull
      condition: free_storage < 20%
      action: Auto-expand or manual intervention
  
  queue:
    - name: QueueBacklog
      condition: oldest_message > 5 minutes
      action: Scale consumers, investigate slow processing
```

---

## Capacity Review Process

```yaml
capacity_review:
  frequency: monthly
  
  inputs:
    - Current utilization metrics (last 30 days)
    - Growth projections (business roadmap)
    - Planned feature launches
    - Historical scaling events
  
  outputs:
    - Capacity forecast (3 months)
    - Scaling recommendations
    - Budget impact
    - Risk assessment (what breaks at 2x, 5x, 10x)
  
  stress_test:
    frequency: quarterly
    tool: k6 / Locust / Artillery
    scenarios:
      - 2x normal load
      - 5x normal load (special event)
      - 10x normal load (viral scenario)
    measure:
      - At what point does latency degrade?
      - At what point do errors start?
      - Does auto-scaling keep up?
```
