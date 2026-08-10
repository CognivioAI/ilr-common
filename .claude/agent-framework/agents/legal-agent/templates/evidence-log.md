# Template: Evidence Log

A running record of every source consulted for a matter, so a later reviewer (or a future
session of this agent) doesn't re-research what's already been checked. Complements
`templates/chronology.md` (dated events) and `templates/risk-register.md` (risk-specific).

```markdown
# [Matter] — Evidence Log

| # | Source (full citation) | Evidence Grade | Position on Source Hierarchy | Access Date | Verified Primary? | Used For |
|---|------------------------|----------------|-------------------------------|--------------|--------------------|----------|
| 1 | [title, year, section, URL] | A/B/C | [reasoning/source-hierarchy.md tier] | [date] | Yes/No | [which conclusion this supports] |
| ... | | | | | | |

## Sources considered but rejected
[Level C sources found but not relied upon, and why — useful so the next reviewer doesn't
re-surface the same blog post]

## Outstanding research gaps
[anything reasoning/legal-analysis-framework.md step 11/12 flagged as still needed]
```

## Rules

- Every row must satisfy `research/citation-verification.md`'s six required fields before
  the citation is usable elsewhere in the matter's artifacts — this log is where that check
  happens once, centrally, rather than being re-verified per-artifact.
- Keep this log per-matter, not per-artifact — a decision record, a DPIA, and a counsel
  brief for the same matter should all draw from one evidence log.
