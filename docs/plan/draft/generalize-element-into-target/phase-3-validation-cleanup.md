# Phase 3 -- Validation and Cleanup

Touches: documentation files; no production Java source changes.

---

## Goal

Confirm the rename is complete, regression-free, and documented. After this phase:

- No source file anywhere in the project references `elements.api.Element`.
- All relevant documentation reflects `UIElement` and `Target`.
- The initiative is closed.

This is a documentation and verification phase. No production code changes.

---

## Verification checklist

Run these before making any documentation changes. All must pass cleanly.

```
# 1. Full build and test suite
mvn clean test -q

# 2. No remaining Element type references in production sources
grep -rn "elements\.api\.Element" src/main/java/
# expected: zero results

grep -rn "implements Element[^R]" src/main/java/
# expected: zero results (ElementRole is fine; catches raw `implements Element`)

grep -rn "import elements\.api\.Element" src/
# expected: zero results (covers both main and test trees)

# 3. UIElement correctly extends Target in all critical files
grep -n "extends Target" src/main/java/elements/api/UIElement.java
# expected: exactly one result

grep -n "UIElement" src/main/java/core/engine/UIEngine.java
# expected: import + resolve() parameter

# 4. Target carries the right methods, no locator leakage
grep -n "LocatorDescriptor\|ElementRole\|locator\|Locator" src/main/java/core/target/Target.java
# expected: zero results

# 5. No behavioral regression
mvn test -q
# expected: all tests pass
```

If any check above fails, return to Phase 2 to fix the missed reference before proceeding.

---

## Documentation changes

### `elements/api/UIElement.java` Javadoc

Confirm the class-level Javadoc:
- Mentions `extends Target` and points to `core.target.Target`
- States the enum-only constraint (ClassCastException warning) clearly
- Does not reference `Element` anywhere

### `core/target/Target.java` Javadoc

Confirm the class-level Javadoc:
- Points to `elements.api.UIElement` via `@see`
- Does not mention locators, roles, or Selenium

### `core/engine/UIEngine.java` Javadoc

Update any inline reference to `Element` in method or class Javadoc to `UIElement`.

### `docs/plan/draft/generalize-element-into-target/index.md`

No changes needed -- the audit document is a historical record and does not need to
be updated to reflect completed work.

### Other plan or architecture documents

Search for any plan document that references `Element` as the root type:

```
grep -rn "implements Element\|elements\.api\.Element\|\bElement\b" docs/
```

Update references found in living documentation (READMEs, layer diagrams). Leave
historical audit documents (e.g., this plan) unchanged.

---

## Non-goals in this phase

- No production Java changes. If a missed `Element` reference is found in source code,
  fix it as a Phase 2 follow-up commit before closing Phase 3.
- No changes to `ElementRole`, `LocatorDescriptor`, `LocatorStrategy`.
- No changes to capability interfaces, page object enums, or test behavior.
- No changes to `UIEngine` logic or `SeleniumEngine` internals.

---

## Files changed

| File | Change |
|------|--------|
| `elements/api/UIElement.java` | Javadoc audit only -- no code changes |
| `core/target/Target.java` | Javadoc audit only -- no code changes |
| `core/engine/UIEngine.java` | Javadoc audit only if `Element` appears in comments |
| Any `README.md` referencing `Element` as the root type | Update to `UIElement` |

---

## Commit

```
docs(elements): update documentation to reflect UIElement and Target hierarchy
```

Only include this commit if documentation changes are actually made. If no docs required
updating, skip the commit and close the phase on the verification checklist alone.

---

## Phase complete when

- [ ] `mvn clean test -q` passes with zero failures.
- [ ] `grep -rn "implements Element[^R]" src/main/java/` returns zero results.
- [ ] `grep -rn "import elements\.api\.Element" src/` returns zero results.
- [ ] `Target.java` Javadoc has no locator or Selenium references.
- [ ] `UIElement.java` Javadoc states the enum-only constraint.
- [ ] Initiative is closed: branch merged, plan marked complete.
