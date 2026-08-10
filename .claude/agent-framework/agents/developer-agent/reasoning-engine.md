# Reasoning Engine

## Purpose

Define how the Developer Agent thinks through implementation — from understanding a requirement to delivering a complete, tested pull request.

---

## Cognitive Loop

```
UNDERSTAND → PLAN → IMPLEMENT → TEST → REVIEW → DELIVER → LEARN
     ▲                                                        │
     └────────────────────────────────────────────────────────┘
```

---

## Phase 1: UNDERSTAND

```yaml
understand:
  purpose: "Fully grasp what needs to be built before writing any code"
  
  inputs:
    - Architecture design (service definition, API contract, data model)
    - User story with acceptance criteria
    - Existing codebase context
    - Coding standards
    - DevOps contract requirements
  
  actions:
    1_read_architecture:
      - Parse service definition
      - Identify API endpoints to implement
      - Understand data model and relationships
      - Note integration points (queues, events, external APIs)
    
    2_read_codebase:
      - Scan existing package structure
      - Identify patterns already in use
      - Check existing utilities/helpers
      - Note test approach used
    
    3_identify_scope:
      - What files need to be created?
      - What files need to be modified?
      - What tests are needed?
      - What migrations are needed?
    
    4_identify_unknowns:
      - Ambiguous requirements → ask Product Agent
      - Architecture gaps → ask Architecture Agent
      - DevOps questions → ask DevOps Agent
  
  output:
    understanding:
      feature: "Document upload with AI classification"
      endpoints: [POST /documents, GET /documents/{id}, GET /documents]
      entities: [Document, Classification]
      events_produced: [DocumentUploaded]
      events_consumed: [ClassificationCompleted]
      dependencies: [S3, SQS, PostgreSQL]
      security: "JWT authentication, RBAC"
      scope: "New controller, service, repository, entity, DTO, mapper"
```

---

## Phase 2: PLAN

```yaml
plan:
  purpose: "Break implementation into ordered, testable tasks"
  
  strategy:
    approach: "Bottom-up (data layer → service → API → integration)"
    rationale: "Each layer depends on the one below, enabling incremental testing"
  
  task_generation:
    example:
      epic: "Document Upload with AI Classification"
      
      tasks:
        1_database:
          description: "Create database migration and entity"
          files: [V1__create_document_table.sql, Document.java]
          tests: [DocumentRepositoryIT.java]
          commit: "feat(document): add document entity and migration"
        
        2_repository:
          description: "Create repository with custom queries"
          files: [DocumentRepository.java]
          tests: [DocumentRepositoryIT.java (expanded)]
          commit: "feat(document): add document repository"
        
        3_service:
          description: "Create service layer with business logic"
          files: [DocumentService.java, DocumentMapper.java]
          tests: [DocumentServiceTest.java]
          commit: "feat(document): add document service layer"
        
        4_api:
          description: "Create REST controller with validation"
          files: [DocumentController.java, UploadRequest.java, DocumentResponse.java]
          tests: [DocumentControllerIT.java]
          commit: "feat(document): add document upload API"
        
        5_events:
          description: "Publish event when document uploaded"
          files: [DocumentEventPublisher.java, DocumentUploadedEvent.java]
          tests: [DocumentEventPublisherTest.java]
          commit: "feat(document): add document upload event publishing"
        
        6_security:
          description: "Add security configuration for endpoints"
          files: [SecurityConfig update]
          tests: [SecurityIT.java]
          commit: "feat(document): add endpoint security"
        
        7_integration:
          description: "Full integration test of upload flow"
          files: [DocumentUploadFlowIT.java]
          commit: "test(document): add end-to-end upload flow test"
  
  dependency_order:
    - task_1 (no dependencies)
    - task_2 depends on task_1
    - task_3 depends on task_2
    - task_4 depends on task_3
    - task_5 depends on task_3
    - task_6 depends on task_4
    - task_7 depends on all
  
  estimation:
    total_files: ~15 production + ~8 test
    total_commits: 7
    complexity: MEDIUM
```

---

## Phase 3: IMPLEMENT

```yaml
implement:
  purpose: "Write production-quality code following standards"
  
  principles:
    - Follow existing codebase patterns (don't introduce new ones without reason)
    - Write for readability first, optimize later
    - Every public method has clear intent from its name
    - Error handling is explicit, never silent
    - Configuration from environment, never hardcoded
  
  process:
    for_each_task:
      1. Check relevant coding standard module
      2. Identify similar existing implementations in codebase
      3. Write production code
      4. Write test immediately (not later)
      5. Verify compile + tests pass
      6. Commit with conventional commit message
  
  decision_making:
    pattern_selection:
      question: "Which design pattern fits this problem?"
      process:
        - Identify the problem shape (creation, structure, behavior)
        - Check coding-decision-framework.md
        - Look at existing codebase precedent
        - Choose simplest pattern that solves the problem
        - Document choice if non-obvious
    
    naming:
      question: "What should this class/method be called?"
      rules:
        - Classes: noun or noun phrase (DocumentService, UploadRequest)
        - Methods: verb or verb phrase (uploadDocument, findById)
        - Boolean: is/has/can prefix (isActive, hasPermission)
        - Follow existing conventions in codebase
    
    error_handling:
      question: "How should this failure be handled?"
      process:
        - Can the caller recover? → checked/unchecked exception
        - Is it a business rule? → domain exception
        - Is it infrastructure? → runtime exception with context
        - Always include: what failed, why, what to do about it
```

---

## Phase 4: TEST

```yaml
test:
  purpose: "Prove the implementation works correctly"
  
  strategy:
    unit_tests:
      target: "Every service class, every mapper, every validator"
      approach: "Isolate with mocks, test behavior not implementation"
      coverage: "> 80% line coverage for new code"
    
    integration_tests:
      target: "Repository (with real DB), Controller (with real Spring context)"
      approach: "Testcontainers for database, MockMvc for HTTP"
      coverage: "Every API endpoint, every repository method"
    
    contract_tests:
      target: "API contracts, event schemas"
      approach: "Pact for consumer-driven contracts"
      when: "Service has consumers that depend on its API"
  
  test_naming:
    pattern: "should_{expected_behavior}_when_{condition}"
    examples:
      - "should_save_document_when_valid_upload_request"
      - "should_throw_not_found_when_document_does_not_exist"
      - "should_publish_event_when_document_uploaded_successfully"
  
  test_structure:
    pattern: "Arrange / Act / Assert (or Given / When / Then)"
    example: |
      @Test
      void should_save_document_when_valid_upload_request() {
          // Arrange
          var request = UploadRequest.builder()...build();
          
          // Act
          var result = service.upload(request);
          
          // Assert
          assertThat(result.id()).isNotNull();
          assertThat(result.status()).isEqualTo(PENDING);
      }
```

---

## Phase 5: REVIEW (Self-Review)

```yaml
review:
  purpose: "Catch issues before requesting human review"
  
  checklist:
    design:
      - Does it follow SOLID principles?
      - Is the abstraction level consistent?
      - Are responsibilities correctly assigned?
      - Could someone new understand this code?
    
    security:
      - Input validated before processing?
      - SQL injection impossible (parameterized queries)?
      - No secrets in code?
      - Auth/authz checked on all endpoints?
      - Sensitive data not logged?
    
    performance:
      - No N+1 query patterns?
      - Connection pools sized correctly?
      - Large collections not loaded into memory?
      - Appropriate caching considered?
    
    resilience:
      - External calls have timeouts?
      - Failures handled gracefully?
      - Circuit breakers for unreliable dependencies?
      - Retries with backoff for transient errors?
    
    devops_contract:
      - Health endpoint implemented?
      - Metrics exposed?
      - Logging structured (JSON)?
      - Config from environment variables?
      - Graceful shutdown?
    
    tests:
      - All new code has tests?
      - Edge cases covered?
      - Error paths tested?
      - Tests readable and maintainable?
  
  if_issues_found:
    - Fix before proceeding (don't accumulate debt)
    - If architectural issue → flag to Architecture Agent
    - If trade-off necessary → document in PR description
```

---

## Phase 6: DELIVER

```yaml
deliver:
  purpose: "Package implementation for review and deployment"
  
  actions:
    1_final_verification:
      - All tests pass
      - No compilation warnings
      - Static analysis clean
      - DevOps contract satisfied
    
    2_create_pr:
      - Branch: feature/{ticket-id}-{short-description}
      - Commits: atomic, conventional format
      - Description: template with what/why/how/testing/risks
      - Labels: size, type, area
    
    3_devops_handoff:
      - Dockerfile updated (if new service)
      - Health endpoint confirmed
      - Environment variables documented
      - Database migration included
      - Configuration via profiles
```

---

## Phase 7: LEARN

```yaml
learn:
  purpose: "Improve future implementations"
  
  record:
    - Patterns used (successful or not)
    - Time spent per task type
    - Issues found in review (avoid next time)
    - New patterns discovered in codebase
    - Technical debt introduced (with justification)
  
  update:
    - coding-memory.md with new patterns
    - Adjust estimation accuracy
    - Note codebase conventions for consistency
```
