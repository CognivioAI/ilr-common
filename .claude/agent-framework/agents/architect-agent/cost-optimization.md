# Cost Optimization Module

> Architecture Agent — Cost Architecture Discipline

---

## Purpose

Every architecture decision has cost implications. The Architecture Agent
produces high-level cost estimates for each option to support informed decisions.
Not exact pricing — enough to differentiate options.

---

## Architecture Cost Comparison

| Architecture | Monthly Infra Cost | Complexity | Scalability |
|--------------|-------------------|-----------|-------------|
| Monolith (ECS) | Low ($200–500) | Low | Medium |
| Microservices (ECS) | Medium ($500–2000) | Medium | High |
| Microservices (EKS) | High ($1000–5000) | High | Very High |
| Serverless (Lambda) | Low initially ($100–300) | Medium | Excellent |
| Hybrid | Medium ($500–2000) | Medium | High |

---

## Cost Categories to Estimate

| Category | Components |
|----------|-----------|
| Compute | ECS/EKS tasks, Lambda invocations, Fargate vCPU |
| Database | RDS instances, storage, IOPS |
| Storage | S3 storage + requests, EBS |
| AI | Bedrock/Claude tokens, Textract pages |
| Networking | NAT Gateway, data transfer, ALB |
| Messaging | SQS requests, SNS notifications |
| Cache | ElastiCache nodes |
| Monitoring | CloudWatch logs, metrics, X-Ray traces |
| Other | Secrets Manager, KMS, Route 53 |

---

## AI Cost Estimation

| Operation | Service | Unit Cost (approx) |
|-----------|---------|-------------------|
| OCR per page | AWS Textract | $0.01–0.05/page |
| Classification prompt | Claude Haiku | $0.001–0.005/call |
| Extraction prompt | Claude Sonnet | $0.01–0.05/call |
| Embedding generation | Bedrock Titan Embed | $0.0001/chunk |
| Semantic search | pgvector query | Negligible (DB cost) |

### Per-Document Estimate (ILR Application)

```
1 document upload:
  OCR (5 pages)           = $0.15
  Classification          = $0.005
  Extraction              = $0.03
  Validation              = $0.03
  ─────────────────────────
  Total AI cost/document  ≈ $0.22

1000 documents/month      ≈ $220/month AI cost
```

---

## Build vs Buy Decision

| Decision | Build | Buy/Managed |
|----------|-------|-------------|
| OCR | Custom ML pipeline | AWS Textract ✅ |
| Auth | Custom JWT service | Cognito ✅ |
| Email sending | SMTP integration | SES ✅ |
| Document storage | Custom file system | S3 ✅ |
| Container orchestration | Self-managed K8s | EKS ✅ |
| LLM inference | Self-hosted model | Bedrock ✅ |
| Vector database | Self-hosted Milvus | pgvector ✅ (or Pinecone) |

**Rule**: Buy managed services when the cost is reasonable and the team is small.
Build only when managed options don't meet requirements.

---

## Right-Sizing Rules

- Start small, scale based on metrics (not predictions)
- Use `t3.medium` equivalent for non-production, `m5.large` for production
- RDS: `db.t3.medium` for development, `db.r5.large` for production
- Review and right-size monthly using AWS Cost Explorer + Compute Optimizer
- Use Reserved Instances or Savings Plans for predictable workloads

---

## Cost Alerts

- Set monthly budget alerts at 50%, 80%, 100%
- Alert on per-service cost anomalies
- Review AI costs weekly (model usage can spike)
- Track cost per customer / cost per document processed

---

## Checklist

- [ ] High-level cost estimate per architecture option
- [ ] AI inference cost estimated per operation
- [ ] Build vs buy decisions documented
- [ ] Right-sizing strategy defined
- [ ] Reserved Instances / Savings Plans considered
- [ ] Cost alerts configured
- [ ] Monthly cost review cadence established
