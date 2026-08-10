# Developer Quality Gate

## Purpose

Score every implementation before handoff to DevOps — ensuring code meets quality, security, and completeness standards.

---

## Quality Gate Architecture

```
Developer Agent Completes Implementation
    │
    ▼
┌─────────────────────────────────────┐
│       DEVELOPER QUALITY GATE         │
│                                     │
│  Requirements Coverage     ─── /10  │
│  Code Quality              ─── /10  │
│  Testing                   ─── /10  │
│  Security                  ─── /10  │
│  Performance               ─── /10  │
│  Maintainability           ─── /10  │
│                                     │
│  OVERALL: X/10                      │
│                                     │
│  ≥ 8.0  → ✅ Ready for DevOps      │
│  6.0-7.9 → ⚠️ Fix issues first     │
│  < 6.0  → ❌ Rework required       │
└─────────────────────────────────────┘
```

---

## Scoring Categories

### 1. Requirements Coverage (Weight: 20%)

```yaml
requirements:
  checks:
    acceptance_criteria:
      - All acceptance criteria from user story implemented
      - Each criterion has at least one test proving it works
      - Edge cases from requirements addressed
    
    api_contract:
      - All endpoints from architecture design implemented
      - Request/response match OpenAPI spec
      - Error codes match error contract
    
    data_model:
      - All entities from architecture design created
      - Relationships match design
      - Migrations cover full schema
  
  scoring:
    10: "All requirements covered, all tested, edge cases handled"
    8: "All requirements covered, most edge cases"
    6: "Core requirements covered, gaps in edge cases"
    4: "Some requirements missing implementation"
    2: "Significant gaps in requirements coverage"
```

### 2. Code Quality (Weight: 20%)

```yaml
code_quality:
  checks:
    solid_principles:
      - Single Responsibility (classes focused)
      - Open/Closed (extensible without modification)
      - Dependency Inversion (abstractions over concretions)
    
    clean_code:
      - Method length < 20 lines (average)
      - Class length < 300 lines
      - Meaningful names (no abbreviations)
      - No dead code or commented-out code
      - No magic numbers
    
    architecture_fitness:
      - Correct layer usage (no skipping layers)
      - Dependencies flow in correct direction
      - No circular dependencies
      - Package structure matches standards
    
    static_analysis:
      - Checkstyle: zero violations
      - SpotBugs: zero bugs
      - SonarQube: quality gate passes
  
  scoring:
    10: "Exemplary code quality, zero static analysis issues"
    8: "Clean code, minor improvements possible"
    6: "Acceptable, some code smells present"
    4: "Multiple code smells, needs refactoring"
    2: "Poor quality, significant rework needed"
```

### 3. Testing (Weight: 20%)

```yaml
testing:
  checks:
    coverage:
      - New code coverage > 80%
      - Service layer coverage > 90%
      - All API endpoints have integration tests
    
    quality:
      - Tests are BDD style (given/when/then)
      - Tests are independent (no shared state)
      - Tests are deterministic (no flakiness)
      - Test names describe behavior clearly
    
    completeness:
      - Happy path tested
      - Error paths tested (not found, validation, auth)
      - Edge cases tested (empty, boundary, concurrent)
      - Integration flow tested end-to-end
    
    strategy:
      - Testing pyramid followed (many unit, some integration, few E2E)
      - Appropriate level for each test (don't over-integrate)
  
  scoring:
    10: "Comprehensive testing, all paths covered, high quality specs"
    8: "Good coverage, most paths tested, clean specs"
    6: "Adequate coverage, some gaps in edge cases"
    4: "Basic happy path only, missing error/edge tests"
    2: "Minimal or no tests"
```

### 4. Security (Weight: 15%)

```yaml
security:
  checks:
    input_validation:
      - All endpoints validate input
      - SQL injection impossible (parameterized queries)
      - No user input in log messages without sanitization
    
    authentication:
      - All API endpoints require authentication
      - JWT validation configured correctly
    
    authorization:
      - Resource ownership checked
      - Role-based access enforced
      - No privilege escalation paths
    
    data_protection:
      - No PII in logs
      - No secrets in code
      - Sensitive fields excluded from responses
      - Encryption for sensitive data at rest
    
    dependencies:
      - No known critical vulnerabilities
      - Dependencies from trusted sources
  
  scoring:
    10: "Zero security issues, proactive hardening"
    8: "No critical/high issues, good practices"
    6: "Minor issues, no critical risks"
    4: "Some security concerns need addressing"
    1: "Critical vulnerability — BLOCKS delivery"
```

### 5. Performance (Weight: 10%)

```yaml
performance:
  checks:
    database:
      - No N+1 queries
      - Queries paginated for list operations
      - Appropriate indexes exist
      - Connection pool sized correctly
    
    api:
      - Response time reasonable (< 200ms for simple ops)
      - No unbounded data loading
      - Async for long operations (> 5s)
    
    resources:
      - No memory leaks (streams closed, connections released)
      - Appropriate caching considered
      - External calls have timeouts
  
  scoring:
    10: "Optimized, no N+1, proper caching, all async where needed"
    8: "Good performance, no obvious issues"
    6: "Acceptable, minor optimization opportunities"
    4: "Performance concerns (N+1, missing pagination)"
    2: "Significant performance issues"
```

### 6. Maintainability (Weight: 15%)

```yaml
maintainability:
  checks:
    readability:
      - Code readable without comments
      - Complex logic explained
      - Consistent style with codebase
    
    documentation:
      - Public API has Javadoc
      - OpenAPI annotations on endpoints
      - README updated if needed
      - ADR written for significant decisions
    
    devops_contract:
      - Health endpoint works
      - Metrics exposed
      - Logging structured
      - Config via environment variables
      - Graceful shutdown
    
    extensibility:
      - Can add new features without rewriting
      - Appropriate abstractions (not over/under engineered)
      - Technical debt documented
  
  scoring:
    10: "Excellent maintainability, well-documented, DevOps-ready"
    8: "Good maintainability, minor documentation gaps"
    6: "Acceptable, some areas hard to understand"
    4: "Difficult to maintain, poor documentation"
    2: "Unmaintainable without significant rework"
```

---

## Gate Output

```
╔══════════════════════════════════════════════════════════════╗
║       DEVELOPER QUALITY GATE                                ║
║       Feature: Document Upload with AI Classification       ║
║       Service: document-service                             ║
║       Date: 2026-07-08                                      ║
╠══════════════════════════════════════════════════════════════╣
║                                                             ║
║  Category              Score   Weight   Weighted            ║
║  ─────────────────────────────────────────────              ║
║  Requirements           9.0     20%      1.80               ║
║  Code Quality           8.5     20%      1.70               ║
║  Testing                9.0     20%      1.80               ║
║  Security               9.5     15%      1.43               ║
║  Performance            8.0     10%      0.80               ║
║  Maintainability        8.5     15%      1.28               ║
║                                                             ║
║  ─────────────────────────────────────────────              ║
║  OVERALL SCORE:         8.8/10                              ║
║  VERDICT:               ✅ READY FOR DEVOPS HANDOFF         ║
║                                                             ║
║  Strengths:                                                 ║
║    - Comprehensive Spock test coverage (9.0)                ║
║    - Strong security posture (9.5)                          ║
║    - All requirements satisfied with edge cases             ║
║                                                             ║
║  Minor Notes:                                               ║
║    - Consider caching for repeated classification lookups   ║
║    - Document AI fallback behavior in runbook               ║
║                                                             ║
╚══════════════════════════════════════════════════════════════╝
```

---

## Gate Rules

```yaml
rules:
  pass (≥ 8.0):
    action: "Proceed to DevOps handoff"
  
  conditional (6.0-7.9):
    action: "Fix identified issues before handoff"
    deadline: "Same day"
  
  blocked (< 6.0):
    action: "Significant rework required"
    
  auto_block:
    - Any security score = 1 (critical vulnerability)
    - Testing score < 4 (insufficient tests)
    - Requirements score < 4 (incomplete implementation)
    - No health endpoint (DevOps contract broken)
```
