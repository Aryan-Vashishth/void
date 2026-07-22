# Coding Standards

## Java

- Java 17. Use records, sealed classes, pattern matching, and text blocks where appropriate.
- No Lombok, no compile-time code generation. All constructors, getters, and utility methods
  must be explicit in source.
- No wildcard imports (`import foo.*`) except for `static` imports from `CustomLogger` and
  `AnsiColors`.
- Prefer `final` on fields, parameters, and local variables when the value is not reassigned.
- Static utility classes must have a `private` constructor.
- Max line length: 120 characters (soft limit).

## Logging

Use `CustomLogger` exclusively. Never use `System.out.println`.

```java
// Correct
CustomLogger.info.log("[SeleniumEngine] Driver initialized.");
CustomLogger.debug.log("[UIEngineFactory] Creating engine: " + engineName);

// Wrong
System.out.println("Engine started");
```

Initialize the logger in test classes in `@BeforeClass`:

```java
@BeforeClass
public void setUp() {
    CustomLogger.initialize(MyTest.class);
}
```

## Testing

- Framework: TestNG (`@Test`, `@BeforeClass`, `@DataProvider`).
- Test method naming: `methodUnderTest_scenario_expectedOutcome`.
- Unit tests must not open a browser. Use reflection for private fields and methods.
- No Mockito. Use proxy-based fakes or reflection-based setup.
- Tests must be idempotent and independent -- no ordering dependencies.
- Use `@DataProvider` for parameterized cases.

## Naming Conventions

| Entity | Convention | Example |
|---|---|---|
| Classes | `PascalCase` | `LocatorResolver` |
| Methods | `camelCase` | `resolveLocator()` |
| Constants | `UPPER_SNAKE_CASE` | `WAIT_FOR_ELEMENT_VISIBLE` |
| Packages | `lowercase`, dot-separated | `core.resolvers.locator.api` |
| Test methods | `method_scenario_outcome` | `clickOn_nullElement_throwsNPE` |

## Comments

Default to no comments. Write one only when the WHY is non-obvious: a hidden constraint,
a subtle invariant, a workaround for a specific bug. Do not describe what the code does --
well-named identifiers already do that.
