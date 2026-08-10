# Reference Architecture: RAG (Retrieval-Augmented Generation)

> AI with domain knowledge via vector search + LLM generation.

---

## When to Use

- AI needs to reference domain-specific knowledge
- Reducing hallucination is critical
- Knowledge base changes over time (without retraining)
- Answers must be grounded in source documents
- Explainability required (cite sources)

---

## Architecture

```
Query → Embed Query (Bedrock Titan)
            │
            ▼
        Vector Search (pgvector / Pinecone)
            │ (top-K relevant chunks)
            ▼
        Construct Prompt:
            "Context: [retrieved chunks]
             Question: [user query]
             Answer based only on context."
            │
            ▼
        LLM (Claude Sonnet)
            │
            ▼
        Response + Sources
```

---

## Ingestion Pipeline

```
Source Documents
    │
    ▼
Chunking (500-1000 tokens per chunk, with overlap)
    │
    ▼
Embedding (Bedrock Titan Embeddings)
    │
    ▼
Store in pgvector (vector + metadata + source reference)
```

---

## Key Decisions

| Decision | Choice |
|----------|--------|
| Embedding model | Bedrock Titan Embeddings |
| Vector store | pgvector (alongside PostgreSQL) |
| LLM | Claude Sonnet (accurate, long context) |
| Chunk size | 500–800 tokens, 100 token overlap |
| Retrieval | Top-5 chunks by cosine similarity |

---

## ILR Use Case

- Knowledge base: UK immigration guidance, ILR requirements
- Source: Gov.uk documents, immigration rules, precedent cases
- Query: "What documents prove 5 years continuous residence?"
- Response: Grounded in official guidance, with source citations
