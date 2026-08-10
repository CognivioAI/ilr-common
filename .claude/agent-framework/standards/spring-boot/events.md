# Event-Driven Architecture Standards

> Version 1.0 | AI-DLC Engineering Handbook
> Purpose: Define how services communicate asynchronously via events.

---

## 1. Principles

- Use events instead of direct calls when possible
- Events represent facts that happened (past tense)
- Publishers don't know about subscribers
- Events are immutable
- At-least-once delivery with idempotent consumers

---

## 2. When to Use Events

| Use Events | Use Direct Calls |
|-----------|-----------------|
| Notifications to other services | Query that needs immediate response |
| Triggering async workflows | Data needed for current request |
| Decoupling services | Within the same service boundary |
| Audit trails | Simple CRUD within one service |

---

## 3. Event Naming

Past tense. Noun + Verb.

```
OrderCreatedEvent
PaymentProcessedEvent
DocumentUploadedEvent
UserRegisteredEvent
InventoryReservedEvent
```

---

## 4. Event Structure

```java
public record OrderCreatedEvent(
    String eventId,         // Unique event ID (idempotency key)
    String eventType,       // "order.created"
    Instant timestamp,      // When it happened
    String orderId,         // Aggregate ID
    String customerId,      // Key business data
    BigDecimal total        // Minimal payload
) {}
```

### Rules

- Include `eventId` for idempotency
- Include `timestamp` for ordering
- Include aggregate ID for routing
- Minimal payload — consumers can query for details
- Never include sensitive data in events

---

## 5. Event Flow Example

```
DOCUMENT_UPLOADED
  ↓
OCR Agent (subscriber)
  ↓
DOCUMENT_EXTRACTED
  ↓
Validation Agent (subscriber)
  ↓
DOCUMENT_VALIDATED
  ↓
Indexing Service (subscriber)
```

---

## 6. Spring Application Events (In-Process)

```java
// Publishing
@Service
@RequiredArgsConstructor
public class OrderService {
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Order create(CreateOrderCommand command) {
        var order = orderRepository.save(Order.create(command));
        eventPublisher.publishEvent(new OrderCreatedEvent(order));
        return order;
    }
}

// Subscribing
@Component
public class OrderNotificationListener {

    @TransactionalEventListener(phase = AFTER_COMMIT)
    public void onOrderCreated(OrderCreatedEvent event) {
        notificationService.sendConfirmation(event.customerId());
    }
}
```

---

## 7. Cross-Service Events (SQS/SNS)

### Publisher

```java
@Component
@RequiredArgsConstructor
public class EventPublisher {
    private final SnsClient snsClient;
    private final ObjectMapper objectMapper;

    public void publish(DomainEvent event) {
        snsClient.publish(PublishRequest.builder()
            .topicArn(topicArn)
            .message(objectMapper.writeValueAsString(event))
            .messageAttributes(Map.of(
                "eventType", attr(event.getEventType())
            ))
            .build());
    }
}
```

### Consumer

```java
@Component
public class OrderEventConsumer {

    @SqsListener("order-events-queue")
    public void handleOrderCreated(OrderCreatedEvent event) {
        // Idempotency check
        if (processedEvents.contains(event.eventId())) return;
        // Process
        inventoryService.reserve(event.orderId());
        processedEvents.mark(event.eventId());
    }
}
```

---

## 8. Idempotency

Consumers must handle duplicate events safely:

- Store processed event IDs
- Use database upserts
- Design operations to be naturally idempotent

---

## 9. Anti-Patterns

- ❌ Event payload too large (include only IDs + key data)
- ❌ Synchronous behavior disguised as events
- ❌ Events without `eventId` (no idempotency)
- ❌ Circular event chains (A → B → A)
- ❌ Business logic in event handlers that should be in services

---

## 10. Checklist

- [ ] Events named in past tense
- [ ] Event ID included for idempotency
- [ ] Minimal payload
- [ ] `@TransactionalEventListener` for in-process events
- [ ] SQS/SNS for cross-service events
- [ ] Idempotent consumers
- [ ] Dead-letter queue configured
- [ ] Event schema documented
