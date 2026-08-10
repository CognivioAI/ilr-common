# Kubernetes Conventions

> Version 1.0 | AI-DLC Engineering Handbook
> Purpose: Define Kubernetes deployment standards for all services.

---

## 1. Principles

- Every service is a Helm chart
- Immutable deployments (no runtime changes)
- Resource limits on everything
- Health checks on everything
- GitOps workflow (ArgoCD)

---

## 2. Required Resources Per Service

Every microservice deployment must include:

| Resource | Purpose |
|----------|---------|
| Deployment | Runs the application pods |
| Service | Internal networking / load balancing |
| ConfigMap | Non-sensitive configuration |
| Secret | Sensitive configuration |
| Ingress | External access routing |
| HorizontalPodAutoscaler | Auto-scaling |
| PodDisruptionBudget | Availability during maintenance |
| ServiceMonitor | Prometheus metrics scraping |

---

## 3. Resource Limits

**Always define both requests and limits.**

```yaml
resources:
  requests:
    cpu: 100m
    memory: 256Mi
  limits:
    cpu: 500m
    memory: 512Mi
```

### Sizing Guidelines

| Service Type | CPU Request | Memory Request |
|-------------|------------|----------------|
| Lightweight API | 100m | 256Mi |
| Standard service | 250m | 512Mi |
| Heavy processing | 500m | 1Gi |
| Database client | 250m | 512Mi |

---

## 4. Health Probes

```yaml
startupProbe:
  httpGet:
    path: /actuator/health
    port: 8080
  initialDelaySeconds: 10
  periodSeconds: 5
  failureThreshold: 30

readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 5
  periodSeconds: 10
  failureThreshold: 3

livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 15
  failureThreshold: 3
```

---

## 5. Image Policy

- **Never use `latest` tag**
- Use immutable semantic version tags: `1.2.3`
- Use SHA digest for critical production workloads
- Pull from private ECR registry only
- Scan images before deployment

```yaml
image:
  repository: 123456789.dkr.ecr.us-east-1.amazonaws.com/order-service
  tag: "1.2.3"    # ✅ Immutable
  # tag: "latest"  # ❌ Never
  pullPolicy: IfNotPresent
```

---

## 6. Secrets

- Use External Secrets Operator (AWS Secrets Manager → K8s Secret)
- **Never** hardcode values in manifests
- **Never** commit secrets to Git
- Rotate secrets automatically

```yaml
apiVersion: external-secrets.io/v1beta1
kind: ExternalSecret
metadata:
  name: order-service-secrets
spec:
  refreshInterval: 1h
  secretStoreRef:
    name: aws-secrets-manager
    kind: ClusterSecretStore
  target:
    name: order-service-secrets
  data:
    - secretKey: DB_PASSWORD
      remoteRef:
        key: /production/order-service/db-password
```

---

## 7. Auto-Scaling

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: order-service-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: order-service
  minReplicas: 2
  maxReplicas: 10
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70
```

---

## 8. AI Agent Prompt

```
Generate production-ready Kubernetes manifests.
Include startup, readiness, and liveness probes.
Include resource requests and limits.
Separate ConfigMaps and Secrets.
Never use latest image tag.
Use External Secrets Operator for secrets.
Include HorizontalPodAutoscaler.
Include PodDisruptionBudget.
Use Helm chart structure.
```

---

## 9. Checklist

- [ ] Deployment with resource limits
- [ ] All three probes configured
- [ ] Immutable image tags (no latest)
- [ ] ConfigMap for configuration
- [ ] External Secrets for sensitive data
- [ ] HPA with CPU/memory metrics
- [ ] PodDisruptionBudget defined
- [ ] ServiceMonitor for Prometheus
- [ ] Ingress with TLS
- [ ] Pod security context (non-root)
