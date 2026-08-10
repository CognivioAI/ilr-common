# Agent Contracts

## Purpose

Define the formal communication interfaces between the DevOps Agent and all other agents in the AI-DLC system. Each contract specifies inputs, outputs, expectations, and feedback mechanisms.

---

## Contract Architecture

```
┌──────────────┐         ┌──────────────┐         ┌──────────────┐
│   Product    │         │ Architecture │         │   DevOps     │
│    Agent     │────────▶│    Agent     │────────▶│    Agent     │
└──────────────┘         └──────────────┘         └──────┬───────┘
                                ▲                        │
                                │   Feedback             │
                                └────────────────────────┤
                                                         │
                              ┌───────────────────────────┤
                              │                           │
                              ▼                           ▼
                    ┌──────────────┐           ┌──────────────┐
                    │  Development │           │  Operations  │
                    │    Agents    │           │  (Runtime)   │
                    └──────────────┘           └──────────────┘
```

---

## Contract 1: Architecture Agent → DevOps Agent

### Input Contract (What DevOps Receives)

```yaml
architecture_to_devops:
  contract_version: "1.0"
  
  required_fields:
    project:
      name: string
      version: string
    
    services:
      - name: string
        type: api | worker | frontend | batch | event-driven
        runtime: java-21 | node-20 | python-3.12
        framework: spring-boot | react | fastapi
        port: number
        dependencies:
          database: {type: string, size: string} | null
          storage: {type: string, access: string} | null
          queue: {type: string, pattern: string} | null
          cache: {type: string} | null
          ai: {type: string, provider: string, model: string} | null
    
    integrations:
      - from: string (service name)
        to: string (service name)
        via: rest | sqs | sns | eventbridge | grpc
        pattern: sync | async
    
    nfrs:
      availability: string (e.g., "99.9%")
      latency_p95: string (e.g., "500ms")
      throughput: string (e.g., "100 req/s")
      data_classification: public | internal | confidential | restricted
      compliance: [string] (e.g., ["gdpr", "uk-gdpr"])
    
    deployment_target:
      cloud: aws
      region: string
  
  optional_fields:
    architecture_style: microservices | monolith | serverless | event-driven
    evolution_phase: mvp | growth | enterprise
    cost_budget: string (monthly budget)
    team_size: number
    
  validation:
    - All services must have name, type, and runtime
    - At least one NFR must be specified
    - Deployment target must include cloud and region
    
  if_incomplete:
    action: "Request missing fields from Architecture Agent"
    message: "Cannot proceed — missing: {list of missing fields}"
```

### Output Contract (What DevOps Returns to Architecture)

```yaml
devops_feedback_to_architecture:
  contract_version: "1.0"
  
  cost_feedback:
    estimated_monthly_cost: number
    budget_status: within_budget | over_budget | significantly_over
    cost_breakdown:
      - component: string
        monthly: number
    alternative_proposals:
      - description: string
        savings: number
        trade_offs: [string]
  
  feasibility_feedback:
    feasible: boolean
    concerns:
      - category: cost | complexity | security | team_capacity
        description: string
        recommendation: string
    
  operational_feedback:
    complexity_score: number (1-10)
    team_readiness: ready | needs_training | not_feasible
    recommended_changes:
      - original: string
        recommended: string
        reason: string
```

---

## Contract 2: DevOps Agent → Development Agents

### Output Contract (What DevOps Provides to Developers)

```yaml
devops_to_development:
  contract_version: "1.0"
  
  service_requirements:
    name: string
    runtime: string
    port: number
    
    mandatory_endpoints:
      health: "/actuator/health"
      health_liveness: "/actuator/health/liveness"
      health_readiness: "/actuator/health/readiness"
      metrics: "/actuator/prometheus"
    
    environment_variables:
      provided_by_devops:
        - name: DB_URL
          description: "Database connection string"
          source: "Kubernetes Secret (from Secrets Manager)"
        - name: AWS_REGION
          description: "AWS region"
          source: "ConfigMap"
        - name: SQS_QUEUE_URL
          description: "Processing queue URL"
          source: "ConfigMap"
        - name: SPRING_PROFILES_ACTIVE
          description: "Active Spring profile"
          source: "Helm values"
      
      developer_must_use:
        - "Read all config from environment variables (not hardcoded)"
        - "Use ${VAR_NAME} in application.yml"
    
    logging_requirements:
      format: JSON
      required_fields: [timestamp, level, service, traceId, message]
      optional_fields: [correlationId, userId, context]
      rules:
        - "Never log PII (user data, email, phone)"
        - "Never log secrets"
        - "Use appropriate log levels"
    
    startup_requirements:
      max_startup_time: "60 seconds"
      graceful_shutdown: "30 seconds"
      signal_handling: "Handle SIGTERM for graceful shutdown"
    
    dependency_patterns:
      circuit_breaker: "Required for all external service calls"
      retry: "Exponential backoff for transient failures"
      timeout: "All outbound calls must have timeout configured"
  
  infrastructure_provided:
    database:
      endpoint: "Provided via DB_URL env var"
      credentials: "Provided via DB_USERNAME, DB_PASSWORD env vars"
      connection_pool: "Size your pool based on: max_replicas × connections_per_pod"
    
    queue:
      url: "Provided via SQS_QUEUE_URL env var"
      dlq: "Configured — messages retry 3 times then go to DLQ"
    
    storage:
      bucket: "Provided via S3_BUCKET env var"
      access: "IAM role attached to pod (no credentials needed)"
    
    ai:
      provider: "Bedrock (via IAM role — no API key needed)"
      model: "Set via AI_MODEL env var"
      rate_limit: "Managed by application (see limits in config)"
```

### Input Contract (What DevOps Expects from Developers)

```yaml
development_to_devops:
  contract_version: "1.0"
  
  deliverables:
    dockerfile:
      required: true
      standards: "Multi-stage, non-root, HEALTHCHECK, specific base image tag"
    
    application_config:
      format: "application.yml with profile support"
      profiles: [local, dev, staging, production]
      secrets: "Referenced via env vars, never hardcoded"
    
    api_documentation:
      format: "OpenAPI 3.0 (auto-generated from code is fine)"
      location: "/v3/api-docs or /swagger-ui.html"
    
    database_migrations:
      format: "Flyway or Liquibase"
      rules:
        - "Always additive (never drop columns in same release)"
        - "Backward compatible with previous version"
        - "Tested rollback scenario"
    
    test_coverage:
      minimum: "80% line coverage"
      required: "Unit tests + integration tests"
```

---

## Contract 3: DevOps Agent → Operations (Runtime)

### Operations Handoff

```yaml
devops_to_operations:
  contract_version: "1.0"
  
  service_deployed:
    name: string
    version: string
    environment: string
    deployment_time: timestamp
    deployment_strategy: string
    
  monitoring:
    dashboard_url: string
    alert_rules:
      - name: string
        severity: P1 | P2 | P3
        condition: string
        runbook_url: string
    slos:
      availability: string
      latency: string
      error_budget_remaining: string
  
  operational_procedures:
    rollback:
      command: string
      estimated_time: string
      validation: string
    
    scaling:
      auto: "HPA configured (min: X, max: Y)"
      manual: "helm upgrade with --set replicaCount=Z"
    
    restart:
      command: "kubectl rollout restart deployment/{name}"
      impact: "Zero-downtime (rolling restart)"
    
    logs:
      location: string (CloudWatch log group or Grafana link)
      query_examples: [string]
  
  on_call_info:
    team: string
    escalation: string
    runbook_location: string
```

---

## Contract 4: DevOps Agent ↔ DevOps Agent (Self-Contract)

### Inter-Module Communication

```yaml
internal_contracts:
  reasoning_engine_to_decision_framework:
    input: {context, constraints, options}
    output: {scored_options, recommendation, confidence}
  
  decision_framework_to_architecture_translator:
    input: {architecture_output, decisions_made}
    output: {terraform_modules, helm_charts, pipelines}
  
  architecture_translator_to_deployment_validator:
    input: {generated_artifacts}
    output: {validation_results, pass_or_block}
  
  deployment_validator_to_autonomy_policy:
    input: {action_to_execute, environment}
    output: {can_execute_autonomously: boolean, approval_needed: boolean}
  
  execution_to_decision_memory:
    input: {decision, outcome, metrics}
    output: {stored, patterns_updated}
```

---

## Contract Versioning

```yaml
versioning:
  strategy: semantic versioning (major.minor)
  
  rules:
    - Adding optional fields: minor version bump
    - Adding required fields: major version bump (breaking)
    - Removing fields: major version bump (breaking)
    - Changing field types: major version bump (breaking)
  
  backward_compatibility:
    - New agent version must accept old contract format
    - Grace period: 2 sprints to upgrade producers
    - Document migration path for breaking changes
  
  current_versions:
    architecture_to_devops: "1.0"
    devops_to_development: "1.0"
    devops_to_operations: "1.0"
    devops_feedback_to_architecture: "1.0"
```

---

## Contract Validation

```yaml
validation:
  on_receive:
    - Validate all required fields present
    - Validate field types correct
    - Validate enum values valid
    - If invalid: reject with specific error message
  
  on_send:
    - Validate output meets contract schema
    - Validate no empty required fields
    - Validate references resolve (e.g., service names exist)
  
  monitoring:
    - Track contract violations (who sends bad data)
    - Alert on repeated violations
    - Review contracts quarterly (are they still correct?)
```

---

## Error Handling

```yaml
error_handling:
  incomplete_input:
    action: "Request missing fields"
    message: "Contract violation: missing required field '{field}'"
    block: true (cannot proceed without required fields)
  
  invalid_input:
    action: "Reject with explanation"
    message: "Contract violation: field '{field}' has invalid value '{value}'"
    suggest: "Expected: {valid_values}"
  
  conflict:
    condition: "Architecture says X but DevOps constraints say Y"
    action: "Report conflict to both agents"
    message: "Architecture requests {X} but DevOps constraint {Y} makes this infeasible"
    resolution: "Architecture Agent must provide alternative or override constraint"
```
