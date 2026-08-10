# Database Design Module

> Architecture Agent — Database Design Discipline

---

## Purpose

Define how the Architecture Agent makes database and storage decisions.
Not everything belongs in PostgreSQL. The agent selects the right storage
for each data type and designs schemas with indexing, versioning, and audit.

---

## Storage Strategy

| Data Type | Storage | Why |
|-----------|---------|-----|
| Structured metadata | PostgreSQL | ACID, relationships, queries |
| Document files (PDF, images) | S3 | Binary, large, versioned |
| OCR output, AI summaries | S3 (JSON) | Large, append-only |
| User sessions | Redis | Fast, ephemeral |
| Full-text search | PostgreSQL FTS or OpenSearch | Text queries |
| Semantic search (AI) | pgvector or Pinecone | Vector similarity |
| Audit log | PostgreSQL (append-only) | Immutable, queryable |
| Event store | PostgreSQL or DynamoDB | Ordered, partitioned |

---

## ILR Example: Data Ownership

```
PostgreSQL (User Service):
  Users, Profiles, Roles

PostgreSQL (Document Service):
  Documents, DocumentMetadata, ProcessingJobs

PostgreSQL (Application Service):
  Applications, ChecklistItems

PostgreSQL (Audit Service):
  AuditLogs (append-only)

S3:
  /documents/{appId}/{docId}/original.pdf
  /documents/{appId}/{docId}/ocr-output.json
  /documents/{appId}/{docId}/ai-summary.json

pgvector:
  Immigration guidance embeddings
  Previous AI reasoning (knowledge base)
  Precedent cases

Redis:
  User sessions
  Processing job status (short-lived)
  Rate limit counters
```

---

## Schema Design Rules

### Always Include

| Column | Purpose |
|--------|---------|
| `id` (UUID) | Primary key |
| `created_at` | Audit: when created |
| `updated_at` | Audit: when last modified |
| `created_by` | Audit: who created |
| `version` | Optimistic locking |

### Entity Template

```sql
CREATE TABLE documents (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id UUID NOT NULL REFERENCES applications(id),
    document_type VARCHAR(50) NOT NULL,
    file_key    VARCHAR(500) NOT NULL,  -- S3 key
    status      VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    metadata    JSONB,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by  VARCHAR(36) NOT NULL,
    version     BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_documents_application_id ON documents(application_id);
CREATE INDEX idx_documents_status ON documents(status);
CREATE INDEX idx_documents_type_status ON documents(document_type, status);
```

---

## Decision: SQL vs NoSQL

| Choose PostgreSQL When | Choose DynamoDB When |
|------------------------|---------------------|
| Complex queries with joins | Simple key-value access patterns |
| ACID transactions required | Extreme scale (> 100k writes/sec) |
| Reporting and analytics | Single-table design possible |
| Relationships matter | Predictable access patterns only |
| Team knows SQL | Global tables needed |

**Default: PostgreSQL** (covers 90% of use cases for typical SaaS applications).

---

## Indexing Strategy

| Always Index | Why |
|-------------|-----|
| Foreign keys | JOIN performance |
| Status columns | Filtered queries |
| Columns in WHERE clauses | Query performance |
| Columns in ORDER BY | Sort performance |
| Composite: (type, status) | Multi-filter queries |

**Never:**
- Index columns with very low cardinality (boolean with 50/50 split)
- Over-index (each index slows writes)

---

## Versioning and History

### Option A: `version` Column (Optimistic Locking)

Use for concurrent update protection. Does not track history.

### Option B: Audit Log Table (Recommended for ILR)

```sql
CREATE TABLE audit_logs (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type VARCHAR(50) NOT NULL,
    entity_id   UUID NOT NULL,
    action      VARCHAR(20) NOT NULL,  -- CREATE, UPDATE, DELETE
    actor       VARCHAR(36) NOT NULL,
    changes     JSONB NOT NULL,        -- before/after diff
    timestamp   TIMESTAMP NOT NULL DEFAULT NOW()
);
```

### Option C: Event Sourcing (for highly auditable domains)

Use only when you need a full, immutable history of every state change
and can derive current state from events.

---

## Data Retention

| Data | Retention | Action |
|------|-----------|--------|
| Active applications | Indefinite | — |
| Completed applications | 7 years | Legal requirement |
| Audit logs | 10 years | Compliance |
| S3 originals | Same as application | Lifecycle policy |
| Processing job logs | 90 days | Archive to S3 Glacier |
| User sessions | 30 minutes | Redis TTL |

---

## Checklist

- [ ] Storage type chosen per data type (SQL, S3, Redis, vector)
- [ ] Each service owns its database
- [ ] UUID primary keys
- [ ] Timestamps + created_by on all tables
- [ ] Optimistic locking (`version`)
- [ ] Indexes on FK, status, and query columns
- [ ] Audit logging strategy defined
- [ ] Data retention policies documented
- [ ] Migrations planned (Flyway)
- [ ] PII columns identified and classified
