# AWS Best Practices

> Version 1.0 | AI-DLC Engineering Handbook
> Purpose: Define AWS usage standards for all services.

---

## 1. Principles

- Infrastructure as Code — everything versioned
- Least privilege — always
- Encrypt everything — at rest and in transit
- Automate everything — no manual console changes
- Monitor everything — observe before it fails

---

## 2. Compute

- Use ECS or EKS for container orchestration
- Containerize all applications (Docker)
- Services must be stateless
- Use Fargate for serverless containers where appropriate
- Auto-scale based on CPU/memory metrics

---

## 3. Storage

### S3

- Private buckets (never public)
- Signed URLs for temporary access
- Versioning enabled
- Lifecycle policies for cost optimization
- Server-side encryption (SSE-S3 or SSE-KMS)

---

## 4. Secrets

**Use AWS Secrets Manager.**

| ❌ Never | ✅ Always |
|----------|-----------|
| Secrets in `application.yml` | Secrets Manager |
| Secrets in environment variables | External Secrets Operator |
| Secrets in Git | IAM role-based access |
| Hardcoded API keys | Dynamic secret rotation |

---

## 5. IAM

- Least privilege — only permissions needed
- **No Administrator access** for services
- Use IAM Roles for service accounts (IRSA in EKS)
- No long-lived access keys
- Separate roles per service
- Use permission boundaries

---

## 6. Logging and Observability

- CloudWatch for logs and metrics
- OpenTelemetry for distributed tracing
- Structured JSON logs
- Correlation IDs across services
- Dashboards per service
- Alerts on error rates and latency

---

## 7. Database

- RDS PostgreSQL (primary choice)
- Automatic backups enabled
- Encryption at rest (KMS)
- Multi-AZ for production
- Connection through private subnets only
- Read replicas for read-heavy workloads

---

## 8. Networking

- VPC per environment
- Private subnets for applications and databases
- Public subnets only for load balancers
- VPC endpoints for AWS service access
- Security groups as primary firewall
- No public IPs on application instances

---

## 9. AI Agent Prompt

```
Deploy using AWS best practices.
Use IAM roles — no access keys.
Use Secrets Manager — never hardcode secrets.
Use private networking — no public access to services.
Encrypt all storage (S3, RDS, EBS).
Containerize everything.
Infrastructure as Code (Terraform or CDK).
Auto-scale based on metrics.
```

---

## 10. Checklist

- [ ] All infrastructure defined as code
- [ ] IAM roles with least privilege
- [ ] No secrets in code or environment variables
- [ ] Encryption at rest and in transit
- [ ] Private subnets for applications
- [ ] Auto-scaling configured
- [ ] CloudWatch + OpenTelemetry enabled
- [ ] Automated backups for databases
- [ ] No public S3 buckets
