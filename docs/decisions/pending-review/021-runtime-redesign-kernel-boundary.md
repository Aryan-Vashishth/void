# ADR-021 -- Runtime Redesign: Kernel Boundary, Ontology, and Open Decisions

**Date:** 2026-07-22
**Status:** Pending Review (`initiative/runtime-redesign`)

---

## Context

The 2026-07 architecture audit (`docs/audits/ongoing/architecture-audit-2026-07-domain-model.md`)
identified two co-existing problems:

**Implementation gap (Part I findings D1-D18):** The framework evolved Selenium-first.
Engine-agnostic layers (`core.runtime`, `core.interactions`, `dsl`) carry direct Selenium
coupling; `LocatorDescriptor` lives in the engine contract package; the bootstrap fails
without `driver.properties`; the legacy `Interactions` class acts as a gravitational center
preventing cleanup.

**Ontological gap (Part II findings O1-O9):** The five-concept model (Runtime, Interaction,
Capability, Target, Domain) has no named execution owner -- the concept that carries a
Capability set and knows how to execute an Interaction against a set of Targets. This
unnamed gap (O1) means the redesign cannot consistently name the seam between kernel and
domain. Three additional structural questions were left open (AD1-AD3) because they are
genuinely architectural choices with no clear dominant option.

ADR-021 resolves the decisions that gate all 37 subsequent phases. It records: the two
neutrality axes, the closed kernel membership list, the formal ontology, and the three
open-decision resolutions.

---

## Decision

### Neutrality axes

The framework has two independent neutrality axes. Every invariant governs exactly one.

| Axis | Question | ADRs |
|---|---|---|
| **Engine neutrality** | Does this seam prevent swapping Selenium for Playwright or Appium? | ADR-007, ADR-018, ADR-019 |
| **Domain neutrality** | Does this seam prevent adding REST, CLI, or Database alongside Web/UI? | This ADR, governing I1-I9 |

A seam that removes Selenium dependency but still assumes a browser domain satisfies
engine neutrality, not domain neutrality. Both axes must be honored independently.

### Kernel membership (closed list)

The kernel is the domain-neutral center of the runtime. It may grow only by ADR.

**In the kernel:**

| Type | Current name | Notes |
|---|---|---|
| Interaction description | `Action` | renamed to `Interaction` in I9.4 |
| Interaction collection | `Flow` | stays |
| Orchestrator | `FlowExecutor` | stays; domain-neutral after I4 |
| Profile | `ActionProfile` / `ActionProfiles` | stays; naming reviewed in I9.4 |
| Hook contract | `BeforeActionHandler`, `AfterActionHandler`, `ActionHandler` | neutral hook interfaces; domain-specific hook payloads live in domain |
| Occurrence record | `ActionTrace` | stays; formal occurrence promotion in I8.1 |
| Runtime facade | `VOID`, `VOIDBuilder` | stays; surface frozen until I9.5 |
| Session | `SessionContext` (evolved) | first-class in I5 |
| Capability descriptor | `ActionCapability` | stays; open-set in I3 |
| Executor contract | new `Executor` interface | introduced in I4; domain side provides its implementation |

**Not in the kernel (domain-side):**

- `UIEngine` -- web domain's executor contract (implements `Executor`); Selenium, Playwright, Appium implement it
- `UIElement` / `Element` -- web domain's Target vocabulary (renamed in I1)
- `LocatorDescriptor` -- web domain's addressing type (moved in I7.2)
- `LocatorStrategy` -- web domain (open-set in I7.1)
- `Via` / `Interactions` -- legacy; deleted in I9.3
- `core.utils.web.*` -- legacy; deleted in I9.2

### Ontology adoption

The five concepts from Part II are formally adopted as the framework's design vocabulary.

| Concept | Code mapping (current) | Direction |
|---|---|---|
| **Runtime** | `VOID` + `FlowExecutor` + `FrameworkBootstrap` | consolidate; Runtime orchestrates, does not execute |
| **Session** | `VOID` (smeared) | first-class type in I5 |
| **Interaction** | `Action` | code name unchanged until I9.4 frees the noun |
| **Capability** | `ActionCapability` | open-set from I3 onward |
| **Target** | `elements.api.Element` | neutral `Target` root in I1; `UIElement` is the web-domain refinement |
| **Domain** | no code concept yet | registration contract in I6 |
| **Executor** | `UIEngine` (web-domain only) | kernel `Executor` interface in I4; `UIEngine` implements it |

**Scope limit:** the ontology covers discrete, bounded interactions. Streaming, continuous,
or stateful-loop execution semantics are out of scope until a concrete domain requires them
(audit O9).

### AD1 -- Session-to-Domain cardinality

**Resolution: one session binds exactly one domain at creation time.**

A session is created for a specific domain. Multi-domain work uses multiple sessions,
orchestrated by calling code. The session's executor is fixed at `VOIDBuilder.start()`.

**Rationale:**
- Simplest runtime: the executor slot is set once, never swapped.
- The probe domain (I6.3) demonstrates the model with a single domain; this forces
  the registration contract to be complete, not a shortcut.
- Relaxing to one-to-many is additive (no existing call site breaks) and can be done
  in a future ADR once a real multi-domain requirement exists. There is no known
  requirement today, so we pay no option price by starting here.
- REST + Web hybrid: each gets its own session; a test method holds two VOID instances.
  This is explicit, auditable, and avoids accidental cross-domain interaction dispatch.

**Withdrawn invariant:** Audit invariant I8 ("Session binds exactly one Domain") was
listed as withdrawn from the invariants table because it is a choice, not a logical
necessity. This ADR makes it a recorded choice.

### AD2 -- Execution-owner name and shape

**Resolution: the concept is named `Executor`. It is a kernel interface.**

```
Executor (kernel, neutral)
    ^
    |
UIEngine (web domain, extends Executor)
    ^
    |
SeleniumEngine, PlaywrightEngine  (web domain implementations)
```

The kernel introduces `core.engine.Executor` (or a package TBD in I4) -- a neutral
interface with one execution contract: receive an Interaction, a Session, and a set of
Targets; return a Result. The kernel dispatches to `Executor`. No kernel type references
`UIEngine`.

`UIEngine` is the web domain's refinement of `Executor`. It carries the web-specific
execution contract (locator resolution, hooks with `LocatorDescriptor`, driver lifecycle).
It is NOT removed or merged; its existing API continues to be the contract Selenium and
future Playwright engines implement. After I4, `UIEngine extends Executor`.

**Rationale:** "Executor" is the established term for the component that carries a
Capability set and translates interaction descriptions into platform operations. It is
domain-neutral at the kernel level and domain-specific in its refinements. Alternatives
considered: Dispatcher (routing connotation, wrong), Interpreter (language connotation,
wrong), DomainRuntime (duplicates "Runtime"), OperationHandler (verbose).

**Implication for I4:** I4's primary deliverable is the `Executor` interface and the
registry that maps domains to `Executor` factories.

### AD3 -- Validation timing

**Resolution: validation occurs at dispatch time, before execution begins.**

When a session dispatches an interaction, the runtime validates that the interaction's
declared capabilities are all supported by the session's registered Executor before any
execution step runs. Validation is synchronous and blocking.

**Rationale:**
- Fail fast: a capability mismatch surfaces immediately on dispatch, not mid-execution.
- Context available: at dispatch time the session knows its Executor (which holds its
  capability registry), making validation straightforward.
- Avoids complexity of description-time validation (no session context yet) and
  execution-time validation (partial execution possible before failure).

**Implementation note (I4):** The dispatch path in `FlowExecutor` grows a validation
step that calls `session.executor().supports(interaction.capabilities())`. This check
runs before the Executor's `execute()` is invoked.

---

## Scope constraints

### This ADR decides; it does not design APIs

The `Executor` interface signature, the Domain registration contract, and the Session
contract are implementation decisions belonging to I4, I5, and I6 respectively. This ADR
establishes the concept name and its position in the architecture; the phase plans define
the Java API.

### Kernel membership is closed from this point

No type may be added to the kernel membership list without an ADR. This is guardrail rule
7 in the runtime-redesign roadmap index.

### Engine neutrality invariants carry forward unchanged

ADR-007 (UIEngine as execution authority), ADR-018 (engine lifecycle ownership), and
ADR-019 (LocatorDescriptor isolation) remain in force. This ADR adds domain neutrality
as a second axis; it does not replace the engine neutrality work already done.

---

## Consequences

- The kernel boundary check in Phase 0.2 (`KernelBoundaryRulesTest`) is the automated
  enforcement of this ADR's closed list. Future phases tighten it as boundaries are won.
- Every subsequent phase that introduces a new type must state which side of the
  kernel/domain line it occupies.
- `UIEngine` gains a supertype (`Executor`) in I4. Its existing API is unchanged.
- `core.engine.Executor` (or its final package location, per I4's scope) is introduced
  as a kernel type. It does not exist yet.
- The vocabulary `Action`, `Flow`, `ActionCapability` etc. is stable until I8/I9 land
  the naming changes. ADR-021 names the target vocabulary; the code names change only
  when I9.4 executes the mapping table.

---

## Related

- Architecture audit: `docs/audits/ongoing/architecture-audit-2026-07-domain-model.md`
- Runtime-redesign roadmap: `docs/plan/draft/runtime-redesign/index.md`
- [ADR-007 -- UIEngine as Single Execution Authority](../accepted/007-uiengine-execution-authority.md)
- [ADR-018 -- Engine Lifecycle Ownership](../accepted/018-engine-lifecycle-ownership.md)
- [ADR-019 -- SeleniumLocatorBridge Isolation](../accepted/019-selenium-locator-bridge.md)
- [ADR-020 -- Core Utils Selenium Decoupling](../accepted/020-core-utils-selenium-decoupling.md)
