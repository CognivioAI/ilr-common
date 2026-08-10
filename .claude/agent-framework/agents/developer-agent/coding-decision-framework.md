# Coding Decision Framework

## Purpose

Define how the Developer Agent makes implementation-level decisions — design pattern selection, abstraction choices, and trade-offs within the boundaries of the approved architecture.

---

## Decision Scope

```yaml
developer_decides:
  - Which design pattern to use for a specific problem
  - How to structure classes and methods
  - When to introduce abstraction vs keep simple
  - How to handle errors within the defined strategy
  - Which utility library to use (within approved stack)
  - How to optimize a query or algorithm

architecture_decides:
  - Service boundaries
  - Database technology
  - API style (REST/GraphQL/gRPC)
  - Cross-service patterns (saga, event sourcing)
  - Security approach (OAuth, JWT, mTLS)
```

---

## Design Pattern Selection

### When to Apply Patterns

| Problem Shape | Pattern | Example |
|--------------|---------|---------|
| Multiple types with same interface | Strategy | Document processors by type |
| Object creation is complex | Builder / Factory | Complex DTOs, test fixtures |
| Need to decouple event producer/consumer | Observer / Event | Domain events |
| Need to wrap external dependency | Adapter | S3 client wrapper |
| Need to add behavior without modifying class | Decorator | Logging, caching, metrics |
| Need to validate/transform in sequence | Chain of Responsibility | Validation pipeline |
| Complex object construction step-by-step | Builder | Query builders, request builders |
| Need one instance shared across application | Singleton (via Spring `@Bean`) | Config, clients |
| Need to convert between representations | Mapper | Entity ↔ DTO |

### Decision Process

```yaml
pattern_decision:
  step_1: "Is there a problem? (Don't pattern for fun)"
  step_2: "Is the simplest solution (if/else, direct call) adequate?"
  step_3: "Will the code grow? (2 cases = if/else, 5+ cases = pattern)"
  step_4: "Check existing codebase — is this pattern already used here?"
  step_5: "If yes → follow existing pattern. If no → justify introducing it."
```

### Anti-Pattern: Over-Engineering

```yaml
over_engineering_signals:
  - Interface with only one implementation (and no plan for more)
  - Abstract factory for simple object creation
  - Strategy pattern for 2 options that won't grow
  - Event system for in-process communication that's never async
  
  rule: "You Aren't Gonna Need It (YAGNI) — add complexity when needed, not before"
```

---

## Abstraction Decisions

### When to Abstract

| Situation | Abstract? | Why |
|-----------|-----------|-----|
| External dependency (S3, SQS, DB) | ✅ Yes | Testability, swap-ability |
| Business rule likely to change | ✅ Yes | Isolate change |
| Same code in 3+ places | ✅ Yes | DRY |
| Complex algorithm | ✅ Yes | Named, tested, reusable |
| Simple CRUD with no business logic | ❌ No | Don't abstract for abstraction's sake |
| Code used in exactly one place | ❌ No | Extract when second use appears |
| Framework glue code | ❌ No | Wrapping Spring adds nothing |

### Rule of Three

```
First time: Write it inline
Second time: Note the duplication, keep it
Third time: Extract into shared abstraction
```

---

## Error Handling Decision Tree

```yaml
error_handling:
  can_caller_recover:
    yes: "Return Optional or Result type — let caller decide"
    no: "Throw exception with context"
  
  is_it_a_business_rule:
    yes: "Domain exception (DocumentNotFoundException, InsufficientPermissionException)"
    no: "Infrastructure exception (let Spring handle or wrap)"
  
  is_it_expected:
    yes: "Handle gracefully (e.g., item not found = 404, not a 500)"
    no: "Log error, propagate up, let global handler respond"
  
  never:
    - Empty catch blocks
    - Catching Exception/Throwable broadly
    - Swallowing exceptions silently
    - Returning null where Optional is appropriate
    - Throwing generic RuntimeException without context
```

---

## Immutability Decision

```yaml
immutability:
  default: immutable
  
  use_immutable:
    - DTOs (records)
    - Events
    - Value objects
    - Configuration
    - Method parameters
  
  use_mutable:
    - JPA entities (Hibernate requires setters)
    - Builders (by design)
    - Collections being built incrementally
  
  implementation:
    java_records: "Use for DTOs, events, value objects"
    final_fields: "Use for service classes (constructor injection)"
    unmodifiable_collections: "Use Collections.unmodifiableList() for returned collections"
```

---

## Dependency Choice

```yaml
dependency_decisions:
  prefer:
    - Well-known, actively maintained libraries
    - Libraries already in the project
    - Spring ecosystem libraries (consistency)
    - Libraries with minimal transitive dependencies
  
  avoid:
    - Libraries not updated in > 1 year
    - Libraries with known vulnerabilities
    - Libraries that duplicate existing functionality
    - Libraries with restrictive licenses (GPL in commercial code)
  
  when_adding_new:
    - Justify in PR description
    - Check for existing alternative in project
    - Verify active maintenance
    - Check vulnerability databases
    - Pin exact version (not range)
```

---

## Naming Decisions

```yaml
naming:
  classes:
    service: "{Domain}Service" (DocumentService, ClassificationService)
    repository: "{Domain}Repository"
    controller: "{Domain}Controller"
    exception: "{Domain}{Problem}Exception" (DocumentNotFoundException)
    event: "{Domain}{Action}Event" (DocumentUploadedEvent)
    dto: "{Action}{Domain}Request/Response" (UploadDocumentRequest)
    mapper: "{Domain}Mapper"
    config: "{Feature}Config" (SecurityConfig, S3Config)
    
  methods:
    queries: "findBy{Criteria}", "getById" (throws if not found)
    commands: "create{Domain}", "update{Domain}", "delete{Domain}"
    boolean: "is{Condition}", "has{Property}", "can{Action}"
    conversion: "to{Target}", "from{Source}"
    
  packages:
    base: "com.ilr.{service-name}"
    never: deep nesting beyond 2 levels from base

  variables:
    collections: plural (documents, classifications)
    single: singular (document, request)
    boolean: is/has prefix (isActive, hasPermission)
    avoid: single letters (except loop variables i, j), abbreviations
```

---

## When to Ask for Help

```yaml
escalation:
  ask_architecture_agent:
    - "This feature requires a new service boundary"
    - "The data model needs a relationship not in the design"
    - "The approved pattern doesn't fit this use case"
    - "Performance requires caching not in the architecture"
  
  ask_devops_agent:
    - "I need a new environment variable"
    - "The service needs a new infrastructure dependency"
    - "The health check needs to verify a new dependency"
  
  ask_product_agent:
    - "The acceptance criteria are ambiguous"
    - "Edge case not covered in requirements"
    - "Conflicting requirements between stories"
  
  decide_alone:
    - Everything else within the approved architecture
    - Internal implementation details
    - Test approach and coverage
    - Refactoring within a service
```
