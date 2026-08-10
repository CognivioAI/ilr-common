# Architecture Translator

## Purpose

Convert Architecture Agent output into concrete implementation structure — packages, classes, interfaces, and files that realize the design.

---

## Translation Flow

```
Architecture Output
    │
    ├── Service definition
    ├── API contract (OpenAPI)
    ├── Data model (entities, relationships)
    ├── Event schemas
    ├── Security requirements
    │
    ▼
Developer Agent Translation
    │
    ├── Package structure
    ├── Class hierarchy
    ├── Interface definitions
    ├── File list (what to create)
    ├── Dependency list (what to add)
    └── Configuration needed
```

---

## Service → Package Structure

### Input

```yaml
architecture:
  service:
    name: document-service
    type: api
    runtime: java-21
    framework: spring-boot
    responsibilities:
      - Manage document lifecycle
      - Validate uploads
      - Trigger AI classification
    api:
      - POST /api/v1/documents (upload)
      - GET /api/v1/documents/{id} (get by id)
      - GET /api/v1/documents (list with pagination)
      - DELETE /api/v1/documents/{id} (soft delete)
    data_model:
      entities:
        - Document (id, filename, size, status, uploadedBy, createdAt)
        - Classification (id, documentId, category, confidence, classifiedAt)
    events:
      publishes: [DocumentUploaded, DocumentDeleted]
      consumes: [ClassificationCompleted]
    dependencies:
      database: postgresql
      storage: s3
      queue: sqs
    security:
      authentication: jwt
      authorization: rbac (ADMIN, USER)
```

### Output

```
src/main/java/com/ilr/document/
├── DocumentServiceApplication.java
│
├── controller/
│   └── DocumentController.java
│
├── service/
│   ├── DocumentService.java
│   └── DocumentEventPublisher.java
│
├── repository/
│   ├── DocumentRepository.java
│   └── ClassificationRepository.java
│
├── domain/
│   ├── Document.java
│   ├── Classification.java
│   └── DocumentStatus.java (enum)
│
├── dto/
│   ├── request/
│   │   └── UploadDocumentRequest.java
│   ├── response/
│   │   ├── DocumentResponse.java
│   │   └── DocumentListResponse.java
│   └── event/
│       ├── DocumentUploadedEvent.java
│       └── ClassificationCompletedEvent.java
│
├── mapper/
│   └── DocumentMapper.java
│
├── exception/
│   ├── DocumentNotFoundException.java
│   ├── DocumentValidationException.java
│   └── GlobalExceptionHandler.java
│
├── config/
│   ├── SecurityConfig.java
│   ├── S3Config.java
│   ├── SqsConfig.java
│   └── OpenApiConfig.java
│
└── infrastructure/
    ├── s3/
    │   └── S3StorageService.java
    └── sqs/
        ├── SqsPublisher.java
        └── ClassificationEventListener.java

src/main/resources/
├── application.yml
├── application-local.yml
├── application-dev.yml
└── db/migration/
    ├── V1__create_document_table.sql
    └── V2__create_classification_table.sql

src/test/groovy/com/ilr/document/
├── controller/
│   └── DocumentControllerIT.groovy
├── service/
│   └── DocumentServiceSpec.groovy
├── repository/
│   ├── DocumentRepositoryIT.groovy
│   └── ClassificationRepositoryIT.groovy
├── integration/
│   └── DocumentUploadFlowIT.groovy
└── fixture/
    ├── DocumentFixture.groovy
    └── TestContainersConfig.groovy
```

---

## Translation Rules

### API Endpoint → Controller Method

| API Definition | Controller Method |
|---------------|-------------------|
| `POST /api/v1/documents` | `@PostMapping` uploadDocument(request) |
| `GET /api/v1/documents/{id}` | `@GetMapping("/{id}")` getDocument(id) |
| `GET /api/v1/documents` | `@GetMapping` listDocuments(pageable) |
| `DELETE /api/v1/documents/{id}` | `@DeleteMapping("/{id}")` deleteDocument(id) |

### Entity → Class Structure

| Architecture Entity | Java Implementation |
|--------------------|---------------------|
| Entity with fields | `@Entity` JPA class with `@Column` fields |
| Entity relationships | `@ManyToOne`, `@OneToMany` JPA annotations |
| Status field | Enum with defined values |
| Timestamps | `@CreatedDate`, `@LastModifiedDate` |
| ID | `@Id @GeneratedValue(strategy = UUID)` |

### Event → Class Structure

| Architecture Event | Java Implementation |
|-------------------|---------------------|
| Event published | Record class + SQS publisher method |
| Event consumed | `@SqsListener` method on listener class |

### Dependency → Configuration

| Architecture Dependency | Implementation |
|------------------------|----------------|
| `database: postgresql` | Spring Data JPA + Flyway migration |
| `storage: s3` | AWS SDK S3Client + config class |
| `queue: sqs` | Spring Cloud AWS SQS + publisher/listener |
| `authentication: jwt` | Spring Security + JWT filter |

---

## Layer Rules (Never Violate)

```
Controller (HTTP layer)
    │
    │  DTOs only (Request/Response objects)
    │  Never passes entities to client
    ▼
Service (Business logic)
    │
    │  Domain objects
    │  Orchestrates operations
    │  Transaction boundaries here
    ▼
Repository (Data access)
    │
    │  Entities only
    │  No business logic
    ▼
Database

RULES:
  ✅ Controller → Service → Repository → Database
  ❌ Controller → Repository (skip service)
  ❌ Controller → Database (skip everything)
  ❌ Service → Controller (circular)
  ❌ Repository contains business logic
```

---

## Naming Conventions

```yaml
naming:
  packages:
    base: "com.ilr.{service-name}"
    layers: [controller, service, repository, domain, dto, mapper, exception, config, infrastructure]
  
  classes:
    controller: "{Entity}Controller"
    service: "{Entity}Service"
    repository: "{Entity}Repository"
    entity: "{Entity}" (plain name)
    dto_request: "{Action}{Entity}Request"
    dto_response: "{Entity}Response"
    event: "{Entity}{Action}Event"
    mapper: "{Entity}Mapper"
    exception: "{Entity}{Problem}Exception"
    config: "{Feature}Config"
  
  methods:
    create: "create{Entity}", "save{Entity}"
    read: "get{Entity}ById", "find{Entity}s", "list{Entity}s"
    update: "update{Entity}"
    delete: "delete{Entity}"
    search: "search{Entity}s"
  
  tests:
    unit: "{Class}Spec.groovy"
    integration: "{Class}IT.groovy"
    fixture: "{Entity}Fixture.groovy"
```

---

## Dependency Resolution

```yaml
dependencies_to_add:
  spring_boot_starters:
    - spring-boot-starter-web
    - spring-boot-starter-data-jpa
    - spring-boot-starter-validation
    - spring-boot-starter-security
    - spring-boot-starter-actuator
  
  aws:
    - spring-cloud-aws-starter-sqs
    - software.amazon.awssdk:s3
  
  database:
    - postgresql (runtime)
    - flyway-core
  
  utilities:
    - lombok (compile only)
    - mapstruct (compile only)
  
  testing:
    - spring-boot-starter-test
    - spock-core
    - spock-spring
    - testcontainers (postgresql)
    - rest-assured
    - groovy (test scope)
```
