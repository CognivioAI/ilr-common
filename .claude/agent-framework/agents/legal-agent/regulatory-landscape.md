# UK Regulatory Landscape — Reference Vocabulary, Not Conclusions

This module is a checklist of frameworks the agent should recognize and correctly apply
*by name*, so its output gives counsel a running start. It is deliberately not a source of
conclusions on live matters — apply `scope-boundaries.md` throughout.

## Computer Misuse Act 1990 (CMA)

The relevant UK statute for "did our software access a third-party system it shouldn't
have." Sections most likely to come up:

- **s.1 — Unauthorised access to computer material.** Historically the hook for
  automation/scraping disputes. Courts tend to focus on **authorisation** — did the system
  owner give permission for this kind of access, and would they object if asked. Signals
  that push toward "unauthorised": exceeding an authorisation given for a different
  purpose, bypassing a technical control designed to exclude automated access, acting
  against the system owner's clearly stated wishes.
- **s.3 — Unauthorised acts with intent to impair operation** (e.g. causing degraded
  service via load). Relevant if any design involves high request volume.
- **s.3A — Making/supplying articles for use in an offence** — relevant if the platform
  were to distribute a tool (e.g. a browser extension) whose primary purpose is to defeat
  another system's access controls.

Flag rather than conclude: whether a specific design crosses this line is exactly the
question for counsel, informed by the ToS finding (`tos-and-third-party-risk.md`) and the
transparency/intent posture of the design (conservative, single-session, human-paced
designs read differently to enforcement bodies than bulk/hidden automation).

## UK GDPR / Data Protection Act 2018

- **Lawful basis** required for any processing of personal data — identify which basis
  (consent, contract, legitimate interests) a given data flow relies on; don't assume.
- **Special category data** (Article 9) — immigration/nationality status sits close to
  this line depending on context; treat data flows touching it as higher scrutiny by
  default rather than waiting for a definitive classification.
- **DPIA triggers** (Article 35) — required for processing likely to result in high risk:
  large-scale special-category processing, systematic monitoring, new technology used in a
  novel way. See `dpia-and-data-protection.md` for the drafting process.
- **Breach notification** — 72-hour clock to the ICO from awareness of a qualifying
  breach. Relevant to anything touching session data, credentials, or third-party account
  access — flag the clock explicitly in any design review that goes there.

## Legal Services Act 2007

Defines **reserved legal activities** (see `scope-boundaries.md` item 3) — this is the
statute underpinning why this agent must never present output as authoritative legal
advice or a completed legal service.

## Immigration and Asylum Act 1999, s.84 — OISC regulation

**Specific to this platform.** Giving immigration advice to a specific person (not
general/published information) requires OISC registration or a statutory exemption (e.g.
practising solicitors, registered charities in some cases). This is the same category of
problem as the legal-agent's own boundary, but it's a *product* concern, not an
engineering-tooling one — it already governs `ilr-assistant-service`'s guardrails
(guidance-only chat, disclaimer gate, `AdviceIntentDetector` deflection, BR-011). If this
agent is ever asked to review a *product feature* (not internal tooling), check whether it
risks crossing from "information" into "advice on a specific case" and flag it against
this same statute — don't treat it as a separate, lesser concern.

## Consumer/contract law basics (click-wrap enforceability)

Relevant when assessing whether a third party's ToS is likely enforceable against us at
all (a browsewrap/clickwrap distinction, unusual/onerous term notice requirements). Useful
context for counsel, not a basis for concluding a ToS *doesn't* apply — assume it does
unless and until counsel says otherwise.

## Non-exhaustiveness

This list is not comprehensive and will miss sector-specific rules the agent doesn't know
to look for (e.g. financial services regulation if a feature ever touches payments beyond
the existing billing-service scope). When a new domain comes up, say so explicitly rather
than silently applying only what's listed here.
