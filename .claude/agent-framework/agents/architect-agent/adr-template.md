# ADR Template

> Architecture Agent — Architecture Decision Record Discipline

---

## Purpose

Every significant architectural decision must be recorded as an ADR.
ADRs form the project's architectural memory — they explain *why* decisions
were made, what alternatives were considered, and when to reconsider.

---

## When to Write an ADR

Write an ADR when deciding:
- Compute platform (Lambda vs ECS vs EKS)
- Database technology (PostgreSQL vs DynamoDB)
- Messaging system (SQS vs Kafka)
- AI model selection (Bedrock vs custom)
- Architecture style (monolith vs microservices)
- Authentication approach (Cognito vs custom)
- Any decision that would be expensive to reverse

---

## ADR Template

```markdown
# ADR-{number}: {Title}

## Status

Proposed | Accepted | Deprecated | Superseded by ADR-{n}

## Date

YYYY-MM-DD

## Context

What is the situation that requires a decision?
What constraints exist?
What requirements must be satisfied?

## Decision

What is the decision and why was it chosen?

## Options Considered

### Option A: {Name}

- **Description**: [What this option entails]
- **Pros**: [Benefits]
- **Cons**: [Drawbacks]
- **Cost**: [Relative cost: Low/Medium/High]

### Option B: {Name}

- **Description**: [What this option entails]
- **Pros**: [Benefits]
- **Cons**: [Drawbacks]
- **Cost**: [Relative cost: Low/Medium/High]

### Option C: {Name}

- **Description**: [What this option entails]
- **Pros**: [Benefits]
- **Cons**: [Drawbacks]
- **Cost**: [Relative cost: Low/Medium/High]

## Decision Rationale

Why was this option selected over the alternatives?
Reference the scoring criteria (speed, cost, scalability, security, maintainability).

## Consequences

### Positive

- [Benefits that come from this decision]

### Negative

- [Trade-offs accepted]

### Risks

- [What could go wrong]
- [Mitigation strategies]

## When to Reconsider

- [Trigger conditions that would prompt revisiting this decision]
  - e.g., "If monthly document volume exceeds 100k, revisit SQS vs Kinesis"
  - e.g., "If team grows beyond 10 developers, consider splitting monolith"

## References

- Related ADRs
- External documentation
- Benchmark results
```

---

## Example ADRs for ILR Application

### ADR-001: Use event-driven processing for document analysis

**Status**: Accepted
**Context**: Document processing involves OCR, classification, extraction, and validation.
Each step can take several seconds. Users should not wait synchronously.
**Decision**: Use Amazon SQS between services. Each AI step publishes completion events.
**Alternatives**: Direct REST calls, Apache Kafka
**Reason**: SQS provides reliable delivery with DLQ, simpler operations than Kafka,
and decouples services for independent scaling.
**Consequences**: Eventually consistent — user doesn't see results instantly.
Acceptable because we show progress indicators.

---

### ADR-002: Use PostgreSQL over DynamoDB

**Status**: Accepted
**Context**: Application requires complex queries, joins, and transactions.
**Decision**: PostgreSQL (RDS) for all relational data.
**Alternatives**: DynamoDB, MongoDB
**Reason**: Strong ACID guarantees, pgvector for AI embeddings, team SQL expertise.
**Consequences**: Vertical scaling has limits. Acceptable for projected traffic (< 10k concurrent users).

---

### ADR-003: Use AWS Bedrock (Claude) over OpenAI

**Status**: Accepted
**Context**: AI classification and extraction require LLM inference.
**Decision**: AWS Bedrock with Claude Sonnet/Haiku.
**Alternatives**: OpenAI API, self-hosted model, Azure OpenAI
**Reason**: Data stays in AWS account (compliance), no external data transfer,
pay-per-token with no commitments, Haiku is cost-effective for classification.
**Consequences**: Claude-specific prompt engineering. Acceptable — can swap models within Bedrock.

---

## ADR Numbering

- Sequential: ADR-001, ADR-002, ADR-003...
- Store in: `/docs/architecture/adr/` or alongside feature documentation
- Never delete ADRs — mark deprecated/superseded with reference to replacement

---

## Checklist

- [ ] ADR written for every major technology choice
- [ ] At least 2–3 alternatives considered per ADR
- [ ] Pros/cons/cost documented for each option
- [ ] Decision rationale clearly stated
- [ ] Risks and mitigation identified
- [ ] "When to reconsider" triggers defined
- [ ] ADR numbered sequentially
- [ ] Status is current (Accepted / Deprecated)
