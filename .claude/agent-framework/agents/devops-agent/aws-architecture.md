# AWS Architecture

## Purpose

Define AWS service selection, topology, and best practices for the ILR AI platform.

---

## Service Selection Decision Engine

### Compute

| Service | When to Use | Cost Profile |
|---------|------------|--------------|
| ECS Fargate | Simple containerized services, MVP | Low ops, pay-per-use |
| EKS | Kubernetes requirement, complex orchestration | Higher ops, lower per-unit cost at scale |
| Lambda | Event-driven, short-lived tasks | Zero idle cost, pay-per-invocation |
| App Runner | Simple APIs, quick deploy | Lowest ops overhead |

**Decision Logic:**

```yaml
compute_decision:
  if:
    - team_size < 3 AND complexity == "low":
        recommend: ECS_FARGATE
        reason: "Low operational overhead, good for MVP"
    - kubernetes_required OR complex_scheduling:
        recommend: EKS
        reason: "Full Kubernetes capability needed"
    - event_driven AND duration < 15min:
        recommend: LAMBDA
        reason: "Zero idle cost, event-triggered"
    - simple_api AND no_special_requirements:
        recommend: APP_RUNNER
        reason: "Fastest path to production"
```

### Database

| Service | When to Use | Cost Profile |
|---------|------------|--------------|
| RDS PostgreSQL | Relational data, complex queries | Predictable, instance-based |
| Aurora PostgreSQL | High availability, auto-scaling reads | Higher base, scales well |
| DynamoDB | Key-value, high throughput, simple access patterns | Pay-per-request or provisioned |

### Queue / Messaging

| Service | When to Use | Cost Profile |
|---------|------------|--------------|
| SQS | Point-to-point, decoupling, retries | Very low cost |
| SNS | Fan-out, notifications | Low cost per message |
| EventBridge | Event routing, schema registry | Low cost, powerful routing |
| Kinesis | Real-time streaming, ordering | Higher cost, throughput-based |

### AI / ML

| Service | When to Use | Cost Profile |
|---------|------------|--------------|
| Bedrock | LLM access (Claude, Titan) | Pay-per-token |
| SageMaker | Custom model training/hosting | Instance-based |
| Comprehend | NLP tasks (sentiment, entities) | Pay-per-request |
| Textract | Document processing, OCR | Pay-per-page |

---

## Network Topology

```
┌─────────────────────────────────────────────────────────────┐
│ VPC (10.0.0.0/16)                                           │
│                                                             │
│  ┌──────────────────┐  ┌──────────────────┐                │
│  │ Public Subnet    │  │ Public Subnet    │  (AZ-a, AZ-b)  │
│  │ - ALB            │  │ - ALB            │                │
│  │ - NAT Gateway    │  │ - NAT Gateway    │                │
│  └──────────────────┘  └──────────────────┘                │
│                                                             │
│  ┌──────────────────┐  ┌──────────────────┐                │
│  │ Private Subnet   │  │ Private Subnet   │  (AZ-a, AZ-b)  │
│  │ - ECS/EKS Tasks  │  │ - ECS/EKS Tasks  │                │
│  │ - Lambda         │  │ - Lambda         │                │
│  └──────────────────┘  └──────────────────┘                │
│                                                             │
│  ┌──────────────────┐  ┌──────────────────┐                │
│  │ Data Subnet      │  │ Data Subnet      │  (AZ-a, AZ-b)  │
│  │ - RDS            │  │ - RDS Replica    │                │
│  │ - ElastiCache    │  │ - ElastiCache    │                │
│  └──────────────────┘  └──────────────────┘                │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## IAM Strategy

### Principle: Least Privilege

```yaml
iam_strategy:
  services:
    - Each service gets its own IAM role
    - Roles scoped to minimum required permissions
    - No wildcard (*) resource ARNs in production
  humans:
    - SSO via AWS IAM Identity Center
    - Role-based access (Admin, Developer, ReadOnly)
    - MFA required for all human access
  ci_cd:
    - OIDC federation with GitHub Actions
    - Short-lived credentials (no long-lived keys)
    - Separate deploy roles per environment
```

### Role Examples

```yaml
roles:
  document-service:
    permissions:
      - s3:GetObject on document-bucket
      - sqs:SendMessage on processing-queue
      - secretsmanager:GetSecretValue on db-credentials
      - bedrock:InvokeModel on claude-v3

  ci-deploy-role:
    permissions:
      - ecs:UpdateService
      - ecr:PutImage
      - s3:PutObject on deploy-artifacts
    trust: GitHub OIDC (repo-specific)
```

---

## High Availability

```yaml
availability:
  compute:
    - Multi-AZ deployment (minimum 2 AZs)
    - Auto-scaling based on CPU/memory/custom metrics
  database:
    - Multi-AZ RDS (automatic failover)
    - Read replicas for read-heavy workloads
  load_balancing:
    - ALB with health checks
    - Cross-zone load balancing enabled
  dns:
    - Route 53 with health checks
    - Failover routing for DR
```

---

## Tagging Standards

All AWS resources must include:

```yaml
tags:
  required:
    - Environment: dev | staging | production
    - Service: document-service | api-gateway | etc.
    - Owner: team-name
    - CostCenter: project-code
    - ManagedBy: terraform
  optional:
    - DataClassification: public | internal | confidential
    - BackupSchedule: daily | weekly | none
```
