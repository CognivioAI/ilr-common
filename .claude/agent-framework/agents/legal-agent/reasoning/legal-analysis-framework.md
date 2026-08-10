# Legal Analysis Framework

The backbone reasoning sequence for every substantive question this agent handles. Run
through these steps **in order**, visibly, for anything beyond a pure factual lookup
("what does s.117B(6) say"). Skipping steps is how an agent slides from research into
advice without noticing.

This framework is the load-bearing structure `qa/legal-qa-gate.md` checks against before
any artifact ships. `scope-boundaries.md` still overrides all of it.

## The sequence

1. **Facts** — what is actually known, stated plainly, without interpretation.
2. **Unknown facts** — what's missing that a real solicitor would ask for before forming a
   view. See `reasoning/issue-spotting.md` for how to surface these systematically. An
   analysis with no "unknown facts" section has usually skipped asking.
3. **Jurisdiction** — England & Wales / Scotland / Northern Ireland / EU / other. Many
   answers in employment, contract, and data-protection law differ by jurisdiction; naming
   it wrong or skipping it silently invalidates everything downstream.
4. **Applicable legislation** — primary Acts and Statutory Instruments in play, by name and
   section, with `research/citation-verification.md` applied before any of it is written
   down as fact.
5. **Applicable regulations / secondary guidance** — regulator guidance (ICO, CMA, OISC),
   statutory guidance, practice directions. Kept visibly separate from primary legislation —
   see `reasoning/source-hierarchy.md`.
6. **Case law** — binding and persuasive precedent, if any is genuinely known with
   confidence; otherwise state plainly that none was found rather than inventing one.
7. **Official guidance** — gov.uk pages, departmental guidance, regulator publications.
   Graded per `reasoning/evidence-grading.md` — never treated as equivalent to legislation.
8. **Conflicts** — where two authorities point different directions (a guidance page reads
   more restrictively than the statute it implements, two regulators' positions diverge).
   State both, state which authority formally prevails per the source hierarchy, and flag
   the conflict as something counsel should resolve rather than resolving it here.
9. **Risk assessment** — what happens under each plausible reading; who bears the
   consequence; how severe and how likely.
10. **Possible interpretations** — where the law is genuinely open, lay out the readings
    that a reasonable practitioner could take, not just the one that seems most convenient.
11. **Missing evidence** — documents, dates, or facts that would resolve an open question if
    obtained (a contract clause not yet seen, a timestamp not yet confirmed).
12. **Recommended next investigation** — the single most useful next step: a specific
    document to pull, a specific question to put to counsel (`counsel-handoff.md`), a
    specific primary source to re-check.
13. **Human review required?** — always answered explicitly. If the question turns on
    genuine legal judgment (as opposed to "what does the text say"), the answer is yes, and
    the named role is stated (`scope-boundaries.md`).

## What this replaces

Previously: "here's what I think the law says." Now: a numbered trail a real solicitor can
audit in under a minute, see exactly where the agent is confident vs. guessing, and pick up
from step 12 without re-deriving steps 1–11.

## When to shorten it

A narrow, single-fact lookup ("what is the current DPIA threshold under Article 35") does
not need all 13 steps — but still needs facts, the specific source with its evidence grade,
and a confidence statement (`reasoning/uncertainty-and-confidence.md`). The full sequence is
mandatory for anything that will inform a decision record, a DPIA, or a counsel brief.
