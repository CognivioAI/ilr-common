# GitHub Actions

## Purpose

Define GitHub Actions workflow templates, reusable actions, and CI/CD patterns for the ILR platform.

---

## Workflow Structure

```
.github/
├── workflows/
│   ├── ci.yml                    # Build + Test + Security (on PR)
│   ├── cd-dev.yml                # Deploy to dev (on push to main)
│   ├── cd-staging.yml            # Deploy to staging (on push to main, after dev)
│   ├── cd-production.yml         # Deploy to production (manual trigger / tag)
│   ├── terraform-plan.yml        # IaC preview (on PR to infrastructure/)
│   ├── terraform-apply.yml       # IaC apply (on merge to main)
│   └── security-scan.yml         # Scheduled security scanning
│
└── actions/
    ├── setup-java/               # Reusable: Java build environment
    ├── docker-build-push/        # Reusable: Build + push to ECR
    ├── helm-deploy/              # Reusable: Deploy via Helm
    └── notify/                   # Reusable: Slack notification
```

---

## CI Workflow (Pull Requests)

```yaml
name: CI Pipeline
on:
  pull_request:
    branches: [main]
    paths-ignore:
      - '*.md'
      - 'docs/**'

concurrency:
  group: ci-${{ github.ref }}
  cancel-in-progress: true

permissions:
  contents: read
  security-events: write
  pull-requests: write

jobs:
  build-and-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Setup Java
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: 'maven'

      - name: Build and Test
        run: ./mvnw verify -B

      - name: Upload Coverage
        uses: codecov/codecov-action@v4
        with:
          file: target/site/jacoco/jacoco.xml

  security-scan:
    runs-on: ubuntu-latest
    needs: build-and-test
    steps:
      - uses: actions/checkout@v4

      - name: SAST - SonarQube
        uses: sonarqube/sonarqube-scan-action@v2
        env:
          SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}

      - name: Dependency Check
        uses: dependency-check/Dependency-Check_Action@main
        with:
          project: 'ilr-platform'
          path: '.'
          format: 'SARIF'

      - name: Secret Detection
        uses: trufflesecurity/trufflehog@main
        with:
          extra_args: --only-verified

  docker-build:
    runs-on: ubuntu-latest
    needs: build-and-test
    steps:
      - uses: actions/checkout@v4

      - name: Build Docker Image
        run: |
          docker build -t ${{ env.IMAGE_NAME }}:test .

      - name: Scan Image
        uses: aquasecurity/trivy-action@master
        with:
          image-ref: ${{ env.IMAGE_NAME }}:test
          format: 'sarif'
          severity: 'CRITICAL,HIGH'
          exit-code: '1'
```

---

## CD Workflow (Deploy to Production)

```yaml
name: Deploy Production
on:
  workflow_dispatch:
    inputs:
      version:
        description: 'Version to deploy'
        required: true
      strategy:
        description: 'Deployment strategy'
        required: true
        default: 'canary'
        type: choice
        options:
          - rolling
          - canary
          - blue-green

permissions:
  id-token: write
  contents: read

jobs:
  deploy:
    runs-on: ubuntu-latest
    environment: production
    steps:
      - uses: actions/checkout@v4

      - name: Configure AWS Credentials
        uses: aws-actions/configure-aws-credentials@v4
        with:
          role-to-assume: ${{ secrets.AWS_DEPLOY_ROLE_ARN }}
          aws-region: eu-west-2

      - name: Setup Helm
        uses: azure/setup-helm@v3

      - name: Configure kubectl
        run: |
          aws eks update-kubeconfig --name ilr-production --region eu-west-2

      - name: Deploy via Helm
        run: |
          helm upgrade --install ${{ env.RELEASE_NAME }} ./charts/ilr-service \
            --namespace ilr-production \
            --values charts/ilr-service/values-production.yaml \
            --set image.tag=${{ inputs.version }} \
            --atomic \
            --timeout 10m \
            --wait

      - name: Smoke Tests
        run: |
          ./scripts/smoke-test.sh production

      - name: Notify
        if: always()
        uses: ./.github/actions/notify
        with:
          status: ${{ job.status }}
          environment: production
          version: ${{ inputs.version }}
```

---

## AWS OIDC Authentication

```yaml
# No long-lived credentials — use OIDC federation
- name: Configure AWS Credentials
  uses: aws-actions/configure-aws-credentials@v4
  with:
    role-to-assume: arn:aws:iam::123456789:role/github-actions-deploy
    aws-region: eu-west-2
    # No access keys needed — uses OIDC token
```

---

## Reusable Action: Docker Build + Push

```yaml
# .github/actions/docker-build-push/action.yml
name: Docker Build and Push
description: Build Docker image and push to ECR

inputs:
  service-name:
    required: true
  version:
    required: true
  aws-region:
    default: 'eu-west-2'

runs:
  using: composite
  steps:
    - name: Login to ECR
      uses: aws-actions/amazon-ecr-login@v2

    - name: Build and Push
      uses: docker/build-push-action@v5
      with:
        context: .
        push: true
        tags: |
          ${{ env.ECR_REGISTRY }}/${{ inputs.service-name }}:${{ inputs.version }}
          ${{ env.ECR_REGISTRY }}/${{ inputs.service-name }}:latest
        cache-from: type=gha
        cache-to: type=gha,mode=max
```

---

## Scheduled Security Scan

```yaml
name: Security Scan (Scheduled)
on:
  schedule:
    - cron: '0 6 * * 1'  # Every Monday at 6am

jobs:
  scan-images:
    runs-on: ubuntu-latest
    strategy:
      matrix:
        service: [document-service, api-gateway, ai-worker]
    steps:
      - name: Scan ECR Image
        uses: aquasecurity/trivy-action@master
        with:
          image-ref: ${{ env.ECR_REGISTRY }}/${{ matrix.service }}:latest
          format: 'sarif'
          severity: 'CRITICAL,HIGH'

      - name: Alert on Findings
        if: failure()
        uses: ./.github/actions/notify
        with:
          status: 'vulnerability-found'
          service: ${{ matrix.service }}
```

---

## Branch Protection Rules

```yaml
branch_protection:
  main:
    required_reviews: 1
    required_status_checks:
      - build-and-test
      - security-scan
      - docker-build
    enforce_admins: true
    restrict_pushes: true
    require_linear_history: true
```
