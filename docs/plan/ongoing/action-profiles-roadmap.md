# Action Profiles — Roadmap

**Status:** Ongoing  
**Date:** 2026-06-13  
**Area:** `core.actions`, `core.interactions.hooks`, VOID developer experience

---

## Context

The hook engine is solid. `before(...).after(...)` is correct low-level plumbing.

The gap is the **developer experience layer** sitting on top of it.

Test writers currently need to know: `Before`, `After`, `ActionHandler`, hook ordering, descriptor lifecycle.

They shouldn't.

---

## Direction

Move from:

```text
Hooks (explicit plumbing)
```

to:

```text
Execution Profiles (named behavior)
```

---

## Phase 1 — Action Profiles (Shorthand Methods)

Add to `Action`:

```java
Action safely();
Action debug();
Action raw();
```

Usage:

```java
USERNAME.type("admin").safely();
LOGIN.click().safely();
```

### Type `safely()` expands to:

```java
.before(Before.CLEAR_FIELD, Before.WAIT_FOR_ELEMENT_VISIBLE)
.after(After.HIGHLIGHT_ELEMENT)
```

### Click `safely()` expands to:

```java
.before(Before.WAIT_FOR_ELEMENT_CLICKABLE)
.after(After.WAIT_FOR_ANGULAR_LOADER, After.HIGHLIGHT_ELEMENT)
```

Same API. Different behavior. Action type determines safety strategy.

---

## Phase 2 — Profile Registry

Introduce `ActionProfile`:

```java
public interface ActionProfile {
    List<ActionHandler> before();
    List<ActionHandler> after();
}
```

Built-ins: `SAFE`, `DEBUG`, `RAW`

Add to `Action`:

```java
Action using(ActionProfile profile);
```

Usage:

```java
USERNAME.type("admin").using(Profile.SAFE);
LOGIN.click().using(Profile.DEBUG);
```

---

## Phase 3 — Capability-Aware Profiles

Each capability resolves a profile differently:

| Capability | `SAFE` expands to |
|---|---|
| `Clickable` | `WAIT_CLICKABLE` → `WAIT_ANGULAR_LOADER, HIGHLIGHT` |
| `Typeable` | `CLEAR, WAIT_VISIBLE` → `HIGHLIGHT` |
| `Selectable` | `WAIT_VISIBLE, WAIT_CLICKABLE, WAIT_ANGULAR_LOADER` → `HIGHLIGHT` |

---

## Phase 4 — Preset Library

```java
Profiles.SAFE
Profiles.DEBUG
Profiles.FAST
Profiles.VISUAL
Profiles.RELIABLE
```

Usage:

```java
LOGIN.click().using(Profiles.RELIABLE);
SEARCH.type("Laptop").using(Profiles.FAST);
```

---

## Phase 5 — App-Level Default Profiles

Framework config:

```properties
void.profile.default=SAFE
```

Application-level constant:

```java
Profiles.COMPANY_STANDARD  // includes WAIT_ANGULAR_LOADER, SPINNER, HIGHLIGHT, SCREENSHOT_ON_FAILURE
```

Result:

```java
LOGIN.click();  // automatically receives COMPANY_STANDARD behavior
```

No repeated hooks anywhere.

---

## Phase 6 — Docs Shift

Current docs show `before(...).after(...)` everywhere.

Future docs should show:

```java
USERNAME.type("admin").safely();
PASSWORD.type("secret").safely();
LOGIN.click().safely();
```

Hooks become **framework extension mechanism**, not everyday API.  
Advanced users still have full `before(...).after(...)` access.

---

## Phase 7 — Custom Profile Builder

```java
ActionProfile LOGIN_PROFILE = Profile.builder()
    .before(Before.WAIT_FOR_ANGULAR_LOADER)
    .before(Before.WAIT_FOR_ELEMENT_CLICKABLE)
    .after(After.WAIT_FOR_ANGULAR_LOADER)
    .after(After.HIGHLIGHT_ELEMENT)
    .build();

LOGIN.click().using(LOGIN_PROFILE);
```

---

## Priority

| Priority | Feature | Value |
|---|---|---|
| 1 | `safely()` / `debug()` / `raw()` | Very High |
| 2 | Profile Registry + `using(profile)` | High |
| 3 | Capability-aware profile resolution | Very High |
| 4 | Preset Library (`Profiles.*`) | High |
| 5 | Custom Profile builder | Very High |
| 6 | Global default profile (config-driven) | Extremely High |
| 7 | Docs shift — profiles as primary API | High |

---

## What NOT To Do

Do not attempt import-free hook constants (e.g., `.after(HIGHLIGHT_ELEMENT)` without imports).  
Java syntax battle. Payoff is negligible.

---

## Notes

Hooks (`before` / `after`) remain the implementation mechanism.  
Profiles are the language people speak.  
This is the moment VOID transitions from *framework* to *UI execution platform*.

---

*MIT License © 2025–2026 VOID Project*

