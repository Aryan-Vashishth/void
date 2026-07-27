# Action Layer Architecture

VOID's **deferred execution model** — a typed hierarchy of composable `Action` objects that represent UI operations without executing them — is split across two packages: `core.actions` (kernel contracts) and `elements.api.actions` (concrete UI actions). Actions are the primary abstraction between element declaration (what exists) and engine execution (how the browser interaction happens).

---

## Table of Contents

1. [Overview](#overview)
2. [Kernel / UI-Domain Boundary](#kernel--ui-domain-boundary)
3. [Class Hierarchy](#class-hierarchy)
4. [Template Method Pattern](#template-method-pattern)
5. [Concrete Action Families](#concrete-action-families)
6. [ActionProfiles — Profile Constants](#actionprofiles--profile-constants)
7. [Profiles — Public Presets](#profiles--public-presets)
8. [operationLabel Derivation](#operationlabel-derivation)
9. [ActionCapability — Metadata Enum](#actioncapability--metadata-enum)
10. [ElementActions Factory](#elementactions-factory)
11. [Custom Hook Libraries](#custom-hook-libraries)
12. [Extension Guide](#extension-guide)

---

## Overview

Actions are **intent objects** — they capture what should happen (click this element, type this text) without performing it. Execution is deferred until `action.perform(engine)` is called, typically by `FlowExecutor`.

The action layer is governed by three ADRs:

- **ADR-012 (ElementActions Factory Scope)** — `ElementActions.of()` is `@Internal`; only test infrastructure may use it.
- **ADR-013 (Architectural Layering Principle)** — Capabilities describe structure; actions declare execution policy. Hooks, waits, and retries live in the action layer, never in capability interfaces.
- **ADR-014 (Concrete Actions over Anonymous Lambdas)** — Every UI operation is a named, typed class — not an `Action` lambda. Named classes produce traceable operations, profileable behavior, and meaningful labels in observability output.

---

## Kernel / UI-Domain Boundary

The action layer is physically split across two packages, per ADR-021's kernel
membership list and the `runtime-redesign` roadmap's Initiative I2 (Kernel Extraction):

- **`core.actions`** (kernel, domain-neutral, per ADR-021): `Action`, `ActionCapability`,
  `ActionProfile`, `ActionProfiles`, `Profile`, `Profiles`, `HookChainAction`. These
  reference `core.target.Target` at most — never `elements.api.UIElement`,
  `elements.meta.ElementRole`, or `elements.api.capability` — so the kernel can be
  pointed at without dragging the UI element model along.
- **`elements.api.actions`** (UI-domain): `ElementAction` and its three abstract family
  intermediaries (`ClickableElementAction`, `TypeableElementAction`,
  `SelectableElementAction`), the 17 concrete leaf action classes, the `ElementActions`
  factory, and `CapabilityProfiles` (capability-specific profile constants). These are
  genuinely UI-specific — `ElementAction.resolve()` calls `UIEngine.resolve(UIElement,
  ElementRole, ...)`, which requires the UI-domain locator model.

This split happened in two stages:

1. **`runtime-redesign` I1, phase 1.4** ("kernel target-neutrality"): auditing
   `core.actions` found the kernel types were already `UIElement`-free (a consequence of
   I1's earlier `Target` extraction), so the phase encoded the invariant as an automated
   ratchet rather than moving code. At that point the UI-domain content was still
   physically co-located in `core.actions`, exempted from the ratchet by name.
2. **`runtime-redesign` I2, phase 2.2** ("kernel/UI action split"): the physical
   relocation. `ElementAction` and its entire family moved to `elements.api.actions`;
   `ActionProfiles`' capability-specific constants (`CLICKABLE_SAFE`, `TYPEABLE_SAFE`,
   etc.) extracted into the new `elements.api.actions.CapabilityProfiles`, leaving
   `ActionProfiles` with only the domain-neutral `DEFAULT_SAFE`/`DEFAULT_RELIABLE` and
   the config-driven default-selection mechanism.

`KernelBoundaryRulesTest` (`src/test/java/core/architecture/`) enforces the boundary with
no exemptions needed anymore: `core.actions` (root package) may never depend on
`UIElement`, `ElementRole`, or `elements.api.capability`; `core.actions.trace`,
`core.executor`, and `core.flow` may never depend on `UIElement` either;
`elements.api.actions` must stay Selenium-free.

**What remains a documented, deliberate gap (not fixed by 2.2):** `ActionProfiles` and
`Profiles` still import `core.interactions.hooks.Before`/`After` directly for their
domain-neutral `DEFAULT_SAFE`/`DEFAULT_RELIABLE` and `RAW`/`DEBUG`/`FAST`/`VISUAL`
presets. This is a separate, smaller tension (the kernel's one universal default profile
happens to be implemented with a UI-domain hook constant) than the capability-specific
constants 2.2 moved out; it is excluded by name from `KernelBoundaryRulesTest`'s
`core.interactions` check, same as before 2.2.

---

## Class Hierarchy

All classes below live in `elements.api.actions`, not `core.actions`.

```
ElementAction (abstract, public)
├── ClickableElementAction (abstract, package-private)
│   ├── ClickAction (public final)
│   ├── ToggleAction (public final)
│   └── CheckAction (public final)
├── TypeableElementAction (abstract, package-private)
│   ├── TypeAction (public final)
│   ├── ClearAction (public final)
│   ├── AppendTypeAction (public final)
│   ├── TypeAndPressAction (public final)
│   ├── TypeSearchAction (public final)
│   └── SubmitSearchAction (public final)
├── SelectableElementAction (abstract, package-private)
│   ├── OpenAction (public final)
│   ├── SelectAction (public final)
│   ├── SelectByTextAction (public final)
│   ├── SelectByValueAction (public final)
│   └── SearchAndSelectAction (public final)
├── HoverAction (public final)       — extends ElementAction directly, DEFAULT profiles
├── UploadAction (public final)      — extends ElementAction directly, DEFAULT profiles
└── ReadTextAction (public final)    — extends ElementAction directly, DEFAULT profiles
```

**Abstract intermediaries** (`ClickableElementAction`, `TypeableElementAction`, `SelectableElementAction`) are package-private. They centralize the `defaultSafeProfile()` and `defaultReliableProfile()` overrides for their family, eliminating 28 lines of repeated boilerplate across the 14 concrete classes that previously each overrode these two methods.

Concrete classes extend the appropriate intermediary and implement only `execute()`. The three classes that extend `ElementAction` directly (`HoverAction`, `UploadAction`, `ReadTextAction`) use `DEFAULT_SAFE` / `DEFAULT_RELIABLE` — the same profiles `ElementAction` defaults to — so they gain nothing from an intermediary.

---

## Template Method Pattern

`ElementAction` follows the Template Method pattern. `perform()` is `final` and defines the invariant sequence:

```
perform(engine)
  ├── resolve(engine)          → LocatorDescriptor  (engine.resolve(element, role))
  └── execute(engine, desc)   ← abstract; implemented by each concrete class
```

**Final fluent APIs** on `ElementAction` (cannot be overridden):

| Method | Effect |
|--------|--------|
| `safely()` | Wraps the action with the family-specific SAFE profile |
| `reliable()` | Wraps the action with the family-specific RELIABLE profile |
| `debug()` | Applies `Profiles.DEBUG` |
| `raw()` | Applies `Profiles.RAW` — no hooks at all |
| `using(ActionProfile)` | Applies an arbitrary profile |
| `before(ActionHandler...)` | Prepends manual before hooks |
| `after(ActionHandler...)` | Appends manual after hooks |

**Abstract hook methods** (overridden by intermediaries, not by concrete classes):

| Method | Default in `ElementAction` | Overridden in |
|--------|--------------------------|---------------|
| `defaultSafeProfile()` | `ActionProfiles.DEFAULT_SAFE` | each intermediary |
| `defaultReliableProfile()` | `ActionProfiles.DEFAULT_RELIABLE` | each intermediary |

`operationLabel()` returns a human-readable name derived from the class name — see [operationLabel Derivation](#operationlabel-derivation).

---

## Concrete Action Families

### Click Family — `ClickableElementAction`

Profiles: `ActionProfiles.CLICKABLE_SAFE` / `ActionProfiles.CLICKABLE_RELIABLE`

| Class | Emitted by | Engine call | Role | Capability |
|-------|-----------|-------------|------|------------|
| `ClickAction` | `Clickable.click()` | `engine.click()` | `TRIGGER` | `CLICKABLE` |
| `ToggleAction` | `Checkable.toggle()` | `engine.click()` | `PRIMARY` | `CHECKABLE` |
| `CheckAction` | `Checkable.set(boolean)` | conditional `engine.click()` | `PRIMARY` | `CHECKABLE` |

### Type Family — `TypeableElementAction`

Profiles: `ActionProfiles.TYPEABLE_SAFE` / `ActionProfiles.TYPEABLE_RELIABLE`

| Class | Emitted by | Engine call | Role | Capability |
|-------|-----------|-------------|------|------------|
| `TypeAction` | `Typeable.type(String)` | `engine.type()` | `INPUT` | `TYPEABLE` |
| `ClearAction` | `Typeable.clear()` | `engine.clear()` | `INPUT` | `TYPEABLE` |
| `AppendTypeAction` | `Typeable.append(String)` | `engine.type()` without prior clear | `INPUT` | `TYPEABLE` |
| `TypeAndPressAction` | `Typeable.typeAndPress(String, Key)` | `engine.type()` + `engine.pressKey()` | `INPUT` | `TYPEABLE` |
| `TypeSearchAction` | `SearchField.typeSearch(String)` | `engine.type()` | `SEARCH_INPUT` | `SEARCH_FIELD` |
| `SubmitSearchAction` | `SearchField.submitSearch()` | `engine.click()` | `SEARCH_INPUT` | `SEARCH_FIELD` |

### Select Family — `SelectableElementAction`

Profiles: `ActionProfiles.SELECTABLE_SAFE` / `ActionProfiles.SELECTABLE_RELIABLE`

| Class | Emitted by | Engine call | Role | Capability |
|-------|-----------|-------------|------|------------|
| `OpenAction` | `Selectable.open()` | `engine.click()` | `TRIGGER` | `SELECTABLE` |
| `SelectAction` | `Selectable.select()` | click trigger + wait for overlay + click list | `TRIGGER` | `SELECTABLE` |
| `SelectByTextAction` | `Selectable.selectByText(String)` | `engine.selectByVisibleText()` | `LIST` | `SELECTABLE` |
| `SelectByValueAction` | `Selectable.selectByValue(String)` | `engine.selectByValue()` | `LIST` | `SELECTABLE` |
| `SearchAndSelectAction` | `SearchableDropdown.searchAndSelect(String)` | click + type + wait for result + click result | `TRIGGER` | `SEARCHABLE_DROPDOWN` |

### Default Family — direct `ElementAction` subclasses

Profiles: `ActionProfiles.DEFAULT_SAFE` / `ActionProfiles.DEFAULT_RELIABLE`

| Class | Emitted by | Engine call | Role | Capability |
|-------|-----------|-------------|------|------------|
| `HoverAction` | `Hoverable.hover()` | `engine.hover()` | `PRIMARY` | `HOVERABLE` |
| `UploadAction` | `Uploadable.upload(Path)` | `engine.upload()` | `PRIMARY` | `UPLOADABLE` |
| `ReadTextAction` | `ReadOnly.readText()` | `engine.getText()` | `TEXT` | `READ_ONLY` |

---

## ActionProfiles — Profile Constants

Profile constants are split across two classes, per the kernel/UI-domain boundary:

- **`core.actions.ActionProfiles`** (public since I2.2, was package-private): owns only
  the domain-neutral `DEFAULT_SAFE`/`DEFAULT_RELIABLE` pair, plus the config-driven
  default-selection mechanism. Referenced across the kernel/UI-domain boundary by
  `elements.api.actions.ElementAction` (defaults) and `ElementActions` (config-driven
  wrapping).
- **`elements.api.actions.CapabilityProfiles`** (package-private, new in I2.2): owns the
  6 capability-specific constants — one SAFE/RELIABLE pair per action family. Referenced
  only by the 3 abstract family intermediaries in the same package.

### SAFE Profiles

| Constant | Owner | Before hooks | After hooks |
|----------|-------|-------------|------------|
| `DEFAULT_SAFE` | `core.actions.ActionProfiles` | `WAIT_FOR_ELEMENT_VISIBLE` | — |
| `CLICKABLE_SAFE` | `CapabilityProfiles` | `WAIT_FOR_ELEMENT_CLICKABLE` | `WAIT_FOR_ANGULAR_LOADER`, `HIGHLIGHT_ELEMENT` |
| `TYPEABLE_SAFE` | `CapabilityProfiles` | `CLEAR_FIELD`, `WAIT_FOR_ELEMENT_VISIBLE` | `HIGHLIGHT_ELEMENT` |
| `SELECTABLE_SAFE` | `CapabilityProfiles` | `WAIT_FOR_ELEMENT_VISIBLE`, `WAIT_FOR_ELEMENT_CLICKABLE`, `WAIT_FOR_ANGULAR_LOADER` | `HIGHLIGHT_ELEMENT` |

### RELIABLE Profiles

Reliable profiles extend their SAFE counterpart with loader waits on both sides. All four RELIABLE constants share the same after-hook set.

| Constant | Owner | Before hooks | After hooks |
|----------|-------|-------------|------------|
| `DEFAULT_RELIABLE` | `core.actions.ActionProfiles` | `WAIT_FOR_ELEMENT_VISIBLE` | `WAIT_FOR_ANGULAR_LOADER`, `WAIT_FOR_SPIN_SPINNER_LOADER`, `HIGHLIGHT_ELEMENT` |
| `CLICKABLE_RELIABLE` | `CapabilityProfiles` | `WAIT_FOR_ANGULAR_LOADER`, `WAIT_FOR_ELEMENT_CLICKABLE` | `WAIT_FOR_ANGULAR_LOADER`, `WAIT_FOR_SPIN_SPINNER_LOADER`, `HIGHLIGHT_ELEMENT` |
| `TYPEABLE_RELIABLE` | `CapabilityProfiles` | `WAIT_FOR_ANGULAR_LOADER`, `WAIT_FOR_ELEMENT_VISIBLE`, `CLEAR_FIELD` | `WAIT_FOR_ANGULAR_LOADER`, `WAIT_FOR_SPIN_SPINNER_LOADER`, `HIGHLIGHT_ELEMENT` |
| `SELECTABLE_RELIABLE` | `CapabilityProfiles` | `WAIT_FOR_ANGULAR_LOADER`, `WAIT_FOR_ELEMENT_VISIBLE`, `WAIT_FOR_ELEMENT_CLICKABLE` | `WAIT_FOR_ANGULAR_LOADER`, `WAIT_FOR_SPIN_SPINNER_LOADER`, `HIGHLIGHT_ELEMENT` |

### Config-Driven Default

`core.actions.ActionProfiles.applyConfiguredDefault(action)` reads the `void.profile.default` key from `ConfigLoader`. When set to `SAFE`, `RELIABLE`, `DEBUG`, `FAST`, or `VISUAL`, it wraps every outgoing action in that profile automatically. Defaults to `RAW` (no wrapping) when the key is absent or unknown.

---

## Profiles — Public Presets

`Profiles` (public class) exposes engine-agnostic presets that any action can adopt via `action.using(Profiles.X)`:

| Constant | Description |
|----------|------------|
| `RAW` | No hooks — direct execution, no waits or highlights. |
| `DEBUG` | Adds `HIGHLIGHT_ELEMENT` + `LOG_INTENT` before and after. |
| `FAST` | Minimal hooks — skips waits; for speed-sensitive flows. |
| `VISUAL` | Adds scroll + highlight for visual verification runs. |

> `Profiles.SAFE` and `Profiles.RELIABLE` were removed in this branch. Capability-specific profiles are now accessed through `.safely()` and `.reliable()` on the action itself, which dispatch to the correct `ActionProfiles` constant for the action's family. Generic capability-blind profiles are no longer needed.

---

## operationLabel Derivation

`ElementAction.operationLabel()` produces the human-readable name used in `ActionTrace` records and log output. Derivation rules:

1. Take the simple class name (e.g., `ClickAction`).
2. Strip a trailing `Action` suffix if present (→ `Click`).
3. Lowercase the first character (→ `click`).
4. Anonymous classes and lambda-wrapped actions return `"perform"`.

| Class | `operationLabel()` |
|-------|--------------------|
| `ClickAction` | `click` |
| `TypeAction` | `type` |
| `SelectByTextAction` | `selectByText` |
| `SearchAndSelectAction` | `searchAndSelect` |
| `ReadTextAction` | `readText` |
| `HoverAction` | `hover` |
| anonymous lambda | `perform` |

This label appears in `ActionTrace.operation` and `ActionTraceLogger` output — it is the primary human-readable identity of an action in observability tooling.

---

## ActionCapability — Metadata Enum

`ActionCapability` is a **metadata-only** enum. It identifies what kind of operation an action represents for observability, logging, and tracing. It is **never used for execution dispatch**.

Full value set: `CLICKABLE`, `TYPEABLE`, `SELECTABLE`, `HOVERABLE`, `CHECKABLE`, `UPLOADABLE`, `SEARCHABLE`, `SEARCH_FIELD`, `SEARCHABLE_DROPDOWN`, `READ_ONLY`, `TABLE`, `EDITABLE_TABLE`, `LISTABLE`, `MULTI_SELECTABLE`, `UNKNOWN`.

Each concrete action declares its capability in the `super(element, role, capability)` constructor call. The value flows through to `ActionTrace.capability` for observability and is not read back to make execution decisions.

> **ADR-013:** `ActionCapability` describes; it does not dispatch. A `switch` on `capability()` that selects element execution methods is an antipattern and is prohibited.

---

## ElementActions Factory

`ElementActions` is annotated `@Internal` (ADR-012). It provides `ElementActions.of(...)` — a custom-operation factory for test-infrastructure use only (step definitions, shared helpers, page-object utilities that need to wrap arbitrary engine logic as a named `Action`).

Page object enum definitions must NOT call `ElementActions.of()`. Those should define dedicated concrete action subclasses. `ElementActions.of()` is the escape hatch for the narrow cases where a full subclass is not warranted.

---

## Custom Hook Libraries

Teams building on VOID should define their own reusable hook libraries following the same constants-holder pattern used by `core.interactions.hooks.After` and `core.interactions.hooks.Before`: a `final` class with a private constructor and `public static final` typed constants.

`tests.demo.hooks.DemoHooks` is the canonical example in this repository:

```java
package tests.demo.hooks;

import core.engine.LocatorDescriptor;
import core.interactions.hooks.AfterActionHandler;
import tests.demo.pages.DemoLoginPage;
import elements.meta.ElementRole;
import java.time.Duration;
import static core.logging.CustomLogger.debug;

public final class DemoHooks {

    private static final Duration LOGIN_SUCCESS_TIMEOUT = Duration.ofSeconds(5);

    private DemoHooks() {}

    public static final AfterActionHandler WAIT_FOR_LOGIN_SUCCESS = (engine, descriptor) -> {
        LocatorDescriptor successMsg = engine.resolve(
                DemoLoginPage.Labels.SUCCESS_MESSAGE, ElementRole.TEXT);
        engine.waitForVisible(successMsg, LOGIN_SUCCESS_TIMEOUT);
        debug.log("[HOOK] Login success message visible.");
    };
}
```

Usage at the call site:

```java
DemoLoginPage.Button.LOGIN_BUTTON.click()
        .safely()
        .after(DemoHooks.WAIT_FOR_LOGIN_SUCCESS)
```

**Rules for custom hook libraries:**
- Use typed constants: `AfterActionHandler` or `BeforeActionHandler`, not the raw `ActionHandler` supertype. This lets callers see at a glance whether a constant is a before or after hook.
- One hook, one concern. Compose in `.after(hookA, hookB)` rather than building combined hooks.
- Use `engine` methods exclusively — never reference `WebDriver` or `WebElement` directly.
- Parameterized hooks that need runtime values should be static factory methods returning `AfterActionHandler` / `BeforeActionHandler`.

---

## Extension Guide

To add a new action type:

1. **Identify the family.** Does the new action click something, type something, interact with a dropdown, or is it standalone?
2. **Extend the correct class:**
   - Click-family → `extends ClickableElementAction`
   - Type-family → `extends TypeableElementAction`
   - Select-family → `extends SelectableElementAction`
   - Standalone / default profile → `extends ElementAction`
3. **Implement `execute(UIEngine engine, LocatorDescriptor descriptor)`** — this is the only abstract method to implement. The intermediary already provides `defaultSafeProfile()` and `defaultReliableProfile()`.
4. **Declare role and capability** in the `super(element, role, capability)` constructor call.
5. **Annotate `@Beta`** until the API stabilizes.
6. **Emit the action** from the corresponding capability interface method (e.g., `Clickable.click()` returns `new ClickAction(this)`).
7. **Write a profile test** in `ElementActionsSafeProfileTest` asserting the before/after hook lists for both `safely()` and `reliable()`.

No changes to `ActionProfiles`, existing action classes, or `ElementAction` are required — this is the Open/Closed Principle in practice.
