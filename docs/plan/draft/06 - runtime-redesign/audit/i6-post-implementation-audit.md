**Initiative:** I6 Domain Registration
**Type:** Post-implementation audit
**Date:** 2026-07-31
**Branch:** `initiative/domain-registration`
**Status:** CLEAN -- no hotfix needed

---

## Exit-criteria checklist

### I6.1 -- Domain registration contract

| Criterion | Result |
|---|---|
| Runtime bootstrap consults only the registration surface to learn what domains exist | PASS |
| `DomainRegistry` has no compile-time dependency on any domain implementation | PASS (ArchUnit `domainRegistryIsImplementationFree`) |
| `VOIDBuilder` reads domain + engine via registration surface only | PASS |
| Suite green | PASS |

### I6.2 -- Web domain assembly and ownership audit

| Criterion | Result |
|---|---|
| Sweep table has no "unassigned" row | PASS -- every main-tree package assigned to kernel / web-domain / observability / tooling / legacy |
| Class Migration Matrix complete (zero unassigned rows) | PASS -- 74+ types classified, matrix committed to docs |
| Startup path: bootstrap -> registration -> session(web) -> pipeline | PASS (covered by existing session + integration tests) |
| Suite green | PASS |

### I6.3 -- Probe domain (neutrality CI gate)

| Criterion | Result |
|---|---|
| Probe exercises: registration, session creation, capability validation, interaction dispatch, hooks, tracing | PASS |
| Probe green in CI | PASS -- `StoreDomainNeutralityTest` passes in every `mvn test` run |
| Zero edits to runtime-owned `src/main/java` files required to enable the probe | PASS (probe is test-scope only) |
| Documented as permanent invariant check | PASS (gate label in test name; ArchUnit + TestNG suite include it) |

### I6.4 -- Physical domain package relocation

| Criterion | Result |
|---|---|
| `domain.automation.web.*` contains exactly the matrix's entries | PASS |
| No kernel package references `domain.automation.web.vocabulary.*` or `domain.automation.web.selenium.*` | PASS (ArchUnit `kernelPackagesDoNotDependOnElements`) |
| `domain.automation.web.vocabulary.*` is Selenium-free | PASS (ArchUnit `elementsApiIsSeleniumFree`, `uiActionsAreSeleniumFree`) |
| `LocatorDescriptor` is Selenium-free | PASS (ArchUnit `locatorDescriptorIsSeleniumFree`) |
| `UIEngineFactory` does not depend on `domain.automation.web.selenium` | PASS (ArchUnit `engineFactoryIsImplementationFree`) |
| Kernel purity gate green | PASS (ArchUnit `kernelPurity`, 1100 tests) |
| Visibility audit: zero reflexive `public` widenings | PASS -- all package-private classes kept package-private |
| Zero remaining imports of pre-move FQNs in non-deprecated production code | PASS |
| CHANGELOG FQN mapping table published | PASS -- full table under `[Unreleased]` in CHANGELOG.md |
| `SeleniumDriverContext` field check updated to new FQN | PASS (tightened in this session) |
| `DomainRegistry` implementation-free check updated to new package | PASS (tightened in this session) |
| Stale `core/driver/README.md`, `elements/api/README.md`, `core/engine/selenium/package-info.java` removed | PASS |
| package-info.java added to all 16 `domain.automation.web.*` packages | PASS |
| Suite green | PASS -- 1100 tests, 0 failures, 0 errors |

---

## Tracked gaps (not violations)

**G1 -- `Waiter` still returns `WebDriverWait` (open ADR-007 violation).**
Three callers (`Upload.java`, `KeyValuePairHandler.java`, `WaitUtils`) depend on
`WebDriverWait` from `Waiter`. Intentionally not fixed in I6.4 -- it is a behavior
change, and 6.4 is relocation-only (guardrail rule 1). Tracker file moved with the
package; two of the three callers are not on the I9.2 graveyard list. Remains open
for a dedicated fix post I9.

**G2 -- `SeleniumDriverFactory` `instanceof` preference dispatch (P-OCP, lines 722-724).**
Low-cost OCP violation noted during relocation; `SeleniumDriverFactory.java` was
touched only for package rename, not logic changes. Logged to
`docs/audits/backlog/violations/`; fix deferred to avoid mixing relocation with a
behavior change in the same commit (guardrail rule 1).

**G3 -- `domain.automation.web.engine.UIEngine` and `LocatorDescriptor` remain in kernel purity exceptions.**
`Action.perform()`, `ActionHandler.execute()`, `HookChainAction`, `SessionContext`,
and `VOID`/`VOIDBuilder` still carry `UIEngine` and `LocatorDescriptor` in their
signatures. These are pre-existing bridge exceptions, cross-referenced to I9.4
(bridge-method deletion phase). Not introduced by I6; tracked and named in
`KERNEL_PURITY_TEMPORARY_EXCEPTIONS`.

**G4 -- `elements.locator` is an intermediate home for `LocatorDescriptor` and `LocatorStrategy`.**
The I7.2 audit noted these would receive a final relocation in I6.4. That relocation
happened: both now live at `domain.automation.web.locator.*`. The I7 audit's G3 is
therefore closed by this initiative. No remaining stale intermediate package.

---

## Verdict

All exit criteria pass. All gaps are pre-existing or properly deferred items with named
closing phases. Fitness checks tightened to their post-I6.4 final form. I7 audit gap G3
closed. No hotfix initiative is warranted.
