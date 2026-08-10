# Tool Integrations

## Purpose

Define the tools and MCP (Model Context Protocol) integrations the DevOps Agent uses to interact with infrastructure, cloud services, and deployment systems.

---

## Agent Tool Architecture

```
DevOps Agent (Reasoning Engine)
    │
    ├── Terraform MCP      → Provision infrastructure
    ├── AWS MCP            → Query/manage AWS services
    ├── Kubernetes MCP     → Deploy and manage workloads
    ├── GitHub MCP         → Manage repositories, PRs, Actions
    ├── Docker MCP         → Build and manage container images
    ├── Prometheus MCP     → Query metrics and alerts
    ├── Grafana MCP        → Manage dashboards
    └── Cost Explorer MCP  → Track and optimize costs
```

---

## Tool Categories

### Infrastructure Provisioning

```yaml
terraform_integration:
  capabilities:
    - terraform init
    - terraform plan
    - terraform apply
    - terraform destroy
    - terraform state (query, move, rm)
    - terraform output
  
  usage_patterns:
    provision_service:
      input: service definition (from architecture translator)
      action: generate and apply Terraform
      output: infrastructure endpoints, security groups, IAM roles
    
    destroy_environment:
      input: environment name
      action: terraform destroy (with safety checks)
      output: confirmation of resources removed
    
    query_state:
      input: resource type or name
      action: terraform state show
      output: current resource configuration
  
  safety:
    - Always plan before apply
    - Require approval for production
    - Never auto-apply destructive changes
    - State locking enforced
```

### Cloud Services (AWS)

```yaml
aws_integration:
  capabilities:
    query:
      - Describe EC2 instances, ECS services, Lambda functions
      - List S3 buckets, RDS instances
      - Get CloudWatch metrics
      - Read CloudTrail events
      - Get Secrets Manager values (non-sensitive metadata)
      - Cost Explorer queries
    
    manage:
      - Scale ECS services
      - Update Lambda configuration
      - Manage S3 lifecycle policies
      - Create/rotate secrets
      - Update security groups
    
    monitor:
      - CloudWatch alarms status
      - RDS performance insights
      - ECS service health
      - Cost and usage data
  
  usage_patterns:
    investigate_issue:
      input: alert or anomaly description
      actions:
        - Query CloudWatch metrics for affected service
        - Check ECS service events
        - Review CloudTrail for recent changes
        - Check RDS performance insights
      output: diagnostic summary with probable root cause
    
    cost_analysis:
      input: date range, service filter
      actions:
        - Query Cost Explorer
        - Break down by service, resource type
        - Compare to previous period
      output: cost report with recommendations
```

### Kubernetes

```yaml
kubernetes_integration:
  capabilities:
    query:
      - kubectl get (pods, deployments, services, events)
      - kubectl describe (detailed resource info)
      - kubectl logs (container logs)
      - kubectl top (resource usage)
      - helm list (deployed releases)
      - helm history (release revisions)
    
    manage:
      - kubectl scale (adjust replicas)
      - kubectl rollout (status, undo, restart)
      - helm upgrade (deploy new version)
      - helm rollback (revert release)
      - kubectl apply (apply manifests)
    
    diagnose:
      - kubectl get events --sort-by='.lastTimestamp'
      - kubectl logs --previous (crashed container)
      - kubectl describe pod (scheduling issues)
      - kubectl get endpoints (service discovery)
  
  usage_patterns:
    deploy_service:
      input: service name, version, environment
      actions:
        - helm upgrade --install with appropriate values
        - Wait for rollout completion
        - Verify health checks
        - Run smoke tests
      output: deployment status, service URL
    
    troubleshoot_pod:
      input: pod name or service with issue
      actions:
        - Get pod status and events
        - Check logs (current and previous)
        - Check resource usage
        - Check networking (endpoints, DNS)
      output: diagnosis with recommended action
```

### GitHub

```yaml
github_integration:
  capabilities:
    query:
      - List PRs, issues, workflows
      - Get workflow run status
      - Read file contents
      - Get commit history
    
    manage:
      - Create/update PRs
      - Trigger workflows
      - Comment on PRs (deployment status)
      - Create releases and tags
      - Manage branch protection
    
    automate:
      - Update GitOps config (image tags)
      - Create environment promotion PRs
      - Post deployment notifications
  
  usage_patterns:
    promote_to_staging:
      input: service name, version
      actions:
        - Create branch in GitOps repo
        - Update staging values (image tag)
        - Create PR with description
        - Auto-merge if all checks pass
      output: PR URL, deployment status
```

### Docker / Container Registry

```yaml
docker_integration:
  capabilities:
    build:
      - docker build (with build args)
      - docker tag
      - docker push (to ECR)
    
    query:
      - List images in ECR
      - Get image vulnerability scan results
      - Check image size and layers
      - Get image manifest/digest
    
    manage:
      - ECR lifecycle policies
      - Image retagging
      - Clean up untagged images
  
  usage_patterns:
    build_and_push:
      input: service directory, version tag
      actions:
        - Build multi-stage Docker image
        - Tag with version and commit SHA
        - Push to ECR
        - Verify image scan results
      output: image URI, scan status
```

### Monitoring and Observability

```yaml
prometheus_integration:
  capabilities:
    - Execute PromQL queries
    - Get alert status (firing, pending, inactive)
    - Query label values
    - Get metric metadata
  
  usage_patterns:
    check_service_health:
      queries:
        - rate(http_requests_total{service="X"}[5m])
        - histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m]))
        - sum(rate(http_requests_total{status=~"5.."}[5m])) / sum(rate(http_requests_total[5m]))

grafana_integration:
  capabilities:
    - Create/update dashboards
    - Manage alert rules
    - Query data sources
    - Manage notification channels
  
  usage_patterns:
    create_service_dashboard:
      input: service name, metrics list
      action: generate Grafana dashboard JSON
      output: dashboard URL
```

---

## Tool Selection Strategy

```yaml
tool_selection:
  principle: "Use managed tools that reduce toil"
  
  decisions:
    iac: Terraform (multi-cloud, mature ecosystem)
    ci_cd: GitHub Actions (integrated with repo)
    container_registry: ECR (AWS native, scanning built-in)
    kubernetes: EKS (managed control plane)
    gitops: ArgoCD (declarative, drift detection)
    monitoring: Prometheus + Grafana (open source, powerful)
    logging: CloudWatch Logs (or ELK for scale)
    tracing: OpenTelemetry + X-Ray (AWS integrated)
    secrets: AWS Secrets Manager (rotation, auditing)
    cost: AWS Cost Explorer + Infracost (PR feedback)
```

---

## MCP Server Configuration

```json
{
  "mcpServers": {
    "terraform": {
      "command": "terraform-mcp-server",
      "args": ["--workspace", "/infrastructure/terraform"],
      "env": {
        "AWS_PROFILE": "ilr-platform"
      }
    },
    "aws": {
      "command": "aws-mcp-server",
      "args": ["--region", "eu-west-2"],
      "env": {
        "AWS_PROFILE": "ilr-platform"
      }
    },
    "kubernetes": {
      "command": "kubectl-mcp-server",
      "args": ["--context", "ilr-production"],
      "env": {
        "KUBECONFIG": "~/.kube/config"
      }
    },
    "github": {
      "command": "github-mcp-server",
      "args": ["--repo", "ilr/ilr-platform"],
      "env": {
        "GITHUB_TOKEN": "${GITHUB_TOKEN}"
      }
    }
  }
}
```

---

## Tool Access Control

```yaml
access_control:
  by_environment:
    dev:
      terraform: plan + apply
      kubernetes: full access
      aws: full access (scoped to dev account)
    
    staging:
      terraform: plan + apply (with approval)
      kubernetes: full access
      aws: read + deploy
    
    production:
      terraform: plan only (apply requires approval)
      kubernetes: read + rollback only
      aws: read only (deploy via CI/CD)
  
  by_action_type:
    read: always allowed
    create: allowed (within budget/policy)
    update: allowed (non-destructive)
    delete: requires confirmation
    destroy: requires explicit approval
```

---

## Integration Testing (Verify Tools Work)

```yaml
tool_health_check:
  frequency: daily
  checks:
    - terraform version (correct version installed)
    - aws sts get-caller-identity (credentials valid)
    - kubectl cluster-info (cluster accessible)
    - helm list (Helm functioning)
    - docker info (Docker daemon running)
  
  on_failure:
    - Alert platform team
    - Document which tool is unavailable
    - Identify workaround or fallback
```
