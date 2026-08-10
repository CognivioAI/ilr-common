# Security Standards

> Version 1.0 | AI-DLC Engineering Handbook
> Purpose: Define security practices for all Java services.

---

## 1. Principles

- Security is not optional — it's a default
- Defense in depth (multiple layers)
- Least privilege everywhere
- Never trust user input
- Fail closed (deny by default)

---

## 2. Authentication

### JWT Token Validation

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health/**").permitAll()
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
            .build();
    }
}
```

### Rules

- Stateless authentication (JWT)
- Validate tokens on every request
- Short token expiration (15 minutes access, 7 days refresh)
- Rotate signing keys regularly

---

## 3. Authorization

- Role-based access control (RBAC)
- Method-level security with `@PreAuthorize`
- Never expose admin endpoints without role check

```java
@PreAuthorize("hasRole('MANAGER')")
public void approveOrder(String orderId) { ... }
```

---

## 4. Input Validation

**Validate everything. Trust nothing.**

```java
// ✅ Use Jakarta Validation on all DTOs
public record CreateUserRequest(
    @NotBlank @Size(max = 100) String name,
    @NotBlank @Email String email,
    @NotNull @Size(min = 8, max = 128) String password
) {}

// ✅ Validate at controller level
@PostMapping
public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
    ...
}
```

### Protect Against

- SQL Injection — use parameterized queries (Spring Data handles this)
- XSS — sanitize output, use Content-Security-Policy headers
- Path traversal — validate file paths, reject `..`
- Mass assignment — use DTOs, never bind directly to entities

---

## 5. Secrets Management

**Never hardcode secrets.**

| ❌ Never | ✅ Always |
|----------|-----------|
| `application.yml` with passwords | AWS Secrets Manager |
| Environment variables in code | Spring Cloud Config |
| Committed `.env` files | Kubernetes External Secrets |

```yaml
# ✅ Reference secrets via environment or Secrets Manager
spring:
  datasource:
    password: ${DB_PASSWORD}
```

---

## 6. Data Protection

- Encrypt data at rest (database encryption, S3 SSE)
- Encrypt data in transit (TLS 1.3 minimum)
- Hash passwords with BCrypt (cost factor 12+)
- Never log sensitive data (passwords, tokens, PII)
- Mask sensitive fields in API responses

---

## 7. Dependency Security

- Run OWASP dependency-check in CI pipeline
- Update dependencies monthly
- No dependencies with known critical CVEs in production
- Pin dependency versions (no dynamic ranges)

---

## 8. HTTP Security Headers

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    return http
        .headers(headers -> headers
            .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'"))
            .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
            .contentTypeOptions(Customizer.withDefaults())
        )
        .build();
}
```

---

## 9. Anti-Patterns

- ❌ Disabling CSRF without stateless justification
- ❌ `permitAll()` on sensitive endpoints
- ❌ Storing passwords in plain text
- ❌ Logging JWT tokens or passwords
- ❌ Using `@Secured` (prefer `@PreAuthorize`)
- ❌ Trusting client-side validation alone

---

## 10. Checklist

- [ ] JWT validation configured
- [ ] Role-based access on all endpoints
- [ ] Jakarta Validation on all DTOs
- [ ] No secrets in source code
- [ ] TLS enforced
- [ ] Security headers configured
- [ ] OWASP dependency-check in pipeline
- [ ] Passwords hashed with BCrypt
- [ ] No sensitive data in logs
