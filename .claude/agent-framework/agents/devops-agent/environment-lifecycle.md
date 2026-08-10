# Environment Lifecycle Manager

## Purpose

Manage the complete lifecycle of environments — creation, deployment, validation, monitoring, and destruction — including ephemeral environments for development and testing.

---

## Environment Lifecycle

```
Create
    │
    ├── Provision infrastructure (Terraform)
    ├── Configure networking
    ├── Set up secrets
    └── Deploy base services
    │
    ▼
Deploy
    │
    ├── Deploy application version
    ├── Run database migrations
    ├── Configure feature flags
    └── Warm caches
    │
    ▼
Validate
    │
    ├── Health checks pass
    ├── Smoke tests pass
    ├── Integration tests pass
    └── Performance baseline established
    │
    ▼
Monitor
    │
    ├── Track resource usage
    ├── Watch cost accumulation
    ├── Detect drift
    └── Check expiry policy
    │
    ▼
Destroy (if ephemeral)
    │
    ├── Notify users
    ├── Backup data (if needed)
    ├── Terraform destroy
    └── Clean up artifacts
```

---

## Environment Types

### Long-Lived Environments

```yaml
long_lived:
  production:
    lifecycle: permanent
    owner: platform team
    access: CI/CD only (no direct access)
    backup: continuous
    monitoring: full
    cost_control: budget alerts + optimization
  
  staging:
    lifecycle: permanent
    owner: platform team
    access: developers (read) + CI/CD (deploy)
    backup: daily
    monitoring: full
    data: anonymized production snapshot (weekly refresh)
  
  development:
    lifecycle: permanent
    owner: platform team
    access: all developers
    backup: none
    monitoring: basic
    data: seed data
    schedule: scale to zero off-hours (optional)
```

### Ephemeral Environments

```yaml
ephemeral:
  preview_environment:
    trigger: pull request opened
    lifecycle: until PR closed/merged
    max_age: 7 days
    naming: "preview-{pr-number}"
    url: "preview-{pr-number}.dev.ilr.example.com"
    resources:
      cpu: 250m
      memory: 256Mi
      replicas: 1
    includes:
      - Application (PR branch)
      - Shared database (read-only or isolated)
      - Isolated queue
    excludes:
      - AI services (cost)
      - Full monitoring
    cost_estimate: £2-5/day
  
  feature_environment:
    trigger: manual request (developer)
    lifecycle: until manually destroyed or max age reached
    max_age: 14 days
    naming: "feature-{name}-{owner}"
    resources:
      cpu: 500m
      memory: 512Mi
      replicas: 1
    includes:
      - Full application stack
      - Dedicated database (small)
      - AI services (optional, cost-controlled)
    cost_estimate: £5-15/day
  
  load_test_environment:
    trigger: manual or scheduled
    lifecycle: duration of test (hours)
    max_age: 24 hours
    naming: "loadtest-{date}"
    resources:
      - Production-equivalent (for accurate results)
    includes:
      - Full stack at production scale
      - Load generators
      - Monitoring (detailed)
    cost_estimate: £20-50/run
    auto_destroy: after test completes
```

---

## Environment Creation Automation

### Terraform Workspace Strategy

```hcl
# Each environment is a Terraform workspace or separate state
resource "aws_ecs_service" "app" {
  name            = "${var.project}-${var.environment}-${var.service}"
  desired_count   = var.environments[var.environment].replicas
  # ... configuration varies by environment via tfvars
}
```

### Creation Workflow

```yaml
create_environment:
  inputs:
    type: preview | feature | loadtest
    name: string
    owner: string
    branch: git branch to deploy
    ttl: max lifetime (hours/days)
    services: [list of services to include]
  
  steps:
    1_provision:
      - Create namespace (Kubernetes)
      - Apply network policies
      - Create service accounts
      - Set up secrets (from shared Secrets Manager)
    
    2_deploy:
      - Pull images from ECR (matching branch tag)
      - Deploy via Helm (minimal values)
      - Wait for health checks
    
    3_configure:
      - Set up DNS (subdomain routing)
      - Configure ingress
      - Apply TLS certificate
      - Seed database (if dedicated)
    
    4_validate:
      - Run smoke tests
      - Verify all endpoints accessible
      - Post URL to PR / Slack
    
    5_register:
      - Record environment in registry
      - Set TTL/expiry timer
      - Tag resources for cost tracking
```

---

## Preview Environment (PR-Based)

### GitHub Actions Integration

```yaml
name: Preview Environment
on:
  pull_request:
    types: [opened, synchronize, reopened]

jobs:
  deploy-preview:
    runs-on: ubuntu-latest
    steps:
      - name: Build and Push Image
        run: |
          docker build -t $ECR/app:pr-${{ github.event.pull_request.number }} .
          docker push $ECR/app:pr-${{ github.event.pull_request.number }}
      
      - name: Deploy Preview
        run: |
          helm upgrade --install preview-${{ github.event.pull_request.number }} \
            ./charts/ilr-service \
            --namespace previews \
            --set image.tag=pr-${{ github.event.pull_request.number }} \
            --set ingress.host=preview-${{ github.event.pull_request.number }}.dev.ilr.example.com \
            --values charts/ilr-service/values-preview.yaml
      
      - name: Comment PR with URL
        uses: actions/github-script@v7
        with:
          script: |
            github.rest.issues.createComment({
              issue_number: context.issue.number,
              body: '🚀 Preview deployed: https://preview-${{ github.event.pull_request.number }}.dev.ilr.example.com'
            })

  cleanup-preview:
    if: github.event.action == 'closed'
    runs-on: ubuntu-latest
    steps:
      - name: Destroy Preview
        run: |
          helm uninstall preview-${{ github.event.pull_request.number }} -n previews
```

---

## Environment Registry

```yaml
environment_registry:
  storage: DynamoDB or ConfigMap
  
  schema:
    id: "preview-42"
    type: preview
    owner: "developer-name"
    branch: "feature/document-upload"
    created_at: "2025-01-15T10:00:00Z"
    expires_at: "2025-01-22T10:00:00Z"
    status: running | stopped | expired
    url: "https://preview-42.dev.ilr.example.com"
    cost_to_date: £12.50
    services: [document-service, web-frontend]
  
  operations:
    list: "Show all active environments"
    extend: "Extend TTL (max 14 days total)"
    stop: "Scale to zero (preserve state)"
    destroy: "Full cleanup"
```

---

## Cleanup and Cost Control

### Automatic Cleanup

```yaml
cleanup_policy:
  preview:
    trigger: PR closed/merged
    action: immediate destroy
    grace_period: 1 hour (in case PR reopened)
  
  feature:
    trigger: TTL expired (14 days max)
    warning: 24 hours before expiry (Slack notification)
    action: destroy after grace period
    extendable: yes (up to 14 more days with approval)
  
  loadtest:
    trigger: test completion or max age (24 hours)
    action: immediate destroy
  
  orphaned:
    detection: no activity for 7 days + no linked PR
    action: notify owner → destroy after 48 hours if no response
```

### Cost Guardrails

```yaml
cost_guardrails:
  per_environment:
    preview: £5/day max
    feature: £15/day max
    loadtest: £50/run max
  
  total_ephemeral:
    daily_budget: £50
    monthly_budget: £500
    action_on_exceed: stop oldest environments, notify team
  
  monitoring:
    track: per-environment cost via resource tags
    alert: when any environment exceeds daily budget
    report: weekly summary of ephemeral environment costs
```

---

## Environment Parity Validation

```yaml
parity_check:
  purpose: "Ensure environments match where they should"
  
  must_match:
    - Docker image version (same artifact promoted)
    - Helm chart version
    - Network policies
    - Security configurations
    - Application config structure
  
  allowed_differences:
    - Scale (replicas, instance sizes)
    - Data (synthetic vs production)
    - External endpoints (sandbox vs production)
    - Monitoring depth
    - Cost optimization settings
  
  drift_detection:
    frequency: daily
    action: report differences, auto-fix if possible
```

---

## Self-Service Interface

```yaml
self_service:
  developer_actions:
    create:
      command: "make env-create NAME=my-feature"
      or: "POST /api/environments { type: feature, name: my-feature }"
    
    status:
      command: "make env-status NAME=my-feature"
    
    extend:
      command: "make env-extend NAME=my-feature DAYS=7"
    
    destroy:
      command: "make env-destroy NAME=my-feature"
    
    list:
      command: "make env-list"
      output: table of all environments with status, URL, cost, expiry
```
