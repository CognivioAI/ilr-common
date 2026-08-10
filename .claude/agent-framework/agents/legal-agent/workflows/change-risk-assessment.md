# Change Risk Assessment

For a proposed change to an *existing* behaviour, feature, or data flow — distinct from
`workflows/feature-legal-review.md` (new feature) and `workflows/architecture-legal-review.md`
(new architecture decision). The distinguishing question here is always "what's different,"
not "what's new."

## The sequence

```
Current behaviour
    ↓
Proposed change
    ↓
Legal differences        (what actually changes from a legal standpoint — not every
                           technical change has a legal difference, and not every small
                           technical change has a small legal difference)
    ↓
New obligations           (what the change creates that wasn't previously required)
    ↓
Removed obligations       (what the change makes no-longer-applicable — verify this
                           carefully; an obligation "no longer needed" is a claim that
                           itself needs evidence, not an assumption)
    ↓
New risks                 (reasoning/issue-spotting.md against the new state)
    ↓
Mitigations               (what reduces each new risk)
    ↓
Residual risk              (reasoning/legal-risk-scoring.md on what's left after
                           mitigation)
```

## Why this is a distinct workflow

Changes are where assumption drift happens — a decision record justified the original
behaviour (`decision-record-protocol.md`), and a change can silently invalidate that
justification without anyone noticing, because no one re-runs the original analysis. This
workflow forces that re-run every time, scoped to the delta rather than the whole feature.

## Output

```
Change: [one-line description]
Current behaviour: [...]
Proposed change: [...]

Legal differences:
  [ ] None identified — explain why the framework was still run
  [ ] [difference 1] — [why it matters]
  [ ] [difference 2] — ...

New obligations: [...]
Removed obligations: [...] (with the evidence that they're actually no longer applicable)
New risks: [reasoning/issue-spotting.md output]
Mitigations: [...]
Residual risk: [reasoning/legal-risk-scoring.md table]

Does this change reverse a recorded assumption or prior decision record?
  [ ] No
  [ ] Yes — see decision-record-protocol.md; a new decision record is required, not just
      this assessment
```

## Escalation

If residual risk after mitigation is Medium or higher on any dimension
(`reasoning/legal-risk-scoring.md`), or if any obligation is being removed, this always
routes to a named human for sign-off before the change ships — never resolved by this
agent alone.
