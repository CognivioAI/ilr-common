# Java Coding Standards

## Purpose

Define Java coding conventions, idioms, and anti-patterns for the ILR platform. Consistency across all services.

---

## Language Version

- **Java 21** (LTS)
- Use modern features: records, sealed classes, pattern matching, text blocks

---

## Formatting

```yaml
formatting:
  indentation: 4 spaces (no tabs)
  line_length: 120 characters max
  braces: same line (K&R style)
  blank_lines: 1 between methods, 2 between sections
  imports: no wildcard imports, organized (java, javax, org, com)
  tool: Checkstyle (enforced in CI)
```

---

## Modern Java Idioms

### Records (for immutable data)

```java
// ✅ Use records for DTOs, events, value objects
public record UploadDocumentRequest(
        @NotBlank String filename,
        @Positive long size,
        @NotNull DocumentType type
) {}

public record DocumentResponse(
        UUID id,
        String filename,
        String status,
        Instant createdAt
) {}

public record DocumentUploadedEvent(
        UUID documentId,
        String filename,
        String uploadedBy,
        Instant uploadedAt
) {}
```

### Sealed Classes (for restricted hierarchies)

```java
// ✅ Use sealed classes for type-safe domain models
public sealed interface ProcessingResult
        permits ProcessingResult.Success, ProcessingResult.Failure {
    
    record Success(Classification classification) implements ProcessingResult {}
    record Failure(String reason, Exception cause) implements ProcessingResult {}
}
```

### Pattern Matching

```java
// ✅ Use pattern matching with instanceof
if (result instanceof ProcessingResult.Success success) {
    document.setClassification(success.classification());
} else if (result instanceof ProcessingResult.Failure failure) {
    log.error("Processing failed: {}", failure.reason());
}
```

### Text Blocks

```java
// ✅ Use text blocks for multi-line strings (queries, JSON templates)
String query = """
        SELECT d FROM Document d
        WHERE d.status = :status
        AND d.createdAt > :since
        ORDER BY d.createdAt DESC
        """;
```

---

## Class Design

### Service Classes

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private final DocumentRepository repository;
    private final DocumentMapper mapper;
    private final DocumentEventPublisher eventPublisher;
    private final Clock clock;  // Inject clock for testability

    @Transactional
    public DocumentResponse upload(UploadDocumentRequest request, String uploadedBy) {
        log.info("Uploading document: filename={}, user={}", request.filename(), uploadedBy);
        
        var document = mapper.toEntity(request);
        document.setUploadedBy(uploadedBy);
        document.setStatus(DocumentStatus.PENDING);
        document.setCreatedAt(clock.instant());
        
        var saved = repository.save(document);
        eventPublisher.publishUploaded(saved);
        
        log.info("Document uploaded: id={}", saved.getId());
        return mapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public DocumentResponse getById(UUID id) {
        return repository.findById(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new DocumentNotFoundException(id));
    }
}
```

### Entity Classes

```java
@Entity
@Table(name = "documents")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String filename;

    @Column(nullable = false)
    private Long size;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentStatus status;

    @Column(name = "uploaded_by", nullable = false)
    private String uploadedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
```

---

## Method Design

```yaml
method_rules:
  length: "< 20 lines (prefer < 10)"
  parameters: "Max 3 (use object if more)"
  return: "Never return null — use Optional for queries"
  naming: "Verb phrase describing action (uploadDocument, findByStatus)"
  single_responsibility: "One method does one thing"
  
  patterns:
    guard_clauses: "Validate inputs at top, fail fast"
    early_return: "Return early to avoid nesting"
    no_side_effects: "Methods that return values shouldn't modify state"
```

### Guard Clauses

```java
// ✅ Good — fail fast with guard clauses
public DocumentResponse upload(UploadDocumentRequest request, String user) {
    if (request.size() > MAX_FILE_SIZE) {
        throw new DocumentValidationException("File exceeds maximum size of " + MAX_FILE_SIZE);
    }
    if (!ALLOWED_TYPES.contains(request.type())) {
        throw new DocumentValidationException("File type not allowed: " + request.type());
    }
    // Happy path continues here (not nested)
    ...
}

// ❌ Bad — deep nesting
public DocumentResponse upload(UploadDocumentRequest request, String user) {
    if (request.size() <= MAX_FILE_SIZE) {
        if (ALLOWED_TYPES.contains(request.type())) {
            // Happy path buried in nesting
            ...
        } else {
            throw new DocumentValidationException(...);
        }
    } else {
        throw new DocumentValidationException(...);
    }
}
```

---

## Null Safety

```yaml
null_rules:
  return_values:
    single_entity: "Optional<Entity> for queries"
    collections: "Empty collection, never null"
    required: "Throw exception if not found (getById pattern)"
  
  parameters:
    required: "@NotNull annotation (or just assume non-null)"
    optional: "Use @Nullable or Optional<T> parameter"
  
  forbidden:
    - "Returning null from a public method"
    - "Passing null intentionally"
    - "Using null as a sentinel value"
```

---

## Collections

```java
// ✅ Return unmodifiable collections from methods
public List<DocumentResponse> findAll() {
    return repository.findAll().stream()
            .map(mapper::toResponse)
            .toList();  // Returns unmodifiable list (Java 16+)
}

// ✅ Use Stream API for transformations
var activeDocuments = documents.stream()
        .filter(doc -> doc.getStatus() == ACTIVE)
        .sorted(comparing(Document::getCreatedAt).reversed())
        .limit(10)
        .toList();

// ❌ Never return mutable internal collections
public List<Document> getDocuments() {
    return this.documents;  // Exposes internal state
}
```

---

## Anti-Patterns (Never Do)

| Anti-Pattern | Problem | Correct Approach |
|-------------|---------|-----------------|
| `@Autowired` field injection | Untestable, hidden dependencies | Constructor injection |
| Empty catch block | Silent failures | Log + rethrow or handle |
| `new` for service dependencies | Tight coupling, untestable | Inject via constructor |
| String concatenation for SQL | SQL injection risk | Parameterized queries |
| `System.out.println` | Unstructured, no levels | SLF4J logging |
| Magic numbers | Unreadable | Named constants |
| God class (> 500 lines) | Too many responsibilities | Split into focused classes |
| Utility class with state | Misleading design | Static methods or proper service |
| Throwing `Exception`/`RuntimeException` | Too generic | Typed domain exceptions |
| `Optional.get()` without check | NPE risk | Use `orElseThrow()` or `map()` |
