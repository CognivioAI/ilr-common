# Refactoring Engine

## Purpose

Detect code smells, identify improvement opportunities, and apply safe refactoring patterns. The agent should proactively suggest improvements — not wait for technical debt to accumulate.

---

## Smell Detection

### Code Smells to Flag

| Smell | Detection Signal | Severity |
|-------|-----------------|----------|
| God Class | > 300 lines, 10+ dependencies | HIGH |
| Long Method | > 30 lines | MEDIUM |
| Feature Envy | Method uses another class's data more than its own | MEDIUM |
| Primitive Obsession | String/int used where value object is appropriate | LOW |
| Data Clumps | Same group of fields repeated across classes | MEDIUM |
| Shotgun Surgery | One change requires modifying many files | HIGH |
| Divergent Change | One class changed for many unrelated reasons | HIGH |
| Switch/If Chains | > 3 cases checking same type discriminator | MEDIUM |
| Dead Code | Unreachable code, unused methods/classes | LOW |
| Inappropriate Intimacy | Class accesses another's private internals | MEDIUM |

### Detection Examples

```yaml
detection_examples:
  god_class:
    signal: "DocumentService.java — 2500 lines, handles upload, validation, classification, notification"
    recommendation:
      extract:
        - DocumentValidationService (validation rules)
        - DocumentStorageService (S3 upload/download)
        - DocumentNotificationService (event publishing)
        - DocumentClassificationService (AI integration)
      result: "4 focused services, each < 200 lines"
  
  switch_chain:
    signal: |
      if (type.equals("PDF")) { processPdf(); }
      else if (type.equals("DOCX")) { processDocx(); }
      else if (type.equals("IMAGE")) { processImage(); }
      else if (type.equals("INVOICE")) { processInvoice(); }
    recommendation:
      pattern: "Strategy pattern"
      implementation: |
        interface DocumentProcessor { void process(Document doc); }
        class PdfProcessor implements DocumentProcessor { ... }
        class DocxProcessor implements DocumentProcessor { ... }
        // Select processor from Map<DocumentType, DocumentProcessor>
  
  primitive_obsession:
    signal: "String email passed around, validated in multiple places"
    recommendation:
      pattern: "Value object"
      implementation: |
        public record Email(String value) {
            public Email {
                if (!EMAIL_PATTERN.matcher(value).matches())
                    throw new InvalidEmailException(value);
            }
        }
  
  long_method:
    signal: "uploadDocument() — 45 lines with validation, storage, event, response"
    recommendation:
      pattern: "Extract method"
      implementation: |
        public DocumentResponse upload(request) {
            validate(request);
            Document document = createEntity(request);
            storeFile(document, request.content());
            publishEvent(document);
            return mapper.toResponse(document);
        }
```

---

## Safe Refactoring Rules

### Rule 1: Tests Before Refactoring

```yaml
pre_refactoring:
  requirement: "Comprehensive tests must exist before refactoring"
  
  if_tests_exist:
    proceed: true
    verification: "All tests pass before AND after refactoring"
  
  if_tests_missing:
    action: "Write tests first (characterization tests)"
    then: "Refactor with confidence"
    
  never: "Refactor untested code without writing tests first"
```

### Rule 2: One Refactoring at a Time

```yaml
atomic_refactoring:
  rule: "One refactoring pattern per commit"
  
  good:
    commit_1: "refactor(document): extract validation to DocumentValidator"
    commit_2: "refactor(document): extract storage to DocumentStorageService"
    commit_3: "refactor(document): extract notification to EventPublisher"
  
  bad:
    commit_1: "refactor(document): rewrite entire service"
    # Impossible to review, impossible to revert safely
```

### Rule 3: Preserve Behavior

```yaml
behavior_preservation:
  rule: "Refactoring changes structure, NOT behavior"
  
  verify:
    - All existing tests still pass (without modification)
    - API contract unchanged (same request/response)
    - Database schema unchanged
    - Event schemas unchanged
  
  if_behavior_change_needed:
    action: "That's not refactoring — it's a feature/fix (separate PR)"
```

---

## Refactoring Patterns

### Extract Class

```yaml
when: "Class has multiple responsibilities (SRP violation)"
before: "DocumentService handles validation + storage + events + classification"
after:
  - DocumentService (orchestration only)
  - DocumentValidator (validation rules)
  - DocumentStorageService (S3 operations)
  - DocumentEventPublisher (event publishing)
safety: "Extract one at a time, verify tests pass after each"
```

### Extract Method

```yaml
when: "Method is too long or does multiple things"
before: "45-line upload method with inline validation, storage, events"
after: "5-line orchestration calling validate(), store(), publish()"
safety: "IDE extract method refactoring — automatic and safe"
```

### Replace Conditional with Strategy

```yaml
when: "Switch/if chain with 4+ cases, likely to grow"
before: "if/else chain processing documents by type"
after: "Map<Type, Processor> with strategy per type"
safety: "Add strategy interface, migrate one case at a time"
```

### Introduce Value Object

```yaml
when: "Primitive type carries meaning and validation (email, money, ID)"
before: "String email validated in 3 different places"
after: "Email value object with built-in validation"
safety: "Introduce type, update usages gradually"
```

---

## When NOT to Refactor

```yaml
do_not_refactor:
  - Code that works and won't be modified again
  - Under time pressure (refactoring requires attention)
  - Without tests (too risky)
  - During a production incident (stabilize first)
  - Someone else's PR (suggest, don't refactor for them)
  - Framework/generated code (it will be overwritten)
  
prefer_refactoring_when:
  - About to add a feature to messy code (clean first)
  - Bug caused by confusing code (clarify to prevent recurrence)
  - Code review identified structural issues
  - Technical debt sprint (dedicated time)
```

---

## Refactoring Prioritization

```yaml
priority:
  high:
    - Security smell (injection risk, auth bypass)
    - Performance smell (N+1 query, memory leak)
    - Correctness risk (race condition, data corruption)
  
  medium:
    - Maintainability (God class, long method)
    - Extensibility (can't add features without rewriting)
    - Testability (untestable due to tight coupling)
  
  low:
    - Cosmetic (naming, formatting)
    - Style preference (not violating standards)
    - Speculative (might need to change someday)
```
