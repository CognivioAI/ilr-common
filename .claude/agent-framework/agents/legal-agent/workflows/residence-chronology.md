# Residence Chronology

Many ILR requirements turn entirely on dates — continuous residence, absence limits,
qualifying periods. Reconstructing an accurate chronology is one of the highest-value,
lowest-risk things this agent can do: it's arithmetic and fact-organization applied to
dates the applicant has already provided, not a legal judgment. Still built on
`workflows/chronology-and-precedent-extraction.md`'s chronology structure, specialized for
residence/absence data.

## What to reconstruct

- Visas/permissions held, with start and end dates
- Entry and exit dates (from passport stamps, travel records, or other evidence supplied)
- Resulting periods of continuous residence in the UK
- Gaps between visas/permissions, if any
- Individual absences (date out → date back), with duration
- Employment history where the route's requirements are employment-linked

## Method

1. Lay out every dated event in chronological order, source-tagged
   (`templates/chronology.md` structure).
2. Calculate each absence's duration. Apply the **currently verified** counting rule for
   the relevant route — cross-check against what the platform has already verified
   (`playbooks/eligibility-framework.md`'s cross-check note) rather than assuming a rule
   from memory; state which counting method was applied and why.
3. Sum absences against the route's rolling-window limit (e.g. 180 days per rolling 12
   months for the Long Residence route) and show the running total, not just the final
   number — this lets a reviewer spot exactly which absence pushed a window over the
   limit, if any did.
4. Flag any gap in the record — a period with no visa/permission evidence, or a date that
   doesn't reconcile with an adjacent entry — as a factual observation, not a legal
   conclusion.

## What this workflow does NOT do

State whether the reconstructed chronology means the residence requirement is met. That is
exactly the case-mode restriction in `reasoning/immigration-analysis-framework.md` — this
workflow produces the chronology and the running absence totals as **facts**; whether those
facts satisfy the Rules is `playbooks/evidence-assessment.md` / `playbooks/refusal-risk-
analysis.md` territory, phrased as gaps and questions, never as a conclusion.

## Output shape

```
[templates/chronology.md table, specialized:]

| Date | Event | Type (visa/entry/exit/employment) | Running absence total (rolling 12mo) | Source |
|------|-------|-------------------------------------|----------------------------------------|--------|
| ...  | ...   | ...                                 | ...                                    | ...    |

Gaps in the record: [listed]
Absence-limit method applied: [named, with citation/cross-check note]
Closest approach to any relevant limit: [stated as a fact — e.g. "the 12-month window
  ending [date] shows a running total of 165 of 180 days" — not "this is fine" or "this
  is a problem"]
```

## Cross-references

`playbooks/eligibility-framework.md`, `playbooks/evidence-assessment.md`,
`playbooks/refusal-risk-analysis.md`, `templates/chronology.md`.
