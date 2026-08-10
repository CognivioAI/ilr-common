# REST API Standards

> Version 1.0 | AI-DLC Engineering Handbook
> Purpose: Define how REST APIs are designed and implemented.

---

## 1. Principles

- APIs are contracts — treat them as products
- Consistent, predictable conventions
- Versioned from day one
- Documented with OpenAPI
- Always return `ResponseEntity`

---

## 2. URL Design

### Base Pattern

```
/api/v{version}/{resource}
```

### Conventions

| Rule | Example |
|------|---------|
| Plural nouns | `/api/v1/orders` |
| Lowercase | `/api/v1/users` |
| Hyphens for multi-word | `/api/v1/order-items` |
| IDs in path | `/api/v1/orders/{orderId}` |
| Sub-resources | `/api/v1/orders/{orderId}/items` |
| Query params for filtering | `/api/v1/orders?status=PENDING&page=0` |

### Never

- Verbs in URLs: ~~`/api/v1/createOrder`~~
- Trailing slashes: ~~`/api/v1/orders/`~~
- File extensions: ~~`/api/v1/orders.json`~~

---

## 3. HTTP Methods

| Operation | Method | Path | Response |
|-----------|--------|------|----------|
| List | GET | `/orders` | 200 + Page |
| Get one | GET | `/orders/{id}` | 200 + Object |
| Create | POST | `/orders` | 201 + Object |
| Full update | PUT | `/orders/{id}` | 200 + Object |
| Partial update | PATCH | `/orders/{id}` | 200 + Object |
| Delete | DELETE | `/orders/{id}` | 204 (no body) |

---

## 4. Request / Response Format

### Request DTOs

```java
public record CreateOrderRequest(
    @NotBlank String customerId,
    @NotEmpty List<OrderItemRequest> items,
    @NotNull Address shippingAddress
) {}
```

### Response DTOs

```java
public record OrderResponse(
    String id,
    String customerId,
    OrderStatus status,
    BigDecimal total,
    Instant createdAt
) {}
```

---

## 5. Pagination

**All list endpoints must be paginated.**

```java
@GetMapping
public ResponseEntity<Page<OrderResponse>> listOrders(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size,
    @RequestParam(defaultValue = "createdAt,desc") String sort) {

    var pageable = PageRequest.of(page, size, Sort.by(sort));
    return ResponseEntity.ok(orderService.findAll(pageable));
}
```

### Response Structure

```json
{
  "content": [...],
  "page": { "number": 0, "size": 20, "totalElements": 142, "totalPages": 8 }
}
```

---

## 6. Error Response

```json
{
  "timestamp": "2024-01-15T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "details": [
    { "field": "email", "message": "must be a valid email" }
  ],
  "traceId": "abc-123-def"
}
```

---

## 7. Versioning

- Use URL path versioning: `/api/v1/`, `/api/v2/`
- Keep old versions running during migration periods
- Document breaking changes in changelog
- Deprecate before removing

---

## 8. OpenAPI Documentation

```java
@Operation(summary = "Create a new order")
@ApiResponses({
    @ApiResponse(responseCode = "201", description = "Order created"),
    @ApiResponse(responseCode = "400", description = "Validation error"),
    @ApiResponse(responseCode = "409", description = "Duplicate order")
})
@PostMapping
public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
    ...
}
```

---

## 9. Anti-Patterns

- ❌ Returning entities directly from controllers
- ❌ Unbounded list endpoints (no pagination)
- ❌ Business logic in controllers
- ❌ Inconsistent naming (`/getUsers` vs `/orders`)
- ❌ Returning 200 for creation (should be 201)
- ❌ Returning body on DELETE (should be 204)

---

## 10. Checklist

- [ ] All endpoints use `/api/v{n}/` prefix
- [ ] Plural nouns, lowercase, hyphens
- [ ] Correct HTTP methods and status codes
- [ ] Request validation with Jakarta `@Valid`
- [ ] All list endpoints paginated
- [ ] Response DTOs (never entities)
- [ ] OpenAPI annotations on all endpoints
- [ ] Error responses follow standard format
- [ ] `ResponseEntity` wraps all responses
