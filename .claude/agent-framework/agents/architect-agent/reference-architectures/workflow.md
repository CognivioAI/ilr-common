# Reference Architecture: Workflow / State Machine

> Long-running processes with human approvals, waits, and branching.

---

## When to Use

- Process spans minutes, hours, or days
- Human approval or input needed mid-process
- Process has branching logic (decisions)
- Need to resume from last successful step after failure
- State and progress visible to users

---

## Architecture

```
API Request → Create Workflow Instance
                    │
                    ▼
            State: STEP_1
                    │ (execute)
                    ▼
            State: WAITING_FOR_HUMAN
                    │ (wait)
                    ▼
            Human Action (via API)
                    │ (resume)
                    ▼
            State: STEP_2
                    │ (execute with branch)
                    ├── Path A → STEP_3A
                    └── Path B → STEP_3B
                    │
                    ▼
            State: COMPLETE
```

---

## Technology Options

| Option | When |
|--------|------|
| Custom state machine (Spring Boot) | Complex domain logic, full testability |
| AWS Step Functions | AWS-native, visual designer, short workflows |
| Temporal | Complex, long-running, testable, language SDKs |

---

## Key Decisions

| Decision | Choice |
|----------|--------|
| State persistence | PostgreSQL (workflow_instances table) |
| State transitions | Enum-based state machine |
| Wait/resume | API trigger (human action) |
| Timeouts | Scheduled check (cron or CloudWatch Events) |
| Notifications | On every state transition |

---

## Failure Recovery

- State persisted after every step
- On crash: resume from last persisted state
- On timeout: escalate or auto-advance
- On error: retry (configurable per step) → DLQ → manual
