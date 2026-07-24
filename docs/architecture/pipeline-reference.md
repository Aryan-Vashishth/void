# VOID Pipeline Reference

Complete map of runtime pipeline participants: hierarchy, dependencies, execution
sequence, and flowcharts. Generated from codebase analysis on 2026-07-23.

For the conceptual overview see `system-overview.md`. For action composition detail
see `actions.md`. For locator resolution see `locator-resolution.md`.

---

## Layer Map

```
┌─────────────────────────────────────────────────┐
│  TEST CODE                                      │
│  (page objects, step defs, test methods)        │
└────────────────────┬────────────────────────────┘
                     │ calls
┌────────────────────▼────────────────────────────┐
│  SESSION LAYER                                  │
│  VOID  ──owns──  SessionContext                 │
│  VOIDBuilder  ──builds──  VOID                  │
└──────┬──────────────────────────────────────────┘
       │ delegates run() to
┌──────▼──────────────────────────────────────────┐
│  EXECUTION LAYER                                │
│  FlowExecutor  ──iterates──  Flow               │
│  Flow  ──contains──  Action[]                   │
└──────┬──────────────────────────────────────────┘
       │ calls action.perform(engine)
┌──────▼──────────────────────────────────────────┐
│  ACTION LAYER                                   │
│  Action (interface)                             │
│    └─ ElementAction (abstract, template method) │
│         ├─ ClickableElementAction               │
│         │    ├─ ClickAction                     │
│         │    ├─ ToggleAction                    │
│         │    └─ HoverAction  ...                │
│         ├─ TypeableElementAction                │
│         │    ├─ TypeAction                      │
│         │    └─ ClearAction  ...                │
│         └─ SelectableElementAction              │
│              └─ SelectAction  ...               │
│  HookChainAction  (wraps any Action + hooks)    │
└──────┬──────────────────────────────────────────┘
       │ calls engine.click / type / getText / resolve
┌──────▼──────────────────────────────────────────┐
│  ENGINE LAYER                                   │
│  UIEngine  (interface -- single exec authority) │
│    └─ SeleniumEngine  (current implementation)  │
└──────┬──────────────────────────────────────────┘
       │ calls WebDriver methods
┌──────▼──────────────────────────────────────────┐
│  PLATFORM LAYER  (engine-private)               │
│  WebDriver  (Selenium)                          │
│  Page       (Playwright -- future)              │
└─────────────────────────────────────────────────┘
```

The platform layer is **engine-private**: `WebDriver` and `Page` never appear
outside `SeleniumEngine` internals. No neutral kernel type references them.

---

## Execution Sequence

Annotated trace for a single `Flow.of(click(LoginPage.SUBMIT))` call.

```
Test code
  │
  ├─ VOID.start()
  │     VOIDBuilder
  │       ├─ FrameworkBootstrap.init()           load driver.properties, utils config
  │       ├─ UIEngineFactory.create()
  │       │     └─ SeleniumEngine.initialize(EngineConfig)
  │       │           └─ WebDriver instantiated here (engine-private)
  │       └─ new SessionContext(config, engine)
  │
  ├─ app.run( Flow.of( click(LoginPage.SUBMIT) ) )
  │     FlowExecutor.run(flow)
  │       └─ for each Action:
  │
  │           [without hooks]
  │           ElementAction.perform(engine)
  │             ├─ resolve(engine)
  │             │     engine.resolve(element, role)
  │             │       └─ LocatorResolver
  │             │             ├─ load template from file / properties
  │             │             ├─ format args
  │             │             └─ return LocatorDescriptor
  │             └─ execute(engine, descriptor)
  │                   └─ engine.click(descriptor)
  │                         └─ WebDriver.findElement(By...).click()
  │
  │           [with hooks -- .before() / .after() / .using()]
  │           HookChainAction.perform(engine)
  │             ├─ delegate.resolve(engine)       LocatorDescriptor (shared across hooks)
  │             ├─ before hooks: hook.execute(engine, descriptor)
  │             ├─ delegate.perform(engine)        actual action
  │             ├─ after hooks:  hook.execute(engine, descriptor)
  │             └─ emit ActionTrace
  │
  └─ app.shutdown()
        engine.shutdown() → WebDriver.quit()
```

---

## Action Type Hierarchy

```
Action  (interface)
  └─ ElementAction  (abstract -- template method: resolve → execute)
       ├─ ClickableElementAction  (abstract -- shared click profiles)
       │    ├─ ClickAction
       │    ├─ ToggleAction
       │    ├─ CheckAction
       │    └─ HoverAction
       ├─ TypeableElementAction  (abstract -- shared type profiles)
       │    ├─ TypeAction
       │    ├─ AppendTypeAction
       │    ├─ ClearAction
       │    ├─ TypeAndPressAction
       │    ├─ TypeSearchAction
       │    └─ SubmitSearchAction
       ├─ SelectableElementAction  (abstract -- shared select profiles)
       │    ├─ SelectAction
       │    ├─ SelectByTextAction
       │    ├─ SelectByValueAction
       │    ├─ SearchAndSelectAction
       │    └─ OpenAction
       ├─ ReadTextAction
       └─ UploadAction

HookChainAction  (final -- wraps any Action with before/after pipeline)
```

---

## Participant Reference

| Participant | Package | Type | Role |
|---|---|---|---|
| `VOID` | `core.runtime` | class | Session facade; primary test entry point |
| `VOIDBuilder` | `core.runtime` | class | Fluent builder; wires engine and context |
| `SessionContext` | `core.context` | class | Immutable per-session config and engine holder |
| `FrameworkBootstrap` | `core.bootstrap` | class | One-time JVM init; loads config files |
| `UIEngineFactory` | `core.engine` | class | Instantiates the correct UIEngine implementation |
| `EngineBootstrap` | `core.engine` | sealed interface | Opaque init token passed from builder to factory |
| `EngineConfig` | `core.engine` | class | Engine-agnostic settings (timeouts, baseUrl, polling) |
| `UIEngine` | `core.engine` | interface | Single execution authority over the browser |
| `SeleniumEngine` | `core.engine` | class | Selenium implementation of UIEngine |
| `FlowExecutor` | `core.executor` | class | Iterates a Flow and executes each Action |
| `Flow` | `core.flow` | class | Immutable ordered sequence of Actions |
| `Action` | `core.actions` | interface | Deferred UI operation; receives engine at perform time |
| `ElementAction` | `core.actions` | abstract class | Template method: resolve locator then execute |
| `HookChainAction` | `core.actions` | final class | Wraps any Action with a before/after hook pipeline |
| `ClickAction` etc. | `core.actions` | concrete classes | Leaf operations that delegate to UIEngine methods |
| `Target` | `core.target` | interface | Domain-neutral root: display text, args, effective-args |
| `UIElement` | `elements.api` | interface | Page object descriptor: locator keys, roles, external file; extends `Target` |
| `ElementRole` | `elements.meta` | enum | Locator slot selector (PRIMARY, SECONDARY, ...) |
| `LocatorDescriptor` | `core.engine` | record | Engine-agnostic resolved locator passed to UIEngine |
| `LocatorResolver` | `core.resolvers` | class | Loads, formats, and parses locator templates |
| `ActionHandler` | `core.interactions.hooks` | interface | Hook function: `execute(engine, descriptor)` |
| `ActionTrace` | `core.actions.trace` | class | Immutable execution record for observability |
| `Interactions` | `core.interactions` | class | Legacy orchestrator (frozen; BDD path only) |
| `VoidDSL` | `dsl` | record | BDD bridge; translates plain-text params to enums |
| `WebDriver` | Selenium (external) | interface | Raw browser automation client (engine-private) |

---

## Direct Dependency Map

| Participant | Depends on |
|---|---|
| `VOID` | `SessionContext`, `UIEngine`, `FlowExecutor` |
| `VOIDBuilder` | `FrameworkBootstrap`, `SessionContext`, `UIEngineFactory`, `EngineBootstrap`, `UIEngine`, `DriverFactory.Profile` |
| `SessionContext` | `UIEngine`, `Properties` |
| `UIEngineFactory` | `UIEngine`, `SeleniumEngine`, `EngineBootstrap`, `EngineConfig`, `Properties` |
| `UIEngine` | `LocatorDescriptor`, `EngineConfig`, `UIElement`, `ElementRole` |
| `FlowExecutor` | `UIEngine`, `Action`, `Flow` |
| `Flow` | `Action[]` |
| `Action` | `UIEngine`, `LocatorDescriptor`, `ActionCapability`, `ActionHandler`, `ActionProfile` |
| `ElementAction` | `UIElement`, `ElementRole`, `ActionCapability`, `UIEngine`, `LocatorDescriptor` |
| `HookChainAction` | `Action`, `ActionHandler[]`, `LocatorDescriptor`, `ActionTrace` |
| `UIElement` | `Target` |
| `LocatorResolver` | `LocatorSourceRegistry`, `LocatorTemplate.Policy`, `ByParser`, `UIElement`, `ElementRole`, `LocatorDescriptor` |
| `Interactions` | `UIEngine`, `LocatorResolver`, `UIElement`, `ElementRole`, `ActionHandler` |
| `VoidDSL` | `Interactions`, `EnumClassRegistry`, `UIElement`, `ResolvableEnum`, `ActionHandler` |

---

## Key Invariants

- **`UIEngine` is the single execution authority.** No type outside a `UIEngine`
  implementation calls `WebDriver` methods directly. (ADR-007)
- **Platform types are engine-private.** `WebDriver`, Playwright `Page`, and equivalent
  platform clients never appear in `core.runtime`, `core.actions`, `core.flow`,
  `core.executor`, or `dsl`. (ADR-018)
- **`FlowExecutor` does not resolve locators.** Resolution is `ElementAction`'s
  responsibility, delegated through `UIEngine.resolve()`.
- **`HookChainAction` resolves the locator once.** The descriptor is shared across
  all before hooks, the delegate action, and all after hooks -- no redundant resolution.
- **`Interactions` is frozen.** It is preserved for BDD backward compatibility only;
  no new call sites should reference it.
