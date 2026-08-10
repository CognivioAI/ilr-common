# Persistence Patterns

## Purpose

Define database access patterns — JPA usage, repository design, migration strategy, query optimization, and transaction management.

---

## Repository Design

### Spring Data JPA Repository

```java
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    // Simple derived query
    Optional<Document> findByIdAndDeletedFalse(UUID id);
    
    // Pagination
    Page<Document> findAllByStatusAndDeletedFalse(DocumentStatus status, Pageable pageable);
    
    // Custom JPQL query
    @Query("SELECT d FROM Document d WHERE d.uploadedBy = :userId AND d.createdAt > :since")
    List<Document> findRecentByUser(@Param("userId") String userId, @Param("since") Instant since);
    
    // Projection (return only needed fields)
    @Query("SELECT new com.ilr.document.dto.response.DocumentSummary(d.id, d.filename, d.status) FROM Document d")
    Page<DocumentSummary> findSummaries(Pageable pageable);
    
    // Existence check (cheaper than loading entity)
    boolean existsByFilenameAndUploadedBy(String filename, String uploadedBy);
    
    // Batch update
    @Modifying
    @Query("UPDATE Document d SET d.status = :status WHERE d.id IN :ids")
    int updateStatusByIds(@Param("status") DocumentStatus status, @Param("ids") List<UUID> ids);
}
```

---

## Migration Strategy (Flyway)

### File Naming

```
db/migration/
├── V1__create_document_table.sql
├── V2__create_classification_table.sql
├── V3__add_document_type_column.sql
├── V4__add_status_index.sql
└── V5__add_audit_columns.sql
```

### Safe Migration Rules

```yaml
migration_rules:
  always_safe:
    - Add new table
    - Add nullable column
    - Add index
    - Add column with default value
    
  requires_expand_contract:
    - Rename column (add new → migrate data → drop old)
    - Change column type (add new → migrate → drop old)
    - Add NOT NULL (add nullable → backfill → add constraint)
    - Remove column (ensure no code reads it first)
    
  never_in_production:
    - DROP TABLE (unless absolutely certain)
    - DROP COLUMN in same release as code change
    - ALTER COLUMN TYPE directly
    - TRUNCATE

  backward_compatible:
    rule: "Old code must still work with new schema"
    reason: "Rolling deployment means both versions run simultaneously"
```

### Migration Template

```sql
-- V3__add_document_type_column.sql
-- Purpose: Add document type for classification
-- Backward compatible: Yes (nullable, old code ignores it)

ALTER TABLE documents
    ADD COLUMN document_type VARCHAR(50);

COMMENT ON COLUMN documents.document_type IS 'Document classification type (PDF, INVOICE, CONTRACT)';

-- Index for type-based queries
CREATE INDEX idx_documents_type ON documents(document_type);
```

---

## Transaction Management

```yaml
transaction_rules:
  service_layer: "Transaction boundary is at the service method level"
  read_only: "Use @Transactional(readOnly = true) for queries (performance)"
  write: "Use @Transactional for mutations"
  propagation: "Default REQUIRED (join existing or create new)"
  isolation: "Default READ_COMMITTED"
  
  never:
    - Transactions in controllers
    - Transactions in repositories (Spring manages these)
    - Long-running transactions (keep short)
    - Transactions spanning external calls (HTTP, SQS)
```

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)  // Default: read-only for the class
public class DocumentService {

    @Transactional  // Override: write transaction for mutations
    public DocumentResponse upload(UploadDocumentRequest request, String user) {
        // DB write + event publish within single transaction
        var saved = repository.save(entity);
        eventPublisher.publishUploaded(saved);  // Transactional outbox recommended
        return mapper.toResponse(saved);
    }

    // Inherits class-level readOnly = true
    public DocumentResponse getById(UUID id) {
        return repository.findById(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new DocumentNotFoundException(id));
    }
}
```

---

## Query Optimization

### N+1 Prevention

```java
// ❌ N+1 problem: loads documents, then queries classifications per document
var documents = documentRepository.findAll();
documents.forEach(doc -> doc.getClassifications().size()); // Triggers N queries

// ✅ Fix: JOIN FETCH
@Query("SELECT d FROM Document d LEFT JOIN FETCH d.classifications WHERE d.status = :status")
List<Document> findWithClassifications(@Param("status") DocumentStatus status);

// ✅ Fix: @EntityGraph
@EntityGraph(attributePaths = {"classifications"})
Page<Document> findAllByStatus(DocumentStatus status, Pageable pageable);
```

### Pagination (Always)

```java
// ✅ Always paginate list endpoints
Page<Document> findAllByDeletedFalse(Pageable pageable);

// ❌ Never load all records
List<Document> findAll(); // Dangerous — could be millions of rows
```

### Projection (Fetch Only What You Need)

```java
// ✅ Use projection for list views (avoid loading full entity graph)
public interface DocumentSummary {
    UUID getId();
    String getFilename();
    DocumentStatus getStatus();
    Instant getCreatedAt();
}

Page<DocumentSummary> findSummariesByStatus(DocumentStatus status, Pageable pageable);
```

---

## Indexing Strategy

```yaml
indexing:
  always_index:
    - Foreign keys (JPA doesn't auto-index in PostgreSQL)
    - Columns used in WHERE clauses
    - Columns used in ORDER BY
    - Columns used in JOIN conditions
    - Status/type columns used for filtering
  
  index_naming: "idx_{table}_{columns}"
  
  example:
    table: documents
    indexes:
      - idx_documents_status (status)
      - idx_documents_uploaded_by (uploaded_by)
      - idx_documents_created_at (created_at DESC)
      - idx_documents_status_created (status, created_at DESC)  # composite
```

---

## Soft Delete Pattern

```java
@Entity
@Where(clause = "deleted = false")  // Hibernate filter
public class Document {
    // ...
    
    @Column(nullable = false)
    private boolean deleted = false;
    
    @Column(name = "deleted_at")
    private Instant deletedAt;
}

// Service method
@Transactional
public void delete(UUID id) {
    var document = repository.findById(id)
            .orElseThrow(() -> new DocumentNotFoundException(id));
    document.setDeleted(true);
    document.setDeletedAt(clock.instant());
    repository.save(document);
    eventPublisher.publishDeleted(document);
}
```

---

## Auditing

```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;
    
    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;
}
```

---

## Connection Pool Configuration

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10       # DB max connections / max replicas
      minimum-idle: 5
      connection-timeout: 5000    # 5s to get connection from pool
      idle-timeout: 300000        # 5min idle before closing
      max-lifetime: 1800000       # 30min max connection age
      leak-detection-threshold: 30000  # Warn if connection held > 30s
```

```yaml
pool_sizing:
  formula: "DB max connections / max pods = pool size per pod"
  example:
    db_max_connections: 100
    max_pods: 10
    pool_per_pod: 10
  safety: "Always leave 10-20% headroom for admin connections"
```
