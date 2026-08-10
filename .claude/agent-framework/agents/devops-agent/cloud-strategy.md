# Cloud Strategy

## Purpose

Define the cloud adoption strategy, provider selection rationale, and multi-cloud position for the AI-DLC platform.

---

## Cloud Provider Selection

### Primary: AWS

| Criterion | AWS Score | Reasoning |
|-----------|-----------|-----------|
| AI/ML Services | ★★★★★ | Bedrock, SageMaker, Comprehend |
| Kubernetes | ★★★★☆ | EKS mature but complex |
| Managed Services | ★★★★★ | Broadest service catalog |
| Cost Model | ★★★★☆ | Reserved instances, Savings Plans |
| UK Region | ★★★★★ | eu-west-2 (London) |
| Compliance | ★★★★★ | GDPR, ISO 27001, SOC 2 |

### Multi-Cloud Position

```yaml
strategy: AWS-primary
position: single-cloud-preferred
rationale:
  - Smaller team benefits from deep expertise in one cloud
  - Avoid abstraction tax of multi-cloud tooling
  - Use cloud-agnostic patterns where cost is zero (containers, Terraform)
escape_hatches:
  - Containerized workloads (portable)
  - Terraform modules (adaptable)
  - Standard APIs (REST, gRPC, SQL)
```

---

## Cloud Adoption Framework

### Landing Zone

```
AWS Organization
├── Management Account
│   └── Billing, SCPs, CloudTrail
├── Security Account
│   └── GuardDuty, Security Hub, Config
├── Shared Services Account
│   └── ECR, CI/CD, Networking Hub
├── Development Account
│   └── Dev workloads
├── Staging Account
│   └── Pre-production
└── Production Account
    └── Live workloads
```

### Account Strategy

| Account | Purpose | Access Level |
|---------|---------|-------------|
| Management | Billing, governance | Admin only |
| Security | Centralized security tooling | Security team |
| Shared | Shared infrastructure (ECR, CI/CD) | Platform team |
| Dev | Development workloads | Developers |
| Staging | Pre-production testing | Developers + QA |
| Production | Live traffic | Restricted (deploy pipelines only) |

---

## Region Strategy

```yaml
primary_region: eu-west-2  # London
dr_region: eu-west-1       # Ireland
rationale:
  - Data residency (UK GDPR)
  - Lowest latency for UK users
  - DR region in same geographic area
```

---

## Cloud-Native vs Cloud-Agnostic Decision

| Component | Approach | Reasoning |
|-----------|----------|-----------|
| Compute | Cloud-agnostic (containers) | Portability |
| Database | Cloud-native (RDS/Aurora) | Managed ops |
| Queue | Cloud-native (SQS) | Cost, reliability |
| AI/ML | Cloud-native (Bedrock) | Capability |
| Monitoring | Hybrid (Prometheus + CloudWatch) | Flexibility |
| IaC | Cloud-agnostic (Terraform) | Portability |
| CI/CD | Cloud-agnostic (GitHub Actions) | Not locked to AWS |

---

## Cost Governance

### Budget Boundaries

```yaml
environments:
  dev:
    monthly_budget: £200
    alert_threshold: 80%
  staging:
    monthly_budget: £300
    alert_threshold: 80%
  production:
    monthly_budget: £2000
    alert_threshold: 70%
```

### Cost Controls

1. **Tagging Policy** — All resources must have: `Environment`, `Service`, `Owner`, `CostCenter`
2. **Budget Alerts** — AWS Budgets with SNS notifications
3. **Unused Resource Detection** — Weekly scan for idle resources
4. **Right-Sizing** — Monthly Compute Optimizer review
5. **Reserved Capacity** — For stable production workloads (after 3 months of data)

---

## Migration Path (If Applicable)

```
Phase 1: Lift & Shift
  └── Containerize existing services

Phase 2: Re-platform
  └── Use managed services (RDS, SQS, S3)

Phase 3: Re-architect
  └── Event-driven, serverless where appropriate

Phase 4: Optimize
  └── Cost optimization, reserved instances, spot
```
