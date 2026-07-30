**Initiative:** I7 Locator Generalization
**Type:** Post-implementation audit
**Date:** 2026-07-30
**Branch:** `initiative/locator-generalization`
**Status:** CLEAN -- no hotfix needed

---

## Exit-criteria checklist

| Criterion | Result |
|---|---|
| `LocatorStrategy` is an open interface; no exhaustive iteration outside deprecated paths | PASS |
| `core.engine.LocatorStrategy` does not exist | PASS (new ArchUnit `coreEngineHasNoLocatorTypes`) |
| `LocatorDescriptor` and `LocatorStrategy` in `elements.locator` | PASS |
| Kernel purity check green | PASS |
| `LocatorResolver.resolve()` deprecated; no new non-deprecated callers | PASS |
| 3 production call sites migrated (WaitUtils, KeyValuePairHandler, Upload) | PASS |
| Suite green | PASS |

---

## Tracked gaps (not violations -- each is properly named and cross-referenced)

**G1 -- `LocatorResolver.resolve()` deprecated, not deleted.**
Via.locator() and Interactions remain callers; both are @Deprecated(forRemoval=true)
themselves. Full deletion tracked in I9.3 alongside the Via/Interactions removal pass.

**G2 -- ByParser / ByPrefixStrategy deprecated, not moved.**
The optional "move ByParser to core.engine.selenium" step was deferred. PropertiesFileLocatorReader
is the only remaining live consumer; it is a @Deprecated-annotated legacy bridge. Deletion
in I9.3 with the rest of the legacy resolver surface.

**G3 -- elements.locator is an intermediate home.**
LocatorDescriptor and LocatorStrategy live at elements.locator, not at a fully-qualified
domain package. Final relocation (domain.automation.web.* or equivalent) deferred to I6.4
once the domain registration initiative settles the canonical Web-domain package structure.

**G4 -- Kernel bridge methods carry LocatorDescriptor.**
Action.resolve(), ActionHandler.execute(), and HookChainAction still reference
elements.locator.LocatorDescriptor in their kernel-side signatures. Listed and reasoned in
KERNEL_PURITY_TEMPORARY_EXCEPTIONS. Closes in I9.4.

---

## Verdict

All exit criteria pass. All gaps are pre-existing tracked items with named closing phases.
No hotfix initiative is warranted.
