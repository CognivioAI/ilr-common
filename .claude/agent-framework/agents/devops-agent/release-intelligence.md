# Release Intelligence

## Purpose

Analyze releases before deployment — assess risk, recommend deployment strategy, predict impact, and learn from release history. The agent's "release advisor" capability.

---

## Release Analysis Flow

```
Release Candidate
    │
    ▼
1. ANALYZE — What changed? How much? What's affected?
    │
    ▼
2. ASSESS RISK — Score the release risk
    │
    ▼
3. RECOMMEND STRATEGY — Rolling / Canary / Blue-Green
    │
    ▼
4. PREDICT IMPACT — What could go wrong?
    │
    ▼
5. EXECUTE — Deploy with chosen strategy
    │
    ▼
6. OBSERVE — Monitor for issues post-deploy
    │
    ▼
7. LEARN — Record outcome for future predictions
```

---

## Release Analysis Engine

### Input: Release Metadata

```yaml
release_input:
  service: document-service
  version: 1.3.0
  previous_version: 1.2.0
  
  changes:
    commits: 15
    files_changed: 23
    lines_added: 450
    lines_removed: 120
    
    categories:
      feature: 3 commits (new document classification endpoint)
      bugfix: 8 commits (queue timeout fixes)
      dependency: 2 commits (Spring Boot 3.2.1 → 3.2.2)
      config: 2 commits (logging level, timeout values)
    
    breaking_changes: false
    database_migration: true (add column — non-destructive)
    api_changes: true (new endpoint, no changes to existing)
    ai_model_change: false
    
  affected_services:
    direct: [document-service]
    downstream: [ai-worker (consumes document-service events)]
    
  deployment_window:
    preferred: "Tuesday 10:00 UTC"
    forbidden: "Friday after 15:00, bank holidays"
```

---

## Risk Scoring

### Risk Factors

```yaml
risk_factors:
  change_size:
    weight: 20%
    scoring:
      - commits < 5, files < 10: LOW (1-3)
      - commits 5-15, files 10-30: MEDIUM (4-6)
      - commits > 15, files > 30: HIGH (7-9)
    this_release: MEDIUM (15 commits, 23 files)
  
  change_type:
    weight: 25%
    scoring:
      - config_only: VERY LOW (1)
      - bugfix: LOW (2-3)
      - feature: MEDIUM (4-6)
      - dependency_update: MEDIUM (4-6)
      - database_migration: HIGH (7-8)
      - breaking_change: VERY HIGH (9)
      - ai_model_change: HIGH (7-8)
    this_release: MEDIUM-HIGH (feature + migration = 6)
  
  blast_radius:
    weight: 20%
    scoring:
      - single_service_no_deps: LOW (1-3)
      - single_service_with_deps: MEDIUM (4-6)
      - multiple_services: HIGH (7-8)
      - platform_wide: VERY HIGH (9)
    this_release: MEDIUM (1 service + 1 downstream = 5)
  
  historical_pattern:
    weight: 15%
    scoring:
      - previous_releases_all_clean: LOW (1-3)
      - occasional_issues (< 10%): MEDIUM (4-6)
      - frequent_issues (> 20%): HIGH (7-8)
      - last_release_failed: VERY HIGH (9)
    this_release: LOW (last 5 releases clean = 2)
  
  time_since_last_deploy:
    weight: 10%
    scoring:
      - < 1 week: LOW (1-3) — small batches
      - 1-2 weeks: MEDIUM (4-5)
      - > 2 weeks: HIGH (6-8) — large batch, more unknowns
    this_release: LOW (5 days since last deploy = 2)
  
  reversibility:
    weight: 10%
    scoring:
      - fully_reversible (no data changes): LOW (1-2)
      - mostly_reversible (additive migration): MEDIUM (3-5)
      - partially_reversible (data transformation): HIGH (6-8)
      - irreversible (destructive migration): VERY HIGH (9)
    this_release: MEDIUM (additive column — reversible = 4)
```

### Risk Score Calculation

```yaml
risk_calculation:
  factors:
    change_size: 5 × 0.20 = 1.0
    change_type: 6 × 0.25 = 1.5
    blast_radius: 5 × 0.20 = 1.0
    historical: 2 × 0.15 = 0.3
    time_since: 2 × 0.10 = 0.2
    reversibility: 4 × 0.10 = 0.4
  
  total_risk_score: 4.4 / 9 = 4.9 (MEDIUM)
  
  risk_level: MEDIUM
  confidence: HIGH (good historical data)
```

---

## Strategy Recommendation Engine

```yaml
strategy_recommendation:
  rules:
    risk_low (1-3):
      strategy: rolling_update
      reasoning: "Low risk, simple and fast deployment"
      monitoring: standard (5-minute post-deploy check)
    
    risk_medium (4-6):
      strategy: canary
      reasoning: "Moderate risk — validate with subset of traffic first"
      canary_config:
        initial_weight: 10%
        stages: [10%, 25%, 50%, 100%]
        stage_duration: 10 minutes
        auto_rollback: true
      monitoring: enhanced (metrics comparison, error rate watch)
    
    risk_high (7-8):
      strategy: canary_slow
      reasoning: "High risk — very gradual rollout with extended observation"
      canary_config:
        initial_weight: 5%
        stages: [5%, 10%, 25%, 50%, 100%]
        stage_duration: 30 minutes
        auto_rollback: true
      monitoring: intensive (all metrics, traces, user feedback)
      requires: manual approval at 50% stage
    
    risk_very_high (9):
      strategy: blue_green
      reasoning: "Very high risk — full parallel environment, instant rollback"
      blue_green_config:
        validation_time: 1 hour
        traffic_switch: manual approval
        rollback: instant (DNS/ALB switch)
      monitoring: full observability + manual verification
      requires: team lead approval + on-call aware
  
  this_release:
    recommended_strategy: canary
    reasoning: "Medium risk (4.9): new feature + database migration warrants gradual rollout"
    config:
      stages: [10%, 25%, 50%, 100%]
      stage_duration: 10 minutes
      abort_criteria:
        - error_rate > 2%
        - latency_p95 > 2x baseline
        - any 5xx on new endpoint
```

---

## Impact Prediction

```yaml
impact_prediction:
  potential_issues:
    - risk: "Database migration may slow queries during column addition"
      probability: LOW
      impact: "Temporary latency increase (seconds)"
      mitigation: "Run migration during low traffic, monitor query performance"
    
    - risk: "New endpoint may have unexpected load pattern"
      probability: MEDIUM
      impact: "Potential resource pressure if heavily used"
      mitigation: "Rate limiting on new endpoint, HPA will handle gradual increase"
    
    - risk: "Downstream ai-worker may receive new event format"
      probability: LOW (event format unchanged)
      impact: "Worker processing failures"
      mitigation: "Event schema validated, backward compatible"
  
  confidence: "MEDIUM — new feature introduces some uncertainty"
  
  recommended_monitoring:
    - Watch: new endpoint error rate and latency
    - Watch: database query duration during migration
    - Watch: ai-worker processing success rate
    - Duration: enhanced monitoring for 24 hours post-deploy
```

---

## Release Decision Output

```yaml
release_decision:
  service: document-service
  version: 1.3.0
  
  risk_assessment:
    score: 4.9/9 (MEDIUM)
    level: MEDIUM
    confidence: HIGH
  
  recommendation:
    strategy: canary
    stages: [10%, 25%, 50%, 100%]
    stage_duration: 10 minutes
    total_rollout_time: ~40 minutes
  
  pre_deployment:
    - ✓ Run database migration first (non-blocking, additive)
    - ✓ Verify migration successful before code deploy
    - ✓ Notify ai-worker team of new version
    - ✓ Ensure on-call aware of deployment
  
  abort_criteria:
    - Error rate > 2% at any stage
    - p95 latency > 1000ms (2x baseline)
    - Database connection errors
    - ai-worker processing failures increase
  
  post_deployment:
    - Monitor for 24 hours (enhanced)
    - Review AI processing of new classification type
    - Validate no regression in existing endpoints
  
  approval_required: false (medium risk, canary strategy provides safety)
```

---

## Release History (Learning)

```yaml
release_history:
  - version: 1.2.0
    risk_predicted: LOW (2.8)
    strategy_used: rolling
    outcome: SUCCESS
    actual_issues: none
    time_to_deploy: 8 minutes
  
  - version: 1.1.0
    risk_predicted: MEDIUM (5.1)
    strategy_used: canary
    outcome: SUCCESS
    actual_issues: "Minor latency increase at 25% stage (resolved itself)"
    time_to_deploy: 35 minutes
  
  - version: 1.0.1
    risk_predicted: LOW (1.5)
    strategy_used: rolling
    outcome: SUCCESS
    actual_issues: none
    time_to_deploy: 5 minutes

  accuracy_tracking:
    predictions_correct: 95% (19/20 releases matched predicted risk)
    false_alarms: 1 (predicted HIGH, was actually fine)
    missed_issues: 0 (no surprise failures on LOW-risk predictions)
```

---

## Integration with Other Modules

```yaml
integrations:
  reasoning_engine: "Release intelligence is invoked in DECIDE phase"
  deployment_strategies: "Provides the specific strategy configuration"
  decision_memory: "Records release outcomes for future predictions"
  monitoring_alerting: "Defines what to watch post-deploy"
  rollback_strategy: "Defines abort criteria and rollback trigger"
  autonomy_policy: "High-risk releases may require approval"
```
