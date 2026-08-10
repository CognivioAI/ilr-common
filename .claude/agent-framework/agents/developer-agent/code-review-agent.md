# Code Review Agent

## Purpose

Automated self-review before requesting human review. The Developer Agent reviews its own code against quality, security, performance, and architecture fitness criteria. Issues are fixed before the PR is created.

---

## Review Process

```
Code Implementation Complete
    │
    ▼
Self-Review (automated)
    │
    ├── Design Review
    ├── Security Review
    ├── Performance Review
    ├── Maintainability Review
    ├── Test Review
    ├── DevOps Contract Review
    │
    ▼
Issues Found?
    │
    ├── YES → Fix, then re-review
    └── NO → Proceed to PR
```

---

## Review Checklist

### 1. Design Quality

```yaml
design_review:
  solid_principles:
    single_responsibility:
      check: "Does each class have one reason to change?"
      violations:
        - Controller with business logic
        - Service handling HTTP concerns
        - Repository with domain rules
    
    open_closed:
      check: "Can behavior be extended without modifying existing code?"
      violations:
        - Giant switch/if-else chains
        - Type-checking with instanceof
    
    liskov_substitution:
      check: "Can subtypes replace base types without breaking?"
      violations:
        - Overridden methods that throw UnsupportedOperationException
    
    interface_segregation:
      check: "Are interfaces focused and minimal?"
      violations:
        - Large interfaces forcing empty implementations
    
    dependency_inversion:
      check: "Do high-level modules depend on abstractions?"
      violations:
        - Service directly instantiating implementations
        - Tight coupling to infrastructure classes

  clean_code:
    - Methods < 20 lines (prefer < 10)
    - Classes < 200 lines
    - Max 3 parameters per method
    - No magic numbers (use named constants)
    - No commented-out code
    - Descriptive names (no abbreviations except well-known: id, url, dto)
```

### 2. Security Review

```yaml
security_review:
  input_validation:
    - All user input validated before processing
    - Bean Validation annotations on DTOs (@NotNull, @Size, @Email)
    - File upload: validated type, size, content
    - Path parameters: validated format (UUID, numeric)
  
  injection_prevention:
    - No string concatenation in SQL (use parameterized queries)
    - No user input in log messages without sanitization
    - No user input in file paths
    - No eval/reflection with user input
  
  authentication:
    - All endpoints (except health/info) require authentication
    - JWT validated on every request
    - Token expiry checked
  
  authorization:
    - Endpoints restricted by role
    - Resource ownership checked (user can only access own data)
    - Admin functions separated from user functions
  
  data_protection:
    - No PII in logs
    - No secrets in code or config files
    - Passwords never stored in plain text
    - Sensitive fields excluded from API responses
  
  dependencies:
    - No known vulnerable dependencies
    - Dependencies from trusted sources
    - Versions pinned (not ranges)
```

### 3. Performance Review

```yaml
performance_review:
  database:
    n_plus_one:
      check: "Are collections loaded eagerly where needed?"
      pattern: "Iterating over results and querying for each"
      fix: "Use JOIN FETCH or @EntityGraph"
    
    missing_index:
      check: "Do WHERE/ORDER BY columns have indexes?"
      fix: "Add index in migration script"
    
    unbounded_query:
      check: "Are large result sets paginated?"
      fix: "Use Pageable/LIMIT in all list queries"
  
  memory:
    large_collections:
      check: "Are entire tables loaded into memory?"
      fix: "Use pagination, streaming, or projection"
    
    object_creation:
      check: "Are expensive objects created in loops?"
      fix: "Reuse or move outside loop"
  
  network:
    chatty_api:
      check: "Does one operation require multiple API calls?"
      fix: "Batch or aggregate in single endpoint"
    
    timeout:
      check: "Do all external calls have timeouts?"
      fix: "Set connection + read timeout for all HTTP clients"
  
  caching:
    repeated_computation:
      check: "Is the same expensive computation repeated?"
      fix: "Cache with appropriate TTL"
```

### 4. Maintainability Review

```yaml
maintainability_review:
  readability:
    - Can a new team member understand this code in 5 minutes?
    - Are complex algorithms explained with comments?
    - Is the flow of control obvious?
  
  complexity:
    cyclomatic: "Method complexity < 10 (warn at 7)"
    cognitive: "Can you hold the logic in your head?"
    nesting: "Max 3 levels of nesting"
  
  duplication:
    check: "Are there similar code blocks that should be extracted?"
    threshold: "3 or more similar blocks = extract"
  
  naming:
    - Do names reveal intent?
    - Are abbreviations avoided (except: id, url, dto, dao)?
    - Are booleans named with is/has/can?
    - Are collections named as plurals?
  
  documentation:
    - Public APIs have Javadoc
    - Complex business logic has comments explaining WHY
    - No obvious comments (don't explain WHAT code does if it's clear)
```

### 5. Test Review

```yaml
test_review:
  coverage:
    - All new code has tests
    - Happy path tested
    - Error paths tested
    - Edge cases tested
    - Validation tested
  
  quality:
    - Tests are independent (no shared state)
    - Tests are deterministic (no flakiness)
    - Test names describe behavior
    - Arrange/Act/Assert clearly separated
    - No logic in tests (if/loops)
  
  missing:
    - Missing negative tests (what should fail)
    - Missing boundary tests (limits, empty, null)
    - Missing security tests (auth/authz)
    - Missing concurrency tests (if shared state)
```

### 6. DevOps Contract Review

```yaml
devops_contract_review:
  health:
    check: "Health endpoint exists and reports dependencies"
    endpoint: "/actuator/health"
    includes: [database, s3, sqs]
  
  metrics:
    check: "Prometheus metrics exposed"
    endpoint: "/actuator/prometheus"
    custom: "Business metrics added for key operations"
  
  logging:
    check: "JSON structured logging with required fields"
    fields: [timestamp, level, service, traceId, message]
    no_pii: true
  
  configuration:
    check: "All config via environment variables"
    no_hardcoded: [URLs, credentials, ports, feature flags]
  
  shutdown:
    check: "Graceful shutdown configured"
    timeout: "30 seconds"
  
  startup:
    check: "Service starts within 60 seconds"
    health_ready: "Within 10 seconds of start"
```

---

## Review Output

```yaml
review_result:
  status: PASS | ISSUES_FOUND
  
  issues:
    - severity: HIGH
      category: security
      location: "DocumentController.java:42"
      description: "User input used directly in log message"
      fix: "Sanitize or use structured logging parameters"
    
    - severity: MEDIUM
      category: performance
      description: "N+1 query in document listing"
      fix: "Add @EntityGraph or JOIN FETCH"
    
    - severity: LOW
      category: maintainability
      description: "Method exceeds 20 lines"
      fix: "Extract helper method"
  
  action:
    high_issues: "Fix before PR (blocking)"
    medium_issues: "Fix before PR (recommended)"
    low_issues: "Document in PR, fix later if time permits"
```

---

## When to Skip Self-Review

Never. Even for:
- "Simple" one-line changes (can still break things)
- "Urgent" hotfixes (especially need review — pressure causes mistakes)
- "Copy-paste" from existing code (existing code may have issues)

The review is fast (mental checklist). The cost of skipping is always higher than the cost of reviewing.
