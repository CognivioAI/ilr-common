# DR Planner

## Purpose

Autonomous disaster recovery planning — the agent analyzes each service, determines appropriate DR strategy, generates RTO/RPO targets, and validates recoverability.

---

## DR Planning Flow

```
Service Definition
    │
    ▼
1. CLASSIFY — Data criticality and dependencies
    │
    ▼
2. ANALYZE — What can fail? What's the blast radius?
    │
    ▼
3. DESIGN — Select DR strategy per component
    │
    ▼
4. IMPLEMENT — Configure backups, replication, failover
    │
    ▼
5. VALIDATE — Test recovery (quarterly)
    │
    ▼
6. DOCUMENT — Runbook for each recovery scenario
```

---

## Service Classification Engine

```yaml
classification:
  input:
    service: document-service
    data_types:
      - user_documents (confidential, irreplaceable)
      - ai_results (regenerable from source)
      - audit_logs (compliance-required)
      - metadata (configuration, regenerable)
  
  classification_rules:
    tier_1_critical:
      criteria:
        - Data is irreplaceable
        - Revenue-impacting if lost
        - Regulatory requirement to preserve
      rto_target: "< 1 hour"
      rpo_target: "< 5 minutes"
      strategy: multi_az + cross_region
      examples: [user_documents, financial_records, audit_logs]
    
    tier_2_important:
      criteria:
        - Data is important but recoverable
        - Service degradation without it
        - Business operations impacted
      rto_target: "< 4 hours"
      rpo_target: "< 1 hour"
      strategy: multi_az + daily_backup
      examples: [ai_results, processed_data, search_index]
    
    tier_3_operational:
      criteria:
        - Data is regenerable
        - No direct user impact if temporarily lost
        - Convenience, not necessity
      rto_target: "< 24 hours"
      rpo_target: "< 24 hours"
      strategy: daily_backup
      examples: [caches, temporary_state, analytics]
```

---

## DR Strategy Generation

### Per-Component Strategy

```yaml
dr_strategy:
  input:
    application: ILR Document Processing
    components:
      - name: document-service (API)
        type: stateless
        tier: 1
      - name: ai-worker
        type: stateless
        tier: 2
      - name: postgresql (RDS)
        type: stateful
        tier: 1
        data: user metadata, relationships
      - name: s3 (documents)
        type: stateful
        tier: 1
        data: user uploaded documents
      - name: sqs (processing queue)
        type: transient
        tier: 2
        data: in-flight messages
  
  output:
    rds_postgresql:
      strategy:
        availability: Multi-AZ (automatic failover)
        backup: automated daily snapshots (retention: 35 days)
        point_in_time: enabled (5-minute granularity)
        cross_region: read replica in eu-west-1 (DR region)
      rto: 15 minutes (Multi-AZ failover)
      rpo: 5 minutes (point-in-time recovery)
      recovery_procedure: "Automatic failover for AZ failure. Promote DR replica for region failure."
    
    s3_documents:
      strategy:
        durability: 99.999999999% (11 nines — S3 standard)
        versioning: enabled (protect against accidental deletion)
        replication: cross-region replication to eu-west-1
        deletion_protection: MFA delete enabled
      rto: 1 hour (DNS switch to DR region bucket)
      rpo: 0 (cross-region replication is near-real-time)
      recovery_procedure: "Switch application to DR region bucket. Data already replicated."
    
    ecs_services:
      strategy:
        availability: Multi-AZ (tasks spread across AZs)
        recovery: automatic (ECS restarts failed tasks)
        dr_region: Terraform modules pre-configured for eu-west-1
      rto: 5 minutes (within region), 2 hours (cross-region)
      rpo: N/A (stateless — no data to lose)
      recovery_procedure: "Within region: automatic. Cross-region: deploy from ECR to DR ECS cluster."
    
    sqs_queue:
      strategy:
        durability: messages replicated across AZs
        retention: 14 days
        dlq: configured (max 3 retries)
      rto: 0 (SQS is multi-AZ by default)
      rpo: 0 (messages preserved)
      recovery_procedure: "SQS self-recovers. For complete loss: replay from source."
```

---

## Failure Scenarios

```yaml
failure_scenarios:
  scenario_1_pod_failure:
    probability: HIGH (weekly)
    impact: LOW
    detection: health check failure (< 30 seconds)
    recovery: Kubernetes auto-restart (< 60 seconds)
    human_action: none
    tested: continuously (by design)
  
  scenario_2_az_failure:
    probability: LOW (yearly)
    impact: MEDIUM
    detection: ALB health checks + CloudWatch (< 2 minutes)
    recovery:
      compute: automatic (K8s/ECS reschedules to surviving AZ)
      database: automatic (RDS Multi-AZ failover: 60-120s)
      load_balancer: automatic (cross-zone balancing)
    human_action: monitor recovery, assess data integrity
    tested: quarterly (game day)
  
  scenario_3_region_failure:
    probability: VERY LOW (rare)
    impact: CRITICAL
    detection: AWS status page + Route 53 health checks (< 5 minutes)
    recovery:
      dns: Route 53 failover to DR region (manual trigger)
      database: promote read replica to primary (10-15 minutes)
      compute: deploy services in DR region from ECR (30-60 minutes)
      storage: already replicated (S3 cross-region)
    human_action: decision to activate DR, coordinate recovery
    estimated_rto: 2-4 hours
    tested: annually (tabletop + partial technical test)
  
  scenario_4_database_corruption:
    probability: LOW
    impact: CRITICAL
    detection: application errors + data integrity checks
    recovery:
      method: point-in-time recovery
      granularity: 5-minute intervals
      process:
        1. Identify corruption timestamp
        2. Restore to point before corruption
        3. Assess data loss (RPO: 5 minutes)
        4. Restart services against restored database
    human_action: determine corruption point, authorize restore
    estimated_rto: 30-60 minutes
    tested: quarterly (restore drill)
  
  scenario_5_accidental_deletion:
    probability: MEDIUM
    impact: varies
    detection: application errors or user reports
    recovery:
      s3: restore from version (immediate)
      rds: restore from snapshot or PITR
      kubernetes: restore from Velero backup
      terraform: re-apply (resources recreated)
    human_action: identify what was deleted, authorize restore
    prevention: MFA delete, IAM restrictions, break-glass audit
```

---

## DR Validation (Testing)

```yaml
dr_testing:
  quarterly:
    - name: Database Failover Test
      environment: staging
      procedure:
        1. Trigger RDS failover (reboot with failover)
        2. Measure downtime
        3. Verify application recovers automatically
        4. Confirm no data loss
      success_criteria:
        - Downtime < 120 seconds
        - Zero data loss
        - Application self-heals
    
    - name: Point-in-Time Recovery Test
      environment: staging
      procedure:
        1. Insert known test data
        2. Wait 5 minutes
        3. Insert "corruption" data
        4. Restore to pre-corruption point
        5. Verify test data present, corruption absent
      success_criteria:
        - Recovery completes in < 30 minutes
        - Correct data restored
        - RPO <= 5 minutes confirmed
    
    - name: S3 Recovery Test
      environment: staging
      procedure:
        1. Upload test document
        2. Delete document
        3. Restore from version history
        4. Verify document intact
      success_criteria:
        - Recovery immediate (< 1 minute)
        - Document byte-for-byte identical
  
  annually:
    - name: Cross-Region DR Drill
      environment: DR region (eu-west-1)
      procedure:
        1. Simulate primary region unavailable
        2. Execute DR runbook
        3. Deploy services in DR region
        4. Switch DNS
        5. Validate full functionality
      success_criteria:
        - RTO < 4 hours
        - All services functional in DR region
        - Data consistent (replicated)
      
  after_every_test:
    - Update runbooks with findings
    - Record actual RTO/RPO vs targets
    - Create tickets for gaps
    - Update decision memory
```

---

## DR Runbook (Auto-Generated)

```yaml
runbook_template:
  title: "DR Runbook: Region Failure Recovery"
  
  pre_requisites:
    ✓ DR region infrastructure pre-provisioned (Terraform)
    ✓ RDS read replica in DR region (current)
    ✓ S3 cross-region replication active
    ✓ ECR images replicated to DR region
    ✓ Route 53 health check configured
    ✓ DR Terraform state separate and accessible
  
  procedure:
    step_1:
      action: "Confirm region outage (not transient)"
      check: "AWS status page, multiple services affected"
      time: "5 minutes"
    
    step_2:
      action: "Activate incident response"
      check: "Notify stakeholders, assign incident commander"
      time: "5 minutes"
    
    step_3:
      action: "Promote RDS read replica to primary"
      command: "aws rds promote-read-replica --db-instance-identifier ilr-dr-replica"
      time: "10-15 minutes"
    
    step_4:
      action: "Deploy services in DR region"
      command: "terraform apply -var-file=environments/dr/terraform.tfvars"
      time: "15-30 minutes"
    
    step_5:
      action: "Switch DNS to DR region"
      command: "aws route53 ... (update alias to DR ALB)"
      time: "5 minutes (+ DNS propagation)"
    
    step_6:
      action: "Validate services healthy"
      check: "Run smoke tests against DR endpoint"
      time: "10 minutes"
    
    step_7:
      action: "Monitor and communicate"
      check: "Dashboards healthy, notify customers of recovery"
  
  total_estimated_time: "2-4 hours"
  
  rollback_to_primary:
    trigger: "Primary region recovered"
    procedure: "Reverse DNS, resync data, demote DR to replica"
    caution: "Data written to DR during outage must be synced back"
```
