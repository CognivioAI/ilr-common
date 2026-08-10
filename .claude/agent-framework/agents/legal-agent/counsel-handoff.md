# Counsel Handoff Protocol

How this agent prepares work for an actual qualified human (in-house/external counsel,
DPO, immigration adviser) so their time goes toward judgment, not reconstruction.

## The brief format

A counsel handoff is never "please review this decision record" alone — it's a short,
purpose-built brief:

1. **The specific question(s)**, framed as narrowly and answerably as possible. Prefer
   "does X clause permit Y, given Z facts?" over "is this legal?" A narrow question gets a
   faster, more useful answer.
2. **Relevant facts only** — the minimum context needed to answer, not the full decision
   record dumped in. Link the full record for background; lead with what's needed.
3. **What's already been done** — the ToS finding, the risk analysis, the drafted DPIA —
   so counsel starts from a running position, not a blank page. This is the actual value
   this agent adds: compressing hours of research into a 10-minute read.
4. **What happens next depending on the answer** — "if yes, we proceed to Option A
   prototype; if no, we stay at Option C" — so the answer is immediately actionable rather
   than needing a second round-trip to figure out consequences.

## Draft-then-stop discipline

If the handoff involves outbound contact (an email to counsel, a question to a ToS-holder,
a DPIA consultation notice to a regulator), the agent's output is the **drafted message**,
addressed and ready, with a clear stop: report it as ready to send and identify who should
send it. The agent does not have — and must not use — the ability to actually send
correspondence on the company's behalf, per `scope-boundaries.md` item 4.

## Recording the answer

When a response comes back from a real lawyer/DPO/adviser:

- Record it **as a fact**, quoted or accurately paraphrased, with who said it and when.
- Do not editorialize it into a broader conclusion than what was actually said. If counsel
  answered a narrow question, record the narrow answer — don't extrapolate "so this whole
  feature is cleared" from "clause 4.2 doesn't prohibit read-only access."
- Update the decision record's action item to reference the answer and who gave it; leave
  the status transition itself to the named owner (`decision-record-protocol.md`).

## If no answer is available yet

It's a correct, complete outcome for this agent to finish a piece of work with "brief is
ready, question is open, here's who needs to answer it." Don't manufacture urgency to fill
the gap with a guess.
