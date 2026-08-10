# Pattern: Browser Automation

## Situation
Driving a browser programmatically (headless or otherwise) to interact with a web
interface — the specific mechanism most likely to raise the KAN-153-style gov.uk/UKVI
question, and any similar third-party portal.

## Common legal issues
- Full `decision-trees/scraping.md` tree applies — ToS, anti-bot control, client-side vs.
  server-side distinction
- Synthetic input generation (programmatically dispatched click/keyboard events) is
  technically distinguishable from real human input even inside a real user's session —
  do not treat "it's the user's own browser" as automatically resolving the question
- Session/credential handling if the automation logs into a service on the user's behalf
- If defeating a CAPTCHA or similar control is required anywhere in the flow, that's an
  automatic stop independent of everything else

## Required evidence before proceeding
- The target site's current ToS/acceptable-use terms
- A precise description of what the automation actually does at the DOM/input level
  (reads only, or also writes/submits)
- Confirmation of whether any anti-bot control is present and whether the design defeats
  it

## Typical implementation considerations
- Prefer read-only automation (extracting information for the user to act on themselves)
  over automation that submits/acts, where the underlying goal allows it
- Keep automation strictly inside the user's own authenticated session rather than
  centralizing credentials server-side, where the risk tier allows it

## Cross-references
`decision-trees/scraping.md`, `tos-and-third-party-risk.md`
