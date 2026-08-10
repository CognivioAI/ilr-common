# Code Generation Engine

## Purpose

Define the discipline of code generation — never generate in isolation, always within context. The agent follows a strict process: understand → analyze → pattern-match → generate → verify.

---

## Generation Discipline

```
❌ WRONG:
  Requirement → Generate random classes

✅ CORRECT:
  Requirement
      │
      ▼
  Read architecture design
      │
      ▼
  Inspect existing codebase
      │
      ▼
  Find matching patterns
      │
      ▼
  Generate consistent code
      │
      ▼
  Write tests
      │
      ▼
  Self-review
```

---

## Generation Rules

### Rule 1: Never Generate Without Context

```yaml
before_generating:
  must_know:
    - What architecture decision was made (service design, API contract)
    - What patterns exist in the codebase (repository-intelligence.md)
    - What conventions the team follows (coding-memory.md)
    - What the DevOps contract requires (health, metrics, logging)
    - What tests are expected (test-strategy.md)
  
  if_context_missing:
    action: "Analyze repository first, then generate"
    never: "Generate and hope it fits"
```

### Rule 2: Match Existing Style

```yaml
style_matching:
  principle: "New code should be indistinguishable from existing code"
  
  how:
    - Use same DTO pattern (records if records exist)
    - Use same exception pattern (hierarchy if hierarchy exists)
    - Use same test pattern (Spock BDD if Spock is used)
    - Use same logging pattern (structured if structured exists)
    - Use same naming pattern (observe and follow)
  
  anti_pattern:
    - Introducing a new framework not in the project
    - Using a different test style than the codebase
    - Changing naming conventions mid-project
    - Adding utility classes that duplicate existing helpers
```

### Rule 3: Generate Complete Units

```yaml
complete_generation:
  principle: "Generate everything needed for a feature to work"
  
  for_new_endpoint:
    generate:
      - Controller method
      - Request DTO (with validation)
      - Response DTO
      - Service method
      - Repository method (if new query)
      - Mapper update
      - Exception (if new error case)
      - Migration (if schema change)
      - Tests (unit + integration)
      - OpenAPI annotation
    
    never:
      - Controller without service
      - Service without tests
      - Entity without migration
      - Endpoint without validation
      - Feature without tests
```

### Rule 4: Generate Tests Alongside Code

```yaml
test_generation:
  timing: "Immediately after (or alongside) production code"
  
  never:
    - "I'll add tests later"
    - "Tests for this are trivial, skip"
    - "Tested manually"
  
  always:
    - Unit spec for service logic
    - Integration test for database operations
    - API test for controller endpoints
    - Edge case tests for validation
```

### Rule 5: Generate Incrementally

```yaml
incremental_generation:
  principle: "Build bottom-up, verify at each layer"
  
  sequence:
    1. Entity + Migration → verify: migration runs
    2. Repository → verify: Testcontainers IT passes
    3. Service → verify: Spock spec passes
    4. Controller → verify: API IT passes
    5. Integration → verify: full flow works
  
  never:
    - Generate all files at once without verifying
    - Generate controller before service exists
    - Generate API without underlying logic
```

---

## Generation Templates

### New Service Method

```yaml
template: new_service_method
trigger: "Architecture defines a new business operation"

generates:
  service_method:
    location: "{service_package}/{Entity}Service.java"
    pattern: |
      @Transactional (if write) / @Transactional(readOnly = true) (if read)
      public {ReturnType} {methodName}({parameters}) {
          log.info("...");
          // validation
          // business logic
          // persistence
          // event publishing (if needed)
          // return mapped response
      }
  
  unit_test:
    location: "test/groovy/{service_package}/{Entity}ServiceSpec.groovy"
    pattern: |
      def "should {behavior} when {condition}"() {
          given: "..."
          when: "..."
          then: "..."
      }
```

### New API Endpoint

```yaml
template: new_api_endpoint
trigger: "Architecture defines a new REST endpoint"

generates:
  - Request DTO (record with validation annotations)
  - Response DTO (record with all response fields)
  - Controller method (with OpenAPI annotations)
  - Service method (business logic)
  - Mapper update (if new DTO)
  - Controller IT (happy path + error paths)
  - Service Spec (business logic tests)
```

### New Event Integration

```yaml
template: new_event
trigger: "Architecture defines event publishing or consumption"

generates_for_publisher:
  - Event record class
  - EventPublisher service
  - Integration in existing service
  - Publisher spec (verify event sent)

generates_for_consumer:
  - Event record class
  - EventListener class (@SqsListener)
  - Processing logic in service
  - Listener IT (with LocalStack)
```

---

## Quality Verification

### After Every Generation

```yaml
verification:
  compile:
    check: "Does the code compile without errors?"
    fix: "If not, fix immediately — never leave broken code"
  
  tests:
    check: "Do all tests pass (new and existing)?"
    fix: "If existing tests break, the generated code has wrong assumptions"
  
  consistency:
    check: "Does generated code match existing patterns?"
    fix: "Adjust to match codebase conventions"
  
  completeness:
    check: "Is the feature fully functional? No dead code?"
    fix: "Remove stubs, complete implementations"
  
  devops_contract:
    check: "Health, metrics, logging, config — all satisfied?"
    fix: "Add missing observability hooks"
```

---

## Anti-Patterns in Generation

| Anti-Pattern | Problem | Correct Approach |
|-------------|---------|-----------------|
| Generate without reading existing code | Inconsistent, duplicates | Repository intelligence first |
| Generate entire service in one shot | Unverifiable, error-prone | Incremental, layer-by-layer |
| Generate code without tests | Untested, unreliable | Tests are part of generation |
| Copy-paste from tutorials | Doesn't match project style | Adapt to existing conventions |
| Generate overly abstract code | Over-engineered, hard to follow | YAGNI — simple until complexity earned |
| Generate TODO/placeholder comments | Incomplete delivery | Implement fully or don't generate |
| Generate without error handling | Fragile in production | Error paths are required |
