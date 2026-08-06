# M4 Full-System Audit

**Scope:** runtime-redesign M4 -- I5 (Session Model), I6 (Domain Registration), I7 (Locator Generalization)
**Date:** 2026-07-31
**Branch:** `initiative/domain-registration` (I6; I5 and I7 already merged to main)
**Test run:** 1100 examples, 0 failures, 0 errors, 0 skipped
**Verdict:** PASS

---

## Scope summary

M4 closes the domain-neutrality program across three initiatives:

| Initiative | Release | Status |
|---|---|---|
| I5 -- Session Model | v0.7.0 | Merged to main |
| I7 -- Locator Generalization | v0.8.0 | Merged to main |
| I6 -- Domain Registration | pending | On `initiative/domain-registration` (this audit verifies the combined state) |

Individual post-implementation audits (I5: 2026-07-29; I6: 2026-07-31; I7: 2026-07-30) verified each initiative in isolation. This audit verifies the combined state and checks for cross-initiative gaps not visible to individual audits.

---

## Architecture invariants (CLAUDE.md)

| Invariant | Axis | Verdict | Notes |
|---|---|---|---|
| `UIEngine` is the single execution authority | engine | PASS | No WebDriver calls outside `domain.automation.web.selenium.*`. ArchUnit `elementsApiIsSeleniumFree`, `uiActionsAreSeleniumFree` pass. |
| Engine-agnostic layers are Selenium-free | engine | PASS | `core.runtime`, `core.actions`, `core.flow`, `core.executor`, `core.context` import no Selenium. `VOID.getDriver()` and `Via` carry By via deprecated bridges (tracked, I9.3). |
| `LocatorDescriptor` is Selenium-free | engine | PASS | `domain.automation.web.locator.LocatorDescriptor` (final location, I6.4). ArchUnit `locatorDescriptorIsSeleniumFree` passes. |
| `ElementSupport` scope is frozen | scope | PASS | Three methods only: `nameOf`, `declaringClassOf`, `ordinalOf`. No additions since ADR-017. |
| `Target` carries no enum-specific defaults | scope | PASS | Unchanged throughout M4. |
| `VOIDBuilder` is single-use | scope | PASS | Guard unchanged. ADR-018 invariant intact. |
| Kernel purity | domain | PASS | `KernelBoundaryRulesTest.kernelPurity` green. All exceptions named and cross-referenced (see below). |

---

## Fitness-check coverage (ArchUnit)

All rules in `KernelBoundaryRulesTest` and `ElementStructureRulesTest` pass.

| Rule | Governs | Status |
|---|---|---|
| `loggingIsSeleniumFree` | `core.logging` | PASS |
| `flowIsSeleniumFree` | `core.flow` | PASS |
| `actionsAreSeleniumFree` | `core.actions` | PASS |
| `elementsApiIsSeleniumFree` | `domain.automation.web.vocabulary` | PASS |
| `runtimeDoesNotDeclareWebDriverField` | `core.runtime` fields | PASS |
| `runtimeDoesNotDeclareDriverContextField` | `core.runtime` fields | PASS |
| `locatorDescriptorIsSeleniumFree` | `LocatorDescriptor` | PASS |
| `coreEngineHasNoLocatorTypes` | `core.engine` | PASS |
| `actionsKernelIsTargetNeutral` | `core.actions` vs `UIElement` | PASS |
| `actionsKernelDoesNotDependOnElementRole` | `core.actions` vs `ElementRole` | PASS |
| `actionsKernelDoesNotDependOnCapabilityInterfaces` | `core.actions` vs capabilities | PASS |
| `kernelCapabilityReferencesAreContractTypedOnly` | kernel vs web capability types | PASS |
| `uiActionsAreSeleniumFree` | `domain.automation.web.vocabulary.actions` | PASS |
| `traceIsTargetNeutral` | `core.actions.trace` | PASS |
| `executorIsTargetNeutral` | `core.executor` | PASS |
| `flowIsTargetNeutral` | `core.flow` | PASS |
| `kernelHooksPackageDoesNotImportLegacyInteractions` | `core.actions.hooks` | PASS |
| `actionsKernelDoesNotImportLegacyInteractions` | `core.actions` vs `core.interactions` | PASS |
| `engineFactoryIsImplementationFree` | `UIEngineFactory` vs `domain.automation.web.selenium` | PASS |
| `domainRegistryIsImplementationFree` | `DomainRegistry` vs `domain.automation.web.selenium` | PASS |
| `executorContractIsNeutral` | `Executor` interface | PASS |
| `engineContractIsDriverFree` | `core.engine` vs driver layer | PASS |
| `kernelPackagesDoNotDependOnElements` | kernel cycle break (D1) | PASS |
| `kernelPurity` | consolidated kernel boundary | PASS |
| `elementEnumsMustBeNested` | `UIElement` enum nesting | PASS |

---

## Kernel purity exception inventory

All exceptions in `KERNEL_PURITY_TEMPORARY_EXCEPTIONS` are live, named, and cross-referenced:

| Exception | Load-bearing use | Closes |
|---|---|---|
| `core.engine.Executor` | `Action.perform()`, `ActionHandler.execute()`, `FlowExecutor` signatures | Permanent (neutral contract) |
| `domain.automation.web.engine.UIEngine` | `VOID.getEngine()`, `navigateTo`/`getCurrentUrl` lambdas, deprecated `Action.perform(UIEngine)` bridge | I9.3 / I9.4 |
| `domain.automation.web.locator.LocatorDescriptor` | `Action.resolve()`, `ActionHandler.execute()`, `HookChainAction` | I9.4 |
| `core.engine.DomainRegistry` | `VOIDBuilder` domain-selection wiring (I6.1) | Permanent (registration contract) |
| `core.engine.EngineBootstrap` | `VOIDBuilder` engine bootstrap | Migration Ledger |
| `domain.automation.web.engine.UIEngineFactory` | `VOIDBuilder` engine factory lookup | Migration Ledger |
| `core.utils.ConfigLoader` | `ActionProfiles` default profile, `FrameworkBootstrap` config paths | Not yet phased |
| `core.utils.ConfigPaths` | `FrameworkBootstrap` config paths | Not yet phased |
| `core.interactions.hooks.Before` / `After` | `ActionProfiles`/`Profiles` default hook constants | I2.1/I2.2 deferred |
| `core.interactions.Interactions` | `VOID.interaction()` deprecated bridge | I9.3 |
| `org.openqa.selenium.WebDriver` | `VOID.getDriver()` deprecated bridge | I9.3 |

No undocumented exceptions are present in the set.

---

## Cross-initiative interaction check

Interactions between I5, I6, and I7 that were not visible to individual audits:

**I5 x I7:** `SessionContext.engine()` returns `Executor` (I5.1); `Action.resolve()` carries `LocatorDescriptor` (I7.2 intermediate, I6.4 final home). These surfaces are disjoint -- the retype in I5 and the relocation in I7/I6 do not interact.

**I5 x I6:** `VOIDBuilder` gained domain-selection wiring in I6.1 after I5.1 removed the `UIEngine` field and `new VOID(ctx, engine)` constructor. The builder now calls `DomainRegistry.create(bootstrap)` and `new VOID(ctx)`. No tension: both changes target the same builder method sequence but at different call sites within it.

**I6 x I7:** `LocatorDescriptor` and `LocatorStrategy` were relocated twice -- first to `elements.locator` (I7.2), then to their final home `domain.automation.web.locator` (I6.4). The fitness check `locatorDescriptorIsSeleniumFree` was updated to the final FQN. All production call sites reference the final location.

**Probe gate:** `StoreDomainNeutralityTest` (I6.3) exercises the full session lifecycle without any `domain.automation.web.selenium` or `elements.*` dependencies. Passes in every test run.

---

## By-returning surface inventory

`org.openqa.selenium.By` outside `domain.automation.web.selenium.*`:

| Location | Status |
|---|---|
| `core.bridge.selenium.SeleniumLocatorBridge` | Active bridge; By-to-LocatorDescriptor translation layer (ADR-019). Not a violation. |
| `domain.automation.web.resolve.api.LocatorResolver.resolve()` | `@Deprecated(forRemoval=true)`; tracked I7 G1; deletes I9.3 alongside Via/Interactions removal. |
| `domain.automation.web.resolve.parser.ByParser` / `ByPrefixStrategy` | `@Deprecated`; tracked I7 G2; deletes I9.3. |
| `domain.automation.web.resolve.properties.PropertiesFileLocatorReader` | Calls deprecated `ByParser`; legacy bridge; deletes I9.3. |
| `core.interactions.Via` | `@Deprecated`; tracked; deletes I9.3. |
| `core.utils.web.*` (WaitUtils, KeyValuePairHandler, Upload, TableHandler) | Deprecated ADR-020 utilities; delete I9.2. |
| `core.utils.EnumResolver` | Deprecated utility; deletes I9.2/I9.3. |
| `core.context.ExecutionContext` | Legacy frozen type; deletes I9.3. |

All instances are deprecated or bridge-layer. No new By-returning surface was added in M4.

---

## OOP violation inventory (M4-relevant)

| ID | Status in M4 | Notes |
|---|---|---|
| P8 | FIXED (I4.1) | `switch` on engine name removed. `engineFactoryIsImplementationFree` enforces. |
| P11 | DEFERRED | Per-capability static helpers in `Via`; deferred to I9.3. |
| G-I6-2 | LOGGED | `SeleniumDriverFactory` instanceof preference dispatch; backlog entry created. |

No new OOP violations introduced in M4.

---

## ADR inventory for M4

| ADR | Status | Notes |
|---|---|---|
| ADR-022 (Session Model) | Accepted -- promoted this audit | Was in pending-review after v0.7.0 merge; promotion was missed. Fixed inline. |
| ADR-023 (Locator Generalization) | Accepted | Correctly promoted when I7 merged (v0.8.0). |
| ADR-024 (Domain Registration) | Pending Review | Awaiting I6 merge. |

---

## Tracked open gaps across M4

All gaps are pre-existing, named, and cross-referenced to closing phases:

| Gap | Initiative | Closes |
|---|---|---|
| R3: Hook contract for session-subject operations | I5 | I8.2 |
| G1: `Waiter` returns `WebDriverWait` (ADR-007) | I6 | Post-I9 (dedicated fix) |
| G2: `SeleniumDriverFactory` instanceof dispatch (P-OCP) | I6 | Backlog |
| G3: UIEngine / LocatorDescriptor kernel bridge exceptions | I6/I7 | I9.4 |
| G1: `LocatorResolver.resolve()` not deleted | I7 | I9.3 |
| G2: `ByParser`/`ByPrefixStrategy` not moved/deleted | I7 | I9.3 |

No new gaps opened by the combined M4 state.

---

## Verdict

All architecture invariants hold in the combined M4 state. All 25 ArchUnit fitness checks pass. The 1100-test suite is green. All tracked gaps are properly named and deferred. One documentation maintenance issue (ADR-022 promotion) was found and corrected inline.

M4 is PASS. No hotfix initiative is warranted. The branch is ready for merge.
