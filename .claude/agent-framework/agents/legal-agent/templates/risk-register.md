# Template: Risk Register

Structured risk output — use instead of prose paragraphs whenever more than one distinct
risk is in play (`playbooks/uk-gdpr-and-ico.md` and `playbooks/commercial-and-third-party.md`
both route here).

```markdown
# [Matter] — Risk Register

Prepared by: Legal Agent (AI) — working draft, not legal advice.
Date: [date]

| # | Risk | Likelihood | Severity | Evidence Quality | Mitigation | Owner | Status |
|---|------|-----------|----------|-------------------|------------|-------|--------|
| 1 | [description] | Low/Med/High | Low/Med/High | ★★★★★ etc. | [specific mitigation] | [named human] | Open/Mitigated/Accepted |
| 2 | ... | | | | | | |

## Notes per risk (expand only where the table row needs supporting reasoning)

### Risk 1
[reasoning/legal-analysis-framework.md walk-through if the risk is non-obvious]

## Overall assessment
[stated plainly — highest residual risk, whether any risk requires escalation before
proceeding]

---
Prepared by the Legal Agent (AI) as a working draft — not legal advice. Requires review
and sign-off by [named human role] before being relied upon.
```

## Rules

- "Accepted" status may only be set by the named human owner, never by this agent
  (`scope-boundaries.md` rule 7 — never silently close a legal action item).
- Likelihood/Severity are qualitative judgments, not the legal conclusion itself — keep
  them visibly separate from any authority-based analysis.
