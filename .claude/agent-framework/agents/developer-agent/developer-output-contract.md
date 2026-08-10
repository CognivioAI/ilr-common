# Developer Output Contract

## Purpose

Define exactly what the Developer Agent delivers to the DevOps Agent — the handoff checklist that ensures code is production-ready.

---

## Contract Summary

```yaml
developer_delivers:
  source_code:
    repository: Git (feature branch → PR to main)
    language: Java 21 + Spring Boot 3.x
    build: Maven (mvn clean verify)
    tests: Spock (Groovy) with Testcontainers
    
  artifacts:
    - Dockerfile (multi-stage, non-root, health check)
    - application.yml (with profile support)
    - Database migrations (Flyway, backward-compatible)
    - OpenAPI documentation (auto-generated)
    
  endpoints:
    health: /actuator/health (liveness + readiness probes)
    metrics: /actuator/prometheus (Micrometer + Prometheus)
    info: /actuator/info (version, build info)
    api_docs: /v3/api-docs (OpenAPI spec)
    
  configuration:
    method: Environment variables (never hardcoded)
    profiles: local, dev, staging, production
    
  logging:
    format: JSON structured
    fields: [timestamp, level, service, traceId, correlationId, message]
    framework: SLF4J + Logback
    
  startup:
    max_time: 60 seconds
    graceful_shutdown: 30 seconds (SIGTERM handling)
```

---

## Service Manifest (Per Service)

```yaml
service_manifest:
  name: document-service
  version: 1.2.0
  runtime: Java 21
  framework: Spring Boot 3.2.x
  
  build:
    command: "./mvnw clean verify -B"
    output: target/document-service-1.2.0.jar
    test_command: "./mvnw test -B"
    
  docker:
    file: Dockerfile
    port: 8080
    base_image: eclipse-temurin:21-jre-alpine
    health_check: "wget --spider http://localhost:8080/actuator/health"
    
  endpoints:
    api_base: /api/v1
    health: /actuator/health
    health_liveness: /actuator/health/liveness
    health_readiness: /actuator/health/readiness
    metrics: /actuator/prometheus
    info: /actuator/info
    
  environment_variables:
    required:
      - name: DB_URL
        description: PostgreSQL JDBC connection URL
        example: jdbc:postgresql://host:5432/dbname
      - name: DB_USERNAME
        description: Database username
      - name: DB_PASSWORD
        description: Database password
      - name: AWS_REGION
        description: AWS region for SDK calls
        example: eu-west-2
      - name: SPRING_PROFILES_ACTIVE
        description: Active Spring profile
        values: [dev, staging, production]
    optional:
      - name: SQS_QUEUE_URL
        description: Document processing queue URL
        default: auto-discovered
      - name: S3_BUCKET
        description: Document storage bucket
        default: ilr-documents-{environment}
      - name: LOG_LEVEL
        description: Root log level
        default: INFO
        
  dependencies:
    infrastructure:
      - type: PostgreSQL
        version: "16"
        usage: Primary data store
        connection_pool: 10 per pod
      - type: S3
        usage: Document file storage
        access: read-write
      - type: SQS
        usage: Async processing queue
        access: send + receive
      - type: Bedrock
        usage: AI classification (Claude 3 Sonnet)
        access: invoke-model
        
  startup:
    estimated_time: 25 seconds
    readiness_delay: 10 seconds
    dependencies_checked:
      - PostgreSQL connection
      - S3 bucket accessible
      - SQS queue accessible
      
  shutdown:
    grace_period: 30 seconds
    behavior: 
      - Stop accepting new requests
      - Complete in-flight requests
      - Close database connections
      - Flush pending logs
      
  resource_hints:
    cpu_request: 500m
    memory_request: 512Mi
    cpu_limit: "2"
    memory_limit: 2Gi
    jvm_options: "-XX:MaxRAMPercentage=75.0"
```

---

## Quality Guarantees

```yaml
quality:
  tests:
    framework: Spock (Groovy BDD)
    unit_coverage: "> 80% (new code)"
    integration_tests: "Every API endpoint + repository"
    all_passing: true (CI verified)
    
  security:
    sast: "SonarQube scan clean (no critical/high)"
    dependencies: "No known critical vulnerabilities"
    secrets: "Zero hardcoded secrets"
    input_validation: "All endpoints validate input"
    auth: "All API endpoints require authentication"
    
  code_quality:
    self_reviewed: true
    static_analysis: "CheckStyle + SpotBugs clean"
    architecture_fitness: "Layer rules enforced (no shortcuts)"
    technical_debt: "Documented in coding-memory.md"
```

---

## PR Deliverable

```yaml
pull_request:
  branch: "feature/{ticket}-{description}"
  
  commits:
    format: "Conventional commits"
    atomic: "One logical change per commit"
    examples:
      - "feat(document): add document entity and migration"
      - "feat(document): add document upload API"
      - "test(document): add upload integration tests"
      
  description:
    template: |
      ## Summary
      Brief description of what was implemented and why.
      
      ## Changes
      - List of significant changes
      - New endpoints added
      - Database migrations included
      
      ## Architecture Decisions
      - Any notable implementation decisions
      - Reference to architecture design: [link]
      
      ## Testing
      - [ ] Unit tests (Spock specs)
      - [ ] Integration tests (Testcontainers)
      - [ ] API tests (REST Assured)
      - [ ] Manual testing in local environment
      
      ## DevOps Notes
      - New environment variables: [list]
      - Database migration included: V{n}__description.sql
      - New infrastructure dependencies: [none / list]
      
      ## Risks
      - Low / Medium / High
      - [Describe any risks or concerns]
      
  labels:
    size: [XS, S, M, L, XL]
    type: [feature, bugfix, refactor, test]
    area: [backend, frontend, infrastructure]
```

---

## DevOps Handoff Checklist

Before marking a PR ready for DevOps:

```yaml
handoff_checklist:
  ✓ Build passes: "mvn clean verify — green"
  ✓ All tests pass: "Unit + Integration + API"
  ✓ Dockerfile present and builds: "docker build succeeds"
  ✓ Health endpoint works: "/actuator/health returns UP"
  ✓ Metrics exposed: "/actuator/prometheus returns metrics"
  ✓ Logging structured: "JSON format with traceId"
  ✓ Config via env vars: "No hardcoded values for env-specific config"
  ✓ Migration backward-compatible: "Old code still works with new schema"
  ✓ Environment variables documented: "In service manifest"
  ✓ Graceful shutdown: "SIGTERM handled, in-flight requests complete"
  ✓ No secrets in code: "All sensitive values from Secrets Manager"
  ✓ Security scan clean: "No critical/high findings"
  ✓ PR description complete: "What, why, testing, risks documented"
```
