# Business Architecture Module

> Architecture Agent — Business Goal Alignment and Domain-Driven Design

---

## Purpose

Architecture must start with business goals, not technology. This module ensures
every design traces back to business outcomes, and service boundaries align with
business domains — not technical convenience.

---

## Business Architecture Flow

```
Business Goal
    ↓
Business Capabilities
    ↓
Business Processes
    ↓
Domain Discovery (DDD)
    ↓
Bounded Contexts
    ↓
Application Architecture
    ↓
Technology Architecture
```

---

## Step 1: Business Goals

Before any technical design, articulate:

| Question | Answer |
|----------|--------|
| What business problem are we solving? | |
| Who is the target user? | |
| What does success look like? (metric) | |
| What is the competitive advantage? | |
| What is the revenue model? | |
| What is the timeline to value? | |

### ILR Example

```
Goal: Help users obtain UK ILR faster and with higher success rates.
User: UK immigration applicants.
Success metric: 50% reduction in application preparation time.
Competitive advantage: AI-powered document validation and checklist guidance.
Revenue: Subscription SaaS (per-application fee).
Timeline: MVP in 3 months, production in 6 months.
```

---

## Step 2: Business Capabilities

Identify what the system must *do* at a business level (not how):

```
ILR Platform Capabilities:
├── Identity Management
│   ├── Register users
│   ├── Authenticate users
│   └── Manage profiles
├── Document Management
│   ├── Upload documents
│   ├── Classify documents
│   ├── Extract information
│   └── Validate documents
├── Application Tracking
│   ├── Create application
│   ├── Track checklist progress
│   ├── Generate recommendations
│   └── Produce summary report
├── Knowledge Management
│   ├── Store immigration guidance
│   ├── Search precedents
│   └── Generate contextual advice
├── Communication
│   ├── Send notifications
│   ├── Track delivery
│   └── In-app messaging
└── Administration
    ├── Caseworker review queue
    ├── System configuration
    └── Reporting and analytics
```

---

## Step 3: Business Processes

Map key user journeys as business processes:

### Process: Document Submission and Analysis

```
1. User selects document type from checklist
2. User uploads document (photo or PDF)
3. System acknowledges receipt
4. System processes document (async)
5. System displays results (classification + extracted data)
6. User confirms or corrects
7. Checklist updates automatically
8. User receives guidance on next steps
```

### Process: Application Preparation

```
1. User creates new ILR application
2. System presents personalised checklist
3. User uploads required documents (iterative)
4. System validates and tracks progress
5. System generates recommendations
6. User reviews final summary
7. User downloads prepared application pack
```

---

## Step 4: Domain Discovery (DDD)

### Identifying Domains

Use these heuristics:
- Different business capability = likely different domain
- Different rate of change = separate domain
- Different stakeholder/expert = separate domain
- Different data lifecycle = separate domain

### ILR Domains

| Domain | Core Concept | Domain Expert |
|--------|-------------|---------------|
| Identity | User, Profile, Authentication | Security team |
| Application | Application, Checklist, Status | Product team |
| Document | Document, Metadata, Classification | AI/Document team |
| Processing | Pipeline, Stage, Result, Confidence | AI/Engineering |
| Knowledge | Guidance, Precedent, Embedding | Legal/Content team |
| Notification | Message, Channel, Delivery | Product team |
| Audit | Event, Actor, Action | Compliance team |

---

## Step 5: Bounded Contexts

Each domain has a bounded context — a clear boundary within which terms have
specific meaning.

```
┌─────────────────────────────────────────────────────────────────┐
│ Identity Context                                                │
│   User (id, email, role, profile)                              │
│   Authentication, Authorization                                 │
└─────────────────────────────────────────────────────────────────┘
         │ (userId reference only)
         ▼
┌─────────────────────────────────────────────────────────────────┐
│ Application Context                                             │
│   Application (id, userId, status, checklist)                  │
│   ChecklistItem (id, applicationId, documentType, satisfied)   │
│   "User" here = just userId, no profile details                │
└─────────────────────────────────────────────────────────────────┘
         │ (applicationId reference)
         ▼
┌─────────────────────────────────────────────────────────────────┐
│ Document Context                                                │
│   Document (id, applicationId, type, status, fileKey)          │
│   DocumentMetadata (extractedFields, confidence)               │
│   "Application" here = just applicationId                      │
└─────────────────────────────────────────────────────────────────┘
         │ (documentId via events)
         ▼
┌─────────────────────────────────────────────────────────────────┐
│ Processing Context                                              │
│   Pipeline (id, documentId, stages, currentStage)              │
│   StageResult (agentName, output, confidence)                  │
│   "Document" here = just documentId + raw content              │
└─────────────────────────────────────────────────────────────────┘
```

### Bounded Context Rules

- Each context owns its data (no shared database tables)
- Communication between contexts: REST (sync) or Events (async)
- Shared identifiers only (userId, applicationId, documentId) — not objects
- Each context has its own ubiquitous language

---

## Step 6: Ubiquitous Language

Define key terms per domain to prevent ambiguity:

| Term | In Application Context | In Document Context | In Processing Context |
|------|----------------------|--------------------|-----------------------|
| "Document" | A checklist requirement | A file with metadata | An input for AI pipeline |
| "Status" | Application lifecycle state | Upload/processing state | Pipeline stage state |
| "Validation" | Checking checklist completion | Checking document authenticity | AI validation stage output |

---

## Step 7: Domain Events

Events that cross bounded context boundaries:

| Event | Published By | Consumed By |
|-------|-------------|-------------|
| `application.created` | Application Context | Notification |
| `document.uploaded` | Document Context | Processing, Audit |
| `document.processed` | Processing Context | Document, Application |
| `checklist.updated` | Application Context | Notification |
| `application.complete` | Application Context | Notification, Audit |

---

## Step 8: Aggregates

Each domain has a root aggregate — the consistency boundary:

| Domain | Aggregate Root | Contains |
|--------|---------------|----------|
| Application | `Application` | ChecklistItems, Status, Timeline |
| Document | `Document` | Metadata, Classifications, Extractions |
| Processing | `Pipeline` | Stages, Results, State |
| Identity | `User` | Profile, Roles, Sessions |

Rules:
- Transactions only within one aggregate
- Cross-aggregate communication via events
- Reference between aggregates by ID only

---

## Business-to-Architecture Traceability

```
Business Goal: Reduce application prep time by 50%
    ↓
Business Capability: Validate Documents Automatically
    ↓
Domain: Processing
    ↓
Bounded Context: Processing Context
    ↓
Service: AI Orchestrator
    ↓
Technology: Spring Boot + SQS + Bedrock
```

Every technology choice traces back to a business capability.

---

## C4 Model Output

### Level 1: System Context

```
[Applicant] ──── uses ────▶ [ILR Platform]
                                    │
                    ┌───────────────┼───────────────┐
                    ▼               ▼               ▼
            [Gov.uk Guidance]  [AWS Bedrock]  [AWS Textract]
            (knowledge source) (AI inference) (OCR)
```

### Level 2: Container Diagram

```
[React SPA] ──▶ [API Gateway]
                      │
          ┌───────────┼───────────┬───────────┐
          ▼           ▼           ▼           ▼
   [User Service] [App Service] [Doc Service] [AI Orchestrator]
       │               │              │              │
       ▼               ▼              ▼              ▼
   [Users DB]    [Apps DB]      [Docs DB]     [Processing DB]
                                  [S3]           [SQS]
```

### Level 3: Component Diagram (per service)

```
Document Service
├── DocumentController (REST API)
├── DocumentService (business logic)
├── DocumentRepository (data access)
├── S3Client (file storage)
└── EventPublisher (publishes document.uploaded)
```

---

## Checklist

- [ ] Business goals articulated (not just features)
- [ ] Business capabilities mapped
- [ ] Key business processes documented
- [ ] Domains identified using DDD heuristics
- [ ] Bounded contexts defined with clear boundaries
- [ ] Ubiquitous language documented per context
- [ ] Domain events identified (cross-context)
- [ ] Aggregates defined per domain
- [ ] Business-to-architecture traceability shown
- [ ] C4 diagrams at Level 1 and Level 2 minimum
- [ ] Every service traces back to a business capability
