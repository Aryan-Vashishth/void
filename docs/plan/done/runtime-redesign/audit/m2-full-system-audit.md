# Full-System Audit: M2 Milestone (runtime-redesign)

**Date:** 2026-07-28
**Branch:** `initiative/runtime-redesign` (pre-merge to main, 0.5.0)
**Scope:** I1 Target Model, I2 Kernel Extraction, I3 Capability Model
**Governing ADR:** ADR-021 (pending-review)
**Verdict:** PASS -- all M2 objectives met; no new violations; deferred items explicitly tracked

---

## Phases Verified

| Phase | Commit | Compile | Suite | Outcome |
|---|---|---|---|---|
| I1.1 -- Target root | `da84e31` | Pass | Pass | `core.target.Target` introduced; domain-neutral root for Action, UIElement, future domain types |
| I1.2 -- UIElement rename | `484c122` | Pass | Pass | `Element` renamed `UIElement`; extends `Target`; deprecated alias in place |
| I1.3 -- Docs sync | `df1ad36` / `c5d4075` | Pass | Pass | `elements.md`, `actions.md`, package-info updated; no production change |
| I1.4 -- Kernel target-neutrality ratchet | `b0969c8` | Pass | Pass | `actionsKernelIsTargetNeutral` ArchUnit rule added; `core.actions` confirmed UIElement-free |
| I2.1 -- Hook contract relocation | `1f7957f` | Pass | Pass | `ActionHandler`/`BeforeActionHandler`/`AfterActionHandler` moved to `core.actions.hooks`; deprecated bridges in `core.interactions.hooks` |
| I2.2 -- Kernel/UI action split | `888cde7` | Pass | Pass | `ElementAction` family (3 abstract, 17 concrete) moved `core.actions` -> `elements.api.actions`; `CapabilityProfiles` extracted; `core.actions` zero UIElement/ElementRole dependency |
| I2.3 -- Cycle break ratchet | `0bef85e` | Pass | Pass | `kernelPackagesDoNotDependOnElements` ArchUnit rule added |
| I2.4 -- Kernel purity gate | `2f82938` | Pass | Pass | `kernelPurity` consolidates I2.1-I2.3; positive allowlist with 12 named, cross-referenced temporary exceptions |
| I3.1 -- Open capability set | `f534414` | Pass | Pass | `ActionCapability` converted from closed enum to open interface; `NamedCapability` record backs value equality; 15 built-in constants preserved; extension fitness test added |
| I3.2 -- UNKNOWN fallback removal | `18e8b87` | Pass | Pass | `safely()` throws on UNKNOWN capability; `applyConfiguredDefault()` guards and warns; `configuration-reference.md` created |
| I3.3 -- Neutral capability contract | `4cfc90c` | Pass | Pass | All 15 capability interfaces declare Web-domain ownership; `package-info.java` updated; `elements.md` gains ownership table; `kernelCapabilityReferencesAreContractTypedOnly` ArchUnit rule added |

Final suite run before this audit: **1205 tests, 0 failures, 0 errors.**

---

## Architecture Invariant Check

| Invariant | Status | Evidence |
|---|---|---|
| `UIEngine` is the single execution authority (ADR-007) | Pass | No new direct `WebDriver` callsites introduced |
| Engine-agnostic layers are Selenium-free (ADR-018) | Pass | `core.runtime`, `core.interactions`, `dsl` unchanged; pre-existing gaps tracked in Migration Ledger |
| `LocatorDescriptor` is Selenium-free (ADR-019) | Pass | Not touched in M2 |
| `ElementSupport` scope frozen (ADR-017) | Pass | Not touched in M2 |
| `Target` carries no enum-specific defaults (I1) | Pass | `core.target.Target` has no `Enum<?>` cast or ordinal logic; `actionsKernelIsTargetNeutral` green |
| `VOIDBuilder` is single-use (ADR-018) | Pass | Not touched in M2 |
| Kernel purity (ADR-021, I2.4) | Pass | `kernelPurity` green; 12 temporary exceptions named and cross-referenced to closing phases |

---

## M2 Objective Verification

| Objective | Requirement | Met by | Status |
|---|---|---|---|
| Domain-neutral subject root | `core.target.Target` exists; `UIElement` extends it | I1.1, I1.2 | Done |
| Kernel free of UI imports | `core.actions` and sibling packages have zero `elements.*` dependency | I2.2, I2.4 | Done |
| Capability set open | `ActionCapability` is an extensible interface; new domains use `ActionCapability.of()` | I3.1 | Done |
| No UNKNOWN silent hooks | `safely()` and `applyConfiguredDefault()` fail fast for UNKNOWN capability | I3.2 | Done |
| Capability ownership declared | All 15 capability interfaces carry Web-domain ownership; kernel fitness check green | I3.3 | Done |

---

## Seeding Audit Resolution (2026-07 Part I)

Findings from `docs/audits/ongoing/architecture-audit-2026-07-domain-model.md` resolved by M2:

| Finding | Description | Resolution |
|---|---|---|
| D1 | Kernel/UI-domain fusion; `elements.api` and `core.actions` mutually dependent | Resolved -- I2.2 physically separated the bounded context; I2.4 automated enforcement |
| D4 | Modern `core.actions` depends on deprecated-by-location `core.interactions.hooks` | Resolved -- I2.1 relocated hook contracts to `core.actions.hooks`; deprecated bridges remain in `core.interactions.hooks` pending I9.3 |
| D18 | Closed enums at extension points; `UNKNOWN` silent hook application | Resolved -- I3.1 opens `ActionCapability`; I3.2 removes silent fallback |

---

## Deferred Findings

Findings explicitly carried forward; each has an assigned initiative and is documented in the Migration Ledger or ongoing audit.

| Finding | Description | Assigned to |
|---|---|---|
| D2 | Contract depends on implementation (`UIEngineFactory` switch-on-string, P8) | I4.1 |
| D3 | Contract depends on platform (`EngineBootstrap` / `UIEngineFactory` Selenium compile dependency) | I4.2 |
| D5 | `dsl` depends on legacy pipeline (`core.interactions`) | I9.1 |
| D7 | `UIEngine.resolve(Element, role)` bypasses Action ownership; three resolution-truth sources | I7, I4 |
| D17 | Single-artifact enforcement; ArchUnit ratchets added in M2 but no module split | Ongoing ratchet; physical boundary I6.4 |
| Kernel purity temporary exceptions (12) | `UIEngine`, `LocatorDescriptor`, `EngineBootstrap`, `UIEngineFactory`, `DriverFactory`, `ConfigLoader`, `ConfigPaths`, `Before`, `After`, `Interactions`, `WebDriver` | Individually cross-referenced in `kernelPurity` javadoc; each has a closing phase |

---

## Hotfix Assessment

No hotfix initiative required. No blocking findings uncovered during this audit.

---

## Recommendations Before Merge

1. **Bump version to 0.5.0** in `pom.xml` and `version.json`; cut `## [0.5.0]` entry in `CHANGELOG.md`.
2. **Promote ADR-021** from `docs/decisions/pending-review/` to `docs/decisions/accepted/` after merge.
3. **Update `docs/audits/ongoing/architecture-audit-2026-07-domain-model.md`** -- mark D1, D4, D18 resolved with M2 cross-references (done in same commit as this audit).
4. **Next initiative:** I4 Execution Boundary on a fresh `initiative/execution-boundary` branch from post-merge main.
