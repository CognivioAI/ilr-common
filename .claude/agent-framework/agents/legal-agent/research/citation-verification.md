# Citation Verification

This is the single highest-stakes discipline in this agent — `scope-boundaries.md` rule 6
calls a hallucinated citation worse than no citation at all. This module is the concrete
gate that rule is enforced through.

## Required fields

A citation is not usable — in a decision record, DPIA, brief, or any other artifact —
unless it carries all of:

```
Title           (Act/case/guidance name, in full)
Year            (Act year, judgment year, or guidance publication year)
Section/¶       (specific section, paragraph, or page — never "somewhere in the Act")
URL             (if the source is public — legislation.gov.uk, BAILII, gov.uk, etc.)
Official source (was it retrieved from the primary/official publisher, or a secondary one)
Access date     (when this agent actually retrieved/verified it, not when it thinks it
                 might have been true)
```

If any field is missing, the rule is:

```
DO NOT CITE.
```

State instead: "Reference to [topic] located but not independently verified — needs
confirmation from a qualified researcher or counsel before citing" — never fill the gap
with a plausible-sounding guess.

## Verification method

1. **Prefer fetching the live source** over recalling it from memory — memory is
   time-sensitive and wrong more often for legislation/guidance than for stable case names.
2. **Never invent a section number.** If genuinely uncertain which section applies, say so
   explicitly rather than picking the closest-sounding one.
3. **Never invent a case name or citation.** If a point of law is well known but the
   specific authority isn't confidently recalled, describe the point and flag "needs a
   specific citation from counsel/qualified researcher" rather than manufacturing one that
   sounds plausible.
4. **Cross-check date-sensitive claims** against `research/legal-research-protocol.md`
   (commencement, amendment, repeal) before treating a citation as current.
5. **Distinguish "I found this and verified it" from "I recall this."** Every citation in
   an output should make clear which.

## Self-check before any artifact ships

Walk every citation in the draft and confirm each one has all six required fields and was
independently verified, not recalled. This is one of the checks `qa/legal-qa-gate.md` runs
explicitly — but do it here, at the point of writing, rather than treating QA as a
downstream safety net.
