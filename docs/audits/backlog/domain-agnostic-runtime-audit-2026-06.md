# VOID Domain-Agnostic Runtime Audit

**Date:** 2026-06-16 (three rounds) | **Consolidated:** 2026-07-20
**Branch evaluated:** `initiative/engine-decoupling`
**Rounds:** Initial (Verdict B), Revised (Verdict C), Stress-test (Verdict C+), Remediation synthesis
**Final verdict:** C+ -- Runtime Model Mostly Survives Domain Substitution

---

## Verdict

> **C+** -- Core runtime concepts survive domain substitution. Most remaining blockers are
> boundary abstractions. Migration path is visible and credible. Second domain cannot yet
> be added without implementation work. Architecture appears designed for generalization
> even if implementation is incomplete.

The runtime is an **unfinished domain-agnostic runtime**, not a UI runtime that appears
generalizable. The distinction matters: the abstractions were built for generalization and
are in migration, not retrofitted for appearance. Evidence:

- `SessionContext` holds `UIEngine`, not `WebDriver` -- written explicitly to decouple from
  browser lifecycle, with Javadoc explaining the predecessor it replaces.
- `resolveDescriptor()` was added alongside deprecated `resolve()` as the target resolution
  path -- migration behavior, not cosmetic.
- `// case "playwright" -> new PlaywrightEngine();` -- explicit future mapping in the factory.
- JSON locator files and `LocatorSourceRegistry` are overengineered for a single-domain tool
  and correctly engineered for a multi-domain platform.

---

## Domain Survival Analysis

Seven runtime primitives evaluated against UI, Robot, Agent, and Workflow domains under
a single-domain-per-session model (one VOID session hosts one engine domain).

| Component | Verdict | Reasoning |
|---|---|---|
| `Action` | **Survives** | "Deferred execution intent" is domain-agnostic. Under single-domain-per-session, actions never cross domain boundaries. |
| `Flow` | **Survives** | Pure sequential composition. Zero domain semantics. Unchanged across any engine. |
| `FlowExecutor` | **Survives** | A for-loop calling `action.perform(engine)`. After type-pin generalization, domain-neutral. |
| `HookChainAction` | **Survives** | Before/after hook orchestration. Zero domain semantics. The mechanism is agnostic; only hook implementations are domain-specific. |
| `ActionProfile` (interface) | **Survives** | Domain-neutral mechanism. A `RobotProfiles` or `AgentProfiles` can be written without touching the interface. Built-in presets are UI-domain assets, not runtime constraints. |
| `LocatorDescriptor` | **Evolves** | Structural pattern survives. Naming (`locator`, `parent`, `LocatorStrategy`) encodes browser DOM semantics. Migration path: rename `parent` to `scope`; extensible `LocatorStrategy`; eventual rename to `TargetDescriptor`. Not redesign -- evolution. |
| `ActionCapability` | **Evolves** | Capability-aware dispatch concept survives. The closed enum (`CLICKABLE`, `TYPEABLE`, `SELECTABLE`, `UNKNOWN`) does not. Must become an extensible open set (interface-with-constants). |

Six of seven runtime primitives survive conceptually. Neither evolving primitive requires
redesign. This is the primary evidence for C+ over C.

---

## What Cracks First When a Second Domain Is Introduced

Ranked by order of encounter during development:

| Rank | What Breaks | How It Breaks |
|---|---|---|
| 1 | `ActionCapability` closed enum | New domain capabilities don't exist in enum; `UNKNOWN` fallback silently applies browser wait hooks, producing wrong runtime behavior |
| 2 | `UIEngineFactory.create(Properties, WebDriver)` | Creating a non-Selenium engine requires passing a `WebDriver` with no meaning |
| 3 | `Action.perform(UIEngine)` type pin | Non-UI engine cannot be passed to `perform()` |
| 4 | `VOID.start()` unconditional WebDriver creation | Session startup always creates a browser; no entry point for non-browser domains |
| 5 | `LocatorStrategy` closed enum | Non-browser strategies cannot be declared |
| 6 | `LocatorDescriptor.parent` DOM scoping | Nested lookups model DOM containment; does not map to robot joints or agent registries |

`ActionCapability` is the most dangerous blocker: unlike the `UIEngine` type pin (fails
loudly at compile time), `ActionCapability.UNKNOWN` fails silently at runtime -- agent
actions receive browser wait hooks and time out with no obvious cause.

---

## Remediation Priorities

All findings from all three audit rounds, classified by remediation priority.

### Critical -- Must fix before further evolution

| Finding | File | Why Critical |
|---|---|---|
| `Action.perform(UIEngine)` type pin | `core/actions/Action.java` | Every future domain capability is blocked. All downstream generalization requires this first. |
| `ActionCapability` closed enum | `core/actions/ActionCapability.java` | Introduced on this branch as new code. Fix before it hardens -- after release costs 3x more. `Profiles.SAFE`/`RELIABLE` exhaustive switches are the immediate forcing function. |
| `ActionHandler.execute(UIEngine, LocatorDescriptor)` | `core/actions/hooks/ActionHandler.java` | Largest hidden surface area. Every `Before.*` / `After.*` constant is typed to `UIEngine`. Changing `Action.perform()` propagates here. |

### Important -- Fix within next few releases

| Finding | File | Why Important |
|---|---|---|
| No abstract `Engine` interface | n/a | Required for `UIEngine extends Engine`. Without it, Critical items cannot be resolved. One new file, zero risk. |
| `VOID.start()` unconditional WebDriver creation | `core/runtime/VOID.java` | Once Engine is generalized, this is the entry point that blocks non-browser sessions. |
| `UIEngineFactory.create(Properties, WebDriver)` signature | `core/engine/UIEngineFactory.java` | `WebDriver` in the parameter is wrong per its own Javadoc. Fix when Engine is generalized. |
| `LocatorStrategy` closed enum | `core/engine/LocatorStrategy.java` | Same extensibility problem as `ActionCapability` but lower severity. Fix when touching nearby code. |
| `ByParser` in resolver layer | `core/resolvers/locator/parser/ByParser.java` | Wrong layer for Selenium imports. Should be internal to `SeleniumEngine`. 10-minute cleanup. |
| `resolve()` returning `By` in `LocatorResolver` | `core/resolvers/locator/api/LocatorResolver.java` | Both `resolve()` and `resolveDescriptor()` coexist. Formally deprecate and schedule deletion. |
| `HookChainAction` / `HookedAction` type pins | `core/actions/HookChainAction.java`, `HookedAction.java` | Carry the UIEngine pin into hook orchestration. Update with `Action` type changes. |

### Opportunistic -- Fix when touching nearby code

| Finding | Why Opportunistic |
|---|---|
| `UIEngine.waitForOverlay(Duration)` CDK concept | Application-level concern in engine contract. Suppress with `@Deprecated` pointing to profile hook. |
| `Before.WAIT_FOR_ANGULAR_LOADER` in profile library | Application-level constant in framework. Move to test project custom hooks. |
| `WaitUtils.ANGULAR_LOADER = By.tagName("app-loader")` | Angular-specific selector in framework utility. Remove when touching WaitUtils. |
| `Interactions` legacy class | Already `@Deprecated(since="2.1", forRemoval)`. Do not add new methods. Leave for 3.0 removal. |

### Ignore

| Finding | Why Ignore |
|---|---|
| `Clickable`, `Typeable`, `Selectable` interface naming | Correct domain vocabulary for UI elements. Semantically accurate. |
| `SeleniumEngine` using `By`, `WebElement`, `WebDriver` | Correctly isolated to the implementation class. Not a framework concern. |
| Multi-domain flow coexistence | Premature. Single-domain-per-session is the correct initial model. Design later. |
| `LocatorDescriptor.parent` rename only | Low value for high cost; rename together with `LocatorStrategy` extensibility work (use `scope`). |

---

## LLM Readiness

### Implementation Readiness: 2/10

No LLM integration, no output schema, no code generator tooling. `JsonMigratorCli` exists
but does not consume LLM output. Every gap is tooling above the runtime, not a runtime change.

### Architectural Readiness: 8/10

| Pipeline Stage | Supporting Artifact | Status |
|---|---|---|
| Element discovery -> enum names | `Element` interface | Ready |
| Capability assignment | `Clickable`, `Typeable`, `Selectable` | Ready |
| Locator extraction -> JSON | `JsonLocatorReader`, `JsonLocatorSource` | Ready |
| Parameterized locators | `LocatorTemplate` with `%s` formatting | Ready |
| Migration CLI | `JsonMigratorCli` | Ready |
| Config-driven profile selection | `void.profile.default`, `ActionProfiles` | Ready |
| Flow execution of generated artifacts | `Flow.of(...)`, `app.run(flow)` | Ready |

The architecture does not need to change to support an AI generation pipeline. An LLM
generates a JSON locator asset and an enum implementing capability interfaces without any
Selenium knowledge. The runtime consumes both unchanged.

**The key structural property:** The descriptor-first, externalized-asset, capability-classified
design separates what an element IS (address and capability) from HOW it is executed.
AI systems generate knowledge; runtimes execute it. Traditional Selenium POM requires an
LLM to generate Java code with Selenium imports, XPath correctness, and driver lifecycle
awareness. VOID requires only key-value locator pairs and interface declarations. This
separation is rare and positions VOID for AI tooling in ways that POM frameworks cannot
easily retrofit.

---

## Locator Evolution

`LocatorDescriptor` is a transitional abstraction. The structural `(value, strategy, args, scope)`
pattern can hold any domain's target address. The conceptual model -- "locating" a target
by XPath/CSS/ID within a parent DOM element -- is browser-derived.

The `parent` field encodes DOM containment. A `GRIPPER` is not "within" an `ARM` the
way a `<button>` is within a `<div>`. The field name is wrong for non-UI domains.

**Rename `parent` to `scope` now.** `scope` expresses "find this descriptor within this
context" -- which holds for DOM nesting, robot subsystems, and hierarchical address spaces.
Affects `LocatorDescriptor`, `SeleniumEngine.toBy()`, and call sites of `isScoped()` /
`withParent()`. Do as part of `LocatorStrategy` extensibility work.

**Defer full rename to `TargetDescriptor` until a second domain exists.** A rename without
a second domain is premature abstraction that adds diff noise without delivering value.

---

## What Earlier Audit Rounds Got Wrong

### Overstated

**`LocatorDescriptor.parent` as a major concern.** All three rounds mentioned the DOM-centric
scoping model as a meaningful blocker. It is one field name. Until a second-domain developer
asks "what does parent mean for a robot joint?", this has zero practical effect.

**Multi-domain flow coexistence.** Round 3 raised "what happens when UI and robot actions
coexist in a single Flow." The single-domain-per-session model resolves this entirely. The
concern was answered correctly but raised unnecessarily.

**`ByParser` in the resolver layer.** All rounds called this significant coupling. It is a
two-method class -- a 10-minute package rename with no architectural consequence. Treated
as a meaningful design violation.

### Understated

**`ActionHandler.execute(UIEngine, LocatorDescriptor)` as the true propagation bottleneck.**
No round named this explicitly. When `Action.perform(UIEngine)` changes to `Action.perform(Engine)`,
every `Before.*` and `After.*` constant carries `UIEngine` in its lambda. The propagation
surface was underestimated by roughly 3x.

**`ActionCapability` severity.** Rounds 1 and 2 did not assess it. Round 3 classified it as
the next hidden bottleneck. None noted it was introduced on the branch under evaluation --
new code with a window to fix at zero migration cost that closes at release.

**`VOID.start()` session API mismatch for non-browser domains.** `navigateTo()`,
`getCurrentUrl()`, `getTitle()`, `refresh()` are browser navigation methods on the session
facade. A `RobotSession.start()` would return a different session type with different
top-level methods. `VOID` is currently both the session contract AND the browser navigation
API. These need separation before a second domain entry point is designed. No round named
this concern.

### Incorrect Assumptions

**"Six type signature changes cover the migration."** Actual count: `Action.java` (2 pins:
`perform`, `resolve`), `FlowExecutor.java` (1), `HookChainAction.java` (2), `HookedAction.java`
(2), `ElementBoundAction` inner class (2), `ActionHandler.java` (1) = 10+ pins, plus
`BiConsumer<UIEngine, LocatorDescriptor>` in `ElementActions.of()`. Six was a file count,
not a signature count.

**`ActionCapability` as "implementation coupling" with moderate severity.** `Profiles.SAFE`
and `Profiles.RELIABLE` both use exhaustive `switch` over `ActionCapability`. Two exhaustive
switches over a four-value closed enum, introduced on the branch being evaluated. This is a
structural lock-in being built in real time.

---

## Roadmap

### Next Release

| Change | Cost | Risk |
|---|---|---|
| Introduce `Engine` interface | 1 file, 5 lines | Zero -- additive |
| `UIEngine extends Engine` | 1 line | Zero -- backward compatible |
| `Action.perform(Engine)` + `Action.ui()` migration helper | ~10 type signatures | Low |
| `FlowExecutor(Engine)`, `HookChainAction(Engine)` | ~4 signatures | Low |
| `ActionCapability` enum -> interface-with-constants | Replace 12 lines | Low -- same names, identical behavior for existing capabilities |
| `Profiles.SAFE`/`RELIABLE` -> map-based dispatch | ~20 lines | Low -- output identical for existing capabilities |
| Wire `SessionContext` into `VOID.start()` | ~10 lines in VOID.java | Low -- SessionContext already tested |

Note on `ActionCapability`: replace exhaustive switch with `Map<ActionCapability, List<...>>`
dispatch. New domain capabilities then work without modifying framework code. `ActionCapability`
has no declared callers outside the framework's own `Profiles` and `ElementActions` classes --
no deprecation period needed for this pre-release code.

### Next 3 Releases

| Change | Cost | Risk |
|---|---|---|
| `LocatorStrategy` enum -> interface-with-constants | 1 file replace | Low |
| `LocatorDescriptor.parent` -> `scope` | Record field rename + ~10-20 call sites | Low |
| Formal deprecation of all `By`-returning paths in `LocatorResolver` | Add `@Deprecated` to ~5 methods | Zero |
| Move `ByParser` to `core.engine.selenium` | Package rename | Zero |
| `UIEngineFactory.create()` removes `WebDriver` parameter | ~15 lines | Medium |
| `ExecutionContext` `@Deprecated(forRemoval=true)` | 1 annotation | Zero |
| `UIEngine.waitForOverlay()` `@Deprecated` pointing to profile hook | 1 annotation | Zero |

### Long-Term

| Change | Cost |
|---|---|
| `BrowserSession extends VOID` -- separate browser navigation API from session contract | Medium refactor of VOID.java |
| Remove `By`-returning resolution paths | Delete ~5 methods after callers warned |
| Remove `ExecutionContext`, `HookedAction`, `Interactions` (3.0 removal) | Delete ~300 lines |
| `ActionHandler<E extends Engine>` generalization | Medium -- breaking for hook implementors |
| `void-ai-tools` module: LLM integration, JSON schema, code generator | High implementation, zero runtime risk |
| Publish `LocatorDescriptor` JSON schema for external tooling | Low |

---

*Supersedes: domain-agnostic-runtime-audit-2026-06-revised.md,
domain-agnostic-runtime-audit-2026-06-stress-test.md,
domain-agnostic-runtime-remediation-2026-06.md.
All source findings verified against branch `initiative/engine-decoupling`.*
