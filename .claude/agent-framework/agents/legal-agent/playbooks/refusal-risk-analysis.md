# Playbook: Refusal Risk Analysis

The "if I were the caseworker" method, adapted from `reasoning/adversarial-review.md`'s
regulator-perspective question to this platform's specific domain. This is genuinely
useful for surfacing weaknesses before submission — but the output is a list of **possible
objections**, never a probability or a verdict on the actual application.

## Why this stays as objection-spotting, not prediction

"This application will likely be refused" or "this application is low-risk" are both
predictions about a real legal outcome for a real person — exactly the sufficiency/outcome
judgment `playbooks/immigration-boundary.md` puts out of scope. "A caseworker could raise
concern X because evidence category Y is [status] per the evidence map" is a structural
observation about a gap, not a prediction. This playbook produces the second, never the
first.

## Method

For each requirement in the evidence map (`playbooks/evidence-assessment.md`) whose status
is not "Provided," and for any requirement where the evidence map shows something
inconsistent (e.g. two dates that don't reconcile, a gap in a chronology per
`workflows/residence-chronology.md`):

```
Possible objection: [the specific concern a caseworker's decision letter could raise,
                     framed factually — e.g. "the evidence map shows no travel history
                     covering [date range], which is within the continuous-residence
                     period"]

Basis in the Rules/guidance: [cited — why this gap matters under the actual requirement,
                     not just "it looks incomplete"]

Evidence present that speaks to it: [what's already in the evidence map, if anything,
                     that's relevant even if incomplete]

Evidence absent that speaks to it:  [what specific document/fact, if supplied, would most
                     directly address the gap]

Would more evidence resolve this?  [Yes — name what / Uncertain — explain why / Not
                     applicable — this is a rule-interpretation question, not an evidence
                     gap]
```

## Rules

- Only generate objections tied to an actual gap or inconsistency identified in the
  evidence map or chronology — do not speculate about objections with no basis in the
  facts as described; unfounded objection-spotting is just as much an overreach as a
  false-confidence "this is fine."
- Never aggregate the objections into an overall refusal probability or risk score. Each
  objection stands alone; `reasoning/immigration-analysis-framework.md` step 9 explicitly
  forbids a bottom-line eligibility/outcome conclusion in case mode.
- Every objection ends with "would more evidence resolve this" framed as a question, not
  an instruction to the applicant about what they need to do — that instruction is
  properly a qualified adviser's to give.

## Output

Feeds `playbooks/ilr-case-analysis.md`. Always accompanied by the reminder that this is a
list of possible caseworker concerns identified from the facts as described, not a
prediction of the outcome, and that a qualified immigration adviser should review before
submission.
