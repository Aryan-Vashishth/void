# 002 — Cucumber as Optional Dependency

**Date:** 2026-05-01  
**Status:** Accepted

---

## Context

VOID declared `cucumber-java` and `cucumber-testng` as compile-scope dependencies. However:

- No `.feature` files exist in the project
- The only file referencing Cucumber (`CommonStepDef.java`) was entirely commented out (546 lines)
- `StepDefInteractions` and `EnumResolver` provide BDD-compatible interaction without requiring Cucumber
- Cucumber uses classpath scanning, reflection-based step binding, and its own lifecycle that bypasses TestNG's native flow

---

## Decision

Mark Cucumber dependencies as `<optional>true</optional>` in the POM. VOID does not depend on Cucumber at runtime — it provides an adapter surface (`StepDefInteractions`, `ResolvableEnum`, `EnumResolver`) that works independently.

Consumers who need Cucumber declare it in their own project.

---

## Reasoning

1. **Dead dependency** — zero active imports, no feature files, no runner
2. **Hidden behavior risk** — classpath scanning and reflection-based binding contradict VOID's explicitness principle
3. **VOID's internal BDD support is self-contained** — `StepDefInteractions` resolves enums by context string without Cucumber
4. **Consumer responsibility** — if a project needs Cucumber, it should own that dependency declaration

---

## Consequences

- Cucumber is available for consumers who explicitly declare it
- VOID's internal step-definition layer (`StepDefInteractions`, `EnumClassRegistry`, `EnumResolver`) continues to work
- Removed transitive dependency weight from VOID's default classpath
- Dead step-definition files cleaned up

