# Compliance

## Purpose

Define compliance standards, audit logging, policy-as-code, and regulatory requirements for the ILR platform.

---

## Regulatory Requirements

### GDPR / UK GDPR

| Requirement | Implementation |
|-------------|---------------|
| Data minimization | Only collect what's needed, TTL on stored data |
| Right to erasure | Automated deletion pipeline, cascade across services |
| Data portability | Export API (JSON format) |
| Consent management | Explicit consent tracking, audit trail |
| Breach notification | 72-hour notification process |
| Data residency | eu-west-2 (London) primary, no data outside UK/EU |
| Privacy by design | Encryption at rest + transit, pseudonymization |

### Data Protection

```yaml
data_protection:
  encryption:
    at_rest: AES-256 (AWS managed keys or CMK)
    in_transit: TLS 1.3 (minimum TLS 1.2)
    database: RDS encryption enabled
    s3: SSE-S3 or SSE-KMS
    secrets: AWS Secrets Manager (encrypted)
  
  access_control:
    principle: least privilege
    review: quarterly access audit
    logging: all access logged to CloudTrail
  
  data_classification:
    public: Marketing content, public docs
    internal: Technical docs, non-sensitive config
    confidential: User data, documents, PII
    restricted: Credentials, keys, financial data
```

---

## Audit Logging

### What to Log

```yaml
audit_events:
  authentication:
    - Login success/failure
    - MFA events
    - Token refresh
    - Session expiry
  
  authorization:
    - Permission denied
    - Role changes
    - Access grant/revoke
  
  data_access:
    - Document viewed
    - Document downloaded
    - Data exported
    - Bulk operations
  
  data_modification:
    - Record created
    - Record updated
    - Record deleted
    - Schema changes
  
  administrative:
    - Configuration changes
    - Deployment events
    - Secret rotation
    - User management
```

### Audit Log Format

```json
{
  "timestamp": "2025-01-15T10:30:00.123Z",
  "eventType": "DATA_ACCESS",
  "action": "DOCUMENT_VIEWED",
  "actor": {
    "userId": "user-456",
    "role": "MANAGER",
    "ipAddress": "10.0.1.100",
    "userAgent": "Mozilla/5.0..."
  },
  "resource": {
    "type": "DOCUMENT",
    "id": "doc-789",
    "classification": "CONFIDENTIAL"
  },
  "outcome": "SUCCESS",
  "metadata": {
    "correlationId": "req-abc-123",
    "serviceVersion": "1.2.0"
  }
}
```

### Audit Log Retention

| Log Type | Retention | Storage |
|----------|-----------|---------|
| Security events | 7 years | S3 Glacier |
| Data access | 3 years | S3 IA |
| Application logs | 90 days | CloudWatch |
| Infrastructure logs | 90 days | CloudWatch |
| Metric data | 15 months | CloudWatch |

---

## Policy-as-Code

### OPA/Gatekeeper (Kubernetes)

```yaml
# Enforce: No containers run as root
apiVersion: constraints.gatekeeper.sh/v1beta1
kind: K8sBlockRootUser
metadata:
  name: block-root-user
spec:
  match:
    kinds:
      - apiGroups: [""]
        kinds: ["Pod"]
    namespaces: ["ilr-production", "ilr-staging"]
```

### AWS Config Rules

```yaml
aws_config_rules:
  - name: encrypted-volumes
    description: All EBS volumes must be encrypted
    scope: AWS::EC2::Volume
    
  - name: rds-encryption-enabled
    description: All RDS instances must be encrypted
    scope: AWS::RDS::DBInstance
    
  - name: s3-bucket-public-read-prohibited
    description: No S3 buckets with public read
    scope: AWS::S3::Bucket
    
  - name: iam-no-inline-policies
    description: No inline IAM policies
    scope: AWS::IAM::User
    
  - name: vpc-sg-restricted-common-ports
    description: No unrestricted access to common ports
    scope: AWS::EC2::SecurityGroup
```

### Terraform Sentinel / OPA

```yaml
terraform_policies:
  - All resources must have required tags
  - No public S3 buckets
  - No wildcard IAM permissions
  - All RDS instances must be Multi-AZ in production
  - All security groups must have descriptions
  - No SSH (port 22) open to 0.0.0.0/0
```

---

## Compliance Scanning

```yaml
compliance_scanning:
  tools:
    - AWS Security Hub (CIS Benchmarks)
    - Prowler (AWS security assessment)
    - kube-bench (CIS Kubernetes Benchmark)
    - Checkov (Terraform compliance)
  
  schedule:
    - Continuous: AWS Config Rules (real-time)
    - Daily: Security Hub scan
    - Weekly: Full Prowler assessment
    - On PR: Checkov Terraform scan
  
  reporting:
    - Weekly compliance dashboard
    - Monthly compliance report (for management)
    - Immediate alert on critical findings
```

---

## Access Reviews

```yaml
access_reviews:
  frequency: quarterly
  
  scope:
    - AWS IAM users and roles
    - Kubernetes RBAC
    - Database access
    - Application roles
    - CI/CD permissions
  
  process:
    1. Generate access report (who has access to what)
    2. Service owners review their service access
    3. Remove unused access (> 90 days inactive)
    4. Document exceptions (with expiry date)
    5. Sign-off by engineering manager
  
  automation:
    tool: AWS IAM Access Analyzer
    alerts: unused permissions > 90 days
```

---

## Compliance Checklist (Per Service)

```yaml
service_compliance:
  ✓ Data classification documented
  ✓ Encryption at rest and in transit
  ✓ Audit logging enabled
  ✓ Access control (least privilege)
  ✓ PII handling documented
  ✓ Data retention policy applied
  ✓ Backup and recovery tested
  ✓ Security scan passed
  ✓ Dependency vulnerabilities resolved
  ✓ Network isolation (security groups + network policies)
```
