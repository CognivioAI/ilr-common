# Architecture Translator

## Purpose

Translate Architecture Agent output into deployable infrastructure artifacts. This is the bridge between design and implementation — the most critical function of the DevOps Agent.

---

## Translation Flow

```
Architecture Agent Output
    │
    ├── Service definitions
    ├── Technology decisions
    ├── NFR requirements
    ├── Integration patterns
    ├── Data models
    └── Security requirements
    │
    ▼
DevOps Agent Translation Engine
    │
    ├── Map services → compute resources
    ├── Map data stores → managed services
    ├── Map integrations → messaging/networking
    ├── Map NFRs → scaling/reliability config
    ├── Map security → IAM/network policies
    └── Map observability → monitoring stack
    │
    ▼
DevOps Output
    │
    ├── Terraform modules
    ├── Helm charts
    ├── CI/CD pipelines
    ├── Monitoring configuration
    ├── Security policies
    └── Cost estimates
```

---

## Input: Architecture Design Document

The DevOps Agent expects this structure from the Architecture Agent:

```yaml
architecture_output:
  project: ilr-platform
  version: "1.0.0"
  
  services:
    - name: document-service
      type: api
      runtime: java-21
      framework: spring-boot
      port: 8080
      dependencies:
        database:
          type: postgresql
          size: medium
        storage:
          type: object-storage
          access: read-write
        queue:
          type: message-queue
          pattern: producer
        ai:
          type: llm
          provider: bedrock
          model: claude-3-sonnet
    
    - name: ai-worker
      type: worker
      runtime: java-21
      framework: spring-boot
      dependencies:
        queue:
          type: message-queue
          pattern: consumer
        ai:
          type: llm
          provider: bedrock
          model: claude-3-sonnet
    
    - name: web-frontend
      type: frontend
      runtime: node-20
      framework: react
  
  integrations:
    - from: document-service
      to: ai-worker
      via: sqs
      pattern: async
    
    - from: web-frontend
      to: document-service
      via: rest
      pattern: sync
  
  nfrs:
    availability: 99.9%
    latency_p95: 500ms
    throughput: 100 req/s
    data_classification: confidential
    compliance: [gdpr, uk-gdpr]
  
  architecture_style: event-driven
  deployment_target: aws
  region: eu-west-2
```

---

## Translation Rules

### Service Type → Compute

| Architecture Service Type | DevOps Compute Selection |
|--------------------------|--------------------------|
| `api` (Spring Boot, small team) | ECS Fargate |
| `api` (Spring Boot, K8s required) | EKS Pod |
| `worker` (queue consumer) | ECS Fargate (auto-scale on queue) |
| `frontend` (React SPA) | S3 + CloudFront |
| `frontend` (SSR) | ECS Fargate or Lambda@Edge |
| `batch` (scheduled processing) | Lambda or ECS Scheduled Task |
| `event-driven` (short-lived) | Lambda |

### Dependency → AWS Service

| Architecture Dependency | DevOps AWS Service | Terraform Module |
|------------------------|-------------------|------------------|
| `database: postgresql` | RDS PostgreSQL | `modules/rds` |
| `database: postgresql (serverless)` | Aurora Serverless v2 | `modules/aurora` |
| `storage: object-storage` | S3 | `modules/s3` |
| `queue: message-queue` | SQS | `modules/sqs` |
| `queue: event-bus` | EventBridge | `modules/eventbridge` |
| `cache: key-value` | ElastiCache Redis | `modules/elasticache` |
| `ai: llm (bedrock)` | Bedrock | IAM permissions only |
| `search: full-text` | OpenSearch | `modules/opensearch` |
| `cdn: static-assets` | CloudFront | `modules/cloudfront` |

### NFR → Infrastructure Configuration

| NFR | DevOps Translation |
|-----|-------------------|
| `availability: 99.9%` | Multi-AZ, 3 replicas, PDB, auto-healing |
| `availability: 99.99%` | Multi-region, active-passive DR |
| `latency_p95: 500ms` | Caching layer, CDN, connection pooling |
| `latency_p95: 100ms` | In-memory cache, read replicas, edge compute |
| `throughput: 100 req/s` | HPA, 3-10 replicas, ALB |
| `throughput: 10000 req/s` | Auto-scaling, queue buffering, async processing |
| `data_classification: confidential` | Encryption at rest + transit, network isolation |
| `compliance: gdpr` | eu-west-2, audit logging, data retention policies |

### Integration Pattern → Infrastructure

| Architecture Pattern | DevOps Implementation |
|---------------------|----------------------|
| Sync REST | ALB → Target Group → Service |
| Async message queue | SQS + DLQ + consumer auto-scaling |
| Event fan-out | SNS → SQS subscriptions |
| Event routing | EventBridge rules |
| Streaming | Kinesis Data Streams |
| API Gateway | API Gateway + Lambda authorizer |

---

## Translation Output Template

For each service in the architecture, generate:

```yaml
# DevOps translation for: document-service
translation:
  service: document-service
  
  compute:
    platform: ecs-fargate
    cpu: 512          # 0.5 vCPU
    memory: 1024      # 1 GB
    replicas:
      dev: 1
      staging: 2
      production: 3
    auto_scaling:
      min: 3
      max: 10
      target_cpu: 70%
  
  networking:
    vpc: ilr-vpc
    subnet: private
    security_group:
      inbound:
        - port: 8080, source: alb-sg
      outbound:
        - port: 5432, destination: rds-sg
        - port: 443, destination: 0.0.0.0/0 (AWS APIs)
    load_balancer:
      type: ALB
      listener: 443 (HTTPS)
      target_port: 8080
      health_check: /actuator/health
  
  database:
    service: RDS PostgreSQL
    instance_class: db.t4g.medium
    storage: 100GB
    multi_az: true
    encryption: true
    backup_retention: 35 days
  
  storage:
    service: S3
    bucket: ilr-documents-production
    encryption: AES-256
    versioning: true
    lifecycle:
      transition_to_ia: 30 days
      transition_to_glacier: 90 days
  
  messaging:
    service: SQS
    queue: document-processing-queue
    dlq: document-processing-dlq
    visibility_timeout: 300s
    retention: 14 days
    dlq_max_receive: 3
  
  iam:
    role: document-service-role
    permissions:
      - s3:GetObject, s3:PutObject on document bucket
      - sqs:SendMessage on processing queue
      - secretsmanager:GetSecretValue on db credentials
      - bedrock:InvokeModel on claude-3-sonnet
  
  monitoring:
    metrics: prometheus (/actuator/prometheus)
    dashboard: grafana (auto-generated)
    alerts:
      - error_rate > 5% → P1
      - latency_p95 > 1s → P2
      - cpu > 80% → P3
    slo:
      availability: 99.9%
      latency_p95: 500ms
  
  deployment:
    strategy: rolling_update
    pipeline: github-actions
    environments: [dev, staging, production]
  
  cost_estimate:
    monthly:
      compute: £45
      database: £120
      storage: £5
      queue: £1
      networking: £50
      total: £221
```

---

## Multi-Service Translation

When architecture defines multiple services, translate the entire system:

```yaml
system_translation:
  project: ilr-platform
  
  shared_infrastructure:
    vpc:
      cidr: 10.0.0.0/16
      azs: [eu-west-2a, eu-west-2b]
    ecr:
      repositories: [document-service, ai-worker, web-frontend]
    dns:
      zone: ilr.example.com
    monitoring:
      prometheus: shared cluster
      grafana: shared instance
  
  services:
    document-service: { ... translation above ... }
    ai-worker: { ... translation ... }
    web-frontend: { ... translation ... }
  
  total_monthly_cost: £X
  
  terraform_modules_needed:
    - networking (VPC, subnets, NAT, ALB)
    - ecs-cluster
    - ecs-service (x3)
    - rds
    - s3
    - sqs
    - iam
    - monitoring
    - ecr
    - cloudfront
```

---

## Validation Rules

Before generating infrastructure, validate:

```yaml
validation:
  architecture_completeness:
    ✓ All services have defined runtime and port
    ✓ All dependencies have type specified
    ✓ NFRs defined (availability, latency, throughput)
    ✓ Security requirements stated
    ✓ Integration patterns documented
  
  devops_feasibility:
    ✓ Compute selection supports the runtime
    ✓ Database selection supports the access pattern
    ✓ Budget can accommodate estimated cost
    ✓ Team can operate chosen services
    ✓ Compliance requirements met by service choices
  
  if_validation_fails:
    - Document what's missing
    - Request clarification from Architecture Agent
    - Propose defaults with caveats
```

---

## Feedback Loop

```yaml
feedback_to_architecture:
  triggers:
    - Cost exceeds budget by > 30%
    - Operational complexity too high for team size
    - Security requirement cannot be met with chosen pattern
    - Scalability ceiling lower than NFR target
  
  format:
    issue: "Architecture proposes OpenSearch for full-text search"
    concern: "Adds £150/month and significant operational burden"
    alternative: "PostgreSQL full-text search for MVP (free, already provisioned)"
    recommendation: "Use PostgreSQL now, migrate to OpenSearch at > 1M documents"
    trigger_to_revisit: "Document count > 1M OR search latency > 200ms"
```
