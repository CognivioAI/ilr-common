# Security Review

## Purpose

Security coding review — every implementation must pass security checks before delivery. Critical for the ILR application which handles personal documents and sensitive data.

---

## Security Review Checklist

### Java / Spring Boot

```yaml
java_security:
  injection:
    sql_injection:
      check: "All database queries use parameterized statements"
      bad: 'query = "SELECT * FROM docs WHERE id = " + userInput'
      good: "@Query('SELECT d FROM Document d WHERE d.id = :id')"
      severity: CRITICAL
    
    command_injection:
      check: "No user input in system commands"
      bad: "Runtime.exec('convert ' + filename)"
      good: "Use ProcessBuilder with explicit arguments array"
      severity: CRITICAL
    
    log_injection:
      check: "User input sanitized in log messages"
      bad: 'log.info("User searched: " + searchTerm)'
      good: 'log.info("User searched: {}", sanitize(searchTerm))'
      severity: MEDIUM

  authentication:
    jwt_validation:
      check: "JWT signature verified, expiry checked, issuer validated"
      verify: "Spring Security filter chain configured correctly"
    
    session_management:
      check: "Stateless (JWT) — no server-side session"
      verify: ".sessionManagement(s -> s.sessionCreationPolicy(STATELESS))"
    
    password_handling:
      check: "Never stored plaintext, bcrypt for hashing"
      verify: "PasswordEncoder bean = BCryptPasswordEncoder"

  authorization:
    endpoint_protection:
      check: "All /api/** endpoints require authentication"
      verify: "SecurityConfig denies anonymous access to API paths"
    
    resource_ownership:
      check: "Users cannot access other users' documents"
      verify: "Service layer checks ownership before returning data"
      bad: "findById(id) — returns regardless of owner"
      good: "findByIdAndUploadedBy(id, currentUser)"
    
    privilege_escalation:
      check: "Users cannot perform admin actions"
      verify: "@PreAuthorize or role checks in service layer"

  data_exposure:
    pii_in_logs:
      check: "No personally identifiable information in logs"
      bad: 'log.info("Processing passport: {}", passportNumber)'
      good: 'log.info("Processing document: id={}", document.getId())'
      severity: HIGH
    
    sensitive_in_response:
      check: "Passwords, tokens, internal IDs not in API responses"
      verify: "DTO excludes sensitive fields, @JsonIgnore on entity"
    
    error_detail_leaking:
      check: "Stack traces not returned to client"
      verify: "GlobalExceptionHandler returns code+message only"
      bad: "return ResponseEntity.body(exception.getStackTrace())"
      good: "return ResponseEntity.body(new ErrorResponse(code, message))"

  deserialization:
    unsafe_deserialization:
      check: "No ObjectInputStream on untrusted data"
      verify: "Use JSON (Jackson) with type validation"
    
    jackson_config:
      check: "Default typing disabled, polymorphic types restricted"
      verify: "ObjectMapper does not enable DEFAULT_TYPING"

  file_handling:
    path_traversal:
      check: "File names sanitized, no ../ in paths"
      verify: "Validate and sanitize filename before storage"
      bad: "new File(uploadDir + request.getFilename())"
      good: "Paths.get(uploadDir).resolve(sanitize(filename)).normalize()"
    
    file_type_validation:
      check: "Validate content type, not just extension"
      verify: "Check magic bytes / MIME type detection"
    
    file_size:
      check: "Maximum file size enforced"
      verify: "spring.servlet.multipart.max-file-size configured"
```

### React / Frontend

```yaml
frontend_security:
  xss:
    check: "No dangerouslySetInnerHTML with user content"
    bad: '<div dangerouslySetInnerHTML={{__html: userComment}} />'
    good: '<div>{userComment}</div> (React auto-escapes)'
    exception: "Only for sanitized HTML from trusted source (DOMPurify)"
    severity: CRITICAL
  
  token_storage:
    check: "Auth tokens stored securely"
    bad: "localStorage.setItem('token', jwt) — accessible to XSS"
    good: "httpOnly cookie (set by server) — inaccessible to JS"
    alternative: "Memory-only (lost on refresh but safe)"
  
  api_calls:
    check: "CSRF protection for cookie-based auth"
    verify: "CSRF token included in state-changing requests"
    check: "No sensitive data in URL params"
    bad: "/api/users?password=secret"
    good: "Sensitive data in request body (POST)"
  
  dependencies:
    check: "No dependencies with known XSS vulnerabilities"
    tool: "npm audit / Snyk"
    severity: HIGH
  
  content_security_policy:
    check: "CSP headers configured"
    verify: "Restrict script sources, disable inline scripts"
```

### API Security

```yaml
api_security:
  rate_limiting:
    check: "Rate limiting configured for all endpoints"
    verify: "API Gateway or Spring rate limiter"
    limits:
      login: "5 attempts per minute per IP"
      api: "100 requests per minute per user"
      upload: "10 uploads per minute per user"
  
  input_validation:
    check: "All input validated server-side (never trust client)"
    verify: "@Valid on all request bodies, @Size/@Pattern on strings"
    rules:
      - Max length on all string fields
      - Pattern validation on structured fields (email, UUID)
      - Positive/range on numeric fields
      - Enum validation on status/type fields
  
  cors:
    check: "CORS restricted to known origins"
    bad: "@CrossOrigin(origins = '*')"
    good: "AllowedOrigins = specific frontend URL"
  
  headers:
    required:
      - "X-Content-Type-Options: nosniff"
      - "X-Frame-Options: DENY"
      - "Strict-Transport-Security: max-age=31536000"
      - "Content-Security-Policy: appropriate policy"
    verify: "Spring Security default headers enabled"
```

---

## Security Review Process

```yaml
review_process:
  when: "Self-review phase (before PR creation)"
  
  automated_checks:
    - SonarQube security hotspots
    - SpotBugs (security category)
    - OWASP Dependency Check
    - npm audit (frontend)
  
  manual_checks:
    - Review every endpoint for auth/authz
    - Check all user input paths
    - Verify no PII in logs
    - Confirm secrets from Secrets Manager (not config)
    - Check file handling for path traversal
  
  output:
    if_issues_found:
      critical: "Fix immediately — do not proceed"
      high: "Fix before PR"
      medium: "Fix or document mitigation in PR"
      low: "Note in PR, fix in next iteration"
```

---

## Security Patterns (Use These)

```yaml
secure_patterns:
  input_validation:
    pattern: "Validate at boundary (controller), sanitize before use"
    
  output_encoding:
    pattern: "Encode output for context (HTML, JSON, SQL)"
    
  authentication:
    pattern: "Verify identity on every request (stateless JWT)"
    
  authorization:
    pattern: "Check permissions in service layer (not just controller)"
    
  secrets:
    pattern: "Environment variables → Secrets Manager → never in code"
    
  logging:
    pattern: "Log events, not data. IDs not values. Actions not content."
    
  error_handling:
    pattern: "Generic message to client, detailed message to logs"
```
