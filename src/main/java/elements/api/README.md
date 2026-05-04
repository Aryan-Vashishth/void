# elements.api — Interface Architecture

## Overview

This package defines the **element abstraction layer** for VOID framework's UI automation engine.
Every UI element (button, input, dropdown, table, etc.) is modeled as a 3-level interface hierarchy:

```
Element → *Target → *Action
```

| Layer | Responsibility | Contains Actions? |
|-------|---------------|:-:|
| **Element** | Base descriptor contract (locator keys, args, display text, role map) | ❌ |
| **\*Target** | Capability — declares what locators an element type exposes | ❌ |
| **\*Action** | Behavior — produces deferred `Action` objects for Flow execution | ✅ |

---

## Core Base

### `Element`
Root interface for every UI element descriptor.

| Method | Purpose |
|--------|---------|
| `getExternalFileName()` | Properties file name for locator lookup |
| `getPrimaryLocator()` | Primary locator key |
| `getSecondaryLocator()` | Optional fallback locator key |
| `getArgs()` | Dynamic args for `%s` locator templates |
| `effectiveArgs(Object...)` | Override-aware arg resolution |
| `getDisplayText()` | Human-friendly label for logging |
| `getAllLocatorRoles()` | Ordered `Map<ElementRole, String>` of all locator keys |

---

## Element Hierarchies

### Click

```
Element → ClickTarget → ClickAction
```

| Interface | Key Methods |
|-----------|-------------|
| `ClickTarget` | `getTriggerLocator()` · Role: `TRIGGER` |
| `ClickAction` | `click()` → `Action` |

---

### Text Input

```
Element → TextInputTarget → TextInputAction
```

| Interface | Key Methods |
|-----------|-------------|
| `TextInputTarget` | `getInputLocator()` · Role: `INPUT` |
| `TextInputAction` | `type(String)` → `Action` · `clear()` → `Action` |

---

### Dropdown

```
Element → ClickTarget ─┐
Element → ListTarget  ─┤→ DropdownTarget → DropdownAction
```

| Interface | Key Methods |
|-----------|-------------|
| `ListTarget` | `getListLocator()` · `getIndex()` · Role: `LIST` |
| `DropdownTarget` | Combines trigger + list. Roles: `TRIGGER`, `LIST` |
| `DropdownAction` | `open()` · `selectByText(String)` · `selectByValue(String)` → `Action` |

---

### Checkbox

```
Element → ClickTarget → CheckboxTarget → CheckboxAction
```

| Interface | Key Methods |
|-----------|-------------|
| `CheckboxTarget` | `isChecked(WebDriver)` · `getAllCheckboxes()` · `getChecked(WebDriver)` · `getUnchecked(WebDriver)` |
| `CheckboxAction` | `toggle()` → `Action` |

---

### Search Field

```
Element → TextInputTarget ─┐
Element → ClickTarget     ─┤→ SearchFieldTarget → SearchFieldAction
```

| Interface | Key Methods |
|-----------|-------------|
| `SearchFieldTarget` | `getSearchInputLocator()` · `getSearchButtonLocator()` · Roles: `SEARCH_INPUT`, `SEARCH_BUTTON` |
| `SearchFieldAction` | `typeSearch(String)` · `submitSearch()` → `Action` |

---

### Searchable (extends SearchField)

```
SearchFieldTarget → SearchableTarget
```

| Interface | Key Methods |
|-----------|-------------|
| `SearchableTarget` | `getSearchResultLocator()` · Additional role: `SEARCH_RESULT` |

---

### Searchable Dropdown

```
DropdownTarget ─┐
SearchableTarget┤→ SearchableDropdownTarget → SearchableDropdownAction
```

| Interface | Key Methods |
|-----------|-------------|
| `SearchableDropdownTarget` | Combines all 4 roles: `TRIGGER`, `SEARCH_INPUT`, `SEARCH_BUTTON`, `SEARCH_RESULT` |
| `SearchableDropdownAction` | `open()` · `typeSearch(String)` · `submitSearch()` · `selectByText(String)` → `Action` |

---

### File Input

```
Element → FileInputTarget → FileInputAction
```

| Interface | Key Methods |
|-----------|-------------|
| `FileInputTarget` | `getInputLocator()` · Role: `INPUT` |
| `FileInputAction` | `upload(String filePath)` → `Action` |

---

### Read-Only (Labels / Static Text)

```
Element → ReadOnlyTarget → ReadOnlyAction
```

| Interface | Key Methods |
|-----------|-------------|
| `ReadOnlyTarget` | `getTextLocator()` · Role: `TEXT` |
| `ReadOnlyAction` | `scrollIntoView()` → `Action` |

---

### Tooltip

```
Element → ReadOnlyTarget → ToolTipTarget → ToolTipAction
```

| Interface | Key Methods |
|-----------|-------------|
| `ToolTipTarget` | `getToolTipContentLocator()` · `getEndsWith()` · Roles: `TEXT`, `TOOLTIP_CONTENT` |
| `ToolTipAction` | `hover()` → `Action` |

---

### List

```
Element → ListTarget → ListAction
```

| Interface | Key Methods |
|-----------|-------------|
| `ListTarget` | `getListLocator()` · `getIndex()` · Role: `LIST` |
| `ListAction` | `scrollToList()` → `Action` |

---

### Table

```
Element → TableTarget → TableAction
```

| Interface | Key Methods |
|-----------|-------------|
| `TableTarget` | `getTableLocator()` · `getRowLocator()` · `getColumnLocator()` · `getCellLocator()` · `getHeaderLocator()` · Roles: `TABLE`, `ROW`, `COLUMN`, `CELL`, `HEADER` |
| `TableAction` | `scrollToTable()` → `Action` |

---

### Writable Table

```
Element → TableTarget → WritableTableTarget → WritableTableAction
```

| Interface | Key Methods |
|-----------|-------------|
| `WritableTableTarget` | `getAddRowButtonLocator()` · `getRemoveRowButtonLocator()` · `getFooterInputRowLocator()` · Additional roles: `ADD_ROW_BUTTON`, `REMOVE_ROW_BUTTON`, `FOOTER_INPUT_ROW` |
| `WritableTableAction` | `clickAddRow()` · `clickRemoveRow()` → `Action` |

---

### Multi-Instance Dropdown

```
Element → MultiDropdownTarget → MultiDropdownAction
```

| Interface | Key Methods |
|-----------|-------------|
| `MultiDropdownTarget` | `getTriggerLocator()` · `getListLocator()` · `getArgsWithIndex(int)` · `argsForIndex(Integer)` · Roles: `MULTI_TRIGGER`, `MULTI_LIST` |
| `MultiDropdownAction` | `open()` · `selectByText(String)` → `Action` |

---

### Key-Value Pair

```
Element → KeyValuePair<K, V>
```

Standalone utility interface for enums mapping an internal key to a user-facing label. Not part of the Target/Action hierarchy.

---

## Deprecated Bridges (Backward Compatibility)

The following interfaces are **deprecated thin wrappers** that extend their corresponding `*Target` or `*Action` interface. They exist solely so that existing enum page objects continue to compile without modification.

| Deprecated Interface | Extends |
|---------------------|---------|
| `Clickable` | `ClickTarget` |
| `ClickableElement` | `ClickAction` |
| `TextInputField` | `TextInputTarget` |
| `TextInputElement` | `TextInputAction` |
| `Dropdown` | `DropdownTarget` |
| `DropdownElement` | `DropdownAction` |
| `Checkbox` | `CheckboxTarget` |
| `CheckboxElement` | `CheckboxAction` |
| `SearchField` | `SearchFieldTarget` |
| `SearchFieldElement` | `SearchFieldAction` |
| `Searchable` | `SearchableTarget` |
| `SearchableDropdown` | `SearchableDropdownTarget` |
| `MultipleIdenticalDropdowns` | `MultiDropdownTarget` |
| `ReadOnly` | `ReadOnlyTarget` |
| `ReadOnlyElement` | `ReadOnlyAction` |
| `ListPanel` | `ListTarget` |
| `ListElement` | `ListAction` |
| `FileInput` | `FileInputTarget` |
| `FileInputElement` | `FileInputAction` |
| `Table` | `TableTarget` |
| `TableElement` | `TableAction` |
| `WritableTable` | `WritableTableTarget` |
| `WritableTableElement` | `WritableTableAction` |
| `ToolTip` | `ToolTipTarget` |
| `ToolTipElement` | `ToolTipAction` |

> **Migration**: Replace deprecated interface with the appropriate `*Target` (if no actions needed) or `*Action` (if the enum should produce Actions for Flow execution).

---

## Design Rules

### ❌ NEVER

- Put execution logic in `Element` or `*Target`
- Resolve locators eagerly (outside the Action lambda)
- Reference `WebElement`, `By`, or `WebDriver` in `*Action` interfaces
- Let an `Action` know about the resolver or `ElementRole`

### ✅ ALWAYS

- Resolve descriptor **inside** the Action lambda (deferred execution)
- Use `LocatorResolvers.strict().resolveDescriptor(this, ElementRole.*)` inside the lambda
- `Action` = dumb execution wrapper
- `Engine` = smart executor
- `Role` = locator selection only (never leaks past element layer)

---

## Usage Example

```java
// Define page elements (implement *Action interfaces)
enum LoginButton implements ClickAction {
    SUBMIT("LOGIN_BTN");
    // ... getExternalFileName(), getTriggerLocator(), getArgs() ...
}

enum LoginField implements TextInputAction {
    USERNAME("USERNAME_INPUT"),
    PASSWORD("PASSWORD_INPUT");
    // ... getExternalFileName(), getInputLocator(), getArgs() ...
}

// Execute via Flow + Runner
Runner runner = new Runner(engine);
runner.run(Flow.of(
    LoginField.USERNAME.type("admin"),
    LoginField.PASSWORD.type("secret"),
    LoginButton.SUBMIT.click()
));
```

---

## Folder Structure

```
elements/api/
├── Element.java                  ← base contract
├── KeyValuePair.java             ← standalone utility
│
├── ClickTarget.java              ← capability
├── ClickAction.java              ← behavior
│
├── TextInputTarget.java
├── TextInputAction.java
│
├── ListTarget.java
├── ListAction.java
│
├── DropdownTarget.java
├── DropdownAction.java
│
├── CheckboxTarget.java
├── CheckboxAction.java
│
├── SearchFieldTarget.java
├── SearchFieldAction.java
│
├── SearchableTarget.java
├── SearchableDropdownTarget.java
├── SearchableDropdownAction.java
│
├── FileInputTarget.java
├── FileInputAction.java
│
├── ReadOnlyTarget.java
├── ReadOnlyAction.java
│
├── ToolTipTarget.java
├── ToolTipAction.java
│
├── TableTarget.java
├── TableAction.java
│
├── WritableTableTarget.java
├── WritableTableAction.java
│
├── MultiDropdownTarget.java
├── MultiDropdownAction.java
│
└── (deprecated bridges)
    ├── Clickable.java
    ├── ClickableElement.java
    ├── TextInputField.java
    ├── TextInputElement.java
    ├── Dropdown.java
    ├── DropdownElement.java
    ├── Checkbox.java
    ├── CheckboxElement.java
    ├── SearchField.java
    ├── SearchFieldElement.java
    ├── Searchable.java
    ├── SearchableDropdown.java
    ├── MultipleIdenticalDropdowns.java
    ├── ReadOnly.java
    ├── ReadOnlyElement.java
    ├── ListPanel.java
    ├── ListElement.java
    ├── FileInput.java
    ├── FileInputElement.java
    ├── Table.java
    ├── TableElement.java
    ├── WritableTable.java
    ├── WritableTableElement.java
    ├── ToolTip.java
    └── ToolTipElement.java
```

