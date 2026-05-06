# 008 — Capability Interfaces Replace Behavioral Interfaces

**Date:** 2026-06-01  
**Status:** Accepted

---

## Context

VOID's original element interfaces used behavioral names that described what the UI component *was* rather than what it *could do*:

| Old Interface | New Capability |
|---------------|---------------|
| `TextInputField` | `Typeable` |
| `Dropdown` | `Selectable` |
| `MultipleIdenticalDropdowns` | `MultiSelectable` |
| `ToolTipElement` | `Hoverable` |
| `ReadOnlyElement` | `ReadOnly` |
| `Checkbox` | `Checkable` |
| `FileInputElement` | `Uploadable` |
| `TableElement` | `Table` |
| `WritableTableElement` | `EditableTable` |
| `ListElement` | `Listable` |

The old interfaces contained NO execution logic but also emitted no execution intent — they were pure data contracts. With the introduction of `UIEngine` and the `Action` pattern, capability interfaces now emit **deferred Action objects**, making elements self-descriptive about what operations they support.

---

## Decision

Replace all behavioral element interfaces with **capability interfaces** in `elements.api.capability.*`. Each capability interface:

1. Defines the structural contract (locator keys, role mappings)
2. Emits deferred `Action` objects via default methods
3. Contains **NO** execution logic — Actions are lambdas over `UIEngine`
4. Uses verb-based naming (`Clickable`, `Typeable`, `Selectable`) instead of noun-based (`TextInputField`, `Dropdown`)

---

## Reasoning

1. **Verb-based naming is self-documenting** — `implements Clickable` tells you immediately what the element can do
2. **Action emission enables Flow composition** — `element.click()`, `element.type("text")` return `Action` objects for `Flow.of(...)` + `FlowExecutor.run(flow)`
3. **Deferred resolution pattern** — locator resolution happens inside Action lambdas at execution time, preventing stale locators
4. **Consistent hierarchy** — all capabilities extend `Element`, with clear inheritance chains (e.g., `Clickable → Checkable`, `ReadOnly → Hoverable`, `Clickable + Listable → Selectable`)
5. **Package separation** — capabilities live in `elements.api.capability.*`, keeping the API surface organized

---

## Consequences

### New Capability Hierarchy

```
Element (root)
├── Clickable              → TRIGGER role, emits click()
│   ├── Checkable          → inherits TRIGGER, adds toggle(), set(boolean)
│   └── (used by Selectable, SearchField)
├── Typeable               → INPUT role, emits type(), clear(), append(), typeAndPress()
│   └── (used by SearchField)
├── ReadOnly               → TEXT role, emits getText()
│   └── Hoverable          → adds TOOLTIP_CONTENT, emits hover()
├── Selectable             → TRIGGER + LIST, extends Clickable + Listable, emits open(), select()
│   └── SearchableDropdown → extends Selectable + Searchable, emits searchAndSelect()
├── MultiSelectable        → MULTI_TRIGGER + MULTI_LIST, emits open(), selectAtIndex()
├── Uploadable             → INPUT role, emits upload(path)
├── Listable               → LIST role
├── SearchField            → extends Typeable + Clickable, SEARCH_INPUT + SEARCH_BUTTON
│   └── Searchable         → extends SearchField, adds SEARCH_RESULT
├── Table                  → TABLE + ROW/COLUMN/CELL/HEADER
│   └── EditableTable      → adds ADD_ROW_BUTTON/REMOVE_ROW_BUTTON, emits clickAddRow()
└── KeyValuePair           → standalone contract
```

### Action Emission Pattern

Every capability interface follows the same pattern:

```java
default Action click() {
    return engine -> {
        var d = engine.resolve(this, ElementRole.TRIGGER);  // deferred resolution
        engine.click(d);                                     // engine executes
    };
}
```

Key rules:
- Resolve locators **inside** the lambda (deferred, not eager)
- Never reference `WebDriver`, `WebElement`, or `By`
- Action = deferred execution intent. Engine = smart executor.

### Migration

All old interface names are removed. Enum declarations update their `implements` clause:

```java
// Before
enum Actions implements Clickable { ... }        // old Clickable was actually a behavioral interface
// After
enum Actions implements Clickable { ... }        // new Clickable is a capability with click() Action

// Before
enum Fields implements TextInputField { ... }
// After
enum Fields implements Typeable { ... }

// Before
enum Dropdowns implements Dropdown { ... }
// After
enum Dropdowns implements Selectable { ... }
```

---

## Related

- [007 — UIEngine as Single Execution Authority](007-uiengine-execution-authority.md) — the execution model these capabilities emit into
- [System Overview](../../../5-architecture/system-overview.md) — full capability table
- [Quick Start Guide](../../../5-architecture/quick-start.md) — defining elements with capabilities

