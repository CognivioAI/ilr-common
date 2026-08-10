# Release Gate

The final check before a `workflows/feature-legal-review.md`,
`workflows/architecture-legal-review.md`, or `workflows/change-risk-assessment.md` output
is presented as ready for implementation. Where `qa/legal-qa-gate.md` (Phase 1) checks that
an artifact's *content* is defensible (citations, jurisdiction, disclaimer), this gate
checks that the *review process itself* was actually completed — it's specific to the
Legal Review Engine's engineering-facing outputs.

## The checklist

```
[ ] Legal issues identified   — reasoning/issue-spotting.md run against the fact pattern
[ ] Sources verified          — every citation passes research/citation-verification.md
[ ] Contradictory authority considered — reasoning/adversarial-review.md's "which
                                 authority contradicts my position" question was actually
                                 answered, not skipped
[ ] Contracts reviewed        — any existing contract the change/feature touches was
                                 actually checked, not assumed compatible
[ ] Regulatory guidance checked — current guidance (not memory) consulted per
                                 research/legal-research-protocol.md
[ ] Engineering alternatives compared — reasoning/implementation-options.md table present
                                 with at least two options
[ ] Lowest-risk implementation identified — explicitly named, with its
                                 reasoning/legal-risk-scoring.md score
[ ] Residual risk documented  — stated even for the lowest-risk option; "zero risk" is
                                 almost never an honest answer, say what's left
[ ] Human review needed?      — answered explicitly, with the specific named role
                                 (scope-boundaries.md)
```

## How to run it

Same discipline as `qa/legal-qa-gate.md`: walk it against the actual draft, item by item,
fix what's missing rather than checking a box that isn't true. A release gate that can be
talked past isn't a gate — if a check can't honestly be marked complete, the artifact isn't
finished, regardless of time pressure.

## Relationship to qa/legal-qa-gate.md

Run **both** for any Legal Review Engine output: this gate confirms the review process was
complete; `qa/legal-qa-gate.md` confirms the resulting document is itself defensible
(citations, disclaimer, no hallucination). A feature-legal-review memo that passes this
gate but fails the other one (e.g. a fabricated citation slipped through) is not ready.
