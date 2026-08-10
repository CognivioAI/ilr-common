# Bug Fix Workflow (AI-DLC Abbreviated Pipeline)

> Version 2.0 | AI-DLC Engineering Handbook
> Purpose: Abbreviated pipeline for fixing defects.

---

## Pipeline

```
Bug Report
  ↓
Product Agent (triage + clarify)
  ↓ ─── Gate: reproduction steps clear, severity assigned
Architecture Agent (root cause analysis — if needed)
  ↓ ─── Gate: root cause identified, fix approach agreed
Java Agent / React Agent (implement fix)
  ↓ ─── Gate: compiles, standards met
Testing Agent (regression + verification)
  ↓ ─── Gate: all tests pass, regression test added
DevOps Agent (deploy fix)
  ↓ ─── Gate: staging verified, production healthy
✅ Resolved
```

---

## Stage 1: Triage (Product Agent)

### Actions

1. Clarify reproduction steps
2. Identify affected users and impact
3. Assess severity (P0–P3)
4. Define expected vs actual behavior
5. Identify related user stories

### Gate

- [ ] Clear reproduction steps
- [ ] Severity assigned
- [ ] Expected behavior documented

---

## Stage 2: Root Cause (Architecture Agent)

> Skip this stage for obvious, isolated bugs.

### Actions

1. Trace data flow to locate failure
2. Identify whether it's design flaw or implementation bug
3. Assess blast radius (other affected features)
4. Recommend fix approach
5. Flag if architecture change needed

### Gate

- [ ] Root cause identified
- [ ] Fix approach documented
- [ ] Blast radius assessed

---

## Stage 3: Fix (Java Agent / React Agent)

### Actions

1. Implement the fix following coding standards
2. Add defensive checks to prevent recurrence
3. Ensure backward compatibility
4. Update documentation if needed

### Gate

- [ ] Code compiles/builds
- [ ] Follows coding standards
- [ ] No new lint warnings
- [ ] Fix is minimal and focused

---

## Stage 4: Verification (Testing Agent)

### Actions

1. Write regression test covering the exact bug scenario (Spock BDD)
2. Verify the fix resolves the issue
3. Run full test suite — no regressions
4. Verify related edge cases

### Gate

- [ ] Regression test written and passes
- [ ] Full test suite passes
- [ ] No coverage decrease
- [ ] Bug's acceptance criteria verified

---

## Stage 5: Deploy (DevOps Agent)

### Actions

1. Deploy to staging
2. Run smoke tests
3. Verify bug is fixed in staging
4. Deploy to production
5. Monitor golden signals for 15 minutes
6. Close the bug

### Gate

- [ ] Staging deployment successful
- [ ] Bug verified fixed in staging
- [ ] Production deployment successful
- [ ] No error rate spike post-deploy

---

## Severity-Based Routing

| Severity | Pipeline Mode | SLA |
|----------|--------------|-----|
| P0 (critical) | Hotfix — skip to Java Agent | Fix in < 2 hours |
| P1 (high) | Abbreviated — skip Architecture | Fix in < 8 hours |
| P2 (medium) | Full bug-fix pipeline | Fix in < 2 days |
| P3 (low) | Backlog — next sprint | Fix in < 1 week |
