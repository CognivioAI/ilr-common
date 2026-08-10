# Legal QA Gate

Every substantive artifact (decision record, DPIA draft, counsel brief, risk note, drafted
correspondence) runs through this checklist **before** it's presented as finished. This is
the module every other module answers to — if something fails here, it goes back for
correction, it does not ship with a caveat bolted on.

## The checklist

```
[ ] Citations verified   — every citation has all six fields required by
                            research/citation-verification.md, and was independently
                            verified rather than recalled from memory
[ ] No hallucinated content — no section, case, or guidance reference appears that
                            wasn't actually confirmed to exist
[ ] Jurisdiction confirmed — stated explicitly, and consistent throughout the artifact
[ ] Dates checked         — commencement/amendment/repeal status current as of the
                            stated access date (research/legal-research-protocol.md)
[ ] Reserved-activity check — nothing in the output resembles a reserved legal activity
                            under the Legal Services Act 2007 (scope-boundaries.md rule 3)
[ ] Advice-boundary check  — framed as analysis + open question, never as a conclusion
                            the team can act on without sign-off (scope-boundaries.md
                            rule 1); immigration content specifically passes
                            playbooks/immigration-boundary.md's test
[ ] Missing facts listed   — reasoning/issue-spotting.md's missing-facts output is present
                            if the matter has any unresolved factual gaps
[ ] Confidence assigned    — every material conclusion carries a confidence level and
                            reason per reasoning/uncertainty-and-confidence.md
[ ] Evidence graded        — every source used carries its grade per
                            reasoning/evidence-grading.md
[ ] Action items owned     — every open action item names a specific human role/person,
                            never left unowned (scope-boundaries.md rule 7)
[ ] Disclaimer present     — the standard scope-boundaries.md disclaimer line appears
[ ] Nothing sent           - any drafted outbound correspondence is clearly marked
                            draft-only with a named human sender, not dispatched
```

## How to run it

Walk the list top to bottom against the actual draft, item by item — not from memory of
having "generally done this." If any box can't be honestly checked, fix the underlying
draft rather than checking it anyway. A gate that can be talked past isn't a gate.

## Failure handling

If a check fails and can't be fixed within the current research (e.g. a citation can't be
verified with the tools available), don't drop the point — mark it explicitly as an open
research gap in the artifact itself, so the named human reviewer sees exactly what wasn't
confirmed rather than a silently thinner analysis.
