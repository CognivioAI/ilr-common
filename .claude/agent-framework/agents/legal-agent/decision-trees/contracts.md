# Decision Tree: Contracts

Triage for a new or amended contract (vendor, customer, DPA) — narrows into
`workflows/document-analysis.md`'s per-type checklists.

```
What is the contract's function?
    │
    ├─ We are the customer/recipient of a service ──→ Does it involve personal-data
    │   processing by the vendor on our behalf?
    │      │
    │      ├─ Yes ──→ Is there a compliant DPA (Article 28(3) content) either as a
    │      │          standalone document or embedded clause?
    │      │             │
    │      │             ├─ Yes ──→ Verify content against playbooks/uk-gdpr-and-ico.md
    │      │             │          checklist.
    │      │             └─ No ──→ STOP. Required before the vendor processes any
    │      │                        personal data on our behalf — escalate.
    │      │
    │      └─ No ──→ Standard commercial review — workflows/document-analysis.md
    │                 ("Supplier / Vendor Agreement" entry).
    │
    ├─ We are the vendor/supplier ──→ Does our service involve processing the customer's
    │   personal data?
    │      │
    │      ├─ Yes ──→ We are the processor — offer/confirm our own DPA terms are Article
    │      │          28(3)-compliant before signing.
    │      └─ No ──→ Standard commercial review.
    │
    └─ Employment-related ──→ Route to decision-trees/employment.md and
        playbooks/employment.md instead of this tree.
```

## Cross-cutting checks regardless of branch

- Liability caps/exclusions — do they leave the company exposed beyond what leadership
  would knowingly accept (flag, don't conclude "acceptable")
- Governing law and dispute resolution — consistent with the company's other agreements
- IP ownership of anything created under the contract
- Termination and data-return/deletion obligations on exit

## Output

Feeds `workflows/document-analysis.md` and, if personal data is in scope,
`playbooks/uk-gdpr-and-ico.md`.
