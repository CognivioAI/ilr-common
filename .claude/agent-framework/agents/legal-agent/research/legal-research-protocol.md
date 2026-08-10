# Legal Research Protocol

Most legal-research failures in an LLM agent come from reading one webpage and stopping.
A solicitor's research habit is different in kind, not just thoroughness — this module
codifies the specific checks that make research defensible.

## Researching legislation

1. **Locate the current version, not the "as enacted" version.** legislation.gov.uk
   publishes both — always use the version marked as currently in force, and note its
   "point in time" date.
2. **Check for amendments.** Read the legislation.gov.uk "Changes to Legislation" /
   amendment table for the specific section relied on. An unamended reading of a heavily
   amended section is a common and serious error.
3. **Verify commencement.** An Act being passed is not the same as a specific section being
   in force — commencement is often staggered across Statutory Instruments, sometimes years
   apart. State the commencement status of the specific provision relied on, not just the
   Act.
4. **Check for repeal.** Confirm the section hasn't been repealed or superseded before
   citing it as current law.
5. **Compare versions if the timing of an event matters.** If the fact pattern spans a
   period during which the law changed, identify which version applied at the relevant
   date — don't apply today's version to yesterday's facts by default.
6. **Locate explanatory notes.** They're not law, but they're the clearest Level A-adjacent
   source for what a provision was intended to do — useful for interpreting ambiguous text,
   never for overriding it.

## Researching case law

1. **Verify jurisdiction.** A Scottish case is not binding in England & Wales and vice
   versa; an EU-era CJEU case may or may not still be relevant post-Brexit depending on the
   area of retained law.
2. **Identify binding vs. persuasive precedent** per `reasoning/source-hierarchy.md` —
   state which the case is before relying on it.
3. **Distinguish ratio from dicta.** Only the ratio decidendi (the reasoning necessary to
   the decision) is binding; obiter dicta is persuasive at most. State which is being cited.
4. **Trace the citation chain.** If a case is cited via a secondary source, verify it
   against the actual judgment (BAILII, the official law report) before relying on it —
   secondary sources routinely mischaracterize holdings.
5. **Check it hasn't been overturned, distinguished, or superseded** by a later judgment or
   subsequent legislation.

## Output

Every research finding states: the source, its evidence grade (`reasoning/evidence-grading.md`),
its position on `reasoning/source-hierarchy.md`, the retrieval/access date, and whether it
was independently verified against a primary source or only located via a secondary one.

Feeds directly into `research/citation-verification.md` — nothing above is complete until
the citation itself passes that gate.
