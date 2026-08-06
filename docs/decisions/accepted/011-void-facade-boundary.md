# 011 — VOID as Primary Session Façade

**Date:** 2026-05-29  
**Status:** Implemented — 2026-05-29

---

## Context

With the introduction of `UIEngine` (ADR-007) and the Action/Flow/FlowExecutor pipeline (ADR-009), VOID has a clean, engine-agnostic execution model. However, the `VOID` class has not evolved to reflect its role as the **primary session object**. Test authors increasingly bypass the façade:

```java
// Direct engine access for basic navigation
engine.navigateTo(url);
engine.getCurrentUrl();

// Manual executor construction
FlowExecutor executor = new FlowExecutor(engine);
executor.run(flow);

// Legacy Interactions path
app.interaction().clickOn(element);
```

This creates three problems:

1. **Façade becomes optional** — examples couple directly to `UIEngine` for session-level operations that should live on `VOID`.
2. **FlowExecutor leaks** — test authors construct their own executors, diluting session ownership and lifecycle control.
3. **Multi-session isolation is broken** — `VOID.shutdown()` calls `DriverManager.quitAll()`, killing all drivers on the thread rather than just the session's own driver.

The Façade Boundary Audit (2026-05-29) identified these gaps and rated F1–F3 as critical.

---

## Decision

Establish `VOID` as the **primary session abstraction** by:

1. **Promoting session-level operations** to the façade surface (navigation, URL, title, refresh).
2. **Internalising FlowExecutor** — users execute via `VOID.run()`, never constructing executors directly.
3. **Adding `run(Action)`** alongside the existing `run(Flow)`.
4. **Fixing session-scoped shutdown** — quit only this session's driver, call `engine.shutdown()`.
5. **Deprecating legacy access** — `interaction()` and `getDriver()` marked for removal.
6. **Preserving the escape hatch** — `getEngine()` remains public, documented as advanced API.

### Target Façade Surface

```java
// Lifecycle
public static VOID start()
public static VOID start(DriverFactory.Profile profile)
public void shutdown()

// Session-level navigation
public void navigateTo(String url)
public String getCurrentUrl()
public String getTitle()
public void refresh()

// Execution
public void run(Flow flow)
public void run(Action action)

// Escape hatch (advanced)
public UIEngine getEngine()
```

### Explicitly Excluded (never add to VOID)

```java
// Element-level execution primitives — belong to Action/Flow pipeline
click(...), type(...), clear(...), select(...)
waitForVisible(...), resolve(...), hover(...), dragAndDrop(...)
```

---

## Reasoning

1. **Session-first mental model** — A test should think in terms of a session (`VOID`), not an engine. Navigation, URL queries, and lifecycle are session concerns. Element interactions are pipeline concerns.

2. **FlowExecutor is an implementation detail** — The executor pattern exists to iterate Actions against an engine. Tests should not know or care about this mechanism. `VOID.run(flow)` is the correct abstraction level.

3. **Multi-session correctness** — Each `VOID` instance represents an independent session. Shutdown must be session-scoped. The current `quitAll()` implementation violates session isolation.

4. **Façade stays small** — Only browser-session-level operations are promoted. Element interactions remain in the pipeline (`Element → Action → Flow → VOID.run()`). This prevents the façade from growing into a second `Interactions` class.

5. **Engine portability preserved** — All façade methods delegate to `UIEngine`. No Selenium types appear in the façade's public API. Engine swap remains transparent to test authors.

6. **Escape hatch prevents over-abstraction** — Advanced users who need custom waits, native commands, or engine-specific features can still access `getEngine()`. The boundary is documented, not enforced by hiding.

---

## Usage Example

### Before (current)

```java
VOID app = VOID.start();
UIEngine engine = app.getEngine();

engine.navigateTo("https://example.com/login");

FlowExecutor executor = new FlowExecutor(engine);
executor.run(Flow.of(
    LoginPage.USERNAME.type("admin"),
    LoginPage.PASSWORD.type("secret"),
    LoginPage.SUBMIT.click()
));

String url = engine.getCurrentUrl();
assertTrue(url.contains("/dashboard"));

app.shutdown(); // quits ALL drivers on thread
```

### After (target)

```java
VOID app = VOID.start();

app.navigateTo("https://example.com/login");

app.run(Flow.of(
    LoginPage.USERNAME.type("admin"),
    LoginPage.PASSWORD.type("secret"),
    LoginPage.SUBMIT.click()
));

assertTrue(app.getCurrentUrl().contains("/dashboard"));

app.shutdown(); // quits only this session's driver
```

### Multi-session (target)

```java
VOID admin = VOID.start();
VOID customer = VOID.start();

admin.navigateTo(adminUrl);
admin.run(loginAsAdminFlow);

customer.navigateTo(customerUrl);
customer.run(loginAsCustomerFlow);

admin.run(approveOrderFlow);
customer.run(verifyOrderApprovedFlow);

admin.shutdown();   // does NOT affect customer session
customer.shutdown();
```

---

## Consequences

### Architecture

- `VOID` becomes the **single entry point** for test-level code — examples import `VOID`, `Flow`, `Action`, and `Element` types only.
- `FlowExecutor` remains `public` (internal consumers exist) but is **not constructed by test authors** — documentation and ArchUnit rules discourage it.
- `interaction()` is deprecated with a removal target — new examples must not use it.
- `getDriver()` is deprecated — subclasses should use engine-level abstractions instead.

### Façade Growth Policy

To prevent `VOID` from becoming a god class:

- ✅ **Add:** browser-session-level operations (navigate, URL, title, cookies, window size)
- ❌ **Never add:** element-level operations (click, type, select, wait-for-element)
- ❌ **Never add:** resolution operations (resolve, find, locate)
- ⚠️ **Gate:** any new method must answer "Is this a session concern or an element concern?" — only session concerns qualify.

### Migration Path

| Current Pattern | Replacement | Timeline |
|----------------|-------------|----------|
| `engine.navigateTo(url)` | `app.navigateTo(url)` | Immediate |
| `engine.getCurrentUrl()` | `app.getCurrentUrl()` | Immediate |
| `new FlowExecutor(engine).run(flow)` | `app.run(flow)` | Immediate |
| `executor.run(action)` | `app.run(action)` | Immediate |
| `app.interaction().clickOn(...)` | `app.run(element.click())` | Deprecated → removed next major |
| `app.getDriver()` | `app.getEngine().getNativeDriver()` (escape hatch) | Deprecated → removed next major |

### Deprecations

| Method | Deprecated Since | Removal Target |
|--------|-----------------|----------------|
| `interaction()` | 2.1 | 3.0 |
| `getDriver()` | 2.1 | 3.0 |
| `getContext()` | 2.1 | 3.0 (replace with session-scoped config access) |

### Files Changed

- Modified: `VOID.java` — add navigation/execution methods, fix `shutdown()`, deprecate legacy accessors
- Modified: `FlowExecutor.java` — update Javadoc to note preference for `VOID.run()`
- New: ArchUnit rules enforcing façade-first patterns in test code

---

## Related

- [007 — UIEngine as Single Execution Authority](007-uiengine-execution-authority.md) — the engine `VOID` delegates to
- [009 — Action / Flow / FlowExecutor Execution Model](009-action-flow-runner.md) — the pipeline `VOID.run()` invokes
- [010 — Hook Evolution](010-hook-evolution.md) — hooks execute within the pipeline, not on the façade
- [Façade Boundary Audit](../../audits/facade-boundary-audit-2026-05.md) — the audit that motivated this decision

