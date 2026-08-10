# Reference Architecture: Document Processing

> File upload → async processing pipeline → structured output.

---

## When to Use

- Users upload files (PDF, images, documents)
- Files need processing (OCR, analysis, conversion)
- Processing takes seconds to minutes
- Users should not wait synchronously
- Output is structured data or a transformed file

---

## Architecture

```
Client
    │ (presigned URL upload)
    ▼
S3 Bucket (original files)
    │ (S3 event)
    ▼
SQS Queue
    │
    ▼
Processing Worker(s)
    ├── Stage 1: Validation (file type, size, virus scan)
    ├── Stage 2: OCR / Text extraction
    ├── Stage 3: Data extraction / classification
    └── Stage 4: Output generation
    │
    ▼
PostgreSQL (metadata, status, results)
S3 (processed outputs)
    │
    ▼
SNS Notification → Client polling / WebSocket
```

---

## Key Patterns

- **Presigned URL**: Client uploads directly to S3 (bypasses API for large files)
- **S3 Event Trigger**: Processing starts automatically on upload
- **Queue-based workers**: Scale independently from API
- **Status polling**: Client polls for completion (or WebSocket push)

---

## Key Decisions

| Decision | Choice |
|----------|--------|
| Upload mechanism | S3 presigned URL (client-direct) |
| Processing trigger | S3 event → SQS |
| Worker deployment | ECS/EKS with auto-scaling |
| Status delivery | REST polling (simple) or WebSocket (real-time) |
| Failure handling | DLQ + retry + manual review queue |

---

## Scaling

- Upload capacity: S3 (unlimited)
- Processing throughput: Worker count (HPA on queue depth)
- Status queries: API service (standard scaling)
