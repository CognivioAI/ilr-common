# Decision Tree: AI Training

Triage for using data to train, fine-tune, or evaluate an AI/ML model — the platform's own
domain, so treat this tree as high-relevance rather than hypothetical.

```
Does the training data include personal data?
    │
    ├─ No ──→ Data-protection analysis not triggered by training itself. Still check IP/
    │          licensing of the source data (patterns/ai-training.md).
    │
    └─ Yes ──→ Is it special category data (health, biometric, immigration/ethnic-origin-
               adjacent data is common on this platform)?
                  │
                  ├─ Yes ──→ Article 9 condition required in addition to Article 6 lawful
                  │          basis. DPIA very likely required
                  │          (dpia-and-data-protection.md Article 35 triggers).
                  │          High-scrutiny — playbooks/uk-gdpr-and-ico.md escalation
                  │          trigger.
                  │
                  └─ No ──→ What lawful basis applies to using this data for training
                             specifically (not just for the original collection purpose)?
                                │
                                ├─ Same purpose as original collection ──→ Confirm
                                │   compatibility under Article 6(4) if basis differs from
                                │   original.
                                │
                                └─ New/different purpose ──→ Requires its own lawful basis
                                    and, generally, a fresh transparency notice to data
                                    subjects. Do not assume consent already given for a
                                    different purpose covers this.
```

## Additional checks regardless of path

- Was the data minimized to what training actually needs, or is a full dataset being used
  where a subset would do (data-minimisation principle)?
- Is there a mechanism for a data subject to object or request erasure, and does removing
  their data from a trained model remain technically feasible — flag this as an open
  engineering/legal joint question if the answer isn't clearly yes.
- If the model's output could produce a decision with legal or similarly significant
  effect on a person, Article 22 (automated decision-making) considerations apply — flag
  for DPIA regardless of the branch above.

## Output

Feeds `dpia-and-data-protection.md` and `patterns/ai-training.md` for the recurring-
situation write-up.
