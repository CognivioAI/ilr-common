# Security Module

> Architecture Agent — Security Architecture Discipline

---

## Purpose

Define how the Architecture Agent performs a security review for every feature.
For an ILR application handling immigration documents, security is a first-class requirement.

---

## Security Review Questions

For every feature, answer:

1. **Authentication**: How is the caller identified?
2. **Authorization**: Who is allowed to do this?
3. **Encryption at rest**: Is sensitive data encrypted?
4. **Encryption in transit**: Is the connection secure?
5. **PII handling**: What personal data is processed?
6. **Data classification**: What sensitivity level?
7. **Audit logging**: Are actions recorded immutably?
8. **Input validation**: Can the input be weaponized?
9. **Secrets management**: Where do credentials come from?
10. **Document retention**: How long is data kept? When is it deleted?

---

## Authentication Decisions

| Option | When |
|--------|------|
| Amazon Cognito | SaaS, managed auth, social login |
| Custom JWT + Spring Security | Full control, existing identity system |
| API Key + IAM | Service-to-service internal calls |
| OAuth 2.0 external IdP | Enterprise SSO integration |

---

## Authorization Model

| Level | Implementation |
|-------|---------------|
| Endpoint access | Role-based (`USER`, `ADMIN`, `CASEWORKER`) |
| Resource ownership | User can only access their own applications |
| Field-level access | Sensitive fields visible only to specific roles |
| Feature flags | Gradual rollout gated by permissions |

```
User (APPLICANT role):
  ✅ View own applications
  ✅ Upload documents
  ❌ View other users' applications
  ❌ Access admin endpoints

Caseworker (CASEWORKER role):
  ✅ View assigned applications
  ✅ Override AI decisions
  ❌ Delete applications
  ❌ Modify audit logs

Admin (ADMIN role):
  ✅ All operations
  ✅ System configuration
  ❌ Delete audit logs (immutable)
```

---

## Data Classification

| Level | Examples | Controls |
|-------|----------|----------|
| Public | Marketing content | None |
| Internal | System metrics, logs | Network isolation |
| Confidential | Email, names, addresses | Encrypted, access-controlled |
| Restricted | Passport numbers, financial data | Encrypted, audited, limited access, PII redaction in logs |

### ILR Application: PII Inventory

| Data | Classification | Storage | Encrypted |
|------|---------------|---------|-----------|
| Full name | Confidential | PostgreSQL | Column-level |
| Passport number | Restricted | PostgreSQL | Column-level |
| Bank statements | Restricted | S3 | SSE-KMS |
| Email address | Confidential | PostgreSQL | At rest |
| Address history | Confidential | PostgreSQL | At rest |
| AI classification output | Internal | S3 | At rest |

---

## Document Retention and Deletion

| Data | Retention | Deletion Method |
|------|-----------|----------------|
| Active applications | Indefinite | — |
| Completed applications | 7 years | Automated lifecycle |
| Rejected applications | 2 years | Automated lifecycle |
| Original documents (S3) | Same as application | S3 lifecycle |
| AI outputs | Same as application | Cascade delete |
| Audit logs | 10 years | Never deleted |
| User account (deleted user) | 30 days grace | GDPR right to erasure |

---

## PII in AI Prompts

When sending data to AI models:
- Never send raw passport numbers
- Mask PII before sending to external models
- Use private model endpoints (AWS Bedrock — data stays in your account)
- Log prompt hash, not prompt content
- Never log AI responses containing PII

---

## Audit Logging Requirements

Every sensitive action must be audited:

| Action | Required Fields |
|--------|----------------|
| Document upload | userId, documentId, timestamp, IP |
| Document access | userId, documentId, timestamp, accessType |
| AI decision applied | agentName, documentId, confidence, decision |
| Role change | adminId, targetUserId, oldRole, newRole |
| Data export | userId, exportType, timestamp |
| Failed login | email (masked), IP, timestamp, reason |

---

## Threat Model (ILR Application)

| Threat | Mitigation |
|--------|-----------|
| Unauthorized document access | Resource ownership + role check |
| Forged documents | AI validation + human review |
| Credential theft | MFA, short-lived tokens, anomaly detection |
| Injection attacks | Input validation, parameterized queries |
| IDOR (insecure direct object reference) | Authorization check on every resource |
| Data exfiltration | DLP policies, VPC isolation, audit logging |
| AI manipulation (prompt injection) | Input sanitization, prompt boundaries |

---

## Checklist

- [ ] Authentication method decided (ADR)
- [ ] Authorization model designed (RBAC)
- [ ] PII inventory documented
- [ ] Data classification applied
- [ ] Encryption at rest (KMS)
- [ ] Encryption in transit (TLS 1.3)
- [ ] Audit logging for all sensitive actions
- [ ] Document retention policy defined
- [ ] Secrets in AWS Secrets Manager (not code)
- [ ] AI prompt PII handling documented
- [ ] Threat model reviewed
