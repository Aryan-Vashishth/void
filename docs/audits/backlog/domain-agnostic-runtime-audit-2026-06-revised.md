# VOID Architecture Audit — Revised Assessment
## Challenge Response to Initial Audit (domain-agnostic-runtime-audit-2026-06.md)

**Date:** 2026-06-16
**Branch:** Updated codebase (post-initial-audit)
**Premise:** The initial audit concluded B — UI Automation Runtime. This document challenges that conclusion by requiring each finding to be classified as structural coupling, transitional coupling, implementation coupling, or intentional domain vocabulary — and by distinguishing target architecture from current implementation state.

---

## What Changed on This Branch

Before addressing the challenge, the new code must be accounted for. Three significant additions:

**`SessionContext`** (`core.context.SessionContext`) — replaces `ExecutionContext`. Holds `UIEngine` instead of `WebDriver`. Its Javadoc is a design statement: *"ExecutionContext holds a raw WebDriver — Selenium-coupled. This class holds a UIEngine, enabling Playwright or any future engine."* This is the migration step that directly addresses the `ExecutionContext` coupling identified in the initial audit. `VOID.java` still uses `ExecutionContext` on this branch — the migration is underway, not complete.

**`ActionProfile` / `ActionProfiles` / `Profiles`** — a named, composable hook-bundle system applied to Actions. Profiles can be capability-aware (`action.capability()`) and are config-driven via `void.profile.default`. The mechanism is domain-agnostic. The built-in profile names (`SAFE`, `RELIABLE`, etc.) are UI-domain presets.

**`ActionCapability`** enum — `CLICKABLE`, `TYPEABLE`, `SELECTABLE`, `UNKNOWN`. Runtime-level capability classification used for profile dispatch. Current values are UI terms. The enum is closed, which is the same extensibility concern as `LocatorStrategy`.

---

## Challenged Assumption 1: Magnitude of `Action.perform(UIEngine)`

**Original claim:** This pin proves the runtime is fundamentally UI-specific.

**Is the claim accurate?**

The execution path after this pin is resolved is:

```
FlowExecutor.run(flow) {
    for (Action action : flow.getActions()) {
        action.perform(engine);   // only line that references UIEngine
    }
}
```

That is the entire runtime dispatch logic. There is no execution logic inside `FlowExecutor` that depends on UIEngine's methods. The engine is passed to `action.perform()` and the action decides what to call. `FlowExecutor` itself contains zero UI semantics.

The change needed to generalize this:

```
1. New file:  interface Engine { void initialize(EngineConfig); void shutdown(); }
2. One line:  UIEngine extends Engine
3. One line:  Action.perform(Engine engine)
4. One line:  FlowExecutor(Engine engine)
5. One line:  HookedAction.perform(Engine engine)
6. One line:  ActionHandler.execute(Engine engine, LocatorDescriptor descriptor)
```

No execution logic changes. No flow composition changes. No hook orchestration changes. No locator resolution changes. The change is entirely in type boundaries.

**Revised classification:** `Action.perform(UIEngine)` is **Transitional Coupling** — the correct abstraction is visible one step away. Calling it structural overclaims the magnitude.

The initial verdict was wrong about the nature of this pin. It is a type boundary that has not yet been generalized, not evidence that the runtime is designed for UI only.

---

## Challenged Assumption 2: Classification

**Original claim:** B — UI Automation Runtime.

**Challenge:** Evaluate whether the runtime components are independent of the UI domain.

**`Flow`** — pure `List<Action>`. No coupling to any domain. Unchanged across any engine swap.

**`FlowExecutor.run(flow)`** — iterates actions and calls `perform(engine)`. The logic is a for-loop. Zero domain semantics. Change one type signature and it executes robot actions equally.

**`HookChainAction`** — collects before/after hooks and orchestrates their execution order. Contains zero UI logic. The orchestration: run before-hooks, run delegate, run after-hooks. This model is domain-neutral.

**`ActionProfile`** — a composable hook-bundle interface. The interface itself is domain-agnostic. The built-in presets in `Profiles` happen to include Angular/CDK-specific hooks because the current domain is a web application. An `AgentProfiles` or `RobotProfiles` class could be written without touching the `ActionProfile` interface.

**`LocatorDescriptor`** — no Selenium imports. A record holding a string and a strategy enum. Domain-neutral in form.

**`LocatorSourceRegistry` + `LocatorTemplate`** — pure string lookup and template formatting. The concept of "find a locator by key in a file" is not web-specific. A robot arm's joint coordinates could live in the same system.

**What the runtime owns that is not domain-specific:**
- Deferred execution (Action)
- Sequential composition (Flow)
- Dispatch (FlowExecutor)
- Hook orchestration (HookChainAction)
- Cross-cutting behavior (ActionProfile, Profiles)
- Configurable defaults (ActionProfiles via `void.profile.default`)
- Location abstraction (LocatorDescriptor, LocatorSourceRegistry)

**What is domain-specific and correctly so:**
- `UIEngine` methods (`click`, `type`, `hover`, `waitForOverlay`, etc.)
- `SeleniumEngine` implementation
- `Before.WAIT_FOR_ANGULAR_LOADER` with `app-loader` CSS
- Capability interfaces (`Clickable`, `Typeable`, etc.)

**What is domain-specific and incorrectly placed:**
- `Action.perform(UIEngine)` — should be `perform(Engine)` where `UIEngine extends Engine`
- `ActionCapability` closed enum — should be extensible for new domains
- `LocatorStrategy` closed enum — same concern

**Revised classification:** **C — Runtime Migrating Toward Domain Agnosticism**

The runtime model — deferred execution, compositional flows, lifecycle management, configurable hook profiles, descriptor-based location — is architecturally correct for multi-domain use. The type pins binding it to `UIEngine` are a layer that has not yet been generalized. `SessionContext` existing on this branch proves the migration is active.

The distinction from B: a UI runtime is *designed* for UI and would require rethinking execution semantics for another domain. VOID would require changing type boundaries. These are not the same thing.

---

## Reclassified Findings

Every major finding from the initial audit, classified using the four categories.

| Finding | Initial Classification | Revised Classification | Reasoning |
|---|---|---|---|
| `Action.perform(UIEngine)` | Structural | **Transitional** | Six type-signature changes; zero logic changes |
| `ActionHandler.execute(UIEngine, ...)` | Not classified | **Transitional** | Same analysis; hook interface is structurally sound |
| `FlowExecutor(UIEngine)` | Structural | **Transitional** | Constructor type pin only; dispatch logic is domain-neutral |
| `HookedAction.perform(UIEngine)` | Not classified | **Transitional** | Same pin; orchestration logic has zero UI semantics |
| `ExecutionContext` holds `WebDriver` | Structural | **Transitional** | `SessionContext` replacing it exists on this branch |
| `VOID.start()` creates WebDriver unconditionally | Structural | **Transitional** | `SessionContext` not yet wired; migration step is visible |
| `UIEngineFactory.create(Properties, WebDriver)` | Structural | **Transitional** | Comment in code: "ignored by other engines"; config/env resolution already in place |
| `ByParser` in resolver producing `By` | Structural | **Transitional** | `resolveDescriptor()` path exists and is the target; old path has explicit migration path |
| `LocatorResolver.resolve()` returns `By` | Structural | **Transitional** | Deprecated path coexisting with `resolveDescriptor()`; migration incomplete, not abandoned |
| `core.utils.web.*` in core | Structural | **Implementation Coupling** | Domain-layer utilities in the wrong package; does not affect runtime contracts |
| `LocatorStrategy` closed enum (XPATH, CSS, ID, NAME) | Structural | **Implementation Coupling** | Mild; adding a new domain requires new values, not redesign of the resolution pipeline |
| `ActionCapability` closed enum (NEW on this branch) | — | **Implementation Coupling** | Same concern as `LocatorStrategy`; profile dispatch needs extensibility for new domains |
| `UIEngine.waitForOverlay(CDK CSS)` | Not classified | **Implementation Coupling / Design Debt** | Application-level selector in engine contract; not transitional — this is accumulated design debt that needs active management |
| `Before.WAIT_FOR_ANGULAR_LOADER` with `app-loader` CSS | Not classified | **Implementation Coupling** | Application constant in framework hook library; wrong layer but isolated |
| `WaitUtils.ANGULAR_LOADER = By.tagName("app-loader")` | Not classified | **Implementation Coupling** | Same; framework class carrying application-specific selectors |
| Capability interfaces (`Clickable`, `Typeable`, etc.) | Implementation Coupling | **Intentional Domain Vocabulary** | These ARE the domain; runtime never inspects them |
| `SeleniumEngine` using `By`, `WebDriver`, `WebElement` | N/A | **Intentional Domain Vocabulary** | Correctly isolated to implementation class |
| `EngineConfig` (Properties + Duration) | N/A | **Not coupling** | Correctly domain-agnostic |
| `LocatorDescriptor` record | N/A | **Not coupling** | No Selenium imports; engine-agnostic |
| `ActionProfile` interface | N/A | **Not coupling** | Domain-agnostic mechanism; built-in presets are UI-domain but the mechanism is not |

---

## Where the Initial Audit Was Wrong

**Wrong: calling type pins structural.**

The initial audit treated `Action.perform(UIEngine)` as evidence the runtime is fundamentally UI-specific. That is incorrect. A pin is not a design constraint — it is an unfinished generalization. The execution logic that would run under `Action.perform(Engine)` is identical to what runs today. There is no semantic change.

**Wrong: not accounting for migration state.**

The initial audit evaluated coupling without separating "target architecture" from "current implementation." `SessionContext` on this branch is direct evidence that the migration is active, intentional, and structurally progressing. The initial audit would have classified `ExecutionContext` as structural when its replacement already exists in the same codebase.

**Wrong: underweighting the locator abstraction pipeline.**

`LocatorDescriptor`, `LocatorSourceRegistry`, `LocatorTemplate`, and the `resolveDescriptor()` path represent a functioning, domain-agnostic location infrastructure. Any domain can express "find my target by key in a file." This is real architectural work that substantially reduces the cost of adding a second domain. The initial audit mentioned this but did not give it appropriate weight.

---

## Where the Initial Audit Was Correct

**The `UIEngine` contract problem is real.**

`UIEngine.waitForOverlay(Duration)` with its Angular CDK CSS selector embedded in `SeleniumEngine` is a real design concern — not because it blocks domain-agnosticism (it doesn't), but because engine contracts that accumulate application-level semantics drift away from being clean runtime contracts. If every Angular-specific wait pattern gets added to `UIEngine`, the interface becomes unusable as a generalization point. This requires active discipline, not a one-time migration.

**`ActionCapability` is a new closed-enum problem.**

The initial audit identified `LocatorStrategy` as a closed enum that can't be extended by new domains. This branch adds `ActionCapability` (CLICKABLE, TYPEABLE, SELECTABLE, UNKNOWN) with the same problem. The profile dispatch system (`Profiles.SAFE` switching on `capability()`) will need to be extended or overridden for non-UI domains. This is not a blocker — it is an architectural note that the pattern being introduced should be made extensible before it hardens.

**Perspective A vs Perspective B are different questions.**

The initial audit conflated them. They remain different:

- **Perspective A — today:** A `RobotEngine` cannot be introduced without changing type signatures. This is true on this branch as it was on the previous branch.
- **Perspective B — migration cost:** The changes required are: introduce `Engine`, update ~8 type signatures, wire `SessionContext` into `VOID.start()`, remove `WebDriver` from factory signature, complete the `resolveDescriptor()` migration. This is a week of mechanical refactoring for a disciplined developer. It is not a redesign.

Both answers are accurate. Neither cancels the other.

---

## Architectural Readiness Scores

Scores reflect **current implementation state**, not theoretical potential.

| Area | Score | Basis |
|---|---|---|
| **Runtime Abstraction** | 7/10 | Action/Flow/FlowExecutor/HookChainAction/ActionProfile are domain-neutral in structure; type pins are the only deduction |
| **Domain Separation** | 8/10 | Element/Capabilities correctly local to domain; LocatorDescriptor domain-agnostic; SeleniumEngine correctly isolated; deduction for UIEngine mixing runtime and domain contract |
| **Engine Agnosticism** | 6/10 | UIEngine is an interface (good); factory has config/env engine selection (good); SessionContext replaces WebDriver context (good, not wired); VOID.start() still creates WebDriver unconditionally (active blocker) |
| **Externalization Readiness** | 7/10 | JSON/properties locator files working; config-driven profiles via `void.profile.default`; profile builder API; ActionProfile composable externally; no external flow schema yet |
| **LLM Readiness** | 4/10 | Declarative flow model structurally compatible with LLM generation; external locator files LLM-maintainable; no schema, no intent-to-action mapping, nothing wired |
| **Multi-Domain Readiness** | 3/10 | Target architecture visible and correct; type pins block second domain today; ActionCapability and LocatorStrategy are closed enums; changes needed are mechanical but not yet made |

The low multi-domain score is a current-state score. The architectural ceiling is significantly higher — the same assessment run after the `Engine` supertype is introduced and `SessionContext` is wired would score 7-8/10.

---

## Most Important Question

**Is VOID fundamentally blocked by architectural design, or primarily waiting for migration completion?**

**Waiting for migration completion.**

The architectural design is correct. The execution model — deferred action emission, compositional flows, configurable hook profiles, descriptor-based location, engine-delegated dispatch — is domain-agnostic in structure. None of the runtime components contain UI logic. The type pins bind these components to `UIEngine` but do not change their semantics.

**Evidence that the blocks are transitional, not structural:**

1. `SessionContext` was written on this branch specifically to replace the WebDriver-coupled `ExecutionContext`. The migration step exists; it just hasn't been wired into `VOID.start()` yet.

2. The factory (`UIEngineFactory`) already reads the engine name from config, system properties, and environment variables. The infrastructure for switching engines is in place. The `WebDriver` parameter is acknowledged as wrong in its own Javadoc (`"ignored by other engines"`).

3. `resolveDescriptor()` exists alongside `resolve()` returning `By`. The target is implemented; the old path is not yet removed. This is a migration, not a missing design.

4. `// case "playwright" -> new PlaywrightEngine(); // Phase 3` in `UIEngineFactory` is an explicit roadmap comment — the path for a second engine is already mapped.

5. The profile system (`ActionProfile`, `Profiles`) was built as an interface-based mechanism, not hardcoded behavior. A `RobotProfiles` class could be written without touching `ActionProfile`. The built-in presets are domain-specific; the mechanism is not.

**What a genuine structural block looks like:**

A structural block would mean the execution model is wrong for domain-agnosticism — that the very act of defining "an action performs against an engine" is the problem, or that Flow composition assumes sequential state that is UI-specific, or that the hook system fundamentally requires browser DOM semantics. None of these are true. The model is architecturally sound.

**The remaining migration work:**

| Task | Magnitude |
|---|---|
| Introduce abstract `Engine` interface | Small — 1 new file |
| `UIEngine extends Engine` | Small — 1 line |
| Update `Action`, `FlowExecutor`, `HookedAction`, `ActionHandler` type signatures | Small — 4-6 lines across 4 files |
| Wire `SessionContext` into `VOID.start()` | Medium — replaces `ExecutionContext` construction |
| Remove `WebDriver` from `UIEngineFactory.create()` | Medium — each engine creates its own connection |
| Complete `resolveDescriptor()` migration, remove `By`-returning paths | Medium — delete deprecated methods, fix callers |
| Make `ActionCapability` extensible | Small — convert enum to interface with default values |
| Make `LocatorStrategy` extensible | Small — same pattern |

The total scope is a focused refactoring sprint, not an architectural redesign. Every piece of the resulting architecture already exists in the current codebase — the framework just needs the connective tissue.

---

## Revised Verdict

**C — Runtime Migrating Toward Domain Agnosticism**

The runtime model is structurally correct for multi-domain use. The current implementation is Selenium-specific because Selenium is the only domain that has been built. The migration toward a generalized engine boundary is active, evidenced by `SessionContext`, the profile system, the `resolveDescriptor()` path, and the factory's config-based engine selection.

VOID is not a UI framework that happens to have some clean abstractions. It is a runtime whose abstractions are mostly correct and whose current implementation is UI-specific by necessity, not by design.

The migration debt is real and needs a clear completion path. The target architecture is not speculative — it is visible in the current code.

---

*Assessment against commit on updated branch. Specific source references available in companion audit.*