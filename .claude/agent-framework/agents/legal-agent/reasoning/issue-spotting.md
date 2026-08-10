# Issue Spotting

Solicitors don't answer the question asked — they first work out every question that
*should* be asked. Run issue-spotting before research, not after: it determines what to
research.

## Method

Given any fact pattern, scan it against these lenses and list every one that plausibly
applies, before investigating any of them:

- Data protection (UK GDPR / DPA 2018)
- Confidentiality (contractual or equitable)
- Contract (formation, breach, termination, unfair terms)
- Employment (if any employment relationship is present)
- Security (technical/organisational measures, breach notification)
- Consumer protection
- Intellectual property
- Competition/anti-trust (CMA 1990, Competition Act 1998)
- Regulatory/licensing (sector-specific — financial services, immigration/OISC, etc.)
- Criminal exposure (Computer Misuse Act, fraud)
- Litigation/dispute risk

## Worked shape

Input: *"Employer copied customer data to Dropbox."*

```
Issues detected
  - Data protection (UK GDPR) — lawful basis, processor agreement with Dropbox, cross-border transfer
  - Confidentiality — contractual duty owed to the customer
  - Contract — does the customer contract restrict where data may be stored
  - Employment — was this within the employee's authority; disciplinary exposure
  - Security — was the transfer encrypted, was access controlled
  - Possible criminal offences — unauthorised access/exfiltration angle, low likelihood but worth naming
  - Breach reporting — does this cross the Article 33/34 threshold
  - Evidence preservation — what needs to be preserved before anything is deleted or overwritten
```

Each detected issue then gets run through `reasoning/legal-analysis-framework.md`
individually — don't collapse them into one undifferentiated answer.

## Missing-facts detection

Issue-spotting and missing-facts detection are the same move, run before answering rather
than after. For a question like *"Can I terminate this employee?"*, the correct first
output is the list of facts needed, not an answer:

```
Critical missing facts
  - Length of continuous employment (unfair dismissal qualifying period)
  - Jurisdiction (E&W / Scotland / NI)
  - Reason for termination
  - Any protected characteristic in play (discrimination risk)
  - Disciplinary process followed so far
  - Collective consultation obligations (redundancy, headcount)
  - Relevant contract terms (notice, PILON, restrictive covenants)
  - Union/representation status
```

Only once these are answered (or explicitly marked unknown and flagged as a gap) does the
analysis proceed to step 4 onward of the framework.

## Failure mode this prevents

An agent that answers the literal question asked, using only the facts volunteered,
systematically under-issues a matter — exactly the failure mode a junior (not a solicitor)
falls into. Issue-spotting first is what closes that gap.
