# Decision Record Protocol

The standard artifact this agent produces when a proposed change touches legal/regulatory
risk or reverses a recorded product assumption. Formalizes the pattern first used for
KAN-153 (gov.uk/UKVI assisted form-fill) so every future case follows the same shape and
is easy to find.

## When to write one

- A feature request would reverse or narrow a recorded product assumption (e.g. A-04 —
  "reference package only, no gov.uk submission").
- A proposed integration touches a third party whose ToS/legal status is unclear
  (`tos-and-third-party-risk.md`).
- A design would create a new credential-custody, special-category-data, or
  regulator-facing surface.
- Any time the calling agent or user explicitly asks for a decision record.

Not every legal question needs one — routine, low-impact questions can go straight to a
Jira comment. Use judgment; if unsure, err toward writing the record (cheap insurance).

## Structure (match this shape every time)

1. **Status panel** (top of page) — `DECISION PENDING`, `DECIDED`, `REJECTED`, or
   `DEFERRED`. **Only a named human owner changes this status** — the agent proposes,
   never flips it itself (`scope-boundaries.md` item 2).
2. **Summary table** — Impact (High/Medium/Low), Driver (who requested this), Approvers
   required (named roles, not "legal" in the abstract), Engineering recommendation.
3. **Context** — what was asked and why, in plain language.
4. **What this reverses/affects** — quote the existing assumption/ADR/policy verbatim if
   one exists; link it.
5. **Decision (proposed)** — a decision list (one item per line, each marked
   `DECIDED`/`UNDECIDED` as the agent's proposal, not a real decision) covering the
   concrete choices on the table.
6. **Options considered** — a comparison table (minimum: description, key risk dimension,
   who bears it, overall risk tier). Always include the do-nothing / status-quo option.
7. **Risk analysis** — numbered, concrete, each risk tied to a specific consequence, not
   generic hand-waving ("could cause X, resulting in Y").
8. **Technical feasibility** (if applicable) — what engineering can assess without legal
   input; explicitly caveated as conditional on the legal gate, never framed as clearance
   to proceed.
9. **Action items** — a Confluence task list, each item with a **named human role** as
   owner (not "someone," not "the team"). This is the actual gate — the record isn't done
   until every item here either has an owner or is explicitly marked out of scope.
10. **Proposed policy/assumption update** — draft wording for the updated assumption/ADR,
    clearly marked "for approval — not yet in force."
11. **Guardrails until decided** — an explicit, testable statement of what must NOT be
    built while this is open (mirrors what became a hard engineering constraint on
    KAN-153: "no code that stores credentials, automates login, or submits").
12. **Attribution line** — the `scope-boundaries.md` disclaimer, verbatim.

## Where it's filed

Follow the Documentation Agent's routing rule
(`agents/documentation-agent/README.md`): if the decision is scoped to one feature, file
under `Features/<Feature Name>/`; if it's cross-cutting (like an assumption reversal or a
new ADR-style precedent), file under the architecture decision records / standards area
alongside other ADRs — hand off to the Documentation Agent to execute the actual
Confluence write, rather than writing pages directly outside that routing logic when a
Documentation Agent is available in the session.

## Companion Jira ticket

Every decision record has a Jira ticket. The agent may comment on it with progress and
links, and may transition it between working states (e.g. To Do → In Progress) to reflect
that analysis is underway — but **never transitions it to Done/Resolved**. A legal-gated
ticket only closes when its named human owners say so; report that it's ready for their
call rather than closing it.
