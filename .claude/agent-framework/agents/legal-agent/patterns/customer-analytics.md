# Pattern: Customer Analytics

## Situation
Collecting and analyzing user behaviour data (page views, feature usage, funnel
completion) — typically via a third-party analytics tool or an in-house event pipeline.

## Common legal issues
- Cookie/tracking-technology consent (`decision-trees/cookies.md`) if any client-side
  identifier is used
- Personal-data processing even when "just usage data" — device/browser fingerprints and
  usage patterns are frequently identifiable when combined with account data
- Third-party analytics vendor as a sub-processor — DPA coverage
  (`playbooks/uk-gdpr-and-ico.md`)
- International transfer if the analytics vendor processes data outside the UK/EEA

## Required evidence before proceeding
- The specific data points collected, listed (not "analytics data" as a category)
- The analytics vendor's DPA and data-residency terms
- Confirmation of the consent mechanism in place before any non-essential tracking fires

## Typical implementation considerations
- Prefer aggregated/pseudonymized analytics where the underlying question doesn't require
  individual-level data
- Ensure the consent mechanism actually gates the tracking technically, not just
  procedurally (`decision-trees/cookies.md`'s "fires before consent" failure mode)

## Cross-references
`decision-trees/cookies.md`, `decision-trees/personal-data.md`, `playbooks/uk-gdpr-and-ico.md`
