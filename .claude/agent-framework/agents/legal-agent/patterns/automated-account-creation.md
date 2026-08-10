# Pattern: Automated Account Creation

## Situation
The platform programmatically creates or manages an account with a third-party service on
a user's behalf (e.g. registering for a portal, initiating a service request).

## Common legal issues
- ToS terms on who may create an account and how (many services require the account
  holder to act personally)
- Agency/authority — does the platform have clear authorization from the user to act as
  their agent for this specific act
- Data accuracy — an automatically created account populated with user data creates a
  new personal-data processing activity with the third party as a recipient
- Click-wrap/click-through acceptance — if account creation requires accepting the third
  party's terms, who is legally accepting them (the user, or the platform acting for them)

## Required evidence before proceeding
- The third party's account-creation terms, quoted
- A clear, recorded user authorization for the platform to act on their behalf for this
  specific act
- Confirmation of what data is transmitted to create the account and under what lawful
  basis

## Typical implementation considerations
- Prefer flows where the user completes the final acceptance step themselves (reduces the
  agency/click-wrap ambiguity) over fully silent automation
- Log the authorization and the exact data sent, for evidence-preservation purposes

## Cross-references
`decision-trees/scraping.md`, `decision-trees/personal-data.md`, `tos-and-third-party-risk.md`
