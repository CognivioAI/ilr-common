# CI/CD Pipeline

## Purpose

Define pipeline architecture, stage design, quality gates, and best practices for automated build, test, and deployment.

---

## Pipeline Architecture

```
Code Commit
    │
    ▼
┌─────────────────────────────┐
│  BUILD STAGE                │
│  ├── Compile                │
│  ├── Unit Tests             │
│  └── Code Coverage          │
└─────────────────────────────┘
    │
    ▼
┌─────────────────────────────┐
│  SECURITY STAGE             │
│  ├── SAST (SonarQube)       │
│  ├── Dependency Scan        │
│  ├── Secret Detection       │
│  └── License Check          │
└─────────────────────────────┘
    │
    ▼
┌─────────────────────────────┐
│  PACKAGE STAGE              │
│  ├── Docker Build           │
│  ├── Container Scan (Trivy) │
│  └── Push to ECR            │
└─────────────────────────────┘
    │
    ▼
┌─────────────────────────────┐
│  DEPLOY DEV                 │
│  ├── Helm Upgrade           │
│  ├── Smoke Tests            │
│  └── Integration Tests      │
└─────────────────────────────┘
    │
    ▼
┌─────────────────────────────┐
│  DEPLOY STAGING             │
│  ├── Helm Upgrade           │
│  ├── E2E Tests              │
│  ├── Performance Tests      │
│  └── Security Tests (DAST)  │
└─────────────────────────────┘
    │
    ▼
┌─────────────────────────────┐
│  APPROVAL GATE              │
│  └── Manual Approval        │
└─────────────────────────────┘
    │
    ▼
┌─────────────────────────────┐
│  DEPLOY PRODUCTION          │
│  ├── Canary / Blue-Green    │
│  ├── Health Validation      │
│  ├── Traffic Shift          │
│  └── Rollback if unhealthy  │
└─────────────────────────────┘
    │
    ▼
┌─────────────────────────────┐
│  POST-DEPLOY                │
│  ├── Smoke Tests (prod)     │
│  ├── Monitoring Check       │
│  └── Notify Team            │
└─────────────────────────────┘
```

---

## Quality Gates

### Build Stage

| Gate | Threshold | Action |
|------|-----------|--------|
| Unit test pass rate | 100% | Block on any failure |
| Code coverage | ≥ 80% | Block below threshold |
| Build time | < 5 minutes | Warning if exceeded |

### Security Stage

| Gate | Threshold | Action |
|------|-----------|--------|
| SAST critical issues | 0 | Block deployment |
| SAST high issues | 0 (new) | Block deployment |
| Dependency vulnerabilities (critical) | 0 | Block deployment |
| Secret detection | 0 findings | Block deployment |

### Package Stage

| Gate | Threshold | Action |
|------|-----------|--------|
| Container scan (critical) | 0 | Block deployment |
| Container scan (high) | 0 new | Block deployment |
| Image size | < 500MB | Warning if exceeded |

### Deploy Stage

| Gate | Threshold | Action |
|------|-----------|--------|
| Health check pass | All pods healthy | Rollback if failed |
| Smoke test pass | 100% | Rollback if failed |
| Error rate increase | < 1% | Rollback if exceeded |
| Latency p95 increase | < 20% | Warning, manual review |

---

## Pipeline Triggers

```yaml
triggers:
  main_branch:
    on: push to main
    pipeline: full (build → deploy production)
  
  pull_request:
    on: PR opened/updated
    pipeline: build + security + test (no deploy)
  
  release_tag:
    on: tag v*.*.*
    pipeline: full with production deploy
  
  hotfix:
    on: push to hotfix/*
    pipeline: fast-track (reduced tests, deploy staging → production)
  
  infrastructure:
    on: push to infrastructure/**
    pipeline: terraform plan → approve → apply
```

---

## Parallel Execution

```yaml
parallel_stages:
  after_build:
    - security_scan    # SAST, dependency check
    - lint             # Code style
    - docker_build     # Container image
  
  after_deploy_staging:
    - e2e_tests        # End-to-end
    - performance      # Load testing
    - dast_scan        # Dynamic security
```

---

## Artifact Management

```yaml
artifacts:
  docker_images:
    registry: ECR
    retention: 90 days (untagged: 14 days)
  
  build_artifacts:
    storage: S3
    retention: 30 days
  
  test_reports:
    storage: S3
    retention: 90 days
    format: JUnit XML, HTML
  
  security_reports:
    storage: S3
    retention: 1 year
    format: SARIF
```

---

## Pipeline Variables

```yaml
# Never hardcode — always from environment/secrets
required_variables:
  - AWS_ACCOUNT_ID
  - AWS_REGION
  - ECR_REPOSITORY
  - EKS_CLUSTER_NAME
  - HELM_RELEASE_NAME

secret_variables:  # From GitHub Secrets / AWS Secrets Manager
  - SONAR_TOKEN
  - SLACK_WEBHOOK_URL
  - AWS_ROLE_ARN (via OIDC)
```

---

## Pipeline SLOs

| Metric | Target |
|--------|--------|
| Build + Test time | < 10 minutes |
| Deploy to dev | < 15 minutes (from commit) |
| Deploy to production | < 30 minutes (from approval) |
| Rollback time | < 5 minutes |
| Pipeline success rate | > 95% |

---

## Failure Handling

```yaml
failure_policy:
  build_failure:
    - Notify author via Slack/PR comment
    - Block merge
  
  security_failure:
    - Notify security team
    - Block deployment
    - Create security ticket
  
  deploy_failure:
    - Automatic rollback (--atomic)
    - Notify on-call team
    - Create incident ticket
  
  test_failure:
    - Retry once (flaky test mitigation)
    - If still failing, block and notify
```
