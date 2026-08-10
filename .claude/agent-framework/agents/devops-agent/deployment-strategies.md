# Deployment Strategies

## Purpose

Define deployment strategy decision engine — when to use rolling, blue/green, or canary deployments based on risk profile and change type.

---

## Strategy Decision Engine

```yaml
deployment_decision:
  if:
    - change_type == "normal_release" AND risk == "low":
        strategy: rolling_update
        reason: "Simple, low overhead, self-healing"
    
    - change_type == "critical_change" OR affects_data_schema:
        strategy: blue_green
        reason: "Instant rollback, zero-downtime validation"
    
    - change_type == "ai_model_change" OR risk == "high":
        strategy: canary
        reason: "Gradual traffic shift with real-user validation"
    
    - change_type == "infrastructure_change":
        strategy: blue_green
        reason: "Full environment validation before switch"
```

---

## Rolling Update

### When to Use
- Normal feature releases
- Bug fixes (low risk)
- Configuration changes
- Dependency updates

### Configuration

```yaml
apiVersion: apps/v1
kind: Deployment
spec:
  replicas: 3
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxUnavailable: 1    # At most 1 pod down during update
      maxSurge: 1          # At most 1 extra pod during update
```

### Behavior

```
Time 0:  [v1] [v1] [v1]          ← All old version
Time 1:  [v1] [v1] [v1] [v2]    ← New pod starting (+1 surge)
Time 2:  [v1] [v1] [v2]         ← Old pod terminated
Time 3:  [v1] [v1] [v2] [v2]    ← Another new pod
Time 4:  [v1] [v2] [v2]         ← Another old terminated
Time 5:  [v2] [v2] [v2]         ← Complete
```

### Rollback

```bash
kubectl rollout undo deployment/document-service -n ilr-production
# Or via Helm:
helm rollback document-service-production 1
```

---

## Blue/Green Deployment

### When to Use
- Database schema migrations
- Breaking API changes
- Major version releases
- Infrastructure changes
- Regulatory/compliance releases

### Architecture

```
                    ┌──────────────┐
                    │     ALB      │
                    └──────┬───────┘
                           │
              ┌────────────┼────────────┐
              │            │            │
              ▼            │            ▼
    ┌─────────────────┐    │    ┌─────────────────┐
    │  Blue (v1.0)    │    │    │  Green (v2.0)   │
    │  ├── Pod 1      │  100%   │  ├── Pod 1      │
    │  ├── Pod 2      │◄───┘    │  ├── Pod 2      │
    │  └── Pod 3      │         │  └── Pod 3      │
    └─────────────────┘         └─────────────────┘
                                      ▲
                                      │ 0% (testing)
                                      │
                                After validation: switch 100% → Green
```

### Process

```yaml
blue_green_process:
  1. Deploy Green alongside Blue (both running)
  2. Run automated tests against Green
  3. Validate health checks and metrics
  4. Switch ALB target group from Blue → Green
  5. Monitor for 15 minutes
  6. If healthy: terminate Blue
  7. If unhealthy: switch back to Blue (instant rollback)
```

### Rollback
- Instant: Switch ALB back to Blue target group
- Time: < 30 seconds

---

## Canary Deployment

### When to Use
- AI model/prompt changes
- High-risk features
- Performance-sensitive changes
- New service versions with unknown behavior
- Recommendation engine updates

### Traffic Progression

```yaml
canary_stages:
  stage_1:
    traffic: 5%
    duration: 10 minutes
    validation:
      - error_rate < 1%
      - p95_latency < 500ms
      - no 5xx errors
  
  stage_2:
    traffic: 25%
    duration: 15 minutes
    validation:
      - error_rate < 1%
      - p95_latency < 500ms
      - business_metrics stable
  
  stage_3:
    traffic: 50%
    duration: 15 minutes
    validation:
      - all metrics healthy
  
  stage_4:
    traffic: 100%
    validation:
      - 30 minute stability window
  
  rollback_trigger:
    - error_rate > 2%
    - p95_latency > 1000ms
    - any 5xx spike
    - manual decision
```

### Implementation (Argo Rollouts)

```yaml
apiVersion: argoproj.io/v1alpha1
kind: Rollout
metadata:
  name: document-service
spec:
  replicas: 5
  strategy:
    canary:
      steps:
        - setWeight: 5
        - pause: {duration: 10m}
        - analysis:
            templates:
              - templateName: success-rate
        - setWeight: 25
        - pause: {duration: 15m}
        - setWeight: 50
        - pause: {duration: 15m}
        - setWeight: 100
      canaryMetadata:
        labels:
          role: canary
      stableMetadata:
        labels:
          role: stable
```

### Canary Analysis

```yaml
apiVersion: argoproj.io/v1alpha1
kind: AnalysisTemplate
metadata:
  name: success-rate
spec:
  metrics:
    - name: success-rate
      interval: 1m
      successCondition: result[0] > 0.99
      provider:
        prometheus:
          address: http://prometheus:9090
          query: |
            sum(rate(http_requests_total{status=~"2..",app="document-service",role="canary"}[5m]))
            /
            sum(rate(http_requests_total{app="document-service",role="canary"}[5m]))
```

---

## Decision Matrix

| Change Type | Strategy | Risk | Rollback Time |
|-------------|----------|------|---------------|
| Bug fix | Rolling | Low | ~2 minutes |
| New feature | Rolling | Low-Medium | ~2 minutes |
| Database migration | Blue/Green | High | < 30 seconds |
| AI model change | Canary | High | < 30 seconds |
| Breaking API change | Blue/Green | High | < 30 seconds |
| Performance optimization | Canary | Medium | < 30 seconds |
| Security patch | Rolling (fast) | Low | ~1 minute |
| Infrastructure change | Blue/Green | High | < 30 seconds |

---

## Pre-Deployment Checklist

```yaml
before_any_deploy:
  ✓ All tests passing
  ✓ Security scan clean
  ✓ Rollback plan documented
  ✓ Monitoring dashboards ready
  ✓ On-call engineer aware
  ✓ Communication plan (if user-facing)
  ✓ Database migration tested (if applicable)
  ✓ Feature flags configured
```
