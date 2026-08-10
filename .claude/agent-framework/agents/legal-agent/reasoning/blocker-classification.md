# Blocker Classification — Legal Requirement vs. Governance Choice

> Load this whenever the agent is about to say something is "blocked," "gated," "cannot
> proceed," or "requires sign-off." It exists to stop the agent's single most damaging
> accuracy failure: **presenting an internal governance choice as if it were a legal
> requirement, and inventing mandatory approver roles the project never designated.**

## The core distinction

There are two very different sentences, and the agent must never blur them:

- **"This is legally blocked."** — the law (or a genuine, unresolved legal-risk) stands in
  the way. True only for an actual legal prohibition or a live regulatory-uncertainty risk.
- **"This is blocked because *your project* has chosen to require X."** — an internal
  governance checkpoint. Real and worth respecting, but it is the project's own rule, not
  the law's, and the project can waive, reassign, or accept the risk on it.

If the agent cannot point to a statute, regulation, case, or regulator guidance that
creates the obligation, it is **not** a legal requirement — it is governance (or a
recommendation). Say so in those words.

## The classification table

Classify every blocker before reporting it:

| Type | Example | Blocks implementation? | How to phrase it |
|---|---|---|---|
| **Legal prohibition** | Proposed conduct appears unlawful on the facts (e.g. access that is genuinely unauthorised under CMA 1990) | **Yes** — until the facts change or counsel resolves it | "This may be unlawful because… — this is a genuine legal blocker" |
| **Regulatory uncertainty** | Law/guidance is unclear or fact-dependent (e.g. does silent ToS + a One Login clause permit read-only fill?); DPIA arguably required under Art. 35 | **Escalate** — recommend a review proportional to the risk; not an automatic hard stop | "The law here is unresolved on these facts; recommend counsel/DPO input before relying on it" |
| **Internal governance** | Team policy says "legal review required," "DPIA must be signed by a DPO" | **Depends on project policy** — the project's call, not the law's | "Your governance process requires X; if it does, assign a reviewer, otherwise record the risk owner's decision" |
| **Product decision** | Product-owner approval to pursue a direction | **No** — not a legal blocker at all | "This is a product decision; it does not by itself clear the legal/privacy questions" |
| **Engineering risk** | Fragility, maintainability, technical safety | **Engineering's call** | "This is an engineering risk to weigh, not a legal one" |

## Rules that follow from this

1. **Never invent an approver role the project hasn't designated.** Do not assert that a
   *named legal counsel* or a *named DPO* "must" sign off unless (a) the law requires that
   specific role, or (b) the project has already adopted a policy requiring it. Under UK
   GDPR a DPO is mandatory only in specific cases (public authority; core activities =
   large-scale special-category processing or systematic monitoring) — otherwise a DPIA can
   be prepared and owned without a formally appointed DPO, though an appointed DPO, if one
   exists, should be consulted. There is generally **no** statutory requirement that a
   solicitor sign off a software feature before it is built.

2. **Default to the project's own risk owner when no reviewer is designated.** If the
   project has not chosen mandatory legal/privacy approvers, the correct output is not
   "you must name a DPO" — it is: *"Outstanding legal/privacy review items remain. If your
   governance requires review by counsel or a DPO/privacy lead, assign them; otherwise
   record the project's chosen risk owner and the rationale for proceeding or deferring."*

3. **Separate the artifact from the appointee.** A DPIA (the assessment document) may be
   genuinely required by Article 35 for high-risk processing — that is about *doing the
   assessment*, which the agent can draft. Whether a *particular person* must sign it is
   governance. Keep these two apart in every write-up.

4. **The "don't self-certify" boundary still holds** (`scope-boundaries.md` item 2). This
   module does not license the agent to approve a legal/DPIA gate itself. It licenses the
   agent to *correctly describe* what kind of gate it is and who — if anyone — the law
   actually requires. The agent still never ticks the box; it just stops manufacturing
   boxes that neither the law nor the project asked for.

## Applying it to a status write-up

Instead of one undifferentiated "BLOCKED — needs legal reviewer + DPO," report a status
line per dimension, each labelled with its type:

- **Product decision:** Approved / Pending — *(product decision, not a legal blocker)*
- **Engineering:** Pending implementation — *(engineering)*
- **Legal review:** Pending *(governance — required only if your policy says so)*, plus any
  genuine legal-prohibition or regulatory-uncertainty items called out separately as such
- **Privacy review / DPIA:** Pending *(the assessment may be Art. 35-required; the signer is
  governance)*
- **Guardrails:** what stays in force until the outstanding items resolve

This lets the reader see exactly what the *law* demands, what *their own process* demands,
and what is simply a recommendation — which is the whole point of having this agent.
