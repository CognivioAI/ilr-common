# Docker Standards

## Purpose

Define Dockerfile best practices, multi-stage builds, image security, and container standards for all services.

---

## Multi-Stage Build Template (Java Spring Boot)

```dockerfile
# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .

# Download dependencies (cached layer)
RUN ./mvnw dependency:go-offline -B

COPY src ./src
RUN ./mvnw package -DskipTests -B

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine AS runtime

# Security: Non-root user
RUN addgroup -g 1000 appgroup && \
    adduser -u 1000 -G appgroup -D appuser

WORKDIR /app

# Copy only the built artifact
COPY --from=builder /app/target/*.jar app.jar

# Security: Own the app files
RUN chown -R appuser:appgroup /app

USER appuser

EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

## Multi-Stage Build Template (React)

```dockerfile
# Stage 1: Build
FROM node:20-alpine AS builder

WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --production=false

COPY . .
RUN npm run build

# Stage 2: Runtime (Nginx)
FROM nginx:1.25-alpine AS runtime

# Security: Non-root
RUN addgroup -g 1000 appgroup && \
    adduser -u 1000 -G appgroup -D appuser

# Copy built assets
COPY --from=builder /app/build /usr/share/nginx/html
COPY nginx.conf /etc/nginx/nginx.conf

# Security: Restrict permissions
RUN chown -R appuser:appgroup /usr/share/nginx/html && \
    chown -R appuser:appgroup /var/cache/nginx && \
    chown -R appuser:appgroup /var/log/nginx && \
    touch /var/run/nginx.pid && \
    chown appuser:appgroup /var/run/nginx.pid

USER appuser

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
```

---

## Image Standards

### Base Image Selection

| Language | Base Image | Reasoning |
|----------|-----------|-----------|
| Java 21 | `eclipse-temurin:21-jre-alpine` | Minimal, trusted, Alpine-based |
| Node.js | `node:20-alpine` | LTS, minimal Alpine |
| Python | `python:3.12-slim` | Slim variant, no extras |
| Nginx | `nginx:1.25-alpine` | Minimal web server |

### Rules

1. **Always use specific version tags** — Never use `latest`
2. **Always use Alpine or slim variants** — Minimal attack surface
3. **Always run as non-root** — `USER 1000`
4. **Always use multi-stage builds** — Separate build from runtime
5. **Always include HEALTHCHECK** — Container self-reporting
6. **Never include secrets** — Use env vars or mounted secrets
7. **Never include dev dependencies** — Production only

---

## .dockerignore

```
# .dockerignore
.git
.gitignore
.env
.env.*
node_modules
target
*.md
!README.md
docker-compose*.yml
.idea
.vscode
*.log
coverage
.nyc_output
```

---

## Image Security Scanning

### In CI Pipeline

```yaml
# Scan with Trivy
- name: Scan Docker Image
  uses: aquasecurity/trivy-action@master
  with:
    image-ref: ${{ env.IMAGE_NAME }}:${{ env.IMAGE_TAG }}
    format: 'sarif'
    output: 'trivy-results.sarif'
    severity: 'CRITICAL,HIGH'
    exit-code: '1'  # Fail build on HIGH/CRITICAL
```

### Vulnerability Thresholds

| Severity | Action |
|----------|--------|
| CRITICAL | Block deployment, immediate fix required |
| HIGH | Block deployment, fix within 48 hours |
| MEDIUM | Warning, fix within sprint |
| LOW | Informational, fix when convenient |

---

## Container Runtime Standards

```yaml
container_runtime:
  read_only_root: true
  drop_all_capabilities: true
  no_privilege_escalation: true
  resource_limits_required: true
  tmp_volume: emptyDir  # For writable /tmp
```

---

## Image Tagging Strategy

```yaml
tagging:
  production:
    pattern: "{version}"
    example: "1.2.0"
  development:
    pattern: "{branch}-{short-sha}"
    example: "feature-auth-abc1234"
  ci:
    pattern: "{version}-{build-number}"
    example: "1.2.0-42"

rules:
  - Never push `latest` to production ECR
  - Immutable tags in production (no overwriting)
  - Clean up untagged images after 14 days
```

---

## ECR Configuration

```hcl
# Terraform: ECR repository
resource "aws_ecr_repository" "service" {
  name                 = "ilr/${var.service_name}"
  image_tag_mutability = "IMMUTABLE"  # Production safety

  image_scanning_configuration {
    scan_on_push = true
  }

  encryption_configuration {
    encryption_type = "AES256"
  }
}

# Lifecycle policy: keep last 10 tagged images
resource "aws_ecr_lifecycle_policy" "cleanup" {
  repository = aws_ecr_repository.service.name

  policy = jsonencode({
    rules = [
      {
        rulePriority = 1
        description  = "Remove untagged images after 14 days"
        selection = {
          tagStatus   = "untagged"
          countType   = "sinceImagePushed"
          countUnit   = "days"
          countNumber = 14
        }
        action = { type = "expire" }
      }
    ]
  })
}
```

---

## Docker Compose (Local Development Only)

```yaml
# docker-compose.yml - local development
version: '3.8'

services:
  document-service:
    build:
      context: ./services/document-service
      target: builder  # Use build stage for hot-reload
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=local
      - DB_URL=jdbc:postgresql://db:5432/ilr
    depends_on:
      db:
        condition: service_healthy

  db:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: ilr
      POSTGRES_USER: dev
      POSTGRES_PASSWORD: devpass
    ports:
      - "5432:5432"
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U dev"]
      interval: 5s
      timeout: 3s
      retries: 5
```
