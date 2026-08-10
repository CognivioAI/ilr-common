# API Design Module

> Architecture Agent — API Design Discipline

---

## Purpose

The Architecture Agent designs API contracts before implementation begins.
The API contract is the interface agreement between frontend, backend, and
external consumers.

---

## Process

1. Identify resources from domain model
2. Define endpoints (URL, method, status codes)
3. Define request/response schemas (DTOs)
4. Define error response format
5. Define pagination model
6. Define authentication/authorization requirements per endpoint
7. Document in OpenAPI 3.0

---

## URL Design Rules

```
/api/v{version}/{resource}
/api/v{version}/{resource}/{id}
/api/v{version}/{resource}/{id}/{sub-resource}
```

| Rule | Example |
|------|---------|
| Plural nouns | `/api/v1/documents` |
| Lowercase, hyphenated | `/api/v1/processing-jobs` |
| IDs in path | `/api/v1/documents/{documentId}` |
| Sub-resources for ownership | `/api/v1/applications/{appId}/documents` |
| Query params for filtering | `/api/v1/documents?status=PENDING&type=PASSPORT` |
| Never verbs | ~~`/api/v1/uploadDocument`~~ |
| Version from day one | `/api/v1/` |

---

## HTTP Methods

| Operation | Method | Path | Status | Body |
|-----------|--------|------|--------|------|
| List (paginated) | GET | `/resources` | 200 | Page |
| Get one | GET | `/resources/{id}` | 200 | Resource |
| Create | POST | `/resources` | 201 | Created resource |
| Full replace | PUT | `/resources/{id}` | 200 | Updated resource |
| Partial update | PATCH | `/resources/{id}` | 200 | Updated resource |
| Delete | DELETE | `/resources/{id}` | 204 | Empty |
| Long-running action | POST | `/resources/{id}/actions/process` | 202 | Job status |

---

## Pagination

All list endpoints are paginated:

```json
{
  "content": [...],
  "page": {
    "number": 0,
    "size": 20,
    "totalElements": 142,
    "totalPages": 8
  }
}
```

---

## Error Response Format

```json
{
  "timestamp": "2024-01-15T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "details": [
    { "field": "email", "message": "must be a valid email address" }
  ],
  "traceId": "abc-123-def"
}
```

---

## Security Annotations Per Endpoint

For every endpoint, document:
- Who can call it (roles)
- Whether it contains PII in request/response
- Rate limiting requirements

```yaml
/api/v1/documents:
  post:
    security: [bearer_auth]
    x-roles: [USER, ADMIN]
    x-rate-limit: 30/minute
    x-contains-pii: true
```

---

## OpenAPI Output

The Architecture Agent produces a complete OpenAPI 3.0 spec:

```yaml
openapi: 3.0.3
info:
  title: Document Service API
  version: 1.0.0
paths:
  /api/v1/documents:
    get:
      summary: List documents with pagination
      parameters:
        - name: page
          in: query
          schema: { type: integer, default: 0 }
        - name: size
          in: query
          schema: { type: integer, default: 20 }
      responses:
        '200':
          description: Paginated list of documents
    post:
      summary: Upload a new document
      requestBody:
        required: true
        content:
          multipart/form-data:
            schema:
              properties:
                file: { type: string, format: binary }
                applicationId: { type: string }
                documentType: { type: string, enum: [PASSPORT, BANK_STATEMENT, PAYSLIP] }
      responses:
        '201':
          description: Document uploaded, processing started
        '400':
          description: Validation error
```

---

## Checklist

- [ ] Resources identified from domain model
- [ ] URL pattern follows conventions
- [ ] All endpoints documented (OpenAPI 3.0)
- [ ] Request/response schemas defined
- [ ] Error format standardized
- [ ] Pagination on all list endpoints
- [ ] Auth/roles per endpoint
- [ ] Rate limiting considered
- [ ] PII identified in payloads
- [ ] Versioning strategy in place
