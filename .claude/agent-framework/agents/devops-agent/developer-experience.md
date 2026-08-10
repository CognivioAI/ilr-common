# Developer Experience

## Purpose

Define the golden path for developers — onboarding, local development, inner/outer loop, and self-service capabilities that make the platform easy to use.

---

## Developer Onboarding (Day 1 → Productive)

### Target: First Commit in < 4 Hours

```yaml
onboarding_path:
  hour_1_setup:
    - Clone repository
    - Install prerequisites (check `make doctor`)
    - Run `make setup` (installs tools, configures local env)
    - Verify: all tools installed, versions correct
  
  hour_2_local_run:
    - Run `docker compose up` (all dependencies start)
    - Run application locally (`./mvnw spring-boot:run -Plocal`)
    - Access: http://localhost:8080/actuator/health → 200 OK
    - Verify: can hit API endpoints
  
  hour_3_understand:
    - Read service README
    - Review architecture diagram
    - Explore API documentation (Swagger UI)
    - Understand deployment pipeline
  
  hour_4_contribute:
    - Create feature branch
    - Make a small change
    - Run tests locally
    - Push → see CI pass
    - Open PR → see preview environment
```

### Prerequisites Check

```makefile
# Makefile
.PHONY: doctor
doctor:
	@echo "Checking prerequisites..."
	@command -v docker >/dev/null 2>&1 || echo "❌ Docker not installed"
	@command -v java >/dev/null 2>&1 || echo "❌ Java not installed"
	@java -version 2>&1 | grep "21" || echo "❌ Java 21 required"
	@command -v node >/dev/null 2>&1 || echo "❌ Node.js not installed"
	@command -v helm >/dev/null 2>&1 || echo "❌ Helm not installed"
	@command -v aws >/dev/null 2>&1 || echo "❌ AWS CLI not installed"
	@echo "✅ All prerequisites met"
```

---

## Local Development Environment

### Docker Compose (One Command Start)

```yaml
# docker-compose.yml
version: '3.8'

services:
  # Application service (hot-reload)
  document-service:
    build:
      context: ./services/document-service
      target: builder
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=local
      - DB_URL=jdbc:postgresql://db:5432/ilr_dev
      - AWS_REGION=eu-west-2
      - SQS_ENDPOINT=http://localstack:4566
      - S3_ENDPOINT=http://localstack:4566
    depends_on:
      db:
        condition: service_healthy
      localstack:
        condition: service_healthy
    volumes:
      - ./services/document-service/src:/app/src  # Hot reload

  # PostgreSQL
  db:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: ilr_dev
      POSTGRES_USER: dev
      POSTGRES_PASSWORD: devpass
    ports:
      - "5432:5432"
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U dev"]
      interval: 5s
    volumes:
      - ./scripts/init-db.sql:/docker-entrypoint-initdb.d/init.sql

  # AWS services (LocalStack)
  localstack:
    image: localstack/localstack:latest
    ports:
      - "4566:4566"
    environment:
      - SERVICES=s3,sqs,secretsmanager
      - DEFAULT_REGION=eu-west-2
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:4566/_localstack/health"]
      interval: 5s
    volumes:
      - ./scripts/localstack-init.sh:/etc/localstack/init/ready.d/init.sh

  # React Frontend
  web-frontend:
    build:
      context: ./services/web-frontend
      target: development
    ports:
      - "3000:3000"
    environment:
      - REACT_APP_API_URL=http://localhost:8080
    volumes:
      - ./services/web-frontend/src:/app/src  # Hot reload
```

### Makefile (Developer Commands)

```makefile
# Common developer commands
.PHONY: setup dev test clean deploy-dev

# First-time setup
setup:
	@echo "Setting up development environment..."
	docker compose pull
	docker compose build
	@echo "✅ Setup complete. Run 'make dev' to start."

# Start everything
dev:
	docker compose up -d
	@echo "🚀 Services starting..."
	@echo "   API:      http://localhost:8080"
	@echo "   Frontend: http://localhost:3000"
	@echo "   Swagger:  http://localhost:8080/swagger-ui.html"

# Run tests
test:
	./mvnw verify -B

# Run specific service tests
test-service:
	./mvnw verify -pl services/$(SERVICE) -B

# Clean everything
clean:
	docker compose down -v
	./mvnw clean

# Build Docker image
build:
	docker build -t $(SERVICE):local ./services/$(SERVICE)

# Deploy to dev (via pipeline)
deploy-dev:
	git push origin HEAD  # CI/CD handles the rest

# View logs
logs:
	docker compose logs -f $(SERVICE)

# Database shell
db-shell:
	docker compose exec db psql -U dev ilr_dev

# Run linter
lint:
	./mvnw checkstyle:check spotbugs:check

# Security scan (local)
security:
	./mvnw dependency-check:check
	docker run --rm -v $(PWD):/src aquasec/trivy fs /src
```

---

## Inner Loop (Developer Workflow)

```
Code Change
    │
    ├── Save file
    │
    ▼
Hot Reload (< 3 seconds)
    │
    ├── Spring DevTools detects change
    ├── Application restarts (or hot-swaps)
    │
    ▼
Test Locally (< 30 seconds)
    │
    ├── Run unit tests for changed module
    │
    ▼
Commit & Push
    │
    ├── Pre-commit hooks run:
    │   ├── Formatting check
    │   ├── Linting
    │   └── Secret detection
    │
    ▼
CI Feedback (< 10 minutes)
    │
    ├── Full test suite
    ├── Security scan
    ├── Preview environment deployed
    └── PR status updated
```

### Developer Feedback Loop Targets

| Stage | Target Time | Current |
|-------|-------------|---------|
| Hot reload | < 3 seconds | Measure |
| Unit tests (changed) | < 30 seconds | Measure |
| Full test suite | < 5 minutes | Measure |
| CI pipeline | < 10 minutes | Measure |
| Preview deploy | < 3 minutes | Measure |
| Production deploy | < 15 minutes (from merge) | Measure |

---

## Self-Service Capabilities

### What Developers Can Do Without Waiting

```yaml
self_service:
  deployment:
    dev: "Push to main → auto-deploys"
    staging: "Automatic after dev passes"
    preview: "Open PR → preview environment created"
    production: "Approved PR merge → deployment"
  
  observability:
    logs: "Grafana / CloudWatch (scoped to own services)"
    metrics: "Pre-built dashboards in Grafana"
    traces: "X-Ray / Tempo (full request traces)"
  
  environment:
    create: "make env-create (ephemeral dev environment)"
    destroy: "make env-destroy"
    reset_db: "make db-reset (re-seed development database)"
  
  secrets:
    dev: "Self-service via CLI / parameter store"
    staging: "Self-service with audit trail"
    production: "Request via platform team (approved same day)"
  
  scaling:
    dev: "Modify values file, push"
    staging: "Modify values file, push"
    production: "Within pre-approved limits (HPA handles automatically)"
```

---

## Documentation Standards

### Service README Template

```markdown
# Service Name

## Overview
One paragraph: what this service does and why it exists.

## Quick Start
\`\`\`bash
make dev         # Start all dependencies
make test        # Run tests
make build       # Build Docker image
\`\`\`

## API
- Swagger UI: http://localhost:8080/swagger-ui.html
- Health: http://localhost:8080/actuator/health

## Architecture
Brief description + link to architecture doc.

## Configuration
| Variable | Description | Default |
|----------|-------------|---------|
| DB_URL | Database connection | localhost |

## Deployment
- Dev: auto-deploy on merge to main
- Production: via release pipeline

## Troubleshooting
Common issues and solutions.

## Team
Owner: @team-name
Slack: #team-channel
```

---

## Developer Tooling

### Recommended IDE Setup

```yaml
ide_setup:
  java:
    ide: IntelliJ IDEA (or VS Code with Java extensions)
    plugins:
      - Lombok
      - Spring Boot
      - Docker
      - Kubernetes
      - SonarLint
    run_config: Spring Boot with local profile
  
  frontend:
    ide: VS Code
    extensions:
      - ESLint
      - Prettier
      - React DevTools
      - Docker
    run_config: npm start with proxy to local API
```

### CLI Tools (Required)

```yaml
required_tools:
  - docker (container runtime)
  - java 21 (application runtime)
  - maven (build tool, via wrapper)
  - node 20 (frontend)
  - aws-cli (AWS access)
  - kubectl (Kubernetes access)
  - helm (chart management)
  - make (task runner)
  
optional_tools:
  - k9s (Kubernetes TUI)
  - stern (multi-pod log tailing)
  - kubectx (context switching)
  - terraform (if working on infra)
  - jq (JSON processing)
```

---

## Feedback Mechanisms

```yaml
developer_feedback:
  ci_feedback:
    - PR checks with clear pass/fail
    - Comment on PR with test results
    - Preview environment URL in PR
    - Security scan results as PR comment
    - Cost impact estimate on infra PRs
  
  runtime_feedback:
    - Structured error messages (not stack traces in API responses)
    - Clear health check status
    - Accessible logs (no SSH required)
    - Self-service metrics dashboards
  
  platform_feedback:
    - Monthly developer satisfaction survey
    - Slack channel for platform questions
    - Office hours (weekly)
    - Documented known issues and workarounds
```

---

## Anti-Patterns (Developer Experience Debt)

| Anti-Pattern | Impact | Fix |
|-------------|--------|-----|
| "Works on my machine" | Blocks others | Docker-based dev environment |
| Manual deployment steps | Slow, error-prone | Full CI/CD automation |
| No local testing possible | Slow feedback | LocalStack + Docker Compose |
| SSH to see logs | Friction, security risk | Centralized log access |
| Ticket to get environment | Multi-day delay | Self-service environments |
| Undocumented configuration | Onboarding pain | Service README template |
| Flaky tests | Erodes trust in CI | Fix or quarantine flaky tests |
