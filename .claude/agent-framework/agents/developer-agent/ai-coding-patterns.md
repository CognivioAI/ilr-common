# AI Coding Patterns

## Purpose

Define patterns for implementing AI features within the ILR platform — Bedrock integration, prompt management, RAG pipelines, confidence routing, and AI service resilience.

---

## AI Service Architecture

```
┌──────────────────────────────────────────────────┐
│  Application Service (Spring Boot)                │
│                                                  │
│  ┌──────────┐   ┌──────────────┐   ┌─────────┐ │
│  │ API Layer│──▶│ AI Service   │──▶│ Bedrock │ │
│  │          │   │              │   │ Client  │ │
│  │          │   │ - Prompt Mgmt│   │         │ │
│  │          │   │ - Retry      │   └─────────┘ │
│  │          │   │ - Circuit Brk│                │
│  │          │   │ - Metrics    │                │
│  └──────────┘   └──────────────┘                │
└──────────────────────────────────────────────────┘
```

---

## Bedrock Client Pattern

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class BedrockClassificationService {

    private final BedrockRuntimeClient bedrockClient;
    private final PromptManager promptManager;
    private final MeterRegistry meterRegistry;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    private static final String MODEL_ID = "anthropic.claude-3-sonnet-20240229-v1:0";

    @CircuitBreaker(name = "bedrock", fallbackMethod = "classifyFallback")
    @Retry(name = "bedrock")
    @TimeLimiter(name = "bedrock")
    public ClassificationResult classify(Document document) {
        var prompt = promptManager.getPrompt("document-classification", document);
        
        var timer = Timer.builder("ai.request.duration")
                .tag("model", MODEL_ID)
                .tag("operation", "classify")
                .register(meterRegistry);

        return timer.record(() -> {
            var request = InvokeModelRequest.builder()
                    .modelId(MODEL_ID)
                    .contentType("application/json")
                    .body(SdkBytes.fromUtf8String(buildRequestBody(prompt)))
                    .build();

            var response = bedrockClient.invokeModel(request);
            var result = parseResponse(response);
            
            recordMetrics(result);
            return result;
        });
    }

    private ClassificationResult classifyFallback(Document document, Exception ex) {
        log.warn("Bedrock classification failed, using fallback: {}", ex.getMessage());
        meterRegistry.counter("ai.fallback.total", "reason", ex.getClass().getSimpleName()).increment();
        
        return ClassificationResult.builder()
                .category("UNCLASSIFIED")
                .confidence(0.0)
                .requiresHumanReview(true)
                .fallbackReason(ex.getMessage())
                .build();
    }

    private void recordMetrics(ClassificationResult result) {
        meterRegistry.counter("ai.tokens.input", "model", MODEL_ID)
                .increment(result.inputTokens());
        meterRegistry.counter("ai.tokens.output", "model", MODEL_ID)
                .increment(result.outputTokens());
        meterRegistry.summary("ai.confidence", "model", MODEL_ID)
                .record(result.confidence());
    }
}
```

---

## Prompt Management

### Prompt Storage and Versioning

```java
@Service
@RequiredArgsConstructor
public class PromptManager {

    private final PromptRepository promptRepository;

    public String getPrompt(String promptName, Object context) {
        var template = promptRepository.findActiveVersion(promptName)
                .orElseThrow(() -> new PromptNotFoundException(promptName));
        
        return renderTemplate(template, context);
    }
}
```

### Prompt Template Structure

```yaml
prompts:
  document-classification:
    version: "1.2.0"
    model: "anthropic.claude-3-sonnet"
    template: |
      You are a document classification specialist.
      
      Classify the following document into one of these categories:
      - INVOICE
      - CONTRACT
      - REPORT
      - CORRESPONDENCE
      - IDENTITY_DOCUMENT
      - OTHER
      
      Document metadata:
      - Filename: {{filename}}
      - Size: {{size}} bytes
      - Content preview: {{contentPreview}}
      
      Respond in JSON format:
      {
        "category": "CATEGORY_NAME",
        "confidence": 0.0-1.0,
        "reasoning": "Brief explanation"
      }
    
    max_tokens: 256
    temperature: 0.1
    stop_sequences: ["}"]
```

---

## Confidence Routing

```java
@Service
@RequiredArgsConstructor
public class ClassificationRouter {

    private static final double AUTO_ACCEPT_THRESHOLD = 0.85;
    private static final double HUMAN_REVIEW_THRESHOLD = 0.60;

    public ClassificationDecision route(ClassificationResult result) {
        if (result.confidence() >= AUTO_ACCEPT_THRESHOLD) {
            return ClassificationDecision.autoAccept(result);
        } else if (result.confidence() >= HUMAN_REVIEW_THRESHOLD) {
            return ClassificationDecision.lowConfidence(result);
        } else {
            return ClassificationDecision.requiresHumanReview(result);
        }
    }
}
```

```yaml
routing_rules:
  confidence >= 0.85: "Auto-accept classification"
  confidence 0.60-0.84: "Accept but flag for spot-check"
  confidence < 0.60: "Queue for human review"
  
  metrics:
    track: "Distribution of confidence scores"
    alert: "If > 30% go to human review → prompt needs improvement"
```

---

## AI Resilience Patterns

### Circuit Breaker Configuration

```yaml
resilience4j:
  circuitbreaker:
    instances:
      bedrock:
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 30s
        permittedNumberOfCallsInHalfOpenState: 3
  
  retry:
    instances:
      bedrock:
        maxAttempts: 3
        waitDuration: 1s
        exponentialBackoffMultiplier: 2
        retryExceptions:
          - software.amazon.awssdk.services.bedrockruntime.model.ThrottlingException
          - software.amazon.awssdk.core.exception.SdkServiceException
        ignoreExceptions:
          - software.amazon.awssdk.services.bedrockruntime.model.ValidationException
  
  timelimiter:
    instances:
      bedrock:
        timeoutDuration: 30s
```

### Rate Limiting

```java
@Component
public class BedrockRateLimiter {
    
    private final RateLimiter rateLimiter = RateLimiter.create(80.0 / 60.0); // 80 req/min
    
    public void acquire() {
        if (!rateLimiter.tryAcquire(5, TimeUnit.SECONDS)) {
            throw new AiRateLimitExceededException("Bedrock rate limit reached");
        }
    }
}
```

---

## AI Testing

### Unit Testing AI Services (Mock Bedrock)

```groovy
class BedrockClassificationServiceSpec extends Specification {

    BedrockRuntimeClient bedrockClient = Mock()
    PromptManager promptManager = Mock()
    MeterRegistry meterRegistry = new SimpleMeterRegistry()
    
    BedrockClassificationService service = new BedrockClassificationService(
            bedrockClient, promptManager, meterRegistry)

    def "should classify document with high confidence"() {
        given: "a document and a successful Bedrock response"
        def document = DocumentFixture.aDocument().build()
        promptManager.getPrompt("document-classification", _) >> "prompt text"
        bedrockClient.invokeModel(_) >> mockResponse('{"category":"INVOICE","confidence":0.95}')

        when: "classification is requested"
        def result = service.classify(document)

        then: "document is classified correctly"
        result.category == "INVOICE"
        result.confidence == 0.95
    }

    def "should return fallback when Bedrock unavailable"() {
        given: "Bedrock is failing"
        bedrockClient.invokeModel(_) >> { throw new SdkServiceException.builder().message("unavailable").build() }

        when: "classification is requested"
        def result = service.classify(DocumentFixture.aDocument().build())

        then: "fallback result returned"
        result.category == "UNCLASSIFIED"
        result.requiresHumanReview == true
    }
}
```

### Integration Testing (with Mock API)

```groovy
class ClassificationFlowIT extends Specification {
    // Use WireMock to simulate Bedrock responses
    // Test full flow: API → Service → (mock) Bedrock → Response
}
```

---

## AI Cost Tracking

```java
@Aspect
@Component
@RequiredArgsConstructor
public class AiCostTracker {

    private final MeterRegistry meterRegistry;

    @AfterReturning(pointcut = "@annotation(TrackAiCost)", returning = "result")
    public void trackCost(ClassificationResult result) {
        double cost = calculateCost(result.inputTokens(), result.outputTokens());
        
        meterRegistry.counter("ai.cost.total")
                .increment(cost);
        meterRegistry.summary("ai.cost.per_request")
                .record(cost);
    }
    
    private double calculateCost(int inputTokens, int outputTokens) {
        // Claude 3 Sonnet pricing
        return (inputTokens * 3.0 / 1_000_000) + (outputTokens * 15.0 / 1_000_000);
    }
}
```

---

## Rules for AI Code

```yaml
ai_coding_rules:
  never:
    - Call AI synchronously in API request path (always async via queue)
    - Ignore AI failures (always have fallback)
    - Trust AI output blindly (validate schema, check confidence)
    - Log full prompts with user data (PII risk)
    - Hardcode model IDs (configure externally)
  
  always:
    - Implement circuit breaker for AI calls
    - Track token usage and cost per request
    - Validate AI response format before processing
    - Provide human escalation path for low confidence
    - Version prompts (never modify in-place)
    - Test with mocked AI responses (deterministic tests)
```
