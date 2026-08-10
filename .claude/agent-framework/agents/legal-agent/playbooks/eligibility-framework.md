# Playbook: Eligibility Framework (Per Route)

Structures each ILR route's eligibility requirements so the agent works systematically
through them rather than reasoning free-form. This is the **rule layer only** — the
requirements a route imposes, cited from the Rules/guidance. Applying it to an actual
applicant's facts is `playbooks/evidence-assessment.md`'s job, in case mode
(`reasoning/immigration-analysis-framework.md`), never this file's.

## Method

For any route, produce a requirements table before doing anything else:

```
Route: [name, e.g. "10-year Long Residence"]
Rule citation: [Immigration Rules paragraph, e.g. Appendix/Part reference —
                research/citation-verification.md]

| # | Requirement | Rule text (summarized, cited) | Typical evidence category |
|---|-------------|-------------------------------|----------------------------|
| 1 | ...         | ...                             | ...                         |
| 2 | ...         | ...                             | ...                         |
```

Every row must be cited — if a requirement's current wording can't be verified against a
primary source, mark the row `[UNVERIFIED — needs confirmation]` rather than filling it in
from memory (`research/citation-verification.md`).

## Cross-check discipline

Before treating any requirement as settled, cross-check it against anything this platform
has **already verified**, rather than re-deriving from scratch:

- Continuous residence / absence-counting rules — the platform has previously verified
  that departure and return days themselves are excluded from an absence count (from
  Home Office caseworker guidance, not Appendix CR directly) — confirm this is still
  current before relying on it, don't silently assume it hasn't changed.
- The 10-year Long Residence route's core numeric thresholds (10 continuous lawful years;
  180-day-per-rolling-12-month absence limit) were verified against GOV.UK directly and
  implemented in the platform's own logic (KAN-143) — treat that as a starting point to
  re-verify against the current live page, not as a citation to copy without checking.

If the platform's encoded logic and a fresh check of the primary source disagree, flag the
discrepancy explicitly — don't silently prefer one over the other.

## Known route shapes to structure (verify current criteria for each before use — this
is a checklist of routes to cover, not a substitute for checking current rules)

- 10-year Long Residence
- 5-year routes (skilled worker, family, etc. — whichever the applicant's history suggests
  is relevant)
- Settlement via a partner/family route
- Any route-specific continuous-residence or absence-limit variant (thresholds differ by
  route — never assume the Long Residence route's 180-day figure applies elsewhere without
  checking)

## Output

A requirements table per route, feeding `playbooks/evidence-assessment.md` — this file
never itself states whether an applicant meets a requirement; it only states what the
requirement is.
