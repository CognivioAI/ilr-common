# Pattern: AI Inference (Using a Model at Runtime)

## Situation
Sending user data to an AI model (in-house or third-party, e.g. AWS Bedrock/Claude) at
runtime to produce an output used in the product — the platform's core mechanism for its
AI-assisted features.

## Common legal issues
- Personal data leaving the platform's own infrastructure to a model provider — sub-
  processor relationship, DPA coverage (`playbooks/uk-gdpr-and-ico.md`)
- Whether the output constitutes a decision with legal or similarly significant effect on
  the person (Article 22 automated-decision-making) — especially relevant given the
  platform's ILR-application domain
- Advice-boundary risk if the inference output could read as advice on a specific
  applicant's case (`playbooks/immigration-boundary.md`)
- Model-provider data-retention/training-use terms — does the provider retain or train on
  submitted data by default, and has that been confirmed off

## Required evidence before proceeding
- The model provider's data-handling terms for the specific product/API used, quoted and
  timestamped
- Confirmation of whether a DPA is in place with the provider
- A description of what human review, if any, sits before the output reaches the user

## Typical implementation considerations
- Minimize what's sent in the prompt/context to what inference actually needs
- If output could be read as advice, the product's own disclaimer/intent-detection gates
  (e.g. `AdviceIntentDetector`, BR-011) need to be confirmed present, not assumed
- Log enough to reconstruct what was sent and returned, for both debugging and evidence-
  preservation purposes, consistent with the platform's retention policy

## Cross-references
`decision-trees/ai-training.md`, `dpia-and-data-protection.md`, `playbooks/immigration-boundary.md`
