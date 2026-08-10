# API Contract Management

## Purpose

Manage API contracts as first-class artifacts — detect breaking changes, enforce backward compatibility, manage versioning, and coordinate consumer/provider evolution.

---

## API Lifecycle

```
Architecture Agent defines contract (OpenAPI)
    │
    ▼
Developer Agent implements contract
    │
    ▼
Contract validated in CI (automated)
    │
    ▼
Contract published (API docs)
    │
    ▼
Consumers integrate
    │
    ▼
Contract evolution (add fields, new endpoints)
    │
    ▼
Breaking change? → Version bump (v1 → v2)
```

---

## Breaking Change Detection

### What is Breaking

| Change | Breaking? | Action |
|--------|-----------|--------|
| Add optional field to response | ❌ No | Safe to deploy |
| Add optional field to request | ❌ No | Safe to deploy |
| Add new endpoint | ❌ No | Safe to deploy |
| Remove field from response | ✅ Yes | New API version |
| Remove endpoint | ✅ Yes | Deprecate first, remove later |
| Rename field | ✅ Yes | Add new + deprecate old |
| Change field type | ✅ Yes | New API version |
| Make optional field required | ✅ Yes | New API version |
| Change response status code | ✅ Yes | New API version |
| Change URL path | ✅ Yes | New API version |
| Change authentication method | ✅ Yes | Migration plan needed |

### Automated Detection

```yaml
breaking_change_detection:
  method: "Compare current OpenAPI spec with previous version"
  tool: "openapi-diff or oasdiff"
  
  ci_integration:
    trigger: "PR modifies API classes (controller, DTO, OpenAPI annotations)"
    action: "Generate OpenAPI spec from code, compare with main branch"
    
    if_breaking:
      block: true
      message: |
        ⚠️ Breaking API change detected:
        - Removed field: DocumentResponse.createdDate
        - Consumers affected: web-frontend, admin-dashboard
        
        Options:
        1. Add field back (preserve compatibility)
        2. Create v2 endpoint (if change is intentional)
        3. Coordinate with consumers first
    
    if_non_breaking:
      block: false
      message: "✅ API changes are backward-compatible"
```

---

## Versioning Strategy

```yaml
versioning:
  approach: URL path versioning
  current: /api/v1/{resource}
  
  when_to_version:
    - Removing fields from response
    - Changing field types
    - Changing resource structure significantly
    - Changing authentication/authorization model
  
  migration_process:
    1. Create v2 endpoint alongside v1
    2. Document migration guide for consumers
    3. Notify all consumers (give 30 days minimum)
    4. Monitor v1 usage (track which consumers still use it)
    5. Deprecate v1 (add deprecation header)
    6. Remove v1 after all consumers migrated (minimum 6 months)
  
  both_versions_running:
    implementation: "Separate controller methods, shared service layer"
    example:
      v1: "@GetMapping('/api/v1/documents/{id}')"
      v2: "@GetMapping('/api/v2/documents/{id}')"
      shared: "DocumentService handles both (maps to appropriate response)"
```

---

## Consumer-Driven Contracts (Pact)

### Provider Side (Your Service)

```groovy
// Pact provider verification test
@Provider("document-service")
@PactBroker(url = "https://pact-broker.ilr.internal")
class DocumentServicePactVerificationIT extends Specification {

    @TestTarget
    HttpTarget target = new HttpTarget("localhost", port)

    @State("document exists with id abc-123")
    void setupDocumentExists() {
        documentRepository.save(Document.builder()
                .id(UUID.fromString("abc-123"))
                .filename("test.pdf")
                .status(DocumentStatus.ACTIVE)
                .build())
    }
}
```

### Consumer Side (Frontend)

```typescript
// Pact consumer test (frontend defines expectations)
describe('Document API', () => {
  it('should return document by ID', async () => {
    provider.addInteraction({
      state: 'document exists with id abc-123',
      uponReceiving: 'a request for document abc-123',
      withRequest: { method: 'GET', path: '/api/v1/documents/abc-123' },
      willRespondWith: {
        status: 200,
        body: { id: 'abc-123', filename: like('test.pdf'), status: like('ACTIVE') }
      }
    });
  });
});
```

---

## Contract Evolution Patterns

### Safe: Add Optional Response Field

```yaml
safe_evolution:
  before:
    DocumentResponse: { id, filename, status, createdAt }
  
  after:
    DocumentResponse: { id, filename, status, createdAt, expiresAt }
  
  why_safe: "Existing consumers ignore unknown fields (should by contract)"
  action: "Deploy without coordination"
```

### Safe: Add New Endpoint

```yaml
safe_evolution:
  before:
    endpoints: [GET /documents, GET /documents/{id}, POST /documents]
  
  after:
    endpoints: [GET /documents, GET /documents/{id}, POST /documents, GET /documents/expired]
  
  why_safe: "New endpoint doesn't affect existing consumers"
  action: "Deploy, then notify consumers of new capability"
```

### Unsafe: Remove Response Field

```yaml
unsafe_evolution:
  before:
    DocumentResponse: { id, filename, status, createdAt, createdDate }
  
  desired:
    DocumentResponse: { id, filename, status, createdAt }  # removed createdDate
  
  why_breaking: "Consumers may rely on createdDate"
  
  migration_plan:
    step_1: "Add @Deprecated to createdDate in code"
    step_2: "Add Deprecation header to response"
    step_3: "Notify consumers, provide migration guide"
    step_4: "Monitor usage of deprecated field"
    step_5: "Remove after all consumers migrated"
    timeline: "Minimum 1 month between deprecation and removal"
```

---

## OpenAPI as Source of Truth

```yaml
openapi_management:
  generation: "From code annotations (Springdoc OpenAPI)"
  publication: "Auto-generated on build, served at /v3/api-docs"
  validation: "CI compares spec before/after to detect breaking changes"
  
  annotations:
    controller: "@Tag, @Operation, @ApiResponse"
    dto: "@Schema (for field descriptions)"
    
  rules:
    - Every endpoint has @Operation with summary
    - Every response has @ApiResponse with description
    - Every DTO field has @Schema with description
    - Error responses documented for each endpoint
```

---

## Contract Testing in CI

```yaml
ci_pipeline:
  steps:
    1. Build application
    2. Generate OpenAPI spec (from code)
    3. Compare with previous spec (breaking change detection)
    4. Run Pact provider verification (consumer contracts still satisfied?)
    5. If breaking → block merge, notify consumers
    6. If compatible → proceed
    
  tools:
    spec_diff: "oasdiff (OpenAPI diff tool)"
    contract_testing: "Pact (consumer-driven contracts)"
    spec_publication: "Swagger UI + Pact Broker"
```
