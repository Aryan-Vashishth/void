# I4 Pre-Implementation Audit -- Execution Boundary

**Date:** 2026-07-28
**Branch:** `initiative/runtime-redesign`
**Scope:** Current state of the execution seam before I4 begins. Maps every finding to the phase that owns it.

---

## A1 -- UIEngineFactory: switch on engine name + direct SeleniumEngine coupling (4.1)

**File:** `core/engine/UIEngineFactory.java`

`UIEngineFactory.create()` dispatches on a name string via a `switch` expression (lines 44-56) with a hardcoded `case "selenium"` arm that instantiates `SeleniumEngine` directly:

```java
UIEngine engine = switch (engineName) {
    case "selenium" -> {
        if (bootstrap instanceof EngineBootstrap.FromProfile fp) {
            yield new SeleniumEngine(fp.profile());
        } else {
            throw new IllegalStateException(...);
        }
    }
    default -> throw new IllegalStateException(
            "Unsupported engine: '" + engineName + "'. Supported: selenium");
};
```

Three coupling points:
1. `import core.engine.selenium.SeleniumEngine` -- the contract package imports its own implementation.
2. `instanceof EngineBootstrap.FromProfile` -- adding a second engine requires a new bootstrap variant, opening the sealed hierarchy, and adding another switch arm.
3. Error message hardcodes the engine list (`"Supported: selenium"`).

This is P8 (OCP: open for extension by adding a registration, not by editing the switch). Phase 4.1 owns the fix. Cross-reference: audit finding D2 (`docs/audits/ongoing/architecture-audit-2026-07-domain-model.md`).

---

## A2 -- EngineBootstrap: sealed interface carrying a driver-layer type (4.2)

**File:** `core/engine/EngineBootstrap.java`

The kernel bootstrap token is a sealed interface with one permitted record:

```java
public sealed interface EngineBootstrap permits EngineBootstrap.FromProfile {
    record FromProfile(DriverFactory.Profile profile) implements EngineBootstrap {}
    static EngineBootstrap fromProfile(DriverFactory.Profile profile) { ... }
}
```

`DriverFactory.Profile` is a driver-layer type (`core.driver`). Its presence on the sealed interface means:
- `core.engine` imports `core.driver` (line 3).
- Every call site that pattern-matches on `EngineBootstrap` is coupled to the driver-layer type.
- A non-Selenium engine cannot produce a `FromProfile` bootstrap without depending on `DriverFactory`.

Phase 4.2 replaces `FromProfile(DriverFactory.Profile)` with an opaque, engine-owned settings map. `sealed` remains but the driver-layer type leaves the public surface. Cross-reference: audit D3.

---

## A3 -- VOIDBuilder / VOID: DriverFactory.Profile on the public API (4.2 / 4.5)

**Files:** `core/runtime/VOIDBuilder.java`, `core/runtime/VOID.java`

Both import `core.driver.DriverFactory` for the `Profile` type:

| File | Usage |
|---|---|
| `VOIDBuilder.java:5` | `import core.driver.DriverFactory` |
| `VOIDBuilder.java:41` | `private DriverFactory.Profile profile` field |
| `VOIDBuilder.java` | `.profile(DriverFactory.Profile)` public method |
| `VOID.java:6` | `import core.driver.DriverFactory` |

Framework users calling `VOID.builder().profile(DriverFactory.Profile.DEFAULT)` must import from `core.driver` just to start a session. The driver-layer type has escaped from the Selenium boundary onto the public startup surface.

Phase 4.2 removes `DriverFactory.Profile` from `EngineBootstrap`; once that is done, `VOIDBuilder.profile()` can accept an opaque settings object or engine-owned type and drop the driver import. Phase 4.5 then enforces `core.driver` as Selenium-executor-internal only via a fitness check.

---

## A4 -- Action.perform, FlowExecutor, ActionHandler: UIEngine-typed kernel signatures (4.4)

**Files:** `core/actions/Action.java`, `core/executor/FlowExecutor.java`, `core/actions/hooks/ActionHandler.java`

All three kernel execution edges accept `UIEngine` directly:

| Type | Signature | Line |
|---|---|---|
| `Action` | `void perform(UIEngine engine)` | 54 |
| `Action` | `default LocatorDescriptor resolve(UIEngine engine)` | 67 |
| `FlowExecutor` | `public FlowExecutor(UIEngine engine)` | 30 |
| `ActionHandler` | `void execute(UIEngine engine, @Nullable LocatorDescriptor descriptor)` | 66 |

`ActionHandler`'s own Javadoc (lines 43-45) documents this explicitly:
> "The contract still references UIEngine and LocatorDescriptor directly -- both domain-side types today; retyping this signature against the neutral Executor contract is I4's job, not this move's."

No overloads exist on any of these signatures. Phase 4.4 retypes them against the neutral contract introduced in 4.3, with UIEngine-typed overloads kept as deprecated bridges. The kernel suite and existing test code compile unchanged through the bridge period.

---

## A5 -- Neutral contract insertion point: confirmed clean (4.3)

Phase 4.3 introduces a new type above `UIEngine` in `core.engine` (or a kernel-adjacent package per ADR-021 AD2). The kernel purity gate (`KernelBoundaryRulesTest.kernelPurity`, I2.4) already enforces that `core.actions`, `core.flow`, `core.executor`, `core.context`, `core.runtime`, and `core.bootstrap` carry no `elements.*` or Selenium imports. The gate does not yet reference the neutral contract (which does not exist), so adding it is additive and will not break the gate. The fitness check for 4.3 (neutral contract imports no Selenium, no `elements.*`, no UI vocabulary) is a new check, not a relaxation of an existing one.

No pre-existing coupling to address before 4.3 can land.

---

## Risk register

| Risk | Phase | Severity | Mitigation |
|---|---|---|---|
| Neutral contract grows a UI method before a second domain demands it | 4.3 | Critical | Fitness check forbids UI vocabulary on the neutral type. Promotion requires a second domain's demand (extension before modification). |
| Bridge signatures in 4.4 duplicate rather than delegate the execution path | 4.4 | High | Bridge-delegation test: both signatures must reach the same pipeline. Commit review gate. |
| EngineBootstrap settings replacement becomes stringly-typed | 4.2 | Medium | Settings are engine-owned by contract, documented as such. No open `Map<String, Object>` on the public surface. |
| Fitness check for 4.5 (core.driver Selenium-only) breaks existing test infrastructure that imports DriverFactory directly | 4.5 | Low | The rule constrains import direction, not existence. Selenium-specific test infra may keep importing from the platform side; the rule only prevents framework-layer code from importing it. |

---

## Verdict

**READY TO PROCEED.** All four I4 findings (A1-A4) are contained within the engine/runtime seam as expected by the plan. Kernel packages (actions, flow, executor, context, bootstrap) are clean. The neutral contract insertion point (4.3) is unobstructed. No scope changes to the I4 plan are required.

Implementation order: 4.1 -> 4.2 -> 4.3 -> 4.4 -> 4.5, per plan dependencies.
