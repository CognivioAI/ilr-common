# Feature Legal Review

The mandatory review this agent runs whenever the Product Agent or a user proposes a new
feature. The output reads like a **legal design review** — issues, obligations, options,
residual risk — not a legal opinion on whether the feature is "allowed."

## Trigger

Any new feature request routed to this agent, or any feature the calling agent flags as
having a legal/regulatory dimension.

## The mandatory sequence

1. **What problem is being solved?** State the feature's actual purpose in one or two
   sentences — issue-spotting is more accurate when grounded in intent, not just mechanism.
2. **What legal issues are triggered?** Run `reasoning/issue-spotting.md` against the
   checklist below.
3. **What assumptions are being made?** Name them explicitly (e.g. "assumes users are UK-
   resident," "assumes the third-party API's ToS permits this use").
4. **What evidence supports those assumptions?** Cite it (`research/citation-verification.md`)
   or flag it as unverified.
5. **What is the lowest-risk implementation?** Produce a `reasoning/implementation-options.md`
   table.
6. **What must be escalated before implementation?** Name every item requiring sign-off and
   the named human role.

## Checklist — scan every item, list only what applies

```
[ ] Personal data processing (playbooks/uk-gdpr-and-ico.md)
[ ] Contractual restrictions (existing customer/vendor contracts this touches)
[ ] Intellectual property (ownership, licensing of anything built or incorporated)
[ ] Licensing (open-source components, third-party content, API terms)
[ ] Employment implications (if the feature affects how staff work or are monitored)
[ ] Consumer law (unfair terms, distance-selling, cancellation rights if applicable)
[ ] Accessibility (WCAG/Equality Act 2010 duties — public-facing features)
[ ] Competition law (any dominant-position or coordination angle — rare, still check)
[ ] AI regulation (if the feature uses or produces AI-driven decisions/content)
[ ] Sector-specific regulation (immigration — playbooks/immigration-boundary.md; any other
    regulated sector the feature touches)
[ ] International implications (cross-border data flows, jurisdiction of users)
[ ] Audit requirements (does this need to produce an auditable trail)
[ ] Record-keeping (statutory retention obligations triggered)
[ ] Evidence preservation (does the feature touch anything that might need to be preserved
    for a dispute or investigation)
```

## Output shape

A memo (`templates/legal-memo.md` structure) containing: purpose, issues checklist results,
assumptions + evidence, implementation options with risk scores
(`reasoning/legal-risk-scoring.md`), adversarial review (`reasoning/adversarial-review.md`)
on the recommended option, and named escalation items. Runs through `qa/red-flag-detector.md`
and `qa/release-gate.md` before being presented as finished.
