# Red Flag Detector

Run **before** any recommendation is drafted, not just before it ships — a red flag found
early changes how much scrutiny the whole analysis gets, not just what caveat is attached
at the end. This is upstream of `qa/release-gate.md`, which checks the finished artifact;
this module checks the fact pattern itself as soon as it's known.

## The scan

Check the fact pattern against every item below. This is not "does the feature involve
X" in the abstract — check the actual data/action in front of you.

```
[ ] Processing personal data at all (baseline — decision-trees/personal-data.md)
[ ] Biometric information (facial recognition, fingerprints — Article 9 special category)
[ ] Children's data (under-18 — heightened ICO scrutiny, Age Appropriate Design Code
    considerations if any part of the service could be accessed by a minor)
[ ] Financial information (payment data, income/means data — heightened security and
    sometimes sector-specific regulation)
[ ] Medical/health information (Article 9 special category)
[ ] Immigration/ethnic-origin-adjacent data (core to this platform's domain — treat as
    special-category-adjacent even where not squarely Article 9, given sensitivity)
[ ] Sanctions exposure (any party, jurisdiction, or transaction that could touch UK/EU/US
    sanctions lists)
[ ] Export controls (any software, cryptographic, or technical data crossing a controlled
    jurisdiction boundary)
[ ] Anti-competitive conduct (coordination with a competitor, exclusionary design —
    rare for this platform, still check)
[ ] Intellectual property concerns (unlicensed third-party content, unclear AI-output
    ownership — decision-trees/copyright.md)
[ ] Anti-circumvention measures (defeating a technical protection/anti-bot control —
    decision-trees/scraping.md)
[ ] Regulated immigration advice (playbooks/immigration-boundary.md — the platform's own
    highest-frequency boundary risk)
[ ] Reserved legal activity (Legal Services Act 2007 s.12 — scope-boundaries.md rule 3)
```

## Effect of a hit

Any single item checked:

- **Increases scrutiny automatically** — the full `reasoning/legal-analysis-framework.md`
  sequence runs even if the question looked simple, and `reasoning/adversarial-review.md`
  is mandatory, not optional, on the resulting conclusion.
- **Escalation trigger** — the relevant playbook's escalation trigger applies
  (`playbooks/`), and the artifact names the specific human role who must review before
  anything ships.
- Anti-circumvention and reserved-legal-activity hits are **hard stops**, not just
  scrutiny increases — see `decision-trees/scraping.md` and `scope-boundaries.md` rule 3.

## Why this runs first, not last

A red flag found after a conclusion is already drafted tends to get treated as a caveat
bolted onto an otherwise-finished answer. Running the scan first changes the depth of the
analysis itself — the fact pattern, not the write-up, is what should escalate.
