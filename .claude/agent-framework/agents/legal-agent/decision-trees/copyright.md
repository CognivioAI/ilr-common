# Decision Tree: Copyright

Triage for using, incorporating, or generating content that raises copyright questions —
covers both "can we use this input" and "who owns this output" angles.

```
Is the content being used an input (something we're incorporating) or an output
(something being generated, including by AI)?
    │
    ├─ Input ──→ Do we have a license or is it otherwise free to use (public domain,
    │   sufficiently transformed under a recognized exception)?
    │      │
    │      ├─ Licensed / permitted ──→ Confirm the license scope actually covers the
    │      │   intended use (commercial use, modification, redistribution as applicable)
    │      │   — a license permitting one use doesn't imply permission for another.
    │      │
    │      └─ Not clearly licensed ──→ STOP. Do not incorporate without either
    │          obtaining a license or getting confirmation it falls under a genuine
    │          exception (e.g. UK text-and-data-mining exception — narrow, checked
    │          case by case, not assumed).
    │
    └─ Output (AI-generated or otherwise) ──→ Is UK copyright likely to subsist in the
        output at all?
           │
           ├─ Purely AI-generated with no identifiable human creative input ──→ UK law
           │   (CDPA 1988 s.9(3)) has a computer-generated works provision, but its
           │   application to modern generative AI is genuinely unsettled — flag Low
           │   confidence, recommend counsel input rather than asserting ownership.
           │
           └─ Human-authored or substantially human-directed ──→ Standard authorship
               analysis; confirm who the "author" is under the platform's own
               employment/contractor arrangements (work-for-hire implications).
```

## Output

Feeds `patterns/ai-training.md` / `patterns/ai-inference.md` where AI-generated content is
involved, or `workflows/document-analysis.md` for licensed third-party input review.
