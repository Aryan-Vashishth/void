# VOID Framework - Versatile Object-Oriented Integration for Debugging

## Overview

VOID (Versatile Object-Oriented Integration for Debugging) is a **next-generation Selenium test automation framework** — not a wrapper, but a **complete re-engineering of how Selenium frameworks are structured and executed**. It’s built from the ground up for **scalability, observability, and precision debugging**, enabling test engineers to interact with UI components in a modular, interface-driven, and context-aware way.

VOID replaces conventional Selenium wrappers with a fully object-oriented architecture where every UI element, action, and verification point is modeled as a first-class citizen. Through its **enum-driven element model**, **smart locator resolution**, and **deeply integrated logging**, VOID offers a development experience that is both clean and traceable.

---

## 🚀 Key Features

### 🧠 Not Just Another Selenium Wrapper

* VOID is **not** a thin abstraction over Selenium — it’s a **re-engineered framework** that redefines interaction semantics.
* Every UI element implements strongly-typed interfaces for specific behavior.
* Deep logging and contextual tracing make every test execution reproducible and debuggable.
* Designed for **large-scale enterprise test automation**, ensuring maintainability and precision at scale.

### 🔧 Enum-Driven Object Model

* Every UI element is represented as an **enum constant** implementing interfaces like `ClickableElement`, `DropdownElement`, or `ToolTipElement`.
* Nested enums (e.g., `AdminHomeElements.Tiles`) organize elements by page and functional context.
* Provides structural hierarchy and eliminates XPath duplication across pages.

### 🧠 Debug-Oriented Core

* **CustomLogger** provides timestamped, color-coded console logs with IDE-clickable file-line references.
* Debug logs include class hierarchy, element context, and action origin.
* Persistent trace logs preserve full stack depth for each session.

### 📍 Unified Locator Resolution

* `LocatorResolver` intelligently chooses between **JSON locators** and legacy `.properties` files.
* **JsonLocatorReader** and **JsonLocatorMigrator** simplify migration and maintain full backward compatibility.
* Supports dynamic `%s` substitutions, fallback mechanisms, and robust error tracing.

### 🧩 Interface-Driven Design

VOID’s architecture is modular and extensible through fine-grained interfaces:

| Interface                             | Description                                               |
| ------------------------------------- | --------------------------------------------------------- |
| **BaseElement**                       | Root locator contract for all UI elements.                |
| **ClickableElement**                  | Represents clickable UI components.                       |
| **DropdownElement**                   | Defines trigger and list locators for dropdowns.          |
| **SearchableElement**                 | Handles search field and result lists.                    |
| **ToolTipElement**                    | Supports hover tooltip and fallback resolution.           |
| **TableElement**                      | Models structured read-only tables.                       |
| **WritableTableElement**              | Extends table logic to support editing.                   |
| **ListElement**                       | Represents static or dynamic list-based UI patterns.      |
| **CheckboxElement**                   | Handles state validation and toggling logic.              |
| **FileInputElement**                  | Automates file uploads.                                   |
| **TextInputFieldElement**             | Represents input text fields.                             |
| **KeyValuePairElement**               | Models key-value display or edit pairs.                   |
| **MultipleIdenticalDropdownElements** | Supports repeated dropdown patterns.                      |
| **ReadOnlyElement**                   | For non-editable static elements.                         |
| **ResolvableEnum**                    | Enables reflection-driven enum resolution across modules. |

---

### 🧭 Centralized Interactions API

* `Interactions` and `StepDefInteractions` abstract WebDriver logic.
* Standardized methods like `clickOn()`, `selectFromDropdown()`, `getText()`, and `searchFor()` resolve elements dynamically.
* Integrates `WaitUtils` for smart waiting and Angular loader handling.

### 🧱 Modular Utilities

* `UIUtils`: Core DOM interaction and tooltip utilities.
* `WaitUtils`: Fluent waits, Angular stabilization, flicker detection.
* `FileUtils`: File resolution, download verification, JSON parsing.
* `DataVerifier`: Data-level validation with normalization and tolerance settings.

### 🧾 Intelligent Logging

* Dual-channel logging: real-time console + full-depth trace file.
* Console format: `[LEVEL] OriginClass.method <message> ← CallerClass.method`
* All logs include precise callsite tracking and execution duration.

### 🧰 Config & Driver Management

* `DriverFactory` handles browser creation, headless operation, and remote WebDriver sessions.
* `DriverContext` manages driver lifecycle and multi-browser orchestration.
* Internal configuration hierarchy: System → ENV → Classpath → Defaults.

---

## 📂 Project Structure

```
void-framework/
├── Elements/
│   ├── Interfaces/
│   │   ├── BaseElement.java
│   │   ├── ClickableElement.java
│   │   ├── DropdownElement.java
│   │   ├── SearchableElement.java
│   │   ├── ToolTipElement.java
│   │   ├── TableElement.java
│   │   ├── WritableTableElement.java
│   │   ├── ListElement.java
│   │   ├── CheckboxElement.java
│   │   ├── FileInputElement.java
│   │   ├── TextInputFieldElement.java
│   │   ├── KeyValuePairElement.java
│   │   ├── MultipleIdenticalDropdownElements.java
│   │   ├── ReadOnlyElement.java
│   │   └── ResolvableEnum.java
│   ├── CommonElements.java
│   ├── AdminHomeElements.java
│   └── ...
│
├── interactions/
│   ├── Interactions.java
│   └── StepDefInteractions.java
│
├── utils/
│   ├── locators/
│   ├── wait/
│   ├── logging/
│   ├── base/
│   ├── data/
│   └── ...
│
├── locators/
│   ├── json/
│   └── properties/
│
├── config/
│   └── framework.properties
│
├── tests/
└── README.md
```

---

## ⚙️ Execution Flow

1. **Enum Resolution** → Contextual enum mapping.
2. **Locator Resolution** → JSON fallback to .properties.
3. **Wait Handling** → Adaptive loader and stability checks.
4. **Action Execution** → Interaction and validation.
5. **Debug Logging** → Multi-layer tracing and timing capture.

---

## 🧠 Example Usage

```java
// Click a tile
Vartopia.action().clickOn(AdminHomeElements.Tiles.ACCOUNT_MAPPING);

// Select from dropdown
Vartopia.action().selectFromDropdown(CommonElements.Switcher.VARTOPIA_SWITCHER, "Admin");

// Get tooltip text
String tooltip = Vartopia.action().getText(UserCards.EMAIL_TOOLTIP);

// Search for an entry
Vartopia.action().searchFor(CommonElements.SearchBar.GLOBAL_SEARCH, "Deal Registration");
```

---

## 🧰 Migration & Debugging Tools

* `JsonLocatorMigrator` converts `.properties` to `.json` locators.
* `JsonLocatorTemplateBuilder` generates default locator templates for new enums.
* Full debug traces record enum path, resolved locator, and execution context.

---

## 🧾 Log Output Example

```
[INFO] Interactions.clickOn <Clicked on: Account Mapping Tile> ← StepDefInteractions.userClicksOnTile
[DEBUG] LocatorResolver.primary <Resolved JSON locator for AdminHomeElements.Tiles.ACCOUNT_MAPPING>
```

---

## 🧩 Configuration

```
webdriver.browser=chrome
webdriver.headless=true
locators.json.enabled=true
logging.theme=dark
```

---

## 🧠 Design Philosophy

> *VOID is not a Selenium wrapper.* It’s a **scalable, object-oriented automation platform** purpose-built for clarity, extensibility, and precision debugging.
> Every line of code is designed for introspection — enabling you to see not only what failed, but **why** and **how**.

---

## 🧪 Authors & Maintainers

**VOID Framework Team**
Maintained by: Automation Engineering Group
Inspired by: Clean Architecture × Enum-Driven Design × Precision Debugging

---

## 📜 License

MIT License © 2025 VOID Framework Project
