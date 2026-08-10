# AWS Networking Standards

> Version 1.0 | AI-DLC Engineering Handbook
> Purpose: Define networking architecture for AWS deployments.

---

## 1. VPC Design

### One VPC Per Environment

| Environment | CIDR | Purpose |
|-------------|------|---------|
| Production | 10.0.0.0/16 | Live traffic |
| Staging | 10.1.0.0/16 | Pre-production validation |
| Development | 10.2.0.0/16 | Development and testing |

### Subnet Strategy

```
VPC (10.0.0.0/16)
├── Public Subnets (10.0.0.0/20)      ← ALB, NAT Gateway only
│   ├── AZ-a: 10.0.0.0/22
│   ├── AZ-b: 10.0.4.0/22
│   └── AZ-c: 10.0.8.0/22
├── Private App Subnets (10.0.16.0/20) ← ECS/EKS workloads
│   ├── AZ-a: 10.0.16.0/22
│   ├── AZ-b: 10.0.20.0/22
│   └── AZ-c: 10.0.24.0/22
└── Private DB Subnets (10.0.32.0/20)  ← RDS, ElastiCache
    ├── AZ-a: 10.0.32.0/22
    ├── AZ-b: 10.0.36.0/22
    └── AZ-c: 10.0.40.0/22
```

---

## 2. Security Groups

### Principle: Deny All, Allow Specific

```terraform
# ALB Security Group
resource "aws_security_group" "alb" {
  ingress {
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

# Application Security Group
resource "aws_security_group" "app" {
  ingress {
    from_port       = 8080
    to_port         = 8080
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id]  # Only from ALB
  }
}

# Database Security Group
resource "aws_security_group" "db" {
  ingress {
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.app.id]  # Only from App
  }
}
```

---

## 3. VPC Endpoints

Use VPC endpoints to avoid internet transit for AWS services:

- S3 (Gateway endpoint)
- ECR (Interface endpoint)
- Secrets Manager (Interface endpoint)
- SQS (Interface endpoint)
- CloudWatch Logs (Interface endpoint)

---

## 4. Load Balancing

- Application Load Balancer (ALB) for HTTP/HTTPS
- TLS termination at ALB (ACM certificates)
- Path-based routing for microservices
- Health check on each target group
- WAF attached to ALB for request filtering

---

## 5. DNS

- Route 53 for domain management
- Private hosted zones for internal service discovery
- Alias records for AWS resources (ALB, CloudFront)

---

## 6. Anti-Patterns

- ❌ Public subnets for application servers
- ❌ Security groups with 0.0.0.0/0 on non-ALB resources
- ❌ Direct internet access from application instances
- ❌ Hardcoded IP addresses in configurations
- ❌ Single AZ deployments in production

---

## 7. Checklist

- [ ] VPC per environment with proper CIDR planning
- [ ] Multi-AZ deployment (minimum 2)
- [ ] Private subnets for workloads and databases
- [ ] Security groups follow least-privilege
- [ ] VPC endpoints for AWS services
- [ ] NAT Gateway for outbound internet (private subnets)
- [ ] ALB with TLS and WAF
- [ ] No public IPs on application instances
