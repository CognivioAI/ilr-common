# Environment Management

## Purpose

Define environment strategy, promotion flow, configuration management, and parity rules between environments.

---

## Environment Hierarchy

```
Development
    │
    │  Auto-deploy on merge to main
    ▼
Testing / QA
    │
    │  Auto-deploy after dev tests pass
    ▼
Staging
    │
    │  Manual approval for production
    ▼
Production
```

---

## Environment Configuration

| Property | Dev | Testing | Staging | Production |
|----------|-----|---------|---------|------------|
| Replicas | 1 | 2 | 2 | 3-10 (auto-scaled) |
| CPU Request | 250m | 250m | 500m | 500m |
| Memory Request | 256Mi | 256Mi | 512Mi | 512Mi |
| Database | Shared RDS (small) | Dedicated RDS (small) | Dedicated RDS (medium) | Multi-AZ RDS (large) |
| Cache | None | ElastiCache (small) | ElastiCache (medium) | ElastiCache (cluster) |
| SSL | Self-signed / ACM | ACM | ACM | ACM |
| Monitoring | Basic | Basic | Full | Full + Alerting |
| Backup | None | Daily | Daily | Continuous + DR |
| Data | Synthetic/seed | Synthetic | Anonymized production | Real |
| Access | All developers | QA + Developers | Limited team | CI/CD only |

---

## Promotion Flow

### Artifacts Promotion (Not Rebuild)

```yaml
promotion_rule:
  principle: "Promote the same artifact — never rebuild"
  
  flow:
    1. Build Docker image with tag (e.g., 1.2.0-build.42)
    2. Deploy to dev → run tests
    3. Promote SAME image to staging → run tests
    4. Promote SAME image to production
  
  why:
    - Ensures what you test is what you deploy
    - Eliminates "works on my machine" issues
    - Immutable artifacts guarantee consistency
```

---

## Configuration Management

### Per-Environment Configuration

```yaml
# Configuration sources (in priority order):
1. Environment variables (injected by Helm/K8s)
2. AWS Secrets Manager (secrets)
3. AWS Parameter Store (non-secret config)
4. ConfigMaps (Kubernetes — non-sensitive)
5. application-{profile}.yml (defaults, committed)
```

### Spring Boot Profiles

```yaml
# application.yml (common defaults)
server:
  port: 8080

# application-dev.yml
spring:
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/ilr_dev}
logging:
  level:
    root: DEBUG

# application-production.yml
spring:
  datasource:
    url: ${DB_URL}  # From Secrets Manager
logging:
  level:
    root: INFO
management:
  endpoints:
    web:
      exposure:
        include: health,prometheus
```

---

## Feature Flags

```yaml
feature_flags:
  tool: AWS AppConfig / LaunchDarkly / Custom
  
  strategy:
    dev: all flags ON (developers test everything)
    staging: match production flags
    production: controlled rollout
  
  pattern:
    - Deploy code with flag OFF
    - Enable flag in staging → test
    - Enable flag in production (% rollout)
    - Remove flag after stable (cleanup)
```

---

## Environment Parity Rules

### Must Be Identical

| Aspect | Rule |
|--------|------|
| Docker images | Same image across all environments |
| Kubernetes manifests | Same templates, different values |
| Helm charts | Same chart, environment-specific values files |
| Network policies | Same rules (different CIDRs) |
| Security policies | Same policies everywhere |
| Application code | Same artifact — never rebuild |

### Allowed to Differ

| Aspect | Why |
|--------|-----|
| Scale (replicas, instance size) | Cost optimization |
| Data | Privacy, volume |
| External integrations | Sandbox vs production endpoints |
| Monitoring depth | Cost of full monitoring in dev |
| Backup configuration | DR only needed for production |

---

## Environment Lifecycle

```yaml
lifecycle:
  dev:
    auto_deploy: on merge to main
    auto_destroy: never (always running)
    data_refresh: weekly (seed data)
  
  testing:
    auto_deploy: after dev deploy succeeds
    auto_destroy: never
    data_refresh: daily (synthetic data)
  
  staging:
    auto_deploy: after testing passes
    auto_destroy: never
    data_refresh: weekly (anonymized production snapshot)
  
  production:
    deploy: manual approval required
    destroy: never
    data: real production data
```

---

## Preview / Ephemeral Environments

```yaml
preview_environments:
  trigger: pull request opened
  lifespan: until PR closed/merged
  content:
    - Application (PR branch code)
    - Shared database (read-only or isolated)
  
  naming: preview-{pr-number}.dev.ilr.example.com
  
  resources:
    cpu: 250m
    memory: 256Mi
    replicas: 1
  
  cleanup:
    - Destroy on PR close/merge
    - Auto-expire after 7 days
```

---

## Access Control

```yaml
access:
  dev:
    - All developers (read/write)
    - CI/CD (deploy)
  
  testing:
    - Developers (read)
    - QA team (read/write)
    - CI/CD (deploy)
  
  staging:
    - Platform team (read/write)
    - Developers (read only)
    - CI/CD (deploy with approval)
  
  production:
    - CI/CD pipeline only (deploy)
    - Platform team (read — emergency access)
    - No direct developer access
```
