# Security & DevSecOps

## Purpose

Shift security left — integrate security scanning, vulnerability detection, and compliance checks into every stage of the delivery pipeline.

---

## Security Pipeline

```
Code Commit
    │
    ├── Pre-commit: Secret detection (git-secrets)
    │
    ▼
Pull Request
    │
    ├── SAST: Static code analysis (SonarQube)
    ├── SCA: Dependency vulnerability scan (OWASP, Snyk)
    ├── Secret scan: Credential detection (TruffleHog)
    ├── License check: OSS license compliance
    │
    ▼
Build
    │
    ├── Container scan: Image vulnerability (Trivy)
    ├── Dockerfile lint: Best practice check (Hadolint)
    │
    ▼
Deploy (Staging)
    │
    ├── DAST: Dynamic application testing (OWASP ZAP)
    ├── API security: Endpoint testing
    ├── Penetration testing: Automated pen test (scheduled)
    │
    ▼
Runtime (Production)
    │
    ├── WAF: Web Application Firewall (AWS WAF)
    ├── Runtime protection: Falco / GuardDuty
    ├── Audit logging: CloudTrail + application logs
    └── Anomaly detection: GuardDuty findings
```

---

## SAST (Static Application Security Testing)

### SonarQube Configuration

```yaml
sonar:
  quality_gate:
    new_code:
      coverage: ">= 80%"
      duplications: "< 3%"
      security_hotspots: 0
      vulnerabilities: 0
      bugs: 0
    overall:
      security_rating: A
      reliability_rating: A
```

### Rules to Enforce

| Category | Examples |
|----------|---------|
| Injection | SQL injection, XSS, command injection |
| Authentication | Weak passwords, missing auth checks |
| Cryptography | Weak algorithms, hardcoded keys |
| Sensitive Data | PII exposure, logging secrets |
| Input Validation | Unvalidated input, buffer overflow |

---

## SCA (Software Composition Analysis)

### Dependency Scanning

```yaml
dependency_scanning:
  tools:
    - OWASP Dependency Check
    - Snyk (optional)
  
  policy:
    critical:
      action: block_merge
      sla: immediate_fix
    high:
      action: block_merge
      sla: 48_hours
    medium:
      action: warning
      sla: next_sprint
    low:
      action: informational
      sla: backlog

  exceptions:
    - vulnerability_id: CVE-XXXX-YYYY
      reason: "Not exploitable in our context"
      approved_by: security-team
      expires: 2025-06-01
```

---

## Container Security

### Image Scanning Policy

```yaml
container_security:
  scan_on: push_to_ecr
  
  base_image_policy:
    allowed_registries:
      - docker.io/library
      - gcr.io/distroless
      - public.ecr.aws
    banned_tags:
      - latest
      - dev
    max_age_days: 90  # Rebuild if base > 90 days old

  runtime_policy:
    no_root: true
    read_only_filesystem: true
    no_privilege_escalation: true
    drop_all_capabilities: true
    no_new_privileges: true
```

### Dockerfile Linting (Hadolint)

```yaml
hadolint_rules:
  error:
    - DL3007  # Using latest tag
    - DL3008  # Pin versions in apt-get
    - DL3009  # Delete apt cache
    - DL3018  # Pin versions in apk
  warning:
    - DL3025  # Use JSON form of CMD
    - DL4006  # Set SHELL option -o pipefail
```

---

## Secrets Management

### Rules

1. **Never** commit secrets to source control
2. **Never** embed secrets in Docker images
3. **Never** log secrets in application output
4. **Never** pass secrets via environment variables in CI logs
5. **Always** use AWS Secrets Manager or Parameter Store
6. **Always** rotate secrets on schedule
7. **Always** use short-lived credentials (OIDC, STS)

### Detection Tools

```yaml
secret_detection:
  pre_commit:
    tool: git-secrets
    patterns:
      - aws_access_key
      - private_key
      - password
      - api_key
      - token
  
  ci:
    tool: trufflehog
    mode: verified_only
    action: block_merge
```

---

## Network Security

### AWS Security Groups

```yaml
security_group_rules:
  - Never allow 0.0.0.0/0 ingress (except ALB on 443)
  - Restrict egress to required destinations only
  - Use security group references (not CIDR) between services
  - Document every rule with description
```

### Kubernetes Network Policies

```yaml
network_policy:
  default: deny_all
  allow:
    - ingress from known services only
    - egress to specific services + DNS
  audit: log denied connections
```

---

## IAM Security

```yaml
iam_standards:
  principle: least_privilege
  rules:
    - No wildcard (*) actions in production
    - No wildcard (*) resources in production
    - Service-specific roles (one per microservice)
    - Time-bounded credentials (STS, OIDC)
    - MFA for all human access
    - Separate roles per environment
  
  review:
    frequency: quarterly
    tool: IAM Access Analyzer
    action: remove_unused_permissions
```

---

## DAST (Dynamic Application Security Testing)

```yaml
dast:
  tool: OWASP ZAP
  trigger: after_staging_deploy
  scan_types:
    - passive  # Every deploy
    - active   # Weekly scheduled
  
  targets:
    - https://staging-api.ilr.example.com
    - https://staging.ilr.example.com
  
  exclusions:
    - /actuator/**  # Health endpoints
    - /swagger-ui/** # Documentation
```

---

## Compliance Checks

```yaml
compliance:
  standards:
    - OWASP Top 10
    - CIS Benchmarks (AWS, Kubernetes, Docker)
    - GDPR (data handling)
  
  automated_checks:
    - AWS Config Rules
    - Kubernetes OPA/Gatekeeper policies
    - Checkov/tfsec for Terraform
  
  audit_trail:
    - CloudTrail (all API calls)
    - Application audit logs
    - Access logs (ALB, S3)
    - Kubernetes audit logs
```

---

## Incident Response (Security)

```yaml
security_incident_response:
  severity_1_critical:
    - Immediately isolate affected service
    - Rotate all credentials
    - Notify security team
    - Begin forensic analysis
    sla: 15 minutes response
  
  severity_2_high:
    - Assess blast radius
    - Patch or mitigate
    - Notify team lead
    sla: 1 hour response
  
  severity_3_medium:
    - Schedule fix in current sprint
    - Monitor for exploitation
    sla: 24 hours response
```
