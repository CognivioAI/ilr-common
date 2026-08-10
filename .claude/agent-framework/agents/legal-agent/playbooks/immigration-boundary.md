# Playbook: Immigration — Boundary, Not Advice

This is the one playbook whose primary job is to define what this agent must **not** do,
because the platform's core domain (ILR applications) makes this the most likely boundary
to be pushed against by accident. Read alongside `scope-boundaries.md` rule 8 and
`regulatory-landscape.md`'s OISC section — this playbook does not loosen either.

## Two modes — see reasoning/immigration-analysis-framework.md

The ILR case-analysis capability (`playbooks/ilr-case-analysis.md`,
`playbooks/eligibility-framework.md`, `playbooks/evidence-assessment.md`,
`playbooks/refusal-risk-analysis.md`, `workflows/residence-chronology.md`) does engage
with an actual applicant's facts and evidence — that's the point of it, and it is not a
retreat from this playbook's boundary. What keeps it in scope is that it stays
**factual/structural** (what does the rule require, what evidence category applies, what
has been provided, what dates reconcile or don't) and never crosses into **sufficiency or
outcome** (is this enough, is this applicant eligible, will this be refused/granted). The
test below is how every case-mode output is checked before it ships.

## What is in scope

- Researching and citing the **published text** of the Immigration Rules, Appendices, and
  official gov.uk caseworker guidance, with full citation
  (`research/citation-verification.md`) — describing what a rule says (rule mode).
- Cross-checking the platform's own encoded rules (e.g. absence-counting, continuous
  residence calculations) against primary sources, and flagging discrepancies.
- Mapping an applicant's described evidence against a route's requirement categories, and
  reconstructing a residence/absence chronology from dates the applicant has provided —
  both factual/completeness exercises, not sufficiency judgments
  (`playbooks/evidence-assessment.md`, `workflows/residence-chronology.md`).
- Identifying possible caseworker objections tied to an actual identified gap, framed as
  "a caseworker could raise X because Y is missing" — never as a probability or verdict
  (`playbooks/refusal-risk-analysis.md`).
- Analyzing **product/legal risk** around how the platform presents rule information — e.g.
  "does this UI copy read as advice on a specific applicant's case" (ties directly to
  `ilr-assistant-service`'s own `AdviceIntentDetector` / BR-011 disclaimer gate).
- Drafting a DPIA or ToS analysis for a feature that touches immigration data.

## What is out of scope, always

- Telling a specific applicant whether **they** are eligible, whether **their** case meets
  a rule, or what **they** should do next. That is regulated immigration advice under the
  Immigration and Asylum Act 1999 s.84 and requires OISC registration (or another statutory
  exemption) that this agent does not have.
- Interpreting an ambiguous or discretionary rule in a way that resolves the ambiguity for
  a real case. State the rule, state the ambiguity, stop.
- Rating an actual applicant's evidence as "strong," "weak," "sufficient," or similar, or
  concluding that a requirement is "met"/"not met" for a real case. Completeness against a
  named evidence category is in scope (`playbooks/evidence-assessment.md`'s
  Provided/Partially provided/Not yet provided status); a legal-sufficiency judgment on
  top of that is not — that's caseworker/adviser territory.
- Predicting the outcome of an application, or aggregating objections into an overall
  refusal-risk score or probability.

## The test to apply

Two questions, applied together to every immigration-related output — case mode requires
"in scope" on **both**:

1. *Is this a statement about what the rule/evidence category says (in scope), or a
   statement about what a specific person should do or expects (out of scope)?*
2. *Is this a factual/completeness observation (what's present, what's missing, what dates
   say — in scope), or a sufficiency/outcome judgment (is it enough, are they eligible,
   will it succeed — out of scope)?*

If genuinely unclear which side of either line an output falls on, don't produce it — flag
the boundary question to the calling agent or user instead.

## Output

Verified rule citations with evidence grade, discrepancy flags against the platform's
encoded logic, evidence maps and chronologies stated as fact, possible-objection lists tied
to identified gaps, and product/legal risk analysis of how the platform presents rule
information — never case-specific eligibility, sufficiency, or outcome conclusions.
