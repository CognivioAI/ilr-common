# Secrets Management

## Purpose

Define how secrets are stored, accessed, rotated, and audited. Zero-plaintext policy — no secrets in code, config files, or environment variable definitions.

---

## Golden Rules

1. ❌ Never: `application.properties` → `password=myPassword123`
2. ❌ Never: Hardcoded in Dockerfile or docker-compose
3. ❌ Never: Committed to Git (even encrypted)
4. ❌ Never: Passed as plain-text CLI arguments
5. ✅ Always: AWS Secrets Manager or Parameter Store
6. ✅ Always: Kubernetes Secrets (from external source)
7. ✅ Always: Short-lived credentials (STS, OIDC)
8. ✅ Always: Automated rotation

---

## AWS Secrets Architecture

```
┌──────────────────────────────────────────────┐
│  AWS Secrets Manager                          │
│  ├── /ilr/production/database-credentials    │
│  ├── /ilr/production/api-keys               │
│  ├── /ilr/production/jwt-signing-key        │
│  └── /ilr/production/external-service-keys  │
└──────────────────────────────────────────────┘
         │
         │  IAM Role (per service)
         ▼
┌──────────────────────────────────────────────┐
│  Application (Spring Boot)                    │
│  └── Reads at startup via AWS SDK            │
└──────────────────────────────────────────────┘
```

---

## Secret Types and Storage

| Secret Type | Storage | Rotation | Access Pattern |
|-------------|---------|----------|----------------|
| Database credentials | Secrets Manager | Automatic (30 days) | IAM role → SDK |
| API keys (external) | Secrets Manager | Manual (90 days) | IAM role → SDK |
| JWT signing keys | Secrets Manager | Automatic (90 days) | IAM role → SDK |
| TLS certificates | ACM | Automatic | AWS-managed |
| SSH keys | Parameter Store | Manual (quarterly) | IAM role → SDK |
| CI/CD tokens | GitHub Secrets | Manual (90 days) | OIDC federation |

---

## Kubernetes Integration

### External Secrets Operator

```yaml
# ExternalSecret CRD - syncs AWS Secrets Manager → K8s Secret
apiVersion: external-secrets.io/v1beta1
kind: ExternalSecret
metadata:
  name: database-credentials
  namespace: ilr-production
spec:
  refreshInterval: 1h
  secretStoreRef:
    name: aws-secrets-manager
    kind: ClusterSecretStore
  target:
    name: database-credentials
    creationPolicy: Owner
  data:
    - secretKey: url
      remoteRef:
        key: /ilr/production/database-credentials
        property: url
    - secretKey: username
      remoteRef:
        key: /ilr/production/database-credentials
        property: username
    - secretKey: password
      remoteRef:
        key: /ilr/production/database-credentials
        property: password
```

### Pod Secret Consumption

```yaml
# Deployment - reference Kubernetes Secret (populated by External Secrets)
env:
  - name: DB_URL
    valueFrom:
      secretKeyRef:
        name: database-credentials
        key: url
  - name: DB_USERNAME
    valueFrom:
      secretKeyRef:
        name: database-credentials
        key: username
  - name: DB_PASSWORD
    valueFrom:
      secretKeyRef:
        name: database-credentials
        key: password
```

---

## Spring Boot Integration

### application.yml (No secrets here)

```yaml
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}

aws:
  region: ${AWS_REGION:eu-west-2}
```

### AWS SDK Secret Retrieval (Alternative)

```java
@Configuration
public class SecretsConfig {

    @Bean
    public DataSource dataSource(SecretsManagerClient secretsClient) {
        String secretJson = secretsClient.getSecretValue(
            GetSecretValueRequest.builder()
                .secretId("/ilr/production/database-credentials")
                .build()
        ).secretString();

        var creds = objectMapper.readValue(secretJson, DbCredentials.class);
        return DataSourceBuilder.create()
            .url(creds.url())
            .username(creds.username())
            .password(creds.password())
            .build();
    }
}
```

---

## Secret Rotation

### Automatic Rotation (AWS Secrets Manager)

```hcl
# Terraform: Enable rotation
resource "aws_secretsmanager_secret_rotation" "db_creds" {
  secret_id           = aws_secretsmanager_secret.db_creds.id
  rotation_lambda_arn = aws_lambda_function.secret_rotator.arn

  rotation_rules {
    automatically_after_days = 30
  }
}
```

### Rotation Schedule

| Secret | Rotation Period | Method |
|--------|----------------|--------|
| Database passwords | 30 days | Automatic (Lambda) |
| API keys | 90 days | Manual + notification |
| JWT signing keys | 90 days | Automatic (dual-key) |
| Service account tokens | 24 hours | OIDC (automatic) |
| TLS certificates | Auto-renew (ACM) | AWS-managed |

---

## CI/CD Secrets

### GitHub Actions (OIDC — No Long-Lived Keys)

```yaml
# Authenticate to AWS without access keys
- name: Configure AWS
  uses: aws-actions/configure-aws-credentials@v4
  with:
    role-to-assume: ${{ secrets.AWS_DEPLOY_ROLE_ARN }}
    aws-region: eu-west-2
    # Uses GitHub OIDC token — no stored credentials
```

### Secret Hierarchy

```yaml
github_secrets:
  organization_level:
    - AWS_ACCOUNT_ID
    - SONAR_TOKEN
  repository_level:
    - AWS_DEPLOY_ROLE_ARN (per environment)
  environment_level:
    - Scoped to specific environments (production, staging)
    - Requires approval for access
```

---

## Audit and Detection

```yaml
audit:
  access_logging:
    - CloudTrail: All Secrets Manager API calls
    - CloudWatch: Secret access patterns
    - Alerts: Unusual access patterns
  
  detection:
    - Pre-commit: git-secrets hook
    - CI: TruffleHog verified scan
    - Runtime: GuardDuty credential exfiltration alerts
  
  alerts:
    - Secret accessed from unknown IP
    - Secret accessed by unexpected role
    - Failed secret access attempts > 5/minute
    - Secret rotation failure
```

---

## Emergency Procedures

```yaml
compromised_secret:
  immediate:
    1. Rotate the secret immediately
    2. Identify all services using the secret
    3. Restart affected services (pick up new secret)
    4. Review audit logs for unauthorized access
  
  follow_up:
    5. Determine how compromise occurred
    6. Strengthen controls
    7. Document in incident report
```
