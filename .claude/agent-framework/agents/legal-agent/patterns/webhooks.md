# Pattern: Webhooks (Inbound and Outbound)

## Situation
Receiving asynchronous push notifications from a third party (inbound) or sending them to
a third party or customer-controlled endpoint (outbound).

## Common legal issues
- Inbound: verifying the sender's identity (signature verification) — a security control
  with a legal dimension, since an unverified webhook is an unauthenticated data-
  ingestion path
- Outbound: sending personal data to a customer-controlled endpoint the platform doesn't
  control the security of — does this create a transfer the platform isn't equipped to
  vouch for
- Retry/replay behaviour — could a retried webhook duplicate a processing action with its
  own legal consequence (e.g. duplicate notification, duplicate charge)
- Payload content — is more data being sent in the webhook payload than the receiving
  system actually needs

## Required evidence before proceeding
- Confirmation of signature verification (inbound) or endpoint validation (outbound)
- What data fields are actually in the payload, listed explicitly
- For outbound webhooks to customer endpoints: what the platform's terms say about the
  customer's own security obligations for data they choose to receive this way

## Typical implementation considerations
- Minimize payload content to what's operationally required
- Log delivery attempts for evidence-preservation and dispute-resolution purposes
- Treat webhook endpoints as a distinct attack surface in any security review this
  pattern feeds into

## Cross-references
`patterns/api-integrations.md`, `playbooks/commercial-and-third-party.md`
