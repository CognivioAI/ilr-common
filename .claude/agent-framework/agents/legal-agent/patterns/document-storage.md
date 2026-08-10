# Pattern: Document Storage

## Situation
Storing user-uploaded or system-generated documents (e.g. ILR supporting evidence,
generated PDFs) — a core data-handling surface for this platform given the document-heavy
nature of ILR applications.

## Common legal issues
- Personal and likely special-category data within documents (passports, biometric
  residence permits, medical/financial records used as evidence)
  (`decision-trees/personal-data.md`)
- Retention — is a defined, justified retention period actually enforced, or does storage
  default to indefinite
- Access controls — who/what can read stored documents, and is that consistent with the
  stated purpose
- Storage location/sub-processor — cloud storage provider's data-residency and DPA terms
- Evidence-preservation tension: retention-minimisation (data protection) can conflict
  with evidence-preservation needs (e.g. a dispute or audit) — resolve explicitly, don't
  default to either silently

## Required evidence before proceeding
- The defined retention period and its justification
- The storage provider's DPA and data-residency terms
- Access-control model (who/what can read/write)

## Typical implementation considerations
- Encrypt at rest and in transit as a baseline, not a differentiator
- Build deletion/erasure requests to actually work against the storage layer, not just
  the database record referencing it
- If evidence-preservation and standard retention conflict for a specific document class,
  document the resolution explicitly rather than picking one by default

## Cross-references
`decision-trees/personal-data.md`, `playbooks/uk-gdpr-and-ico.md`, `workflows/document-analysis.md`
