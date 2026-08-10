# Pattern: Website Scraping

## Situation
Automated retrieval of content or data from a third-party website, for any purpose
(price comparison, document verification, content aggregation).

## Common legal issues
- ToS automation/anti-bot clauses (`tos-and-third-party-risk.md`, `decision-trees/scraping.md`)
- Copyright in the scraped content itself
- Personal data if the scraped content includes identifiable individuals
- Computer Misuse Act 1990 exposure if technical controls are defeated

## Required evidence before proceeding
- Current ToS text, quoted, timestamped
- Confirmation of whether an official API/channel exists
- A description of exactly which technical controls (if any) the target site uses

## Typical implementation considerations
- Rate-limiting on our side, even absent a stated limit, reduces both legal and
  relationship risk
- Respecting `robots.txt` doesn't resolve ToS-level restrictions, but ignoring it is
  itself evidence of intent worth avoiding
- Client-side (in the user's own session) vs. server-side automation materially changes
  the risk tier — always state which

## Cross-references
`decision-trees/scraping.md`, `tos-and-third-party-risk.md`, `playbooks/commercial-and-third-party.md`
