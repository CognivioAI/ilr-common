# Test Generation Engine

## Purpose

Systematic test generation — given a feature implementation, automatically determine what tests are needed, generate comprehensive Spock specifications, and ensure all paths are covered.

---

## Generation Process

```
Feature Implementation
    │
    ▼
1. ANALYZE — What code was written? What paths exist?
    │
    ▼
2. IDENTIFY — What test scenarios are needed?
    │
    ▼
3. GENERATE — Write Spock specs for each scenario
    │
    ▼
4. VERIFY — Are all paths covered? Any gaps?
```

---

## Test Scenario Identification

### From API Endpoint

```yaml
endpoint_analysis:
  endpoint: "POST /api/v1/documents"
  method: upload
  
  scenarios:
    happy_path:
      - "should upload document when valid request"
      - "should return 201 with document id and PENDING status"
      - "should store file in S3"
      - "should publish DocumentUploaded event"
    
    validation_errors:
      - "should return 400 when filename is blank"
      - "should return 400 when filename exceeds 255 characters"
      - "should return 400 when size is zero or negative"
      - "should return 400 when size exceeds 50MB limit"
      - "should return 400 when document type is null"
      - "should return 400 when document type is invalid"
    
    authentication:
      - "should return 401 when no authentication token"
      - "should return 401 when token is expired"
      - "should return 403 when user lacks UPLOAD role"
    
    error_cases:
      - "should return 500 when database is unavailable"
      - "should return 500 when S3 upload fails"
      - "should still save document if event publishing fails"
    
    edge_cases:
      - "should handle filename with special characters"
      - "should handle duplicate filename (same user)"
      - "should handle concurrent uploads from same user"
```

### From Service Method

```yaml
service_analysis:
  method: "DocumentService.upload(request, user)"
  
  scenarios:
    behavior:
      - "should save document with PENDING status"
      - "should set uploadedBy from authenticated user"
      - "should set createdAt to current time"
      - "should call mapper to convert request to entity"
      - "should call repository to persist entity"
      - "should call event publisher after save"
      - "should return mapped response"
    
    failures:
      - "should throw DocumentValidationException when file type not allowed"
      - "should throw DocumentValidationException when file too large"
      - "should propagate repository exceptions"
      - "should not publish event if save fails"
    
    interactions:
      - "should call repository.save exactly once"
      - "should call eventPublisher.publishUploaded exactly once"
      - "should not call eventPublisher if repository throws"
```

### From Data Model

```yaml
repository_analysis:
  entity: Document
  
  scenarios:
    crud:
      - "should persist document with all required fields"
      - "should generate UUID on save"
      - "should retrieve document by id"
      - "should return empty Optional when id not found"
    
    custom_queries:
      - "should find documents by status"
      - "should find documents by uploadedBy"
      - "should paginate results correctly"
      - "should sort by createdAt descending"
    
    constraints:
      - "should enforce filename not null"
      - "should enforce status not null"
      - "should enforce uploadedBy not null"
    
    soft_delete:
      - "should not return deleted documents in standard queries"
      - "should mark document as deleted (not remove)"
```

---

## Spock Spec Generation Templates

### Unit Spec (Service Layer)

```groovy
class DocumentServiceSpec extends Specification {

    // Dependencies (mocked)
    DocumentRepository repository = Mock()
    DocumentMapper mapper = Mock()
    DocumentEventPublisher eventPublisher = Mock()
    Clock clock = Clock.fixed(Instant.parse("2026-01-15T10:00:00Z"), ZoneOffset.UTC)
    
    // System under test
    DocumentService service = new DocumentService(repository, mapper, eventPublisher, clock)

    // --- Happy Path ---
    
    def "should save document with PENDING status when valid upload"() {
        given: "a valid upload request"
        def request = anUploadRequest().build()
        def entity = aDocument().build()
        def response = aDocumentResponse().build()
        
        when: "uploading the document"
        def result = service.upload(request, "user-1")
        
        then: "document is mapped, saved, and event published"
        1 * mapper.toEntity(request) >> entity
        1 * repository.save({ it.status == DocumentStatus.PENDING }) >> entity
        1 * eventPublisher.publishUploaded(entity)
        1 * mapper.toResponse(entity) >> response
        
        and: "response is returned"
        result == response
    }

    // --- Validation ---
    
    def "should throw validation exception when file type not allowed"() {
        given: "a request with invalid file type"
        def request = anUploadRequest().type(DocumentType.EXECUTABLE).build()
        
        when: "uploading the document"
        service.upload(request, "user-1")
        
        then: "validation exception is thrown"
        def ex = thrown(DocumentValidationException)
        ex.message.contains("type not allowed")
    }

    // --- Error Handling ---
    
    def "should not publish event when repository save fails"() {
        given: "repository will throw"
        mapper.toEntity(_) >> aDocument().build()
        repository.save(_) >> { throw new DataAccessException("DB down") {} }
        
        when: "uploading the document"
        service.upload(anUploadRequest().build(), "user-1")
        
        then: "exception propagates and no event published"
        thrown(DataAccessException)
        0 * eventPublisher.publishUploaded(_)
    }

    // --- Data Driven ---
    
    def "should reject documents exceeding size limit"() {
        given: "a request with excessive file size"
        def request = anUploadRequest().size(fileSize).build()
        mapper.toEntity(_) >> aDocument().build()
        
        when: "uploading the document"
        service.upload(request, "user-1")
        
        then: "validation exception thrown"
        thrown(DocumentValidationException)
        
        where: "various oversized files"
        fileSize << [50_000_001, 100_000_000, Long.MAX_VALUE]
    }
}
```

### Integration Spec (Controller)

```groovy
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
class DocumentControllerIT extends Specification {

    @LocalServerPort int port
    @Autowired DocumentRepository repository
    
    @Shared @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine")

    def setup() {
        repository.deleteAll()
    }

    def "should upload document and return 201"() {
        given: "a valid upload request"
        def request = [filename: "report.pdf", size: 1024, type: "PDF"]
        
        when: "POST /api/v1/documents"
        def response = given()
                .port(port)
                .contentType(JSON)
                .body(request)
            .when()
                .post("/api/v1/documents")
        
        then: "201 created with document id"
        response.statusCode() == 201
        response.jsonPath().getString("id") != null
        response.jsonPath().getString("status") == "PENDING"
    }

    def "should return 400 when #field is invalid"() {
        when: "POST with invalid request"
        def response = given()
                .port(port)
                .contentType(JSON)
                .body(request)
            .when()
                .post("/api/v1/documents")
        
        then: "400 with validation error"
        response.statusCode() == 400
        response.jsonPath().getString("code") == "VALIDATION_ERROR"
        
        where: "various invalid inputs"
        field      | request
        "filename" | [size: 1024, type: "PDF"]
        "size"     | [filename: "test.pdf", size: -1, type: "PDF"]
        "type"     | [filename: "test.pdf", size: 1024]
    }
}
```

---

## Coverage Verification

```yaml
coverage_check:
  after_generation:
    verify:
      - Every public service method has a unit spec
      - Every API endpoint has an integration test
      - Every validation rule has a test
      - Every error path has a test
      - Every Spock spec has given/when/then blocks
    
    gaps_to_flag:
      - Service method without spec → "Missing unit test for {method}"
      - Endpoint without IT → "Missing integration test for {endpoint}"
      - Catch block without test → "Error path untested"
      - If/else branch without test → "Conditional path untested"
    
    report:
      total_scenarios: 28
      covered: 26
      gaps: 2
      gaps_detail:
        - "DocumentService.delete — no test for deleting already-deleted document"
        - "DocumentController GET — no test for invalid UUID format in path"
```

---

## Generation Rules

```yaml
rules:
  never:
    - Generate tests without understanding what the code does
    - Generate trivial tests (testing getters/setters)
    - Generate tests that depend on implementation details
    - Generate tests with complex logic (if/for in tests)
    - Generate tests without clear given/when/then structure
  
  always:
    - Generate from behavior perspective (what should happen)
    - Use descriptive test names (readable as documentation)
    - Use fixtures for test data (not inline construction)
    - Use Spock's where: blocks for data-driven scenarios
    - Verify both positive and negative paths
    - Include edge cases (null, empty, boundary values)
```
