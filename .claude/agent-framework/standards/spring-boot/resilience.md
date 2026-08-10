# Resilience Standards

> Version 1.0 | AI-DLC Engineering Handbook
> Purpose: Define resilience patterns for distributed Spring Boot services.

---

## 1. Principles

- Everything fails eventually — design for it
- Fail fast, recover gracefully
- Isolate failures (bulkhead pattern)
- Provide degraded service over no service
- Monitor and alert on failure rates

---

## 2. Resilience4j (Preferred Library)

### Circuit Breaker

Prevents cascading failures when a downstream service is down.

```java
@CircuitBreaker(name = "paymentService", fallbackMethod = "paymentFallback")
public PaymentResponse processPayment(PaymentRequest request) {
    return paymentClient.charge(request);
}

private PaymentResponse paymentFallback(PaymentRequest request, Exception e) {
    log.warn("Payment service unavailable, queuing for retry orderId={}", request.orderId());
    paymentQueue.enqueue(request);
    return PaymentResponse.pending();
}
```

### Configuration

```yaml
resilience4j:
  circuitbreaker:
    instances:
      paymentService:
        sliding-window-size: 10
        failure-rate-threshold: 50
        wait-duration-in-open-state: 30s
        permitted-number-of-calls-in-half-open-state: 3
```

---

## 3. Retry

```java
@Retry(name = "emailService", fallbackMethod = "emailFallback")
public void sendNotification(String userId, String message) {
    emailClient.send(userId, message);
}
```

```yaml
resilience4j:
  retry:
    instances:
      emailService:
        max-attempts: 3
        wait-duration: 2s
        exponential-backoff-multiplier: 2
        retry-exceptions:
          - java.io.IOException
          - java.util.concurrent.TimeoutException
```

---

## 4. Timeout

```java
@TimeLimiter(name = "inventoryService")
public CompletableFuture<InventoryResponse> checkStock(String productId) {
    return CompletableFuture.supplyAsync(() -> inventoryClient.check(productId));
}
```

```yaml
resilience4j:
  timelimiter:
    instances:
      inventoryService:
        timeout-duration: 3s
```

---

## 5. Bulkhead

Isolate resources so one failing service doesn't exhaust all threads.

```yaml
resilience4j:
  bulkhead:
    instances:
      paymentService:
        max-concurrent-calls: 10
        max-wait-duration: 500ms
```

---

## 6. Rate Limiter

Protect services from being overwhelmed.

```yaml
resilience4j:
  ratelimiter:
    instances:
      externalApi:
        limit-for-period: 100
        limit-refresh-period: 1s
        timeout-duration: 0s
```

---

## 7. Fallback Strategies

| Strategy | When |
|----------|------|
| Return cached data | Read operations with stale-ok tolerance |
| Queue for retry | Write operations that can be deferred |
| Return default/empty | Non-critical data (recommendations) |
| Return error | Critical operations that cannot degrade |

---

## 8. Health Checks

```yaml
management:
  endpoint:
    health:
      show-details: when-authorized
  health:
    circuitbreakers:
      enabled: true
    ratelimiters:
      enabled: true
```

---

## 9. Anti-Patterns

- ❌ No timeout on HTTP clients (will hang forever)
- ❌ Retrying non-idempotent operations
- ❌ Retrying on 4xx errors (client errors won't self-resolve)
- ❌ Circuit breaker without fallback
- ❌ Ignoring partial failures (some items succeed, some fail)
- ❌ Infinite retry without backoff

---

## 10. Checklist

- [ ] Circuit breaker on all external service calls
- [ ] Retry with exponential backoff for transient failures
- [ ] Timeout configured on all HTTP clients
- [ ] Bulkhead for resource isolation
- [ ] Fallback methods for critical paths
- [ ] Health endpoint exposes circuit breaker state
- [ ] Alerts on circuit breaker open events
- [ ] Dead-letter queue for failed async operations
