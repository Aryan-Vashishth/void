# VOID Framework — Façade Boundary Audit

**Date:** 2026-05-29  
**Scope:** `VOID` session façade, `FlowExecutor` visibility, session-level operations, escape-hatch design  
**Goal:** Evaluate whether `VOID` functions as the primary session abstraction and identify gaps between current state and the target façade-first model

---

## 1. Current State Assessment

### 1.1 VOID Façade — Present Surface

| Method | Category | Status |
|--------|----------|--------|
| `start()` / `start(Profile)` | Lifecycle | ✅ Present |
| `shutdown()` | Lifecycle | ⚠️ Present but incomplete (calls `DriverManager.quitAll()` only, not `engine.shutdown()`) |
| `interaction()` | Legacy access | ⚠️ Returns raw `Interactions` — Selenium-coupled, 833-line class |
| `getEngine()` | Escape hatch | ✅ Present |
| `run(Flow)` | Execution | ✅ Present — delegates to internal `FlowExecutor` |
| `getContext()` | Subclass utility | ⚠️ Protected — exposes `ExecutionContext` with raw `WebDriver` |
| `getDriver()` | Subclass utility | ⚠️ Protected — leaks `WebDriver` type |

### 1.2 What's Missing from the Façade

| Expected Session Operation | Present? | Current Workaround |
|----------------------------|----------|-------------------|
| `navigateTo(url)` | ❌ | `engine.navigateTo(url)` or `interaction().navigateTo(url)` |
| `getCurrentUrl()` | ❌ | `engine.getCurrentUrl()` |
| `run(Action)` | ❌ | Must create `FlowExecutor` manually or wrap in `Flow.of(action)` |
| `getTitle()` | ❌ | `engine.getTitle()` or native driver call |
| `refresh()` | ❌ | `engine.refresh()` or native driver call |
| `back()` / `forward()` | ❌ | Native driver call only |

### 1.3 FlowExecutor Visibility

| Aspect | Current State | Target State |
|--------|---------------|--------------|
| Class visibility | `public` | `public` (unchanged — internal consumers exist) |
| Constructor | `public FlowExecutor(UIEngine)` | Internal to `VOID` — users never construct |
| Direct usage by tests | Possible and documented in Javadoc | Discouraged — `VOID.run()` preferred |
| `VOID` integration | `VOID.run(Flow)` delegates to internal executor | Add `VOID.run(Action)` to complete coverage |

---

## 2. Identified Issues

### Critical

| # | Issue | Location | Impact |
|---|-------|----------|--------|
| F1 | `VOID` lacks session-level navigation methods — forces users to call `engine.navigateTo()` directly | `VOID.java` | **Façade becomes optional**; tests couple to `UIEngine` for basic navigation |
| F2 | No `run(Action)` on the façade — single-action execution requires wrapping in `Flow.of()` or constructing a `FlowExecutor` | `VOID.java` | **Pipeline bypass**; users create their own executors |
| F3 | `interaction()` returns a fully Selenium-coupled 833-line class as the primary interaction surface | `VOID.java:160-165` | **Façade delegates to legacy**; contradicts engine-agnostic goal |

### High

| # | Issue | Location | Impact |
|---|-------|----------|--------|
| F4 | `shutdown()` calls `DriverManager.quitAll()` but does not call `engine.shutdown()` | `VOID.java:130-133` | Engine-owned resources (connections, processes) may leak |
| F5 | `FlowExecutor` is publicly constructible — tests can bypass the session entirely | `FlowExecutor.java:25` | Multiple executors per session; lifecycle ownership diluted |
| F6 | `getDriver()` protected accessor leaks `WebDriver` type signature into subclass contracts | `VOID.java:151-153` | Subclasses become Selenium-coupled; engine swap breaks inheritance hierarchy |
| F7 | Multi-session test pattern still requires manual `FlowExecutor` wiring per the existing documentation | `FlowExecutor.java` Javadoc | Users associate execution with executor rather than session |

### Medium

| # | Issue | Location | Impact |
|---|-------|----------|--------|
| F8 | No `getCurrentUrl()`, `getTitle()`, `refresh()`, `back()`, `forward()` on façade | `VOID.java` | Session-level browser state queries require engine access |
| F9 | Javadoc layer model in `VOID.java` still references `VOID → Interactions` as the primary path | `VOID.java:42-48` | Documentation contradicts the target architecture |
| F10 | No logging/tracing when `run(Flow)` executes — executor is silent | `FlowExecutor.java:34-37` | No session-level observability for flow execution |

---

## 3. Compliance Matrix — Design Principles

| Principle | Current Compliance | Gap |
|-----------|-------------------|-----|
| **P1 — VOID represents a Session** | Partial — owns `ExecutionContext`, `UIEngine`, `FlowExecutor`; does not own full lifecycle (`shutdown` incomplete) | `shutdown()` must call `engine.shutdown()`; session identity should be explicit |
| **P2 — FlowExecutor becomes internal** | Partial — `VOID.run(Flow)` exists; `VOID.run(Action)` missing; `FlowExecutor` still publicly constructible | Add `run(Action)`; consider package-private constructor or factory restriction |
| **P3 — Expose Session-Level Operations** | Not met — no navigation, URL, title, refresh methods on façade | Implement thin delegation methods |
| **P4 — Keep Execution Primitives Off Façade** | Met — no `click()`, `type()`, `resolve()` on `VOID` | ✅ Correctly maintained |
| **P5 — Preserve Escape Hatch** | Met — `getEngine()` is public | ✅ Present; needs documentation as advanced API |

---

## 4. Multi-Session Analysis

### Current Pattern (observed)

```java
VOID admin = VOID.start();
VOID customer = VOID.start();

// Users forced to either:
// Option A — access engine directly
admin.getEngine().navigateTo(adminUrl);
customer.getEngine().navigateTo(customerUrl);

// Option B — use legacy Interactions
admin.interaction().navigateTo(adminUrl);

// Option C — create external executors (anti-pattern)
FlowExecutor adminExec = new FlowExecutor(admin.getEngine());
adminExec.run(loginFlow);
```

### Target Pattern (after façade strengthening)

```java
VOID admin = VOID.start();
VOID customer = VOID.start();

admin.navigateTo(adminUrl);
admin.run(loginFlow);

customer.navigateTo(customerUrl);
customer.run(customerFlow);

admin.run(approveFlow);
customer.run(verifyFlow);

admin.shutdown();
customer.shutdown();
```

### Gap Assessment

| Capability | Available Today | Required Change |
|------------|----------------|-----------------|
| Independent sessions | ✅ Multiple `VOID.start()` works | None |
| Per-session navigation | ❌ Requires `getEngine()` bypass | Add `navigateTo()` to façade |
| Per-session flow execution | ✅ `run(Flow)` works per instance | None |
| Per-session action execution | ❌ `run(Action)` missing | Add method |
| Per-session URL assertion | ❌ Requires `getEngine()` bypass | Add `getCurrentUrl()` to façade |
| Independent shutdown | ⚠️ `shutdown()` calls `DriverManager.quitAll()` — affects ALL drivers on thread | Fix to session-scoped cleanup |

### Critical Multi-Session Bug

`VOID.shutdown()` calls `DriverManager.quitAll()` which quits **all drivers on the current thread**, not just the session's driver. In a multi-session scenario:

```java
admin.shutdown();   // ← kills customer's driver too
customer.run(flow); // ← NPE or SessionNotFoundException
```

This is a **session isolation violation** that must be fixed regardless of façade work.

---

## 5. Façade Surface Design — Recommended API

### Session Lifecycle

```java
public static VOID start()
public static VOID start(DriverFactory.Profile profile)
public void shutdown()
```

### Session-Level Navigation

```java
public void navigateTo(String url)
public String getCurrentUrl()
public String getTitle()
public void refresh()
public void back()
public void forward()
```

### Execution

```java
public void run(Flow flow)
public void run(Action action)
```

### Escape Hatch (advanced — documented as such)

```java
public UIEngine getEngine()
```

### Deprecated / Removal Candidates

```java
@Deprecated public Interactions interaction()   // → remove in next major
protected WebDriver getDriver()                 // → replace with engine-based access
```

### Explicitly Excluded (per Principle 4)

```java
// NEVER add to VOID:
// click(...), type(...), clear(...), waitForVisible(...), resolve(...)
// select(...), hover(...), dragAndDrop(...)
```

---

## 6. Implementation Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| Façade grows beyond session concerns | Medium | Violates P4, creates second `Interactions` | Strict method-addition policy: only browser-session-level ops |
| `interaction()` remains primary path despite deprecation | High | Dual-pipeline indefinitely | Remove from new test examples; deprecate with removal date |
| `FlowExecutor` continues to be used directly in tests | Medium | Session abstraction bypassed | Documentation + constructor visibility restriction |
| `shutdown()` session isolation bug causes flaky multi-session tests | High | Trust erosion in framework | Fix immediately — scope shutdown to session's own driver |
| Navigation methods create false equivalence with `Interactions` | Low | Confusion about where to call what | Clear Javadoc: "session ops here, element ops in Action/Flow" |

---

## 7. Recommended Execution Sequence

### Phase A — Fix Critical Session Issues (Immediate)

**Scope:** Session correctness and lifecycle  
**Changes:**
1. Fix `shutdown()` to quit only this session's driver (not `DriverManager.quitAll()`)
2. Add `engine.shutdown()` call in `VOID.shutdown()` before driver quit
3. Add `run(Action action)` to `VOID` — delegates to `executor.run(action)`

**Exit criteria:**
- Multi-session tests can start/stop independently
- Single actions can execute without `Flow.of()` wrapper

---

### Phase B — Promote Session-Level Operations (1–2 days)

**Scope:** Navigation and state-query delegation  
**Changes:**
1. Add `navigateTo(String url)` → `engine.navigateTo(url)`
2. Add `getCurrentUrl()` → `engine.getCurrentUrl()`
3. Add `getTitle()` → `engine.getTitle()`
4. Add `refresh()` → `engine.navigateTo(engine.getCurrentUrl())` or dedicated engine method
5. Add `back()` / `forward()` if `UIEngine` supports them (or defer)
6. Update Javadoc layer model to reflect new architecture

**Exit criteria:**
- Basic test can be written using only `VOID`, `Flow`, `Action`, `Element`
- No `getEngine()` calls required for standard navigation workflows

---

### Phase C — Deprecate Legacy Access (1 week)

**Scope:** Signal migration intent  
**Changes:**
1. Annotate `interaction()` with `@Deprecated(since = "2.1", forRemoval = true)`
2. Annotate `getDriver()` with `@Deprecated`
3. Update `FlowExecutor` Javadoc to discourage direct construction: _"Prefer `VOID.run()` for test-level execution"_
4. Mark `getEngine()` Javadoc as _"Advanced API — most tests should not need this"_

**Exit criteria:**
- IDE warnings surface for any new `interaction()` usage
- Documentation consistently promotes `VOID.run()` over direct executor/engine access

---

### Phase D — Enforce and Observe (Ongoing)

**Scope:** Governance  
**Changes:**
1. Add ArchUnit rule: _"Classes in `tests.*` should not directly instantiate `FlowExecutor`"_
2. Add ArchUnit rule: _"Classes in `tests.*` should not call `UIEngine` methods except via known escape-hatch patterns"_
3. Track `getEngine()` call frequency — if rising, investigate missing façade methods
4. Review new test code for direct engine interaction patterns monthly

**Exit criteria:**
- >90% of test classes interact only with `VOID`, `Flow`, `Action`, `Element`
- `getEngine()` usage limited to genuinely advanced scenarios (custom waits, native commands)

---

## 8. Success Metrics

| Metric | Current | Target | Measurement |
|--------|---------|--------|-------------|
| Test classes using only `VOID` + pipeline types | ~30% (estimated) | >90% | grep for `getEngine()` / `interaction()` in test sources |
| Direct `FlowExecutor` construction in tests | Present in examples | 0 | grep for `new FlowExecutor` in test sources |
| `interaction()` calls in new code | Active | 0 new usages | PR review + deprecation warnings |
| Multi-session tests that function correctly | Broken (quitAll bug) | 100% pass | CI suite |
| Façade method count | 5 public methods | 10–12 public methods | Manual count |
| `UIEngine` direct calls in test code | Common | Rare (<5% of test classes) | ArchUnit enforcement |

---

## 9. Verdict

> **The foundation is in place. The wiring is not.**

`VOID` already owns the right components (`ExecutionContext`, `UIEngine`, `FlowExecutor`) and has the correct `run(Flow)` delegation. But the façade has not yet promoted session-level operations to its surface, forcing users to bypass it for basic navigation and URL assertions.

The critical multi-session `shutdown()` bug (`quitAll` instead of session-scoped quit) is a correctness defect that blocks reliable multi-session testing regardless of façade design.

**Estimated effort to reach target state:**
- Phase A: 2–3 hours (critical fixes)
- Phase B: 3–4 hours (thin delegation methods + Javadoc)
- Phase C: 1–2 hours (annotations + doc updates)
- Phase D: Ongoing governance

**Total active development:** ~1 day of focused work.  
**Architectural payoff:** Tests become session-oriented; engine portability preserved; FlowExecutor hidden; multi-session patterns work correctly.

---

*Audit version 1.0 — 2026-05-29. Re-audit recommended after Phase B completion.*

---

## Related

- [011 — VOID as Primary Session Façade](../plan/ongoing/011-void-facade-boundary.md) — implementation plan derived from this audit

