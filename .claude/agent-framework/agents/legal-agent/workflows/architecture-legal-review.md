# Architecture Legal Review

The equivalent of `workflows/feature-legal-review.md` for architecture decisions — run
whenever the Architecture Agent proposes a design that this agent is called into (its own
`architect-agent/compliance.md` / `architect-agent/security.md` flag the trigger; this
module is what the Legal Agent does once called).

## Common architecture decisions and what to check for each

```
Microservices split       — does data crossing a service boundary create a new processing
                             activity or sub-processor relationship
Logging                    — does log content include personal or special-category data;
                             retention period; access controls
Data retention              — is a retention period actually defined and justified, not
                             left as "keep everything indefinitely"
Third-party APIs            — tos-and-third-party-risk.md in full; playbooks/commercial-
                             and-third-party.md
AI models (build or buy)    — training-data provenance, inference-time data handling,
                             explainability obligations if used in a decision affecting a
                             person
Authentication               — credential storage obligations, session-data handling
Scraping / automated access — tos-and-third-party-risk.md Steps 1-3; decision-trees/
                             scraping.md
Cloud provider selection     — data-residency implications, sub-processor chain, DPA
                             coverage
Backups                      — retention/deletion consistency with the stated retention
                             policy; who can access backup data and under what process
Monitoring                    — same lens as logging; also check for employee-monitoring
                             implications if internal systems are monitored
                             (playbooks/employment.md)
Analytics                     — playbooks/uk-gdpr-and-ico.md; cookie/consent implications
                             (decision-trees/cookies.md)
International data flows      — transfer mechanism (adequacy/SCCs/UK IDTA); which
                             jurisdiction's law applies to the data at rest
```

## Output structure — for every decision reviewed

```
Decision: [the architecture choice]

Applicable legal obligations: [named, cited per research/citation-verification.md]
Implementation constraints:   [what the obligation actually requires the design to do]
Acceptable alternatives:      [reasoning/implementation-options.md table]
Residual risk:                [reasoning/legal-risk-scoring.md, after the recommended
                               alternative is applied]
```

## Rules

- Review the decision, not the whole system, in each pass — a single architecture review
  covering ten decisions should produce ten of the blocks above, not one blended narrative.
- Route through `qa/red-flag-detector.md` per decision, since different components of one
  architecture can trigger different red flags.
- Feed the finished review to the Architecture Agent to fold into its own decision record,
  or produce a standalone `decision-record-protocol.md` write-up if the review itself
  reverses a recorded assumption.
