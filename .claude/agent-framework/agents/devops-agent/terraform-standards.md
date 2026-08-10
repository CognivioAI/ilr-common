# Terraform Standards

## Purpose

Define Terraform coding conventions, patterns, and CI integration for consistent, maintainable infrastructure code.

---

## Version Constraints

```hcl
terraform {
  required_version = ">= 1.6.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.25"
    }
    helm = {
      source  = "hashicorp/helm"
      version = "~> 2.12"
    }
  }
}
```

---

## Naming Conventions

```yaml
naming:
  resources:
    pattern: "{project}-{environment}-{service}-{resource}"
    example: "ilr-prod-document-service-sg"
  files:
    - main.tf        # Primary resources
    - variables.tf   # Input declarations
    - outputs.tf     # Output declarations
    - locals.tf      # Local values
    - data.tf        # Data sources
    - versions.tf    # Provider/terraform version constraints
  variables:
    style: snake_case
    example: database_instance_class
  resources:
    style: snake_case
    example: aws_ecs_service.document_processor
  modules:
    style: kebab-case directories
    example: modules/ecs-service/
```

---

## Module Patterns

### Standard Service Module

```hcl
# modules/ecs-service/main.tf

resource "aws_ecs_service" "this" {
  name            = "${var.project}-${var.environment}-${var.service_name}"
  cluster         = var.cluster_arn
  task_definition = aws_ecs_task_definition.this.arn
  desired_count   = var.desired_count
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = var.private_subnet_ids
    security_groups  = [aws_security_group.service.id]
    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.this.arn
    container_name   = var.service_name
    container_port   = var.container_port
  }

  deployment_circuit_breaker {
    enable   = true
    rollback = true
  }

  tags = merge(var.tags, {
    Service     = var.service_name
    Environment = var.environment
  })
}
```

### Module Composition

```hcl
# environments/production/main.tf

module "networking" {
  source      = "../../modules/networking"
  environment = "production"
  vpc_cidr    = "10.0.0.0/16"
  azs         = ["eu-west-2a", "eu-west-2b"]
}

module "database" {
  source            = "../../modules/rds"
  environment       = "production"
  instance_class    = "db.r6g.large"
  multi_az          = true
  subnet_ids        = module.networking.data_subnet_ids
  security_group_id = module.networking.db_security_group_id
}

module "document_service" {
  source             = "../../modules/ecs-service"
  environment        = "production"
  service_name       = "document-service"
  cluster_arn        = module.ecs_cluster.arn
  desired_count      = 3
  private_subnet_ids = module.networking.private_subnet_ids
  container_port     = 8080

  environment_variables = {
    DB_URL     = module.database.endpoint
    AWS_REGION = "eu-west-2"
    QUEUE_URL  = module.processing_queue.url
  }
}
```

---

## Variable Validation

```hcl
variable "environment" {
  type = string
  validation {
    condition     = contains(["dev", "staging", "production"], var.environment)
    error_message = "Must be dev, staging, or production."
  }
}

variable "instance_class" {
  type = string
  validation {
    condition     = can(regex("^db\\.", var.instance_class))
    error_message = "Instance class must start with 'db.'"
  }
}

variable "desired_count" {
  type = number
  validation {
    condition     = var.desired_count >= 1 && var.desired_count <= 20
    error_message = "Desired count must be between 1 and 20."
  }
}
```

---

## Lifecycle Rules

```hcl
# Prevent accidental deletion of stateful resources
resource "aws_rds_instance" "main" {
  # ...

  lifecycle {
    prevent_destroy = true
  }
}

# Ignore external changes to auto-scaling
resource "aws_ecs_service" "main" {
  # ...

  lifecycle {
    ignore_changes = [desired_count]
  }
}
```

---

## CI/CD Integration

### Pre-commit Hooks

```yaml
# .pre-commit-config.yaml
repos:
  - repo: https://github.com/antonbabenko/pre-commit-terraform
    hooks:
      - id: terraform_fmt
      - id: terraform_validate
      - id: terraform_tflint
      - id: terraform_docs
```

### GitHub Actions Workflow

```yaml
name: Terraform Plan
on:
  pull_request:
    paths: ['infrastructure/terraform/**']

jobs:
  plan:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: hashicorp/setup-terraform@v3
      - run: terraform fmt -check -recursive
      - run: terraform init
      - run: terraform validate
      - run: terraform plan -out=plan.out
      - uses: borchero/terraform-plan-comment@v2
        with:
          plan-file: plan.out
```

---

## Security Scanning

```yaml
# Run tfsec/checkov in CI
security_checks:
  - tool: tfsec
    severity: HIGH,CRITICAL
    fail_on: any
  - tool: checkov
    framework: terraform
    fail_on: HIGH
```

### Common tfsec Rules to Enforce

| Rule | Description |
|------|-------------|
| `aws-s3-enable-bucket-encryption` | S3 encryption at rest |
| `aws-rds-encrypt-instance-storage` | RDS encryption |
| `aws-vpc-no-public-ingress` | No 0.0.0.0/0 ingress |
| `aws-iam-no-policy-wildcards` | No wildcard IAM |
| `aws-ecs-enable-container-insight` | Container Insights |
