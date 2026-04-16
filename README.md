# VOID Framework  
**Versatile Object-Oriented Integration for Debugging**

---

## 🧠 What This Is (and What It Isn’t)

VOID is **not** another Selenium wrapper.

It doesn’t try to make Selenium “simpler.”  
It makes it **structured, observable, and debuggable** — which is what you actually need once things stop working.

Most frameworks focus on *running tests*.  
VOID focuses on **understanding them when they fail**.

---

## ❌ The Problem (You’ve Seen This Before)

If you’ve worked with Selenium long enough, you already know:

- Page Objects start clean → end up as 2,000-line nightmares  
- Locators live in 6 different places → none of them updated  
- Failures say *“element not found”* → thanks, very helpful  
- Debugging = scroll logs + guess + retry  

At scale, automation doesn’t fail because of Selenium.  
It fails because of **lack of structure and visibility**.

---

## ✅ What VOID Does Differently

VOID doesn’t patch these problems.  
It replaces the way things are modeled.

### 🔹 Elements are not classes. They’re enums.
Each UI element is a **first-class, typed entity** — not a string buried in a page file.

### 🔹 Locators are resolved, not hardcoded
Dynamic, role-based resolution through `LocatorResolverV1`.

### 🔹 Actions are pipelines, not method calls
Every interaction supports **before/after hooks** — composable, reusable, predictable.

### 🔹 Logging actually explains things
Not just *what failed*, but:
- where it failed  
- why it failed  
- what was attempted  

---

## ⚡ Why VOID Exists

Because this:

```java
driver.findElement(By.xpath("//div[3]/span[2]")).click();
```

…is not automation.

That’s just future debugging debt.

---

## 🚀 Core Features

### 🧩 Enum-Driven Element Model
- Elements defined as enums implementing interfaces (`Clickable`, `Dropdown`, etc.)
- Nested enums for contextual grouping
- Each element carries:
  - locator key
  - external file reference
  - dynamic arguments
  - display text

---

### 📍 Role-Based Locator Resolution
- Centralized via `LocatorResolverV1`
- Supports `.json` and `.properties`
- Uses `ElementRole`
- Dynamic `%s` substitution at runtime

---

### 🪝 Hook-Based Execution Pipeline
Reusable hooks like:
- WAIT_FOR_ELEMENT_VISIBLE  
- WAIT_FOR_ELEMENT_CLICKABLE  
- HIGHLIGHT_ELEMENT  
- WAIT_FOR_ANGULAR_LOADER  

---

### 🧠 Debug-Oriented Logging
- Color-coded logs  
- Call-site tracing  
- Console + persistent logs  

---

### 🧭 Centralized Interactions API

```java
void.interaction().clickOn(element);
void.interaction().selectFromDropdown(dropdown);
void.interaction().searchFor(searchField, "text");
```

---

## 🧠 Example Usage

```java
VOID void = new VOID();

void.interaction().clickOn(ManageUsersElements.UserCards.LOGIN_AS_BUTTON);

void.interaction().clickOn(
    List.of(Before.WAIT_FOR_ANGULAR_LOADER),
    MyElements.SUBMIT_BUTTON,
    List.of(After.DO_NOTHING)
);

void.interaction().selectFromDropdown(CommonElements.AppSwitcher.ADMIN);

String name = void.interaction().getText(ManageUsersElements.UserCards.FULL_NAME);

void.interaction().searchFor(CommonElements.GlobalSearch.SEARCH, "Deal Registration");
```

---

## 🧱 Architecture

Test → VOID → Interactions → LocatorResolver → WebDriver  
                        ↓  
                   Hooks + Logging + Context  

---

## 🧰 Driver & Config

- Chrome, Firefox, Edge  
- Local / Grid / Selenoid  
- Headless, proxy, mobile emulation  
- Config via `driver.properties`  

---

## 🧾 Logging Example

```
[INFO]  Interactions.clickOn  Clicked on: Login As
[DEBUG] LocatorResolverV1     Resolved JSON locator
[WARN]  Interactions.clickOn  Selenium failed, retrying with JS...
```

---

## 📂 Project Structure

- elements/  
- interactions/  
- core/  
- utils/  

---

## 🧠 Design Philosophy

Automation should be understandable under failure — not just execution.

---

## 🧪 Tech Stack

- Java 17  
- Selenium 4  
- TestNG + Cucumber  
- Extent Reports  

---

## 📜 License

MIT License © 2025–2026

---

## 🧩 Final Note

VOID won’t magically fix bad test design.
But it will expose it very clearly.

---

## `Refer to /docs for detailed documents`
