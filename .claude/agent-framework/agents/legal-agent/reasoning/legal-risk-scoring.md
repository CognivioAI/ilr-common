# Legal Risk Scoring

Turns a qualitative risk finding into a comparable score, so multiple issues (or multiple
options for the same issue, per `reasoning/implementation-options.md`) can be prioritized
against each other instead of each sitting in its own paragraph of prose.

## Dimensions

Score each issue 1 (lowest) to 5 (highest) on:

```
Regulatory exposure   — likelihood + severity of regulator action (ICO, CMA, OISC, etc.)
Litigation exposure    — likelihood + severity of a claim from a counterparty/data subject
Financial impact       — direct cost if the risk materializes (fines, damages, remediation)
Reputational impact    — foreseeable harm to trust/brand if the risk materializes
Operational impact     — disruption to the product/team if the risk materializes or if the
                          mitigation is applied
```

Plus a non-numeric field:

```
Confidence — High/Medium/Low, per reasoning/uncertainty-and-confidence.md — how much
             weight the score itself should carry
```

## Output shape

```
Issue: [name]

| Dimension            | Score |
|-----------------------|------:|
| Regulatory exposure   |   4/5 |
| Litigation exposure    |   2/5 |
| Financial impact       |   3/5 |
| Reputational impact    |   4/5 |
| Operational impact     |   2/5 |
| Confidence             |  High |

Overall priority: [High/Medium/Low — see method below]
```

## Rolling up to overall priority

This is a judgment aid, not a formula to hide behind:

- **High priority**: any dimension scores 4-5 AND confidence is Medium or High.
- **Medium priority**: dimensions cluster around 2-3, or a high individual score is paired
  with Low confidence (the uncertainty itself is the thing to resolve first).
- **Low priority**: all dimensions 1-2.

State the overall priority plainly, but always alongside the dimension table — never as a
bare number with no visible reasoning behind it. A reviewer should be able to disagree with
one dimension and see exactly how that changes the overall call.

## Rules

- Score every issue identified by `reasoning/issue-spotting.md`, not just the one that
  seems most serious — a low-profile issue can still be High priority if regulatory
  exposure is severe even at low likelihood.
- Low confidence should pull the overall priority toward "resolve the uncertainty first,"
  not toward "assume it's fine."
- This scoring never substitutes for `reasoning/adversarial-review.md` — a well-scored risk
  can still be reasoned about one-sidedly; run both.
