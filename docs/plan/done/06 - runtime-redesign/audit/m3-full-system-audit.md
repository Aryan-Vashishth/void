# M3 Full-System Audit -- Execution Boundary (I4.1-I4.5)

**Milestone:** M3 (runtime-redesign I4 -- Execution Boundary)
**Date:** 2026-07-29
**Branch:** `initiative/runtime-redesign`
**Scope:** I4.1 Engine Registry, I4.2 EngineBootstrap Decoupling,
I4.3 Neutral Executor Contract, I4.4 Kernel Retyping, I4.5 Driver Isolation
**Verdict:** PASS -- all five deliverables met; kernel purity gate maintained;
two deprecated bridge sets survive as planned with named closing phases

---

## Deliverables Verified

| Initiative | Key deliverable | Status |
|---|---|---|
| I4.1 -- Engine Registry | `UIEngineFactory` replaced with `EngineRegistrar` SPI; no `core.engine.selenium` compile-time import in the contract package | PASS |
| I4.2 -- EngineBootstrap Decoupling | `EngineBootstrap` carries engine name + opaque settings; zero `core.driver` imports in `core.engine` | PASS |
| I4.3 -- Neutral Executor Contract | `Executor` interface introduced in `core.engine`; `UIEngine extends Executor`; three abstract methods (`initialize`, `shutdown`, `getEngineName`) | PASS |
| I4.4 -- Kernel Retyping | `Action.perform(Executor)`, `ActionHandler.execute(Executor, ...)`, `FlowExecutor(Executor)` are the primary signatures; UIEngine-typed deprecated bridges present and delegating correctly | PASS |
| I4.5 -- Driver Isolation Rule | `DriverIsolationRulesTest.driverIsolation()` ArchUnit rule enforced; 8 named current violators with closing phases; `EnumResolver.printEnumFormat` parameterized to remove Waiter dependency | PASS |

---

## Architecture Invariant Check

| Invariant | Axis | Status |
|---|---|---|
| `UIEngine` is the single execution authority (ADR-007) | engine | PASS -- no outside WebDriver calls; UIEngine internal methods unchanged |
| Engine-agnostic layers are Selenium-free (ADR-018) | engine | PASS -- `core.engine` is now Selenium-free; bridges in kernel are temporary and named |
| `LocatorDescriptor` is Selenium-free (ADR-019) | engine | PASS -- unchanged; verified no regression |
| `ElementSupport` scope is frozen (ADR-017) | scope | PASS -- no additions |
| `Target` carries no enum-specific defaults | scope | PASS -- unchanged |
| `VOIDBuilder` is single-use (ADR-018) | scope | PASS -- unchanged |
| Kernel purity (ADR-021, I2.4) | domain | PASS -- `KernelBoundaryRulesTest.kernelPurity` green; `Executor` and `UIEngine` both in exception set with documented reasons |

---

## Audit D2 / D3 Resolution Confirmed

Both findings from `docs/audits/ongoing/architecture-audit-2026-07-domain-model.md`
are resolved by this milestone and annotated in the ongoing audit:

- **D2** (contract depends on implementation): `UIEngineFactory` switch-on-string (P8)
  replaced by `EngineRegistrar` SPI in I4.1. `core.engine` imports `core.engine.selenium`
  nowhere. P8 closed in `docs/contributing/coding-standards.md` OOP violations table.
- **D3** (contract depends on driver infrastructure): `EngineBootstrap` redesigned in I4.2.
  `core.engine` imports `core.driver` nowhere (confirmed: ArchUnit `kernelPurity` exception
  set retains `core.driver.DriverFactory` for `core.bootstrap` only, not `core.engine`).

---

## Kernel Purity Gate State

`KernelBoundaryRulesTest.KERNEL_PURITY_TEMPORARY_EXCEPTIONS` as of M3:

```
core.engine.Executor          -- primary kernel execution-owner contract
core.engine.UIEngine          -- (a) deprecated bridges in Action/ActionHandler;
                                 (b) VOID/SessionContext/VOIDBuilder (out of I4.4 scope)
core.engine.LocatorDescriptor -- UI-domain vocabulary; generalised in I7
core.engine.EngineBootstrap   -- migration parameter object; closes with I5.1 Session contract
core.engine.UIEngineFactory   -- retained for VOIDBuilder call site; closes with I6.4 registry
core.driver.DriverFactory     -- bootstrap only; closes 9.3
core.driver.DriverFactory$Profile -- VOIDBuilder public API; closes with I6.4 API decision
core.utils.ConfigLoader       -- framework init; closes with I6 or 9.x
core.utils.ConfigPaths        -- same as ConfigLoader
core.interactions.hooks.Before  -- stable hook library; closes with I8/I9 hooks migration
core.interactions.hooks.After   -- same as Before
core.interactions.Interactions  -- deprecated; closes with I9 legacy removal
org.openqa.selenium.WebDriver   -- VOID.getDriver() deprecated bridge; closes 9.3
```

No exception was added by I4.4 beyond what the gate already tracked. The exception
count is the same as at M2; no new permanent exceptions were introduced.

---

## Driver Isolation Gate State

`DriverIsolationRulesTest.DRIVER_ISOLATION_EXCEPTIONS` as of M3 (new gate, I4.5):

| Class | Closing phase |
|---|---|
| `core.interactions.Via` | I9.x (Via/Interactions cleanup) |
| `core.utils.web.WaitUtils` | I9.2 (Migration Ledger, utils dismantling) |
| `core.utils.web.DOMUtils` | I9.2 |
| `core.utils.web.TableHandler` | I9.2 |
| `core.utils.web.KeyValuePairHandler` | I9.2 |
| `core.utils.web.Upload` | I9.2 |
| `core.runtime.VOID` | I9.3 (deprecated `start(Profile)` static factory) |
| `core.runtime.VOIDBuilder` | I6.4 (API surface decision for `profile(DriverFactory.Profile)`) |

All exceptions are `@Deprecated` classes or classes with a documented blocking decision.
No production-path non-deprecated code outside `core.engine.selenium.*` imports `core.driver.*`
except the eight named entries above.

---

## Bridge Discipline Confirmed

I4.4 introduced two deprecated bridge sets:

**`Action` interface:**
- Primary SAM: `void perform(Executor executor)` (abstract)
- Bridge: `default void perform(UIEngine engine) { perform((Executor) engine); }` -- delegates
- Bridge: `default void resolve(Executor executor)` (primary)
- Bridge: `default void resolve(UIEngine engine) { resolve((Executor) engine); }` -- delegates

**`ActionHandler` interface:**
- Primary SAM: `void execute(Executor executor, @Nullable LocatorDescriptor descriptor)` (abstract)
- Bridge: `default void execute(UIEngine engine, ...) { execute((Executor) engine, descriptor); }` -- delegates

Both bridge methods are `@Deprecated(forRemoval = true)` and delegate to the primary.
No bridge duplicates logic. `HookBridgeCompatibilityTest` verifies round-trip delegation.

---

## Risks Accepted

| Risk | Accepted at | Closing |
|---|---|---|
| UIEngine in kernel purity exception set | I4.4 | deprecated bridges close I9.4; VOID/SessionContext/VOIDBuilder close I5/I6 |
| 8 driver isolation exceptions | I4.5 | each named above with closing phase |
| VOIDBuilder public `profile(DriverFactory.Profile)` API | I4.5 | I6.4 API surface decision |

---

## Tests Green

- `KernelBoundaryRulesTest.kernelPurity` -- PASS
- `DriverIsolationRulesTest.driverIsolation` -- PASS
- `HookBridgeCompatibilityTest` -- PASS (bridge delegation verified)
- `ActionProfilesTest`, `HookPipelineTest`, `ActionTraceTest` -- PASS
- `ConcreteActionsTest`, `ElementActionTest` -- PASS (17+ Executor-typed perform calls)
- Full suite green at HEAD of `initiative/runtime-redesign`

---

## Remaining Open Audit Findings

Findings in `docs/audits/ongoing/architecture-audit-2026-07-domain-model.md` not
addressed by M3 (tracked for future initiatives):

| ID | Finding | Addressed by |
|---|---|---|
| D1 (two-axis conflation) | Architecture must distinguish engine vs domain neutrality | Ongoing -- ADR-021 two-axis model documents it; I5+ implements domain registration |
| D5 | `dsl.VoidDSL` -> `Interactions` (legacy gravitational pull) | I8/I9 |
| D6 | `core.utils` inverted dependency profile | I9.2 |
| D7 | `UIEngine.resolve(Element, ...)` backdoor | I7 |
| D8-D12 | Vocabulary overloads (Context x5, Profile x3, etc.) | I5+ vocabulary alignment |
| D13 | Abstraction introduction asymmetry | I6+ |
| D14 | Legacy boundary marking inconsistent (`EnumResolver` live Selenium) | I9.2 |
| D15 | Resolution ownership (three owners) | I7 |
| D17 | Single-artifact enforcement | I6+ (ArchUnit ratchet per initiative is the interim mitigation) |
