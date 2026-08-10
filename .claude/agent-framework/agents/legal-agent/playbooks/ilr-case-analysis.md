# Playbook: ILR Case Analysis

The orchestrating playbook for a UK ILR case-analysis request — ties together
`reasoning/immigration-analysis-framework.md` and the modules it calls into one working
method. This is the entry point for "help me understand where this application stands," and
it is **case mode** by definition (`reasoning/immigration-analysis-framework.md`) — every
output below stops short of an eligibility conclusion, by design.

## Trigger

Any request to analyze a specific ILR case's route, evidence, or readiness — as distinct
from a general question about how a rule works (route straight to
`playbooks/eligibility-framework.md` / `research/legal-research-protocol.md` for that,
rule mode, no case-mode restrictions apply).

## The method

1. **Run `qa/red-flag-detector.md` first.** Immigration/ethnic-origin-adjacent data is
   already a listed red flag for this platform — treat every case-analysis request as
   elevated scrutiny by default, not just when something unusual comes up.
2. **Identify the candidate route(s)** — `reasoning/immigration-analysis-framework.md`
   step 1, framed as "the facts suggest," never as a recommendation to apply under a
   specific route.
3. **Pull the requirements table** — `playbooks/eligibility-framework.md`, cited and
   cross-checked against the platform's own verified rules.
4. **Build the residence chronology** if the route is residence/absence-dependent —
   `workflows/residence-chronology.md`.
5. **Build the evidence map** — `playbooks/evidence-assessment.md` /
   `templates/evidence-map.md`. Status only (Provided/Partially provided/Not yet
   provided) — no sufficiency rating.
6. **Run refusal-risk analysis** on every gap the evidence map surfaces —
   `playbooks/refusal-risk-analysis.md`. Objections only, no outcome prediction.
7. **Adversarial pass** — `reasoning/adversarial-review.md`'s method still applies: what
   would a hostile reading of this evidence map find that a sympathetic one wouldn't?
   Surface it as an additional open question, not a new conclusion.
8. **Assemble the output** — evidence map + chronology + objections + gaps, with **no**
   overall "eligible/not eligible" verdict anywhere in the artifact.
9. **Name the escalation** — always: a qualified immigration adviser or OISC-regulated
   counsel should review the full picture before the applicant relies on it for anything.
   This is not a caveat added at the end; it is the structurally required outcome of case
   mode.

## Output structure

```markdown
# [Case reference/date] — ILR Case Analysis

Prepared by: Legal Agent (AI) — working draft, factual analysis, not legal advice.
Mode: Case mode

## Candidate route(s)
[stated as "the facts suggest," with reasoning]

## Requirements (playbooks/eligibility-framework.md)
[table]

## Residence chronology (if applicable)
[workflows/residence-chronology.md output]

## Evidence map (templates/evidence-map.md)
[table]

## Possible caseworker objections (playbooks/refusal-risk-analysis.md)
[list]

## Open questions / further evidence that would help
[list]

## Overall assessment
This agent has not reached, and does not offer, a conclusion on whether the applicant is
eligible or on the likely outcome of an application. The material above is intended to
help a qualified immigration adviser or the applicant's own counsel review the case
efficiently — it does not substitute for that review.

## Human review required
Yes — [named role if known, otherwise "a qualified immigration adviser / OISC-regulated
counsel"].

---
Prepared by the Legal Agent (AI) as a working draft — not legal advice. Requires review
and sign-off by a qualified immigration adviser before being relied upon.
```

## Cross-references

`reasoning/immigration-analysis-framework.md` (the reasoning sequence this playbook
executes), `playbooks/immigration-boundary.md` (the boundary this entire playbook is
built to respect), `qa/legal-qa-gate.md` and `qa/red-flag-detector.md` (both still run).
