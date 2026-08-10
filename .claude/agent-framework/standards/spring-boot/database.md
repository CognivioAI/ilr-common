# Database Standards

> Version 1.0 | AI-DLC Engineering Handbook
> Purpose: Define database design and access patterns for Spring Boot services.

---

## 1. Principles

- Each microservice owns its database
- Schema changes are versioned migrations
- Entities map to tables; DTOs map to APIs
- Performance through indexing and pagination
- Data integrity through constraints, not application logic

---

## 2. Technology

- **Primary database**: PostgreSQL (RDS)
- **Migration tool**: Flyway
- **ORM**: Spring Data JPA (Hibernate)
- **Connection pool**: HikariCP

---

## 3. Entity Design

```java
@Entity
@Table(name = "orders")
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Version
    private Long version; // Optimistic locking

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
```

### Rules

- Use UUID for IDs (not auto-increment)
- Always include `createdAt` and `updatedAt`
- Use `@Version` for optimistic locking
- Use `@Enumerated(EnumType.STRING)` (never ORDINAL)
- Set `nullable = false` where appropriate

---

## 4. Repository Layer

```java
public interface OrderRepository extends JpaRepository<OrderEntity, String> {

    Page<OrderEntity> findByCustomerId(String customerId, Pageable pageable);

    @Query("SELECT o FROM OrderEntity o WHERE o.status = :status AND o.createdAt > :since")
    List<OrderEntity> findRecentByStatus(
        @Param("status") OrderStatus status,
        @Param("since") Instant since);

    @Modifying
    @Query("UPDATE OrderEntity o SET o.status = :status WHERE o.id = :id")
    int updateStatus(@Param("id") String id, @Param("status") OrderStatus status);
}
```

---

## 5. Flyway Migrations

### Naming

```
V1__create_orders_table.sql
V2__add_customer_email_column.sql
V3__create_index_on_status.sql
```

### Rules

- Migrations are **immutable** once applied
- One logical change per migration
- Always include rollback comments
- Test migrations against production-like data

### Example

```sql
-- V1__create_orders_table.sql
CREATE TABLE orders (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id VARCHAR(36)    NOT NULL,
    status      VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    total       DECIMAL(10, 2) NOT NULL,
    created_at  TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP      NOT NULL DEFAULT NOW(),
    version     BIGINT         NOT NULL DEFAULT 0
);

CREATE INDEX idx_orders_customer_id ON orders(customer_id);
CREATE INDEX idx_orders_status ON orders(status);
```

---

## 6. Performance

- Index all columns used in WHERE, JOIN, ORDER BY
- Use pagination (never unbounded queries)
- Use projections for read-heavy queries
- Batch operations for bulk writes
- Monitor slow queries (> 1s)

---

## 7. Anti-Patterns

- ❌ Sharing a database between services
- ❌ Using `ddl-auto: update` in production
- ❌ Missing indexes on foreign keys
- ❌ Eager fetching large collections
- ❌ N+1 queries
- ❌ Auto-increment integer IDs (use UUID)
- ❌ Storing business logic in stored procedures

---

## 8. Checklist

- [ ] PostgreSQL as primary database
- [ ] Flyway for all schema changes
- [ ] UUID primary keys
- [ ] Optimistic locking (`@Version`)
- [ ] Timestamps on all entities
- [ ] Indexes on query columns
- [ ] Pagination on all list queries
- [ ] Connection pool configured (HikariCP)
- [ ] `ddl-auto: validate` in production
