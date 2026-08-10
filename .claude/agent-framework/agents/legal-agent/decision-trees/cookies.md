# Decision Tree: Cookies / Similar Technologies

Triage for whether a cookie or similar storage technology (localStorage, tracking pixel,
device fingerprint) needs consent under PECR (Privacy and Electronic Communications
Regulations 2003) and the UK GDPR.

```
Is the technology "strictly necessary" for a service the user explicitly requested?
  (e.g. a session cookie needed to keep the user logged in mid-transaction)
    │
    ├─ Yes ──→ Consent not required under PECR reg. 6(4) exemption. Still confirm no
    │          personal data implications beyond the exemption's scope
    │          (playbooks/uk-gdpr-and-ico.md).
    │
    └─ No ──→ Is it used for analytics, personalization, or advertising?
                │
                ├─ Yes ──→ Prior, informed, specific consent required (PECR reg. 6 +
                │          UK GDPR Article 4(11)/7). Confirm:
                │            - consent is opt-in, not opt-out or pre-ticked
                │            - the mechanism records consent evidence
                │            - the technology only activates after consent, not before
                │
                └─ No, but data is collected ──→ Treat as a personal-data processing
                           question in its own right — route to
                           playbooks/uk-gdpr-and-ico.md rather than resolving as a cookie
                           question.
```

## Common failure mode this catches

A cookie/tracking script that fires on page load *before* the consent banner is answered —
technically "consent was requested" but functionally consent was never actually obtained
before processing began. Flag this explicitly if found; it's one of the most common ICO
enforcement patterns.

## Output

Feeds `playbooks/uk-gdpr-and-ico.md`'s issue checklist and, if consent-flow implementation
choices are in play, `reasoning/implementation-options.md`.
