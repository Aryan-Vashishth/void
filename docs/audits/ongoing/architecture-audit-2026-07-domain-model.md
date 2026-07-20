# Architecture Audit 2026-07 -- Domain Model, Package Structure, Ubiquitous Language

Date: 2026-07-20
Branch audited: `feature/engine-decoupling` (post ADR-018/019/020 implementation, pre merge)
Scope: architectural domains, bounded contexts, package structure, runtime purity,
dependency direction, naming and vocabulary, long-term domain-neutrality readiness.
Method: System Overview plus verified import-level evidence from `src/main/java`,
cross-checked against `docs/decisions/`, `docs/audits/`, and `docs/plan/draft/`.

This document has two parts:

- **Part I -- Implementation Audit**: what the codebase is today (domains, contexts,
  packages, dependencies, vocabulary). Evidence-based.
- **Part II -- Architecture Ontology Review**: whether the five-concept conceptual
  model the redesign targets (Runtime, Interaction, Capability, Target, Domain) is
  complete, minimal, consistent, and extensible. Reasoned from first principles,
  independent of implementation.

---

# Part I -- Implementation Audit

This audit is descriptive. It proposes no renames, no package moves, and no designs.
It exists to ground the future domain-neutral runtime redesign. The initiative seeded
from it is `docs/plan/draft/runtime-kernel-boundary/`.

Finding IDs in this document use the `D` prefix (D1..D18) to avoid collision with the
C/H/M series in `architecture-audit-2026-05.md`.

---

## Executive Summary

**Architectural maturity: high. Architectural health: mixed. The gap between the two is
the story of this audit.**

VOID has unusually strong architectural process: 17 accepted ADRs plus 3 pending,
explicit stability tiers, written invariants, self-authored audits that honestly flag
debt, and a P-ID violation registry. The modern execution pipeline
(Element -> Action -> Flow -> FlowExecutor -> UIEngine) is designed, not accreted, and
is close to engine-agnostic today.

The central weakness is a **conflation of two different axes of neutrality**:

1. **Engine neutrality** (Selenium vs Playwright vs Appium, within UI automation).
   Nearly all current work targets this axis (ADR-018, 019, 020) and it is well advanced.
2. **Domain neutrality** (UI vs REST vs CLI vs Desktop). This is the stated long-term
   identity of VOID, and it barely exists in code. `UIEngine`, `LocatorDescriptor`,
   `ElementRole`, `ActionHandler(UIEngine, LocatorDescriptor)`, and the `VOID` facade
   itself (`navigateTo`, `getCurrentUrl`, `getTitle`) are engine-neutral but deeply
   UI-semantic.

The June 2026 domain-agnostic runtime audit (verdict C+) reached the same conclusion.
What exists today is a **well-governed UI automation runtime with a clean engine seam**,
packaged as a single monolithic artifact whose `core` package contains five or six
distinct architectural domains.

**Major strengths:** the deferred Action/Flow execution model; the LocatorDescriptor
abstraction; decision traceability; stability tiers; honest self-auditing; the
deliberate freeze-and-strangle strategy for `Interactions`.

**Major weaknesses:** `core` is a container, not a domain; the runtime kernel and the
UI element model are mutually dependent (a hidden single bounded context split across
packages); the engine contract package physically depends on the Selenium
implementation and on `core.driver`; the stable hook API lives inside a deprecated
package; there is no build-level enforcement of any purity invariant; and "Context"
means five different things.

---

## Architectural Domain Inventory

Fourteen domains are discoverable in the codebase. Not all are true domains.

| # | Domain | Location | Responsibility | Assessment |
|---|--------|----------|----------------|------------|
| 1 | Interaction execution kernel | `core.actions`, `core.flow`, `core.executor` | Deferred intent (Action), composition (Flow), iteration (FlowExecutor), profiles, tracing | True domain, the crown jewel. Not independent: imports `elements.api`, `elements.meta`, `core.interactions.hooks`. High internal cohesion, contaminated boundary. |
| 2 | UI element domain model | `elements.api`, `elements.api.capability`, `elements.meta` | Enum-driven element contracts, 15 capability interfaces, roles, registry | True domain. Selenium-free (verified: zero `org.openqa.selenium` imports). Imports `core.actions`, creating a cycle with domain 1. |
| 3 | Engine abstraction | `core.engine` | `UIEngine` contract, `LocatorDescriptor`, `LocatorStrategy`, `EngineConfig`, factory, bootstrap | True domain in intent; leaking in fact. The contract package imports `core.engine.selenium` (factory), `core.driver` (bootstrap), and `elements.api` (`resolve(Element, ...)`, finding C7). |
| 4 | Selenium platform | `core.engine.selenium`, `core.driver`, `core.bridge.selenium` | WebDriver lifecycle, SeleniumEngine, By bridge | True domain but split across three packages with no shared parent. `core.driver` presents itself as framework-level infrastructure rather than Selenium-extension internals. Already backlogged (`core-driver-package-selenium-coupling.md`). |
| 5 | Locator resolution | `core.resolvers.locator.*` | File/key/args -> descriptor pipeline, sources, parsing, templates | True domain, well-factored internally. Two problems: still emits Selenium `By` on governed paths, and hosts a second unrelated lifecycle (see #6). |
| 6 | Locator dev-time tooling | `core.resolvers.locator.sync`, `.json` (migrator, CLI), `.template` (generators) | Template generation, sync orchestration, properties-to-JSON migration CLI | Not a runtime domain. Build/dev tooling living inside the runtime resolution package. Different lifecycle, different consumers, same package. |
| 7 | Session runtime / facade | `core.runtime`, `core.context`, `core.bootstrap` | VOID, VOIDBuilder, SessionContext, one-time init | True domain. This is what the project calls "Runtime", but it is a UI session facade, not a domain-neutral runtime. `core.context` is half deprecated; `core.bootstrap` is Selenium-gated (C4). |
| 8 | Hook system | `core.interactions.hooks` | ActionHandler, Before/After constant libraries | True domain, stable tier, engine-agnostic. Physically owned by the deprecated legacy package `core.interactions`. Lifecycle inversion: the frozen package contains the living API. |
| 9 | Legacy compatibility | `core.interactions` (Interactions, Via), `core.utils.UIContext`, `core.context.ExecutionContext`, `core.bridge.selenium` | Frozen orchestrator and its scaffolding | Deliberate, well-marked, well-governed strangler zone. Its only sin is that living code (`dsl`, hooks) still sits inside or depends on it. |
| 10 | Observability | `core.logging.*`, `core.actions.trace` | CustomLogger facade, themes, intents, action tracing | True domain, excellent cohesion, zero Selenium. The healthiest domain in the codebase. |
| 11 | Configuration | `core.utils.ConfigLoader`, `core.engine.EngineConfig`, `core.logging.config.LogConfig`, `driver.properties`, `test.properties` | Hierarchical config loading | Not a domain, a fragment. The loader is in utils, schemas are per-subsystem, and the root config file (`driver.properties`) is named after the Selenium platform, making the framework's bootstrap contract Selenium-flavored. |
| 12 | Utilities | `core.utils`, `.data`, `.io`, `.web` | Config, enum resolution, data generation/verification, file IO, deprecated web helpers | Not a domain. A dumping ground: configuration, UI thread-local state (`UIContext`), test-data tooling, IO plumbing, and a managed graveyard (`web`). `EnumResolver`, non-deprecated and used by the DSL, imports `By`, `WebElement`, `ExpectedConditions`. |
| 13 | DSL / BDD adaptation | `dsl.VoidDSL`, `core.adapters.cucumber`, `StepDefinition/` | Context-driven test-facing API | Weak domain. `VoidDSL` delegates to the frozen `Interactions`, so the modern DSL is built on the deprecated pipeline. `StepDefinition/` sits outside the package hierarchy. |
| 14 | Demo/test content in main tree | `tests.demo.*` | Demo pages, hooks, page objects | Not a domain. Test content in `src/main/java`. |

Ownership verdict: domains 1, 2, 5, 8, 10 have clear conceptual ownership. Domains 3
and 4 have blurred ownership at exactly the boundary that matters most. Domains 6, 11,
12, 14 have no owner at all.

---

## Bounded Context Report

Natural bounded contexts, inferred from actual dependency clusters rather than package
names:

**1. The Interaction Kernel** (Action, Flow, FlowExecutor, ActionProfile, hooks, trace).
Boundary exists because this is the part the June audit found survives domain
substitution (6 of 7 primitives). Its language is intent, composition, execution, hook,
profile, trace. Overlap: its physical footprint spans four packages (`actions`, `flow`,
`executor`, `interactions.hooks`), and its `perform(UIEngine)` signature imports the
engine context's vocabulary.

**2. The UI Domain Model** (Element, capabilities, roles, plus the 17 concrete Action
subclasses). Boundary exists because this is where UI vocabulary lives: Clickable,
Typeable, dropdowns, tables, hover. Note carefully: the concrete actions (`ClickAction`,
`TypeAction`, `SelectAction`, ...) belong to this context, not to the kernel, since they
encode UI semantics, yet they live in `core.actions` beside the domain-neutral `Action`
interface. **The kernel/UI-domain boundary runs through the middle of `core.actions`,
invisible in the package structure.** The draft `generalize-element-into-target`
initiative confirms the project already senses this.

**3. Locator Resolution** (request -> descriptor). Boundary exists because resolution is
a pure transformation concern with its own sources, formats, and policies. Overlap: its
output type, `LocatorDescriptor`, is owned by `core.engine`, not by the resolution
context or a shared kernel, so the resolution context's core noun lives in someone
else's package. It also still knows `elements.api` and Selenium `By` (parser, reader,
deprecated `resolve()`).

**4. Engine Contract vs 5. Selenium Platform.** These should be two contexts with the
hardest boundary in the system: the contract (`UIEngine`, `LocatorDescriptor`,
`EngineConfig`) on one side, WebDriver-world (`SeleniumEngine`, `DriverFactory`,
`DriverContext`, `DriverManager`, `Waiter`, the bridge) on the other. Today they are
one blurred context: the contract package compile-time depends on the implementation
(factory switch, P8) and on `core.driver` (`EngineBootstrap` carries
`DriverFactory.Profile`). ADR-018 labels `EngineBootstrap` a migration abstraction
with acknowledged design debt, so this is known; as of this audit the boundary does
not hold.

**6. Session Runtime** (VOID, VOIDBuilder, SessionContext, FrameworkBootstrap).
Boundary exists because session lifecycle (create, use, shut down, multi-session
isolation) is orthogonal to what actions do. Overlap: VOID's public surface is
UI-domain (`navigateTo`, `getCurrentUrl`, `getTitle`, `refresh`), so the session
context currently is a browser-session context. Its bootstrap is gated on
`driver.properties` (C4), pulling platform vocabulary into the outermost boundary.

**7. Observability.** Clean, self-contained, correctly cross-cutting. No overlaps.
The model context to imitate.

**8. Legacy Compatibility** (Interactions, Via, UIContext, ExecutionContext, bridge).
A real and intentional context: the strangler zone. Its boundary is defined by
deprecation annotations rather than packages, which mostly works, except that
`dsl.VoidDSL` (not deprecated, test-facing) and `core.interactions.hooks` (stable)
live inside its blast radius.

**Contexts that are missing entirely:** a shared-kernel context (where
`LocatorDescriptor`, `Action`, stability annotations, and other cross-context
vocabulary would live is currently answered ad hoc: annotations in `core.annotations`,
descriptor in `core.engine`), and a configuration context.

---

## Package Audit

**`core`** -- The fundamental problem. `core` is not a domain, a layer, or a context;
it is "everything except elements, dsl, and tests." It contains the kernel, the engine
contract, the Selenium platform, the legacy zone, logging, config, bootstrap, and
utilities as siblings. Package structure here records implementation history (each
initiative added a sibling) rather than architecture. Every boundary violation below
is made easy because everything under `core` feels like one neighborhood.

**`core.actions`** -- High cohesion around the action model, but three populations
share one package: the domain-neutral kernel (`Action`, `ActionProfile`,
`HookChainAction`, trace), the UI-domain concrete actions (17 classes), and deprecated
remnants (`HookedAction`, `Profile`). 33 files, growing linearly with every new UI
capability. The kernel/UI split inside this package is the most consequential hidden
boundary in the codebase.

**`core.flow`, `core.executor`** -- Single-class packages, pure, minimal, correct.
Their existence as separate top-level siblings of `actions` fragments one context
across three packages, but the contents are healthy.

**`core.engine`** -- Mixed responsibilities: contract (`UIEngine`), shared vocabulary
(`LocatorDescriptor`, `LocatorStrategy`), configuration (`EngineConfig`),
instantiation (`UIEngineFactory`), and migration scaffolding (`EngineBootstrap`).
The factory's compile-time reference to `SeleniumEngine` makes the contract package
depend on its implementation. Weak point of an otherwise strong abstraction.

**`core.driver`** -- Internally cohesive, but architecturally mislocated in spirit:
Selenium-extension infrastructure presented as framework infrastructure. Its name, its
config file (`driver.properties`), and its consumption by bootstrap all promote
platform vocabulary to framework level. Already logged in the backlog; this audit
confirms the finding.

**`core.interactions`** -- Three unrelated lifecycles: a frozen 834-line deprecated
orchestrator, `Via` (deprecated cast helpers), and `hooks/` (a stable, engine-agnostic,
actively used API). Housing the stable hook system inside the deprecated package means
the modern kernel (`core.actions`) must import from the legacy zone. The clearest
example of package organization contradicting the architecture.

**`core.resolvers.locator`** -- Best internal structure in the codebase
(api/source/parser/template separation, chain-of-responsibility registry). Two flaws:
dev-time tooling (`sync`, migrator CLI, template writers) shares the package with
runtime resolution, and the dual `resolve() -> By` / `resolveDescriptor() ->
LocatorDescriptor` surface keeps Selenium in play (H1 and the ADR-020 invariant
address this).

**`core.utils`** -- The dumping ground. At least four unrelated concerns, and an
inverted dependency profile: utilities should be leaves, but `core.utils` imports
`core.driver`, `core.engine`, `core.resolvers.locator`, and `elements.api`. The `web`
subpackage is a managed graveyard (fine, per ADR-020), but `EnumResolver` and
`UIContext` carry live Selenium imports in a package with no deprecation story of its
own.

**`core.context`** -- Two classes, one deprecated. After `ExecutionContext` is removed
this package holds a single record. Exists as an artifact of the ADR-018 transition.

**`core.bootstrap`, `core.bridge.selenium`, `core.annotations`, `core.logging`** --
All fine individually. `core.logging` is exemplary: eight subpackages, clear internal
layering, zero domain leakage. `core.bridge.selenium` is exactly what a transitional
package should look like: named for what it bridges, deprecated for removal, scope
documented in ADR-019.

**`dsl`** -- One record that wraps the frozen legacy orchestrator. Its top-level
position implies it is a peer of `core` and `elements`; its dependencies say it is an
appendix of the legacy context. Also flagged in the May audit (C6) for By/WebElement
in its surface.

**`elements`** -- Cohesive and Selenium-free. Its flaws are conceptual, not
organizational: the vocabulary is UI-specific by design (fine for the UI domain, fatal
if this remains the root model of a domain-neutral runtime), and it depends on
`core.actions` (the cycle).

**`tests.demo` and `StepDefinition/`** -- Test and demo content in the production
source tree; the latter also violates naming conventions and hosts broken package-info
links (known findings). Pure implementation-history artifacts.

---

## Runtime Purity Report

"Runtime" here means the layers that must become domain-neutral: session facade,
kernel, executor. Classification of each domain-specific dependency:

| Dependency | Where | Classification | Why |
|---|---|---|---|
| `Action.perform(UIEngine)` type pin | `core.actions` | Architectural leak (domain axis); acceptable (engine axis) | The kernel's central signature names the UI domain. June audit crack #3. No REST/CLI action can exist without touching the kernel. |
| Concrete UI actions inside kernel package | `core.actions` | Architectural leak | ClickAction etc. are UI-domain content compiled into the runtime's own package. |
| `VOID.navigateTo/getCurrentUrl/getTitle/refresh` | `core.runtime` | Architectural leak | The session facade hardcodes browser session semantics. |
| `VOID` imports `WebDriver` | `core.runtime.VOID` | Acceptable | Confined to `@Deprecated(forRemoval=true) getDriver()`; governed bridge with exit date. |
| `SessionContext` holds `UIEngine` | `core.context` | Acceptable today, leak tomorrow | Correct for engine neutrality (ADR-018 achieved its goal); still pins the session to the UI domain. |
| `FrameworkBootstrap` hard-fails on `driver.properties` | `core.bootstrap` | Architectural leak | Framework-level init gated on Selenium platform config (C4). A non-browser domain cannot start. |
| `FlowExecutor` imports `core.engine` | `core.executor` | Acceptable | Only via the UIEngine parameter it forwards; the executor is a pure iterator. Falls out automatically if the perform() pin is generalized. |
| Kernel imports `core.interactions.hooks` | `core.actions` | Acceptable content, leaking location | The hook contract is engine-agnostic; the import path routes the modern kernel through the deprecated package. |
| `ActionHandler(UIEngine, LocatorDescriptor)` | hooks | Architectural leak (domain axis) | The extension mechanism itself is typed to UI. Domain extensions could not reuse the hook system. |
| `ActionCapability`, `LocatorStrategy` closed enums | kernel / engine | Architectural leak | Closed sets in the runtime enumerate UI concepts; a new domain must edit the runtime (June audit cracks #1 and #5). |
| Kernel use of `CustomLogger` | everywhere | Required | Observability is legitimately cross-cutting and domain-free. |
| `Flow`, `core.actions.trace` | kernel | Healthy | Zero domain semantics, verified. |
| `core.runtime` imports `core.driver` | VOID/builder | Acceptable, transitional | ADR-018 scoped; `EngineBootstrap` carries `DriverFactory.Profile` as documented migration debt. Must not outlive the initiative. |

Summary: on the engine axis, the runtime is nearly pure and getting purer with each
pending ADR. On the domain axis, the runtime is not pure and no current initiative
makes it so; the leaks are structural (signatures, enums, facade surface), not
incidental (imports).

---

## Dependency Analysis

Intended flow (ADR-013, System Overview): tests -> runtime facade -> kernel -> engine
contract -> platform implementation, with elements feeding intent in at the top and
resolution feeding descriptors in at the side.

Verified violations and inversions:

| ID | Violation | Evidence |
|----|-----------|----------|
| D1 | `elements.api` and `core.actions` are mutually dependent | Capability interfaces create Actions (`Clickable.click()` returns `ClickAction`); Actions hold Elements. Import lists confirm the cycle. Proof that Element+Capability+ConcreteAction is one bounded context. Any redesign treating "elements" and "actions" as separately movable units will fail. |
| D2 | Contract depends on implementation | `core.engine` -> `core.engine.selenium` via `UIEngineFactory` switch-on-string (P8). Adding PlaywrightEngine requires editing the contract package. DIP violated at the exact point whose purpose is inversion. |
| D3 | Contract depends on platform infrastructure | `core.engine` -> `core.driver` via `EngineBootstrap`/factory. Documented ADR-018 migration debt; today the engine abstraction cannot compile without WebDriver-world on the classpath. |
| D4 | Modern depends on deprecated (by location) | `core.actions` -> `core.interactions.hooks`. The kernel's hook imports route through the frozen package. |
| D5 | `dsl` depends on the legacy pipeline | `dsl` -> `core.interactions`. The non-deprecated DSL is structurally downstream of the frozen orchestrator; the legacy context cannot be deleted without breaking a living layer. |
| D6 | Utilities depend on everything | `core.utils` -> driver, engine, resolvers, elements. Leaves importing the trunk. Any module extraction trips over utils first. |
| D7 | Engine knows the element model | `UIEngine.resolve(Element, role)` plus `core.engine.selenium` importing `elements.api`/`elements.meta` (C7). Execution layer can bypass Action's resolution ownership; three sources of resolution truth (C5) follow directly. |

Ownership answers for the major subsystems:

- Kernel: owned by runtime; depended on by elements, facade, DSL; should be unknown to
  platform implementations (currently true) and to utilities (currently false).
- Engine contract: owned by runtime; depended on by kernel, facade, resolvers, hooks;
  should not know elements (false, D7), Selenium (false, D2), or driver (false, D3).
- Selenium platform: should be owned by the Selenium extension; depended on by nothing
  outside itself plus one registration point. Currently known to: engine contract,
  utils, legacy, bootstrap (via config file), runtime (transitional).
- Locator resolution: owned by the UI domain (its vocabulary is files/keys/roles);
  depended on by engine implementations and legacy; should not know Selenium `By`
  (still does, on governed paths).
- Logging: owned by observability; depended on by all; knows nothing. Correct.

---

## Naming and Vocabulary Report

Suffix discipline is better than average, but several terms carry multiple
architectural meanings.

| ID | Finding |
|----|---------|
| D8 | **"Context" has five meanings.** `DriverContext` (mutable thread-local registry), `UIContext` (mutable thread-local scratchpad, deprecated), `ExecutionContext`/`SessionContext` (immutable per-session value objects), `LocatorContext` (strategy interface for choosing locator files), `LoggerContext` (Log4j holder). A reader cannot infer from "Context" whether a thing is state, a snapshot, or a strategy. Only the `SessionContext` meaning matches the canonical definition (immutable execution state). |
| D9 | **"Profile" has three meanings.** `DriverFactory.Profile` (environment preset), `ActionProfile`/`ActionProfiles`/`Profiles` (hook-bundle execution policy), deprecated `Profile` class. "Run with the CI profile using the SAFE profile" is a producible sentence. |
| D10 | **"Bootstrap" has two meanings.** `FrameworkBootstrap` is an init gate; `EngineBootstrap` is a parameter object carrying startup inputs. Same suffix, opposite roles. ADR-018 schedules EngineBootstrap's retirement, so this is scheduled vocabulary debt. |
| D11 | **"Interactions" is the framework's most expensive name.** VOID's stated identity is an interaction runtime; its future core noun is "interaction". In code, `Interactions` is the frozen deprecated orchestrator and `core.interactions` is the legacy zone (which also shelters the living hooks). Any future ubiquitous language built around "interaction" collides with 834 lines of frozen history. |
| D12 | **Factory/Builder/Manager triad around drivers.** `DriverFactory` (actually a static facade exposing an inner fluent `Builder`), `DriverManager` (thin static lifecycle orchestrator; "Manager" means "leftover coordination"), `DriverContext` (the registry that actually owns instances). `UIEngineFactory` is a true factory, so even "Factory" is used at two granularities. |

Names that match their responsibilities well: `LocatorDescriptor`, `LocatorRequest`,
`FlowExecutor`, `LocatorSourceRegistry`, `SeleniumLocatorBridge` (transitional adapter,
named honestly), the `Resolver` family (`LocatorResolver`, `EnumResolver` both
transform representations), `VOIDBuilder` (genuine builder with single-use guard).

Names that mislead:

- `ElementActions` is a factory named like a utility grab-bag; its `@Internal` tier is
  invisible in the name.
- `Via` communicates nothing; it is a set of static cast/bridge helpers.
- `HookedAction` vs `HookChainAction`: near-identical names, opposite lifecycles
  (deprecated vs internal), and the replacement still delegates to the deprecated one
  for execution, which makes the naming actively confusing during migration.
- `ActionHandler` handles hooks around actions, not actions. `Before`/`After` as class
  names for constant libraries read as annotations and collide conceptually with
  JUnit/Cucumber `@Before`/`@After` in a testing-adjacent codebase.
- `UIEngine.resolve(...)`: the name implies engine-owned resolution while ownership
  actually lies with `LocatorResolvers`. The name codifies the C5 ownership violation.
- Casing: `VOID`/`VOIDBuilder` vs `VoidDSL` spell the brand two ways.

Terminology describing implementation vs role: most modern names describe architectural
role (good). Exceptions cluster in platform and utils layers (`DOMUtils`, `WaitUtils`,
`ByParser`, `JsonTreeBuilder`), acceptable for genuinely platform-specific code and
wrong only when such names surface in engine-agnostic layers (e.g., `ByParser` inside
the resolution context that is supposed to be Selenium-free).

---

## Architectural Consistency Report

Consistent:

- Stability tiers are applied uniformly and respected (`@Beta` on the whole kernel,
  `@Deprecated(forRemoval)` used with precision, `@Internal` on plumbing).
- The deprecate-and-replace pattern is executed the same way every time:
  `ExecutionContext` -> `SessionContext`, `HookedAction` -> `HookChainAction`,
  `resolve()` -> `resolveDescriptor()`, `Interactions` -> Action/Flow. Disciplined
  strangler-pattern work.
- ADR traceability: every invariant in the System Overview maps to a numbered decision.
- Value objects are consistently immutable records.

Inconsistent:

| ID | Finding |
|----|---------|
| D13 | **Abstraction introduction is asymmetric.** The locator subsystem got the full treatment (interface, registry, chain of responsibility, request object, template policy) while engine selection got a switch on a string (P8) and driver configuration got a static facade. Similar problems, wildly different architectural investment. Extensibility exists exactly where it was needed historically, not where the roadmap needs it. |
| D14 | **Legacy boundary marking is inconsistent.** Some legacy is quarantined by package (`bridge.selenium`), some by annotation only (`Interactions` in a package that also holds stable code), some not at all (`UIContext` in utils; `EnumResolver` Selenium imports carry no deprecation). |
| D15 | **Responsibility symmetry breaks at the engine boundary.** Actions own resolution on the modern path, engines own it via `resolve(Element, ...)` on a backdoor path, legacy `Interactions` owns it on a third path (C5). One concept, three owners. |
| D16 | **Organizational grain varies.** Logging is organized by role (excellent); utils by nothing; core by historical accretion; resolvers by pipeline stage (good). The codebase organizes packages well only when a subsystem is deliberately redesigned. |

---

## Long-Term Scalability Assessment

Can this architecture evolve into a domain-neutral interaction runtime? Yes
conceptually, no physically. The June audit's C+ verdict remains accurate.

What will age well: the kernel's concepts (deferred intent, composition, dumb
executor, hook chains, profiles, traces); the observability stack; the governance
system itself; LocatorDescriptor's shape (strategy + value + args + label), even
though its name and `LocatorStrategy` vocabulary will not.

Biggest long-term risks, in order:

| ID | Risk |
|----|------|
| D17 | **Single-artifact enforcement.** Every invariant ("engine-agnostic layers are Selenium-free", "nothing outside UIEngine calls WebDriver") is enforced by convention, review, and grep. One Maven module means the compiler never defends a boundary. 18 files with live Selenium imports, several in non-deprecated non-platform code, show it already slips. |
| D1 | **The kernel/UI-domain fusion.** `Action.perform(UIEngine)`, concrete UI actions inside the kernel package, the elements/actions cycle, and the UI-typed hook signature together mean the runtime cannot be pointed at without dragging the UI domain along. The second domain will force this apart under pressure; deciding it deliberately first is far cheaper. |
| D18 | **Closed enums at extension points.** `ActionCapability` and `LocatorStrategy` are the runtime's two hardcoded vocabularies. Every future domain and engine must edit the runtime to teach it new words. The June audit ranks the capability enum as what cracks first, including the failure mode of `UNKNOWN` silently applying browser wait hooks to non-browser actions. |
| -- | **Facade shape.** `VOID` as the single session object with browser methods means either the facade fragments per domain later, or it bloats into a god object. Neither has been decided. |
| -- | **Configuration identity.** As long as `driver.properties` is the file the bootstrap dies without, the framework's ignition system belongs to Selenium. |
| D5 | **The legacy zone's gravitational pull.** `dsl` (living, test-facing) depending on `Interactions` (frozen) means the legacy pipeline gains new indirect consumers with every DSL user. Frozen code with living dependents does not shrink. |

---

## Architectural Priorities

### Critical (resolve before introducing any new domain)

1. **The two-axis conflation.** The architecture must explicitly distinguish engine
   neutrality (largely achieved) from domain neutrality (not started). Every "runtime"
   claim in docs and every invariant should state which axis it governs.
2. **Kernel/UI-domain entanglement** (D1): the `Action.perform(UIEngine)` pin, concrete
   UI actions cohabiting with the kernel, and the `elements.api`/`core.actions` cycle.
   One finding, not three; the boundary of the runtime kernel is undefined in code.
3. **Engine contract depends on Selenium implementation and driver infrastructure**
   (D2, D3). The one boundary a multi-engine future depends on absolutely is soft in
   both directions.
4. **Closed extension vocabularies** (D18): `ActionCapability` and `LocatorStrategy`
   enums, including the `UNKNOWN` silent-fallback behavior.

### High (will significantly affect scalability)

5. No build-level boundary enforcement for any purity invariant (D17).
6. Bootstrap and configuration Selenium-gating (C4; `driver.properties` as the
   framework's root contract) plus configuration fragmentation across
   utils/engine/logging.
7. Live Selenium in non-deprecated, non-platform code: `EnumResolver`, `UIContext`,
   and the resolvers' By-returning path (D14).
8. Living-on-legacy dependencies: `dsl.VoidDSL` -> `Interactions` (D5); stable hooks
   housed in `core.interactions` (D4).
9. `UIEngine.resolve(Element, ...)` backdoor (D7, C7/C5): three resolution owners; the
   engine layer knows the element model.

### Medium (inconsistencies worth resolving)

10. Vocabulary overloads (D8-D12): Context x5, Profile x3, Bootstrap x2, the
    `Interactions` name collision, `HookedAction`/`HookChainAction`, the driver triad.
11. `core.utils` as an unowned multi-domain package with an inverted dependency
    profile (D6).
12. Runtime resolution and dev-time tooling sharing `core.resolvers.locator` (two
    lifecycles, one package).
13. `LocatorDescriptor` ownership ambiguity: the system's shared vocabulary type is
    housed in the engine contract package while produced by resolvers and carried by
    actions.
14. `core` as a non-domain container whose sibling layout reflects initiative history
    rather than architecture.

### Low (minor observations)

15. `tests.demo` and `StepDefinition/` in the main source tree (already externally
    flagged).
16. `core.context` reduced to near-emptiness post-deprecation; `core.flow`/
    `core.executor` as single-class top-level packages.
17. Naming polish: `Via`, `ElementActions`, `Before`/`After` constant classes,
    VOID vs Void casing.
18. `SeleniumEngine.waitForOverlay` Angular assumption (M5) and remaining M-series
    items, already tracked in `architecture-audit-2026-05.md`.

---

## Closing Opinion

VOID's governance is ahead of its structure, which is the right failure mode: the
project consistently knows about its debt before an outside auditor finds it (C1-C7,
the domain-agnostic audit, the driver-coupling backlog entry all predate this review).
The pending ADR-018/019/020 trajectory will complete engine neutrality. But no active
initiative addresses the four Critical findings above, and all four sit on the path to
"domain-neutral interaction runtime." The redesign this audit feeds should begin by
drawing the kernel's boundary, on purpose, in one place, before any second domain
makes the decision under duress.

---

# Part II -- Architecture Ontology Review

Reviewed model: the five-concept ontology proposed for the domain-neutral redesign.

```
Runtime      owns execution, lifecycle, sessions, orchestration; knows no domain
Interaction  represents an operation; describes intent; requires capabilities;
             never contains execution logic
Capability   describes what a Target supports; contract between Targets and Interactions
Target       virtual object that interactions operate upon
Domain       provides concrete Target implementations; defines domain-specific
             interactions and capabilities
```

This part assumes no implementation exists and reasons from first principles. Finding
IDs use the `O` prefix (O1..O9).

---

## Executive Summary (Ontology)

**The five concepts are the right nucleus. They are necessary. They are not
sufficient, and one of the five is self-contradictory as defined.**

The strongest feature of the ontology is its center of gravity: Interaction as pure
intent, Capability as the only coupling between intent and subject, Target as a
virtual object, Domain as the extension unit. These four survive every domain
substitution tested and every architect would converge on their meaning after modest
definition tightening.

The defect is Runtime. As defined it holds four responsibilities (execution,
lifecycle, sessions, orchestration) and simultaneously promises to know nothing about
domains. But *executing* a click, a query, or an arm movement is irreducibly
domain-specific. A Runtime that owns execution and knows no domain is a contradiction
unless the ontology names the contract through which domains supply executable
behavior. The model currently gives execution logic no home at all: Interaction
explicitly never contains it, Capability is descriptive, Target is a virtual object,
Domain "provides Targets" but nothing says it provides behavior, and Runtime is
domain-blind. **Execution logic is an orphaned responsibility** (O1).

Two repairs follow, and they are of different kinds. One is an audit conclusion:
**Session** must be first-class (already named in Runtime's responsibilities, absent
as a concept, user-visible in multi-session workflows). The other is a named gap, not
a chosen design: **the concept responsible for performing an Interaction must be
explicitly modeled before the runtime redesign proceeds** (O1). Candidate shapes
include Executor, Dispatcher, Interpreter, Domain Runtime, Operation Handler, and
per-interaction strategy objects; choosing among them is a design decision that
belongs to the redesign (ADR-021), and this audit deliberately does not make it.
Throughout Part II, the lowercase placeholder **execution owner** marks that unnamed
concept wherever the analysis must refer to it; the analysis constrains what the
concept must satisfy, not what it is. Every other candidate concept examined
(Context, Resolver, Result, Event, Resource, Provider, Environment, State) folds
naturally into the existing concepts or into the gap, and should not be introduced.

A second structural insight: the unmodeled execution owner is the *ontological source*
of the two-axis conflation found in Part I. Web-via-Selenium and Web-via-Playwright
share one Domain (one vocabulary of Targets, Capabilities, Interactions) but differ in
how that vocabulary is performed. Until the ontology names the performing concept, it
cannot even express the difference between adding Playwright (same Domain, new
performance) and adding REST (new Domain). The implementation confusion documented in
Part I is the ontology's gap made concrete.

With Session added, the execution-ownership gap explicitly modeled, and the definition
tightenings below, the ontology holds for a decade of discrete-interaction automation
domains. Its one honest scope limit: it models bounded, discrete operations, and will
strain under streaming, event-driven, and continuous-control domains (O9). That limit
should be declared, not discovered.

---

## Concept Review

### Runtime

- **Fundamental?** Yes. Something must own orchestration and be the fixed point
  extensions plug into.
- **Derivable?** No.
- **Overloaded?** Yes, fourfold: execution, lifecycle, sessions, orchestration. Three
  of the four belong elsewhere: execution belongs to the domain-supplied execution
  owner (whatever shape it takes, O1), session lifecycle belongs to Session, leaving
  Runtime with orchestration, validation, and observation dispatch. As defined it is
  a layer wearing the name of a concept.
- **Precise?** No. "Owns execution" and "knows nothing about specific domains" are
  jointly unsatisfiable without an execution contract (O1). Two architects would not
  converge: one will build a process host, the other an execution engine.
- **Verdict:** keep, narrow. Runtime = the domain-blind orchestrator that validates
  capability requirements, dispatches Interactions to the domain-supplied execution
  owner within Sessions, and publishes observation points. Nothing more.

### Interaction

- **Fundamental?** Yes. It is the framework's namesake noun and the unit of intent.
- **Derivable?** No.
- **Overloaded?** Slightly: the definition does not distinguish the *description* of
  an operation from an *occurrence* of it (O2). "Click LOGIN" as a value vs "the click
  that happened at 13:15:37 and failed." Traces, retries, and results attach to
  occurrences, not descriptions. The ontology should state: an Interaction is an
  immutable description; execution produces occurrences.
- **Precise?** Mostly, with three gaps: (a) cardinality -- the model implies one
  Target, but drag-from-A-to-B, join-across-tables, and diff-two-endpoints bind
  several (O3); (b) subjectless operations -- navigate, wait, authenticate have no
  Target unless the Session itself is a valid subject (O4); (c) results -- reading
  interactions (read text, query rows) yield values, and the model says nothing about
  them (O5).
- **Verdict:** keep. Amend: an Interaction binds one or more subjects (Targets or the
  Session), requires the capabilities of each, and may yield a Result as part of its
  contract. Result is an aspect of Interaction, not a sixth concept.

### Capability

- **Fundamental?** Borderline by strict minimality: one could derive it as "the set of
  Interactions a Target supports." First-class status is nevertheless justified,
  because it is the *only* thing standing between Interaction vocabulary and Target
  vocabulary. Without it, every Interaction names Target types and every Target names
  Interaction types: the coupling the ontology exists to prevent.
- **Overloaded?** At risk. In the current implementation, capability interfaces also
  *manufacture* actions (`Clickable.click()` returns a `ClickAction`). That is
  acceptable API convenience but must not become ontology: the moment Capability
  creates or performs, it stops being a contract and becomes a factory-executor
  hybrid, and the descriptive invariant dies (O6).
- **Precise?** One ambiguity: declared potential vs runtime state. A button whose
  Target declares Clickable may be disabled right now. The ontology should define
  Capability as *static declared potential*, validated by the Runtime; momentary
  actionability is an execution concern owned by the execution owner. This also
  disarms the "dynamic capabilities" failure scenario.
- **Verdict:** keep, with the descriptive-only and declared-potential clarifications.

### Target

- **Fundamental?** Yes. The V and O of VOID.
- **Derivable?** No.
- **Precise?** One decision is unmade and everything downstream depends on it: is a
  Target a *description* (an address: immutable, stateless, valid before the real
  object exists) or a *handle* (a live proxy holding connection state)? (O7). For a
  button the difference is invisible; for a robot arm or a DB transaction it is the
  whole design. The description model is the right choice: it keeps Targets eternal
  value objects, pushes resolution-to-real-referent into the execution owner, and
  keeps all runtime state inside Session and the execution owner. The handle model
  would smear lifecycle and state across every concept.
- **Neutrality strain:** for CLI terminals and similar, the Target and the Session
  nearly collapse into one thing (the terminal *is* the connection). The
  Session-as-valid-subject amendment (O4) absorbs this: a domain may model the session
  itself as the primary subject and define few or no sub-Targets.
- **Missing relation:** containment. Table contains Row contains Cell; a window
  contains panes. The ontology has no scoping relation, yet composite Targets are
  routine in every listed domain. Amend: a Target description may reference a parent
  Target as its resolution scope. That is a value-level reference, not a new concept.
- **Verdict:** keep. Decide description-not-handle explicitly; add the scope relation.

### Domain

- **Fundamental?** Yes, as the unit of extension. Nothing else can absorb "the
  pluggable vocabulary package."
- **Overloaded?** Yes, in one important way: as defined it conflates *vocabulary*
  (which Targets, Capabilities, Interactions exist) with *implementation* (how they
  are executed against a real technology) (O8). Web-via-Selenium and
  Web-via-Playwright are one vocabulary with two implementations. If Domain owns both,
  either every engine is a separate Domain (vocabulary duplicated) or engines have no
  conceptual identity (Part I's axis conflation, reborn). Separating vocabulary from
  performance resolves this: a Domain defines vocabulary and ships one or more
  execution owners for it. How that separated concept is named and shaped is the
  redesign's decision (O1/AD2).
- **Precise?** The name collides with DDD's "problem domain" but is acceptable; the
  definition "provides concrete Target implementations" should become "defines the
  Target, Capability, and Interaction vocabulary of one interaction medium, and
  provides at least one implementation of the execution-owner contract."
- **Verdict:** keep; separate implementation out of the definition (O1/O8).

---

## Concept Reality Tests

Two litmus tests every proposed concept should pass, applied to the full candidate
set. Concepts that fail are not automatically disqualified, but they must earn their
place with an explicit argument rather than inherit it from appearing in a list.

### Observability: can you point to one at runtime?

| Concept | Observable? | Notes |
|---|---|---|
| Runtime | Yes | A running orchestrator exists; you can point to it. |
| Session | Yes | User-visible, holds identity and lifetime; multi-session tests literally hold two. |
| Target | Yes | A description you can construct, print, and hand around; its resolved referent is equally concrete. |
| Interaction | Yes | A value you can build, compose, log, and trace. |
| Domain | Yes | A registered extension with a name. |
| Capability | **No** | A property, not a thing. It is never instantiated, never held, never observed; it is only declared and checked. |

Capability is the sole non-observable concept, so its first-class status cannot be
assumed; it must be argued. The argument: Capability is the only thing standing
between the Target vocabulary and the Interaction vocabulary. Demote it to Target
metadata and one of two couplings appears: Interactions inspect Targets directly (the
vocabularies fuse), or the Runtime learns both vocabularies to mediate (I1 dies).
First-class-but-not-observable is acceptable in exactly one situation: when the
concept's job is to *prevent* a dependency rather than to *be* something. Capability
is that situation. The redesign should record the corollary: Capability is a contract,
not an entity -- it must never acquire identity, state, or lifecycle, and any design
that gives it those has misread the ontology.

### Independent existence: can the concept exist alone?

| Concept | Alone? | Depends on |
|---|---|---|
| Runtime | Yes | -- |
| Session | No | Runtime (creates it); a Domain binding (cardinality is AD1) |
| Target | Yes | A description is meaningful with no runtime anywhere |
| Capability | No | Declared on Targets; defined in a Domain's vocabulary |
| Interaction | Yes | An intent description exists without ever being executed (it references capabilities, but referencing is not existential dependency) |
| Domain | Yes | -- |
| execution owner (gap, O1) | No | Provided by a Domain; scoped to a Session |

This test exposes attributes masquerading as entities. Three concepts fail it.
Session earns first-class status anyway: it has identity, state, and a user-visible
lifetime -- it is a dependent *entity*, not an attribute. The execution owner earns
it: an unowned responsibility must live somewhere, and no independent concept can
absorb it. Capability fails *both* tests (non-observable and dependent) and survives
only on the decoupling argument above -- which is precisely why that argument must be
written into the ADR rather than left implicit.

Rule for the redesign: any future concept proposal that fails both tests should be
rejected by default; the burden of proof lies with the proposer, and "it decouples
two vocabularies that must not meet" is the only precedent for an exception.

---

## Missing Concepts

Tested candidates: Session, Context, Resource, Adapter, Provider, Resolver,
Environment, Execution, Result, State, Event, Observation.

**Finding O1 -- execution logic has no explicit conceptual owner.** The evidence
supports exactly this much. The ontology intentionally separates intent (Interaction),
declaration (Capability), subject (Target), vocabulary (Domain), and orchestration
(Runtime); none of the five may perform, and something must. This responsibility must
be explicitly modeled before the runtime redesign proceeds.

What this audit does NOT decide is the shape of that concept. Executor, Dispatcher,
Interpreter, Domain Runtime, Operation Handler, and per-interaction strategy objects
are all viable architectures with different trade-offs; choosing among them is design
work belonging to the redesign initiative (ADR-021, decision AD2). Whatever is chosen
must satisfy the constraints this review derives for the placeholder: provided by a
Domain, scoped to a Session, opaque to consumers, and the only place effects happen.
(Adapter and Provider from the candidate list are this same concept under other
names; the redesign should introduce exactly one.)

**Add: Session.** Justification: the ontology already uses the word ("Runtime owns
sessions") without defining it, sessions are user-visible (multi-session workflows
are a stated product feature, so tests hold and compare them), sessions carry
identity, environment, and lifecycle that no other concept can hold once Target is a
stateless description, and subjectless interactions need the Session as their subject
(O4). A concept that is public, stateful, and load-bearing cannot remain implicit.

**Fold, do not add:**

| Candidate | Belongs to | Why |
|---|---|---|
| Result | Interaction | Part of the Interaction contract ("may yield a value"); standalone Result adds a concept without adding a decision. |
| Context / Environment | Session | Configuration, credentials, base addresses are session-scoped state. |
| Resolver / Resolution | execution owner | Mapping a Target description to a real referent is precisely the domain-specific half of execution. |
| Event / Observation | Runtime | Hook dispatch, tracing, and logging are orchestration outputs; they must be typed against neutral concepts (Interaction occurrence, Session), never against any domain vocabulary. |
| Resource | Target or Session | A "resource" is either something interacted with (Target) or something owned by the session (connection). |
| State | Session / execution owner | Once Target-as-description is decided, all mutable state has exactly two homes. |
| Execution | Runtime + execution owner | The phase, not a thing; split as orchestration (Runtime) and performance (the execution owner). |

Final ontology: **six named concepts plus one named gap**. Runtime, Session,
Interaction, Capability, Target, Domain, and the execution-owner concept the redesign
must name (AD2). Anything beyond these should be resisted.

---

## Relationship Model

```
                    defines vocabulary of
        Domain ----------------------------------+
          |                                      |
          | ships                                v
          v                          Target -- declares --> Capability
  [execution owner]                     ^                       ^
          ^                             | binds (1..n,          | requires
          | dispatches through          |  or Session)          |
          |                             +------ Interaction ----+
       Session                                      ^
          ^                                         | submits
          | creates/destroys                        |
       Runtime <------- validates requirements -----+
          ^
          | registers into (additive, at bootstrap)
        Domain

  [execution owner] = placeholder for the unnamed concept of finding O1 (AD2)
```

Validated relationships:

- A **Target declares Capabilities**. Correct direction. A Target never knows which
  Interactions exist.
- An **Interaction requires Capabilities** (of each subject it binds). Correct. An
  Interaction never names Target types; Capability is the only coupling (invariant I6
  below).
- The **Runtime executes Interactions** -- refined: the Runtime *orchestrates*
  execution (validates, dispatches, observes); the **execution owner performs** it.
  Without this refinement the relationship is false.
- A **Domain provides Targets** -- refined: a Domain defines Target *kinds* and the
  capability/interaction vocabulary; concrete Target descriptions are authored by the
  *consumer* (the test author writing page objects, endpoint catalogs, device maps) in
  the language the Domain defines. The consumer is a silent but real participant in
  the model.
- A **Session binds Domain(s)** -- and the cardinality is an open decision (AD1), not
  a relationship this review can settle. The current architecture assumes
  single-domain-per-session (June 2026 audit) and that assumption is protective, but
  without an implementation to test against, the audit cannot elevate it to a truth:
  cross-domain identity flows (a REST session mints a JWT a Web session then uses)
  and composite hardware (robot arm plus vision camera: one session, two, or a
  composite?) are plausible futures the redesign must rule on. What the ontology CAN
  assert regardless of the ruling: cross-domain composition happens somewhere
  explicit -- across Sessions by the consumer, or inside a deliberately-designed
  composite Session -- never implicitly.
- **Missing relationship now named:** the Runtime **validates** that each subject's
  declared Capabilities satisfy the Interaction's requirements, before dispatch. No
  concept owned validation in the original model.

Reversal check: no relationship should be reversed. In particular, Capability must not
point at Interactions (it would enumerate them, closing the vocabulary), and the
execution owner must not create Interactions (it would collapse intent into
execution).

---

## Ownership Matrix

| Concept | Defined by | Instantiated by | Consumed by | Must never know it exists |
|---|---|---|---|---|
| Runtime | Framework | Process bootstrap | Everyone (contracts only) | -- (but Runtime itself must know no concrete Domain, execution owner, Target, Capability, or Interaction type) |
| Session | Framework contract | Runtime, on consumer request | Consumer, execution owner | Targets, Capabilities (they are values; sessions pass through them, not into them) |
| Interaction | Contract by framework; concrete types by Domain | Consumer intent, typically via a Target's capability surface | Runtime (opaque), execution owner (concrete) | Nothing below the execution owner |
| Capability | Vocabulary by Domain; contract by framework | Static declaration | Targets (declare), Interactions (require), Runtime (validate as opaque set) | Other Domains' execution owners |
| Target | Kinds by Domain; descriptions by consumer | Consumer (authored catalogs) | Interactions (bind), execution owner (resolve) | Runtime internals (Runtime treats Targets as opaque values) |
| Domain | Extension author | Registration at bootstrap | Runtime (through registration contract only) | Other Domains |
| execution owner (gap, O1/AD2) | Contract by framework; implementations by Domain | Domain, scoped per Session (binding cardinality pending AD1) | Runtime (dispatch), Session (lifecycle) | Consumers (tests never touch it directly) |

The last column is where architectures die. Three prohibitions carry the whole model:
the Runtime never knows a concrete anything; a Domain never knows another Domain; a
consumer never knows the execution owner.

---

## Lifecycle Model

| Concept | Created | Lives | Destroyed | Identity | Caching |
|---|---|---|---|---|---|
| Runtime | Process bootstrap | Process lifetime | Process exit | Singleton per process | -- |
| Domain | Authored offline; registered at bootstrap | Process lifetime | Never (unregistration is out of scope) | Name in registry | -- |
| Session | Runtime, on consumer request | Until explicit shutdown | Explicitly by consumer; Session owns teardown of its execution owner and all live resources | Yes -- first-class, user-visible | Owns nothing cached beyond its execution owner |
| execution owner (gap, O1/AD2) | Domain, when a Session binds it | Session lifetime | With its Session | Per-Session | Owns resolution caches, connections, handles |
| Target | Authored by consumer as static description | Eternal (immutable value) | Never | Value semantics (its description is its identity) | Nothing to cache; *resolution* of it may be cached by the execution owner |
| Capability | Static declaration | Eternal | Never | Value/type semantics | -- |
| Interaction | Per intent expression | Immutable value; reusable | Garbage semantics | Description: value identity. Occurrence: trace identity, minted by Runtime at dispatch | -- |

Answers to the ownership-of-lifecycle questions: consumers create Targets and
Interactions (descriptions); the Runtime creates Sessions and occurrence identities;
Domains create execution owners; the Runtime validates Capabilities; Sessions own
destruction; the execution owner owns caching; nothing else owns anything.

---

## Architectural Invariants (implied by the ontology)

These are truths the ontology's definitions entail. Rules that are instead *design
choices* have been withdrawn to the Open Architectural Decisions list that follows;
an audit conducted with no implementation cannot settle them, and presenting them as
invariants would smuggle design into the audit.

- I1. The Runtime never depends on any concrete Domain, execution owner, Target,
  Capability, or Interaction type. It sees contracts and opaque values only.
- I2. A Domain never modifies the Runtime. Extension is additive registration.
- I3. An Interaction never executes. It is an immutable description of intent.
- I4. A Capability never performs and never creates. It is declarative.
- I5. A Target never orchestrates and never executes. (Its second property, "holds no
  runtime state", is contingent on the Target-as-description decision, O7.)
- I6. Capability is the only coupling between Interactions and Targets. No Interaction
  names a Target kind; no Target names an Interaction.
- I7. All effects are produced by the domain-supplied execution owner and dispatched
  by the Runtime. No side channels (the ontological generalization of ADR-007).
- I8. Withdrawn -- reclassified as open decision AD1 below. (Session-to-domain
  cardinality is a design constraint, not a truth.)
- I9. Execution state lives only in Session and the execution owner. Everything else
  is a value. (Contingent on the Target-as-description decision, O7.)
- I10. Observation (hooks, traces, logs) is passive: it never alters execution
  semantics, and its contracts are typed against neutral concepts only (the
  generalization of the "ActionCapability is metadata" invariant).
- I11. Capability sets and interaction vocabularies are open: a Domain introduces new
  ones without any change to Runtime-owned types (the ontological form of Part I
  finding D18).
- I12. Requirement validation exists: the Runtime rejects an Interaction whose
  requirements are not satisfied by its subjects' declared Capabilities. (That
  validation must exist is entailed by "Interaction requires Capabilities"; *when* it
  runs -- eagerly before dispatch or lazily at execution -- is design, see AD3.)

### Open Architectural Decisions (not invariants)

Design constraints the review initially risked stating as truths. Each must be
decided in the redesign (ADR-021) with concrete use cases on the table; none can be
settled by an audit with no implementation to test against.

- **AD1. Session-to-domain cardinality** (formerly I8). Single-domain-per-session is
  the current working assumption and the simplest protective rule. But: a REST
  session that mints a JWT a Web session then uses -- one identity across two
  sessions, or one session across two domains? A robot arm plus its vision camera --
  one session, two, or a composite Session concept? The audit cannot answer. The
  derived truth that survives either ruling: cross-domain composition must be
  explicit, never ambient.
- **AD2. The shape and name of the execution owner** (O1). Executor, Dispatcher,
  Interpreter, Domain Runtime, Operation Handler, per-interaction strategies. The
  audit constrains the concept (domain-provided, session-scoped, consumer-opaque,
  sole producer of effects); the redesign chooses its form.
- **AD3. Validation timing.** Eager validation before dispatch is fail-fast and
  simple; lazy validation permits late-bound capability discovery. I12 records only
  that validation exists; its timing is a redesign trade-off.

---

## Extensibility Assessment

Test: introduce a new domain with zero Runtime modification.

Against the original five concepts: **fails**, for a structural reason, not a detail.
With execution unowned (O1), the Runtime must internalize knowledge of how things are
performed, and every new domain edits it. Part I's closed enums (`ActionCapability`,
`LocatorStrategy`) are exactly this failure already fossilized in code: the Runtime
enumerating vocabulary that Domains should own.

Against the amended model (Session added, the execution-ownership gap explicitly
modeled): **holds**, provided four conditions stay true: capability vocabulary is
open (I11), observation contracts are neutral (I10), session creation is
parameterized by domain registration (I1, with binding cardinality per AD1), and
validation is contract-based rather than enumeration-based (I12).

Spot checks:

- **Cloud infrastructure / IoT / industrial:** Targets (instance, topic, valve),
  Capabilities (Provisionable, Publishable, Actuatable), discrete Interactions. Fits.
  Continuous telemetry does not (see O9).
- **Game / OS / browser-extension automation:** structurally identical to UI
  automation. Fits trivially.
- **Quantum hardware:** perhaps the best stress test, and it passes: gates are
  discrete Interactions, qubits are Targets, gate-set support is Capability, a QPU
  binding is a Session with its execution owner. The ontology is agnostic to how
  exotic the medium is; it cares only that operations are discrete and bounded.
- **Streaming APIs / long-running robotics:** does not fit without new semantics
  (subscription, progress, cancellation). See Weaknesses.

---

## Failure Analysis

| Scenario | Verdict | Reasoning |
|---|---|---|
| Composite Targets (table > row > cell) | Holds with amendment | Target descriptions may reference a parent as resolution scope (value-level relation). Without the amendment, every domain reinvents containment. |
| Multi-target Interactions (drag A to B, cross-table join) | Holds with amendment | Interaction binds 1..n subjects, requiring capabilities per subject (O3). The singular phrasing in the original model is the only obstacle. |
| Subjectless Interactions (navigate, wait, authenticate) | Holds with amendment | Session is a valid Interaction subject (O4). Bonus: this unifies the current implementation split where facade methods (`navigateTo`) bypass the Action pipeline entirely -- ontologically they were always Interactions on the Session. |
| Stateful Targets (robot arm pose, DB transaction) | Holds | Only if Target-as-description is decided (O7). State lives in the execution owner and Session. Under Target-as-handle, this scenario breaks the model. |
| Dynamic Capabilities (disabled button, read-only mode) | Holds | Capability = declared potential, validated per I12; momentary actionability is the execution owner's concern. The two-phase distinction must be explicit or every domain will blur it. |
| Distributed Targets (a cluster, a replicated queue) | Holds | A description may denote a logical object with plural physical realization; resolution plurality is internal to the execution owner. |
| Batch execution / composition | Holds | A composite Interaction (sequence) is itself an Interaction; composition is derivable, no new concept. The current `Flow` is this. |
| Virtual Targets (computed, non-addressable subjects) | Holds | "Virtual object" is already the definition; a Target needs no physical referent until the execution owner resolves it. |
| Streaming APIs, continuous control, event-driven domains | **Breaks** | The model assumes bounded, discrete operations with a beginning and an end. Subscriptions, backpressure, continuous setpoints, and mid-operation feedback have no home. This is O9, the ontology's true boundary. |
| Long-running operations (minutes-long robot motion) | Strains | Fits as a discrete Interaction only if occurrence semantics gain progress/cancellation. Can be deferred, but should be named now. |

---

## Weaknesses (Ontology)

- **O1. Execution is an orphaned responsibility.** The defining contradiction of the
  five-concept model. Must be explicitly modeled before the redesign proceeds; the
  shape of the modeling concept is redesign work (AD2), not an audit conclusion.
- **O2. Description vs occurrence is undistinguished** in Interaction; traces,
  retries, and results have nothing to attach to.
- **O3. Interaction cardinality is implicitly singular**; real domains routinely bind
  several subjects.
- **O4. Subjectless operations are unrepresentable** until Session is a concept and a
  valid subject.
- **O5. Results are unmodeled**; every reading/querying interaction is outside the
  model as written.
- **O6. Capability is one convenience away from becoming a factory**; the descriptive
  invariant must be written down or the implementation habit will become the ontology.
- **O7. Target identity (description vs handle) is undecided**, and half the failure
  scenarios hinge on it.
- **O8. Domain conflates vocabulary with implementation**, making engine plurality
  inexpressible and reproducing Part I's axis conflation at the conceptual level.
- **O9. Discrete-operation bias.** The model quietly assumes every interaction is
  bounded. True for a decade of automation domains; false for streaming and
  continuous control. An undeclared scope limit is a future architecture crisis;
  a declared one is a design decision.

---

## Recommendations (Ontology)

1. **Model the execution owner explicitly before the redesign proceeds** (O1/AD2).
   The audit establishes that the concept must exist and what it must satisfy
   (domain-provided, session-scoped, consumer-opaque, the sole producer of effects);
   ADR-021 chooses its name and shape from the candidates (Executor, Dispatcher,
   Interpreter, Domain Runtime, Operation Handler, per-interaction strategies).
   Naming this gap resolves O1 and O8 conceptually and is the fix for Part I
   priorities 1 and 3; choosing prematurely here would be design, not audit.
2. **Promote Session** to a first-class concept: user-visible, stateful, the unit of
   lifetime and isolation, bound to Domain(s) per the AD1 ruling, and a valid subject
   of Interactions (resolves O4).
3. **Narrow Runtime** to orchestration, validation, and observation dispatch. Strike
   "owns execution" (moves to the execution owner) and "owns lifecycle" as stated
   (moves to Session; Runtime owns only session creation and registry).
4. **Tighten Interaction:** immutable description; binds one or more subjects
   (Targets or the Session); requires capabilities per subject; may yield a Result as
   part of its contract; execution produces distinct occurrences (resolves O2, O3,
   O5). Do not introduce Result, Flow, or Trace as concepts; all three are derivable.
5. **Tighten Capability:** declarative only, never creates, never performs; a
   declared potential (validated per I12), distinct from momentary actionability,
   which belongs to the execution owner (resolves O6 and the dynamic-capability
   scenario). Record explicitly that Capability is the ontology's one
   non-observable, dependent concept and survives on the decoupling argument alone
   (see Concept Reality Tests).
6. **Decide Target = immutable description**, never a live handle; permit a parent
   reference for resolution scope; resolution to real referents belongs to the
   execution owner (resolves O7, composite and stateful scenarios).
7. **Redefine Domain** as the vocabulary-plus-performance extension unit: it defines
   Target kinds, Capabilities, and Interactions for one interaction medium, and ships
   one or more implementations of the execution-owner contract.
8. **Declare the scope limit:** the ontology models discrete, bounded interactions.
   Streaming, subscription, and continuous-control semantics are explicitly out of
   scope until a concrete domain requires them; when one does, extend occurrence
   semantics (progress, cancellation) rather than adding concepts (addresses O9
   honestly rather than speculatively).
9. **Adopt invariants I1-I7 and I9-I12** as the redesign's non-negotiables (several,
   I7/I10/I11, are direct generalizations of invariants VOID already enforces on the
   engine axis), and **resolve AD1-AD3 as explicit decisions in ADR-021** rather than
   inheriting them silently as if they were truths.

Six concepts, one named gap, eleven invariants, three open decisions, one declared
scope limit. Nothing else.

---

## Cross-references

- Seeded initiative: `docs/plan/draft/runtime-kernel-boundary/`
- Overlapping drafts: `docs/plan/draft/generalize-element-into-target/` (D1 partial;
  its `Target` root aligns with the ontology's Target concept),
  `docs/plan/draft/oop-violations-remediation/` (P8 covers part of D2)
- Prior audits: `docs/audits/ongoing/architecture-audit-2026-05.md` (C/H/M series),
  `docs/audits/backlog/domain-agnostic-runtime-audit-2026-06.md` (domain axis; its
  seven-primitive survival analysis is the implementation-level shadow of Part II)
- Backlog: `docs/audits/backlog/violations/core-driver-package-selenium-coupling.md` (D3)
