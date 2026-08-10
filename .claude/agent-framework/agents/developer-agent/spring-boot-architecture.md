# Spring Boot Architecture

## Purpose

Define the standard Spring Boot service architecture — package structure, layer responsibilities, configuration patterns, and implementation conventions.

---

## Package Structure

```
com.ilr.{service-name}/
├── {ServiceName}Application.java          # @SpringBootApplication entry point
│
├── controller/                            # HTTP layer (REST endpoints)
│   ├── {Entity}Controller.java
│   └── advice/
│       └── GlobalExceptionHandler.java
│
├── service/                               # Business logic layer
│   ├── {Entity}Service.java
│   └── event/
│       └── {Entity}EventPublisher.java
│
├── repository/                            # Data access layer
│   └── {Entity}Repository.java
│
├── domain/                                # Domain entities (JPA)
│   ├── {Entity}.java
│   └── {EntityStatus}.java (enums)
│
├── dto/                                   # Data transfer objects
│   ├── request/
│   │   └── {Action}{Entity}Request.java
│   └── response/
│       └── {Entity}Response.java
│
├── mapper/                                # Entity ↔ DTO conversion
│   └── {Entity}Mapper.java
│
├── exception/                             # Custom exceptions
│   ├── {Entity}NotFoundException.java
│   └── {Entity}ValidationException.java
│
├── config/                                # Configuration classes
│   ├── SecurityConfig.java
│   ├── OpenApiConfig.java
│   └── {Infrastructure}Config.java
│
└── infrastructure/                        # External integrations
    ├── s3/
    │   └── S3StorageService.java
    └── messaging/
        ├── SqsPublisher.java
        └── {Event}Listener.java
```

---

## Layer Responsibilities

### Controller Layer

```java
@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
@Validated
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentResponse upload(
            @Valid @RequestBody UploadDocumentRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return documentService.upload(request, user.getUsername());
    }

    @GetMapping("/{id}")
    public DocumentResponse getById(@PathVariable UUID id) {
        return documentService.getById(id);
    }

    @GetMapping
    public Page<DocumentResponse> list(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return documentService.list(pageable);
    }
}
```

**Rules:**
- No business logic (delegate to service)
- Validate input (via `@Valid`)
- Convert DTOs if needed (or let service handle)
- Handle HTTP concerns (status codes, headers)
- Never inject Repository directly

### Service Layer

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DocumentService {

    private final DocumentRepository repository;
    private final DocumentMapper mapper;
    private final DocumentEventPublisher eventPublisher;
    private final S3StorageService storageService;

    @Transactional
    public DocumentResponse upload(UploadDocumentRequest request, String uploadedBy) {
        // Business logic: validate, transform, persist, publish
        var document = mapper.toEntity(request);
        document.setUploadedBy(uploadedBy);
        document.setStatus(DocumentStatus.PENDING);
        
        var saved = repository.save(document);
        storageService.upload(saved.getId(), request.content());
        eventPublisher.publishUploaded(saved);
        
        return mapper.toResponse(saved);
    }

    public DocumentResponse getById(UUID id) {
        var document = repository.findById(id)
                .orElseThrow(() -> new DocumentNotFoundException(id));
        return mapper.toResponse(document);
    }
}
```

**Rules:**
- Contains all business logic
- Manages transaction boundaries (`@Transactional`)
- Orchestrates infrastructure calls (DB, queue, storage)
- Throws domain exceptions (not HTTP exceptions)
- Returns DTOs (not entities)

### Repository Layer

```java
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    Optional<Document> findByIdAndDeletedFalse(UUID id);

    @Query("SELECT d FROM Document d WHERE d.status = :status AND d.createdAt > :since")
    List<Document> findRecentByStatus(
            @Param("status") DocumentStatus status,
            @Param("since") Instant since);

    Page<Document> findAllByDeletedFalse(Pageable pageable);
}
```

**Rules:**
- Interface only (Spring Data generates implementation)
- Custom queries when Spring Data naming becomes unwieldy
- Never contains business logic
- Return entities (service converts to DTOs)

---

## Dependency Injection

### Preferred: Constructor Injection

```java
@Service
@RequiredArgsConstructor  // Lombok generates constructor
public class DocumentService {
    private final DocumentRepository repository;  // final = immutable
    private final DocumentMapper mapper;
}
```

### Forbidden

```java
// ❌ NEVER use field injection
@Autowired
private DocumentRepository repository;

// ❌ NEVER use setter injection for required dependencies
@Autowired
public void setRepository(DocumentRepository repo) { ... }
```

---

## Configuration

### application.yml Structure

```yaml
# application.yml — common defaults
spring:
  application:
    name: document-service
  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: validate  # Never auto-create in production
  flyway:
    enabled: true

server:
  port: 8080
  shutdown: graceful

management:
  endpoints:
    web:
      exposure:
        include: health,prometheus,info
  endpoint:
    health:
      probes:
        enabled: true

---
# application-local.yml — local development overrides
spring:
  config:
    activate:
      on-profile: local
  datasource:
    url: jdbc:postgresql://localhost:5432/ilr_dev
    username: dev
    password: devpass

---
# application-production.yml — production (env vars)
spring:
  config:
    activate:
      on-profile: production
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}

logging:
  level:
    root: INFO
    com.ilr: INFO
```

### Environment Variables (DevOps Contract)

```yaml
environment_variables:
  required:
    - DB_URL (database connection)
    - DB_USERNAME (database user)
    - DB_PASSWORD (database password)
    - AWS_REGION (AWS region)
    - SPRING_PROFILES_ACTIVE (active profile)
  optional:
    - SQS_QUEUE_URL (processing queue, has default)
    - S3_BUCKET (document storage, has default)
    - LOG_LEVEL (default: INFO)
```

---

## Exception Handling

### Exception Hierarchy

```java
// Base domain exception
public abstract class DomainException extends RuntimeException {
    private final String code;
    private final HttpStatus httpStatus;
    
    protected DomainException(String code, String message, HttpStatus status) {
        super(message);
        this.code = code;
        this.httpStatus = status;
    }
}

// Specific exceptions
public class DocumentNotFoundException extends DomainException {
    public DocumentNotFoundException(UUID id) {
        super("DOCUMENT_NOT_FOUND", 
              "Document with id " + id + " not found",
              HttpStatus.NOT_FOUND);
    }
}

public class DocumentValidationException extends DomainException {
    public DocumentValidationException(String detail) {
        super("DOCUMENT_VALIDATION_FAILED",
              "Document validation failed: " + detail,
              HttpStatus.BAD_REQUEST);
    }
}
```

### Global Handler

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponse> handleDomainException(DomainException ex) {
        log.warn("Domain exception: {} - {}", ex.getCode(), ex.getMessage());
        return ResponseEntity
                .status(ex.getHttpStatus())
                .body(new ErrorResponse(ex.getCode(), ex.getMessage(), getTraceId()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        var errors = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .toList();
        return ResponseEntity
                .badRequest()
                .body(new ErrorResponse("VALIDATION_ERROR", errors.toString(), getTraceId()));
    }
}
```

### Error Response Contract

```json
{
  "code": "DOCUMENT_NOT_FOUND",
  "message": "Document with id 123 not found",
  "traceId": "abc-123-def"
}
```

---

## Logging Standards

```java
@Slf4j
@Service
public class DocumentService {

    public DocumentResponse upload(UploadDocumentRequest request, String user) {
        log.info("Uploading document: filename={}, user={}", request.filename(), user);
        
        try {
            var result = processUpload(request);
            log.info("Document uploaded successfully: id={}, status={}", result.id(), result.status());
            return result;
        } catch (Exception e) {
            log.error("Document upload failed: filename={}, user={}, error={}", 
                      request.filename(), user, e.getMessage(), e);
            throw e;
        }
    }
}
```

**Rules:**
- Use SLF4J (`@Slf4j` Lombok annotation)
- INFO: business events (uploaded, processed, deleted)
- WARN: recoverable issues (retry, fallback)
- ERROR: failures requiring attention
- DEBUG: technical details (only for troubleshooting)
- Never log PII or secrets
- Always include context (IDs, operation names)

---

## Testing Conventions

```groovy
// Unit test (Spock — mock dependencies)
class DocumentServiceSpec extends Specification {
    DocumentRepository repository = Mock()
    DocumentMapper mapper = Mock()
    DocumentService service = new DocumentService(repository, mapper)
    
    def "should save document when valid request"() {
        given: "a valid upload request"
        // ...
        when: "the document is uploaded"
        // ...
        then: "the document is persisted"
        // ...
    }
}

// Integration test (Spock + Testcontainers — real database)
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = NONE)
class DocumentRepositoryIT extends Specification {
    @Shared @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine")
    
    @Autowired
    DocumentRepository repository

    def "should persist and retrieve document"() {
        given: "a new document entity"
        // ...
        when: "saved and retrieved"
        // ...
        then: "data matches"
        // ...
    }
}

// API test (Spock + REST Assured — full Spring context)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
class DocumentControllerIT extends Specification {
    @LocalServerPort
    int port
    
    def "should return 201 when document uploaded"() {
        given: "a valid upload request body"
        // ...
        when: "POST /api/v1/documents"
        // ...
        then: "201 created with document id"
        // ...
    }
}
```
