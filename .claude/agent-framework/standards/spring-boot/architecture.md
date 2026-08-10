# Spring Boot Architecture

> Version 1.0 | AI-DLC Engineering Handbook
> Purpose: Define the architecture for all Spring Boot microservices.

---

## 1. Principles

- Clean Architecture — dependencies point inward
- Services are independent and own their data
- Loose coupling via events where appropriate
- DTOs at the boundary — never expose entities
- Configuration externalized — no hardcoded values

---

## 2. Layers

```
Controller       (HTTP boundary — thin)
  ↓
Service          (Business logic — all rules here)
  ↓
Repository       (Data access — queries only)
  ↓
Database         (PostgreSQL)
```

**Never:**
```
Controller → Repository     (skipping business logic)
Service → Controller        (reverse dependency)
Entity exposed in API       (always use DTOs)
```

---

## 3. Package Structure

```
com.company.servicename/
├── config/           # Spring configuration and beans
├── controller/       # REST controllers (thin, no logic)
├── service/          # Business logic (transactional)
├── repository/       # Spring Data interfaces
├── entity/           # JPA entities (DB mapping)
├── dto/              # Request/Response records
├── mapper/           # Entity ↔ DTO conversion
├── exception/        # Custom exceptions + global handler
├── security/         # Security configuration
├── events/           # Domain event classes + publishers
└── client/           # Feign/HTTP clients for other services
```

---

## 4. Microservice Ownership

Each service owns:
- **Its database** — no shared databases
- **Its API** — versioned, documented with OpenAPI
- **Its business logic** — no shared business libraries

**Never share a database between services.**

Communication between services:
- Synchronous: REST or gRPC (for queries)
- Asynchronous: Events via message broker (for commands/notifications)

---

## 5. Controller Layer

Thin. No business logic. Just HTTP translation.

```java
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final OrderMapper orderMapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse createOrder(@Valid @RequestBody CreateOrderRequest request) {
        Order order = orderService.create(orderMapper.toDomain(request));
        return orderMapper.toResponse(order);
    }

    @GetMapping("/{id}")
    public OrderResponse getOrder(@PathVariable String id) {
        return orderMapper.toResponse(orderService.findById(id));
    }

    @GetMapping
    public Page<OrderResponse> listOrders(Pageable pageable) {
        return orderService.findAll(pageable).map(orderMapper::toResponse);
    }
}
```

---

## 6. Service Layer

All business logic lives here. Transaction boundaries defined here.

```java
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final PaymentGateway paymentGateway;
    private final EventPublisher eventPublisher;

    @Transactional
    public Order create(CreateOrderCommand command) {
        var order = Order.create(command);
        order = orderRepository.save(order);
        paymentGateway.authorize(order.getPaymentDetails());
        eventPublisher.publish(new OrderCreatedEvent(order.getId()));
        return order;
    }
}
```

---

## 7. Configuration

### application.yml

```yaml
spring:
  application:
    name: order-service
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}

server:
  port: 8080
```

### Profiles

| Profile | Purpose |
|---------|---------|
| `dev` | Local development |
| `test` | Test execution (Testcontainers) |
| `staging` | Pre-production validation |
| `prod` | Production |

### Typed Configuration

```java
@ConfigurationProperties(prefix = "app.payment")
public record PaymentConfig(
    String gatewayUrl,
    Duration timeout,
    int maxRetries
) {}
```

---

## 8. Checklist

- [ ] Layered architecture enforced
- [ ] No controller → repository shortcuts
- [ ] Entities never exposed in API
- [ ] DTOs (records) used at boundaries
- [ ] Each service owns its database
- [ ] Configuration externalized
- [ ] Profiles for all environments
- [ ] Service layer handles transactions

---

## 9. AI Agent Prompt

```
Design using Clean Architecture.
Keep services independent.
Use events where appropriate.
Avoid tight coupling.
Every API must use DTOs (records).
Never expose entities.
Controller → Service → Repository only.
Externalize all configuration.
Use Spring profiles for environments.
```
