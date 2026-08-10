# Terms-of-Service & Third-Party Integration Risk Review

Methodology for assessing whether the platform can integrate with, automate against, or
assist a user in interacting with a third-party system (a government service, a bank, a
document-verification API, etc.) — the exact question raised by KAN-153 (gov.uk/UKVI).

## Step 1 — Look for an official channel first

Before treating a third party's public-facing site as something to work *around*, check
whether an official integration path exists:

- A public developer portal, sandbox, or partner/API program.
- A named "authorised software provider" or "recognised third-party" scheme (UK
  government services increasingly have these — e.g. HMRC's Making Tax Digital software
  providers). If one exists for the relevant department, that changes the entire risk
  picture: sanctioned integration replaces automation-with-legal-risk.
- If nothing is published, the highest-leverage move is often **drafting a direct question
  to the service owner** asking whether third-party assisted tools are permitted — draft
  it, hand it to a human to send (see `counsel-handoff.md`). A direct answer beats
  inferring intent from ambiguous ToS language.

## Step 2 — Read the actual current ToS, don't assume

Retrieve the live terms/acceptable-use policy and record:

- **Exact clause text** (quoted verbatim), not a paraphrase.
- **URL and retrieval date/time.** Government and corporate ToS pages are edited without
  changelog — a finding is only as good as its timestamp. Re-check on a cadence (monthly
  for anything load-bearing) and diff against the last-recorded text; flag any change into
  the relevant decision record or ticket rather than silently updating the finding.
- Classify each relevant clause into one of:
  - **Explicitly prohibited** — automation/bots/scraping named and disallowed.
  - **Explicitly permitted** — an API or automation path is named and allowed.
  - **Silent / ambiguous** — no automation language either way. Do not treat silence as
    permission; flag it as the open question for legal review.
  - **Conditional** — permitted only via a specific mechanism (API key, partner agreement,
    rate limits) that the current design doesn't use.

## Step 3 — Anti-bot / technical controls are a separate, harder line

Treat evasion of technical bot-detection (CAPTCHA solving, browser fingerprint spoofing,
synthetic-event injection designed to look human, rate-limit circumvention) as an
automatic red flag **independent of ToS wording** — see `regulatory-landscape.md` for why
this tends to be read as evidence of intent in Computer Misuse Act analysis. If a design
requires defeating such a control, that alone is enough to block progress rather than
finish grading the ToS.

## Step 4 — Client-side vs server-side matters, but isn't a free pass

Distinguish and record separately:

- **Server-side automation against the third party** (we hold a session/credentials and
  act as the principal) — highest risk tier, generally requires explicit permission.
- **Client-side assistance inside the end-user's own authenticated session** (e.g. a
  browser extension that reads/writes visible DOM the user is already looking at) — lower
  risk, because the user remains the one interacting with the system, but **still not
  automatically clear**: many ToS define "automated access" broadly enough to cover this,
  and synthetic DOM events are technically distinguishable from real input. Say so plainly
  rather than treating "it's the user's own browser" as a legal conclusion.

## Step 5 — Output format

A ToS/third-party risk finding should always include:

1. The specific clauses found (quoted) + retrieval date.
2. The classification (Step 2) with reasoning.
3. Whether an official channel exists (Step 1) — and if not, a drafted outreach question.
4. The client-side/server-side distinction if relevant (Step 4).
5. An explicit "recommend: ask counsel to confirm X" — never a bare "this is fine."

See `decision-record-protocol.md` for where this analysis gets published, and
`scope-boundaries.md` before writing any of it.
