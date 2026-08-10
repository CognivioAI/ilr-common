# Database Change Safety

## Purpose

Ensure every database migration is safe, backward-compatible, and rollback-ready. The Developer Agent validates migrations before they reach production.

---

## Safety Rules

### Golden Rule

```
Old code must still work with new schema.
New code must still work with old schema.
Both versions run simultaneously during rolling deployment.
```

---

## Safe vs Unsafe Operations

### Always Safe

| Operation | Why Safe | Example |
|-----------|----------|---------|
| Add table | No existing code references it | CREATE TABLE classifications (...) |
| Add nullable column | Old code ignores it | ALTER TABLE docs ADD COLUMN expires_at TIMESTAMP |
| Add column with default | Old code ignores it | ALTER TABLE docs ADD COLUMN version INT DEFAULT 1 |
| Add index | Doesn't change data or queries | CREATE INDEX idx_docs_status ON documents(status) |
| Add constraint (CHECK) | Only validates future writes | ALTER TABLE docs ADD CONSTRAINT chk_size CHECK (size > 0) |

### Requires Expand/Contract

| Operation | Danger | Safe Approach |
|-----------|--------|--------------|
| Add NOT NULL column | Fails on existing rows | Add nullable → backfill → add NOT NULL |
| Rename column | Old code uses old name | Add new → copy data → deploy code using new → drop old |
| Change column type | Incompatible reads | Add new type column → migrate → switch code → drop old |
| Remove column | Old code may read it | Deploy code that doesn't read it → then drop column |

### Never in Single Release

| Operation | Why Dangerous | Alternative |
|-----------|--------------|-------------|
| DROP TABLE | Data loss | Soft delete, rename first, drop after verification |
| DROP COLUMN (with code change) | Rolling deploy reads from dropped column | Separate releases |
| ALTER COLUMN TYPE directly | May fail on existing data | Expand/contract |
| TRUNCATE | Data loss | Archive first if needed |

---

## Expand/Contract Pattern

### Example: Add Required Column

```yaml
expand_contract:
  goal: "Add NOT NULL 'document_type' column to existing documents table"
  
  step_1_expand:
    release: "v1.3.0"
    migration: |
      -- V8__add_document_type_column.sql
      ALTER TABLE documents ADD COLUMN document_type VARCHAR(50);
      -- Nullable initially (old rows have NULL)
    code_change: "Write document_type on new inserts (read tolerates NULL)"
    backward_compatible: true
    
  step_2_backfill:
    release: "v1.3.1 (or same release, separate migration)"
    migration: |
      -- V9__backfill_document_type.sql
      UPDATE documents SET document_type = 'UNKNOWN' WHERE document_type IS NULL;
    note: "Run in batches for large tables to avoid locking"
    
  step_3_contract:
    release: "v1.4.0 (after verifying backfill complete)"
    migration: |
      -- V10__enforce_document_type_not_null.sql
      ALTER TABLE documents ALTER COLUMN document_type SET NOT NULL;
    prerequisite: "All rows have non-NULL values, all code writes this field"
```

### Example: Rename Column

```yaml
rename_column:
  goal: "Rename 'created_date' to 'created_at'"
  
  step_1: "Add new column 'created_at'"
  step_2: "Copy data: UPDATE documents SET created_at = created_date"
  step_3: "Deploy code that writes BOTH columns, reads from 'created_at'"
  step_4: "Verify no code reads 'created_date'"
  step_5: "Drop old column (separate release)"
  
  total_releases: 2-3 (spread over days, not same deploy)
```

---

## Pre-Migration Checklist

```yaml
before_writing_migration:
  ✓ Is the change backward-compatible with current running code?
  ✓ What happens to existing data? (NULL handling, defaults)
  ✓ Does it lock the table? (ALTER TABLE on large tables can lock)
  ✓ Can it be rolled back? (What's the rollback migration?)
  ✓ Is there an index needed for the new column/query?
  ✓ Does it affect any existing queries? (performance impact)
  ✓ Is the migration idempotent? (can run twice safely)
  
after_writing_migration:
  ✓ Run against copy of production schema (test locally with real-ish data)
  ✓ Verify Flyway checksum doesn't conflict
  ✓ Verify old code + new schema works (backward compatible)
  ✓ Verify new code + old schema works (forward compatible)
  ✓ Estimate execution time on production data volume
```

---

## Large Table Migration Safety

```yaml
large_table_migration:
  threshold: "> 1 million rows"
  
  concerns:
    - ALTER TABLE may lock entire table
    - UPDATE all rows may take minutes/hours
    - May cause connection pool exhaustion
  
  safe_approaches:
    add_column: "Usually instant in PostgreSQL (no rewrite for nullable)"
    backfill:
      method: "Batch update in chunks"
      example: |
        -- Backfill in batches of 10000
        UPDATE documents
        SET document_type = 'UNKNOWN'
        WHERE document_type IS NULL
        AND id IN (SELECT id FROM documents WHERE document_type IS NULL LIMIT 10000);
      repeat: "Until no more NULLs"
    
    add_index:
      method: "CREATE INDEX CONCURRENTLY (PostgreSQL — no lock)"
      example: "CREATE INDEX CONCURRENTLY idx_docs_type ON documents(document_type)"
      note: "Cannot run inside transaction (Flyway: use separate migration file)"
    
    column_type_change:
      method: "Never ALTER directly — use expand/contract"
```

---

## Rollback Strategy

```yaml
rollback:
  for_additive_changes:
    method: "Leave in place (nullable columns don't hurt)"
    reason: "Rolling back code is enough, schema can stay"
  
  for_destructive_changes:
    method: "Never do destructive changes without separate rollback plan"
    plan:
      drop_column: "Cannot un-drop — ensure backup/snapshot before"
      change_type: "Cannot un-change — use expand/contract instead"
  
  rollback_migration_file:
    when: "Team requires formal rollback scripts"
    naming: "U8__undo_add_document_type.sql (Flyway undo naming)"
    content: "Reverse of the forward migration"
```

---

## Migration Naming Convention

```yaml
naming:
  format: "V{version}__{description}.sql"
  version: "Sequential integer (V1, V2, V3...)"
  description: "snake_case describing the change"
  
  examples:
    - V1__create_document_table.sql
    - V2__create_classification_table.sql
    - V3__add_document_type_column.sql
    - V4__add_status_index.sql
    - V5__backfill_document_type.sql
    - V6__enforce_document_type_not_null.sql
  
  rules:
    - Never modify an existing migration (breaks Flyway checksum)
    - One logical change per migration file
    - Include comment at top explaining purpose and safety
```

---

## Migration Template

```sql
-- V8__add_document_type_column.sql
-- Purpose: Add document type for AI classification
-- Safety: Nullable column, existing rows get NULL (handled by application)
-- Backward compatible: Yes (old code ignores this column)
-- Rollback: Column can remain (nullable, no impact)
-- Estimated time: Instant (PostgreSQL ADD COLUMN is metadata-only for nullable)

ALTER TABLE documents
    ADD COLUMN document_type VARCHAR(50);

COMMENT ON COLUMN documents.document_type IS 'AI-assigned document classification type';

-- Index for filtering by type
CREATE INDEX idx_documents_document_type ON documents(document_type);
```
