# Pattern: Third-Party API Integrations

## Situation
Integrating with a third-party API as an authorized, sanctioned channel — the "official
channel exists" branch of `decision-trees/scraping.md`, treated as its own recurring
pattern because it's the preferred and most common integration shape.

## Common legal issues
- API terms of use — rate limits, permitted use cases, data-handling requirements imposed
  by the provider
- Data flows created by the integration — does calling the API send personal data to the
  provider, creating a sub-processor relationship
- API key/credential custody — who can access the credential, and what happens if it's
  compromised
- Liability terms in the API provider's agreement (often more restrictive than a standard
  commercial contract, offered on a take-it-or-leave-it basis)

## Required evidence before proceeding
- The API's current terms of use / developer agreement, quoted
- Confirmation of what data is sent in each call and the provider's stated handling of it
- A DPA or equivalent if personal data is transmitted

## Typical implementation considerations
- Send the minimum data the API call actually needs
- Store credentials per the platform's existing credential-custody standard rather than
  ad hoc
- Build in graceful handling of the provider changing or revoking terms/access, since this
  is outside the platform's control

## Cross-references
`tos-and-third-party-risk.md`, `playbooks/commercial-and-third-party.md`, `patterns/webhooks.md`
