# Event-Driven Design Module

> Architecture Agent — Event Design Discipline

---

## Purpose

Define when and how services communicate asynchronously through events.
For every event, the Architecture Agent documents: name, payload, producers,
consumers, and idempotency strategy.

---

## Decision: Sync vs Async

| Use Synchronous (REST) When | Use Asynchronous (Events) When |
|-----------------------------|---------------------------------|
| User needs result immediately | Work can be deferred |
| Simple CRUD within one service | Multiple services need to know |
| Query that reads data | Long-running process |
| Result needed for current request | Fire-and-forget notification |

---

## Event Naming Convention

Past tense. Noun + Verb. Namespaced by domain.

```
document.uploaded
document.classification.completed
document.extraction.completed
document.validation.completed
application.checklist.updated
notification.email.sent
audit.event.recorded
```

---

## Event Catalog Template

For every event, produce this:

| Field | Value |
|-------|-------|
| Name | `document.classification.completed` |
| Producer | AI Orchestrator |
| Consumers | Document Service, Audit Service |
| Trigger | When AI classification agent returns a result |
| Payload | See below |
| Idempotency Key | `eventId` |
| Ordering | Per document (partition by `documentId`) |
| Delivery | At-least-once |
| Infrastructure | SNS → SQS fan-out |

### Payload

```json
{
  "eventId": "evt-uuid",
  "eventType": "document.classification.completed",
  "timestamp": "2024-01-15T10:30:00Z",
  "documentId": "doc-uuid",
  "applicationId": "app-uuid",
  "classification": {
    "type": "PASSPORT",
    "confidence": 0.97
  },
  "modelVersion": "claude-3-haiku-20240307"
}
```

---

## ILR Application Event Flow

```
document.uploaded
    ↓ (SNS → SQS)
document.ocr.started
    ↓
document.ocr.completed
    ↓
document.classification.completed
    ↓
document.extraction.completed
    ↓
document.validation.completed
    ↓
application.checklist.updated
    ↓
notification.progress.updated
```

### Event Flow Diagram

```
User uploads → Document Service → publishes: document.uploaded
                                                    │
                    ┌───────────────────────────────┤
                    ▼                               ▼
              AI Orchestrator                  Audit Service
              (starts processing)              (logs event)
                    │
                    ├── OCR Agent → publishes: document.ocr.completed
                    ├── Classification Agent → publishes: document.classification.completed
                    ├── Extraction Agent → publishes: document.extraction.completed
                    └── Validation Agent → publishes: document.validation.completed
                                                    │
                                                    ▼
                                            Checklist Service
                                            (updates checklist)
                                                    │
                                                    ▼
                                        application.checklist.updated
                                                    │
                                                    ▼
                                            Notification Service
                                            (notify user)
```

---

## Infrastructure

| Component | AWS Service | Purpose |
|-----------|-------------|---------|
| Fan-out | SNS | Publish event to multiple queues |
| Queue | SQS | Reliable delivery, retry, DLQ |
| Dead Letter Queue | SQS DLQ | Failed event storage |
| Event ordering | SQS FIFO | When order matters (per entity) |

---

## Idempotency

Every consumer must handle duplicate events safely.

Strategy:
1. Store processed `eventId` in database
2. Check before processing
3. Use UPSERT operations where possible

```java
@SqsListener("document-events")
public void handle(DocumentClassificationCompletedEvent event) {
    if (processedEventRepository.exists(event.eventId())) {
        log.info("Duplicate event ignored eventId={}", event.eventId());
        return;
    }
    // Process
    documentService.updateClassification(event.documentId(), event.classification());
    processedEventRepository.mark(event.eventId());
}
```

---

## Retry and Dead Letter Queue

```
Event → SQS Queue → Consumer
              ↓ (failure)
         Retry (3 attempts, exponential backoff)
              ↓ (still fails)
         Dead Letter Queue
              ↓
         Alert + manual investigation
```

---

## Checklist

- [ ] All events named in past tense
- [ ] Event catalog with payload schemas
- [ ] Producers and consumers identified
- [ ] SNS/SQS topology defined
- [ ] Idempotency strategy per consumer
- [ ] DLQ configured for every queue
- [ ] Retry policy defined (max attempts, backoff)
- [ ] Event ordering needs assessed
- [ ] Alerts on DLQ depth
