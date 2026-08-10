# AWS Security Standards

> Version 1.0 | AI-DLC Engineering Handbook
> Purpose: Define security controls for all AWS deployments.

---

## 1. Principles

- Least privilege everywhere
- Defense in depth (multiple layers)
- Encrypt everything
- Automate security controls
- Detect and respond (not just prevent)

---

## 2. IAM

### Rules

- No `AdministratorAccess` for services
- No long-lived access keys
- Use IAM Roles for Services (IRSA for EKS)
- Separate roles per microservice
- Permission boundaries on all roles
- MFA required for human access

### Policy Structure

```json
{
  "Effect": "Allow",
  "Action": [
    "s3:GetObject",
    "s3:PutObject"
  ],
  "Resource": "arn:aws:s3:::my-bucket/service-prefix/*"
}
```

**Never use wildcards (`*`) for resources in production.**

---

## 3. Secrets Management

| Secret Type | Storage |
|-------------|---------|
| Database passwords | Secrets Manager (auto-rotation) |
| API keys | Secrets Manager |
| TLS certificates | ACM |
| SSH keys | Systems Manager Parameter Store |
| OAuth client secrets | Secrets Manager |

### Access Pattern

```
Application → IRSA Role → Secrets Manager → Secret Value
```

Never:
- Secrets in environment variables at task definition level
- Secrets in application.yml
- Secrets in Docker images
- Secrets in Git (even encrypted)

---

## 4. Encryption

### At Rest

| Service | Encryption |
|---------|-----------|
| S3 | SSE-KMS |
| RDS | KMS-encrypted storage |
| EBS | KMS |
| DynamoDB | KMS |
| SQS | KMS |

### In Transit

- TLS 1.3 minimum for all connections
- ACM certificates on load balancers
- Enforce HTTPS (redirect HTTP → HTTPS)
- Internal service communication over TLS

---

## 5. Detection and Monitoring

| Service | Purpose |
|---------|---------|
| CloudTrail | API audit logging |
| GuardDuty | Threat detection |
| Security Hub | Centralized security findings |
| Config Rules | Configuration compliance |
| Inspector | Vulnerability scanning |

---

## 6. Container Security

- Base images from trusted registries only (ECR)
- No root users in containers
- Read-only file systems
- Scan images for vulnerabilities (Trivy/ECR scanning)
- Sign images and verify signatures
- No secrets baked into images

---

## 7. Network Security

- WAF on all public-facing load balancers
- Security groups per service (not shared)
- VPC Flow Logs enabled
- No public IPs on application resources
- Private endpoints for AWS services

---

## 8. Checklist

- [ ] IAM roles with least privilege (no wildcards)
- [ ] No long-lived access keys
- [ ] All secrets in Secrets Manager
- [ ] Encryption at rest (KMS) for all storage
- [ ] TLS 1.3 for all transit
- [ ] CloudTrail enabled
- [ ] GuardDuty enabled
- [ ] Container images scanned
- [ ] WAF on public endpoints
- [ ] VPC Flow Logs enabled
