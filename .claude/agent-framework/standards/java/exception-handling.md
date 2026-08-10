# Exception Handling Standards

> Version 1.0 | AI-DLC Engineering Handbook
> Purpose: Define how exceptions are created, thrown, caught, and communicated.

---

## 1. Principles

- Exceptions represent **unexpected failures**, not control flow
- Every exception must carry a **meaningful message**
- Never catch and swallow silently
- Fail fast, fail loud
- Use specific exception types — never generic `Exception`

---

## 2. Exception Hierarchy

```
RuntimeException
├── BusinessException             # Business rule violations
│   ├── InsufficientFundsException
│   ├── OrderAlreadyFulfilledException
│   └── DuplicateResourceException
├── ValidationException           # Input validation failures
│   ├── InvalidEmailException
│   └── MissingRequiredFieldException
├── ResourceNotFoundException     # Entity not found
├── ConflictException             # Concurrent modification
└── IntegrationException          # External system failures
    ├── PaymentGatewayException
    └── NotificationServiceException
```

---

## 3. Custom Exception Template

```java
public class ResourceNotFoundException extends RuntimeException {

    private final String resourceType;
    private final String resourceId;

    public ResourceNotFoundException(String resourceType, String resourceId) {
        super(String.format("%s not found with id: %s", resourceType, resourceId));
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }

    public String getResourceType() { return resourceType; }
    public String getResourceId() { return resourceId; }
}
```

---

## 4. Global Exception Handler

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity.status(404)
            .body(ErrorResponse.of(404, ex.getMessage()));
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ValidationException ex) {
        log.warn("Validation failed: {}", ex.getMessage());
        return ResponseEntity.status(400)
            .body(ErrorResponse.of(400, ex.getMessage()));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex) {
        log.warn("Business rule violation: {}", ex.getMessage());
        return ResponseEntity.status(422)
            .body(ErrorResponse.of(422, ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(500)
            .body(ErrorResponse.of(500, "An unexpected error occurred"));
    }
}
```

---

## 5. Error Response Format

```java
public record ErrorResponse(
    Instant timestamp,
    int status,
    String error,
    String message,
    String traceId
) {
    public static ErrorResponse of(int status, String message) {
        return new ErrorResponse(
            Instant.now(),
            status,
            HttpStatus.valueOf(status).getReasonPhrase(),
            message,
            MDC.get("traceId")
        );
    }
}
```

---

## 6. Anti-Patterns

```java
// ❌ Never catch generic Exception
try { ... } catch (Exception e) { ... }

// ❌ Never swallow exceptions
try { ... } catch (IOException e) { /* empty */ }

// ❌ Never use exceptions for control flow
try {
    user = repository.findById(id).get();
} catch (NoSuchElementException e) {
    user = createNewUser();
}

// ❌ Never log and rethrow (causes duplicate logs)
catch (Exception e) {
    log.error("Error", e);
    throw e;
}
```

---

## 7. Correct Patterns

```java
// ✅ Use Optional
User user = repository.findById(id)
    .orElseThrow(() -> new ResourceNotFoundException("User", id));

// ✅ Catch specific exceptions
try {
    paymentGateway.charge(amount);
} catch (PaymentDeclinedException e) {
    throw new BusinessException("Payment declined: " + e.getReason());
} catch (PaymentGatewayException e) {
    log.error("Payment gateway failure orderId={}", orderId, e);
    throw new IntegrationException("Payment service unavailable", e);
}

// ✅ Either log or rethrow — not both
catch (IOException e) {
    throw new IntegrationException("Failed to read file", e);
}
```

---

## 8. Checklist

- [ ] No generic `catch (Exception e)`
- [ ] No empty catch blocks
- [ ] Every exception has a meaningful message
- [ ] Custom exceptions extend the correct base class
- [ ] Global exception handler maps all custom exceptions to HTTP status codes
- [ ] Sensitive data never appears in error messages
- [ ] Integration failures are wrapped in `IntegrationException`
- [ ] `Optional` used instead of catching `NoSuchElementException`
