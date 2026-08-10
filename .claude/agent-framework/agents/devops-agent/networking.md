# Networking

## Purpose

Define VPC architecture, subnet strategy, security groups, load balancing, DNS, and service mesh patterns.

---

## VPC Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│  VPC: 10.0.0.0/16 (65,536 IPs)                                     │
│                                                                     │
│  ┌─────────────────────────────┐  ┌─────────────────────────────┐  │
│  │  AZ: eu-west-2a             │  │  AZ: eu-west-2b             │  │
│  │                             │  │                             │  │
│  │  Public: 10.0.1.0/24        │  │  Public: 10.0.2.0/24        │  │
│  │  ├── ALB                    │  │  ├── ALB                    │  │
│  │  └── NAT Gateway            │  │  └── NAT Gateway            │  │
│  │                             │  │                             │  │
│  │  Private: 10.0.11.0/24      │  │  Private: 10.0.12.0/24      │  │
│  │  ├── EKS Nodes              │  │  ├── EKS Nodes              │  │
│  │  ├── ECS Tasks              │  │  ├── ECS Tasks              │  │
│  │  └── Lambda                 │  │  └── Lambda                 │  │
│  │                             │  │                             │  │
│  │  Data: 10.0.21.0/24         │  │  Data: 10.0.22.0/24         │  │
│  │  ├── RDS                    │  │  ├── RDS (Standby)          │  │
│  │  └── ElastiCache            │  │  └── ElastiCache            │  │
│  └─────────────────────────────┘  └─────────────────────────────┘  │
│                                                                     │
│  VPC Endpoints:                                                     │
│  ├── S3 (Gateway)                                                   │
│  ├── ECR (Interface)                                                │
│  ├── Secrets Manager (Interface)                                    │
│  ├── SQS (Interface)                                                │
│  └── CloudWatch Logs (Interface)                                    │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Subnet Strategy

| Subnet Type | CIDR Pattern | Purpose | Internet Access |
|-------------|-------------|---------|-----------------|
| Public | 10.0.{1,2}.0/24 | ALB, NAT Gateway | Direct (IGW) |
| Private | 10.0.{11,12}.0/24 | Application workloads | Outbound only (NAT) |
| Data | 10.0.{21,22}.0/24 | Databases, caches | No internet access |

---

## Security Groups

### Layered Security

```yaml
security_groups:
  alb:
    inbound:
      - port: 443, source: 0.0.0.0/0 (HTTPS only)
      - port: 80, source: 0.0.0.0/0 (redirect to HTTPS)
    outbound:
      - port: 8080, destination: app-sg

  app:
    inbound:
      - port: 8080, source: alb-sg
      - port: 8080, source: app-sg (inter-service)
    outbound:
      - port: 5432, destination: db-sg
      - port: 6379, destination: cache-sg
      - port: 443, destination: 0.0.0.0/0 (AWS APIs)

  database:
    inbound:
      - port: 5432, source: app-sg
    outbound:
      - none (no internet access)

  cache:
    inbound:
      - port: 6379, source: app-sg
    outbound:
      - none
```

---

## Load Balancing

### Application Load Balancer

```yaml
alb:
  type: application
  scheme: internet-facing
  
  listeners:
    https:
      port: 443
      protocol: HTTPS
      certificate: ACM (auto-renewed)
      default_action: forward to target group
    http:
      port: 80
      protocol: HTTP
      default_action: redirect to HTTPS

  target_groups:
    document-service:
      port: 8080
      protocol: HTTP
      health_check:
        path: /actuator/health
        interval: 15s
        healthy_threshold: 2
        unhealthy_threshold: 3
    
    react-frontend:
      port: 8080
      protocol: HTTP
      health_check:
        path: /
        interval: 15s

  routing_rules:
    - path: /api/*  →  backend target group
    - path: /*      →  frontend target group
```

---

## DNS (Route 53)

```yaml
dns:
  zone: ilr.example.com
  
  records:
    production:
      - api.ilr.example.com → ALB (alias)
      - app.ilr.example.com → CloudFront (alias)
    staging:
      - staging-api.ilr.example.com → Staging ALB
      - staging.ilr.example.com → Staging CloudFront
    dev:
      - dev-api.ilr.example.com → Dev ALB
      - dev.ilr.example.com → Dev CloudFront

  health_checks:
    - endpoint: https://api.ilr.example.com/actuator/health
      interval: 30s
      failover: DR region
```

---

## VPC Endpoints (Private Connectivity)

```yaml
vpc_endpoints:
  gateway:
    - s3           # Free, no NAT cost for S3
    - dynamodb     # Free, no NAT cost for DynamoDB
  
  interface:
    - ecr.api             # Pull images without internet
    - ecr.dkr             # Docker registry
    - secretsmanager      # Access secrets privately
    - sqs                 # Queue access
    - logs                # CloudWatch Logs
    - monitoring          # CloudWatch Metrics
    - sts                 # Assume roles
    - execute-api         # API Gateway (if used)
  
  benefit: 
    - Reduced NAT Gateway costs
    - Traffic stays within AWS network
    - Better security (no internet traversal)
```

---

## Service Mesh (Optional — Phase 2)

```yaml
service_mesh:
  tool: AWS App Mesh / Istio
  when_needed:
    - Service-to-service mTLS required
    - Advanced traffic routing (canary %)
    - Circuit breaking needed
    - Distributed tracing required
  
  not_needed_for:
    - Simple service communication
    - < 5 services
    - MVP phase
```

---

## Network Policies (Kubernetes)

```yaml
default_policy: deny-all

# Default deny
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: default-deny
  namespace: ilr-production
spec:
  podSelector: {}
  policyTypes:
    - Ingress
    - Egress
```

Then explicitly allow per service (see kubernetes-standards.md).

---

## Cost Considerations

| Component | Monthly Cost (approx) | Optimization |
|-----------|----------------------|--------------|
| NAT Gateway | £30 + data processing | Use VPC endpoints to reduce |
| ALB | £20 + LCU hours | Consolidate listeners |
| VPC Endpoints | £7-10 per interface endpoint | Only enable what's needed |
| Data Transfer | Variable | Keep traffic in-region |
