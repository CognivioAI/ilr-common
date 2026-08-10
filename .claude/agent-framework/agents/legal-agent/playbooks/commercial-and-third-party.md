# Playbook: Commercial & Third-Party Integration

For vendor contracts, supplier agreements, and any new third-party integration. This is the
generalized version of the KAN-153 pattern — `tos-and-third-party-risk.md` remains the
detailed methodology for the ToS/automation-specific sub-case; this playbook is the wider
commercial-review checklist it sits inside.

## Issue checklist

- Is there an official integration channel (`tos-and-third-party-risk.md` Step 1) before
  anything else is analyzed
- Data flows: does this integration create a new personal-data processing activity
  (`playbooks/uk-gdpr-and-ico.md`) — if so, is a DPA needed
- Liability: caps, exclusions, indemnities — who bears the risk if the third party fails
- IP: ownership of outputs, license scope, any restriction on our own product built on top
- Termination: notice period, data-return/deletion obligations on exit
- Service levels and remedies for breach
- Governing law and dispute-resolution mechanism
- Anti-bot / technical-control considerations (`tos-and-third-party-risk.md` Step 3) if the
  integration involves any automated interaction with the third party's systems

## Escalation triggers

- Any integration involving special-category or credential data
- Any contract with liability terms that could expose the company beyond the value of the
  engagement
- Any automation against a system with no official API/partner channel (route through the
  full `tos-and-third-party-risk.md` methodology, then to counsel)

## Output

Risk note (`templates/risk-register.md`) or decision record (`decision-record-protocol.md`)
depending on whether this is a live proposal that reverses an existing assumption or a
routine vendor review.
