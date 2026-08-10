# Immigration Analysis Framework

The dedicated reasoning sequence for UK ILR/immigration questions — sharper for this
platform's actual domain than the general `reasoning/legal-analysis-framework.md`. Run
this instead of (not in addition to) the general framework whenever the question is about
an immigration route, eligibility requirement, or application.

## Read this before anything else: two modes, not one

Every question that reaches this framework is either about **the rule** (generic — "what
does the Immigration Rules say about X") or about **a case** (a specific applicant's actual
facts and evidence). The framework below is the same shape for both, but the two modes
produce fundamentally different outputs at steps 3, 6, and 9:

- **Rule mode** — no real applicant data in play. State conclusions plainly: what the rule
  requires, what evidence categories typically satisfy it, what commonly causes refusal.
  This is explaining the law, not advising a person — `playbooks/immigration-boundary.md`'s
  in-scope territory.
- **Case mode** — real applicant facts/evidence are in play, even hypothetically framed
  ("this applicant has..."). Every conclusion step becomes a **question or a flag, never a
  determination.** "Is X requirement met?" is answered with what's present and what's
  absent, not with "met" or "not met." This is the boundary
  `playbooks/immigration-boundary.md` draws, and it is not relaxed by this framework —
  this framework exists specifically to make that boundary operational at each step
  rather than bolted on as an afterthought.

If genuinely unsure which mode applies, treat it as case mode — the safer default.

## The sequence

1. **Immigration route** — which route is potentially applicable. Rule mode: name the
   candidate routes generically. Case mode: name the routes the stated facts suggest
   *might* be relevant, framed as "the facts as described suggest X and Y could be
   candidate routes" — not "you should apply under X."
2. **Eligibility requirements** — the specific requirements the route imposes, per
   `playbooks/eligibility-framework.md`, cited (`research/citation-verification.md`).
3. **Evidence review** — per `playbooks/evidence-assessment.md`. Rule mode: what evidence
   category the Rules/guidance name for each requirement. Case mode: what evidence is
   actually described as present — stated as fact, not judged as sufficient.
4. **Missing evidence** — what evidence category has nothing pointed to it. Always a plain
   list, in both modes — this step is inherently safe since it identifies absence, not
   sufficiency.
5. **Relevant Immigration Rules** — cited per `reasoning/source-hierarchy.md` and
   `research/citation-verification.md`.
6. **Relevant guidance** — Home Office caseworker guidance, graded per
   `reasoning/evidence-grading.md` — never treated as equivalent to the Rules themselves.
7. **Potential refusal reasons** — per `playbooks/refusal-risk-analysis.md`. Framed always
   as "a caseworker could raise this concern because..." — never "this application will be
   refused" or "this application is safe."
8. **Counterarguments** — what would address each potential concern, framed as "if X
   evidence were provided, this concern would likely be addressed" — again a conditional,
   not a conclusion about the actual case.
9. **Overall assessment** — Rule mode: a plain summary of the route and its requirements.
   Case mode: **no eligibility conclusion.** The output is the requirement-by-requirement
   evidence map (`playbooks/evidence-assessment.md`) and the gaps/objections found — not a
   bottom-line "eligible" or "not eligible."
10. **Further evidence needed** — the single most useful next step: what specific document
    or fact would most reduce the biggest identified gap.
11. **Human review?** — always answered explicitly. In case mode, this is effectively
    always "yes" — a qualified immigration adviser or the applicant's own OISC-regulated
    counsel makes the actual eligibility call, per `scope-boundaries.md` rule 8 and
    `playbooks/immigration-boundary.md`.

## Why this is stricter than the general framework

`reasoning/legal-analysis-framework.md` step 13 ("human review required?") is answered
case-by-case. Here, in case mode, it is structurally always yes — the framework doesn't
leave room for this agent to conclude eligibility even with high confidence, because
confidence about a legal conclusion is not the same as authority to give it, and this is
the one area of the platform's own domain where that distinction is enforced hardest.
