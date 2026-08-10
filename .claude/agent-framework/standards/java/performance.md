# Performance Standards

> Version 1.0 | AI-DLC Engineering Handbook
> Purpose: Define performance practices for all Java services.

---

## 1. Principles

- Measure before optimizing
- Optimize for the common case
- Understand memory vs CPU vs I/O trade-offs
- Set performance budgets and enforce them
- Performance is a feature, not an afterthought

---

## 2. Database Performance

### Always

- Use pagination for list queries (never return unbounded results)
- Index columns used in WHERE, JOIN, ORDER BY
- Use projection (select only needed columns) for read-heavy queries
- Use batch operations for bulk writes

```java
// ✅ Pagination
Page<OrderEntity> findByCustomerId(String customerId, Pageable pageable);

// ✅ Projection
@Query("SELECT new com.company.dto.OrderSummary(o.id, o.status, o.total) " +
       "FROM OrderEntity o WHERE o.customerId = :customerId")
List<OrderSummary> findSummariesByCustomerId(@Param("customerId") String customerId);
```

### Avoid

- N+1 queries (use `@EntityGraph` or `JOIN FETCH`)
- Eager fetching of large collections
- `SELECT *` for list endpoints
- Missing indexes on foreign keys

---

## 3. Caching

### When to Cache

- Read-heavy, write-light data
- Expensive computations
- External API responses

### Cache Strategy

```java
@Cacheable(value = "products", key = "#productId")
public ProductResponse getProduct(String productId) { ... }

@CacheEvict(value = "products", key = "#productId")
public void updateProduct(String productId, UpdateProductRequest request) { ... }
```

### Rules

- Set TTL on all cache entries (no infinite caches)
- Use cache-aside pattern
- Invalidate on writes
- Monitor cache hit rates

---

## 4. Connection Pooling

### HikariCP Configuration

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      idle-timeout: 300000
      connection-timeout: 20000
      max-lifetime: 1800000
```

### Rules

- Pool size = (core_count * 2) + disk_spindles (start with 10–20)
- Monitor active/idle connections
- Set connection timeout (20s)
- Log slow queries (> 1s)

---

## 5. Async and Virtual Threads

### Virtual Threads (Java 21)

```java
@Bean
public AsyncTaskExecutor applicationTaskExecutor() {
    return new TaskExecutorAdapter(Executors.newVirtualThreadPerTaskExecutor());
}
```

Use virtual threads for:
- HTTP client calls
- Database queries (with compatible drivers)
- File I/O operations

### CompletableFuture for Parallel Calls

```java
public OrderDetailsResponse getOrderDetails(String orderId) {
    var orderFuture = CompletableFuture.supplyAsync(() -> orderService.getOrder(orderId));
    var paymentFuture = CompletableFuture.supplyAsync(() -> paymentService.getPayment(orderId));

    return new OrderDetailsResponse(orderFuture.join(), paymentFuture.join());
}
```

---

## 6. API Performance

### Response Time Budgets

| Endpoint Type | Target P99 |
|---------------|-----------|
| Health check | < 50ms |
| Simple CRUD | < 200ms |
| Complex query | < 500ms |
| Report generation | < 2s |

### Compression

```yaml
server:
  compression:
    enabled: true
    mime-types: application/json,text/plain
    min-response-size: 1024
```

### Pagination

**Always paginate list endpoints.**

```java
@GetMapping
public Page<OrderResponse> listOrders(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size) {
    return orderService.findAll(PageRequest.of(page, size));
}
```

---

## 7. Memory Management

- Prefer streams over collecting into large lists
- Use `StringBuilder` for repeated string concatenation
- Avoid creating unnecessary objects in loops
- Monitor GC pauses with JVM metrics
- Set appropriate heap sizes (`-Xms` = `-Xmx` in containers)

---

## 8. Anti-Patterns

- ❌ Unbounded queries (`findAll()` without pagination)
- ❌ N+1 queries in loops
- ❌ Synchronous calls to external services in request thread
- ❌ Missing connection pool configuration
- ❌ Caching without TTL
- ❌ Loading entire files into memory
- ❌ String concatenation in loops

---

## 9. Checklist

- [ ] All list endpoints paginated
- [ ] Database indexes on query columns
- [ ] Connection pool configured
- [ ] Caching for read-heavy data
- [ ] Response time budgets defined
- [ ] No N+1 queries
- [ ] Async processing for non-critical paths
- [ ] GC and memory metrics monitored
- [ ] Compression enabled for API responses
