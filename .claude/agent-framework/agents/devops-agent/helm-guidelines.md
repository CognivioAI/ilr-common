# Helm Guidelines

## Purpose

Define Helm chart structure, values strategy, and release management practices for consistent Kubernetes deployments.

---

## Chart Structure

```
charts/
├── ilr-service/                   # Generic service chart (reusable)
│   ├── Chart.yaml
│   ├── values.yaml                # Default values
│   ├── values-dev.yaml
│   ├── values-staging.yaml
│   ├── values-production.yaml
│   ├── templates/
│   │   ├── _helpers.tpl
│   │   ├── deployment.yaml
│   │   ├── service.yaml
│   │   ├── hpa.yaml
│   │   ├── pdb.yaml
│   │   ├── networkpolicy.yaml
│   │   ├── serviceaccount.yaml
│   │   ├── configmap.yaml
│   │   ├── secret.yaml
│   │   └── ingress.yaml
│   └── README.md
│
├── ilr-worker/                    # Worker chart (queue consumers)
│   ├── Chart.yaml
│   ├── values.yaml
│   └── templates/
│
└── ilr-platform/                  # Umbrella chart
    ├── Chart.yaml
    ├── values.yaml
    └── charts/                    # Dependencies
```

---

## Chart.yaml Standard

```yaml
apiVersion: v2
name: ilr-service
description: Standard Helm chart for ILR platform services
type: application
version: 1.0.0        # Chart version (semver)
appVersion: "1.2.0"   # Application version
maintainers:
  - name: Platform Team
    email: platform@ilr.com
dependencies:
  - name: postgresql
    version: "13.x.x"
    repository: "https://charts.bitnami.com/bitnami"
    condition: postgresql.enabled
```

---

## Values Strategy

### Default values.yaml

```yaml
# values.yaml - sensible defaults
replicaCount: 1

image:
  repository: ""      # Required - no default
  tag: ""             # Required - no default
  pullPolicy: IfNotPresent

service:
  type: ClusterIP
  port: 8080

resources:
  requests:
    cpu: 250m
    memory: 256Mi
  limits:
    cpu: "1"
    memory: 1Gi

autoscaling:
  enabled: false
  minReplicas: 1
  maxReplicas: 5
  targetCPUUtilization: 70

probes:
  liveness:
    path: /actuator/health/liveness
    initialDelaySeconds: 30
  readiness:
    path: /actuator/health/readiness
    initialDelaySeconds: 10

securityContext:
  runAsNonRoot: true
  runAsUser: 1000

serviceAccount:
  create: true
  annotations: {}

ingress:
  enabled: false
  className: ""
  hosts: []
  tls: []

env: {}
secrets: {}

networkPolicy:
  enabled: true

podDisruptionBudget:
  enabled: false
  minAvailable: 1
```

### Environment Overrides

```yaml
# values-production.yaml
replicaCount: 3

resources:
  requests:
    cpu: 500m
    memory: 512Mi
  limits:
    cpu: "2"
    memory: 2Gi

autoscaling:
  enabled: true
  minReplicas: 3
  maxReplicas: 10
  targetCPUUtilization: 70

podDisruptionBudget:
  enabled: true
  minAvailable: 2

ingress:
  enabled: true
  className: alb
  hosts:
    - host: api.ilr.example.com
      paths:
        - path: /api/documents
          pathType: Prefix
  tls:
    - secretName: ilr-tls
      hosts:
        - api.ilr.example.com
```

---

## Helpers Template

```yaml
# templates/_helpers.tpl

{{- define "ilr-service.fullname" -}}
{{- printf "%s-%s" .Release.Name .Chart.Name | trunc 63 | trimSuffix "-" }}
{{- end }}

{{- define "ilr-service.labels" -}}
helm.sh/chart: {{ .Chart.Name }}-{{ .Chart.Version }}
app.kubernetes.io/name: {{ .Chart.Name }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{- define "ilr-service.selectorLabels" -}}
app.kubernetes.io/name: {{ .Chart.Name }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}
```

---

## Release Management

### Naming Convention

```yaml
release_naming:
  pattern: "{service}-{environment}"
  examples:
    - document-service-production
    - api-gateway-staging
    - ai-worker-dev
```

### Upgrade Strategy

```bash
# Always use --atomic for production
helm upgrade --install document-service-production ./charts/ilr-service \
  --namespace ilr-production \
  --values charts/ilr-service/values-production.yaml \
  --set image.repository=123456789.dkr.ecr.eu-west-2.amazonaws.com/document-service \
  --set image.tag=1.2.0 \
  --atomic \
  --timeout 5m \
  --wait
```

### Rollback

```bash
# View history
helm history document-service-production -n ilr-production

# Rollback to previous
helm rollback document-service-production 1 -n ilr-production --wait
```

---

## Chart Testing

```bash
# Lint
helm lint ./charts/ilr-service

# Template render (dry-run)
helm template test ./charts/ilr-service -f values-production.yaml

# Install with --dry-run
helm install test ./charts/ilr-service --dry-run --debug

# Chart testing framework
helm test document-service-production -n ilr-production
```

---

## Rules

1. Never use `latest` tag in values
2. Always pin chart dependency versions
3. Use `--atomic` for production deploys (auto-rollback on failure)
4. Never store secrets in values files (use External Secrets Operator)
5. All charts must pass `helm lint` before merge
6. Document all values in README.md with descriptions
