# VOID Architecture Audit — Third Round: Future-Domain and AI-Native Stress Test

**Date:** 2026-06-16
**Basis:** Challenges the revised audit (domain-agnostic-runtime-audit-2026-06-revised.md)
**Method:** Attempt to falsify the C classification and the revised conclusions. Do not defend previous answers. Evaluate what breaks, what survives, and what was overlooked.

---

## Challenge 1: Is `Action.perform(Engine)` Truly Mechanical?

**The revised audit claimed:** The change from `Action.perform(UIEngine)` to `Action.perform(Engine)` is largely mechanical — type signatures, not logic.

**The challenge:** In a runtime hosting UI, Robot, Agent, and Workflow domains simultaneously, `ClickAction` can no longer safely assume `engine.click(...)` exists. What is an Action when multiple domains coexist?

---

### The Type Change Is Mechanical. The Architectural Question It Reveals Is Not.

The type change itself is mechanical. The analysis stands: introduce `Engine`, update six type signatures, change no execution logic. That part was correct.

But the challenge reveals something the revised audit underweighted: changing `Action.perform(UIEngine)` to `Action.perform(Engine)` does not complete the work — it merely exposes a deeper question.

**What actually happens inside `ClickAction` after the change:**

```java
// In Clickable.click() — current state:
return ElementActions.of(this, ElementRole.TRIGGER,
        (engine, d) -> engine.click(d));
```

The lambda is typed as `BiConsumer<UIEngine, LocatorDescriptor>`. If `Engine` is the new base type, `engine.click(d)` doesn't compile unless `click` is on `Engine`. Adding `click` to `Engine` defeats the abstraction. Not adding it requires every UI domain action to downcast: `((UIEngine) engine).click(d)`.

`ElementActions.of()` — on this branch — is typed as `BiConsumer<UIEngine, LocatorDescriptor>`. That type cannot accept `RobotEngine` without generics: `BiConsumer<E extends Engine, LocatorDescriptor>`.

The execution factory, the action lambda types, and `ElementBoundAction` all carry `UIEngine` explicitly. Generalizing the type pin in `Action.perform()` propagates into `ElementActions`, `ElementBoundAction`, and all capability lambdas. This is still refactoring, not redesign — but it is more surface area than the revised audit acknowledged.

**The deeper question:**

Under single-domain-per-session (one VOID session = one engine domain), actions never need to cross domain boundaries. A UI flow contains only UI actions. A Robot flow contains only robot actions. Under this model, the type generalization is sufficient and the action polymorphism problem is sidestepped entirely.

Under multi-domain-per-session (a flow that mixes UI actions and agent actions), the architecture would need domain-aware dispatch — probably visitor pattern or action registries. That is future architectural work.

**Classification:**

| Scope | Nature |
|---|---|
| Type-signature changes | Mechanical |
| Propagation through `ElementActions`, lambdas, capability interfaces | Mechanical (more surface area than claimed) |
| Single-domain-per-session model | Makes remaining changes mostly mechanical |
| Multi-domain coexistence in a single flow | Future architectural work — not yet needed, not yet designed |

**Verdict:** Partially mechanical. The revised audit underestimated the propagation surface. The simplest viable model (one domain per executor) contains the complexity, but that choice is architectural and hasn't been made explicitly.

---

## Challenge 2: Is `LocatorDescriptor` Actually Domain-Agnostic?

**The revised audit gave significant credit to** `LocatorDescriptor`, `LocatorSourceRegistry`, `LocatorTemplate`, and the `resolveDescriptor()` path.

**Testing against non-UI domains:**

### Robot domain

A robot arm's shoulder joint might be addressed as `"shoulder_pan"` with strategy `JOINT_NAME`. `LocatorDescriptor` could hold this structurally — the record is just `(String value, Strategy strategy, Object[] args, LocatorDescriptor parent)`. Structurally, it works.

But: `parent` encodes DOM scoping — "find this element within that parent element." Robot joints don't nest this way. A `GRIPPER` is not "within" an `ARM` the way a `<button>` is within a `<div>`. The scoping model is DOM-derived.

### Agent domain

An agent tool might be addressed as `"search_web"` with strategy `TOOL_NAME`. The record can hold this. But the concept of "locating" a tool is semantically wrong — you don't locate an agent tool, you identify or invoke it. The abstraction has the right shape but the wrong semantics.

### Workflow domain

A queue `"order-processing"` with strategy `QUEUE_NAME` fits the record structure. But "locating" a queue is even further from natural language — you reference it, you subscribe to it, you enqueue into it. The locator metaphor breaks.

**Structural vs. Conceptual fitness:**

`LocatorDescriptor` is structurally portable — the `(value, strategy, args, scope)` pattern can hold any domain's target address. But the naming (`locator`, `parent`, `LocatorStrategy`) encodes browser DOM semantics. The file-based resolution pipeline assumes design-time knowledge of targets — this works for UI elements but does not work for dynamically discovered agent tools or runtime workflow queues.

**Verdict:**

`LocatorDescriptor` is a transitional abstraction with a domain-agnostic structural pattern. It will evolve — likely into something named `TargetDescriptor` or `ResourceDescriptor` with a `scope` field replacing `parent` and an extensible strategy type. The revised audit overclaimed "genuinely domain-agnostic." The correct classification is: structurally portable, conceptually UI-derived, migration path visible.

The locator resolution pipeline (`LocatorSourceRegistry`, `LocatorTemplate`) survives the rename and concept generalization unchanged. The plumbing is sound. The model is transitional.

---

## Challenge 3: LLM Readiness — Two Independent Scores

**The challenge introduces a concrete pipeline:**

```
DOM → LLM Analysis → Element Discovery → Capability Assignment
 → Enum Generation → Locator Extraction → JSON Locator Asset
 → Migration CLI → VOID Runtime
```

The LLM generates descriptors, not Selenium code. The runtime executes.

---

### Which existing abstractions already support this pipeline?

| Pipeline Stage | Supporting Artifact | Status |
|---|---|---|
| Element Discovery → enum names | `Element` interface — simple contract, machine-implementable | ✅ Ready |
| Capability Assignment | `Clickable`, `Typeable`, `Selectable` — named, semantically clear | ✅ Ready |
| Enum Generation | Implements interfaces, declares locator keys — minimal boilerplate | ✅ Structurally ready |
| Locator Extraction → JSON | `JsonLocatorReader`, `JsonLocatorSource` — reads exactly this format | ✅ Ready |
| JSON Locator Asset format | `EnumLocatorScanner`, `JsonLocatorMigrator`, `JsonMigratorCli` | ✅ Tooling exists |
| LocatorTemplate for parameterized locators | `%s` placeholder formatting | ✅ Ready |
| Migration CLI | `JsonMigratorCli` already exists | ✅ Ready |
| Runtime execution of generated artifacts | `Flow.of(action...)`, `app.run(flow)` | ✅ Ready |
| Config-driven profile application | `void.profile.default`, `Profiles.fromName()` | ✅ Ready |

**The LLM is generating artifacts the runtime already knows how to consume.** This is the key observation.

Traditional Selenium POM — an LLM would generate:
```java
driver.findElement(By.xpath("//button[@id='login']")).click();
```
This requires: Selenium API knowledge, Java syntax, driver lifecycle awareness, XPath correctness. LLM error rate is high. Output is tightly coupled to Selenium.

VOID — an LLM generates:
```json
{ "LOGIN_BUTTON": { "trigger": "//button[@id='login']" } }
```
and:
```java
LOGIN_BUTTON implements Clickable { String getTriggerLocator() { return "LOGIN_BUTTON"; } }
```

The LLM does not need to know about Selenium, WebDriver, FluentWait, or any execution mechanism. It generates addresses and semantic classifications. The runtime handles everything else.

**This is the most structurally significant observation in this audit series.** The descriptor-first architecture is not accidentally LLM-friendly. It is structurally optimal for AI generation because it separates what an element IS (its address and capability) from how it is executed.

### Which parts would require redesign?

Nothing requires redesign for this pipeline. The architecture already supports it.

### Which parts need implementation work?

| Gap | Work Type |
|---|---|
| LLM integration (DOM → structured analysis) | Implementation — not built |
| Capability assignment schema (LLM output format) | Design + implementation |
| Enum code generator from LLM output | Implementation — scaffolded by existing patterns |
| Flow definition format for external/LLM-generated flows | Design + implementation |

---

### Scores

| Metric | Score | Basis |
|---|---|---|
| **Current LLM Implementation Readiness** | **2/10** | No LLM integration, no schema, no generator tooling; `JsonMigratorCli` exists but doesn't consume LLM output |
| **Architectural LLM Readiness** | **8/10** | JSON locator assets match LLM output format; capabilities are machine-assignable; no Selenium types in artifact layer; execution completely separated from description; `EnumLocatorScanner` and migration CLI show the tooling direction |

The gap between 2 and 8 is entirely an implementation gap. The architecture does not need to change to support AI-assisted page modeling. It only needs tooling to be built on top of it.

---

## Challenge 4: Is VOID Accidentally Becoming an AI-Native Runtime?

**The question:** Most frameworks assume Human → Code → Execution. This pipeline introduces LLM → Descriptors → Assets → Runtime.

**Assessment:**

This is not accidental. It is a structural consequence of three deliberate design decisions:

1. **Externalized locators** — JSON/properties files instead of hardcoded strings. An LLM can write a JSON file without Java knowledge.

2. **Capability-emitting interfaces** — Elements describe themselves through named interfaces. An LLM can assign `implements Clickable` without understanding execution.

3. **Descriptor-first resolution** — The runtime consumes `LocatorDescriptor`, not `By`. The generated JSON maps directly to `LocatorDescriptor` values.

These decisions, taken together, created a system where the knowledge layer (what elements exist, what they do, where they are) is fully decoupled from the execution layer (how actions are performed). AI systems generate knowledge. Runtimes execute it.

**Would an LLM find VOID easier to generate artifacts for than a traditional framework?**

Yes, categorically. The comparison:

| Dimension | Selenium POM | VOID |
|---|---|---|
| LLM output format | Java code with Selenium imports | JSON + interface annotations |
| Required API knowledge | WebDriver, By, FluentWait, ExpectedConditions | Key-value locator pairs |
| Error consequence | ClassCastException, NoSuchElementException | Missing key (recoverable) |
| Validation surface | Compilation + runtime | JSON schema validation |
| Iteration cost | Recompile + re-run | Update JSON file |
| Coupling to implementation | High (Selenium types embedded) | None (JSON is implementation-neutral) |

**Does this change the long-term architectural value assessment?**

Yes. The descriptor-first, capability-based, externalized-asset design positions VOID for AI tooling in ways that traditional frameworks cannot easily retrofit. This is not a small advantage. AI-generated test artifacts are already becoming a primary source of automation — frameworks that require LLMs to generate Selenium Java code will erode faster than frameworks that accept externalized descriptors.

---

## Challenge 5: Future-Domain Stress Test

**Test each primitive against UI, Robot, Agent, and Workflow domains simultaneously.**

| Component | Verdict | Reasoning |
|---|---|---|
| **`Action`** | **Survives** | "Deferred execution intent" is domain-agnostic. Under single-domain-per-session, actions never cross domains. The concept holds. |
| **`Flow`** | **Survives** | Pure sequential composition. No domain semantics. Unchanged across any engine. |
| **`FlowExecutor`** | **Survives** | A for-loop calling `action.perform(engine)`. After type-pin generalization, domain-neutral. |
| **`HookChainAction`** | **Survives** | Before/after hook orchestration. Zero domain semantics. The mechanism is domain-agnostic; only the hook implementations are domain-specific. |
| **`ActionProfile` (interface)** | **Survives** | The interface is domain-neutral. A `RobotProfiles` or `AgentProfiles` can be written without touching the interface. The built-in presets are UI-domain — they survive as UI-domain assets, not as runtime constraints. |
| **`LocatorDescriptor`** | **Evolves** | Structural pattern survives. Naming, `parent` scoping, and strategy enum need generalization to a `TargetDescriptor` model. Not a redesign — a rename with concept broadening. |
| **`ActionCapability`** | **Evolves** | The concept of capability-aware profile dispatch survives. The closed enum (CLICKABLE, TYPEABLE, SELECTABLE, UNKNOWN) does not. Must become an extensible open set — interface or SPI. |

**What this table reveals:** Six of seven runtime primitives survive domain substitution conceptually. The two that evolve (`LocatorDescriptor`, `ActionCapability`) evolve — they don't get replaced. No primitive requires redesign.

This is the strongest evidence for C+ over C.

---

## Challenge 6: The Next Hidden UI Assumption

After `Engine` is generalized, what cracks first?

**`ActionCapability`.**

Proof: the built-in profiles (`SAFE`, `RELIABLE`) dispatch via exhaustive switch:

```java
// Profiles.SAFE — current code:
return switch (action.capability()) {
    case TYPEABLE  -> List.of(Before.CLEAR_FIELD, Before.WAIT_FOR_ELEMENT_VISIBLE);
    case SELECTABLE -> List.of(Before.WAIT_FOR_ELEMENT_VISIBLE, Before.WAIT_FOR_ELEMENT_CLICKABLE,
                                Before.WAIT_FOR_ANGULAR_LOADER);
    case CLICKABLE  -> List.of(Before.WAIT_FOR_ELEMENT_CLICKABLE);
    case UNKNOWN    -> List.of(Before.WAIT_FOR_ELEMENT_VISIBLE);
};
```

When a developer introduces an `AgentEngine` and creates `SummarizeAction` with capability `SUMMARIZABLE`:

1. `SUMMARIZABLE` is not in the `ActionCapability` enum — won't compile.
2. Adding it to the enum triggers exhaustive switch warnings across `Profiles.SAFE`, `Profiles.RELIABLE` — forcing UI-domain profile code to handle a concept it doesn't understand.
3. The fallback `UNKNOWN` means agent actions get "wait for element visible" before execution — which fails immediately because no browser element exists.

This is the first practical breakage after Engine generalization. It is more hidden than `UIEngine` because `ActionCapability.UNKNOWN` provides a syntactic escape, but that escape produces semantically wrong behavior for non-UI domains.

**Why this is worse than `UIEngine`:** The `UIEngine` type pin fails loudly at the type boundary — a developer knows immediately what's wrong. `ActionCapability.UNKNOWN` fails silently at runtime — agent actions execute with browser wait hooks, and the developer must trace why their agent calls are timing out.

**The runner-up:** The concept of "locating" (`LocatorDescriptor`, `LocatorStrategy`, `LocatorSource`). After `ActionCapability` is fixed, the naming and scoping model of the locator system becomes the next friction point when building a non-UI domain. A robot developer will ask "what is a locator?" with no intuitive answer.

---

## Challenge 7: Multi-Domain Readiness — Two Scores

**The revised audit scored this at 3/10 as a single number.**

The challenge requires splitting current implementation readiness from architectural readiness.

### Current Multi-Domain Readiness: 2/10

Revised downward from 3/10. The `ActionCapability` closed enum is an additional blocker not fully accounted for in the revised audit. A second domain introduced today faces:

1. `Action.perform(UIEngine)` — type prevents non-UI engine
2. `UIEngineFactory.create(Properties, WebDriver)` — requires Selenium WebDriver
3. `VOID.start()` creates WebDriver unconditionally
4. `ActionCapability` is a closed enum — new domain actions can't declare their capability
5. `Profiles.SAFE`/`RELIABLE` switch on `ActionCapability` — exhaustive, not extensible
6. `LocatorStrategy` is a closed enum — non-browser strategies cannot be added

None of these require redesign. All of them must be fixed. The current implementation genuinely cannot host a second domain today.

### Architectural Multi-Domain Readiness: 7/10

| What survives domain substitution | Weight |
|---|---|
| `Flow`, `FlowExecutor`, `HookChainAction` | High — pure mechanics, no domain semantics |
| `ActionProfile` interface | High — domain-agnostic mechanism |
| JSON locator assets + `LocatorSourceRegistry` | High — domain-agnostic plumbing |
| `LocatorDescriptor` structural pattern | Medium — survives conceptually, needs rename |
| `SessionContext` — `UIEngine` not `WebDriver` | Medium — right direction, not wired |
| Factory config/env engine selection | Medium — structural hook for multi-engine |

Deductions from 10:
- No abstract `Engine` type exists (-1)
- `ActionCapability` closed enum is a new introduction that regresses agnosticism (-1)
- `LocatorDescriptor.parent` DOM scoping (-0.5)
- `waitForOverlay` CDK coupling in UIEngine contract (-0.5)

Score: 7/10. The architecture is designed for generalization. The implementation has not caught up.

---

## Final Deliverables

### 1. Revised Conclusions That Remain Strongest

**A. The runtime execution model is conceptually domain-agnostic.**
`Flow`, `FlowExecutor`, `HookChainAction`, and `ActionProfile` (the mechanism, not the presets) survive domain substitution without redesign. This is the most important structural finding and it was correct in the revised audit.

**B. `SessionContext` replacing `ExecutionContext` is active migration evidence.**
`SessionContext` holds `UIEngine`, not `WebDriver`. Its Javadoc says explicitly why it replaces its predecessor. This is not aspiration — it is code that exists.

**C. The architectural LLM readiness is high (8/10) despite zero current implementation.**
This finding emerges most clearly in this third round. The descriptor-first, externalized-asset, capability-classified architecture is structurally optimal for AI generation. No other mainstream automation framework achieves this without retrofitting.

**D. The blocks are transitional, not structural — the classification of C+ is defensible.**
The runtime model mostly survives domain substitution. The boundary abstractions need evolution. This matches C+ as defined.

---

### 2. Where the Revised Audit Was Too Optimistic

**A. `Action.perform(Engine)` propagation was underestimated.**
The revised audit said "six type signatures." The actual propagation includes `ElementBoundAction`, `ElementActions`, capability lambda types — not a different conclusion but a larger surface area than claimed. This is still not a redesign, but it is more refactoring than stated.

**B. `LocatorDescriptor` was overcredited as "genuinely domain-agnostic."**
The structural pattern is portable. The conceptual model — "locating" a target by XPath/CSS/ID/NAME within a parent DOM element — is browser-derived. A transitional abstraction is the accurate description.

**C. `ActionCapability` as a new blocker was missed entirely.**
The revised audit identified `ActionCapability` as "implementation coupling." That was too mild. `ActionCapability` is embedded in the profile dispatch system via exhaustive switch statements in `Profiles.SAFE` and `Profiles.RELIABLE`. A new domain cannot use the profile system without modifying the enum, which triggers changes across the profile library. This is the next migration bottleneck after Engine generalization. The revised audit should have flagged this as a higher-severity concern.

**D. Multi-Domain Readiness at 3/10 was slightly generous.**
The correct score for current implementation is 2/10. The `ActionCapability` blocker and the exhaustive switch-based dispatch add practical friction that compounds the type-pin issues.

---

### 3. The Most Underappreciated Risk

**`ActionCapability` as the next closed-enum bottleneck in the profile dispatch system.**

After Engine is generalized — which is the obvious next migration step — developers will immediately attempt to build non-UI domains. The first thing they will do is create domain-specific actions. The first thing those actions need is capability classification for the profile system. At that point they discover `ActionCapability.CLICKABLE/TYPEABLE/SELECTABLE` is a closed enum that cannot be extended without modifying framework code.

This is worse than the `UIEngine` pin for three reasons:

1. **Silent failure mode.** Unlike a compile error, actions with `UNKNOWN` capability still execute — just with wrong semantics (browser wait hooks on a robot action).

2. **New introduction.** `ActionCapability` didn't exist on the previous branch. It was introduced on this branch as a new feature. If the pattern hardens — more profiles, more switch statements — the cost of making it extensible increases.

3. **Proximity to the runtime core.** `ActionCapability` is wired into `ElementActions.of()` which is wired into every capability interface. The classification propagates from element creation through profile application to hook execution. It is embedded in the action factory that all domain code will use.

---

### 4. What Would Crack First When a Second Domain Is Introduced

Ranked by order of encounter during development:

| Rank | What Breaks | How It Breaks |
|---|---|---|
| 1 | `ActionCapability` closed enum | New domain's capabilities don't exist in enum; `UNKNOWN` fallback produces wrong behavior |
| 2 | `UIEngineFactory.create(Properties, WebDriver)` | Creating a non-Selenium engine through the factory requires passing a `WebDriver` that has no meaning |
| 3 | `Action.perform(UIEngine)` type pin | Non-UI engine cannot be passed to `perform()` |
| 4 | `VOID.start()` unconditional WebDriver creation | Session startup always creates a browser; no entry point for non-browser domains |
| 5 | `LocatorStrategy` closed enum | Robot/agent locator strategies don't exist; cannot describe non-browser targets |
| 6 | `LocatorDescriptor.parent` DOM scoping | Nested lookups work for DOM but not for joint hierarchies or agent tool registries |

---

### 5. Updated Classification

**C+**

By the classification scale definition:

> C+ — Runtime Model Mostly Survives Domain Substitution
> Core runtime concepts survive domain substitution. Most remaining blockers are boundary abstractions. Migration path is visible and credible. Second domain cannot yet be added easily. Architecture appears designed for generalization even if implementation is incomplete.

Test: *If a second domain were added, would most runtime concepts survive unchanged?*

**Mostly yes.**

- `Flow` — survives unchanged
- `FlowExecutor` — survives after one type signature change
- `HookChainAction` — survives unchanged
- `ActionProfile` (mechanism) — survives unchanged
- JSON locator infrastructure — survives unchanged

What requires evolution:
- `Engine` boundary — `UIEngine` needs a supertype
- `ActionCapability` — closed enum needs to become open
- `LocatorDescriptor` — naming and scoping model needs generalization

What does not apply (D- requires "second-domain architecture already defined"):
- No second domain has been architecturally specified. The migration path is visible but the destination isn't drawn.

D- would require the architecture itself to no longer be the limiting factor. `ActionCapability` as a newly-introduced closed-enum bottleneck means the architecture introduced a new constraint even as it resolved old ones. That keeps it at C+ rather than D-.

---

### 6. Most Important Question

**Is VOID primarily an unfinished domain-agnostic runtime, or a UI runtime that currently appears generalizable?**

**An unfinished domain-agnostic runtime.**

The distinction matters because it changes the interpretation of every coupling point. A UI runtime that "appears generalizable" has abstractions that were retrofitted for appearance. An unfinished domain-agnostic runtime has abstractions that were built for generalization but haven't been fully connected.

Evidence that VOID is the latter:

**Deliberate migration actions, not cosmetic ones:**
- `SessionContext` was written specifically to hold `UIEngine` instead of `WebDriver`, with Javadoc explaining the WebDriver coupling it replaces. You don't write a replacement class for an appearance.
- `resolveDescriptor()` was added alongside `resolve()` as the target path. The deprecated `resolve()` returning `By` was not removed yet — that is migration behavior.
- `// case "playwright" -> new PlaywrightEngine(); // Phase 3` — an explicit future mapping in the engine factory.
- `UIContext.getLastElement()` deprecated in favor of `getLastActionTarget()` returning `LocatorDescriptor` — not cosmetic.

**Architectural decisions that pre-commit to generalization:**
- JSON locator files decouple element addresses from code — a framework that planned to stay Selenium-only would not need this.
- The `ActionProfile` interface was built composable and externally configurable (`void.profile.default` property) — a UI-only framework would hardcode profiles.
- `LocatorSourceRegistry` as a chain of responsibility for locator lookup — this is overengineered for a single-domain tool and correctly engineered for a platform that expects new source types.

**What a "UI runtime that appears generalizable" would look like:**
- Thin interface over Selenium with a renamed type or two
- No external asset format
- No pluggable locator sources
- No profile system
- No capability emission model (actions emitted at execution time, not declaration time)

VOID has none of these characteristics. The capability-emitting-action model, external JSON assets, profile composition, and `LocatorDescriptor` abstraction are not cosmetic. They are an execution model designed for a world where the runtime does not own knowledge generation.

**The honest constraint:** This is an unfinished domain-agnostic runtime that introduced a new constraint (`ActionCapability`) on the path to completion. The trajectory is correct. The current state includes both migration progress and migration regression. The classification C+ reflects that honestly.

---

*Third-round assessment against updated branch. All findings reference source code read in this session.*