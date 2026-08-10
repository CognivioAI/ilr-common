# Deployment Standards

> Version 1.0 | AI-DLC Engineering Handbook
> Purpose: Define Kubernetes deployment strategies and patterns.

---

## 1. Deployment Strategy

### Rolling Update (Default)

```yaml
strategy:
  type: RollingUpdate
  rollingUpdate:
    maxSurge: 1
    maxUnavailable: 0
```

- Zero-downtime deployments
- `maxUnavailable: 0` ensures all existing pods stay up during rollout
- New pods must pass readiness check before old pods terminate

---

## 2. Deployment Template

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: order-service
  labels:
    app.kubernetes.io/name: order-service
    app.kubernetes.io/version: "1.2.3"
    app.kubernetes.io/component: backend
    app.kubernetes.io/part-of: ecommerce-platform
    app.kubernetes.io/managed-by: helm
spec:
  replicas: 2
  selector:
    matchLabels:
      app.kubernetes.io/name: order-service
  template:
    metadata:
      labels:
        app.kubernetes.io/name: order-service
        app.kubernetes.io/version: "1.2.3"
    spec:
      serviceAccountName: order-service
      securityContext:
        runAsNonRoot: true
        runAsUser: 1000
        fsGroup: 1000
      containers:
        - name: order-service
          image: 123456.dkr.ecr.us-east-1.amazonaws.com/order-service:1.2.3
          ports:
            - containerPort: 8080
              protocol: TCP
          envFrom:
            - configMapRef:
                name: order-service-config
            - secretRef:
                name: order-service-secrets
          resources:
            requests:
              cpu: 250m
              memory: 512Mi
            limits:
              cpu: 500m
              memory: 1Gi
          securityContext:
            readOnlyRootFilesystem: true
            allowPrivilegeEscalation: false
            capabilities:
              drop: ["ALL"]
```

---

## 3. Pod Disruption Budget

Ensures availability during node drains and cluster upgrades:

```yaml
apiVersion: policy/v1
kind: PodDisruptionBudget
metadata:
  name: order-service-pdb
spec:
  minAvailable: 1
  selector:
    matchLabels:
      app.kubernetes.io/name: order-service
```

---

## 4. Labels

### Required Labels (Kubernetes Recommended)

| Label | Purpose | Example |
|-------|---------|---------|
| `app.kubernetes.io/name` | Service name | `order-service` |
| `app.kubernetes.io/version` | Version | `1.2.3` |
| `app.kubernetes.io/component` | Role | `backend`, `frontend` |
| `app.kubernetes.io/part-of` | System | `ecommerce-platform` |
| `app.kubernetes.io/managed-by` | Tooling | `helm`, `argocd` |

---

## 5. Rollback

- Kubernetes maintains revision history automatically
- Use: `kubectl rollout undo deployment/order-service`
- ArgoCD: Sync to previous Git commit
- Always keep at least 3 revision history records

```yaml
spec:
  revisionHistoryLimit: 5
```

---

## 6. Graceful Shutdown

```yaml
spec:
  terminationGracePeriodSeconds: 30
  containers:
    - lifecycle:
        preStop:
          exec:
            command: ["sh", "-c", "sleep 5"]
```

Spring Boot application should handle `SIGTERM`:
- Stop accepting new requests
- Complete in-flight requests
- Close database connections
- Deregister from service discovery

---

## 7. Anti-Patterns

- ❌ `replicas: 1` in production (no HA)
- ❌ Running as root
- ❌ No resource limits (can starve other pods)
- ❌ No PDB (pods evicted during maintenance)
- ❌ No graceful shutdown (dropped connections)
- ❌ Writable root filesystem

---

## 8. Checklist

- [ ] Rolling update strategy with `maxUnavailable: 0`
- [ ] Minimum 2 replicas in production
- [ ] Pod security context (non-root, read-only FS)
- [ ] PodDisruptionBudget defined
- [ ] Labels follow Kubernetes recommended convention
- [ ] Graceful shutdown configured
- [ ] Revision history retained
- [ ] Service account per deployment
