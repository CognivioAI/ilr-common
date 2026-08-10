# Coding Memory

## Purpose

Persistent knowledge of past implementations, patterns used, lessons learned, and codebase conventions. Enables consistency and avoids repeating mistakes.

---

## Memory Structure

```
Coding Memory
├── Codebase Conventions (observed patterns)
├── Implementation Log (what was built, how)
├── Lessons Learned (mistakes and fixes)
├── Reusable Patterns (proven solutions)
└── Technical Debt Register (known shortcuts)
```

---

## Codebase Conventions

```yaml
conventions:
  project: ilr-platform
  observed:
    architecture:
      style: "Layered (Controller → Service → Repository)"
      packages: "com.ilr.{service-name}.{layer}"
      
    coding_style:
      dependency_injection: "Constructor injection via @RequiredArgsConstructor"
      dto_style: "Java records for immutable DTOs"
      entity_style: "JPA entities with Lombok @Builder"
      error_handling: "DomainException hierarchy + @RestControllerAdvice"
      logging: "SLF4J @Slf4j, JSON structured"
      null_handling: "Optional for queries, never return null"
      
    testing:
      framework: "Spock (Groovy) with BDD given/when/then"
      unit: "{Class}Spec.groovy — mocked dependencies"
      integration: "{Class}IT.groovy — Testcontainers + REST Assured"
      fixtures: "Builder pattern in {Entity}Fixture.groovy"
      naming: "should_{behavior}_when_{condition}"
      
    api:
      versioning: "/api/v1/{resource}"
      pagination: "Spring Pageable (page, size, sort)"
      error_format: '{"code": "...", "message": "...", "traceId": "..."}'
      validation: "Bean Validation (@Valid on request body)"
      
    infrastructure:
      database: "PostgreSQL via Spring Data JPA"
      migrations: "Flyway (V{n}__{description}.sql)"
      messaging: "SQS via Spring Cloud AWS"
      storage: "S3 via AWS SDK"
      
    delivery:
      commits: "Conventional commits (feat, fix, test, refactor)"
      branches: "feature/{ticket}-{description}"
      pr_template: "Summary + Changes + Testing + Risks"
```

---

## Implementation Log

```yaml
implementations:
  - id: "IMPL-001"
    date: "2026-05-15"
    feature: "Document upload API"
    service: document-service
    scope:
      files_created: 14
      tests_created: 8
      migrations: 2
    patterns_used:
      - "Builder (DTO construction)"
      - "Repository pattern (data access)"
      - "Mapper (entity ↔ DTO with MapStruct)"
      - "Domain exceptions (typed error hierarchy)"
    decisions:
      - "Used record for DTOs (immutable, concise)"
      - "Chose MapStruct over manual mapping (reduces boilerplate)"
      - "S3 upload async via SQS (non-blocking for user)"
    outcome: SUCCESS
    review_feedback: "Clean implementation, good test coverage"
    
  - id: "IMPL-002"
    date: "2026-06-01"
    feature: "AI classification integration"
    service: document-service
    patterns_used:
      - "Event-driven (SQS publish/consume)"
      - "Circuit breaker (Bedrock calls)"
      - "Retry with backoff (transient failures)"
    decisions:
      - "Async processing via SQS (decouple from API response time)"
      - "Circuit breaker with fallback to UNCLASSIFIED status"
      - "Confidence threshold for auto-accept (> 0.8)"
    outcome: SUCCESS
    lesson: "Initial timeout too low (5s) — Bedrock needs 15s for large documents"
```

---

## Lessons Learned

```yaml
lessons:
  - id: "LESSON-001"
    context: "Database connection pool"
    mistake: "Default pool size (10) with 5 service replicas"
    consequence: "Pool exhaustion under load (50 total connections, DB allows 100)"
    fix: "Set pool size = DB max connections / max replicas (100 / 10 = 10 per pod)"
    prevention: "Always calculate pool size based on max scale"
    
  - id: "LESSON-002"
    context: "Flyway migration"
    mistake: "Added NOT NULL column without default value"
    consequence: "Migration failed on existing data"
    fix: "Add column nullable first, backfill, then add NOT NULL"
    prevention: "Always use expand/contract pattern for schema changes"
    
  - id: "LESSON-003"
    context: "SQS visibility timeout"
    mistake: "Default 30s visibility, processing takes 60s for large documents"
    consequence: "Messages reprocessed (duplicate work)"
    fix: "Set visibility timeout to 5x max processing time"
    prevention: "Always calculate visibility timeout from max processing duration"
    
  - id: "LESSON-004"
    context: "N+1 query"
    mistake: "Loaded document list then fetched classifications per document"
    consequence: "100 documents = 101 queries, page load > 5 seconds"
    fix: "@EntityGraph or JOIN FETCH in repository query"
    prevention: "Always check SQL log count in integration tests"
    
  - id: "LESSON-005"
    context: "Test flakiness"
    mistake: "Test depended on system clock (Instant.now())"
    consequence: "Intermittent failures near midnight"
    fix: "Inject Clock, use fixed clock in tests"
    prevention: "Never use Instant.now() directly — always inject Clock"
```

---

## Reusable Patterns

```yaml
patterns:
  - name: "Paginated List Endpoint"
    applicable: "Any GET /resources with pagination"
    implementation:
      controller: "Accept Pageable, return Page<Response>"
      service: "Pass pageable to repository"
      repository: "Return Page<Entity>"
    tested: 5 times, 0 failures
    
  - name: "Event-Driven Async Processing"
    applicable: "Any operation that's too slow for sync response"
    implementation:
      api: "Return 202 Accepted with status PENDING"
      publisher: "Send message to SQS"
      listener: "Process async, update status"
      polling: "Client polls GET /resource/{id} for status"
    tested: 2 times, 0 failures
    
  - name: "External Service Integration"
    applicable: "Any call to external API (Bedrock, S3, third-party)"
    implementation:
      client: "Dedicated client class in infrastructure/ package"
      timeout: "Connection 5s, read configurable per service"
      retry: "3 attempts, exponential backoff (1s, 2s, 4s)"
      circuit_breaker: "Open after 5 failures in 60s, half-open after 30s"
      fallback: "Graceful degradation (default value or queue for retry)"
    tested: 3 times, 1 lesson (timeout too low initially)
```

---

## Technical Debt Register

```yaml
technical_debt:
  - id: "TD-001"
    created: "2026-05-20"
    service: document-service
    description: "S3 upload not chunked for large files"
    impact: MEDIUM
    risk: "OOM for files > 500MB"
    fix: "Use multipart upload with streaming"
    effort: "4 hours"
    trigger: "When file size limit > 100MB is needed"
    
  - id: "TD-002"
    created: "2026-06-05"
    service: document-service
    description: "Classification results not cached"
    impact: LOW
    risk: "Repeated AI calls for same document on re-fetch"
    fix: "Cache classification result after completion"
    effort: "2 hours"
    trigger: "When AI costs become concern"
```

---

## Query Interface

```yaml
queries:
  find_pattern:
    input: "paginated list endpoint"
    result: "Paginated List Endpoint pattern — used 5 times, proven"
    
  find_lesson:
    input: "database migration"
    result: "LESSON-002 — always use expand/contract for schema changes"
    
  find_convention:
    input: "how are tests named?"
    result: "Spock BDD: should_{behavior}_when_{condition} in .groovy files"
    
  check_debt:
    input: "document-service"
    result: "2 items: TD-001 (S3 chunking), TD-002 (classification caching)"
```
