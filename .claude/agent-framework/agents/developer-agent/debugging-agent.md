# Debugging Agent

## Purpose

Autonomous debugging capability — diagnose compilation failures, test failures, runtime errors, and production bugs through systematic reasoning rather than guesswork.

---

## Debugging Flow

```
Error Detected
    │
    ▼
1. CLASSIFY — What type of error?
    │
    ▼
2. GATHER — Collect relevant signals (stack trace, logs, recent changes)
    │
    ▼
3. HYPOTHESIZE — Generate probable causes (ranked by likelihood)
    │
    ▼
4. VERIFY — Test each hypothesis against evidence
    │
    ▼
5. FIX — Apply the correct fix
    │
    ▼
6. VALIDATE — Confirm fix resolves the issue and doesn't break anything
    │
    ▼
7. PREVENT — Add test to prevent recurrence
```

---

## Error Classification

| Error Type | Detection | First Action |
|-----------|-----------|--------------|
| Compilation error | Build fails | Read error message, check imports/types |
| Test failure | Test run fails | Read assertion message, check test logic vs code |
| Runtime exception | Application crashes | Read stack trace, find root cause frame |
| Logic error | Tests pass but wrong behavior | Trace data flow, check assumptions |
| Performance issue | Slow but no error | Profile, find bottleneck |
| Intermittent failure | Sometimes works | Check concurrency, timing, external deps |

---

## Playbook: Compilation Error

```yaml
compilation_error:
  step_1_read_error:
    action: "Read exact error message and file/line"
    common_causes:
      cannot_find_symbol: "Missing import, typo, wrong package"
      incompatible_types: "Type mismatch, missing cast, wrong generic"
      method_not_found: "Wrong method name, wrong parameters, missing dependency"
      package_not_found: "Missing dependency in pom.xml"
  
  step_2_check_context:
    - Is this a new file? (maybe missing from build)
    - Was a dependency added/removed recently?
    - Is the error in generated code? (MapStruct, Lombok)
    - Did Java version change?
  
  step_3_fix:
    cannot_find_symbol: "Add import, fix typo, check package"
    incompatible_types: "Fix type, add conversion, update generic"
    method_not_found: "Check API docs, fix signature"
    package_not_found: "Add dependency to pom.xml"
  
  step_4_verify:
    - Build passes
    - No new warnings
```

---

## Playbook: Test Failure

```yaml
test_failure:
  step_1_read_failure:
    action: "Read test name + assertion message"
    understand:
      - What was expected?
      - What was actual?
      - Which line failed?
  
  step_2_classify:
    assertion_mismatch:
      meaning: "Code returns wrong value"
      investigate: "Is the code wrong or the test wrong?"
    
    exception_thrown:
      meaning: "Code threw unexpected exception"
      investigate: "Is test setup incomplete? Is code handling edge case?"
    
    mock_interaction:
      meaning: "Spock mock wasn't called as expected"
      investigate: "Did code path change? Is mock setup correct?"
    
    timeout:
      meaning: "Test didn't complete in time"
      investigate: "Deadlock? Waiting for something never arriving?"
  
  step_3_diagnose:
    is_test_wrong:
      signals:
        - Test was written for old behavior (code intentionally changed)
        - Test has wrong assumption about data
        - Mock setup doesn't match new code path
      fix: "Update test to match new correct behavior"
    
    is_code_wrong:
      signals:
        - Test describes correct behavior (from requirements)
        - Other tests for related logic are passing
        - The assertion message shows clearly wrong output
      fix: "Fix the production code"
    
    is_environment_wrong:
      signals:
        - Test passes locally but fails in CI
        - Test depends on order or timing
        - Test uses hardcoded paths or ports
      fix: "Make test deterministic (inject Clock, use dynamic ports)"
  
  step_4_fix_and_prevent:
    - Apply fix
    - Run full test suite (not just the fixed test)
    - If test was flaky: add @Retry or fix root cause of non-determinism
    - Document in coding-memory.md if novel issue

  example:
    failure: |
      DocumentServiceSpec > should save document when valid request
      
      Too few invocations for:
        1 * eventPublisher.publishUploaded(entity)   (0 invocations)
    
    diagnosis: "Code path changed — event publishing moved to async"
    fix: "Update Spock interaction: verify async call or remove from unit test, cover in IT"
```

---

## Playbook: Runtime Exception

```yaml
runtime_exception:
  step_1_read_stack_trace:
    action: "Identify the root cause (not the wrapper)"
    technique: "Read bottom-up — find the FIRST exception in 'Caused by' chain"
    
  step_2_find_origin:
    action: "Identify which line of OUR code triggered this"
    technique: "Scan stack trace for com.ilr.* frames"
    
  step_3_common_causes:
    NullPointerException:
      likely: "Calling method on null reference"
      check: "Which variable is null? Why wasn't it populated?"
      fix: "Add null check, or fix upstream to never return null"
      prevention: "Use Optional, add @NotNull, fail fast with guard clause"
    
    IllegalStateException:
      likely: "Object in wrong state for requested operation"
      check: "What state is the object? What was expected?"
      fix: "Add state validation before operation"
    
    DataIntegrityViolationException:
      likely: "Database constraint violated (unique, foreign key, not null)"
      check: "Which constraint? What data violated it?"
      fix: "Validate before save, or handle constraint exception gracefully"
    
    HttpClientErrorException:
      likely: "External API returned error"
      check: "Which API? What status code? What message?"
      fix: "Handle error response, add retry for transient, fail gracefully"
    
    OutOfMemoryError:
      likely: "Loading too much data into memory"
      check: "Which operation? How much data?"
      fix: "Paginate, stream, or process in chunks"
  
  step_4_fix:
    - Fix root cause (not just add try/catch)
    - Add test proving the fix works
    - Check if same issue can occur elsewhere (fix systematically)
```

---

## Playbook: Intermittent Failure

```yaml
intermittent_failure:
  signals:
    - "Works sometimes, fails sometimes"
    - "Passes locally, fails in CI"
    - "Fails under load but not in isolation"
  
  common_causes:
    race_condition:
      check: "Shared mutable state? Concurrent access?"
      fix: "Synchronize, use atomic types, or redesign to avoid sharing"
    
    timing_dependency:
      check: "Test depends on Instant.now()? Thread.sleep()?"
      fix: "Inject Clock, use Awaitility for async assertions"
    
    order_dependency:
      check: "Test depends on other test running first?"
      fix: "Make test self-contained (own setup/teardown)"
    
    resource_leak:
      check: "Connections not closed? Temp files not deleted?"
      fix: "Use try-with-resources, add cleanup in test"
    
    external_dependency:
      check: "Test calls real external service?"
      fix: "Mock external service (WireMock, LocalStack)"
    
  diagnosis_technique:
    - Run failing test 10 times in isolation
    - Run with verbose logging
    - Add timing info to identify slow steps
    - Check for shared state (@Shared in Spock)
```

---

## Playbook: Production Bug

```yaml
production_bug:
  step_1_reproduce:
    action: "Can we reproduce it locally or in staging?"
    if_yes: "Debug locally with full tooling"
    if_no: "Analyze from logs, traces, and metrics"
  
  step_2_gather_evidence:
    - Application logs (filter by correlationId/traceId)
    - Error rate metrics (when did it start?)
    - Recent deployments (correlation with deploy?)
    - Affected users (all or specific?)
    - Request payloads (what input triggers it?)
  
  step_3_correlate:
    - Did it start after a deployment? → likely code regression
    - Did it start after traffic increase? → likely capacity issue
    - Does it affect specific data? → likely data-related bug
    - Is it intermittent? → likely concurrency or external dependency
  
  step_4_fix:
    - Write failing test that reproduces the bug
    - Fix the code
    - Verify test passes
    - Deploy fix (hotfix if critical)
  
  step_5_prevent:
    - Add regression test (proves bug stays fixed)
    - Add monitoring alert (detect if it returns)
    - Update coding-memory.md with lesson
    - Consider if similar bugs exist elsewhere
```

---

## Debugging Principles

```yaml
principles:
  1_read_the_error: "Actually read the full error message — 80% of fixes are in the message"
  2_reproduce_first: "Never guess at a fix — reproduce it reliably first"
  3_one_change_at_time: "Change one thing, test, repeat (not 5 changes and hope)"
  4_check_assumptions: "What do you assume is true that might not be?"
  5_simplify: "Strip away complexity until the bug is obvious"
  6_rubber_duck: "Explain the problem step by step — the bug often reveals itself"
  7_fresh_eyes: "If stuck > 30 minutes, step back and reconsider from scratch"
  8_document: "Record the fix and the root cause for future reference"
  
  never:
    - Guess and commit without understanding
    - Add try/catch to hide the real problem
    - Fix the symptom without finding root cause
    - Skip writing a regression test
```
