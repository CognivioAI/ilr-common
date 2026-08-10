# Reference Architecture: Serverless

> AWS managed services, pay-per-use, zero server management.

---

## When to Use

- Variable or unpredictable traffic
- Event-driven workloads
- Small team, low operational capacity
- Cost-sensitive (pay only for usage)
- Tasks complete in < 15 minutes

---

## Architecture

```
Client → API Gateway (HTTP API)
              │
              ├── Lambda (CRUD operations)
              │     └── DynamoDB or RDS Proxy → PostgreSQL
              │
              └── S3 (file upload)
                    │ (S3 event)
                    ▼
                  Lambda (processing)
                    │
                    ▼
                  SQS → Lambda (AI inference via Bedrock)
```

---

## Key Decisions

| Decision | Choice |
|----------|--------|
| Compute | AWS Lambda |
| API | API Gateway (HTTP API) |
| Database | DynamoDB (simple) or RDS + Proxy (complex) |
| Events | S3 events + SQS + EventBridge |
| AI | Bedrock (invoked from Lambda) |

---

## Limitations

- Cold starts (mitigated with provisioned concurrency)
- 15 minute execution limit
- Complex local debugging
- Higher vendor lock-in

---

## Cost (low-to-medium traffic)

- Lambda: ~$5–50/month (pay per request)
- API Gateway: ~$3.50/million requests
- DynamoDB: ~$5–25/month (on-demand)
- S3: < $5/month
- **Total: ~$20–100/month**
