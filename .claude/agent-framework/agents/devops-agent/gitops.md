# GitOps

## Purpose

Define GitOps practices for declarative, version-controlled, automated Kubernetes deployments with drift detection and self-healing.

---

## GitOps Principles

```yaml
principles:
  1_declarative: "Desired state defined in Git (not imperative commands)"
  2_versioned: "All changes tracked via Git history"
  3_automated: "Approved changes automatically applied to cluster"
  4_observable: "Drift between Git and cluster detected and reconciled"
```

---

## GitOps Architecture

```
┌──────────────┐     ┌──────────────┐     ┌──────────────────┐
│  Application │     │  Config      │     │  Kubernetes      │
│  Repository  │     │  Repository  │     │  Cluster         │
│              │     │              │     │                  │
│  src/        │────▶│  helm/       │────▶│  Deployments     │
│  Dockerfile  │ CI  │  values/     │ CD  │  Services        │
│  tests/      │     │  overlays/   │     │  ConfigMaps      │
└──────────────┘     └──────────────┘     └──────────────────┘
                           ▲                       │
                           │   Drift Detection     │
                           └───────────────────────┘
                                 ArgoCD
```

---

## Repository Strategy

### Separation of Concerns

```yaml
repositories:
  application_repo:
    purpose: "Source code, tests, Dockerfiles"
    contains:
      - src/
      - Dockerfile
      - pom.xml / package.json
      - unit tests
    ci_output: Docker image pushed to ECR
    branch_strategy: trunk-based (main + feature branches)
  
  gitops_repo:
    purpose: "Deployment configuration (source of truth for cluster state)"
    contains:
      - environments/
      - helm-charts/
      - kustomize overlays/
      - argocd-apps/
    branch_strategy: 
      main: production
      staging: staging deployments
      (or single branch with directory-per-environment)
```

### GitOps Repository Structure

```
gitops-config/
├── argocd/
│   ├── app-of-apps.yaml           # Root application
│   ├── applications/
│   │   ├── document-service.yaml
│   │   ├── ai-worker.yaml
│   │   └── web-frontend.yaml
│   └── projects/
│       └── ilr-platform.yaml
│
├── environments/
│   ├── dev/
│   │   ├── document-service/
│   │   │   └── values.yaml
│   │   ├── ai-worker/
│   │   │   └── values.yaml
│   │   └── web-frontend/
│   │       └── values.yaml
│   ├── staging/
│   │   ├── document-service/
│   │   │   └── values.yaml
│   │   └── ...
│   └── production/
│       ├── document-service/
│       │   └── values.yaml
│       └── ...
│
├── charts/                         # Shared Helm charts
│   ├── ilr-service/
│   └── ilr-worker/
│
└── infrastructure/                 # Cluster-level resources
    ├── namespaces.yaml
    ├── network-policies/
    ├── rbac/
    └── monitoring/
```

---

## ArgoCD Configuration

### Application Definition

```yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: document-service-production
  namespace: argocd
  finalizers:
    - resources-finalizer.argocd.argoproj.io
spec:
  project: ilr-platform
  
  source:
    repoURL: https://github.com/ilr/gitops-config.git
    targetRevision: main
    path: environments/production/document-service
    helm:
      valueFiles:
        - values.yaml
  
  destination:
    server: https://kubernetes.default.svc
    namespace: ilr-production
  
  syncPolicy:
    automated:
      prune: true          # Remove resources not in Git
      selfHeal: true       # Revert manual changes
      allowEmpty: false
    syncOptions:
      - CreateNamespace=true
      - PrunePropagationPolicy=foreground
    retry:
      limit: 3
      backoff:
        duration: 5s
        factor: 2
        maxDuration: 3m
```

### App of Apps Pattern

```yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: ilr-platform
  namespace: argocd
spec:
  project: default
  source:
    repoURL: https://github.com/ilr/gitops-config.git
    targetRevision: main
    path: argocd/applications
  destination:
    server: https://kubernetes.default.svc
    namespace: argocd
  syncPolicy:
    automated:
      selfHeal: true
      prune: true
```

---

## Environment Promotion

### Image Promotion Flow

```
1. CI builds image → pushes to ECR with tag (e.g., 1.2.0-build.42)

2. CI updates GitOps repo (dev):
   environments/dev/document-service/values.yaml
     image.tag: "1.2.0-build.42"

3. ArgoCD auto-syncs dev environment

4. After dev tests pass, promote to staging:
   environments/staging/document-service/values.yaml
     image.tag: "1.2.0-build.42"  (same image!)

5. After staging validation, promote to production:
   environments/production/document-service/values.yaml
     image.tag: "1.2.0-build.42"  (same image!)
```

### Promotion Automation

```yaml
promotion:
  dev_to_staging:
    trigger: automated (after dev health checks pass)
    method: PR auto-created to update staging values
    approval: auto-merge if tests pass
  
  staging_to_production:
    trigger: manual approval
    method: PR created to update production values
    approval: team lead review + merge
    deployment: ArgoCD auto-syncs on merge
```

---

## Drift Detection

```yaml
drift_detection:
  enabled: true
  interval: 3 minutes (ArgoCD default)
  
  on_drift_detected:
    production:
      action: auto-heal (revert to Git state)
      alert: notify team (someone made manual change)
    staging:
      action: auto-heal
      alert: warning
    dev:
      action: warn only (allow experimentation)
  
  exceptions:
    - HPA replica count (managed by auto-scaler)
    - Pod annotations (managed by controllers)
```

---

## Rollback via GitOps

```yaml
rollback:
  method: "git revert"
  
  process:
    1. Identify the commit that introduced the bad change
    2. git revert <commit-hash>
    3. Push to main (GitOps repo)
    4. ArgoCD auto-syncs → cluster reverts to previous state
  
  time_to_rollback: < 3 minutes
  
  alternative:
    argocd_cli: "argocd app rollback document-service-production"
    history: "argocd app history document-service-production"
```

---

## Secrets in GitOps

### Problem
Secrets cannot be stored in Git (even encrypted in most cases).

### Solution: External Secrets Operator

```yaml
secrets_strategy:
  approach: External Secrets Operator
  
  flow:
    1. Secret stored in AWS Secrets Manager
    2. ExternalSecret CRD defined in GitOps repo
    3. External Secrets Operator syncs to K8s Secret
    4. Pod references K8s Secret as normal
  
  gitops_repo_contains:
    - ExternalSecret resource definition (what to fetch)
    - NOT the actual secret values
  
  example:
    # This is safe to store in Git:
    apiVersion: external-secrets.io/v1beta1
    kind: ExternalSecret
    metadata:
      name: database-credentials
    spec:
      refreshInterval: 1h
      secretStoreRef:
        name: aws-secrets-manager
        kind: ClusterSecretStore
      target:
        name: database-credentials
      data:
        - secretKey: password
          remoteRef:
            key: /ilr/production/database-credentials
            property: password
```

---

## GitOps Security

```yaml
security:
  repository_access:
    - GitOps repo: restricted to platform team + ArgoCD service account
    - Application repos: development teams
  
  argocd_access:
    - Admin: platform team only
    - Read-only: all developers
    - Sync: CI/CD service accounts (scoped per project)
  
  branch_protection:
    main:
      required_reviews: 1 (production changes)
      required_status_checks: [lint, validate]
      no_force_push: true
  
  rbac:
    argocd_projects:
      - project: ilr-platform
        allowed_namespaces: [ilr-production, ilr-staging, ilr-dev]
        allowed_sources: [https://github.com/ilr/gitops-config.git]
        denied_resources: [Namespace, ClusterRole, ClusterRoleBinding]
```

---

## Comparison: GitOps vs Push-Based CI/CD

| Aspect | Push-Based (CI deploys) | Pull-Based (GitOps/ArgoCD) |
|--------|------------------------|---------------------------|
| Trigger | CI pipeline pushes to cluster | ArgoCD pulls from Git |
| Drift detection | None | Continuous |
| Self-healing | None | Automatic |
| Audit trail | CI logs | Git history |
| Rollback | Redeploy old version | git revert |
| Security | CI needs cluster credentials | Only ArgoCD has cluster access |
| Complexity | Simpler initially | More setup, better long-term |

### When to Use GitOps

```yaml
use_gitops_when:
  - Multiple environments (dev, staging, production)
  - Need drift detection and self-healing
  - Audit trail for compliance
  - Multiple teams deploying to same cluster
  - Want to separate CI (build) from CD (deploy)

use_push_based_when:
  - Single environment
  - Simple deployment (1-2 services)
  - MVP/prototype phase
  - Team not ready for GitOps complexity
```
