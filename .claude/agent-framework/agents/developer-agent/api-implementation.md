# API Implementation

## Purpose

Define REST API implementation standards — OpenAPI-first approach, validation, error contracts, pagination, and versioning.

---

## OpenAPI-First Approach

```yaml
workflow:
  1. Architecture Agent defines API contract (OpenAPI 3.0)
  2. Developer Agent implements to match contract exactly
  3. Integration tests verify contract compliance
  4. API documentation auto-generated from code annotations

principle: "The OpenAPI spec is the source of truth. Code must match it."
```

---

## Controller Implementation

### Standard CRUD Controller

```java
@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
@Validated
@Tag(name = "Documents", description = "Document management API")
@Slf4j
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Upload a document")
    @ApiResponse(responseCode = "201", description = "Document created")
    @ApiResponse(responseCode = "400", description = "Validation error")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    public DocumentResponse upload(
            @Valid @RequestBody UploadDocumentRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return documentService.upload(request, user.getUsername());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get document by ID")
    @ApiResponse(responseCode = "200", description = "Document found")
    @ApiResponse(responseCode = "404", description = "Document not found")
    public DocumentResponse getById(@PathVariable UUID id) {
        return documentService.getById(id);
    }

    @GetMapping
    @Operation(summary = "List documents with pagination")
    public Page<DocumentResponse> list(
            @RequestParam(required = false) DocumentStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = DESC) Pageable pageable) {
        return documentService.list(status, pageable);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a document")
    @ApiResponse(responseCode = "204", description = "Document deleted")
    @ApiResponse(responseCode = "404", description = "Document not found")
    public void delete(@PathVariable UUID id) {
        documentService.delete(id);
    }
}
```

---

## Request Validation

### DTO with Bean Validation

```java
public record UploadDocumentRequest(
        @NotBlank(message = "Filename is required")
        @Size(max = 255, message = "Filename must be less than 255 characters")
        String filename,

        @NotNull(message = "Size is required")
        @Positive(message = "Size must be positive")
        @Max(value = 52_428_800, message = "File size must be less than 50MB")
        Long size,

        @NotNull(message = "Document type is required")
        DocumentType type
) {}
```

### Custom Validator (Complex Rules)

```java
@Constraint(validatedBy = FilenameValidator.class)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidFilename {
    String message() default "Invalid filename";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

public class FilenameValidator implements ConstraintValidator<ValidFilename, String> {
    private static final Pattern SAFE_FILENAME = Pattern.compile("^[a-zA-Z0-9._-]+$");
    
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value != null && SAFE_FILENAME.matcher(value).matches();
    }
}
```

---

## Response Standards

### Success Response

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "filename": "report.pdf",
  "size": 1024,
  "status": "PENDING",
  "type": "PDF",
  "uploadedBy": "user-123",
  "createdAt": "2026-01-15T10:30:00Z"
}
```

### Paginated Response

```json
{
  "content": [...],
  "page": {
    "size": 20,
    "number": 0,
    "totalElements": 142,
    "totalPages": 8
  }
}
```

### Error Response

```json
{
  "code": "DOCUMENT_NOT_FOUND",
  "message": "Document with id 550e8400-e29b-41d4-a716-446655440000 not found",
  "traceId": "abc123def456"
}
```

### Validation Error Response

```json
{
  "code": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "traceId": "abc123def456",
  "errors": [
    {"field": "filename", "message": "Filename is required"},
    {"field": "size", "message": "Size must be positive"}
  ]
}
```

---

## HTTP Status Codes

| Operation | Success | Client Error | Server Error |
|-----------|---------|-------------|-------------|
| Create | 201 Created | 400 Validation | 500 Internal |
| Read (found) | 200 OK | 404 Not Found | 500 Internal |
| Read (list) | 200 OK | 400 Bad params | 500 Internal |
| Update | 200 OK | 400/404 | 500 Internal |
| Delete | 204 No Content | 404 Not Found | 500 Internal |
| Async | 202 Accepted | 400 Validation | 500 Internal |

---

## API Versioning

```yaml
versioning:
  strategy: URL path prefix
  format: "/api/v{major}/{resource}"
  current: "/api/v1/documents"
  
  rules:
    - New major version only for breaking changes
    - Additive changes (new fields, endpoints) are NOT breaking
    - Removing fields IS breaking (new version)
    - Changing field types IS breaking (new version)
    - Old version supported for 6 months after new version released
```

---

## Pagination Standards

```yaml
pagination:
  parameters:
    page: "Page number (0-based)"
    size: "Page size (default 20, max 100)"
    sort: "Sort field and direction (e.g., createdAt,desc)"
  
  implementation: "Spring Data Pageable"
  
  max_page_size: 100
  default_page_size: 20
  default_sort: "createdAt,desc"
  
  controller:
    annotation: "@PageableDefault(size = 20, sort = \"createdAt\", direction = DESC)"
```

---

## Security Integration

```yaml
endpoint_security:
  public_endpoints:
    - GET /actuator/health
    - GET /actuator/info
    - GET /v3/api-docs/**
    - GET /swagger-ui/**
  
  authenticated_endpoints:
    - All /api/** require valid JWT
  
  authorized_endpoints:
    - POST /api/v1/documents: role UPLOAD
    - DELETE /api/v1/documents/{id}: role ADMIN
    - GET /api/v1/documents: role USER (own documents only)
  
  resource_ownership:
    rule: "Users can only access their own documents"
    implementation: "@PreAuthorize or service-level check"
```

---

## Idempotency

```yaml
idempotency:
  POST_operations:
    strategy: "Client-provided idempotency key"
    header: "Idempotency-Key"
    implementation: "Check if key already processed, return cached result"
    
  PUT_operations:
    naturally_idempotent: "Same input produces same result"
    
  DELETE_operations:
    naturally_idempotent: "Deleting twice returns 204 both times (or 404 second time)"
```
