# Reference Architecture: Event-Driven

> Services communicate primarily through asynchronous events.

---

## When to Use

- Multiple services need to react to the same business event
- Processing can be deferred (not immediate user response needed)
- Services should be independently deployable and scalable
- Audit trail of all state changes is required
- System needs resilience to downstream failures

---

## Architecture

```
Service A (produces events)
    │
    ▼
SNS Topic
    │
    ├── SQS Queue → Service B (consumer 1)
    ├── SQS Queue → Service C (consumer 2)
    └── SQS Queue → Audit Service (log all events)
```

---

## Key Patterns

- **Fan-out**: SNS topic → multiple SQS queues (one per consumer)
- **At-least-once delivery**: SQS guarantees delivery, consumers are idempotent
- **Dead-letter queue**: Failed messages go to DLQ for investigation
- **Event schema registry**: Contracts documented, versioned

---

## Key Decisions

| Decision | Choice |
|----------|--------|
| Event bus | SNS (fan-out) + SQS (queues) |
| Ordering | SQS FIFO (per entity) when order matters |
| Schema | JSON with eventId, eventType, timestamp, payload |
| Idempotency | Processed eventId stored in DB |
| Monitoring | CloudWatch on queue depth, DLQ alerts |

---

## Scaling

- Producers: Scale independently
- Consumers: Scale on queue depth (messages visible)
- Queue: SQS scales automatically (no provisioning)

---

## Risks

- Eventually consistent (not real-time for user)
- Duplicate messages → idempotent consumers required
- Message ordering → use FIFO queues for critical paths
- DLQ accumulation → alerting + runbook
