# Caching Standards

> Version 1.0 | AI-DLC Engineering Handbook
> Purpose: Define caching strategies for Spring Boot services.

---

## 1. Principles

- Cache read-heavy, write-light data
- Every cache entry has a TTL (no infinite caches)
- Invalidate on writes
- Monitor hit rates
- Cache is an optimization, not a data source

---

## 2. When to Cache

| Cache | Don't Cache |
|-------|-------------|
| Reference data (countries, categories) | Frequently changing data |
| User sessions / profiles | Write-heavy entities |
| Expensive query results | Security-sensitive data |
| External API responses | Data requiring real-time accuracy |
| Configuration values | Large unbounded datasets |

---

## 3. Cache Layers

```
Client (browser cache) → CDN (CloudFront) → Application (Redis) → Database
```

---

## 4. Spring Cache Abstraction

### Configuration

```java
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        var defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(15))
            .serializeValuesWith(
                SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));

        return RedisCacheManager.builder(factory)
            .cacheDefaults(defaultConfig)
            .withCacheConfiguration("products", 
                defaultConfig.entryTtl(Duration.ofHours(1)))
            .withCacheConfiguration("user-sessions",
                defaultConfig.entryTtl(Duration.ofMinutes(30)))
            .build();
    }
}
```

### Usage

```java
@Cacheable(value = "products", key = "#productId")
public ProductResponse getProduct(String productId) {
    return productRepository.findById(productId)
        .map(productMapper::toResponse)
        .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
}

@CachePut(value = "products", key = "#productId")
public ProductResponse updateProduct(String productId, UpdateProductRequest request) {
    // Update and return — cache is refreshed
}

@CacheEvict(value = "products", key = "#productId")
public void deleteProduct(String productId) {
    productRepository.deleteById(productId);
}
```

---

## 5. Cache-Aside Pattern

1. Check cache → if hit, return cached value
2. If miss → query database → store in cache → return

Spring's `@Cacheable` implements this automatically.

---

## 6. TTL Guidelines

| Data Type | TTL |
|-----------|-----|
| Static reference data | 1–24 hours |
| User profiles | 15–30 minutes |
| Search results | 5–15 minutes |
| API rate limit counters | 1 minute |
| Session data | 30 minutes |

---

## 7. Anti-Patterns

- ❌ Caching without TTL (memory leaks)
- ❌ Caching mutable data without invalidation
- ❌ Caching per-user data globally
- ❌ Cache stampede (use locks for expensive computations)
- ❌ Caching errors/null values
- ❌ Over-caching (cache what's needed, not everything)

---

## 8. Checklist

- [ ] Redis configured as cache backend
- [ ] TTL set on all cache entries
- [ ] `@CacheEvict` on write operations
- [ ] Cache hit rate monitored (Prometheus metrics)
- [ ] Graceful degradation if Redis unavailable
- [ ] Serialization configured (JSON)
- [ ] Cache warming for critical data on startup
