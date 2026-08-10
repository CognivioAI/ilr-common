# DPIA Drafting & Data-Protection Assessment

Produces the **first draft** of a Data Protection Impact Assessment for a proposed feature
or data flow — for the named data-protection/security reviewer to correct, extend, and
sign. This module is about assessing a *new* proposed processing activity; the platform's
baseline security/tenancy controls are the architect-agent's domain
(`architect-agent/security.md`, `architect-agent/compliance.md`) and should be referenced,
not re-derived.

## When a DPIA draft is warranted (Article 35 triggers — non-exhaustive)

- Large-scale processing of special-category or immigration-status-adjacent data.
- Systematic monitoring of a publicly accessible area (less relevant here) or of user
  behaviour at scale.
- New technology used in a novel way for this data (e.g. a browser extension reading a
  user's live third-party session).
- Any flow where the platform would handle a user's credentials or session tokens for a
  third-party system, even transiently — always warrants at least a draft, regardless of
  whether it clears the formal Article 35 threshold.

## Draft structure

1. **Description of processing** — what data, from where, to where, retained how long,
   who/what system touches it. Diagram if the flow is non-trivial (request one from the
   calling agent rather than inventing the architecture).
2. **Necessity & proportionality** — is this the least-invasive way to achieve the
   product goal; note any lower-risk alternative considered (e.g. "read-only local
   overlay" vs "credentialed server-side access" — see the KAN-153 decision record for a
   worked example of laying out exactly this trade-off).
3. **Risks to data subjects** — realistic, specific scenarios (not generic "data breach"),
   e.g. "if the extension mis-maps a field, incorrect data could be entered into the
   user's live government account without their noticing."
4. **Mitigations** — technical and procedural controls proposed for each risk.
5. **Consultation** — who else needs to be consulted (data-protection officer if one
   exists, ICO if the risk remains high after mitigation) — list as an open action item,
   never assume it's been done.
6. **Sign-off block** — named reviewer, date, decision (approved / approved with
   conditions / rejected) — always left blank in the draft; the agent never fills this in.

## Special-category / immigration data handling checklist (draft against this, don't skip)

- Is the data encrypted in transit and at rest consistent with the platform baseline?
- Is access scoped to tenant/case as the rest of the platform already enforces (RLS,
  `TenantContext`) — or does the new flow bypass that model (e.g. because it touches a
  third-party system outside our database entirely)? If it bypasses the model, say so
  explicitly — that's a materially different risk profile.
- Is there a retention/deletion story, or does data (e.g. a third-party session artifact)
  persist somewhere it wasn't before?
- Does the flow create a new credential-custody surface? If yes, treat as maximum
  scrutiny regardless of how the rest of the DPIA scores — flag prominently, don't bury it.

## Output

A draft DPIA document (Confluence page, filed per `decision-record-protocol.md`'s routing
rule) with every section above populated as far as the available facts allow, explicit
`[NEEDS INPUT: ...]` markers where information is missing, and the sign-off block left
open. Report back with a link and a one-line list of what's still missing.
