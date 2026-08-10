# Release Management

## Purpose

Define versioning strategy, changelog practices, release approval workflows, and coordinated deployment.

---

## Versioning Strategy

### Semantic Versioning (SemVer)

```
MAJOR.MINOR.PATCH

Examples:
  1.0.0 → First production release
  1.1.0 → New feature (backward compatible)
  1.1.1 → Bug fix
  2.0.0 → Breaking change
```

### Version Rules

| Change Type | Version Bump | Example |
|-------------|-------------|---------|
| Breaking API change | MAJOR | `1.x.x → 2.0.0` |
| New feature (backward compatible) | MINOR | `1.1.x → 1.2.0` |
| Bug fix | PATCH | `1.1.1 → 1.1.2` |
| Security fix | PATCH (fast-track) | `1.1.1 → 1.1.2` |
| AI model/prompt change | MINOR | `1.1.x → 1.2.0` |

---

## Release Process

### Standard Release

```
Feature branches
    │
    ▼
Pull Request → Review → Merge to main
    │
    ▼
CI builds → Tests pass → Security scan
    │
    ▼
Deploy to dev (automatic)
    │
    ▼
Deploy to staging (automatic after dev passes)
    │
    ▼
Release candidate tagged (vX.Y.Z-rc.1)
    │
    ▼
QA validation in staging
    │
    ▼
Release approved (manual gate)
    │
    ▼
Tag release (vX.Y.Z)
    │
    ▼
Deploy to production
    │
    ▼
Post-release validation
```

### Hotfix Release

```
Production issue detected
    │
    ▼
Hotfix branch from main (hotfix/description)
    │
    ▼
Fix + minimal tests
    │
    ▼
Fast-track review (1 reviewer)
    │
    ▼
Deploy to staging → Quick validation
    │
    ▼
Deploy to production (expedited approval)
    │
    ▼
Merge back to main
```

---

## Release Approval

```yaml
approval_matrix:
  patch_release:
    approvers: 1 (any team member)
    testing: unit + integration + smoke
    lead_time: same day
  
  minor_release:
    approvers: 1 (team lead or senior)
    testing: full test suite + staging validation
    lead_time: 1 day
  
  major_release:
    approvers: 2 (team lead + engineering manager)
    testing: full suite + performance + security + UAT
    lead_time: 1 week planning
  
  hotfix:
    approvers: 1 (any senior)
    testing: targeted tests + smoke
    lead_time: < 2 hours
```

---

## Changelog

### Format (Keep a Changelog)

```markdown
# Changelog

## [1.2.0] - 2025-01-15

### Added
- Document AI classification with confidence scoring
- Bulk document upload endpoint

### Changed
- Improved processing queue throughput (2x)
- Updated AI model to Claude 3 Sonnet

### Fixed
- Queue timeout on large documents (> 50MB)
- Memory leak in document parser

### Security
- Updated Spring Boot to 3.2.1 (CVE-2024-XXXX)
```

### Automation

```yaml
changelog_automation:
  tool: conventional-commits + auto-changelog
  
  commit_format:
    feature: "feat: add document classification"
    fix: "fix: resolve queue timeout"
    breaking: "feat!: change API response format"
    security: "security: update Spring Boot"
  
  generation:
    trigger: on release tag
    output: CHANGELOG.md (auto-generated from commits)
```

---

## Release Communication

### Internal

```yaml
internal_communication:
  before_release:
    - Slack #releases: "Deploying v1.2.0 to production at 14:00"
    - Include: what's changing, expected duration, rollback plan
  
  after_release:
    - Slack #releases: "v1.2.0 deployed successfully"
    - Include: changelog link, monitoring dashboard link
  
  if_issues:
    - Slack #incidents: "Rolling back v1.2.0 due to [issue]"
    - Follow incident management process
```

### External (If Customer-Facing)

```yaml
external_communication:
  breaking_changes:
    notice: 2 weeks before deployment
    channel: email + in-app notification
  
  new_features:
    notice: release notes published same day
    channel: release notes page + email digest
  
  maintenance:
    notice: 48 hours before
    channel: status page + email
```

---

## Release Cadence

```yaml
release_cadence:
  standard: weekly (every Tuesday)
  
  rationale:
    - Tuesday: avoid Monday issues and Friday deploys
    - Weekly: small batches, lower risk
    - Consistent: team expects and plans for it
  
  exceptions:
    - Hotfixes: deploy anytime
    - Security patches: deploy immediately
    - Feature flags: no deploy needed (toggle in config)
  
  freeze_periods:
    - Company holidays
    - Major customer events
    - Friday afternoon (no non-critical deploys after 15:00)
```

---

## Feature Flags & Release Decoupling

```yaml
feature_flags:
  principle: "Deploy != Release"
  
  workflow:
    1. Deploy code with feature behind flag (OFF)
    2. Enable flag for internal team (test in production)
    3. Enable flag for 5% of users (canary)
    4. Enable flag for 100% (full release)
    5. Remove flag after stable (2 weeks)
  
  benefits:
    - Deploy anytime without risk
    - Test in production safely
    - Instant rollback (disable flag)
    - Decouple deploy from business release
```
