# Playbook: Evidence Assessment

Immigration work is evidence-driven, and mapping evidence against requirements is one of
the highest-value things this agent can do — but "how strong is this evidence" is a
sufficiency judgment about a real person's case, and sufficiency judgments are exactly what
`playbooks/immigration-boundary.md` puts out of scope. This module maps evidence to
requirements **without** grading its legal sufficiency.

## Why "Strong/Medium/Weak" ratings are not used here

A "Strong" or "Weak" rating on an actual applicant's evidence is a legal conclusion about
whether that evidence satisfies a Home Office/tribunal standard — that is precisely a
sufficiency judgment reserved to a qualified adviser (`scope-boundaries.md` rule 8). This
playbook instead reports **presence and completeness against the stated documentary
category**, which is a factual observation, not a legal one — see the status field below.

## The evidence map

```
| # | Requirement | Evidence category (from Rules/guidance) | What's been provided | Status | Open question |
|---|-------------|-------------------------------------------|------------------------|--------|----------------|
| 1 | Continuous residence | Passport stamps, travel history | [what's actually described as provided] | Provided / Partially provided / Not yet provided | [what would resolve this, if anything] |
| 2 | Absence limits | Travel history, airline records | ... | ... | ... |
```

## Status field — the only judgment this playbook makes

```
Provided           — the evidence category described in guidance has been supplied
Partially provided — something has been supplied but a described element of the category
                      is missing (state exactly what's missing, factually)
Not yet provided    — nothing has been supplied against this requirement
```

This is a completeness check against a named category, not a legal-sufficiency
assessment — deliberately. Whether "partially provided" evidence is actually enough to
satisfy the Rules is the open question named in the last column, for a qualified adviser
to answer, never a conclusion this playbook reaches itself.

## Method

1. Pull the requirements table from `playbooks/eligibility-framework.md`.
2. For each requirement, state the evidence category the Rules/guidance actually name
   (cited, `research/citation-verification.md`).
3. State factually what's been described as provided — quote or summarize, don't
   characterize.
4. Set the Status field per the definitions above.
5. Name the open question — usually "would [specific additional document] change this,"
   phrased as a question for the applicant/adviser, never as advice that it's required.

## Output

Feeds `playbooks/refusal-risk-analysis.md` (a "Not yet provided" or "Partially provided"
row is a natural input to what a caseworker might raise) and
`playbooks/ilr-case-analysis.md`. Use `templates/evidence-map.md` for the standard output
structure.
