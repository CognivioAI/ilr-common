# Repository Intelligence

## Purpose

The Developer Agent must understand an existing codebase before modifying it. Never generate code in isolation — always analyze the repository first to understand architecture, patterns, dependencies, and change impact.

---

## Discovery Process

```
New Task Received
    │
    ▼
1. DISCOVER — Map repository structure
    │
    ▼
2. ANALYZE — Understand existing architecture
    │
    ▼
3. DETECT — Find patterns and conventions in use
    │
    ▼
4. MAP — Identify dependencies and ownership
    │
    ▼
5. ASSESS — Determine change impact
    │
    ▼
6. IMPLEMENT — Write code that fits the existing system
```

---

## Repository Discovery

### Structure Analysis

```yaml
discovery:
  input: repository root
  
  analyze:
    project_type:
      - Build tool: Maven (pom.xml) / Gradle (build.gradle)
      - Language version: Java 21 (from pom.xml)
      - Framework: Spring Boot (from dependencies)
      - Module structure: single-module / multi-module
    
    service_map:
      - services/document-service/
      - services/user-service/
      - services/notification-service/
      - shared/common-lib/
    
    package_structure:
      - Base package: com.ilr.document
      - Layers: controller, service, repository, domain, dto, mapper, exception, config
      - Additional: infrastructure/ (S3, SQS adapters)
    
    test_structure:
      - Location: src/test/groovy/
      - Framework: Spock (detected from dependencies)
      - Pattern: {Class}Spec.groovy, {Class}IT.groovy
      - Fixtures: fixture/ package with builders
    
    infrastructure:
      - Docker: Dockerfile present
      - CI: .github/workflows/ (GitHub Actions)
      - IaC: infrastructure/terraform/
      - Config: application.yml with profiles
```

---

## Architecture Analysis

### Detect Existing Patterns

```yaml
pattern_detection:
  architectural_style:
    observe:
      - Package naming → Layered architecture
      - Interface usage → Port/adapter (if infrastructure/ package exists)
      - Event classes → Event-driven patterns
      - Saga classes → Distributed transaction handling
    
  coding_patterns:
    observe:
      - How DTOs are structured (records vs classes)
      - How exceptions are handled (hierarchy vs generic)
      - How mapping is done (MapStruct vs manual)
      - How validation works (Bean Validation vs custom)
      - How logging is structured (SLF4J pattern)
      - How tests are written (Spock style, fixture usage)
    
  infrastructure_patterns:
    observe:
      - How AWS services are integrated (SDK wrappers)
      - How configuration is managed (profiles, env vars)
      - How events are published (direct vs abstracted)

  output:
    detected_patterns:
      dto_style: "Java records"
      mapping: "MapStruct interfaces"
      exceptions: "DomainException hierarchy + @RestControllerAdvice"
      validation: "Bean Validation on DTOs"
      testing: "Spock BDD with Testcontainers"
      event_publishing: "Dedicated EventPublisher classes"
      external_services: "Infrastructure package with adapter pattern"
```

---

## Dependency Mapping

### Internal Dependencies

```yaml
dependency_map:
  document-service:
    depends_on:
      internal:
        - common-lib (shared DTOs, utilities)
      external:
        - PostgreSQL (primary store)
        - S3 (file storage)
        - SQS (async processing)
        - Bedrock (AI classification)
    
    depended_on_by:
      - notification-service (listens to DocumentUploaded events)
      - user-service (references document ownership)
    
    api_consumers:
      - web-frontend (React app)
      - admin-dashboard (internal tool)
```

### Change Ripple Analysis

```yaml
ripple_analysis:
  if_changing: "DocumentResponse DTO"
  impacts:
    direct:
      - DocumentController.java (returns this type)
      - DocumentMapper.java (produces this type)
      - DocumentService.java (returns this type)
    indirect:
      - web-frontend (consumes this API response)
      - admin-dashboard (consumes this API response)
    tests:
      - DocumentControllerIT.groovy (asserts on response fields)
      - DocumentServiceSpec.groovy (mocks response)
    
  risk: HIGH (breaking change for API consumers)
  recommendation: "Add field (non-breaking) or version API"
```

---

## Change Impact Analysis

### Before Any Modification

```yaml
impact_analysis:
  input:
    task: "Add document validation rules"
    
  analysis:
    1_affected_files:
      modify:
        - DocumentService.java (add validation call)
        - DocumentController.java (add validation error response)
      create:
        - DocumentValidator.java (new validation service)
        - DocumentValidationException.java (new exception)
        - V5__add_validation_rules_table.sql (if rules stored in DB)
      test:
        - DocumentValidatorSpec.groovy (new)
        - DocumentServiceSpec.groovy (update mocks)
        - DocumentControllerIT.groovy (add validation error scenarios)
    
    2_affected_services:
      - document-service (primary change)
      - notification-service (may need to handle validation failure events)
    
    3_api_impact:
      breaking: false
      new_error_codes: ["VALIDATION_FAILED"]
      new_response_fields: none
    
    4_database_impact:
      migration: "Additive (new table or columns)"
      backward_compatible: true
      rollback: "Drop table/column"
    
    5_risk_assessment:
      risk: MEDIUM
      reason: "New validation may reject previously accepted documents"
      mitigation: "Feature flag for gradual rollout"
```

---

## Codebase Conventions Detection

### Auto-Detect Rules

```yaml
auto_detect:
  from_existing_code:
    naming:
      scan: "All class names in src/main/java"
      detect: "Pattern {Entity}Service, {Entity}Controller, etc."
      follow: "Use same pattern for new classes"
    
    structure:
      scan: "Package hierarchy"
      detect: "controller → service → repository flow"
      follow: "Place new classes in correct layer package"
    
    error_handling:
      scan: "Exception classes and @RestControllerAdvice"
      detect: "DomainException base with typed subclasses"
      follow: "Create new exceptions extending DomainException"
    
    testing:
      scan: "Test files in src/test/groovy"
      detect: "Spock specs with given/when/then, IT suffix for integration"
      follow: "Write new tests matching existing style"
    
    commits:
      scan: "git log --oneline -20"
      detect: "Conventional commit format"
      follow: "Use same type/scope pattern"

  output:
    convention_summary:
      "This repository uses: layered architecture, Java records for DTOs,
       MapStruct for mapping, Spock for testing, conventional commits,
       DomainException hierarchy for errors, Spring profiles for config."
```

---

## Pre-Implementation Checklist

Before writing any code, the Developer Agent must:

```yaml
pre_implementation:
  ✓ Repository structure understood
  ✓ Existing patterns identified
  ✓ Change impact analyzed
  ✓ Affected files listed
  ✓ Affected services identified
  ✓ API impact assessed (breaking or not?)
  ✓ Database impact assessed
  ✓ Risk level determined
  ✓ Test files identified (new and modified)
  ✓ Conventions documented (for consistency)
  
  if_any_missing:
    action: "Analyze repository before proceeding"
    never: "Generate code without understanding context"
```

---

## Integration with Reasoning Engine

```yaml
integration:
  when_called: "Phase 1 (UNDERSTAND) of reasoning engine"
  
  flow:
    1. Task received
    2. Repository intelligence scans codebase
    3. Conventions detected and loaded
    4. Impact analysis completed
    5. Context passed to PLAN phase
    6. Implementation follows detected patterns
  
  output_to_plan_phase:
    - Affected files (create/modify)
    - Patterns to follow
    - Conventions to match
    - Risk assessment
    - Test files needed
```
