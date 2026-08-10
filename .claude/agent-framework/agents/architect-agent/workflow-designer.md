# Workflow Designer Module

> Architecture Agent — Long-Running Workflow Design Discipline

---

## Purpose

Long-running processes (minutes, hours, or days) that involve async AI,
human approvals, waits, and branching need proper workflow design.
This module defines when to use queues vs orchestration vs state machines.

---

## When to Use What

| Pattern | When |
|---------|------|
| Simple queue (SQS) | Single async task, no branching, no waiting |
| Queue chain | Multi-step, linear, no human interaction |
| State machine | Complex branching, retries, wait states |
| Workflow engine (Step Functions) | Multi-step with human approval, long waits, timeouts |
| Custom orchestrator (Spring Boot) | Full control, complex domain logic, testable |

---

## Decision Guide

```
Is the workflow a single async task?
  → Yes → SQS queue

Does it have multiple steps in sequence?
  → Yes → Does it need to wait for humans?
      → No → Queue chain (SQS → Lambda/Worker → SQS → ...)
      → Yes → Does it need branching/decisions?
          → No → Step Functions (simple workflow)
          → Yes → Step Functions OR Custom orchestrator

Is the domain logic complex and needs unit testing?
  → Yes → Custom orchestrator (Spring Boot + status tracking)

Is the workflow extremely long (days/weeks)?
  → Yes → Custom state machine (persisted status) + event-driven
```

---

## ILR Application Workflow Example

```
Applicant creates application
    │
    ▼
Status: DRAFT
    │ (user fills profile)
    ▼
Status: DOCUMENTS_REQUIRED
    │ (user uploads documents)
    ▼
Document uploaded → AI Pipeline triggered (async)
    │
    ▼
Status: PROCESSING
    │ (AI pipeline runs stages 1–8)
    │
    ├── If AI confidence HIGH → Status: AWAITING_REVIEW
    │       │
    │       ▼ (caseworker reviews)
    │       Status: APPROVED or REQUIRES_MORE_DOCUMENTS
    │
    └── If AI confidence LOW → Status: MANUAL_REVIEW_REQUIRED
            │
            ▼ (caseworker manually processes)
            Status: APPROVED or REQUIRES_MORE_DOCUMENTS

If REQUIRES_MORE_DOCUMENTS:
    → Notify user → Wait for upload → Restart from DOCUMENTS_REQUIRED

If APPROVED:
    → Generate decision letter → Notify user → Status: COMPLETE
```

---

## State Machine Design

```java
public enum ApplicationStatus {
    DRAFT,
    DOCUMENTS_REQUIRED,
    PROCESSING,
    AWAITING_REVIEW,
    MANUAL_REVIEW_REQUIRED,
    REQUIRES_MORE_DOCUMENTS,
    APPROVED,
    REJECTED,
    COMPLETE;

    public Set<ApplicationStatus> allowedTransitions() {
        return switch (this) {
            case DRAFT -> Set.of(DOCUMENTS_REQUIRED);
            case DOCUMENTS_REQUIRED -> Set.of(PROCESSING);
            case PROCESSING -> Set.of(AWAITING_REVIEW, MANUAL_REVIEW_REQUIRED);
            case AWAITING_REVIEW -> Set.of(APPROVED, REJECTED, REQUIRES_MORE_DOCUMENTS);
            case MANUAL_REVIEW_REQUIRED -> Set.of(APPROVED, REJECTED, REQUIRES_MORE_DOCUMENTS);
            case REQUIRES_MORE_DOCUMENTS -> Set.of(DOCUMENTS_REQUIRED);
            case APPROVED -> Set.of(COMPLETE);
            case REJECTED, COMPLETE -> Set.of();
        };
    }
}
```

---

## Workflow Persistence

For long-running workflows, persist state:

```sql
CREATE TABLE processing_jobs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id     UUID NOT NULL REFERENCES documents(id),
    workflow_type   VARCHAR(30) NOT NULL,
    current_stage   VARCHAR(50) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    started_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    completed_at    TIMESTAMP,
    last_stage_at   TIMESTAMP,
    retry_count     INT NOT NULL DEFAULT 0,
    error_message   TEXT,
    context         JSONB       -- Accumulated results from each stage
);
```

---

## Wait / Resume Patterns

When a workflow needs to wait for human action:

```
Process runs stages 1–5 → arrives at decision point
    ↓
Status set to AWAITING_REVIEW
    ↓
Workflow PAUSES (no active processing)
    ↓
Human acts (through UI → API call)
    ↓
API triggers workflow RESUME
    ↓
Workflow continues from saved state
```

---

## Timeout Handling

| Wait Type | Timeout | On Timeout |
|-----------|---------|-----------|
| User document upload | 14 days | Remind → 7 more days → close |
| Caseworker review | 48 hours | Escalate to senior caseworker |
| AI processing | 5 minutes | Retry → DLQ → flag for manual |
| Human confirmation | 24 hours | Auto-accept if confidence ≥ 90% |

---

## Notification at State Transitions

| Transition | Notification |
|-----------|-------------|
| → PROCESSING | "Your documents are being analysed" |
| → AWAITING_REVIEW | "Analysis complete, under review" |
| → REQUIRES_MORE_DOCUMENTS | "Action needed: please upload X" |
| → APPROVED | "Congratulations, your application is approved" |
| → REJECTED | "Application update — see details" |

---

## Checklist

- [ ] Workflow states defined (state machine diagram)
- [ ] Valid transitions documented
- [ ] Wait/resume patterns designed
- [ ] Timeouts defined per wait state
- [ ] Notifications mapped to transitions
- [ ] Workflow state persisted (database)
- [ ] Retry and failure handling per stage
- [ ] Escalation paths for stalled workflows
- [ ] Status visible to user at all times
