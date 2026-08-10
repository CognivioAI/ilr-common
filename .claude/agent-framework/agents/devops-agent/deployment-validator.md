# Deployment Validator

## Purpose

Automated pre-deployment validation — every artifact is checked against standards before execution. The agent's safety net that catches issues before they reach production.

---

## Validation Flow

```
Artifacts Generated (by Reasoning Engine)
    │
    ▼
┌─────────────────────────────────────┐
│    DEPLOYMENT VALIDATOR              │
│                                     │
│  ┌─────────────┐ ┌─────────────┐   │
│  │ Kubernetes  │ │  Terraform  │   │
│  │ Validator   │ │  Validator  │   │
│  └─────────────┘ └─────────────┘   │
│                                     │
│  ┌─────────────┐ ┌─────────────┐   │
│  │  Docker     │ │   Security  │   │
│  │  Validator  │ │   Validator │   │
│  └─────────────┘ └─────────────┘   │
│                                     │
│  ┌─────────────┐ ┌─────────────┐   │
│  │   Cost      │ │  Compliance │   │
│  │  Validator  │ │  Validator  │   │
│  └─────────────┘ └─────────────┘   │
└─────────────────────────────────────┘
    │
    ▼
ALL PASS → Proceed to execution
ANY FAIL → Block, report, recommend fix
```

---

## Kubernetes Validation

### Mandatory Checks

```yaml
kubernetes_checks:
  resource_limits:
    check: "All containers have CPU and memory requests AND limits"
    severity: BLOCKING
    rule: |
      spec.containers[*].resources.requests.cpu must exist
      spec.containers[*].resources.requests.memory must exist
      spec.containers[*].resources.limits.cpu must exist
      spec.containers[*].resources.limits.memory must exist
    fix: "Add resource block with appropriate values"
  
  health_probes:
    check: "Liveness and readiness probes configured"
    severity: BLOCKING
    rule: |
      spec.containers[*].livenessProbe must exist
      spec.containers[*].readinessProbe must exist
    fix: "Add liveness (/actuator/health/liveness) and readiness (/actuator/health/readiness)"
  
  security_context:
    check: "Running as non-root with restricted capabilities"
    severity: BLOCKING
    rule: |
      spec.securityContext.runAsNonRoot == true
      spec.containers[*].securityContext.allowPrivilegeEscalation == false
      spec.containers[*].securityContext.capabilities.drop contains "ALL"
    fix: "Add security context block"
  
  replica_count:
    check: "Production has minimum 2 replicas"
    severity: BLOCKING (production only)
    rule: |
      if environment == production: spec.replicas >= 2
    fix: "Increase replicas to minimum 2 for HA"
  
  pod_disruption_budget:
    check: "PDB exists for production deployments"
    severity: WARNING (blocking in production)
    rule: |
      PodDisruptionBudget exists with matching selector
      pdb.spec.minAvailable >= 1 OR pdb.spec.maxUnavailable <= 1
    fix: "Create PDB with minAvailable: 1"
  
  network_policy:
    check: "NetworkPolicy restricts ingress and egress"
    severity: BLOCKING (production)
    rule: |
      NetworkPolicy exists with podSelector matching deployment
      policyTypes contains both Ingress and Egress
    fix: "Create NetworkPolicy allowing only required traffic"
  
  image_tag:
    check: "No 'latest' tag used"
    severity: BLOCKING
    rule: |
      spec.containers[*].image must not end with ":latest"
      spec.containers[*].image must contain ":" (explicit tag)
    fix: "Use specific version tag (e.g., 1.2.0 or sha256 digest)"
  
  topology_spread:
    check: "Pods spread across availability zones"
    severity: WARNING (blocking in production)
    rule: |
      spec.topologySpreadConstraints exists
      topologyKey == "topology.kubernetes.io/zone"
    fix: "Add topologySpreadConstraints with zone key"
  
  labels:
    check: "Standard labels present"
    severity: WARNING
    rule: |
      metadata.labels contains:
        app.kubernetes.io/name
        app.kubernetes.io/version
        app.kubernetes.io/component
    fix: "Add standard Kubernetes labels"
  
  hpa:
    check: "HPA configured for production"
    severity: WARNING (blocking in production)
    rule: |
      HorizontalPodAutoscaler exists targeting this deployment
      hpa.spec.minReplicas >= 2 (production)
      hpa.spec.maxReplicas >= 3
    fix: "Create HPA with CPU/memory targets"
```

---

## Terraform Validation

```yaml
terraform_checks:
  syntax:
    check: "terraform validate passes"
    severity: BLOCKING
    tool: "terraform validate"
  
  formatting:
    check: "terraform fmt consistent"
    severity: WARNING
    tool: "terraform fmt -check"
  
  security_scan:
    check: "No HIGH/CRITICAL findings"
    severity: BLOCKING
    tool: "tfsec + checkov"
    rules:
      - No public S3 buckets
      - All storage encrypted
      - No wildcard IAM permissions
      - No 0.0.0.0/0 ingress (except ALB on 443)
      - RDS encryption enabled
      - CloudTrail enabled
  
  state_safety:
    check: "Remote state configured with locking"
    severity: BLOCKING
    rule: |
      backend "s3" configured
      dynamodb_table for locking specified
      encrypt = true
  
  resource_tagging:
    check: "All resources have required tags"
    severity: BLOCKING
    rule: |
      All resources include tags:
        Environment, Service, Owner, ManagedBy
  
  cost_estimate:
    check: "Cost within budget"
    severity: WARNING (blocking if > 150% of budget)
    tool: "infracost"
    rule: |
      monthly_cost <= allocated_budget * 1.5
  
  plan_review:
    check: "No unexpected destructive changes"
    severity: BLOCKING
    rule: |
      terraform plan contains no:
        - destroy of stateful resources (databases, S3)
        - replacement of critical infrastructure
      without explicit acknowledgment
```

---

## Docker Validation

```yaml
docker_checks:
  dockerfile_lint:
    check: "Follows best practices"
    severity: WARNING
    tool: "hadolint"
    rules:
      - No 'latest' base image tag
      - Pin package versions
      - Multi-stage build used
      - USER directive present (non-root)
      - HEALTHCHECK defined
  
  image_scan:
    check: "No CRITICAL/HIGH vulnerabilities"
    severity: BLOCKING
    tool: "trivy"
    thresholds:
      critical: 0 (blocking)
      high: 0 (blocking)
      medium: warning only
      low: informational
  
  image_size:
    check: "Image not excessively large"
    severity: WARNING
    rule: |
      Java service: < 300MB
      Node service: < 200MB
      Nginx/static: < 50MB
  
  non_root:
    check: "Container runs as non-root"
    severity: BLOCKING
    rule: |
      USER directive in Dockerfile
      USER is not root (UID != 0)
  
  secrets_check:
    check: "No secrets baked into image"
    severity: BLOCKING
    rule: |
      No .env files copied
      No credentials in ENV instructions
      No private keys in image layers
```

---

## Security Validation

```yaml
security_checks:
  iam_least_privilege:
    check: "IAM roles have minimal permissions"
    severity: BLOCKING
    rules:
      - No "Action": "*" in production
      - No "Resource": "*" in production
      - Permissions scoped to specific ARNs
      - No inline policies (use managed)
  
  encryption:
    check: "All data encrypted at rest and in transit"
    severity: BLOCKING
    rules:
      - RDS encryption enabled
      - S3 encryption enabled
      - EBS encryption enabled
      - TLS for all endpoints (minimum 1.2)
      - Secrets Manager for sensitive data
  
  network_isolation:
    check: "Services properly isolated"
    severity: BLOCKING
    rules:
      - Database in private subnet (no internet access)
      - Applications in private subnet (NAT for outbound)
      - Only ALB in public subnet
      - Security groups restrict source to specific SGs
  
  secrets_handling:
    check: "No plaintext secrets anywhere"
    severity: BLOCKING
    rules:
      - No secrets in Terraform tfvars
      - No secrets in Helm values
      - No secrets in environment variable definitions
      - All secrets referenced via Secrets Manager or External Secrets
  
  audit_logging:
    check: "Audit trail enabled"
    severity: BLOCKING (production)
    rules:
      - CloudTrail enabled
      - Application audit logging configured
      - ALB access logs enabled
      - S3 access logs enabled (for sensitive buckets)
```

---

## Cost Validation

```yaml
cost_checks:
  budget_compliance:
    check: "Estimated cost within allocated budget"
    severity: WARNING (blocking if > 150%)
    tool: "infracost + internal estimation"
    rule: |
      estimated_monthly <= service_budget
  
  over_provisioning:
    check: "Resources not excessively large for workload"
    severity: WARNING
    rules:
      - dev: instance class not larger than t4g.small
      - staging: instance class not larger than t4g.medium
      - production: justified by load requirements
  
  unnecessary_resources:
    check: "No resources provisioned without justification"
    severity: WARNING
    rules:
      - NAT Gateway: only if outbound internet needed
      - ElastiCache: only if cache hit rate > 30%
      - Multi-AZ: only if availability SLO requires
```

---

## Compliance Validation

```yaml
compliance_checks:
  data_residency:
    check: "All resources in approved regions"
    severity: BLOCKING
    rule: "All resources in eu-west-2 (exception: DR in eu-west-1)"
  
  gdpr:
    check: "GDPR requirements met"
    severity: BLOCKING
    rules:
      - Data encryption at rest
      - Audit logging enabled
      - Data retention policy configured
      - PII handling documented
  
  backup:
    check: "Backup configured for stateful resources"
    severity: BLOCKING (production)
    rules:
      - RDS automated backup enabled (retention >= 7 days)
      - S3 versioning enabled
      - Recovery tested within last quarter
```

---

## Validation Output

```yaml
validation_report:
  format:
    service: document-service
    environment: production
    timestamp: "2026-07-08T10:00:00Z"
    
    results:
      kubernetes:
        passed: 8
        warnings: 1
        blocked: 0
        details:
          - ✅ Resource limits configured
          - ✅ Health probes defined
          - ✅ Security context set
          - ✅ Replicas >= 2
          - ✅ PDB exists
          - ✅ Network policy defined
          - ✅ No latest tag
          - ✅ Labels present
          - ⚠️ Topology spread: recommended but not configured
      
      terraform:
        passed: 5
        warnings: 0
        blocked: 0
      
      docker:
        passed: 4
        warnings: 0
        blocked: 0
      
      security:
        passed: 4
        warnings: 0
        blocked: 0
      
      cost:
        passed: 2
        warnings: 1
        blocked: 0
        details:
          - ⚠️ Cost 20% above estimate (within acceptable range)
    
    verdict: ✅ PASS (proceed to deployment)
    warnings_to_address: 2
    blocking_issues: 0
```

---

## Integration with Reasoning Engine

```yaml
integration:
  when_called: "After ACT phase generates artifacts, before execution"
  if_all_pass: "Proceed to execution (respect autonomy policy)"
  if_warnings: "Proceed with notification, log for improvement"
  if_blocked: "Halt execution, report blockers, suggest fixes"
  auto_fix: "For simple issues (missing labels, formatting), fix and re-validate"
```
