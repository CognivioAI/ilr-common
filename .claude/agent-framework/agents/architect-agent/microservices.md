# Microservices Module

> Architecture Agent — Microservice Boundary Discipline

---

## Purpose

Define how the Architecture Agent determines microservice boundaries,
ownership rules, and inter-service communication patterns.

---

## Service Boundary Principles

A microservice should own:
- **One business capability** (not one technical layer)
- **Its own database** (no shared databases)
- **Its own API** (versioned, documented)
- **Its own business logic** (no shared libraries containing logic)

A microservice should **not**:
- Share a database with another service
- Call another service's database directly
- Own responsibilities that belong to a different capability

---

## Identifying Boundaries

Use these signals to draw boundaries:

| Signal | Meaning |
|--------|---------|
| Different rate of change | Likely separate service |
| Different team ownership | Likely separate service |
| Different scaling requirements | Likely separate service |
| Different data models | Likely separate service |
| Natural language "and" in responsibility | Split it |

---

## Reference: ILR Application Services

```
┌─────────────────────────────────────────────┐
│              API Gateway                    │
│  Auth, routing, rate limiting, logging      │
└──────────────────┬──────────────────────────┘
                   │
       ┌───────────┼────────────────────┐
       ▼           ▼                    ▼
┌──────────┐ ┌──────────┐      ┌──────────────┐
│  User    │ │Document  │      │     AI       │
│ Service  │ │ Service  │      │ Orchestrator │
│          │ │          │      │              │
│ Auth     │ │ Upload   │      │ Classification│
│ Profiles │ │ Storage  │      │ OCR          │
│ Roles    │ │ Metadata │      │ Extraction   │
│ Sessions │ │ Status   │      │ Validation   │
└──────────┘ └──────────┘      └──────────────┘
       ▼           ▼                    ▼
┌──────────┐ ┌──────────┐      ┌──────────────┐
│Notification│ │ Audit  │      │  Knowledge   │
│ Service  │ │ Service  │      │   Service    │
│          │ │          │      │              │
│ Email    │ │ All events│     │ Guidance     │
│ SMS      │ │ Immutable│      │ Vector search│
│ In-app   │ │ Log      │      │ Precedents   │
└──────────┘ └──────────┘      └──────────────┘
```

### Service Responsibilities

| Service | Owns | Does Not Own |
|---------|------|-------------|
| Gateway | Routing, auth verification, rate limiting | Business logic |
| User Service | Identity, profiles, roles, sessions | Document data |
| Document Service | Upload, storage refs, metadata, processing status | User identity |
| AI Orchestrator | Agent coordination, confidence routing | Document storage |
| Notification Service | Message delivery | User or document business logic |
| Audit Service | Immutable event log | Any other business logic |
| Knowledge Service | Guidance content, vector search | User or document data |

---

## Database Ownership

| Service | Database | Why |
|---------|----------|-----|
| User Service | PostgreSQL | Relational, ACID-critical |
| Document Service | PostgreSQL | Metadata, status, relationships |
| AI Orchestrator | PostgreSQL (jobs) + S3 (outputs) | Structured jobs + binary outputs |
| Notification Service | PostgreSQL | Delivery tracking |
| Audit Service | PostgreSQL (append-only) | Immutable audit trail |
| Knowledge Service | PostgreSQL + pgvector | Structured + semantic search |

**No service reads another service's database.**
Cross-service data needs are satisfied by:
- Synchronous API call (queries)
- Asynchronous event (notifications)
- Event-sourced read model (reporting)

---

## Inter-Service Communication

| Pattern | When | Example |
|---------|------|---------|
| Synchronous REST | User needs result immediately | Get user profile during auth |
| Asynchronous SQS | Work that can be deferred | Document AI processing |
| Event SNS fan-out | Multiple consumers for same event | `DOCUMENT_UPLOADED` → OCR + Audit |
| Direct DB (never) | Never | — |

---

## Anti-Patterns

- ❌ "God service" — one service doing everything
- ❌ Shared database between services
- ❌ Chatty microservices (> 3 sync hops per request)
- ❌ Service that only wraps a database (CRUD service without logic)
- ❌ Splitting by technical layer (controller-service / repository-service)
- ❌ Circular service dependencies (A calls B calls A)

---

## Decision: Microservices vs Monolith

```
Start with a modular monolith IF:
  - Team is < 3 developers
  - MVP / validation phase
  - Domain boundaries are unclear
  - < 6 months to first production

Migrate to microservices WHEN:
  - Clear bounded contexts exist
  - Teams need independent deployability
  - Services have different scaling needs
  - Domain is well understood
```

Document this decision in an ADR.
