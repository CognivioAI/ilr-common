# Kubernetes Architecture Module

> Architecture Agent — Deployment Topology Discipline

---

## Purpose

Define how the Architecture Agent designs the Kubernetes deployment topology,
including resource planning, scaling strategy, and service mesh decisions.

---

## Topology Decisions

### Cluster Strategy

| Option | When |
|--------|------|
| Single cluster, namespaces per env | Small team, cost-sensitive |
| Cluster per environment | Stronger isolation, larger team |
| Multi-cluster with federation | Multi-region, enterprise |

### Namespace Strategy

```
production/
├── user-service
├── document-service
├── ai-orchestrator
├── notification-service
├── audit-service
└── knowledge-service

monitoring/
├── prometheus
├── grafana
└── alertmanager

ingress/
└── nginx-ingress / aws-alb-controller
```

---

## Resource Planning

For each service, estimate:

| Service | CPU Request | Memory Request | Replicas (Min) | Replicas (Max) |
|---------|------------|----------------|----------------|----------------|
| API Gateway | 250m | 512Mi | 2 | 6 |
| User Service | 100m | 256Mi | 2 | 4 |
| Document Service | 250m | 512Mi | 2 | 8 |
| AI Orchestrator | 500m | 1Gi | 2 | 10 |
| Notification Service | 100m | 256Mi | 1 | 3 |
| Audit Service | 100m | 256Mi | 2 | 4 |

---

## Scaling Strategy

| Service | Scaling Trigger | HPA Metric |
|---------|----------------|-----------|
| Document Service | Upload volume | CPU > 70% |
| AI Orchestrator | Queue depth | SQS messages visible |
| Notification Service | Event rate | CPU > 60% |
| All | Burst traffic | CPU + custom metrics |

---

## Service Mesh Decision

| Option | When |
|--------|------|
| No mesh | < 5 services, simple communication |
| Istio | mTLS required, traffic management, observability |
| AWS App Mesh | AWS-native, simpler than Istio |
| Linkerd | Lightweight, easy to adopt |

**Default**: No mesh for < 5 services. Add when mTLS or advanced traffic routing is needed.

---

## Node Pool Strategy

| Pool | Instance Type | Purpose | Labels |
|------|-------------|---------|--------|
| General | m5.large | Standard workloads | `pool=general` |
| AI | m5.xlarge | AI orchestrator, high memory | `pool=ai` |
| System | t3.medium | Monitoring, ingress, system pods | `pool=system` |

---

## Checklist

- [ ] Cluster strategy decided (single/multi)
- [ ] Namespace strategy defined
- [ ] Resource requests/limits estimated per service
- [ ] HPA metrics and thresholds defined
- [ ] Node pool strategy defined
- [ ] Service mesh decision (ADR)
- [ ] Minimum replicas ≥ 2 for production
- [ ] PodDisruptionBudget per service
