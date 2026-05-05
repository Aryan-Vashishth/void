# elements.api — Interface Architecture

## Overview

This package defines the **element abstraction layer** for the VOID framework.
Every UI element (button, input, dropdown, table, etc.) is modeled as a **capability interface**
that extends the base `Element` contract and emits deferred `Action` objects.

```
Element → Capability → emits Action (intent)
Action  → UIEngine   (execution)
```

| Layer | Package | Responsibility |
|-------|---------|---------------|
| **Element** | `elements.api` | Base descriptor contract (locator keys, args, display text, role map) |
| **Capability** | `elements.api.capability` | Declares locator structure + emits deferred `Action` objects |

**Key principles:**
- Elements NEVER execute — they emit intent only
- Actions NEVER perform work until executed by UIEngine
- UIEngine owns ALL execution concerns (scroll, waits, retries, fallback)
- Locator resolution happens **inside** the Action lambda at execution time

---

## Package Layout

```
elements/api/
├── Element.java                          ← base contract (locator keys, args, roles)
├── KeyValuePair.java                     ← standalone utility for key→value enums
├── package-info.java
│
└── capability/                           ← CAPABILITY LAYER (structure + action emission)
    ├── package-info.java
    ├── Clickable.java                    ← getTriggerLocator(), TRIGGER role, click()
    ├── Typeable.java                     ← getInputLocator(), INPUT role, type(), clear()
    ├── Listable.java                     ← getListLocator(), getIndex(), LIST role
    ├── Selectable.java                   ← trigger + list (extends Clickable, Listable), open(), select()
    ├── Checkable.java                    ← toggle(), set(boolean) (extends Clickable)
    ├── SearchField.java                  ← SEARCH_INPUT + SEARCH_BUTTON roles, typeSearch(), submitSearch()
    ├── Searchable.java                   ← adds SEARCH_RESULT role (extends SearchField)
    ├── SearchableDropdown.java           ← 4 roles combined, searchAndSelect(term)
    ├── Uploadable.java                   ← file upload field, INPUT role, upload(path)
    ├── ReadOnly.java                     ← getTextLocator(), TEXT role, getText()
    ├── Hoverable.java                    ← TEXT + TOOLTIP_CONTENT roles, hover()
    ├── Table.java                        ← TABLE/ROW/COLUMN/CELL/HEADER roles
    ├── EditableTable.java                ← adds ADD_ROW/REMOVE_ROW/FOOTER roles, clickAddRow()
    └── MultiSelectable.java              ← MULTI_TRIGGER + MULTI_LIST roles, open(), selectAtIndex()
```

---

## Core Base

### `Element` (`elements.api.Element`)

Root interface for every UI element descriptor in the framework.

```java
public interface Element {
    String getExternalFileName();           // properties file for locator lookup
    String getPrimaryLocator();             // primary locator key
    default String getSecondaryLocator();   // optional fallback key
    Object[] getArgs();                     // dynamic args for %s templates
    default Object[] effectiveArgs(Object... overrides); // override-aware resolution
    default String getDisplayText();        // human-friendly label for logs
    default Map<ElementRole, String> getAllLocatorRoles(); // ordered role→key map
}
```

### `KeyValuePair<K, V>` (`elements.api.KeyValuePair`)

Standalone utility interface for enums mapping an internal key to a user-facing label.
Not part of the capability hierarchy.

---

## Element Hierarchies (Detailed)

### Click

```
Element → Clickable
```

**`Clickable`** (`elements.api.capability`)
- Contract: `String getTriggerLocator()`
- Overrides: `getPrimaryLocator()` → delegates to `getTriggerLocator()`
- Role map: `{ TRIGGER: triggerLocator }`
- Action: `click()` → resolves TRIGGER, calls `engine.click(d)`

---

### Text Input

```
Element → Typeable
```

**`Typeable`** (`elements.api.capability`)
- Contract: `String getInputLocator()`
- Role map: `{ INPUT: inputLocator }`
- Actions:
  - `type(String text)` — clears then types
  - `clear()` — clears input field
  - `append(String text)` — types without clearing
  - `typeAndPress(String text, String key)` — types then sends key

---

### Dropdown

```
Element → Clickable ─┐
Element → Listable  ─┤→ Selectable
```

**`Listable`** (`elements.api.capability`)
- Contract: `String getListLocator()`, `int getIndex()`
- Role map: `{ LIST: listLocator }`
- Pure structural — no action methods

**`Selectable`** (`elements.api.capability`)
- Extends: `Clickable`, `Listable`
- Role map: `{ TRIGGER: triggerLocator, LIST: listLocator }`
- Actions:
  - `open()` — clicks trigger
  - `select()` — composite: open → wait overlay → click option
  - `selectByText(String text)` — selects by visible text
  - `selectByValue(String value)` — selects by value attribute

---

### Checkbox

```
Element → Clickable → Checkable
```

**`Checkable`** (`elements.api.capability`)
- Extends: `Clickable`
- Inherits TRIGGER role
- Actions:
  - `toggle()` — clicks the checkbox
  - `set(boolean desiredState)` — reads state, clicks only if needed

---

### Search Field

```
Element → Typeable  ─┐
Element → Clickable ─┤→ SearchField
```

**`SearchField`** (`elements.api.capability`)
- Extends: `Typeable`, `Clickable`
- Contract: `getSearchInputLocator()`, `getSearchButtonLocator()`
- Role map: `{ SEARCH_INPUT: ..., SEARCH_BUTTON: ... }`
- Actions:
  - `typeSearch(String text)` — types into search input
  - `submitSearch()` — clicks search button

---

### Searchable / Searchable Dropdown

```
SearchField → Searchable
Selectable + Searchable → SearchableDropdown
```

**`Searchable`** (`elements.api.capability`)
- Extends: `SearchField`
- Adds: `String getSearchResultLocator()`
- Additional role: `SEARCH_RESULT`

**`SearchableDropdown`** (`elements.api.capability`)
- Extends: `Selectable`, `Searchable`
- Combines all 4 roles: `TRIGGER`, `SEARCH_INPUT`, `SEARCH_BUTTON`, `SEARCH_RESULT`
- Action: `searchAndSelect(String term)` — composite: open → type → wait result → click

---

### File Upload

```
Element → Uploadable
```

**`Uploadable`** (`elements.api.capability`)
- Contract: `String getInputLocator()`
- Role map: `{ INPUT: inputLocator }`
- Action: `upload(String filePath)` — uploads file via input element

---

### Read-Only (Labels / Static Text)

```
Element → ReadOnly
```

**`ReadOnly`** (`elements.api.capability`)
- Contract: `String getTextLocator()`
- Role map: `{ TEXT: textLocator }`
- Action: `getText()` — reads visible text content

---

### Tooltip

```
Element → ReadOnly → Hoverable
```

**`Hoverable`** (`elements.api.capability`)
- Extends: `ReadOnly`
- Adds: `String getToolTipContentLocator()`, `String getEndsWith()`
- Role map: `{ TEXT: textLocator, TOOLTIP_CONTENT: tooltipLocator }`
- Action: `hover()` — hovers to trigger tooltip display

---

### Table / Editable Table

```
Element → Table
Element → Table → EditableTable
```

**`Table`** (`elements.api.capability`)
- Contract: `getTableLocator()`, optional `getRowLocator()`, `getColumnLocator()`, `getCellLocator()`, `getHeaderLocator()`
- Role map: `{ TABLE, ROW, COLUMN, CELL, HEADER }` (sparse — only non-null keys)
- Pure structural — no action methods

**`EditableTable`** (`elements.api.capability`)
- Extends: `Table`
- Adds: `getAddRowButtonLocator()`, `getRemoveRowButtonLocator()`, `getFooterInputRowLocator()`
- Additional roles: `ADD_ROW_BUTTON`, `REMOVE_ROW_BUTTON`, `FOOTER_INPUT_ROW`
- Actions: `clickAddRow()`, `clickRemoveRow()`

---

### Multi-Instance Dropdown

```
Element → MultiSelectable
```

**`MultiSelectable`** (`elements.api.capability`)
- Contract: `getTriggerLocator()`, `getListLocator()`
- Utility: `getArgsWithIndex(int)`, `argsForIndex(Integer)` — prepends index to args array
- Role map: `{ MULTI_TRIGGER: ..., MULTI_LIST: ... }`
- Actions: `open()`, `selectAtIndex(Integer index)`

---

## Design Rules

### ❌ NEVER

- Put execution logic in `Element` or any capability interface
- Resolve locators eagerly (outside the Action lambda)
- Reference `WebElement`, `By`, or `WebDriver` in capability interfaces
- Add scroll, wait, or retry logic to Action methods — engine handles these
- Mix structural and behavioral concerns

### ✅ ALWAYS

- Resolve descriptor **inside** the Action lambda (deferred execution)
- Use `engine.resolve(this, ElementRole.*)` inside the lambda
- Follow the pattern: `var d = engine.resolve(this, ROLE); engine.operation(d);`
- `Action` = deferred intent (lambda over `UIEngine`)
- `Engine` = smart executor (handles scroll, retry, fallback internally)
- `Role` = locator selection only (never leaks past element layer)
- New element types: create capability in `elements/api/capability/`

---

## Usage Example

```java
import elements.api.capability.Clickable;
import elements.api.capability.Typeable;
import core.flow.Flow;
import core.executor.FlowExecutor;

// Define page elements (implement capability for Flow-compatible enums)
enum LoginButton implements Clickable {
    SUBMIT("LOGIN_BTN");

    private final String key;
    LoginButton(String key) { this.key = key; }

    @Override public String getExternalFileName() { return "login.properties"; }
    @Override public String getTriggerLocator()   { return key; }
    @Override public Object[] getArgs()           { return new Object[0]; }
}

enum LoginField implements Typeable {
    USERNAME("USERNAME_INPUT"),
    PASSWORD("PASSWORD_INPUT");

    private final String key;
    LoginField(String key) { this.key = key; }

    @Override public String getExternalFileName() { return "login.properties"; }
    @Override public String getInputLocator()     { return key; }
    @Override public Object[] getArgs()           { return new Object[0]; }
}

// Execute via Flow + FlowExecutor
FlowExecutor executor = new FlowExecutor(engine);
executor.run(Flow.of(
    LoginField.USERNAME.type("admin"),
    LoginField.PASSWORD.type("secret"),
    LoginButton.SUBMIT.click()
));

// Single action execution
executor.run(LoginButton.SUBMIT.click());
```

---

## Execution Pipeline

```
┌─────────────┐     ┌──────────────┐     ┌───────────┐     ┌──────────────┐
│ Page Enum   │────▶│ Capability   │────▶│  Flow     │────▶│FlowExecutor  │
│ .click()    │     │ returns      │     │ groups    │     │ iterates     │
│ .type(text) │     │ Action λ     │     │ Actions   │     │ & calls      │
└─────────────┘     └──────────────┘     └───────────┘     └──────────────┘
                           │                                       │
                           ▼                                       ▼
                    ┌──────────────┐                       ┌──────────────┐
                    │ λ: resolves  │                       │   UIEngine   │
                    │ descriptor   │                       │  .click(d)   │
                    │ at exec time │                       │  .type(d,t)  │
                    └──────────────┘                       └──────────────┘
```

1. **Page enum** calls `element.click()` / `element.type("text")`
2. **Capability interface** returns a lambda `(engine) -> { resolve + execute }`
3. **Flow** groups multiple Actions into an ordered sequence
4. **FlowExecutor** iterates the Flow and calls `action.perform(engine)` for each
5. **Inside the lambda**: `engine.resolve(this, role)` resolves at execution time
6. **UIEngine** receives the `LocatorDescriptor` and performs the browser operation
   (including scroll, wait, retry, fallback — all handled internally by the engine)

---

## How to Add a New Element Type

1. **Create capability** in `elements/api/capability/`:
   - Extend `Element` (or another capability if composite)
   - Declare locator key method(s) (e.g., `getMyLocator()`)
   - Override `getPrimaryLocator()`, `getDisplayText()`, `getAllLocatorRoles()`
   - Add appropriate `ElementRole` if new (update `ElementRole` enum)
   - Add `default Action methodName(...)` methods that resolve via engine and call engine operations

2. **Implement in page enum**:
   - `enum MyElement implements MyCapability { ... }`
   - Provide: `getExternalFileName()`, `getMyLocator()`, `getArgs()`

3. **Use in Flow**:
   - `Flow.of(MyPage.MY_ELEMENT.myAction(...))`
