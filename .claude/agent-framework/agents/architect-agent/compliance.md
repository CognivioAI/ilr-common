# Compliance Module

> Architecture Agent — Regulatory and Legal Architecture Discipline

---

## Purpose

For an ILR platform handling immigration documents, compliance is a first-class
architectural concern. This module ensures every design addresses legal
obligations. It is not optional — compliance failures can shut down the platform.

---

## Applicable Frameworks

| Framework | Applies When |
|-----------|-------------|
| UK GDPR | Processing personal data of UK residents |
| EU GDPR | Processing personal data of EU residents |
| Data Protection Act 2018 | UK law — aligns with UK GDPR |
| FCA | If handling financial data |
| Equality Act 2010 | Accessibility for UK users |
| UK Immigration Law | Handling immigration documents |
| ISO 27001 | If seeking enterprise security certification |

---

## GDPR / UK GDPR Requirements

### Data Subject Rights (Architecture Impact)

| Right | What It Means | Architecture Requirement |
|-------|--------------|--------------------------|
| Right to Access (SAR) | User can request all their data | API to export all user data |
| Right to Erasure | User can request deletion | Soft-delete + purge pipeline |
| Right to Portability | User can receive data in machine-readable format | JSON/CSV export API |
| Right to Rectification | User can correct their data | Edit endpoints for all PII fields |
| Right to Restrict Processing | User can pause processing | Processing status flag per user |
| Right to Object | User can object to automated decisions | Manual review override endpoint |

### Lawful Basis for Processing

The platform must document the lawful basis for each type of data processing:

| Data | Lawful Basis |
|------|-------------|
| Identity documents | Contract (processing immigration application) |
| Financial data | Contract (required for ILR eligibility) |
| Biometric data | Explicit consent + legal obligation |
| AI decisions | Legitimate interest + transparency notice |

---

## PII Architecture Controls

### Data Classification (ILR Application)

| Field | Classification | Controls |
|-------|---------------|----------|
| Full name | Confidential | Encrypted at rest |
| Passport number | Restricted | Column-level encryption, audit log |
| Date of birth | Restricted | Column-level encryption |
| Address | Confidential | Encrypted at rest |
| Email | Confidential | Encrypted at rest |
| Bank statement content | Restricted | S3 SSE-KMS, access controlled |
| Immigration history | Restricted | Column-level encryption, audit log |
| AI classification results | Internal | Standard encryption |

### PII Handling Rules

- Never log PII (passport numbers, bank details, NHS numbers)
- Mask PII in error messages and stack traces
- Never send PII to external AI APIs (use Bedrock — data stays in account)
- Hash or tokenize PII for analytics
- Segregate PII into separate encrypted columns

---

## Consent Management

```
User Registration
    ↓
Consent collection (explicit checkboxes, not pre-ticked)
    ↓
Consent stored with: userId, timestamp, consent text version, IP
    ↓
Platform uses data only within consented scope
    ↓
Consent withdrawal
    ↓
Processing stops, data queued for deletion
```

### Consent Record Schema

```sql
CREATE TABLE consent_records (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id),
    consent_type VARCHAR(50) NOT NULL,
    consented   BOOLEAN NOT NULL,
    text_version VARCHAR(20) NOT NULL,  -- which version of privacy policy
    timestamp   TIMESTAMP NOT NULL DEFAULT NOW(),
    ip_address  VARCHAR(45) NOT NULL  -- audit only
);
```

---

## Data Retention Architecture

### Retention Rules

| Data | Retention Period | Deletion Method |
|------|-----------------|----------------|
| Active application | Until decision + 7 years | Manual trigger |
| Rejected application | 2 years | Automated lifecycle |
| Uploaded documents (S3) | Same as application | S3 lifecycle policy |
| Audit logs | 10 years (legal requirement) | Never deleted |
| User account | 30 days after deletion request | Soft delete → purge |
| AI processing outputs | Same as application | Cascade |
| Session data | 30 minutes | Redis TTL |

### Right to Erasure Flow

```
User requests deletion
    ↓
Account flagged: deletion_requested_at = NOW()
    ↓
30-day grace period (in case of mistake)
    ↓
Erasure job runs:
  1. Anonymise PII fields (name → "Deleted User", email → uuid@deleted.invalid)
  2. Delete S3 documents
  3. Revoke AI processing outputs
  4. Preserve audit log (stripped of PII, events only)
  5. Record deletion in compliance log
    ↓
User receives confirmation email
```

---

## Audit Logging (Legal Requirement)

Every significant action must be immutably logged:

| Action | Required |
|--------|----------|
| Login (success + failure) | ✅ |
| Document uploaded | ✅ |
| Document accessed/downloaded | ✅ |
| AI decision applied | ✅ |
| AI decision overridden | ✅ |
| Data exported | ✅ |
| User deleted | ✅ |
| Admin access | ✅ |

Audit logs are **append-only** — no update or delete operations.

---

## Data Residency

For ILR (UK immigration), all data must remain in the UK:

```
AWS Region: eu-west-2 (London)
  S3 bucket: eu-west-2 only
  RDS: eu-west-2, no cross-region replication to non-UK
  Bedrock: eu-west-2 endpoint (data stays in account)
  CloudWatch: eu-west-2
  Backups: eu-west-2 only
```

### Verification

- S3 bucket region lock enabled
- RDS no cross-region replication to non-UK
- Network policies block data egress outside eu-west-2

---

## Privacy by Design Checklist

- [ ] Privacy impact assessment completed
- [ ] Minimum necessary data collected (data minimisation)
- [ ] PII fields identified and encrypted
- [ ] Consent mechanism implemented
- [ ] Right to access API implemented
- [ ] Right to erasure flow designed
- [ ] Data portability export implemented
- [ ] Data retention and deletion automated
- [ ] Audit logging on all sensitive actions
- [ ] Data residency enforced (eu-west-2)
- [ ] AI decisions explainable and reversible
- [ ] Privacy notice updated for each data use
- [ ] DPIA (Data Protection Impact Assessment) filed
