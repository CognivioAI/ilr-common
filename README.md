# ilr-common

Shared cross-cutting library for the **ILR Application Agent** platform. Every ILR microservice
depends on this jar instead of copy-pasting the same auth / tenancy / error-handling code. It is a
plain library jar (`packaging: jar` — the Spring Boot repackage goal is never applied) consumed as:

```xml
<dependency>
    <groupId>com.cognivio.ai</groupId>
    <artifactId>ilr-common</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

It ships Spring Boot **auto-configurations** (registered via
`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`), so a consuming
service gets everything below just by adding the dependency and setting `ilr.security.issuer-uri` —
no `@Import`, no `@ComponentScan` of this package required.

## What it provides

| Package | What you get |
|---|---|
| `security` | OAuth2 **resource-server JWT verification** against Cognito (JWKS/issuer discovery), Cognito group/role → Spring authority mapping (`JwtRoleConverter`), JSON `401`/`403` handlers, and a stateless `SecurityFilterChain`. Configured under `ilr.security.*`. |
| `context` | Request-scoped **`TenantContext`** (tenantId / firmId / userId / roles) populated **only from verified JWT claims** — never from a client header — plus the filter and claim resolver that fill it. A missing/invalid tenant claim is a `403`. |
| `tenancy` | Postgres **Row-Level Security** binding: an `@Transactional` aspect issues `set_config('app.current_tenant_id', ?, true)` per transaction so RLS policies enforce tenant isolation at the database. Configured under `ilr.rls.*`. |
| `web` | The platform's standard `ErrorResponse` / `FieldErrorDetail` / `PageMeta` DTOs and a `@RestControllerAdvice` (`CommonExceptionHandler`) at lowest precedence that maps validation, domain, and optimistic-lock errors to the standard error shape. |

## Security posture — fails **closed**

`ilr.security.enabled=true` is the default. If security is enabled but **no** `JwtDecoder` bean and
**neither** `ilr.security.issuer-uri` **nor** `ilr.security.jwks-uri` is configured, the service
**refuses to start** (`IllegalStateException` on context refresh) rather than silently booting with
every endpoint permitted. A forgotten issuer URI in production can never disable authentication.

The only two ways to run without token verification are explicit opt-ins:

- `ilr.security.enabled=false` — installs a permit-all chain (used by standalone MockMvc unit tests).
- `ilr.security.dev-permit-all=true` — permit-all with a loud `WARN` log (local development only).

## Configuration

```yaml
ilr:
  security:
    issuer-uri: https://cognito-idp.eu-west-2.amazonaws.com/<userPoolId>  # or jwks-uri
    # tenant-claim: tenant_id      # JWT claim carrying the tenant id (default)
    # firm-claim: firm_id
    # role-claims: [cognito:groups, roles]
    # authority-prefix: ROLE_
    # permit-list: [/actuator/health, /actuator/health/**, /actuator/info,
    #               /v3/api-docs/**, /swagger-ui/**, /swagger-ui.html]
  rls:
    enabled: true
    tenant-parameter: app.current_tenant_id
```

## Testability

The `JwtDecoder` is resolved via an `ObjectProvider`, so a test can register its own `JwtDecoder`
bean (e.g. a Nimbus decoder over a local RSA test key) and the resource server will use it — no live
Cognito JWKS endpoint required.

## Build

```bash
mvn clean install   # JDK 21; installs 0.1.0-SNAPSHOT to your local ~/.m2
```

Tests are Spock (Groovy) specs under `src/test/groovy`, matching the services' convention.

> **CI note:** consuming services build against this SNAPSHOT. For CI (which has no shared local
> `~/.m2`), publish the artifact to a reachable Maven repository — AWS CodeArtifact or Nexus — and
> add it to each service's `<repositories>`/`settings.xml`. Until that exists, cross-service CI must
> `mvn install` this module first.
