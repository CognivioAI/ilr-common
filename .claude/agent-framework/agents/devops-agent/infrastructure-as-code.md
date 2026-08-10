# Infrastructure as Code

## Purpose

Define IaC principles, standards, and practices. Never manually create AWS resources — everything is versioned, reviewed, and repeatable.

---

## Core Principles

1. **Everything in Git** — No ClickOps, no console changes in production
2. **Idempotent** — Running IaC twice produces the same result
3. **Modular** — Reusable modules, DRY principles
4. **Tested** — Validate before apply (plan, lint, security scan)
5. **State Protected** — Remote state with locking
6. **Environment Parity** — Same modules, different variables

---

## Repository Structure

```
infrastructure/
├── terraform/
│   ├── modules/                    # Reusable modules
│   │   ├── networking/
│   │   │   ├── main.tf
│   │   │   ├── variables.tf
│   │   │   ├── outputs.tf
│   │   │   └── README.md
│   │   ├── eks/
│   │   ├── ecs/
│   │   ├── rds/
│   │   ├── s3/
│   │   ├── sqs/
│   │   ├── iam/
│   │   ├── monitoring/
│   │   └── security/
│   │
│   ├── environments/              # Environment-specific configs
│   │   ├── dev/
│   │   │   ├── main.tf
│   │   │   ├── terraform.tfvars
│   │   │   └── backend.tf
│   │   ├── staging/
│   │   │   ├── main.tf
│   │   │   ├── terraform.tfvars
│   │   │   └── backend.tf
│   │   └── production/
│   │       ├── main.tf
│   │       ├── terraform.tfvars
│   │       └── backend.tf
│   │
│   └── global/                    # Shared resources (IAM, DNS)
│       ├── iam/
│       ├── route53/
│       └── ecr/
│
├── kubernetes/                    # K8s manifests (if not using Helm)
│   ├── base/
│   └── overlays/
│
└── scripts/
    ├── bootstrap.sh               # Initial account setup
    └── destroy.sh                 # Teardown script
```

---

## State Management

### Remote State Configuration

```hcl
terraform {
  backend "s3" {
    bucket         = "ilr-terraform-state"
    key            = "environments/dev/terraform.tfstate"
    region         = "eu-west-2"
    encrypt        = true
    dynamodb_table = "terraform-state-lock"
  }
}
```

### Rules

- One state file per environment per component
- State bucket versioning enabled
- DynamoDB locking to prevent concurrent modifications
- Never store state locally in production
- State bucket access restricted to CI/CD and platform team

---

## Module Standards

### Module Interface

Every module must have:

```
modules/service-name/
├── main.tf          # Core resources
├── variables.tf     # Input variables (typed, validated)
├── outputs.tf       # Output values
├── versions.tf      # Provider version constraints
├── locals.tf        # Computed values
└── README.md        # Usage documentation
```

### Variable Conventions

```hcl
variable "environment" {
  description = "Environment name (dev, staging, production)"
  type        = string
  validation {
    condition     = contains(["dev", "staging", "production"], var.environment)
    error_message = "Environment must be dev, staging, or production."
  }
}

variable "service_name" {
  description = "Name of the service being deployed"
  type        = string
}

variable "tags" {
  description = "Common tags applied to all resources"
  type        = map(string)
  default     = {}
}
```

### Output Conventions

```hcl
output "service_endpoint" {
  description = "The endpoint URL for the deployed service"
  value       = aws_lb.main.dns_name
}

output "security_group_id" {
  description = "Security group ID for the service"
  value       = aws_security_group.service.id
}
```

---

## IaC Pipeline

```
Commit (terraform/)
    │
    ▼
terraform fmt — check formatting
    │
    ▼
terraform validate — syntax check
    │
    ▼
tflint — linting rules
    │
    ▼
tfsec / checkov — security scanning
    │
    ▼
terraform plan — preview changes
    │
    ▼
Manual Approval (for staging/production)
    │
    ▼
terraform apply — provision resources
    │
    ▼
Post-apply validation (smoke tests)
```

---

## Anti-Patterns (Never Do)

| Anti-Pattern | Correct Approach |
|-------------|-----------------|
| Console changes in production | All changes via Terraform |
| Hardcoded values | Use variables and tfvars |
| Giant monolithic state | Split by environment and component |
| No state locking | Always use DynamoDB lock table |
| Storing secrets in tfvars | Use AWS Secrets Manager + data sources |
| Wildcard IAM permissions | Least-privilege, resource-specific ARNs |
| No module versioning | Pin module versions in source |
