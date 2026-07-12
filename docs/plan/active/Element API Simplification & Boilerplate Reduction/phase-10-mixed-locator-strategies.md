# Phase 10 — Mixed Locator Strategies

**Status:** Pending  
**Branch:** `feature/element-api-simplification`  
**Risk:** Medium — validation that the resolution order handles mixed pages correctly

---

## Objective

Confirm that a single page can freely mix conventional and hardcoded locators without interference between its enums.

---

## Dependencies

- Phase 9 (locator resolution order) must be implemented

---

## Context

After Phase 9, each element independently follows the three-step resolution order. A page's enums are not required to use the same strategy. Some may use the convention; others may use hardcoded locators.

This phase validates that the design holds for mixed pages and documents the supported patterns.

---

## Supported Mixed Pattern

```java
public interface UsersPage {

    // Resolved from convention: src/main/resources/.../UsersPage/locators.json
    enum Buttons implements Clickable {
        SAVE,
        CANCEL
    }

    // Hardcoded — returns null to bypass external lookup
    enum Dynamic implements Clickable {

        DELETE_ROW;

        @Override
        public String getExternalFileName() {
            return null;
        }

        @Override
        public String getTriggerLocator() {
            return "//tr[td='%s']//button";
        }
    }
}
```

Result:
- `SAVE` → resolved from `UsersPage/locators.json` via convention
- `DELETE_ROW` → hardcoded XPath, no repository lookup

---

## Affected Files

- No new implementation — this is a validation and documentation phase
- May require test-only page definitions to exercise the mixed pattern

---

## Checklist

### Validation
- [ ] Create a test page interface that mixes conventional and hardcoded enums
- [ ] Confirm conventional elements resolve from the repository
- [ ] Confirm hardcoded elements bypass the repository lookup entirely
- [ ] Confirm elements from one enum do not affect resolution of elements from another enum on the same page

### Documentation
- [ ] Document the mixed pattern in the developer guide or relevant README

### Tests
- [ ] Integration test: mixed page — conventional elements resolve correctly
- [ ] Integration test: mixed page — hardcoded elements bypass lookup correctly
- [ ] Regression: `mvn test` passes with no failures

---

## Exit Criteria

- Mixed pages work correctly with no cross-enum interference
- The supported mixed pattern is documented
- All tests pass

---

## What NOT to Do

- Do not require all enums on a page to use the same strategy — mixed strategies are an intentional feature
- Do not introduce a page-level annotation to switch strategies — the per-element override (Phase 8) already handles this

---

*MIT License Copyright (c) 2025-2026 VOID Project*
