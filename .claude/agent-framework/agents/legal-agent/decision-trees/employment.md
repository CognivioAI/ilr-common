# Decision Tree: Employment

Triage for the missing-facts gathering `playbooks/employment.md` requires — this tree helps
route the question fast; the playbook remains the substantive checklist.

```
Is this about an existing employee's continued employment (discipline, dismissal,
redundancy) or about a new hire / general policy?
    │
    ├─ Existing employee, action being considered ──→ STOP analysis here.
    │   Route straight to playbooks/employment.md's missing-facts checklist and
    │   escalate to named counsel before any action is taken or communicated. This
    │   agent does not analyze toward a "you can/can't" conclusion on a live employment
    │   action.
    │
    └─ New hire / policy / monitoring question ──→ Which sub-area?
                  │
                  ├─ Monitoring (email, screen, location, productivity tools) ──→
                  │   Personal-data question first (decision-trees/personal-data.md) —
                  │   employee monitoring is Article 6 processing with its own
                  │   proportionality expectations (ICO guidance on workplace
                  │   monitoring). Transparency to staff is generally required unless a
                  │   narrow covert-monitoring exception applies (rare, high-scrutiny).
                  │
                  ├─ Contract terms (new template, restrictive covenants) ──→
                  │   playbooks/employment.md issue checklist; covenant reasonableness
                  │   (scope/duration/geography) is fact-specific — flag for counsel
                  │   review rather than concluding enforceability.
                  │
                  └─ General policy (e.g. remote work, expenses) ──→ Usually low legal-
                      risk; still issue-spot for discrimination angle (does the policy
                      disadvantage any protected characteristic) before treating as
                      routine.
```

## Output

Feeds `playbooks/employment.md`.
