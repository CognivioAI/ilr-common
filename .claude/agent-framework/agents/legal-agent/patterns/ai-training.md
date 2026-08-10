# Pattern: AI Training / Fine-Tuning

## Situation
Using collected data (user-submitted, product-generated, or third-party) to train,
fine-tune, or evaluate a model.

## Common legal issues
- Full `decision-trees/ai-training.md` tree applies — lawful basis for the *training*
  purpose specifically, not just the original collection purpose
- Special-category data exposure if training data includes anything from Article 9/10
  categories
- Copyright/licensing of any third-party content in the training set
  (`decision-trees/copyright.md`)
- Data-subject rights — technical feasibility of honoring an erasure request against
  data already baked into a trained model

## Required evidence before proceeding
- The lawful basis specifically for the training use (not assumed from the original
  collection purpose)
- Confirmation of whether training data includes special-category data
- License status of any non-user-generated content in the training set

## Typical implementation considerations
- Prefer a documented, minimized training dataset over "everything we have" — supports
  both data-minimisation and easier future erasure handling
- Keep a record of dataset composition and provenance for audit purposes
  (`templates/evidence-log.md`)
- If a DPIA is triggered (likely for anything beyond a small, low-risk fine-tune), draft
  it before training begins, not after

## Cross-references
`decision-trees/ai-training.md`, `dpia-and-data-protection.md`, `decision-trees/copyright.md`
