# Reference Architecture: Hybrid

> Combine patterns: containers for core services + serverless for events.

---

## When to Use

- Most logic fits containers (Spring Boot)
- Some tasks are better as serverless (event-driven, short-lived)
- Want low-ops for non-critical paths (notifications, thumbnails)
- Core business logic needs full control and testability

---

## Architecture

```
Client → ALB → Spring Boot Services (EKS/ECS)
                    │
                    ├── Core business logic (containers)
                    │
                    └── SQS → Lambda (async lightweight tasks)
                                ├── Thumbnail generation
                                ├── Email notifications
                                ├── Webhook delivery
                                └── Scheduled reports
```

---

## Key Principle

Use containers when:
- Logic is complex and needs unit testing
- Long-running (> 15 min)
- Needs persistent connections (WebSocket, DB pool)

Use Lambda when:
- Event-triggered, short-lived (< 5 min)
- Simple input/output transformation
- Infrequent execution (cost-per-use)
- No complex state management

---

## Cost

- ECS/EKS for core: $200–500/month
- Lambda for periphery: $5–20/month
- **Best of both**: control where it matters, cheap where it doesn't
