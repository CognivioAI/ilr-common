# Pattern Recognition Module

> Architecture Agent — Solution Recommendation Engine

---

## Purpose

Before generating architecture options, the agent identifies which architectural
patterns best fit the requirement. This acts as a reasoning step between
"what are we building?" and "how should we build it?"

---

## Pattern Recognition Flow

```
Requirement
    ↓
Pattern Recognition (this module)
    ↓
Candidate Architectures (from reference-architectures/)
    ↓
Technology Selection (from technology-decisions.md)
    ↓
Reference Architecture (customized from library)
    ↓
Detailed Design
```

---

## Pattern Catalog

| Scenario | Primary Pattern | Secondary Pattern |
|----------|----------------|-------------------|
| CRUD business application | Layered Monolith | MVC |
| AI document processing | Event-Driven | Agent Orchestrator |
| Real-time chat | WebSocket | Pub/Sub |
| Reporting / analytics | CQRS | Read replicas |
| High-traffic APIs | API Gateway | Load balancing |
| Long-running workflows | Workflow Engine | State machine |
| File/document processing | Queue-Based | Worker pools |
| AI multi-agent system | Agent Orchestrator | Event-Driven |
| Search-heavy application | RAG | Vector DB + Cache |
| Notifications | Pub/Sub | Fan-out |
| Multi-tenant SaaS | Shared DB, schema isolation | API Gateway |
| Batch data processing | ETL Pipeline | Queue + Workers |
| IoT / streaming | Event Streaming | Time-series DB |
| Authentication/Identity | Token-based (JWT) | OAuth 2.0 / OIDC |

---

## Pattern Selection Rules

### Step 1: Identify Primary Characteristic

| If the requirement is primarily about... | Start with... |
|------------------------------------------|---------------|
| Storing and retrieving data (CRUD) | Layered Architecture |
| Processing data asynchronously | Event-Driven |
| AI inference on unstructured data | Agent Orchestrator |
| High read:write ratio | CQRS |
| Orchestrating multiple steps | Workflow Engine |
| Delivering messages to many consumers | Pub/Sub |
| Connecting frontend to multiple backends | API Gateway |
| Real-time bidirectional communication | WebSocket |
| Finding information semantically | RAG |

### Step 2: Identify Secondary Patterns

Most features combine patterns. Common combinations:

```
AI Document Processing = Event-Driven + Agent Orchestrator + Queue-Based + RAG
SaaS Application = Layered + API Gateway + CQRS (for reporting)
ILR Platform = Event-Driven + Agent Orchestrator + Workflow Engine + RAG
```

### Step 3: Map to Reference Architecture

Once patterns are identified, select the closest reference from:
`agents/architect-agent/reference-architectures/`

---

## Pattern Details

### Layered Monolith

```
Client → Controller → Service → Repository → Database
```

- Use when: MVP, single team, simple domain
- Evolves into: Modular monolith → Microservices

### Event-Driven

```
Producer → Event Bus → Consumer(s)
```

- Use when: Decoupling, async processing, multiple consumers
- Key tech: SQS, SNS, Kafka

### Agent Orchestrator

```
Orchestrator → Agent 1 → Agent 2 → Agent 3 → Result
```

- Use when: Multiple AI models, confidence routing, human-in-the-loop
- Key tech: Spring Boot, SQS, Bedrock

### CQRS (Command Query Responsibility Segregation)

```
Write side: Command → Handler → Event Store
Read side:  Event → Projection → Read Model → Query
```

- Use when: Complex domain, different read/write models, event sourcing
- Overhead: High — only when justified

### Workflow Engine

```
Start → Step 1 → Decision → Step 2A or 2B → Wait → Resume → End
```

- Use when: Long-running processes, human approvals, retries, state tracking
- Key tech: AWS Step Functions, Temporal, custom state machine

### RAG (Retrieval-Augmented Generation)

```
Query → Embed → Vector Search → Context + Query → LLM → Response
```

- Use when: AI needs domain knowledge, reducing hallucination
- Key tech: pgvector, Bedrock Embeddings, Claude

### Queue-Based Processing

```
Request → Queue → Worker Pool → Result Store → Notification
```

- Use when: Variable load, long processing time, batch jobs
- Key tech: SQS, ECS workers, S3 results

### API Gateway

```
Client → Gateway → Auth → Rate Limit → Route → Service
```

- Use when: Multiple backends, authentication, rate limiting
- Key tech: Spring Cloud Gateway, AWS API Gateway, Kong

---

## Pattern Anti-Patterns

| Don't Use | When |
|-----------|------|
| Microservices | Team < 3, domain unclear, MVP phase |
| Event sourcing | Simple CRUD, no audit requirements |
| CQRS | Read and write models are identical |
| Workflow engine | Simple linear operations |
| RAG | Factual data in a well-structured database |
| Agent orchestrator | Single deterministic AI call |

---

## Output

After pattern recognition, produce:

```
## Identified Patterns

Primary: [Pattern Name]
Secondary: [Pattern Name(s)]
Reference Architecture: [Link to reference-architectures/*.md]

Reasoning: [2-3 sentences explaining why these patterns fit]
```
