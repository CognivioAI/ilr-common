# AWS Architecture Module

> Architecture Agent — AWS Topology Discipline

---

## Purpose

Define how the Architecture Agent selects AWS services and designs
the deployment topology for each architecture option.

---

## Service Selection Decision

When selecting AWS services, produce a decision table:

### Compute

| Option | Service | When to Use |
|--------|---------|-------------|
| Containers (managed) | ECS Fargate | Small team, simpler ops |
| Containers (orchestrated) | EKS | Multi-team, complex workloads, Helm/ArgoCD |
| Serverless | Lambda | Event-driven, short tasks (< 15 min) |
| VMs | EC2 | Legacy workloads, specialized hardware |

### Database

| Option | Service | When to Use |
|--------|---------|-------------|
| Relational | RDS PostgreSQL | ACID, complex queries, 90% of use cases |
| Document | DynamoDB | Key-value, extreme scale, simple access |
| In-memory | ElastiCache Redis | Sessions, caching, rate limiting |
| Vector | pgvector (RDS) or OpenSearch | AI embeddings, semantic search |

### Messaging

| Option | Service | When to Use |
|--------|---------|-------------|
| Queue | SQS | Point-to-point, reliable delivery |
| Fan-out | SNS → SQS | One event, multiple consumers |
| Streaming | Kinesis | Ordered, high-volume event stream |
| Orchestration | Step Functions | Multi-step workflows with branching |

### AI

| Option | Service | When to Use |
|--------|---------|-------------|
| LLM inference | Bedrock (Claude, Titan) | Text classification, extraction, generation |
| OCR | Textract | Document text extraction |
| Embeddings | Bedrock Titan Embed | Vector generation for RAG |
| Custom ML | SageMaker | Custom trained models |

---

## Reference Topology: ILR Application

```
Internet
    │
    ▼
Route 53 (DNS)
    │
    ▼
CloudFront (CDN) ─── S3 (React SPA)
    │
    ▼
ALB (HTTPS, WAF)
    │
    ├── /api/v1/* ──▶ EKS Private Subnet
    │                    ├── user-service
    │                    ├── document-service
    │                    ├── ai-orchestrator
    │                    └── notification-service
    │
    └── /auth/* ──▶ Cognito
                        │
                        ▼
                    RDS PostgreSQL (Private Subnet, Multi-AZ)
                    ElastiCache Redis (Private Subnet)
                    S3 (Document Storage, Private)
                    SQS/SNS (Event Queues)
                    Bedrock (AI Inference, VPC Endpoint)
                    Textract (OCR)
                    Secrets Manager
```

---

## Environment Strategy

| Environment | Account | Purpose | Scale |
|-------------|---------|---------|-------|
| Development | Dev Account | Developer testing | Minimal (1 replica, small DB) |
| Staging | Staging Account | Pre-production validation | Production-like (scaled down) |
| Production | Prod Account | Live traffic | Full scale, multi-AZ |

**AWS Organizations**: Separate account per environment for blast-radius isolation.

---

## Networking Topology

```
VPC (10.0.0.0/16)
├── Public Subnets (ALB, NAT GW)
│   ├── AZ-a
│   └── AZ-b
├── Private App Subnets (EKS nodes)
│   ├── AZ-a
│   └── AZ-b
└── Private DB Subnets (RDS, ElastiCache)
    ├── AZ-a
    └── AZ-b
```

---

## IaC Tooling Decision

| Option | When |
|--------|------|
| Terraform | Multi-cloud, team-wide standard, mature ecosystem |
| AWS CDK | AWS-only, Java/TypeScript team, type-safe infra |
| CloudFormation | AWS-native, simpler projects |

---

## Checklist

- [ ] Compute service selected with ADR
- [ ] Database service selected with ADR
- [ ] Messaging service selected with ADR
- [ ] AI services selected with ADR
- [ ] Network topology designed (VPC, subnets, SGs)
- [ ] Environment strategy documented
- [ ] IaC tooling selected
- [ ] Cost implications estimated
- [ ] Multi-AZ for production
- [ ] Private subnets for all workloads
