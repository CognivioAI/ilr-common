# Codebase Analysis

## Purpose

Deep analysis of an existing codebase — architecture discovery, dependency graphing, change impact prediction, and health assessment. Goes beyond repository-intelligence (structure) into understanding how the system actually works.

---

## Analysis Capabilities

```
Codebase
    │
    ├── Architecture Discovery (what patterns are used)
    ├── Dependency Graph (what depends on what)
    ├── Change Impact Prediction (what breaks if I touch X)
    ├── Health Assessment (where is the risk/debt)
    └── Convention Extraction (what style to follow)
```

---

## Architecture Discovery

### Automatic Detection

```yaml
architecture_detection:
  framework:
    scan: [pom.xml, build.gradle, package.json]
    detect:
      spring_boot: "spring-boot-starter-* in dependencies"
      spring_security: "spring-boot-starter-security"
      spring_data: "spring-boot-starter-data-jpa"
      spring_cloud_aws: "spring-cloud-aws-*"
      
  pattern:
    scan: "Package structure and class annotations"
    detect:
      layered: "controller/ service/ repository/ packages"
      hexagonal: "port/ adapter/ domain/ application/ packages"
      ddd: "domain/ containing entities + value objects + aggregates"
      event_driven: "event/ classes, @EventListener, SQS listeners"
      cqrs: "command/ query/ separation in service layer"
    
  output:
    architecture:
      style: layered
      framework: Spring Boot 3.2
      database: PostgreSQL (via Spring Data JPA)
      messaging: SQS (via Spring Cloud AWS)
      storage: S3 (via AWS SDK)
      testing: Spock + Testcontainers
      build: Maven
      java_version: 21
```

---

## Dependency Graph

### Internal Dependencies

```yaml
dependency_graph:
  document-service:
    controller:
      DocumentController:
        depends_on: [DocumentService]
        called_by: [HTTP clients]
        
    service:
      DocumentService:
        depends_on: [DocumentRepository, DocumentMapper, DocumentEventPublisher, S3StorageService]
        called_by: [DocumentController]
        
      DocumentEventPublisher:
        depends_on: [SqsTemplate]
        called_by: [DocumentService]
    
    repository:
      DocumentRepository:
        depends_on: [JPA/PostgreSQL]
        called_by: [DocumentService]
    
    infrastructure:
      S3StorageService:
        depends_on: [S3Client]
        called_by: [DocumentService]
      
      ClassificationEventListener:
        depends_on: [DocumentService, SQS]
        called_by: [SQS message arrival]

  visualization:
    DocumentController
        │
        ▼
    DocumentService
        │
        ├──▶ DocumentRepository ──▶ PostgreSQL
        ├──▶ S3StorageService ──▶ S3
        ├──▶ DocumentEventPublisher ──▶ SQS
        └──▶ DocumentMapper
```

### Cross-Service Dependencies

```yaml
cross_service:
  document-service:
    produces_events:
      - DocumentUploaded → consumed by [ai-worker, notification-service]
      - DocumentDeleted → consumed by [notification-service]
    
    consumes_events:
      - ClassificationCompleted → from [ai-worker]
    
    exposes_api:
      - GET /api/v1/documents → consumed by [web-frontend, admin-dashboard]
      - POST /api/v1/documents → consumed by [web-frontend]
    
    depends_on_infrastructure:
      - PostgreSQL (data store)
      - S3 (file storage)
      - SQS (messaging)
```

---

## Change Impact Prediction

### Impact Analysis Engine

```yaml
impact_analysis:
  input:
    change: "Add document expiry rules"
    scope: "Documents expire after 90 days, status changes to EXPIRED"
  
  analysis:
    1_direct_changes:
      create:
        - DocumentExpiryService.java (new scheduled service)
        - DocumentExpirySpec.groovy (unit test)
        - V8__add_expiry_columns.sql (migration)
      modify:
        - Document.java (add expiresAt field)
        - DocumentStatus.java (add EXPIRED enum value)
        - DocumentResponse.java (include expiresAt in response)
        - DocumentMapper.java (map new field)
      
    2_ripple_effects:
      same_service:
        - DocumentService.java (set expiresAt on upload)
        - DocumentRepository.java (add findExpiredDocuments query)
        - DocumentControllerIT.groovy (verify expiresAt in response)
      
      other_services:
        - web-frontend (display expiry date, handle EXPIRED status)
        - notification-service (send expiry warning emails?)
        - ai-worker (skip expired documents in classification queue)
      
    3_api_impact:
      breaking: false (additive field in response)
      new_endpoint: "GET /api/v1/documents/expired (optional, for admin)"
      
    4_database_impact:
      migration: "Add nullable expiresAt column + index"
      existing_data: "Existing documents get no expiry (null = never expires)"
      backward_compatible: true
      
    5_event_impact:
      new_event: "DocumentExpired (published by scheduler)"
      consumers_to_update: [notification-service]
    
  summary:
    files_affected: 12 (create: 3, modify: 9)
    services_affected: 3 (document-service, web-frontend, notification-service)
    tests_affected: 5 (create: 2, modify: 3)
    risk: MEDIUM
    reason: "Scheduler adds complexity, cross-service event coordination needed"
    recommendation: "Implement in document-service first, then downstream services"
```

---

## Health Assessment

### Code Health Metrics

```yaml
health_assessment:
  scan_for:
    complexity:
      - Methods with cyclomatic complexity > 10
      - Classes with > 300 lines
      - Packages with > 20 classes
    
    coupling:
      - Classes with > 7 dependencies (constructor params)
      - Circular dependencies between packages
      - Service-to-service tight coupling
    
    test_health:
      - Untested public methods
      - Test-to-code ratio (target: > 1:1 for services)
      - Flaky tests (non-deterministic)
      - Slow tests (> 5s per test)
    
    debt_indicators:
      - TODO/FIXME comments (count and age)
      - Suppressed warnings (@SuppressWarnings)
      - Empty catch blocks
      - Deprecated API usage
      - Outdated dependencies (major version behind)
    
    security_indicators:
      - String concatenation in queries
      - Unchecked user input paths
      - Hardcoded configuration values
      - Missing input validation annotations

  output:
    service: document-service
    health_score: 7.5/10
    
    hotspots:
      - DocumentService.java: "High complexity (CC=12 in upload method)"
      - V3 migration: "Large migration without rollback plan"
      
    strengths:
      - Good test coverage (85%)
      - Consistent patterns across service
      - Clean dependency structure
      
    recommendations:
      - Extract upload validation to DocumentValidator
      - Add @Scheduled cleanup for orphaned S3 objects
      - Update spring-boot from 3.2.0 to 3.2.3 (security patch)
```

---

## Convention Extraction

### Auto-Learn Conventions

```yaml
convention_extraction:
  process:
    1. Scan all source files
    2. Identify recurring patterns
    3. Extract as rules
    4. Follow in new code
  
  extracted:
    naming:
      services: "{Entity}Service — found in 5/5 services"
      controllers: "{Entity}Controller — found in 5/5 controllers"
      dtos: "{Action}{Entity}Request/Response — found in 8/8 DTOs"
      tests: "{Class}Spec.groovy / {Class}IT.groovy — found in all tests"
    
    patterns:
      error_handling: "DomainException hierarchy (found in 100% of services)"
      validation: "Bean Validation on DTOs (found in 100% of endpoints)"
      mapping: "MapStruct interfaces (found in 4/5 services)"
      events: "Dedicated EventPublisher class (found in 3/3 event producers)"
    
    structure:
      layers: "controller → service → repository (enforced in all services)"
      packages: "com.ilr.{service}.{layer} (consistent)"
      config: "application.yml with profiles (all services)"
    
    testing:
      framework: "Spock (100% of tests)"
      style: "given/when/then BDD (consistent)"
      integration: "Testcontainers PostgreSQL (all repository tests)"
      api: "REST Assured with Spock (all API tests)"

  confidence: HIGH (patterns consistent across codebase)
```

---

## Analysis Triggers

```yaml
triggers:
  before_any_implementation:
    - Run architecture discovery (if first time)
    - Run dependency graph for affected area
    - Run change impact prediction for the specific task
  
  on_new_service:
    - Full architecture discovery
    - Full convention extraction
    - Dependency mapping with other services
  
  periodic:
    - Health assessment (weekly/sprint)
    - Debt tracking update
    - Dependency version check
```
