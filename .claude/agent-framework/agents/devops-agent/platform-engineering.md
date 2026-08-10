# Platform Engineering

## Purpose

Define the internal developer platform — golden paths, self-service capabilities, and developer experience that the DevOps Agent enables.

---

## Platform Vision

```
Developers should be able to:
  ✓ Deploy a new service in < 1 hour
  ✓ Get observability automatically
  ✓ Access secrets without tickets
  ✓ Scale without DevOps involvement
  ✓ Roll back without fear

Developers should NOT need to:
  ✗ Write Terraform from scratch
  ✗ Configure monitoring manually
  ✗ Manage Kubernetes directly
  ✗ Handle certificate renewal
  ✗ Provision databases manually
```

---

## Golden Paths

### New Service Template

```yaml
golden_path_new_service:
  input:
    service_name: document-service
    language: java-21
    type: api | worker | frontend
    database: postgres | none
    queue: sqs | none
  
  generates:
    application:
      - Dockerfile (multi-stage, secure)
      - application.yml (with profiles)
      - Health check endpoints
      - Prometheus metrics endpoint
      - Structured logging configuration
    
    infrastructure:
      - Terraform module (ECS/EKS service)
      - RDS module (if database needed)
      - SQS module (if queue needed)
      - IAM role (least privilege)
      - Security groups
    
    deployment:
      - Helm chart (with all environments)
      - GitHub Actions workflow (CI/CD)
      - Environment variables template
    
    observability:
      - Grafana dashboard (pre-built)
      - Alert rules (standard set)
      - Log group configuration
    
    documentation:
      - README.md (service overview)
      - API documentation stub
      - Runbook template
```

### Service Catalog

```yaml
service_catalog:
  templates:
    java-api:
      description: Spring Boot REST API
      includes: health, metrics, tracing, structured logging
      database: optional (PostgreSQL)
      deployment: ECS Fargate or EKS
    
    java-worker:
      description: Spring Boot queue consumer
      includes: SQS consumer, DLQ, metrics
      database: optional
      deployment: ECS Fargate or EKS
    
    react-frontend:
      description: React SPA with Nginx
      includes: Static build, CDN-ready
      deployment: S3 + CloudFront or Nginx container
    
    ai-worker:
      description: AI processing service
      includes: Bedrock client, token tracking, cost monitoring
      queue: SQS input
      deployment: ECS Fargate (auto-scaling on queue depth)
```

---

## Self-Service Capabilities

### What Developers Can Do Without Tickets

| Capability | How | Guardrails |
|-----------|-----|-----------|
| Deploy to dev/staging | Git push → auto-deploy | All tests must pass |
| Create feature branch env | PR → preview environment | Auto-expires in 7 days |
| View logs | Grafana / CloudWatch | Scoped to own services |
| View metrics | Grafana dashboards | Pre-built per service |
| Scale service | Update values file | Within max limits |
| Add environment variable | Update ConfigMap/Secret | Non-production self-serve |
| Roll back | Helm rollback command | Any recent revision |

### What Requires Platform Team

| Capability | Why |
|-----------|-----|
| New AWS account | Governance, billing |
| Production database changes | Data safety |
| Network changes | Security, blast radius |
| New external integration | Security review |
| Production secret creation | Security approval |
| Infrastructure cost > £X/month | Budget approval |

---

## Developer Experience (DX)

### Local Development

```yaml
local_development:
  tools:
    - Docker Compose (local service dependencies)
    - LocalStack (AWS service emulation)
    - k3d/minikube (local Kubernetes, optional)
  
  setup:
    command: "make dev-setup"
    time: < 5 minutes
    result: All dependencies running locally
  
  run:
    command: "./mvnw spring-boot:run -Plocal"
    hot_reload: enabled (Spring DevTools)
```

### Inner Loop (Developer Workflow)

```
Code → Build (local) → Test (local) → Commit → Push
         │                   │
         └── < 30 seconds ───┘
         
Outer Loop (CI/CD):
Push → Build (CI) → Test → Security → Deploy Dev
         │                                 │
         └────── < 10 minutes ─────────────┘
```

---

## Platform Metrics

### DORA Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| Deployment Frequency | Daily | Deploys to production per day |
| Lead Time for Changes | < 1 day | Commit to production |
| Change Failure Rate | < 15% | Failed deploys / total deploys |
| Mean Time to Recovery | < 30 min | Incident start to resolution |

### Platform Health

```yaml
platform_metrics:
  developer_satisfaction:
    survey: quarterly NPS
    target: > 8/10
  
  self_service_adoption:
    metric: % of deploys without platform team involvement
    target: > 90%
  
  time_to_first_deploy:
    metric: New service → first production deploy
    target: < 1 day
  
  platform_reliability:
    metric: CI/CD pipeline success rate
    target: > 95%
```

---

## Platform Roadmap

### Phase 1 — Foundation

- [x] CI/CD pipeline templates
- [x] Docker + Helm standards
- [x] Basic monitoring (Prometheus + Grafana)
- [ ] Service template generator
- [ ] Self-service dev/staging deploys

### Phase 2 — Self-Service

- [ ] Internal developer portal (Backstage)
- [ ] Service catalog with one-click provisioning
- [ ] Automated environment provisioning
- [ ] Cost dashboards per service/team
- [ ] Feature flag platform

### Phase 3 — Intelligence

- [ ] AI-assisted incident diagnosis
- [ ] Automated capacity recommendations
- [ ] Predictive scaling
- [ ] Self-healing services
- [ ] Automated toil detection and elimination
