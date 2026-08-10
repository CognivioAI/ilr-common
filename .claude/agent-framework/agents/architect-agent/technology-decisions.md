# Technology Decision Matrix

> Architecture Agent — Technology Selection Engine

---

## Purpose

For every major technology choice, the Architecture Agent evaluates candidates
against consistent criteria. It never says "Use X" without explaining why X
was chosen over alternatives.

---

## Process

```
Requirement (e.g., "need a queue")
    ↓
Identify category (Messaging)
    ↓
List candidates from matrix below
    ↓
Evaluate against criteria (latency, cost, ops, team expertise)
    ↓
Score and recommend
    ↓
Write ADR
```

---

## Compute

| Option | Best For | Scaling | Ops Complexity | Cost Model |
|--------|----------|---------|----------------|-----------|
| AWS Lambda | Event-driven, short tasks (< 15 min) | Auto (instant) | Low | Pay per invocation |
| ECS Fargate | Stateless services, small teams | Task-based | Medium | Pay per vCPU/memory |
| EKS | Complex orchestration, multi-team | Pod-based (HPA) | High | Cluster + nodes |
| EC2 | Legacy, GPU workloads, custom OS | ASG | High | Per instance |

### Decision Triggers

- Lambda: Tasks < 15 min, event-driven, cost-sensitive, low traffic
- ECS: Small team, 2-5 services, simpler ops than K8s
- EKS: > 5 services, Helm/ArgoCD, platform team exists
- EC2: Only if managed services cannot satisfy requirements

---

## Database

| Option | Best For | Query Patterns | Scale Limit | Cost |
|--------|----------|---------------|-------------|------|
| PostgreSQL (RDS) | ACID, complex queries, joins | SQL, full-text, pgvector | Vertical (read replicas) | Medium |
| DynamoDB | Key-value, extreme scale | Single-table, key access | Unlimited horizontal | Pay per RCU/WCU |
| MongoDB (DocumentDB) | Flexible schema, nested docs | Document queries | Horizontal (sharding) | Medium-High |
| Redis (ElastiCache) | Cache, sessions, counters | Key-value, pub/sub | Cluster mode | Medium |
| pgvector | AI embeddings, semantic search | Vector similarity | With PostgreSQL | Included in RDS |
| OpenSearch | Full-text search, analytics | Elasticsearch DSL | Horizontal | High |

### Decision Triggers

- PostgreSQL: Default choice (covers 90% of use cases)
- DynamoDB: Only when access patterns are simple AND scale is extreme
- MongoDB: Only for truly schema-less data (rare in practice)
- Redis: Caching and ephemeral data only
- pgvector: When AI embeddings needed alongside relational data
- OpenSearch: When PostgreSQL FTS is insufficient

---

## Messaging / Queues

| Option | Best For | Ordering | Delivery | Ops |
|--------|----------|----------|----------|-----|
| SQS Standard | General async, decoupling | Best-effort | At-least-once | Zero |
| SQS FIFO | Ordered processing per entity | Strict (per group) | Exactly-once | Zero |
| SNS + SQS | Fan-out (one event, many consumers) | Per-queue | At-least-once | Zero |
| Kafka (MSK) | Event streaming, replay, high volume | Partition-ordered | At-least-once | High |
| RabbitMQ | Complex routing, priority queues | Per-queue | At-least-once | Medium |
| EventBridge | AWS-native event routing, rules | Best-effort | At-least-once | Low |

### Decision Triggers

- SQS: Default for async (simple, managed, reliable)
- SNS + SQS: When multiple services consume the same event
- Kafka: Only when you need event replay, stream processing, or > 100k msg/sec
- RabbitMQ: Only for complex routing patterns (rarely needed on AWS)
- EventBridge: AWS service-to-service events, scheduled rules

---

## AI / LLM

| Option | Best For | Data Residency | Cost | Latency |
|--------|----------|---------------|------|---------|
| Bedrock (Claude) | Production AI, compliance | AWS account | Pay per token | Medium |
| Bedrock (Titan) | Embeddings, lightweight tasks | AWS account | Low | Low |
| OpenAI API | Rapid prototyping | External | Pay per token | Low |
| Azure OpenAI | Enterprise + Microsoft stack | Azure tenant | Pay per token | Low |
| Self-hosted (SageMaker) | Custom models, full control | Your infra | High (fixed) | Variable |

### Decision Triggers

- Bedrock (Claude): Default for production — data stays in account, compliance-ready
- Bedrock (Titan): Embeddings, simple classification tasks
- OpenAI: Prototyping only — not recommended for PII workloads
- Self-hosted: Only for custom-trained models with specific requirements

---

## Search

| Option | Best For | Query Type | Ops | Scalability |
|--------|----------|-----------|-----|-------------|
| PostgreSQL FTS | Simple search within existing DB | Keywords, trigram | None (same DB) | Good for < 1M docs |
| pgvector | Semantic/AI search + relational | Vector similarity | None (same DB) | Good for < 10M vectors |
| OpenSearch | Heavy search, analytics, facets | Full-text, aggregation | Medium-High | Excellent |
| Pinecone | Managed vector-only search | Vector similarity | Zero | Excellent |

### Decision Triggers

- PostgreSQL FTS: Start here. Move away only when proven insufficient
- pgvector: When AI embeddings are needed alongside relational queries
- OpenSearch: When search is a primary feature with complex faceting
- Pinecone: When you need managed vector at scale without ops burden

---

## Authentication

| Option | Best For | Customization | Ops | Cost |
|--------|----------|--------------|-----|------|
| Amazon Cognito | AWS-native, SaaS, social login | Medium | Low | Free tier + per-MAU |
| Auth0 | Feature-rich, rapid integration | High | Zero | Per-MAU (expensive at scale) |
| Keycloak | Full control, on-premises option | Very High | High (self-hosted) | Free (OSS) |
| Custom JWT | Specific requirements, existing IdP | Full | High | Development cost |

### Decision Triggers

- Cognito: Default on AWS — good enough for most SaaS
- Auth0: When advanced features needed (breached password detection, MFA variety)
- Keycloak: When running on-premises or need full control
- Custom JWT: Only when integrating with existing enterprise IdP

---

## Storage

| Option | Best For | Access Pattern | Durability | Cost |
|--------|----------|---------------|-----------|------|
| S3 | Documents, backups, static assets | Object key | 99.999999999% | Very low |
| EFS | Shared file system across containers | File path | 99.999999999% | Medium |
| EBS | Block storage for single instance | Volume mount | 99.999% | Medium |

### Decision Triggers

- S3: Default for any file/document storage
- EFS: Only when multiple containers need shared filesystem (rare)
- EBS: Only for EC2 instances needing block storage

---

## Evaluation Template

When comparing options, produce:

```markdown
## Technology Decision: [Category]

### Requirement
[What we need]

### Candidates
| Criteria | Option A | Option B | Option C |
|----------|----------|----------|----------|
| Meets functional need | ✅ | ✅ | ✅ |
| Team expertise | High | Low | Medium |
| Operational complexity | Low | High | Medium |
| Cost (monthly estimate) | $X | $Y | $Z |
| Scalability | Good | Excellent | Good |
| Vendor lock-in risk | Low | High | Medium |
| Community/Support | Strong | Strong | Medium |

### Recommendation
[Option X] because [reasoning].

### ADR Reference
ADR-{n}: Why [chosen option] over [alternatives]
```
