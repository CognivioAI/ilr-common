# Kubernetes Security Standards

> Version 1.0 | AI-DLC Engineering Handbook
> Purpose: Define security controls for all Kubernetes workloads.

---

## 1. Pod Security

### Security Context (Required on Every Pod)

```yaml
spec:
  securityContext:
    runAsNonRoot: true
    runAsUser: 1000
    fsGroup: 1000
  containers:
    - securityContext:
        readOnlyRootFilesystem: true
        allowPrivilegeEscalation: false
        capabilities:
          drop: ["ALL"]
```

### Rules

- Never run as root
- Read-only root filesystem
- Drop all capabilities
- No privilege escalation
- No host network/PID/IPC

---

## 2. Network Policies

### Default Deny All

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: default-deny-all
  namespace: production
spec:
  podSelector: {}
  policyTypes:
    - Ingress
    - Egress
```

### Allow Specific Traffic

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: allow-order-service-ingress
spec:
  podSelector:
    matchLabels:
      app.kubernetes.io/name: order-service
  policyTypes:
    - Ingress
  ingress:
    - from:
        - podSelector:
            matchLabels:
              app.kubernetes.io/name: api-gateway
      ports:
        - protocol: TCP
          port: 8080
```

---

## 3. RBAC

- Service account per workload
- Minimal permissions on service accounts
- No `cluster-admin` bindings for applications
- Namespace-scoped roles where possible

```yaml
apiVersion: v1
kind: ServiceAccount
metadata:
  name: order-service
  annotations:
    eks.amazonaws.com/role-arn: arn:aws:iam::123456:role/order-service-role
```

---

## 4. Secrets

- Use External Secrets Operator (sync from AWS Secrets Manager)
- Never store secrets in Git (even base64-encoded)
- Encrypt etcd at rest
- Mount secrets as files (not environment variables) when possible
- Rotate secrets regularly

---

## 5. Image Security

| Control | How |
|---------|-----|
| Trusted registry | Pull only from private ECR |
| Vulnerability scanning | Trivy in CI pipeline |
| Image signing | Cosign/Notation |
| No latest tag | Immutable version tags |
| Minimal base image | Distroless or Alpine |

---

## 6. Runtime Security

- Falco for runtime threat detection
- Audit logging enabled on API server
- OPA/Gatekeeper for policy enforcement
- Restrict hostPath mounts
- Restrict nodePort services

---

## 7. Anti-Patterns

- ❌ Running containers as root
- ❌ Writable root filesystem
- ❌ `*` in network policies (allow all)
- ❌ Secrets in environment variables visible in `kubectl describe`
- ❌ Shared service accounts across deployments
- ❌ Pulling from public registries in production

---

## 8. Checklist

- [ ] Pod security context enforced (non-root, read-only, no escalation)
- [ ] Default-deny network policies
- [ ] Explicit allow policies per service
- [ ] Service account per workload
- [ ] External Secrets Operator for secrets
- [ ] Images from private registry only
- [ ] Images scanned for vulnerabilities
- [ ] etcd encryption enabled
- [ ] Audit logging active
