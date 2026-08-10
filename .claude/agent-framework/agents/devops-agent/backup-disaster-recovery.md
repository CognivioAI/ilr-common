# Backup & Disaster Recovery

## Purpose

Define backup schedules, retention policies, RTO/RPO targets, failover procedures, and DR testing practices.

---

## Recovery Objectives

| Service | RTO (Recovery Time) | RPO (Data Loss) | Priority |
|---------|--------------------|--------------------|----------|
| Database (RDS) | 15 minutes | 5 minutes | Critical |
| Document Storage (S3) | 1 hour | 0 (versioned) | High |
| Application Services | 5 minutes | N/A (stateless) | Critical |
| AI Configuration | 30 minutes | 1 hour | Medium |
| Monitoring Data | 4 hours | 24 hours | Low |

---

## Backup Strategy

### Database (RDS PostgreSQL)

```yaml
rds_backup:
  automated_backups:
    enabled: true
    retention: 35 days
    window: "03:00-04:00 UTC"  # Low traffic
    multi_az: true  # Synchronous standby
  
  snapshots:
    daily: automated (AWS managed)
    weekly: manual snapshot (retained 90 days)
    pre_migration: manual snapshot before any schema change
  
  point_in_time_recovery:
    enabled: true
    granularity: 5 minutes
    retention: 35 days
  
  cross_region:
    enabled: true (production only)
    target_region: eu-west-1 (Ireland)
    frequency: daily
```

### Object Storage (S3)

```yaml
s3_backup:
  versioning:
    enabled: true
    reason: "Protect against accidental deletion"
  
  lifecycle:
    current_version:
      storage_class: STANDARD
    previous_versions:
      transition_to_ia: after 30 days
      transition_to_glacier: after 90 days
      delete: after 365 days
  
  replication:
    enabled: true (production)
    destination: eu-west-1
    type: cross-region replication
  
  deletion_protection:
    mfa_delete: enabled
    bucket_policy: deny s3:DeleteObject without MFA
```

### Kubernetes State

```yaml
kubernetes_backup:
  tool: Velero
  schedule: daily
  retention: 30 days
  
  includes:
    - Deployments
    - ConfigMaps
    - Secrets (encrypted)
    - PVCs
    - Custom Resources
  
  excludes:
    - Pods (recreated by deployments)
    - ReplicaSets (managed by deployments)
    - Events
```

### Secrets

```yaml
secrets_backup:
  aws_secrets_manager:
    versioning: automatic (AWS managed)
    replication: cross-region (eu-west-1)
  
  terraform_state:
    s3_versioning: enabled
    cross_region: replicated to eu-west-1
```

---

## Disaster Recovery Plan

### DR Scenarios

| Scenario | Likelihood | Impact | Recovery Strategy |
|----------|-----------|--------|-------------------|
| Pod failure | High | Low | Auto-restart (K8s) |
| Node failure | Medium | Low | Auto-reschedule (K8s) |
| AZ failure | Low | Medium | Multi-AZ failover |
| Region failure | Very Low | Critical | Cross-region failover |
| Database corruption | Low | Critical | Point-in-time recovery |
| Accidental deletion | Medium | Medium | Restore from backup |
| Security breach | Low | Critical | Isolate + restore clean |

### Multi-AZ Recovery (Automatic)

```yaml
multi_az:
  compute:
    strategy: Pods spread across AZs (topologySpreadConstraints)
    failover: Automatic (K8s scheduler)
    recovery_time: 30-60 seconds
  
  database:
    strategy: Multi-AZ RDS (synchronous replication)
    failover: Automatic (AWS managed)
    recovery_time: 60-120 seconds
  
  load_balancer:
    strategy: Cross-zone load balancing
    failover: Automatic (health check based)
    recovery_time: 15-30 seconds
```

### Cross-Region Recovery (Manual)

```yaml
cross_region_dr:
  trigger: Region-wide outage (extremely rare)
  
  procedure:
    1. Confirm region outage (AWS status page)
    2. Activate DR plan (decision by engineering lead)
    3. Update DNS to point to DR region
    4. Promote read replica to primary (database)
    5. Deploy services in DR region (from ECR cross-region)
    6. Validate service health
    7. Notify customers of recovery
  
  estimated_recovery_time: 2-4 hours
  
  prerequisites:
    - Cross-region RDS read replica
    - Cross-region S3 replication
    - Cross-region ECR replication
    - Terraform for DR region (pre-planned)
    - DNS failover configuration (Route 53)
```

---

## Recovery Procedures

### Database Point-in-Time Recovery

```bash
# Restore to specific point in time
aws rds restore-db-instance-to-point-in-time \
  --source-db-instance-identifier ilr-production-db \
  --target-db-instance-identifier ilr-production-db-restored \
  --restore-time "2025-01-15T10:30:00Z" \
  --db-instance-class db.r6g.large \
  --multi-az

# After validation, switch application to restored DB
# Update connection string in Secrets Manager
# Restart affected services
```

### S3 Object Recovery

```bash
# Restore specific version
aws s3api get-object \
  --bucket ilr-documents \
  --key documents/doc-123.pdf \
  --version-id "version-abc" \
  restored-doc-123.pdf

# Restore all deleted objects (bulk)
# Use S3 Batch Operations for large-scale restores
```

### Kubernetes Restore (Velero)

```bash
# List available backups
velero backup get

# Restore specific namespace
velero restore create --from-backup daily-2025-01-15 \
  --include-namespaces ilr-production

# Restore specific resources
velero restore create --from-backup daily-2025-01-15 \
  --include-resources deployments,configmaps \
  --include-namespaces ilr-production
```

---

## DR Testing

```yaml
dr_testing:
  frequency: quarterly
  scope: Full DR simulation in staging
  
  test_scenarios:
    - Database failover (Multi-AZ)
    - Restore from backup (point-in-time)
    - S3 object recovery
    - Kubernetes namespace restore
    - Secret recovery
    - DNS failover
  
  success_criteria:
    - RTO met for each scenario
    - RPO met (data loss within acceptable range)
    - All services healthy after recovery
    - No data corruption
  
  output:
    - DR test report
    - Gaps identified
    - Improvement actions
    - Updated runbooks
```

---

## Data Classification for Backup

| Data Type | Classification | Backup Frequency | Retention | Encryption |
|-----------|---------------|-----------------|-----------|------------|
| User data | Confidential | Continuous (PITR) | 35 days | AES-256 |
| Documents | Confidential | Real-time (versioning) | 1 year | AES-256 |
| Logs | Internal | Daily | 90 days | AES-256 |
| Metrics | Internal | Daily | 30 days | AES-256 |
| Config | Internal | On change (Git) | Indefinite | At rest |
| AI prompts | Internal | On change (Git) | Indefinite | At rest |
