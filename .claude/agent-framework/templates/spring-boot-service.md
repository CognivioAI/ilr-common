# Spring Boot Service Template

## Usage

Use this template when creating a new Spring Boot microservice.

## Project Structure

```
service-name/
├── src/
│   ├── main/
│   │   ├── java/com/company/servicename/
│   │   │   ├── ServiceNameApplication.java
│   │   │   ├── config/
│   │   │   │   ├── SecurityConfig.java
│   │   │   │   └── AppConfig.java
│   │   │   ├── controller/
│   │   │   │   └── ResourceController.java
│   │   │   ├── service/
│   │   │   │   ├── ResourceService.java
│   │   │   │   └── impl/ResourceServiceImpl.java
│   │   │   ├── repository/
│   │   │   │   └── ResourceRepository.java
│   │   │   ├── domain/
│   │   │   │   └── Resource.java
│   │   │   ├── dto/
│   │   │   │   ├── CreateResourceRequest.java
│   │   │   │   └── ResourceResponse.java
│   │   │   ├── exception/
│   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   └── ResourceNotFoundException.java
│   │   │   └── mapper/
│   │   │       └── ResourceMapper.java
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       └── db/migration/
│   │           └── V1__create_resource_table.sql
│   └── test/
│       └── java/com/company/servicename/
│           ├── controller/
│           │   └── ResourceControllerTest.java
│           ├── service/
│           │   └── ResourceServiceTest.java
│           └── integration/
│               └── ResourceIntegrationTest.java
├── Dockerfile
├── pom.xml
└── README.md
```

## Key Files

### application.yml

```yaml
spring:
  application:
    name: service-name
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:servicedb}
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:postgres}
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
  flyway:
    enabled: true

server:
  port: 8080

management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
  endpoint:
    health:
      show-details: when-authorized

logging:
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] [%X{traceId}] %-5level %logger{36} - %msg%n"
```

### Dockerfile

```dockerfile
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/*.jar app.jar
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s CMD wget -qO- http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
```

## Checklist

- [ ] Project structure created
- [ ] Dependencies configured (pom.xml)
- [ ] Application configuration (application.yml)
- [ ] Security configuration
- [ ] Global exception handler
- [ ] Health check endpoints
- [ ] Database migration (initial schema)
- [ ] Dockerfile
- [ ] Unit tests
- [ ] Integration tests
- [ ] README with setup instructions
