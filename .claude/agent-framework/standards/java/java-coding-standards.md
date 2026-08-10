# Java Coding Standards

> Version 1.0 | AI-DLC Engineering Handbook
> Purpose: Define how every Java class in this organization must be written.

---

## 1. Scope

This document governs all Java source code written by human developers and AI agents.
It applies to production code, test code, and infrastructure utilities.

---

## 2. Java Version

**Use Java 21.**

### Preferred Language Features

| Feature | Use Case |
|---------|----------|
| Records | DTOs, value objects, immutable data carriers |
| Sealed classes | Restricted type hierarchies, domain modeling |
| Pattern matching (`instanceof`) | Type-safe conditional logic |
| Switch expressions | Multi-branch logic with exhaustiveness |
| `Optional` | Nullable return types |
| Stream API | Collection transformations |
| Text blocks | Multi-line strings, SQL, JSON |
| Virtual threads | High-concurrency I/O workloads |

### Avoid

| Legacy | Modern Replacement |
|--------|-------------------|
| `java.util.Date` / `Calendar` | `java.time.*` |
| Raw collections (`List`, `Map`) | Generics (`List<Order>`) |
| Null returns | `Optional<T>` or empty collections |
| Anonymous inner classes | Lambdas / method references |
| `synchronized` blocks | `java.util.concurrent` utilities |

---

## 3. Naming Conventions

### Packages

```
com.company.service.module
```

- All lowercase
- Dot-separated
- Singular nouns

### Classes and Interfaces

| Type | Pattern | Example |
|------|---------|---------|
| Controller | `*Controller` | `UserController` |
| Service | `*Service` | `UserService` |
| Repository | `*Repository` | `UserRepository` |
| DTO (request) | `*Request` | `CreateUserRequest` |
| DTO (response) | `*Response` | `UserResponse` |
| Entity | `*Entity` | `UserEntity` |
| Mapper | `*Mapper` | `UserMapper` |
| Config | `*Config` | `SecurityConfig` |
| Exception | `*Exception` | `ResourceNotFoundException` |
| Event | `*Event` | `OrderCreatedEvent` |

### Methods

- Verb-first: `findById`, `calculateTotal`, `validateInput`
- Boolean methods: `isActive`, `hasPermission`, `canExecute`
- Factory methods: `of`, `from`, `create`

### Constants

```java
private static final int MAX_RETRY_COUNT = 3;
private static final Duration CACHE_TTL = Duration.ofMinutes(15);
```

---

## 4. Package Structure

```
com.company.servicename/
├── config/           # Spring configuration
├── controller/       # REST controllers (thin)
├── service/          # Business logic
├── repository/       # Data access
├── entity/           # JPA entities
├── dto/              # Request/Response objects
├── mapper/           # Object mappers
├── exception/        # Custom exceptions
├── security/         # Security utilities
├── events/           # Domain events
└── util/             # Shared utilities
```

---

## 5. SOLID Principles

### Single Responsibility

One class = one reason to change.

```java
// ✅ Good — each class has one responsibility
public class OrderValidator { ... }
public class OrderPriceCalculator { ... }
public class OrderRepository { ... }

// ❌ Bad — one class doing everything
public class OrderManager {
    public void validate() { ... }
    public BigDecimal calculatePrice() { ... }
    public void saveToDatabase() { ... }
    public void sendEmail() { ... }
}
```

### Open/Closed

Open for extension, closed for modification. Use interfaces and strategy patterns.

### Liskov Substitution

Subtypes must be substitutable for their base types without breaking behavior.

### Interface Segregation

Prefer small, focused interfaces over large ones.

```java
// ✅ Good
public interface Readable { byte[] read(); }
public interface Writable { void write(byte[] data); }

// ❌ Bad
public interface ReadWriteDeleteArchive { ... }
```

### Dependency Inversion

Depend on abstractions, not concrete implementations.

**Enforced layer dependencies:**

```
Controller → Service → Repository
```

**Never:**

```
Controller → Repository  (skipping service layer)
Service → Controller     (reverse dependency)
```

---

## 6. Clean Code Practices

### Method Length

- Maximum 20 lines (prefer 5–10)
- If longer, extract methods

### Method Parameters

- Maximum 3 parameters
- More than 3? Use a parameter object (record)

```java
// ✅ Good
public Order createOrder(CreateOrderCommand command) { ... }

// ❌ Bad
public Order createOrder(String customerId, List<Item> items, 
    Address shipping, Address billing, String couponCode) { ... }
```

### Guard Clauses

Prefer early returns over nested if-else.

```java
// ✅ Good
public void processOrder(Order order) {
    if (order == null) throw new ValidationException("Order cannot be null");
    if (order.getItems().isEmpty()) throw new ValidationException("Order has no items");
    // happy path
}

// ❌ Bad
public void processOrder(Order order) {
    if (order != null) {
        if (!order.getItems().isEmpty()) {
            // deeply nested logic
        }
    }
}
```

### Immutability

- Use `final` for fields that don't change
- Use records for value objects
- Return unmodifiable collections

```java
public record Money(BigDecimal amount, Currency currency) {}
```

---

## 7. Dependency Injection

**Always use constructor injection. Never field injection.**

```java
// ✅ Good — constructor injection
@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final PaymentGateway paymentGateway;
    private final EventPublisher eventPublisher;

    public OrderService(OrderRepository orderRepository,
                        PaymentGateway paymentGateway,
                        EventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.paymentGateway = paymentGateway;
        this.eventPublisher = eventPublisher;
    }
}

// ❌ Bad — field injection
@Service
public class OrderService {
    @Autowired private OrderRepository orderRepository;
    @Autowired private PaymentGateway paymentGateway;
}
```

---

## 8. Collections and Streams

### Prefer

```java
// Stream with clear intent
List<String> activeEmails = users.stream()
    .filter(User::isActive)
    .map(User::getEmail)
    .toList();

// Collectors for complex operations
Map<Status, List<Order>> ordersByStatus = orders.stream()
    .collect(Collectors.groupingBy(Order::getStatus));
```

### Avoid

```java
// ❌ Overly complex streams (extract methods instead)
var result = items.stream()
    .filter(i -> i.getPrice().compareTo(BigDecimal.TEN) > 0)
    .map(i -> new ItemDto(i.getName(), i.getPrice().multiply(taxRate)))
    .sorted(Comparator.comparing(ItemDto::price).reversed())
    .limit(10)
    .toList();
```

---

## 9. Concurrency

- Use `java.util.concurrent` utilities
- Prefer virtual threads for I/O-bound work (Java 21)
- Use `ConcurrentHashMap` over `Collections.synchronizedMap`
- Avoid shared mutable state
- Use `CompletableFuture` for async operations

---

## 10. Code Review Checklist

- [ ] Follows naming conventions
- [ ] SOLID principles respected
- [ ] No field injection
- [ ] No raw collections
- [ ] No null returns (use Optional)
- [ ] Guard clauses used
- [ ] Methods under 20 lines
- [ ] Max 3 parameters per method
- [ ] Java 21 features used where appropriate
- [ ] Javadoc on all public methods
- [ ] No `System.out.println`
- [ ] SLF4J logging used correctly
- [ ] Exceptions are specific (not generic)
- [ ] Spock tests included (unit + BDD integration)

---

## 11. AI Agent Prompt

```
Use standards/java/java-coding-standards.md.
Create production-ready Java 21 code.
Follow SOLID strictly.
Write clean architecture.
Use constructor injection — never field injection.
Use records for DTOs and value objects.
Use Optional — never return null.
Use Stream API for collections.
No raw types, no legacy Date API.
Generate JavaDoc for all public methods.
Create Spock unit tests and BDD integration tests.
Use given/when/then blocks in all test specifications.
Follow the code review checklist before delivering code.
```
