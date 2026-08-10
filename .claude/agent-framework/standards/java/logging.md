# Logging Standards

> Version 1.0 | AI-DLC Engineering Handbook
> Purpose: Define how logging is used across all Java services.

---

## 1. Principles

- Logging is for **operations** — debug failures, trace requests, audit actions
- Every log line must be **useful in production**
- Logs must be **structured** for machine parsing
- Never log sensitive data

---

## 2. Framework

**Use SLF4J with Logback.**

```java
private static final Logger log = LoggerFactory.getLogger(OrderService.class);
```

**Never use:**
```java
System.out.println()       // ❌
System.err.println()       // ❌
e.printStackTrace()        // ❌
java.util.logging.Logger   // ❌
```

---

## 3. Log Levels

| Level | When to Use | Example |
|-------|-------------|---------|
| `ERROR` | System failure, requires attention | Database connection lost, unhandled exception |
| `WARN` | Expected but noteworthy issue | Retry triggered, deprecated API called |
| `INFO` | Significant business event | Order created, payment processed, user registered |
| `DEBUG` | Development diagnostics | Method entry/exit, variable values |
| `TRACE` | Fine-grained debugging | Loop iterations, detailed state |

### Production Logging Level

- Production: `INFO`
- Staging: `DEBUG`
- Development: `DEBUG` or `TRACE`

---

## 4. Structured Logging Format

Use key-value pairs for machine-parseable logs:

```java
// ✅ Good — structured context
log.info("Order created orderId={} customerId={} total={}", 
    order.getId(), order.getCustomerId(), order.getTotal());

log.error("Payment failed orderId={} reason={}", 
    orderId, e.getMessage(), e);

// ❌ Bad — unstructured, hard to parse
log.info("Created order for customer " + customerId + " with total " + total);
```

---

## 5. What to Log

### Always Log

- Request entry (controller level): method, path, user
- Business events: order created, payment processed
- External system calls: start, duration, outcome
- Failures: with context to reproduce
- Security events: login, failed auth, permission denied

### Never Log

- Passwords
- Tokens (JWT, API keys)
- Credit card numbers
- Personal identifiers (SSN, full address)
- Request/response bodies containing PII
- Large payloads (serialize only IDs)

---

## 6. Correlation / Trace IDs

Every request must carry a trace ID through all log lines:

```java
// Set in filter/interceptor
MDC.put("traceId", UUID.randomUUID().toString());
MDC.put("userId", authenticatedUser.getId());

// Logging pattern includes trace ID automatically
// %d [%thread] [%X{traceId}] %-5level %logger{36} - %msg%n
```

---

## 7. Performance Logging

Log durations for external calls and expensive operations:

```java
long start = System.nanoTime();
var result = externalService.call(request);
long durationMs = (System.nanoTime() - start) / 1_000_000;
log.info("External call completed service=PaymentGateway durationMs={} status={}",
    durationMs, result.getStatus());
```

---

## 8. Anti-Patterns

```java
// ❌ String concatenation in log (evaluated even if level disabled)
log.debug("Processing order " + order.toString());

// ❌ Logging inside tight loops
for (Item item : items) {
    log.info("Processing item {}", item.getId()); // thousands of lines
}

// ❌ Logging and rethrowing (duplicate noise)
catch (Exception e) {
    log.error("Error", e);
    throw e;
}

// ❌ Generic messages with no context
log.error("An error occurred");
log.info("Done");
```

---

## 9. Logback Configuration

```xml
<configuration>
  <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
    <encoder>
      <pattern>%d{ISO8601} [%thread] [%X{traceId}] %-5level %logger{36} - %msg%n</pattern>
    </encoder>
  </appender>

  <root level="INFO">
    <appender-ref ref="STDOUT"/>
  </root>

  <logger name="com.company" level="DEBUG"/>
  <logger name="org.springframework" level="WARN"/>
  <logger name="org.hibernate.SQL" level="DEBUG"/>
</configuration>
```

---

## 10. Checklist

- [ ] SLF4J used exclusively
- [ ] No `System.out.println` or `e.printStackTrace`
- [ ] Log levels used correctly
- [ ] Structured key-value format
- [ ] Trace ID present in all log lines
- [ ] No sensitive data logged
- [ ] No string concatenation in log statements
- [ ] External call durations logged
- [ ] No logging inside tight loops
