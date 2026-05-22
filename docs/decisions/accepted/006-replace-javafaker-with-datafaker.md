# 006 — Replace JavaFaker with Datafaker

**Date:** 2026-05-01  
**Status:** Accepted

---

## Context

VOID used `com.github.javafaker:javafaker:1.0.2` for test data generation (random names, emails, dates, company names, etc.) via the internal `DataGenerator` utility.

JavaFaker 1.0.2:
- Is **unmaintained** (last release: 2019)
- Pulls in `snakeyaml:1.23` which has **8 known CVEs** (including CVE-2022-1471, severity 8.3)
- Has no active development or security patches

[Datafaker](https://github.com/datafaker-net/datafaker) (`net.datafaker:datafaker`) is the actively maintained fork with:
- Same API surface
- No vulnerable transitive dependencies
- Active development and releases

---

## Decision

Replace `javafaker` with `datafaker` 2.4.2.

---

## Reasoning

1. **CVE elimination** — removes 8 transitive vulnerabilities from snakeyaml
2. **API compatibility** — near drop-in replacement (import change: `com.github.javafaker.Faker` → `net.datafaker.Faker`)
3. **Active maintenance** — regular releases, security patches, new providers
4. **Single usage point** — only `DataGenerator.java` imports Faker, so migration scope is minimal

---

## Consequences

- Zero CVEs from test data generation dependencies
- Import changed in 1 file (`DataGenerator.java`)
- POM updated: `javafaker` → `datafaker`
- API behavior unchanged — all `FieldType` generators continue working identically

