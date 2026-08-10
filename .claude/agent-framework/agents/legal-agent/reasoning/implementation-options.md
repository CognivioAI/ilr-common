# Implementation Options

A legal analysis that ends at "here's the risk" is only half useful to an engineering team
— it needs to end at "here are the ways to build this, and what each one costs legally and
in effort." This module is what keeps `workflows/feature-legal-review.md` and
`workflows/architecture-legal-review.md` connected to actual decisions rather than abstract
risk commentary.

## Method

For every legal issue identified that has more than one viable way to build around it,
produce a graded options table — never a single "here's what to do."

```
Option A — [description]
  Legal risk: Low
  Engineering effort: High
  Why lower risk: [specific — e.g. avoids processing special-category data entirely]

Option B — [description]
  Legal risk: Medium
  Engineering effort: Medium
  Why medium risk: [specific]

Option C — [description]
  Legal risk: Higher
  Engineering effort: Low
  Why higher risk: [specific — e.g. relies on an unverified reading of silent ToS language]
```

## Rules

- **At least two options, ideally three**, spanning the risk/effort trade-off — a single
  "the compliant way to do this" output collapses the decision that should belong to
  product/engineering leadership plus counsel, not this agent.
- **State the legal risk difference concretely**, not just "lower risk" — name the specific
  obligation avoided, reduced, or still present in each option.
- **Effort is an engineering estimate, not a legal one** — flag it as a rough judgment call
  the calling agent (Architecture/Developer) should refine, not a committed estimate.
- **Never recommend the highest-risk option without an explicit red flag** — if Option C
  trips anything in `qa/red-flag-detector.md`, say so in the table itself, not buried in
  prose elsewhere.
- Pair every options table with `reasoning/legal-risk-scoring.md` scores per option so the
  comparison is quantified, not just qualitative.

## Where this feeds

`workflows/feature-legal-review.md`, `workflows/architecture-legal-review.md`, and
`workflows/change-risk-assessment.md` all end in an implementation-options table using this
structure. The named human decision-maker picks the option — this agent never picks for
them, only makes the trade-off visible.
