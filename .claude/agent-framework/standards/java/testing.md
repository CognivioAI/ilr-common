# Testing Standards

> Version 1.1 | AI-DLC Engineering Handbook
> Purpose: Define how all Java services are tested using Spock Framework and BDD.

---

## 1. Principles

- Every feature ships with tests
- Test behavior, not implementation
- Tests are living documentation (BDD)
- Fast feedback: unit > integration > e2e (testing pyramid)
- Tests must be independent and repeatable
- Use Spock's expressive specification language

---

## 2. Testing Pyramid

```
         /  E2E  \          ← Few: critical user journeys
        / Integration \     ← Medium: BDD specs, API contracts, DB access
       /    Unit Tests   \  ← Many: business logic, utilities
```

| Level | Scope | Speed | Tools |
|-------|-------|-------|-------|
| Unit | Single class/method | Milliseconds | Spock Framework |
| Integration (BDD) | Multiple layers, DB, API | Seconds | Spock + Testcontainers + REST Assured |
| E2E | Full HTTP flows | Seconds | Spock + Testcontainers |

---

## 3. Framework: Spock

### Why Spock

- Groovy-based — concise, expressive test code
- Built-in mocking (no Mockito needed)
- BDD blocks: `given`, `when`, `then`, `expect`, `where`
- Data-driven testing with `where:` blocks
- Human-readable test names as strings
- Power assertions with clear failure messages

### Dependencies (Gradle)

```groovy
testImplementation 'org.spockframework:spock-core:2.4-groovy-4.0'
testImplementation 'org.spockframework:spock-spring:2.4-groovy-4.0'
testImplementation 'org.testcontainers:spock:1.19.0'
testImplementation 'org.testcontainers:postgresql:1.19.0'
testImplementation 'io.rest-assured:rest-assured:5.4.0'
```

### Dependencies (Maven)

```xml
<dependency>
    <groupId>org.spockframework</groupId>
    <artifactId>spock-core</artifactId>
    <version>2.4-groovy-4.0</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.spockframework</groupId>
    <artifactId>spock-spring</artifactId>
    <version>2.4-groovy-4.0</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>spock</artifactId>
    <version>1.19.0</version>
    <scope>test</scope>
</dependency>
```

---

## 4. Unit Tests (Spock Specifications)

### Naming Convention

```
src/test/groovy/com/company/service/
├── OrderServiceSpec.groovy
├── OrderValidatorSpec.groovy
└── PriceCalculatorSpec.groovy
```

### Basic Unit Test

```groovy
class OrderServiceSpec extends Specification {

    OrderRepository orderRepository = Mock()
    PaymentGateway paymentGateway = Mock()
    EventPublisher eventPublisher = Mock()

    OrderService orderService = new OrderService(orderRepository, paymentGateway, eventPublisher)

    def "should create order and publish event"() {
        given: "a valid order command"
        def command = new CreateOrderCommand("cust-1", [new OrderItem("item-1", 10.00)])

        and: "repository saves successfully"
        orderRepository.save(_) >> new Order(id: "ord-1", customerId: "cust-1", status: OrderStatus.PENDING)

        when: "the order is created"
        def result = orderService.create(command)

        then: "the order is saved"
        1 * orderRepository.save(_)

        and: "a payment is authorized"
        1 * paymentGateway.authorize(_)

        and: "an event is published"
        1 * eventPublisher.publish(_ as OrderCreatedEvent)

        and: "the order is returned with correct status"
        result.id == "ord-1"
        result.status == OrderStatus.PENDING
    }

    def "should throw BusinessException when order has no items"() {
        given: "a command with no items"
        def command = new CreateOrderCommand("cust-1", [])

        when: "order creation is attempted"
        orderService.create(command)

        then: "a business exception is thrown"
        def ex = thrown(BusinessException)
        ex.message.contains("no items")
    }
}
```

### Data-Driven Tests

```groovy
class PriceCalculatorSpec extends Specification {

    PriceCalculator calculator = new PriceCalculator()

    def "should calculate discount for quantity #quantity at rate #discountRate"() {
        expect: "correct discount is applied"
        calculator.calculateDiscount(quantity, unitPrice) == expectedTotal

        where:
        quantity | unitPrice | discountRate | expectedTotal
        1        | 100.00    | "0%"         | 100.00
        5        | 100.00    | "5%"         | 475.00
        10       | 100.00    | "10%"        | 900.00
        50       | 100.00    | "20%"        | 4000.00
    }

    def "should reject negative quantities"() {
        when:
        calculator.calculateDiscount(-1, 100.00)

        then:
        thrown(ValidationException)
    }
}
```

### Mocking Rules in Spock

```groovy
// Stub — return a value
orderRepository.findById("ord-1") >> Optional.of(order)

// Mock — verify interaction
1 * eventPublisher.publish(_ as OrderCreatedEvent)

// Argument capture
1 * orderRepository.save({ it.customerId == "cust-1" })

// Stub + verify combined
1 * paymentGateway.charge(_) >> PaymentResponse.success()
```

---

## 5. Integration Tests (BDD with Spock)

### BDD Approach

Integration tests follow **Behavior-Driven Development** using Spock's natural
language blocks to describe system behavior from the user's perspective.

Each integration test is a **living specification** that documents:
- **What** the system does (feature)
- **When** it happens (scenario)
- **What** is expected (outcome)

### BDD Structure

```
Feature: [Business capability]
  Scenario: [Specific behavior]
    Given [precondition / system state]
    When [action / trigger]
    Then [expected outcome / observable result]
```

### API Integration Test (BDD Style)

```groovy
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class OrderApiSpec extends Specification {

    @Shared
    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:15")
        .withDatabaseName("testdb")

    @LocalServerPort
    int port

    @Autowired
    OrderRepository orderRepository

    def setup() {
        RestAssured.port = port
        RestAssured.basePath = "/api/v1"
        orderRepository.deleteAll()
    }

    // --- Feature: Order Creation ---

    def "should create a new order and return 201 with order details"() {
        given: "a valid order request for an existing customer"
        def request = [
            customerId: "cust-123",
            items: [
                [productId: "prod-1", quantity: 2, unitPrice: 25.00],
                [productId: "prod-2", quantity: 1, unitPrice: 50.00]
            ],
            shippingAddress: [
                street: "123 Main St",
                city: "Springfield",
                zipCode: "62701"
            ]
        ]

        when: "the order creation API is called"
        def response = given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/orders")
        .then()
            .extract().response()

        then: "a 201 Created response is returned"
        response.statusCode() == 201

        and: "the response contains the order with a generated ID"
        response.jsonPath().getString("id") != null
        response.jsonPath().getString("status") == "PENDING"
        response.jsonPath().getDouble("total") == 100.00

        and: "the order is persisted in the database"
        orderRepository.count() == 1
    }

    def "should return 400 when order has no items"() {
        given: "an order request with empty items list"
        def request = [
            customerId: "cust-123",
            items: []
        ]

        when: "the order creation API is called"
        def response = given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/orders")
        .then()
            .extract().response()

        then: "a 400 Bad Request is returned"
        response.statusCode() == 400

        and: "the error message indicates items are required"
        response.jsonPath().getString("message").contains("items")

        and: "no order is created in the database"
        orderRepository.count() == 0
    }

    def "should return 404 when retrieving a non-existent order"() {
        given: "a non-existent order ID"
        def orderId = "non-existent-id"

        when: "the get order API is called"
        def response = given()
        .when()
            .get("/orders/${orderId}")
        .then()
            .extract().response()

        then: "a 404 Not Found is returned"
        response.statusCode() == 404

        and: "the error message identifies the missing resource"
        response.jsonPath().getString("message").contains("not found")
    }

    // --- Feature: Order Listing ---

    def "should return paginated orders for a customer"() {
        given: "a customer with 5 existing orders"
        (1..5).each { i ->
            orderRepository.save(new OrderEntity(
                customerId: "cust-123",
                status: OrderStatus.PENDING,
                total: BigDecimal.valueOf(i * 10)
            ))
        }

        when: "the first page of orders is requested"
        def response = given()
            .queryParam("page", 0)
            .queryParam("size", 3)
        .when()
            .get("/orders")
        .then()
            .extract().response()

        then: "a 200 OK is returned with the first page"
        response.statusCode() == 200
        response.jsonPath().getList("content").size() == 3
        response.jsonPath().getInt("page.totalElements") == 5
        response.jsonPath().getInt("page.totalPages") == 2
    }
}
```

### Service Integration Test (BDD Style with Events)

```groovy
@SpringBootTest
@Testcontainers
class OrderFulfillmentSpec extends Specification {

    @Shared
    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:15")

    @Autowired
    OrderService orderService

    @Autowired
    OrderRepository orderRepository

    @Autowired
    TestEventListener eventListener

    def setup() {
        orderRepository.deleteAll()
        eventListener.clear()
    }

    def "should fulfill order and publish OrderFulfilledEvent"() {
        given: "an existing confirmed order"
        def order = orderRepository.save(new OrderEntity(
            customerId: "cust-123",
            status: OrderStatus.CONFIRMED,
            total: BigDecimal.valueOf(99.99)
        ))

        when: "the order is marked as fulfilled"
        orderService.fulfill(order.id)

        then: "the order status is updated to FULFILLED"
        def updated = orderRepository.findById(order.id).get()
        updated.status == OrderStatus.FULFILLED

        and: "an OrderFulfilledEvent is published"
        eventListener.receivedEvents.size() == 1
        eventListener.receivedEvents[0] instanceof OrderFulfilledEvent
        eventListener.receivedEvents[0].orderId == order.id
    }

    def "should reject fulfillment of non-confirmed orders"() {
        given: "an order in PENDING status"
        def order = orderRepository.save(new OrderEntity(
            customerId: "cust-123",
            status: OrderStatus.PENDING,
            total: BigDecimal.valueOf(50.00)
        ))

        when: "fulfillment is attempted"
        orderService.fulfill(order.id)

        then: "a BusinessException is thrown"
        def ex = thrown(BusinessException)
        ex.message.contains("Cannot fulfill")

        and: "the order status remains unchanged"
        orderRepository.findById(order.id).get().status == OrderStatus.PENDING

        and: "no event is published"
        eventListener.receivedEvents.isEmpty()
    }
}
```

### Database Integration Test (BDD)

```groovy
@DataJpaTest
@Testcontainers
class OrderRepositorySpec extends Specification {

    @Shared
    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:15")

    @Autowired
    OrderRepository orderRepository

    def setup() {
        orderRepository.deleteAll()
    }

    def "should find orders by customer ID with pagination"() {
        given: "multiple orders for different customers"
        (1..5).each { orderRepository.save(orderFor("cust-1")) }
        (1..3).each { orderRepository.save(orderFor("cust-2")) }

        when: "orders for customer 1 are queried with page size 3"
        def page = orderRepository.findByCustomerId("cust-1", PageRequest.of(0, 3))

        then: "the first page returns 3 results"
        page.content.size() == 3
        page.totalElements == 5
        page.totalPages == 2

        and: "all results belong to the correct customer"
        page.content.every { it.customerId == "cust-1" }
    }

    def "should find recent orders by status since a given date"() {
        given: "orders with different statuses and dates"
        orderRepository.save(orderFor("cust-1", OrderStatus.PENDING, Instant.now().minus(1, ChronoUnit.HOURS)))
        orderRepository.save(orderFor("cust-1", OrderStatus.PENDING, Instant.now().minus(3, ChronoUnit.DAYS)))
        orderRepository.save(orderFor("cust-1", OrderStatus.CONFIRMED, Instant.now().minus(1, ChronoUnit.HOURS)))

        when: "recent PENDING orders from the last 24 hours are queried"
        def results = orderRepository.findRecentByStatus(
            OrderStatus.PENDING,
            Instant.now().minus(24, ChronoUnit.HOURS)
        )

        then: "only the recent PENDING order is returned"
        results.size() == 1
        results[0].status == OrderStatus.PENDING
    }

    private OrderEntity orderFor(String customerId, OrderStatus status = OrderStatus.PENDING, Instant createdAt = Instant.now()) {
        new OrderEntity(customerId: customerId, status: status, total: 99.99, createdAt: createdAt)
    }
}
```

---

## 6. BDD Scenarios: Writing Good Specifications

### Naming Convention

Test method names should read as behavior specifications:

```groovy
// ✅ Good — describes behavior
def "should reject order when customer credit limit is exceeded"()
def "should apply 20% discount for orders over 500 dollars"()
def "should send notification when order status changes to SHIPPED"()

// ❌ Bad — describes implementation
def "testCreateOrder"()
def "test_repository_save"()
def "testException"()
```

### Given/When/Then Best Practices

```groovy
// ✅ Good — clear preconditions, action, and outcomes
def "should calculate shipping cost based on destination zone"() {
    given: "an order weighing 5kg shipping to zone 3"
    def order = OrderFixture.withWeight(5.0)
    def destination = Zone.ZONE_3

    when: "shipping cost is calculated"
    def cost = shippingCalculator.calculate(order, destination)

    then: "the cost reflects zone 3 pricing for 5kg"
    cost == new Money(15.50, Currency.USD)
}

// ✅ Good — multiple and: blocks for readability
def "should process refund for cancelled order"() {
    given: "a confirmed order with payment"
    def order = createConfirmedOrder()

    and: "a valid cancellation reason"
    def reason = CancellationReason.CUSTOMER_REQUEST

    when: "the order is cancelled"
    orderService.cancel(order.id, reason)

    then: "the order status is CANCELLED"
    orderRepository.findById(order.id).get().status == OrderStatus.CANCELLED

    and: "a refund is initiated"
    1 * paymentGateway.refund(order.paymentId, order.total)

    and: "the customer is notified"
    1 * notificationService.sendCancellationEmail(order.customerId)
}
```

---

## 7. Test Data

### Use Fixtures and Builders

```groovy
class OrderFixture {

    static OrderEntity pending(Map overrides = [:]) {
        new OrderEntity(
            id: overrides.id ?: UUID.randomUUID().toString(),
            customerId: overrides.customerId ?: "cust-1",
            status: OrderStatus.PENDING,
            total: overrides.total ?: BigDecimal.valueOf(99.99),
            createdAt: overrides.createdAt ?: Instant.now()
        )
    }

    static OrderEntity confirmed(Map overrides = [:]) {
        pending(overrides + [status: OrderStatus.CONFIRMED])
    }

    static CreateOrderCommand command(Map overrides = [:]) {
        new CreateOrderCommand(
            customerId: overrides.customerId ?: "cust-1",
            items: overrides.items ?: [new OrderItem("prod-1", 2, BigDecimal.TEN)]
        )
    }
}
```

### In Spock Tests

```groovy
def "should confirm order"() {
    given: "a pending order"
    def order = orderRepository.save(OrderFixture.pending(customerId: "cust-99"))

    when: "order is confirmed"
    orderService.confirm(order.id)

    then: "status changes to CONFIRMED"
    orderRepository.findById(order.id).get().status == OrderStatus.CONFIRMED
}
```

---

## 8. Lifecycle and Shared State

```groovy
@SpringBootTest
@Testcontainers
class OrderIntegrationSpec extends Specification {

    // Shared container — started once for all tests in class
    @Shared
    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:15")

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl)
        registry.add("spring.datasource.username", postgres::getUsername)
        registry.add("spring.datasource.password", postgres::getPassword)
    }

    // setup() runs before each test
    def setup() {
        orderRepository.deleteAll()
    }

    // cleanup() runs after each test
    def cleanup() {
        // optional teardown
    }

    // setupSpec() runs once before all tests
    def setupSpec() {
        // one-time setup
    }
}
```

---

## 9. Coverage Requirements

| Type | Minimum |
|------|---------|
| Service layer (business logic) | 90% |
| Controller layer (via integration) | 80% |
| Repository layer (custom queries) | 80% |
| Utility classes | 95% |
| DTOs / Records | Excluded |

---

## 10. Anti-Patterns

- ❌ Testing private methods directly
- ❌ Tests that depend on execution order
- ❌ Tests that call real external services
- ❌ Multiple unrelated behaviors in one test
- ❌ Tests without clear given/when/then structure
- ❌ Sleeping/waiting in tests (use `PollingConditions` instead)
- ❌ Using JUnit assertions in Spock (use Spock's power assertions)
- ❌ Mixing Java test frameworks with Spock

### Use PollingConditions for Async

```groovy
def "should process event asynchronously"() {
    given: "an order event"
    eventPublisher.publish(new OrderCreatedEvent("ord-1"))

    expect: "the event is processed within 5 seconds"
    new PollingConditions(timeout: 5).eventually {
        orderRepository.findById("ord-1").isPresent()
    }
}
```

---

## 11. Checklist

- [ ] Spock Framework used for all tests
- [ ] Unit specs for all service methods
- [ ] BDD integration specs for API endpoints
- [ ] BDD integration specs for business workflows
- [ ] Repository specs with Testcontainers
- [ ] Fixtures/builders for test data
- [ ] Spock power assertions (no JUnit/AssertJ)
- [ ] No hardcoded external dependencies
- [ ] Tests run independently and in any order
- [ ] Coverage meets minimums
- [ ] Test names read as behavior specifications
- [ ] given/when/then blocks used consistently

---

## 12. AI Agent Prompt

```
Use Spock Framework for all tests.
Write tests in Groovy as Specification classes.
Use given/when/then BDD blocks.
Use Spock's built-in mocking — no Mockito.
Use data-driven where: blocks for parameterized tests.
Integration tests use Testcontainers + REST Assured.
Integration tests follow BDD approach — describe behavior from user perspective.
Test names are behavior descriptions (not method names).
Use PollingConditions for async assertions.
Use power assertions — no JUnit or AssertJ.
```
