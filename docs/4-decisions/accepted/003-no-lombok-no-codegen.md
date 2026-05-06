# 003 — No Compile-Time Code Generation (No Lombok)

**Date:** 2026-05-01  
**Status:** Accepted

---

## Context

Lombok and similar compile-time code generation tools (AutoValue, Immutables) are widely used in Java to reduce boilerplate — generating getters, setters, builders, `equals`/`hashCode`, `toString`, and constructors via annotations.

VOID is a system built around **traceability and debuggability**. Every failure should be explainable by reading the source code.

---

## Decision

VOID does not use Lombok or any compile-time code generation tool. All constructors, getters, builders, and utility methods are written explicitly.

---

## Reasoning

1. **What you read is what runs** — no invisible transformations between source and bytecode
2. **Debuggability** — step-through debugging shows actual code, not generated proxies
3. **IDE independence** — no annotation processor plugins required for the code to compile
4. **Traceability** — call-site logging (`CustomLogger`) traces real methods, not generated ones
5. **Philosophical alignment** — VOID's core principle is "no hidden behavior." Code generation is by definition hidden behavior.

---

## Consequences

- Slightly more lines of code for data classes and value objects
- Every behavior is visible in source — no Lombok plugin required
- Debugger, grep, and IDE navigation work without surprises
- New contributors can read and understand the code without knowing Lombok's annotation semantics

