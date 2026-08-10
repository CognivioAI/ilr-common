# Document Analysis by Type

Generic "summarize this document" is the wrong prompt for legal review — different
document types carry different review criteria and different risk points. Use the matching
checklist below; run `reasoning/issue-spotting.md` first for anything the checklist doesn't
anticipate.

## NDA
Parties and their capacity to bind; definition of "Confidential Information" (too broad or
too narrow); term and survival period; carve-outs (independently developed, public domain,
compelled disclosure); return/destruction obligations; jurisdiction and governing law;
remedies (injunctive relief clause).

## Employment Contract
Parties, start date, continuous-service implications; notice periods; restrictive covenants
(non-compete, non-solicit — enforceability turns on reasonableness of scope/duration);
IP assignment; disciplinary/grievance procedure references; governing jurisdiction.

## Terms of Service (third party we integrate with) — see `tos-and-third-party-risk.md`
for the full methodology; this entry is the pointer, not a duplicate.

## Privacy Policy (third party)
What data they collect from us/our users; their stated lawful basis; sub-processor
disclosures; retention periods; whether it's consistent with any DPA we'd need
(`playbooks/commercial-and-third-party.md`).

## Supplier / Vendor Agreement
Service levels and remedies for breach; data-processing terms (Article 28 requirements if
personal data is in scope — `playbooks/uk-gdpr-and-ico.md`); liability caps and carve-outs;
termination rights; IP ownership of deliverables; audit rights.

## Lease
Term, break clauses, permitted use, repair obligations, rent review mechanism — flagged
only for issue-spotting; real estate is not this platform's domain, escalate early.

## DPA (Data Processing Agreement)
Roles (controller/processor/joint-controller) correctly stated; Article 28(3) mandatory
content (subject matter, duration, nature, purpose, data types, subjects, controller
obligations); sub-processor authorization mechanism; international transfer mechanism if
applicable; breach-notification timeline; audit rights; deletion/return on termination.

## Court Order / Judgment
Parties, court, date, the actual order (not just the reasoning); compliance deadline; any
appeal window still open; extract holding/ratio per `workflows/chronology-and-precedent-extraction.md`
if the judgment is being used as precedent rather than complied with directly.

## ICO Notice (Information Notice, Enforcement Notice, Penalty Notice)
Statutory basis cited; specific findings alleged; compliance deadline; appeal window (First-
tier Tribunal, Information Rights); whether it names individuals within the org.

## Letter Before Action / Letter Before Claim
Cause of action alleged; remedy demanded; response deadline (Practice Direction – Pre-
Action Conduct timelines); whether limitation is a live issue; recommend counsel review
before any response is drafted, let alone sent (`counsel-handoff.md`).

## Immigration Rules / Visa Guidance page (platform's own domain)
Extract the rule text verbatim with citation and version date
(`research/citation-verification.md`); cross-check against any rule verification already
recorded for this platform rather than re-deriving from scratch; never translate the rule
into advice for a specific applicant's case — that crosses into OISC territory
(`playbooks/immigration-boundary.md`).

## Standard output for any of the above

Facts extracted → issues spotted → missing information → risk flags → confidence per
`reasoning/uncertainty-and-confidence.md` → recommended next step, named owner if any
decision is needed.
