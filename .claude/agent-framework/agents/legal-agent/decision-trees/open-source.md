# Decision Tree: Open-Source Component Use

Triage for pulling a third-party open-source dependency into the platform's codebase.

```
What license is the component under?
    │
    ├─ Permissive (MIT, Apache 2.0, BSD) ──→ Generally low friction. Confirm:
    │      - attribution/notice requirements are actually satisfied (NOTICE files,
    │        license text retained)
    │      - Apache 2.0 patent-grant clause doesn't conflict with any patent position
    │        the company holds (rare, but check if the company holds patents)
    │
    ├─ Weak copyleft (LGPL, MPL) ──→ Confirm linking method (static vs. dynamic) —
    │      obligations differ. Flag for a specific reading of the license against how the
    │      component is actually integrated, not a general "LGPL is fine" assumption.
    │
    ├─ Strong copyleft (GPL, AGPL) ──→ STOP before integrating into proprietary code.
    │      AGPL in particular extends copyleft obligations to network use (SaaS), which is
    │      directly relevant to a platform delivered as a service. Escalate — this is a
    │      product-architecture decision, not just a legal footnote.
    │
    └─ No license file / unclear ──→ Treat as "all rights reserved" by default (absence of
        a license is not permission). Do not integrate without clarifying the license with
        the maintainer or finding an alternative component.
```

## Additional checks regardless of license

- Is the component actively maintained, or does using it create an unaddressed security-
  patching gap (an operational risk that compounds any legal one)?
- Does the component itself bundle other dependencies with different licenses (license
  compatibility is transitive — check the dependency tree, not just the top-level license)?

## Output

Feeds `workflows/architecture-legal-review.md` when a dependency choice is part of a
broader architecture decision.
