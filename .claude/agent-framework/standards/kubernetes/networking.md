# Kubernetes Networking Standards

> Version 1.0 | AI-DLC Engineering Handbook
> Purpose: Define networking patterns for Kubernetes services.

---

## 1. Service Types

| Type | When to Use |
|------|-------------|
| ClusterIP (default) | Internal service-to-service communication |
| NodePort | Never in production |
| LoadBalancer | Use ALB Ingress Controller instead |

---

## 2. Ingress

Use AWS ALB Ingress Controller:

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: order-service-ingress
  annotations:
    kubernetes.io/ingress.class: alb
    alb.ingress.kubernetes.io/scheme: internet-facing
    alb.ingress.kubernetes.io/target-type: ip
    alb.ingress.kubernetes.io/certificate-arn: arn:aws:acm:...
    alb.ingress.kubernetes.io/listen-ports: '[{"HTTPS": 443}]'
    alb.ingress.kubernetes.io/ssl-redirect: "443"
spec:
  rules:
    - host: api.company.com
      http:
        paths:
          - path: /api/v1/orders
            pathType: Prefix
            backend:
              service:
                name: order-service
                port:
                  number: 8080
```

---

## 3. Service Discovery

Internal services communicate via Kubernetes DNS:

```
http://order-service.production.svc.cluster.local:8080
```

Short form within same namespace:
```
http://order-service:8080
```

---

## 4. Network Policies

### Default Deny (Applied to Every Namespace)

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: default-deny-all
spec:
  podSelector: {}
  policyTypes:
    - Ingress
    - Egress
```

### Allow DNS

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: allow-dns
spec:
  podSelector: {}
  policyTypes:
    - Egress
  egress:
    - to:
        - namespaceSelector: {}
      ports:
        - protocol: UDP
          port: 53
        - protocol: TCP
          port: 53
```

---

## 5. TLS

- TLS termination at ALB (ACM certificates)
- Internal service-to-service: mTLS via service mesh (optional)
- All external traffic over HTTPS
- HTTP → HTTPS redirect enforced

---

## 6. Anti-Patterns

- ❌ NodePort services in production
- ❌ No network policies (all traffic allowed)
- ❌ Hardcoded service IPs
- ❌ Exposing services directly without ingress
- ❌ Missing TLS on external endpoints

---

## 7. Checklist

- [ ] ClusterIP for all internal services
- [ ] ALB Ingress Controller for external access
- [ ] TLS on all ingress
- [ ] Network policies (default deny + explicit allow)
- [ ] DNS-based service discovery
- [ ] No NodePort in production
- [ ] HTTP to HTTPS redirect
