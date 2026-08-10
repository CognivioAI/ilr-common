# Reference Architecture: Modular Monolith

> Single deployable unit with internal module boundaries.

---

## When to Use

- MVP or early-stage product
- Team size 1–4 developers
- Domain boundaries not yet clear
- Need for fast iteration
- Budget-sensitive (< $500/month infra)

---

## Architecture

```
React SPA (S3 + CloudFront)
    │
    ▼
Spring Boot Application (single JAR)
    ├── Module: Users (controller, service, repository)
    ├── Module: Documents (controller, service, repository)
    ├── Module: Processing (service, workers)
    └── Module: Notifications (service)
    │
    ▼
PostgreSQL (single database, schema per module)
S3 (file storage)
```

---

## Key Patterns

- **Module boundaries**: Packages enforce boundaries (ArchUnit)
- **Internal events**: ApplicationEventPublisher (in-process)
- **Shared database**: Single PostgreSQL with logical schema separation
- **Single deployment**: One Docker image, one ECS task

---

## Key Decisions

| Decision | Choice |
|----------|--------|
| Deployment | ECS Fargate (single service) |
| Database | Single RDS PostgreSQL |
| Module communication | In-process method calls + Spring Events |
| Frontend | React SPA on CloudFront |
| AI | Direct Bedrock API calls from within application |

---

## Evolution Path

```
Monolith → Extract heaviest module → Add SQS → Microservices
```

Trigger to split: when a module needs different scaling or a different team.

---

## Cost

- ECS Fargate (1 task, 0.5 vCPU, 1GB): ~$30/month
- RDS PostgreSQL (db.t3.micro): ~$20/month
- S3: < $5/month
- CloudFront: < $5/month
- **Total: ~$60–100/month**
