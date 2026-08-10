# Reasoning Engine

## Purpose

Define how the DevOps Agent **thinks** — the autonomous reasoning loop that transforms inputs into validated decisions and executable artifacts. This is the agent's cognitive architecture.

---

## Reasoning Loop

```
┌─────────────────────────────────────────────────────────────┐
│                  DEVOPS AGENT REASONING LOOP                  │
│                                                              │
│  ┌──────────┐   ┌──────────┐   ┌──────────┐   ┌─────────┐ │
│  │ PERCEIVE │──▶│  REASON  │──▶│  DECIDE  │──▶│   ACT   │ │
│  └──────────┘   └──────────┘   └──────────┘   └─────────┘ │
│       ▲                                             │       │
│       │              ┌──────────┐                   │       │
│       └──────────────│  LEARN   │◀──────────────────┘       │
│                      └──────────┘                           │
└─────────────────────────────────────────────────────────────┘
```

---

## Phase 1: PERCEIVE (Understand Context)

```yaml
perceive:
  purpose: "Gather all relevant information before reasoning"
  
  inputs:
    architecture_output:
      source: Architecture Agent
      contains: [services, technology_decisions, nfrs, integrations, security]
      parse: "Extract service definitions, constraints, requirements"
    
    current_state:
      source: Infrastructure (Terraform state, K8s cluster, AWS)
      contains: [running_services, resource_usage, costs, alerts]
      parse: "Understand what exists and its health"
    
    constraints:
      source: Organization policies, budgets, compliance
      contains: [budget_limit, team_size, compliance_rules, technology_restrictions]
      parse: "Identify hard constraints vs preferences"
    
    history:
      source: Decision Memory (decision-memory.md)
      contains: [past_decisions, outcomes, incidents, patterns]
      parse: "What has worked/failed before in similar situations"
  
  output:
    context_object:
      services_to_deploy: [list]
      constraints: {budget, team_size, compliance}
      current_infrastructure: {state}
      relevant_history: [past_decisions]
      gaps: [missing_information]
  
  actions_if_incomplete:
    - If architecture output missing key info → request from Architecture Agent
    - If constraints unclear → ask human operator
    - If current state unknown → query infrastructure tools
    - Never proceed with insufficient context on critical decisions
```

---

## Phase 2: REASON (Analyze and Generate Options)

```yaml
reason:
  purpose: "Generate multiple options and evaluate trade-offs"
  
  step_1_decompose:
    action: "Break the problem into sub-decisions"
    example:
      input: "Deploy document-service to production"
      sub_decisions:
        - Compute selection (ECS vs EKS vs Lambda)
        - Database provisioning (RDS vs Aurora)
        - Networking topology (VPC, subnets, security groups)
        - Deployment strategy (rolling vs canary vs blue-green)
        - Monitoring configuration (metrics, alerts, dashboards)
        - Cost estimation and optimization
  
  step_2_generate_options:
    action: "For each sub-decision, generate 2-4 viable options"
    method:
      - Load relevant module (cloud-decision-engine.md)
      - Apply constraints filter (eliminate infeasible options)
      - Generate remaining candidates
    example:
      compute_options:
        - option_a: ECS Fargate
        - option_b: EKS
        - option_c: Lambda (eliminated — long-running service)
  
  step_3_evaluate:
    action: "Score each option against decision framework"
    criteria: [cost, ops_complexity, scalability, security, reliability, dx]
    method:
      - Apply weights from decision-framework.md
      - Score 1-9 per criterion
      - Calculate weighted total
      - Document reasoning for each score
    example:
      ecs_fargate:
        cost: 7, ops: 8, scale: 7, security: 8, reliability: 8, dx: 7
        weighted_total: 7.5
      eks:
        cost: 5, ops: 4, scale: 9, security: 8, reliability: 8, dx: 5
        weighted_total: 6.2
  
  step_4_check_history:
    action: "Does decision memory have relevant precedents?"
    query: "Similar service deployed before? What was chosen? How did it perform?"
    influence: "Adjust confidence based on historical outcomes"
  
  step_5_identify_risks:
    action: "What could go wrong with each option?"
    for_each_option:
      - Technical risks
      - Cost risks
      - Operational risks
      - Lock-in risks
    mitigation: "Define mitigation strategy for recommended option"
```

---

## Phase 3: DECIDE (Select and Commit)

```yaml
decide:
  purpose: "Select the best option with documented reasoning"
  
  decision_process:
    1_rank: "Order options by weighted score"
    2_validate_constraints:
      - Does it fit budget? (hard constraint)
      - Does team have skills? (soft constraint)
      - Does it meet compliance? (hard constraint)
      - Does it meet availability NFR? (hard constraint)
    3_apply_precedent:
      - If similar decision exists with good outcome → increase confidence
      - If similar decision failed before → flag and explain difference
    4_determine_autonomy:
      - Check autonomy-policy.md
      - Can agent execute autonomously? Or need approval?
    5_commit:
      - Record decision with full reasoning
      - Generate ADR if significant
      - Set confidence level (HIGH/MEDIUM/LOW)
  
  output:
    decision:
      selected: "ECS Fargate"
      confidence: HIGH
      reasoning: "Best cost/ops ratio for small team, meets all NFRs"
      alternatives_rejected:
        - eks: "Overkill for 2 services, team lacks K8s experience"
      risks:
        - "4 vCPU task limit (not a concern at current scale)"
      trigger_to_revisit:
        - "Service count > 5"
        - "Need GPU compute for AI"
      requires_approval: false
      adr: "ADR-DEVOPS-003"
```

---

## Phase 4: ACT (Generate and Execute)

```yaml
act:
  purpose: "Transform decision into executable artifacts and deploy"
  
  step_1_generate_artifacts:
    action: "Produce all required infrastructure and deployment code"
    outputs:
      - Terraform modules (infrastructure)
      - Helm charts (deployment)
      - CI/CD pipeline (automation)
      - Monitoring configuration (observability)
      - Security policies (hardening)
      - Cost estimate (budget validation)
  
  step_2_validate_artifacts:
    action: "Run validation before execution"
    checks:
      terraform: "terraform validate + tfsec + checkov"
      kubernetes: "kubeval + OPA policies"
      docker: "hadolint + trivy scan"
      cost: "infracost estimate within budget"
      security: "all security gates pass"
    gate: "All validation must pass before proceeding"
  
  step_3_execute:
    action: "Apply artifacts to target environment"
    sequence:
      1. terraform plan → review → apply (infrastructure)
      2. docker build → scan → push (images)
      3. helm upgrade → wait → health check (deployment)
      4. smoke tests → monitoring validation (verification)
    autonomy:
      dev: execute immediately
      staging: execute immediately
      production: request approval, then execute
  
  step_4_verify:
    action: "Confirm execution was successful"
    checks:
      - All pods healthy and ready
      - Health endpoints returning 200
      - Error rate within baseline
      - Latency within SLO
      - No new security findings
      - Cost tracking active
    if_failed:
      - Trigger rollback (automatic for production)
      - Log failure details
      - Notify team
      - Update decision memory (negative outcome)
```

---

## Phase 5: LEARN (Update Knowledge)

```yaml
learn:
  purpose: "Improve future decisions based on outcomes"
  
  record_outcome:
    what_to_store:
      - Decision made (with full context)
      - Outcome (success/failure/partial)
      - Performance metrics (cost, reliability, speed)
      - Time to execute
      - Issues encountered
      - Human overrides (if any)
    where: decision-memory.md (structured knowledge base)
  
  update_models:
    - If decision succeeded → increase confidence in similar pattern
    - If decision failed → analyze why, update scoring criteria
    - If human overrode → understand reasoning, adjust autonomy
  
  identify_patterns:
    - Recurring failures → create automated prevention
    - Cost overruns → adjust estimation models
    - Successful patterns → promote to golden path
  
  improve_process:
    - Reduce time spent on well-known decisions
    - Increase accuracy of cost estimates
    - Better predict risks based on history
```

---

## Reasoning Example (End-to-End)

```yaml
# Complete reasoning trace for a real scenario

scenario: "Architecture Agent delivers design for document-service"

perceive:
  architecture_input:
    service: document-service
    type: api
    runtime: java-21
    nfrs: {availability: 99.9%, latency_p95: 500ms}
    dependencies: [postgresql, s3, sqs, bedrock]
    team_size: 2
    budget: £500/month total
  current_state:
    existing_infrastructure: none (greenfield)
  history:
    similar_decision: none (first service)

reason:
  sub_decisions:
    1_compute:
      options: [ecs_fargate(7.5), eks(6.2), app_runner(7.0)]
      winner: ecs_fargate
    2_database:
      options: [rds_postgres(8.0), aurora(7.0), aurora_serverless(6.5)]
      winner: rds_postgres
    3_deployment:
      options: [rolling(8.0), canary(7.0), blue_green(7.5)]
      winner: rolling (low risk first deployment)
    4_monitoring:
      options: [cloudwatch(7.0), prometheus_grafana(8.5)]
      winner: prometheus_grafana

decide:
  selected_architecture:
    compute: ECS Fargate
    database: RDS PostgreSQL (db.t4g.medium)
    storage: S3
    queue: SQS
    monitoring: Prometheus + Grafana
    deployment: Rolling update
  confidence: HIGH
  estimated_cost: £240/month
  within_budget: true (£240 < £500)
  requires_approval: false (dev environment)

act:
  generate:
    - terraform/modules/ecs-service/
    - terraform/modules/rds/
    - terraform/modules/s3/
    - terraform/modules/sqs/
    - charts/document-service/
    - .github/workflows/ci-cd.yml
  validate:
    - terraform validate ✓
    - tfsec scan ✓
    - cost estimate ✓
  execute:
    - terraform apply → infrastructure provisioned
    - helm install → service deployed
    - smoke tests → passing
  verify:
    - health check: 200 OK
    - error rate: 0%
    - latency: p95 = 120ms (within 500ms SLO)

learn:
  record:
    decision: "ECS Fargate for Java API, small team, greenfield"
    outcome: SUCCESS
    actual_cost: £235/month
    issues: none
    note: "Good baseline for similar future services"
```

---

## Reasoning Principles

```yaml
principles:
  1_never_guess: "If information is missing, ask — don't assume"
  2_always_score: "Every decision backed by quantified reasoning"
  3_consider_history: "Check what worked before in similar contexts"
  4_document_everything: "Future self and other agents need the reasoning"
  5_validate_before_execute: "Never apply un-validated artifacts"
  6_learn_from_outcomes: "Every execution updates the knowledge base"
  7_challenge_defaults: "Don't blindly repeat — re-evaluate if context changed"
  8_minimize_blast_radius: "Prefer safe, reversible actions"
  9_human_in_loop: "Escalate when confidence is LOW or impact is HIGH"
  10_continuous_improvement: "Each cycle should be faster and more accurate"
```
