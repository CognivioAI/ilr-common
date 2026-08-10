# Decision Tree: Scraping / Automated Access

A structured first pass for any proposed automated interaction with a third-party system.
This is a triage tool, not a substitute for `tos-and-third-party-risk.md` — every branch
below still resolves into that full methodology, never a bare yes/no answer.

```
Does an official API / partner / developer channel exist?
    │
    ├─ Yes ──→ Use it. Analyze under its terms, not general ToS.
    │          (tos-and-third-party-risk.md Step 1)
    │
    └─ No ──→ Does the ToS explicitly address automation/scraping/bots?
                │
                ├─ Explicitly prohibited ──→ STOP. Do not proceed without a drafted
                │                            question to the service owner
                │                            (counsel-handoff.md) and explicit permission.
                │
                ├─ Explicitly permitted (via a defined mechanism) ──→ Confirm the current
                │                            design actually uses that mechanism; if not,
                │                            treat as "silent" below.
                │
                └─ Silent / ambiguous ──→ Does the design require defeating any technical
                                           anti-bot control (CAPTCHA, fingerprint spoofing,
                                           rate-limit evasion)?
                                              │
                                              ├─ Yes ──→ STOP. Automatic red flag
                                              │          independent of ToS wording
                                              │          (tos-and-third-party-risk.md
                                              │          Step 3). Escalate.
                                              │
                                              └─ No ──→ Is this server-side automation
                                                        (we act as principal) or client-
                                                        side (inside the user's own
                                                        authenticated session)?
                                                            │
                                                            ├─ Server-side ──→ High risk
                                                            │   tier. Draft outreach
                                                            │   question; escalate for
                                                            │   sign-off before building.
                                                            │
                                                            └─ Client-side ──→ Lower risk
                                                                but not automatically
                                                                clear (Step 4). Document
                                                                the distinction; still
                                                                flag as an open question
                                                                for counsel.
```

## After the tree

Every path except "official channel exists and is used" ends in an open question for a
named human — this tree narrows *which* question, it doesn't answer it. Write up the result
using `tos-and-third-party-risk.md`'s Step 5 output format.
