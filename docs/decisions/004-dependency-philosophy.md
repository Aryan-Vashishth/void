# 004 — Dependency Philosophy

**Date:** 2026-05-01  
**Status:** Accepted

---

## Context

VOID ships as a Maven dependency consumed by other projects. Every dependency VOID declares becomes a transitive dependency for all consumers. Dependency choices have compounding effects on:

- classpath size and conflict risk
- CVE exposure surface
- hidden behavior potential
- debuggability of the overall system

---

## Decision

VOID follows a minimal-dependency philosophy:

1. **Every dependency must be actively used** — no "just in case" declarations
2. **Every dependency must be explicit in behavior** — no annotation magic, no classpath scanning, no bytecode manipulation
3. **Prefer standard/stable libraries** — Jackson, Log4j 2, TestNG are industry-standard with well-documented behavior
4. **One library per concern** — no duplicate annotation libraries, no competing logging facades
5. **CVE-free direct dependencies** — actively maintained versions only

---

## Reasoning

The dependency set is the **trust boundary** of the system. Each library added:

- Increases the surface area for hidden behavior
- Adds transitive dependencies outside our control
- Creates potential version conflicts for consumers
- Requires ongoing maintenance and vulnerability monitoring

VOID's core value proposition is **structured, observable automation**. Dependencies that introduce opacity (reflection, classpath scanning, bytecode generation) directly undermine that value.

---

## Consequences

- Final dependency count: 7 (down from 11 after audit)
- Zero unused dependencies in the tree
- All behavior is traceable through explicit API calls
- Consumers inherit a clean, minimal transitive dependency set
- Periodic audits (see `docs/audits/`) verify continued alignment

---

## Current Dependency Set

| Dependency | Purpose | Why It's Aligned |
|---|---|---|
| `selenium-java` | Browser automation | Explicit API — locators, waits, actions all visible |
| `testng` | Test runner | Declarative annotations, predictable lifecycle |
| `jackson-databind` | JSON parsing | Tree-model usage, no annotation-driven binding |
| `log4j-api` + `log4j-core` | Logging | Configured via `log4j2.xml`, no magic |
| `jsr305` | Nullability annotations | Pure compile-time markers, zero runtime behavior |
| `datafaker` | Test data generation | Explicit calls, no reflection |
| `cucumber-java/testng` | BDD adapter (optional) | Declared optional — consumer's choice |

