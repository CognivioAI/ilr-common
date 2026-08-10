# Decision Tree: Personal Data Processing

The general-purpose entry point for any new data flow — most other decision trees
(`ai-training.md`, `cookies.md`, `employment.md`) narrow into this one at some point; use
this tree first when the situation doesn't already match a more specific one.

```
Will personal data be processed?
  (any information relating to an identified or identifiable living individual)
    │
    ├─ No ──→ UK GDPR/DPA 2018 not triggered. Confirm this conclusion is actually correct
    │          — "anonymised" data that can be re-identified via another dataset the
    │          company holds is still personal data.
    │
    └─ Yes ──→ Is it special category data (Article 9: health, biometric, genetic, racial/
               ethnic origin, religious belief, sex life/orientation, political opinion,
               trade union membership) or criminal-offence data (Article 10)?
                  │
                  ├─ Yes ──→ Article 9/10 condition required in addition to Article 6.
                  │          DPIA likely required — check dpia-and-data-protection.md
                  │          triggers. playbooks/uk-gdpr-and-ico.md escalation trigger.
                  │
                  └─ No ──→ What is the lawful basis (Article 6)?
                              │
                              ├─ Not yet identified ──→ STOP. Identify it before
                              │   proceeding — do not build first and find a basis after.
                              │
                              └─ Identified ──→ Is a DPIA required?
                                    (dpia-and-data-protection.md Article 35 triggers —
                                    systematic monitoring, large-scale special-category
                                    processing, new technology, etc.)
                                       │
                                       ├─ Yes ──→ Draft DPIA before implementation.
                                       │
                                       └─ No ──→ Will the data leave the UK/EEA?
                                             │
                                             ├─ Yes ──→ International transfer mechanism
                                             │   required (adequacy / SCCs / UK IDTA) —
                                             │   confirm one is in place before the
                                             │   transfer happens, not after.
                                             │
                                             └─ No ──→ Proceed with standard data-
                                                 minimisation, retention, and subject-
                                                 rights checks
                                                 (playbooks/uk-gdpr-and-ico.md).
```

## Output

Feeds `playbooks/uk-gdpr-and-ico.md` and, if a DPIA is triggered,
`dpia-and-data-protection.md`.
