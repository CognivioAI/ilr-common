# Adversarial Review

Every conclusion this agent reaches must survive an attack on itself before it's presented
as finished. One-sided analysis — reaching a comfortable conclusion and stopping — is the
single most common way a plausible-sounding legal analysis turns out to be wrong. This
module is the mandatory step between "I have a conclusion" and "I can present this."

Run this **after** `reasoning/legal-analysis-framework.md` produces a draft conclusion,
**before** it goes through `qa/legal-qa-gate.md`.

## The five questions

For every material conclusion, answer each of these explicitly, in writing:

1. **If I represented the regulator, what would I argue?** Take the position of the ICO,
   CMA, OISC, or whichever regulator is in play, and construct the strongest case that the
   proposed approach is non-compliant.
2. **If I represented a claimant, what weaknesses would I identify?** Take the position of
   someone with an adverse interest (a data subject, a counterparty, a competitor) and find
   the gaps in the reasoning that would be exploited in a dispute.
3. **Which assumptions could be disproved?** List every assumption the conclusion rests on
   (see `reasoning/implementation-options.md` and the "unknown facts" step of the analysis
   framework) and ask what evidence, if it turned up, would break each one.
4. **What evidence would I request?** If this went to a genuine adversarial process
   (litigation, a regulatory investigation, a tribunal), what documents or facts would the
   other side demand — and does the current analysis actually have them?
5. **Which authority contradicts my position?** Deliberately search for the counter-
   authority, not just the supporting one. If none is found, say so explicitly rather than
   silently assuming none exists — `reasoning/uncertainty-and-confidence.md` applies.

## Output shape

```
Adversarial Review

Regulator's strongest counter-argument: [stated plainly, steelmanned, not strawmanned]
Claimant's strongest weakness-finding:  [same]
Assumptions vulnerable to disproof:     [list, with what would disprove each]
Evidence a hostile party would demand:  [list]
Contradicting authority found:          [cite it, or state "none located — see confidence
                                          note below"]

Effect on original conclusion:
  [ ] Unchanged — the adversarial review didn't surface anything that weakens it
  [ ] Weakened — confidence level lowered from X to Y, reason: [...]
  [ ] Reversed — the original conclusion doesn't survive; here is the corrected one
```

## Why this can't be skipped for "obvious" conclusions

The conclusions that most need adversarial review are the ones that feel obviously correct
— that's exactly when confirmation bias is strongest and the counter-argument is easiest to
overlook. A conclusion that survives a genuine attempt to break it is far more useful to the
named human reviewer than one that was never tested.
