# Chronology Building & Precedent Extraction

Two related workflows: laying out how a legal position changed over time, and pulling a
usable summary out of a single judgment.

## Chronology / timeline construction

Legal analysis is frequently date-driven — when a right accrued, when a limitation period
started, when guidance changed. Build a timeline whenever more than two dated events are
in play, in this shape:

```
[Year] — [Event: legislation entering force / guidance change / relevant judgment / fact
          in the matter] — [source citation]
    ↓
[Year] — [next event]
    ↓
...
    ↓
[Current date] — current position, stated explicitly
```

Use `templates/chronology.md` for the output structure. Every entry needs a citation
(`research/citation-verification.md`) or a plain "fact stated by the client/user, not yet
independently verified" tag if it's a fact rather than a legal event.

## Precedent extraction

Given a judgment (or a summary of one from a Level A/B source per
`reasoning/evidence-grading.md`), extract:

```
Facts             — the material facts the court actually decided on
Legal issue       — the question the court was asked to resolve
Holding           — what the court decided
Ratio             — the reasoning necessary to the decision (binding)
Obiter            — reasoning not necessary to the decision (persuasive only) — state
                     separately, don't blend into the ratio
Remedy            — what relief was granted
Key paragraphs    — paragraph numbers to cite directly, not the whole judgment
Applicability     — how this maps onto the current fact pattern
Distinguishable?  — the strongest argument the current facts differ, stated honestly even
                     if it weakens the point being made
```

## Why ratio/obiter separation matters

Citing obiter dicta as if it were binding is one of the more common ways secondary sources
(and LLMs) misstate what a case actually decided. If genuinely unsure which is which for a
given judgment, say so rather than guessing — `reasoning/uncertainty-and-confidence.md`
applies here directly.
