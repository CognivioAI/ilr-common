# Architecture Fitness Functions

> Architecture Agent — Automated Architecture Validation

---

## Purpose

Fitness functions are automated checks that verify every service conforms
to architectural standards. They turn "coding standards" into enforced rules
rather than suggestions. Borrowed from "Building Evolutionary Architectures."

---

## Concept

```
Standard Document        →    Fitness Function
"Every service must          "CI pipeline fails if
 expose /actuator/health"     /actuator/health not found"
```

---

## Fitness Function Categories

| Category | What It Checks | Runs In |
|----------|---------------|---------|
| Structural | Package structure, dependency direction | Build (Maven/Gradle) |
| API | OpenAPI spec present, version prefix | CI |
| Security | No plaintext secrets, OWASP pass | CI |
| Observability | Health endpoint, metrics endpoint | CI + Runtime |
| Container | Dockerfile present, no root user | CI |
| Performance | Test coverage, no N+1 queries | CI |
| Compliance | PII fields documented, audit log events | CI |

---

## Structural Fitness Functions

### 1. Dependency Direction (ArchUnit)

Controller must not call Repository directly:

```java
@Test
void controllersMustNotAccessRepositoriesDirectly() {
    noClasses()
        .that().resideInAPackage("..controller..")
        .should().dependOnClassesThat()
        .resideInAPackage("..repository..")
        .check(importedClasses);
}
```

### 2. No Field Injection

```java
@Test
void noFieldInjection() {
    noFields()
        .should().beAnnotatedWith(Autowired.class)
        .check(importedClasses);
}
```

### 3. Entities Not Exposed in Controllers

```java
@Test
void controllersMustNotReturnEntities() {
    methods()
        .that().areDeclaredInClassesThat().resideInAPackage("..controller..")
        .should().notHaveRawReturnType(resideInAPackage("..entity.."))
        .check(importedClasses);
}
```

### 4. Services Must Be in Service Package

```java
@Test
void serviceAnnotationOnlyInServicePackage() {
    classes()
        .that().areAnnotatedWith(Service.class)
        .should().resideInAPackage("..service..")
        .check(importedClasses);
}
```

---

## API Fitness Functions

### 5. All Endpoints Have Version Prefix

```groovy
// Spock integration test
def "all REST endpoints must be versioned with /api/v prefix"() {
    expect:
    allEndpoints.every { it.path.startsWith("/api/v") }
}
```

### 6. OpenAPI Documentation Present

```groovy
def "service must expose OpenAPI spec"() {
    when:
    def response = get("/v3/api-docs")

    then:
    response.statusCode() == 200
    response.jsonPath().getString("openapi") != null
}
```

---

## Security Fitness Functions

### 7. No Plaintext Secrets in Properties

```groovy
// CI script
def "no plaintext passwords in application.yml"() {
    given:
    def content = new File("src/main/resources/application.yml").text

    expect:
    !content.contains("password: ")  // must reference env var or secrets manager
    !content.contains("secret: ")
}
```

### 8. OWASP Dependency Check Passes

```xml
<!-- Maven plugin in CI — fails build on CVSS ≥ 7 -->
<plugin>
    <groupId>org.owasp</groupId>
    <artifactId>dependency-check-maven</artifactId>
    <configuration>
        <failBuildOnCVSS>7</failBuildOnCVSS>
    </configuration>
</plugin>
```

---

## Observability Fitness Functions

### 9. Health Endpoints Present

```groovy
def "service must expose liveness and readiness probes"() {
    expect:
    given().get("/actuator/health/liveness").statusCode() == 200
    given().get("/actuator/health/readiness").statusCode() == 200
}
```

### 10. Prometheus Metrics Endpoint Present

```groovy
def "service must expose prometheus metrics"() {
    when:
    def response = given().get("/actuator/prometheus")

    then:
    response.statusCode() == 200
    response.body().asString().contains("jvm_memory_used_bytes")
}
```

---

## Container Fitness Functions

### 11. Dockerfile Must Not Run as Root

```groovy
// Checked in CI via Hadolint or Dockle
def "Dockerfile must not use root user"() {
    given:
    def dockerfile = new File("Dockerfile").text

    expect:
    dockerfile.contains("USER ") && !dockerfile.contains("USER root")
}
```

### 12. Image Must Not Use Latest Tag

```groovy
def "no latest image tags in Kubernetes manifests"() {
    given:
    def manifests = new File("helm/").text

    expect:
    !manifests.contains(":latest")
}
```

---

## Performance Fitness Functions

### 13. No Unbounded Queries (Custom)

```java
@Test
void noFindAllWithoutPagination() {
    noMethods()
        .that().areDeclaredInClassesThat().areAnnotatedWith(Repository.class)
        .and().haveNameContaining("findAll")
        .should().haveRawReturnType(List.class)  // must return Page, not List
        .check(importedClasses);
}
```

### 14. Test Coverage Thresholds (JaCoCo)

```xml
<rule>
    <element>CLASS</element>
    <limits>
        <limit>
            <counter>LINE</counter>
            <value>COVEREDRATIO</value>
            <minimum>0.80</minimum>
        </limit>
    </limits>
</rule>
```

---

## Full Fitness Function Checklist (per service)

Every service must pass before deployment:

- [ ] Controller → Service → Repository (no skipping)
- [ ] No `@Autowired` field injection
- [ ] No entity returned from controller
- [ ] All endpoints prefixed `/api/v{n}/`
- [ ] OpenAPI spec exposed
- [ ] No plaintext secrets in config files
- [ ] OWASP check passes (CVSS < 7)
- [ ] `/actuator/health/liveness` returns 200
- [ ] `/actuator/health/readiness` returns 200
- [ ] `/actuator/prometheus` returns metrics
- [ ] Dockerfile has non-root user
- [ ] Helm values use no `latest` image tag
- [ ] Test coverage ≥ 80% (line)
- [ ] Service layer coverage ≥ 90%

---

## CI Enforcement

These checks run in the CI pipeline and **block deployment** if they fail:

```yaml
# .github/workflows/ci.yml
- name: Run fitness functions
  run: ./gradlew test -Dtest=ArchitectureFitnessFunctions

- name: OWASP dependency check
  run: ./gradlew dependencyCheckAnalyze

- name: Check Dockerfile
  run: docker run --rm -i hadolint/hadolint < Dockerfile

- name: Check for latest tags
  run: grep -r ":latest" helm/ && exit 1 || echo "OK"
```
