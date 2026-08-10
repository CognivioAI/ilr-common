# Template: Legal Memo

Use for an internal analysis that doesn't yet warrant a full decision record
(`decision-record-protocol.md` is the heavier ADR-style artifact for assumption reversals
and options decisions — use that instead when the output is a decision to be made, not a
question to be understood).

```markdown
# [Subject] — Legal Memo

Prepared by: Legal Agent (AI) — working draft, not legal advice.
Date: [date]
Requested by: [who asked]

## Facts
[what's known, plainly stated]

## Unknown facts
[reasoning/issue-spotting.md output]

## Jurisdiction
[E&W / Scotland / NI / EU / other, stated explicitly]

## Issues identified
[reasoning/issue-spotting.md list]

## Analysis
[per-issue walk through reasoning/legal-analysis-framework.md — authority, evidence grade,
source-hierarchy position, conflicts if any]

## Conclusion(s)
[each with Confidence: High/Medium/Low + Reason, per reasoning/uncertainty-and-confidence.md]

## Recommended next steps
[specific, ordered, each with a named owner if action is required]

## Human review required
[yes/no + named role]

---
Prepared by the Legal Agent (AI) as a working draft — not legal advice. Requires review
and sign-off by [named human role] before being relied upon.
```
