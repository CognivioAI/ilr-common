# Task Planner

## Purpose

Break architecture designs into ordered, granular, independently testable implementation tasks. Never ask the agent to "write the whole service" — decompose into focused units of work.

---

## Planning Strategy

```yaml
strategy:
  approach: bottom_up
  order: "Data layer → Business logic → API → Integration → Security → Tests"
  rationale:
    - Each layer depends on the one below
    - Enables incremental testing at each stage
    - Minimizes rework (foundation first)
    - Allows early detection of design issues
```

---

## Task Decomposition Rules

### Rule 1: One Concern Per Task

```yaml
good:
  task: "Create Document entity and migration"
  scope: [V1__create_document.sql, Document.java, DocumentStatus.java]

bad:
  task: "Create Document entity, repository, service, and controller"
  why: "Too broad — any failure requires debugging everything"
```

### Rule 2: Each Task is Independently Testable

```yaml
good:
  task: "Create DocumentService with upload logic"
  testable: true
  test: "DocumentServiceTest — mock repository, verify behavior"

bad:
  task: "Add helper methods to various classes"
  testable: independently? no — scattered changes
```

### Rule 3: Tasks Have Clear Acceptance Criteria

```yaml
task:
  name: "Create document upload API endpoint"
  acceptance:
    - POST /api/v1/documents accepts multipart file
    - Returns 201 with document ID and status PENDING
    - Returns 400 if file exceeds 50MB
    - Returns 401 if not authenticated
    - Returns 403 if user lacks UPLOAD permission
```

---

## Standard Task Templates

### Template: New CRUD Service

```yaml
tasks:
  1_entity_and_migration:
    description: "Create database table and JPA entity"
    creates: [migration SQL, Entity class, Enum classes]
    tests: none (tested via repository in next task)
    commit: "feat({domain}): add {entity} entity and migration"
  
  2_repository:
    description: "Create Spring Data repository with custom queries"
    creates: [Repository interface]
    tests: [RepositoryIT.groovy with Testcontainers]
    commit: "feat({domain}): add {entity} repository"
  
  3_dto_and_mapper:
    description: "Create request/response DTOs and MapStruct mapper"
    creates: [Request DTO, Response DTO, Mapper interface]
    tests: [MapperSpec.groovy]
    commit: "feat({domain}): add {entity} DTOs and mapper"
  
  4_service:
    description: "Create service layer with business logic"
    creates: [Service class]
    tests: [ServiceSpec.groovy with mocked dependencies]
    commit: "feat({domain}): add {entity} service"
  
  5_exception_handling:
    description: "Create domain exceptions and global handler"
    creates: [Exception classes, GlobalExceptionHandler updates]
    tests: [ExceptionHandler test cases in controller IT]
    commit: "feat({domain}): add {entity} exception handling"
  
  6_controller:
    description: "Create REST controller with validation"
    creates: [Controller class]
    tests: [ControllerIT.groovy with MockMvc]
    commit: "feat({domain}): add {entity} REST API"
  
  7_security:
    description: "Configure endpoint security"
    creates: [SecurityConfig updates]
    tests: [SecurityIT.groovy scenarios]
    commit: "feat({domain}): add {entity} endpoint security"
  
  8_integration_test:
    description: "Full end-to-end integration test"
    creates: [FlowIT.groovy test class]
    commit: "test({domain}): add end-to-end {entity} flow test"
```

### Template: Event Producer

```yaml
tasks:
  1_event_class:
    description: "Create event record class"
    creates: [Event record, Event constants]
    commit: "feat({domain}): add {event} event class"
  
  2_publisher:
    description: "Create event publisher service"
    creates: [EventPublisher class]
    tests: [EventPublisherSpec.groovy with mock SQS]
    commit: "feat({domain}): add {event} publisher"
  
  3_integration:
    description: "Integrate publisher into service layer"
    creates: [Service modification]
    tests: [Updated service specs]
    commit: "feat({domain}): publish {event} on {action}"
```

### Template: Event Consumer

```yaml
tasks:
  1_event_class:
    description: "Create event record for incoming event"
    creates: [Event record class]
    commit: "feat({domain}): add {event} event class"
  
  2_listener:
    description: "Create SQS listener"
    creates: [EventListener class]
    tests: [EventListenerIT.groovy with LocalStack]
    commit: "feat({domain}): add {event} listener"
  
  3_processing:
    description: "Implement event processing logic"
    creates: [Processing logic in service]
    tests: [Service specs for processing]
    commit: "feat({domain}): handle {event} processing"
```

---

## Task Estimation

```yaml
estimation:
  complexity_guide:
    trivial: "< 30 min — config change, add field, simple query"
    small: "1-2 hours — new endpoint (CRUD), simple service method"
    medium: "2-4 hours — new feature with business logic and tests"
    large: "4-8 hours — complex feature with multiple integrations"
    epic: "Multiple days — new service, major refactoring"
  
  size_indicators:
    files_created:
      small: 1-3 files
      medium: 4-8 files
      large: 9-15 files
      epic: 15+ files
    
    tests_needed:
      small: 2-5 test cases
      medium: 5-15 test cases
      large: 15-30 test cases
```

---

## Dependency Graph

```yaml
dependency_management:
  rules:
    - Never start a task before its dependencies are complete
    - Identify parallelizable tasks (no shared dependencies)
    - Critical path = longest chain of dependent tasks
  
  example:
    parallel_possible:
      - task_3 (DTOs) and task_5 (exceptions) can run in parallel
      - Both only depend on task_1 (entity)
    
    must_be_sequential:
      - task_6 (controller) requires task_3, task_4, task_5
      - task_8 (integration test) requires all previous tasks
```

---

## Task Validation

Before starting implementation, validate the plan:

```yaml
validation:
  completeness:
    - Every API endpoint has a corresponding controller task
    - Every entity has a migration task
    - Every service method has a test task
    - DevOps contract satisfied (health, metrics, config)
  
  ordering:
    - No circular dependencies between tasks
    - Foundation (entities, repos) comes before logic (services)
    - Logic comes before presentation (controllers)
    - Integration tests come last
  
  scope:
    - Tasks align with architecture (not gold-plating)
    - No unnecessary abstractions added
    - Scope matches user story acceptance criteria
```
