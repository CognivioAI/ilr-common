# Rollback Strategy

## Purpose

Define automated rollback triggers, procedures, validation steps, and rollback testing practices.

---

## Rollback Decision Matrix

| Signal | Threshold | Action | Automation |
|--------|-----------|--------|------------|
| Error rate spike | > 5% for 2 min | Immediate rollback | Automatic |
| Latency degradation | p95 > 2x baseline for 5 min | Rollback | Automatic |
| Health check failure | > 50% pods unhealthy | Rollback | Automatic (K8s) |
| Critical bug reported | Confirmed by on-call | Manual rollback | Manual trigger |
| Data corruption | Any detection | Immediate rollback + DR | Manual |

---

## Automatic Rollback

### Helm Atomic Deploy

```bash
# --atomic flag: automatically rolls back on failure
helm upgrade --install document-service ./charts/ilr-service \
  --namespace ilr-production \
  --values values-production.yaml \
  --set image.tag=1.2.0 \
  --atomic \
  --timeout 5m \
  --wait
```

### Kubernetes Deployment Rollback

```yaml
# Deployment circuit breaker (auto-rollback on crash loop)
spec:
  strategy:
    type: RollingUpdate
  progressDeadlineSeconds: 300  # 5 min to complete rollout
  revisionHistoryLimit: 5      # Keep 5 previous versions
```

```bash
# Manual rollback
kubectl rollout undo deployment/document-service -n ilr-production

# Rollback to specific revision
kubectl rollout undo deployment/document-service -n ilr-production --to-revision=3

# Check rollout status
kubectl rollout status deployment/document-service -n ilr-production
```

### Argo Rollouts (Canary Auto-Rollback)

```yaml
spec:
  strategy:
    canary:
      steps:
        - setWeight: 5
        - pause: {duration: 5m}
        - analysis:
            templates:
              - templateName: error-rate-check
            args:
              - name: service
                value: document-service
      # Auto-rollback if analysis fails
      abortScaleDownDelaySeconds: 30
```

---

## Rollback Procedures

### Application Rollback

```yaml
application_rollback:
  steps:
    1. Identify current version and target version
    2. Execute rollback command
    3. Wait for pods to stabilize
    4. Verify health checks passing
    5. Run smoke tests
    6. Check error rate returned to baseline
    7. Notify team
  
  commands:
    helm: "helm rollback {release} {revision} --wait"
    kubectl: "kubectl rollout undo deployment/{name}"
    argocd: "argocd app rollback {app} --revision {rev}"
  
  estimated_time: 2-5 minutes
```

### Database Rollback

```yaml
database_rollback:
  strategy: forward-only migrations (never destructive)
  
  rules:
    - Never DROP columns in same release as code change
    - Use expand/contract pattern:
        1. Add new column (expand)
        2. Deploy code that writes to both
        3. Migrate data
        4. Deploy code that reads from new column
        5. Drop old column (contract) — separate release
  
  if_rollback_needed:
    - Application code is backward compatible with old schema
    - Roll back application, schema stays as-is
    - Fix forward with new migration
  
  emergency:
    - Point-in-time recovery (last resort)
    - RTO: 15 minutes
    - RPO: 5 minutes
```

### Infrastructure Rollback

```yaml
infrastructure_rollback:
  terraform:
    - Review terraform state
    - Apply previous known-good state
    - Or: terraform apply with reverted code
  
  procedure:
    1. git revert the infrastructure change
    2. Run terraform plan (verify rollback plan)
    3. Apply with approval
    4. Validate infrastructure health
```

---

## Rollback Validation

### Post-Rollback Checklist

```yaml
post_rollback_validation:
  immediate:
    ✓ All pods healthy and ready
    ✓ Health endpoints returning 200
    ✓ Error rate returned to baseline
    ✓ Latency returned to normal
  
  within_5_minutes:
    ✓ Smoke tests passing
    ✓ No new error patterns in logs
    ✓ Monitoring dashboards stable
    ✓ No customer complaints
  
  within_30_minutes:
    ✓ Full system stable
    ✓ Incident documented
    ✓ Root cause investigation started
    ✓ Team notified of rollback
```

---

## Rollback Testing

```yaml
rollback_testing:
  frequency: monthly (staging)
  
  scenarios:
    - Deploy new version → rollback → verify old version works
    - Canary failure → verify automatic rollback
    - Helm atomic failure → verify auto-rollback
    - Database migration → verify app works with rolled-back code
  
  success_criteria:
    - Rollback completes within 5 minutes
    - No data loss
    - No user-facing errors during rollback
    - Monitoring detects and reports correctly
```

---

## Rollback Communication

```yaml
communication:
  automated:
    - Slack alert: "⚠️ Rollback triggered for {service} v{version}"
    - Include: reason, current status, who's investigating
  
  manual:
    - Status update every 15 minutes during investigation
    - Resolution message with root cause summary
    - Follow-up: post-mortem scheduled
```

---

## Prevention (Reduce Need for Rollbacks)

```yaml
prevention:
  - Feature flags: disable feature without rollback
  - Canary deploys: catch issues with 5% traffic
  - Comprehensive staging tests: catch before production
  - Database migration patterns: always backward compatible
  - Gradual traffic shift: detect early, minimal blast radius
  - Smoke tests: automated validation immediately after deploy
```
