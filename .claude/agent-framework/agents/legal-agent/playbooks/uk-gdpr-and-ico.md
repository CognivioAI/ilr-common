# Playbook: UK GDPR / ICO

For any question involving personal-data processing, a DPIA, a data breach, or an ICO
interaction. Runs on top of the shared `reasoning/legal-analysis-framework.md` — this
playbook supplies the matter-specific checklist and escalation triggers, not a separate
process.

## Issue checklist

- Lawful basis identified for each processing activity (Article 6, plus Article 9 if
  special category data)
- Controller / processor / joint-controller roles correctly assigned
- Data minimisation — is everything collected actually necessary
- Retention period defined and justified
- International transfer mechanism if data leaves the UK/EEA (adequacy, SCCs, UK IDTA)
- Data subject rights mechanisms in place (access, erasure, rectification, portability)
- DPIA required? — see `dpia-and-data-protection.md` for the Article 35 trigger list
- Sub-processor chain documented and authorized
- Breach-notification readiness (72-hour Article 33 clock, Article 34 data-subject
  notification threshold)

## Evidence sources, by grade (`reasoning/evidence-grading.md`)

- Level A: UK GDPR (retained EU law), Data Protection Act 2018, ICO enforcement notices
  against the specific party in question
- Level B: ICO general guidance, ICO's own DPIA template and guidance pages
- Level C: law-firm data-protection blog posts — locate the ICO guidance they're
  summarizing rather than citing the summary

## Escalation triggers — always route to named DPO/counsel, never resolve here

- Any processing of special category or criminal-offence data
- Any breach that plausibly crosses the Article 33/34 notification threshold
- Any international transfer without an already-approved mechanism
- Any DPIA where the residual risk after mitigation is assessed as high

## Output

A DPIA draft (`dpia-and-data-protection.md`) or a risk note (`templates/risk-register.md`),
sign-off block always left open for the named reviewer.
