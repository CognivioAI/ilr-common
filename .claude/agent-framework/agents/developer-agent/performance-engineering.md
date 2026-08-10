# Performance Engineering

## Purpose

Proactive performance analysis — detect performance issues during development, not after production deployment. Catch N+1 queries, memory leaks, and bottlenecks before they ship.

---

## Performance Checks (During Development)

### Database Performance

```yaml
database_checks:
  n_plus_one:
    detect: "Collection access inside loop that triggers lazy loading"
    signal: |
      documents.forEach(doc -> {
          doc.getClassifications().size(); // Triggers SELECT per document
      });
    
    or: |
      documents.stream()
          .map(doc -> repository.findById(doc.getId())) // N queries
    
    fix:
      option_1: "@EntityGraph(attributePaths = {'classifications'})"
      option_2: "JOIN FETCH in JPQL query"
      option_3: "Batch fetch: @BatchSize(size = 50)"
    severity: HIGH
  
  unbounded_query:
    detect: "findAll() without pagination on table that grows"
    signal: "repository.findAll() or repository.findByStatus(status)"
    fix: "Always use Pageable: findAllByStatus(status, pageable)"
    severity: HIGH
  
  missing_index:
    detect: "WHERE clause on unindexed column"
    signal: "Custom query filtering on column without index"
    fix: "Add index in migration: CREATE INDEX idx_{table}_{column}"
    severity: MEDIUM
  
  select_star:
    detect: "Loading full entity when only few fields needed"
    signal: "repository.findAll() when only need id + name for dropdown"
    fix: "Use projection interface or DTO query"
    severity: LOW
  
  connection_pool:
    detect: "Pool configured too small for max replicas"
    signal: "HikariCP maximum-pool-size < expected concurrent queries"
    fix: "pool_size = db_max_connections / max_pods"
    severity: HIGH (causes timeouts at scale)
```

### Memory Performance

```yaml
memory_checks:
  large_collection_in_memory:
    detect: "Loading entire table into List"
    signal: "List<Document> allDocs = repository.findAll()"
    fix: "Stream processing or pagination"
    severity: HIGH
  
  string_concatenation_in_loop:
    detect: "String += in loop (creates many objects)"
    signal: |
      String result = "";
      for (var item : items) { result += item.toString(); }
    fix: "Use StringBuilder or String.join()"
    severity: LOW
  
  unclosed_resources:
    detect: "InputStream/Connection not in try-with-resources"
    signal: "InputStream is = new FileInputStream(...) without close()"
    fix: "try (var is = new FileInputStream(...)) { ... }"
    severity: MEDIUM
  
  large_object_retention:
    detect: "Large objects stored in fields that outlive their usefulness"
    signal: "Service class holding reference to large cached data"
    fix: "Use cache with TTL and max size, or scope to request"
    severity: MEDIUM
```

### API Performance

```yaml
api_checks:
  chatty_api:
    detect: "Multiple API calls needed for one user action"
    signal: "Frontend makes 5 calls to render one page"
    fix: "Aggregate endpoint or GraphQL for complex queries"
    severity: MEDIUM
  
  missing_pagination:
    detect: "List endpoint returns all records"
    signal: "GET /documents returns 10,000 documents in one response"
    fix: "Add Pageable parameter, default page size 20, max 100"
    severity: HIGH
  
  large_payload:
    detect: "Response includes unnecessary data"
    signal: "Document list returns full document content (not just metadata)"
    fix: "Return summary in list, full content only on GET by ID"
    severity: MEDIUM
  
  synchronous_long_operation:
    detect: "API blocks while processing > 5 seconds"
    signal: "POST /documents/process waits for AI classification before responding"
    fix: "Return 202 Accepted, process async, client polls for status"
    severity: HIGH
  
  missing_timeout:
    detect: "External call without timeout configuration"
    signal: "RestTemplate/WebClient without timeout set"
    fix: "Set connection timeout (5s) and read timeout (per-service)"
    severity: HIGH
```

### Spring Boot Specific

```yaml
spring_checks:
  open_session_in_view:
    detect: "spring.jpa.open-in-view=true (default!)"
    problem: "Lazy loading in controller layer, database connection held during rendering"
    fix: "Set spring.jpa.open-in-view=false, fetch needed data in service layer"
    severity: HIGH
  
  missing_cache:
    detect: "Expensive computation repeated on every request"
    signal: "Configuration lookup from DB on every API call"
    fix: "@Cacheable with appropriate TTL"
    severity: MEDIUM
  
  thread_pool_exhaustion:
    detect: "Blocking calls on limited thread pool"
    signal: "Sync HTTP call in async handler without dedicated pool"
    fix: "Dedicated thread pool for blocking I/O, or use WebClient"
    severity: HIGH
  
  startup_time:
    detect: "Service takes > 60 seconds to start"
    signal: "Spring context initialization slow"
    fix: "Lazy initialization for non-critical beans, check component scan scope"
    severity: MEDIUM
```

---

## Performance Review (Before PR)

```yaml
performance_review:
  checklist:
    database:
      - "No N+1 queries (verify SQL log count in integration tests)"
      - "All list queries paginated"
      - "Indexes exist for filter/sort columns"
      - "Connection pool sized correctly"
    
    memory:
      - "No unbounded collections loaded"
      - "Resources closed properly (try-with-resources)"
      - "No unnecessary object retention"
    
    api:
      - "Long operations are async (> 5s)"
      - "External calls have timeouts"
      - "List endpoints are paginated"
      - "Response payload is appropriate size"
    
    caching:
      - "Frequently accessed, rarely changed data → cached"
      - "Cache has TTL (not infinite)"
      - "Cache invalidated on write"

  flagging:
    if_found:
      high_severity: "Fix before PR — will cause production issues"
      medium_severity: "Fix or document with justification"
      low_severity: "Note for future optimization"
```

---

## Performance Testing Integration

```yaml
performance_testing:
  when:
    - New service (baseline performance)
    - New endpoint with complex logic
    - Database migration on large table
    - Architectural change (sync → async)
  
  approach:
    tool: k6 or Gatling
    scenarios:
      smoke: "1 user, verify response time < 500ms"
      load: "50 concurrent users, sustained 5 minutes"
      stress: "Ramp to 200 users, find breaking point"
    
    budgets:
      api_response: "p95 < 500ms (simple CRUD)"
      api_response_with_ai: "p95 < 10s (AI processing)"
      throughput: "Handle 100 req/s without degradation"
      error_rate: "< 0.1% under normal load"
```
