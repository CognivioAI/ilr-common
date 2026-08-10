# Test Strategy

## Purpose

Testing is not an afterthought — it is integral to development. Every feature ships with tests. The Developer Agent generates tests alongside implementation, not after.

---

## Testing Pyramid

```
        ╱╲
       ╱  ╲         E2E Tests (few)
      ╱    ╲        - Critical user flows
     ╱──────╲       - Playwright/Cypress
    ╱        ╲
   ╱          ╲     Integration Tests (some)
  ╱            ╲    - API endpoints (MockMvc/RestAssured)
 ╱              ╲   - Repository (Testcontainers)
╱────────────────╲  - Event listeners
╱                  ╲
╱                    ╲  Unit Tests (many)
╱                      ╲ - Service logic
╱                        ╲- Mappers, validators
╱──────────────────────────╲- Pure functions
```

---

## What to Test Where

| Layer | Test Type | Framework | Mocks? | DB? |
|-------|-----------|-----------|--------|-----|
| Service (business logic) | Unit | Spock + Mockito | Yes | No |
| Mapper | Unit | Spock | No | No |
| Validator | Unit | Spock | No | No |
| Repository | Integration | Spock + Testcontainers | No | Real |
| Controller | Integration | Spock + MockMvc | Service mocked | No |
| Full API | Integration | Spock + REST Assured | No mocks | Real |
| Event listener | Integration | Spock + LocalStack | No | Real |
| User flow | E2E | Playwright | Nothing | Real |

---

## Coverage Targets

```yaml
coverage:
  new_code: "> 80% line coverage (enforced in CI)"
  overall: "> 70% line coverage (tracked, not blocked)"
  
  per_layer:
    service: "> 90% (most critical — business logic)"
    controller: "> 80% (including error paths)"
    repository: "> 70% (custom queries tested, CRUD via integration)"
    mapper: "100% (simple, pure functions)"
  
  what_not_to_cover:
    - Configuration classes (Spring wiring)
    - DTOs (data holders)
    - Generated code (MapStruct, Lombok)
    - Framework glue (main application class)
```

---

## Unit Testing Standards

### Structure (Spock BDD)

```groovy
class DocumentServiceSpec extends Specification {

    DocumentRepository repository = Mock()
    DocumentMapper mapper = Mock()
    DocumentEventPublisher eventPublisher = Mock()
    
    DocumentService service = new DocumentService(repository, mapper, eventPublisher)

    def "should save document when valid upload request"() {
        given: "a valid upload request"
        def request = UploadDocumentRequest.builder()
                .filename("report.pdf")
                .size(1024L)
                .build()
        def entity = Document.builder().id(UUID.randomUUID()).build()
        def response = DocumentResponse.builder().id(entity.id).build()

        when: "the document is uploaded"
        def result = service.upload(request, "user-1")

        then: "the document is saved and event published"
        1 * mapper.toEntity(request) >> entity
        1 * repository.save(entity) >> entity
        1 * mapper.toResponse(entity) >> response
        1 * eventPublisher.publishUploaded(entity)
        
        and: "the response contains the document id"
        result.id == entity.id
    }

    def "should throw not found when document does not exist"() {
        given: "a non-existent document id"
        def id = UUID.randomUUID()
        repository.findById(id) >> Optional.empty()

        when: "retrieving the document"
        service.getById(id)

        then: "a not found exception is thrown"
        def ex = thrown(DocumentNotFoundException)
        ex.message.contains(id.toString())
    }
}
```

### Rules

- BDD style: `given` / `when` / `then` blocks (always)
- Block labels describe intent in plain English
- Test names are readable sentences: `"should {result} when {condition}"`
- Use Spock `Mock()` for interactions, `Stub()` for returns-only
- Use `where:` blocks for data-driven tests
- No logic in tests (no if/loops/try-catch)
- Use Spock's built-in power assertions (no separate assertion library needed)

---

## Integration Testing Standards

### Repository Tests (Spock + Testcontainers)

```groovy
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = NONE)
class DocumentRepositoryIT extends Specification {

    @Shared
    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine")

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl)
        registry.add("spring.datasource.username", postgres::getUsername)
        registry.add("spring.datasource.password", postgres::getPassword)
    }

    @Autowired
    DocumentRepository repository

    def "should persist and retrieve document"() {
        given: "a new document"
        def document = Document.builder()
                .filename("test.pdf")
                .status(DocumentStatus.PENDING)
                .uploadedBy("user-1")
                .build()

        when: "the document is saved and retrieved"
        def saved = repository.save(document)
        def found = repository.findById(saved.id)

        then: "the document is found with correct data"
        found.isPresent()
        found.get().filename == "test.pdf"
        found.get().uploadedBy == "user-1"
    }
}
```

### Controller Tests (Spock + MockMvc)

```groovy
@WebMvcTest(DocumentController)
class DocumentControllerIT extends Specification {

    @Autowired
    MockMvc mockMvc

    @MockBean
    DocumentService documentService

    def "should return 201 when document uploaded"() {
        given: "the service will return a document response"
        def response = DocumentResponse.builder()
                .id(UUID.randomUUID())
                .status("PENDING")
                .build()
        documentService.upload(_, _) >> response

        when: "a valid upload request is made"
        def result = mockMvc.perform(post("/api/v1/documents")
                .contentType(APPLICATION_JSON)
                .content('{"filename": "test.pdf", "size": 1024}'))

        then: "a 201 response is returned with document id"
        result.andExpect(status().isCreated())
        result.andExpect(jsonPath('$.id').exists())
        result.andExpect(jsonPath('$.status').value("PENDING"))
    }

    def "should return 400 when filename missing"() {
        when: "a request without filename is made"
        def result = mockMvc.perform(post("/api/v1/documents")
                .contentType(APPLICATION_JSON)
                .content('{"size": 1024}'))

        then: "a 400 validation error is returned"
        result.andExpect(status().isBadRequest())
        result.andExpect(jsonPath('$.code').value("VALIDATION_ERROR"))
    }

    def "should return 404 when document not found"() {
        given: "the service throws not found"
        def id = UUID.randomUUID()
        documentService.getById(id) >> { throw new DocumentNotFoundException(id) }

        when: "requesting a non-existent document"
        def result = mockMvc.perform(get("/api/v1/documents/{id}", id))

        then: "a 404 response is returned"
        result.andExpect(status().isNotFound())
        result.andExpect(jsonPath('$.code').value("DOCUMENT_NOT_FOUND"))
    }
}
```

### Full API Tests (Spock + REST Assured)

```groovy
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
class DocumentUploadFlowIT extends Specification {

    @LocalServerPort
    int port

    @Shared
    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine")

    def "should upload document and retrieve it"() {
        given: "a valid upload request"
        def request = [filename: "report.pdf", size: 2048]

        when: "the document is uploaded"
        def uploadResponse = given()
                .port(port)
                .contentType(JSON)
                .body(request)
            .when()
                .post("/api/v1/documents")
            .then()
                .statusCode(201)
                .extract().response()

        and: "the document is retrieved by id"
        def documentId = uploadResponse.jsonPath().getString("id")
        def getResponse = given()
                .port(port)
            .when()
                .get("/api/v1/documents/{id}", documentId)
            .then()
                .statusCode(200)
                .extract().response()

        then: "the document data matches"
        getResponse.jsonPath().getString("filename") == "report.pdf"
        getResponse.jsonPath().getString("status") == "PENDING"
    }
}
```

---

## Test Data Management

### Fixture Builders

```groovy
class DocumentFixture {
    
    static Document.DocumentBuilder aDocument() {
        Document.builder()
                .id(UUID.randomUUID())
                .filename("test-document.pdf")
                .size(1024L)
                .status(DocumentStatus.PENDING)
                .uploadedBy("test-user")
                .createdAt(Instant.now())
    }
    
    static UploadDocumentRequest.UploadDocumentRequestBuilder anUploadRequest() {
        UploadDocumentRequest.builder()
                .filename("test-document.pdf")
                .size(1024L)
    }
}
```

### Rules

- Use builder pattern for test data
- Provide sensible defaults (override only what matters for the test)
- Never use production data in tests
- Each test creates its own data (no shared mutable state between tests)
- Clean up: use `@Transactional` (auto-rollback) or Spock's `cleanup:` block
- Use Spock `where:` blocks for data-driven testing:

```groovy
def "should reject files exceeding size limit"() {
    when: "uploading a file of given size"
    service.upload(anUploadRequest().size(fileSize).build(), "user-1")

    then: "validation exception is thrown"
    thrown(DocumentValidationException)

    where: "file sizes exceed the limit"
    fileSize << [51_000_000, 100_000_000, Long.MAX_VALUE]
}
```

---

## What Must Be Tested (Checklist)

```yaml
mandatory_test_cases:
  happy_path:
    - "Valid input produces expected output"
  
  validation:
    - "Missing required field returns 400"
    - "Invalid format returns 400"
    - "Exceeds limit returns 400"
  
  not_found:
    - "Non-existent resource returns 404"
  
  authorization:
    - "Unauthenticated request returns 401"
    - "Unauthorized request returns 403"
  
  edge_cases:
    - "Empty collection returns empty list (not null)"
    - "Boundary values (max size, empty string)"
    - "Concurrent operations (if relevant)"
  
  error_paths:
    - "Database unavailable → graceful error"
    - "External service timeout → appropriate response"
    - "Invalid state transition → domain exception"
```
