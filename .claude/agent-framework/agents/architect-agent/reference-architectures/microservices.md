# Reference Architecture: Microservices

> Independently deployable services, each owning its own data and API.

---

## When to Use

- Multiple teams working independently
- Services have different scaling needs
- Domain boundaries well understood
- Need independent deployment cadence
- System complexity justifies operational overhead

---

## Architecture

```
Client → API Gateway → Service A (own DB)
                     → Service B (own DB)
                     → Service C (own DB)
                            ↕ (SQS/SNS events)
```

---

## Key Decisions

| Decision | Choice |
|----------|--------|
| Deployment | EKS (Helm + ArgoCD) |
| Communication (sync) | REST / gRPC |
| Communication (async) | SNS + SQS |
| Database | Per-service PostgreSQL |
| Service discovery | Kubernetes DNS |
| Observability | Prometheus + Grafana + OpenTelemetry |

---

## Cost (5 services, production)

- EKS cluster: ~$72/month (control plane)
- EC2 nodes (3x m5.large): ~$230/month
- RDS (per service): ~$100/month each
- Total: ~$700–1500/month
