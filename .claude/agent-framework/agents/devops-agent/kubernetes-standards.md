# Kubernetes Standards

## Purpose

Define Kubernetes deployment patterns, mandatory configurations, resource management, and security policies for all workloads.

---

## Mandatory Requirements

Every Kubernetes deployment **must** include:

1. ✅ Resource limits and requests
2. ✅ Health checks (liveness + readiness probes)
3. ✅ Security context (non-root, read-only filesystem)
4. ✅ Pod disruption budgets
5. ✅ Network policies
6. ✅ Labels and annotations
7. ✅ Horizontal Pod Autoscaler

---

## Deployment Template

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: document-service
  namespace: ilr-production
  labels:
    app.kubernetes.io/name: document-service
    app.kubernetes.io/version: "1.2.0"
    app.kubernetes.io/component: backend
    app.kubernetes.io/part-of: ilr-platform
    app.kubernetes.io/managed-by: helm
  annotations:
    deployment.kubernetes.io/revision: "1"
spec:
  replicas: 3
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxUnavailable: 1
      maxSurge: 1
  selector:
    matchLabels:
      app.kubernetes.io/name: document-service
  template:
    metadata:
      labels:
        app.kubernetes.io/name: document-service
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/port: "8080"
        prometheus.io/path: "/actuator/prometheus"
    spec:
      serviceAccountName: document-service
      securityContext:
        runAsNonRoot: true
        runAsUser: 1000
        fsGroup: 1000
      containers:
        - name: document-service
          image: 123456789.dkr.ecr.eu-west-2.amazonaws.com/document-service:1.2.0
          ports:
            - containerPort: 8080
              protocol: TCP
          resources:
            requests:
              cpu: 500m
              memory: 512Mi
            limits:
              cpu: "2"
              memory: 2Gi
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 10
            failureThreshold: 3
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            initialDelaySeconds: 10
            periodSeconds: 5
            failureThreshold: 3
          startupProbe:
            httpGet:
              path: /actuator/health
              port: 8080
            initialDelaySeconds: 10
            periodSeconds: 5
            failureThreshold: 30
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: "production"
            - name: DB_URL
              valueFrom:
                secretRef:
                  name: database-credentials
                  key: url
          securityContext:
            allowPrivilegeEscalation: false
            readOnlyRootFilesystem: true
            capabilities:
              drop:
                - ALL
          volumeMounts:
            - name: tmp
              mountPath: /tmp
      volumes:
        - name: tmp
          emptyDir: {}
      topologySpreadConstraints:
        - maxSkew: 1
          topologyKey: topology.kubernetes.io/zone
          whenUnsatisfiable: DoNotSchedule
          labelSelector:
            matchLabels:
              app.kubernetes.io/name: document-service
```

---

## Resource Guidelines

### Per Environment

| Environment | CPU Request | CPU Limit | Memory Request | Memory Limit | Replicas |
|-------------|-------------|-----------|----------------|--------------|----------|
| dev | 250m | 1 | 256Mi | 1Gi | 1 |
| staging | 500m | 2 | 512Mi | 2Gi | 2 |
| production | 500m | 2 | 512Mi | 2Gi | 3-10 |

### By Service Type

| Service Type | CPU Request | Memory Request | Notes |
|-------------|-------------|----------------|-------|
| API Service | 500m | 512Mi | Standard Spring Boot |
| Worker | 1000m | 1Gi | CPU-intensive processing |
| AI Worker | 500m | 1Gi | Mostly waiting on API calls |
| React Frontend | 100m | 128Mi | Static served by Nginx |

---

## Horizontal Pod Autoscaler

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: document-service
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: document-service
  minReplicas: 3
  maxReplicas: 10
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70
    - type: Resource
      resource:
        name: memory
        target:
          type: Utilization
          averageUtilization: 80
  behavior:
    scaleDown:
      stabilizationWindowSeconds: 300
      policies:
        - type: Pods
          value: 1
          periodSeconds: 60
    scaleUp:
      stabilizationWindowSeconds: 60
      policies:
        - type: Percent
          value: 50
          periodSeconds: 60
```

---

## Pod Disruption Budget

```yaml
apiVersion: policy/v1
kind: PodDisruptionBudget
metadata:
  name: document-service
spec:
  minAvailable: 2
  selector:
    matchLabels:
      app.kubernetes.io/name: document-service
```

---

## Network Policies

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: document-service
spec:
  podSelector:
    matchLabels:
      app.kubernetes.io/name: document-service
  policyTypes:
    - Ingress
    - Egress
  ingress:
    - from:
        - podSelector:
            matchLabels:
              app.kubernetes.io/name: api-gateway
      ports:
        - protocol: TCP
          port: 8080
  egress:
    - to:
        - podSelector:
            matchLabels:
              app.kubernetes.io/name: database
      ports:
        - protocol: TCP
          port: 5432
    - to:  # Allow DNS
        - namespaceSelector: {}
      ports:
        - protocol: UDP
          port: 53
```

---

## RBAC

```yaml
apiVersion: v1
kind: ServiceAccount
metadata:
  name: document-service
  annotations:
    eks.amazonaws.com/role-arn: arn:aws:iam::123456789:role/document-service-role
---
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  name: document-service
rules:
  - apiGroups: [""]
    resources: ["configmaps", "secrets"]
    verbs: ["get", "list"]
---
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: document-service
subjects:
  - kind: ServiceAccount
    name: document-service
roleRef:
  kind: Role
  name: document-service
  apiGroup: rbac.authorization.k8s.io
```

---

## Namespace Strategy

```yaml
namespaces:
  ilr-platform:     # Core platform services
  ilr-ai:           # AI workers and agents
  ilr-monitoring:   # Prometheus, Grafana
  ilr-ingress:      # Ingress controllers
  cert-manager:     # TLS certificate management
```

---

## Labels Standard

| Label | Purpose | Example |
|-------|---------|---------|
| `app.kubernetes.io/name` | Service name | `document-service` |
| `app.kubernetes.io/version` | Version | `1.2.0` |
| `app.kubernetes.io/component` | Component type | `backend`, `frontend`, `worker` |
| `app.kubernetes.io/part-of` | System name | `ilr-platform` |
| `app.kubernetes.io/managed-by` | Management tool | `helm`, `argocd` |
