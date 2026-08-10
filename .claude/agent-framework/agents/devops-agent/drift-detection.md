# Drift Detection

## Purpose

Continuously monitor for differences between desired state (Git/Terraform) and actual state (AWS/Kubernetes). Detect drift, classify severity, and autonomously remediate or escalate.

---

## Drift Detection Architecture

```
┌────────────────┐      ┌────────────────┐
│ Desired State  │      │  Actual State  │
│                │      │                │
│ Terraform Code │      │ AWS Resources  │
│ Helm Charts    │◄────▶│ K8s Cluster    │
│ GitOps Config  │      │ Docker Images  │
└────────────────┘      └────────────────┘
         │                      │
         └──────────┬───────────┘
                    │
                    ▼
         ┌──────────────────┐
         │  DRIFT DETECTOR  │
         │                  │
         │  Compare states  │
         │  Classify drift  │
         │  Decide action   │
         └──────────────────┘
                    │
         ┌──────────┼──────────┐
         │          │          │
         ▼          ▼          ▼
    Auto-Fix    Alert     Escalate
    (safe)     (review)   (human)
```

---

## Drift Categories

### Infrastructure Drift (Terraform)

```yaml
terraform_drift:
  detection:
    method: "terraform plan (detect changes outside Terraform)"
    frequency: daily (scheduled) + on-demand
    command: "terraform plan -detailed-exitcode"
    exit_codes:
      0: no changes (no drift)
      1: error
      2: changes detected (DRIFT)
  
  classification:
    critical:
      examples:
        - Security group rules modified (ports opened)
        - IAM policy changed (permissions added)
        - Encryption disabled on resource
        - Public access enabled on S3/RDS
      severity: P1
      action: IMMEDIATE_REMEDIATION
      autonomy: auto-fix (revert to Terraform state)
    
    high:
      examples:
        - Resource tags removed
        - Backup configuration changed
        - Monitoring configuration altered
        - Network configuration modified
      severity: P2
      action: REMEDIATION_WITH_NOTIFICATION
      autonomy: auto-fix + notify team
    
    medium:
      examples:
        - Instance type changed (manual scaling)
        - Storage size increased
        - Desired count modified (auto-scaler)
      severity: P3
      action: ALERT_AND_REVIEW
      autonomy: alert team, await decision
    
    low:
      examples:
        - Description fields modified
        - Non-functional tags added
        - Console-added metadata
      severity: P4
      action: LOG_AND_PLAN
      autonomy: record, fix in next apply
```

### Kubernetes Drift (GitOps)

```yaml
kubernetes_drift:
  detection:
    method: "ArgoCD sync status + custom checks"
    frequency: continuous (3-minute intervals)
    tools: [ArgoCD, kubectl diff, kube-diff]
  
  classification:
    security_drift:
      examples:
        - SecurityContext removed from pod
        - NetworkPolicy deleted
        - ServiceAccount permissions escalated
        - Secrets exposed in configmap
      severity: CRITICAL
      action: auto-revert immediately
    
    configuration_drift:
      examples:
        - Replica count changed manually
        - Resource limits modified
        - Environment variables changed
        - Image tag updated outside GitOps
      severity: HIGH
      action: revert to Git state (ArgoCD self-heal)
    
    acceptable_drift:
      examples:
        - HPA-managed replica count
        - Pod restart counts
        - Auto-generated annotations
        - Controller-managed labels
      severity: NONE (expected)
      action: ignore (excluded from detection)
```

### Configuration Drift

```yaml
configuration_drift:
  detection:
    method: "Compare running config vs committed config"
    frequency: hourly
  
  checks:
    application_config:
      - Spring profiles active match environment
      - Log levels not changed from committed values
      - Feature flags match configuration source
    
    secrets:
      - Secrets last rotated within policy
      - No expired certificates
      - No disabled rotation schedules
    
    monitoring:
      - Alert rules match committed definitions
      - Dashboards not manually modified
      - SLO targets match configuration
```

---

## Drift Remediation Engine

### Decision Process

```yaml
remediation_decision:
  step_1_classify:
    action: "Determine drift severity and category"
  
  step_2_assess_risk:
    questions:
      - "Is this drift a security risk?"
      - "Was this an intentional manual change?"
      - "Does reverting cause downtime?"
      - "Is there data loss risk in reverting?"
  
  step_3_decide:
    if security_risk AND auto_revert_safe:
      action: AUTO_REMEDIATE
      notify: immediately after
    
    elif intentional_change:
      action: CREATE_PR
      description: "Capture manual change in code (codify the drift)"
    
    elif revert_causes_downtime:
      action: SCHEDULE_REMEDIATION
      window: "Next maintenance window"
    
    elif data_loss_risk:
      action: ESCALATE_TO_HUMAN
      urgency: HIGH
    
    else:
      action: AUTO_REMEDIATE
      method: "terraform apply / argocd sync"
```

### Remediation Actions

```yaml
remediation_actions:
  auto_revert:
    terraform: "terraform apply -auto-approve (restore desired state)"
    kubernetes: "ArgoCD self-heal (auto-sync enabled)"
    when: "Drift is clearly undesired and safe to revert"
  
  create_pr:
    action: "Update Terraform/Helm code to match new actual state"
    when: "Drift was intentional (someone made valid manual change)"
    process:
      1. Detect drift
      2. Generate code change that codifies the drift
      3. Create PR with description of what changed and why
      4. Notify team for review
  
  schedule_fix:
    action: "Plan remediation for safe window"
    when: "Reverting now would cause disruption"
    process:
      1. Record drift and planned fix time
      2. Schedule for maintenance window
      3. Monitor for security implications in meantime
  
  escalate:
    action: "Alert human with full context"
    when: "Agent cannot safely determine correct action"
    provide:
      - What drifted (before/after)
      - When drift occurred (if detectable)
      - Who/what caused it (CloudTrail)
      - Risk assessment
      - Recommended action
```

---

## Drift Detection Schedule

```yaml
schedule:
  continuous:
    - ArgoCD sync monitoring (every 3 minutes)
    - Kubernetes NetworkPolicy validation
    - Certificate expiry checks
  
  hourly:
    - Security group rule verification
    - IAM policy compliance check
    - Encryption status verification
  
  daily:
    - Full terraform plan (all environments)
    - S3 bucket policy validation
    - Backup configuration check
    - Cost anomaly detection
  
  weekly:
    - Comprehensive infrastructure audit
    - Unused resource identification
    - Permission usage review (IAM Access Analyzer)
```

---

## Drift Metrics and Reporting

```yaml
metrics:
  track:
    - drift_events_total (by category, severity, environment)
    - drift_time_to_detect (how quickly we find drift)
    - drift_time_to_remediate (how quickly we fix it)
    - drift_auto_remediated_total (autonomous fixes)
    - drift_human_escalated_total (needed human)
    - drift_source (console change, API call, unknown)
  
  alerts:
    - Critical drift detected → P1 alert
    - Drift rate increasing → P3 alert (process issue)
    - Auto-remediation failed → P2 alert
  
  dashboard:
    - Current drift count by environment
    - Drift trend (is it improving or worsening?)
    - Top drift sources (who/what causes most drift)
    - Remediation success rate

reporting:
  weekly:
    - Total drift events
    - Auto-remediated vs escalated
    - Root causes (console access? Automation? Bug?)
  
  monthly:
    - Drift trend (improving?)
    - Policy changes needed
    - Console access restrictions to propose
```

---

## Prevention (Reduce Drift)

```yaml
drift_prevention:
  technical:
    - ArgoCD self-heal enabled for all production resources
    - Terraform state refresh on every plan
    - Immutable infrastructure (replace, don't modify)
    - AWS Config rules for compliance enforcement
  
  process:
    - No console changes to production (policy)
    - All changes via Git PR (enforced by IAM)
    - Break-glass procedure documented (for emergencies)
    - Emergency console access logs auto-create codification PR
  
  cultural:
    - Team understands "Git is the source of truth"
    - Monthly review of console access patterns
    - Celebrate zero-drift streaks
```

---

## Integration with Other Modules

```yaml
integrations:
  gitops: "ArgoCD provides continuous K8s drift detection"
  terraform_standards: "terraform plan as drift detection tool"
  security_devsecops: "Security drift = highest priority"
  compliance: "Compliance drift = regulatory risk"
  autonomy_policy: "Defines what agent can auto-fix vs escalate"
  incident_response_agent: "Drift may be symptom of incident"
  decision_memory: "Record drift events for pattern detection"
```
