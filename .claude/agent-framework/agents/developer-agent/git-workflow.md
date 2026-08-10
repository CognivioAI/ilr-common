# Git Workflow

## Purpose

Define branching strategy, commit standards, PR process, and merge conventions for consistent delivery.

---

## Branching Strategy

```
main (production-ready, always deployable)
    │
    ├── feature/{ticket}-{description}    (new features)
    ├── bugfix/{ticket}-{description}     (bug fixes)
    ├── hotfix/{ticket}-{description}     (production fixes)
    └── refactor/{description}            (tech debt)
```

### Rules

```yaml
branches:
  main:
    protected: true
    merge_via: PR only (no direct push)
    reviews_required: 1
    ci_must_pass: true
    
  feature:
    source: main
    target: main (via PR)
    naming: "feature/{TICKET-ID}-{short-description}"
    example: "feature/DOC-123-document-upload"
    lifetime: "< 3 days (prefer < 1 day)"
    
  bugfix:
    source: main
    target: main (via PR)
    naming: "bugfix/{TICKET-ID}-{short-description}"
    example: "bugfix/DOC-456-fix-upload-timeout"
    
  hotfix:
    source: main
    target: main (via PR, expedited review)
    naming: "hotfix/{TICKET-ID}-{short-description}"
    example: "hotfix/DOC-789-fix-production-crash"
```

---

## Commit Standards

### Conventional Commits

```
<type>(<scope>): <description>

[optional body]

[optional footer(s)]
```

### Types

| Type | When to Use |
|------|------------|
| `feat` | New feature or capability |
| `fix` | Bug fix |
| `test` | Adding or updating tests |
| `refactor` | Code change that neither fixes nor adds feature |
| `docs` | Documentation changes |
| `style` | Formatting, whitespace (no logic change) |
| `perf` | Performance improvement |
| `chore` | Build, CI, tooling changes |

### Examples

```
feat(document): add document upload API endpoint

- POST /api/v1/documents accepts upload request
- Validates file size and type
- Returns 201 with document ID

Resolves: DOC-123
```

```
test(document): add upload integration tests

- Spock BDD spec with Testcontainers
- Covers happy path, validation errors, auth
```

```
fix(document): resolve timeout on large file uploads

Increased S3 client timeout from 5s to 30s for files > 10MB.
Added retry with backoff for transient S3 errors.

Fixes: DOC-456
```

### Rules

```yaml
commit_rules:
  atomic: "One logical change per commit (not one file per commit)"
  message: "Present tense, imperative mood ('add' not 'added')"
  scope: "Service or domain area (document, auth, classification)"
  body: "Explain WHY, not WHAT (the diff shows WHAT)"
  max_subject: 72 characters
  
  never:
    - "WIP" commits on main
    - "fix typo" without squashing into parent
    - Commits that break the build
    - Multiple unrelated changes in one commit
```

---

## Pull Request Process

### Before Creating PR

```yaml
pre_pr_checklist:
  ✓ Branch is up-to-date with main (rebase or merge)
  ✓ All tests pass locally
  ✓ Self-review completed (code-review-agent.md)
  ✓ Commits are clean (squash if needed)
  ✓ No debug code, console.log, or TODO hacks
  ✓ DevOps contract satisfied
```

### PR Template

```markdown
## Summary
[One paragraph: what this PR does and why]

## Changes
- [Significant change 1]
- [Significant change 2]
- [New endpoint: POST /api/v1/documents]

## Architecture Reference
- Design: [link to architecture decision]
- ADR: [link if applicable]

## Database
- [ ] No migration
- [x] Migration included: V3__add_document_type.sql
- [ ] Migration is backward-compatible

## Testing
- [x] Unit tests (Spock specs)
- [x] Integration tests (Testcontainers)
- [x] API tests (REST Assured)
- [ ] Performance tested
- [x] Tested locally with docker-compose

## DevOps Impact
- New environment variables: SQS_QUEUE_URL
- New infrastructure dependency: SQS queue
- Deployment notes: None (standard rolling deploy)

## Risks
Low — additive change, no modifications to existing endpoints.

## Screenshots / Evidence
[If UI change or relevant output]
```

### Review Expectations

```yaml
review:
  turnaround: "< 4 hours (business hours)"
  reviewer_focus:
    - Does it match the architecture design?
    - Are there security concerns?
    - Are tests sufficient?
    - Is it maintainable?
  
  reviewer_should_not:
    - Bikeshed on naming style (follow existing convention)
    - Request unnecessary abstraction
    - Block on nitpicks (suggest, don't block)
```

---

## Merge Strategy

```yaml
merge:
  strategy: "Squash and merge (feature branches)"
  commit_message: "feat(document): add document upload (#123)"
  
  rationale:
    - Clean history on main (one commit per feature)
    - Detailed commits preserved in PR (for archaeology)
    - Easy to revert (one commit to undo)
  
  exceptions:
    - Large refactors: merge commit (preserve logical steps)
    - Hotfixes: squash and merge (fast)
```

---

## Branch Hygiene

```yaml
hygiene:
  after_merge:
    - Delete feature branch (auto-delete enabled)
    - Verify CI passes on main
    - Verify deploy to dev succeeds
  
  stale_branches:
    - > 7 days without activity: notify author
    - > 14 days: auto-delete (unless marked as long-lived)
  
  conflicts:
    - Rebase onto main (preferred over merge)
    - If complex conflicts: discuss with team before resolving
```
