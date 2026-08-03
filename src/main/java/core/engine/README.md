# `core.engine` -- Kernel-Neutral Execution Contract

Kernel-owned execution abstractions. Web-domain types (`UIEngine`, `UIEngineFactory`,
`EngineRegistrar`, `LocatorDescriptor`, `LocatorStrategy`) relocated to
`domain.automation.web.*` in runtime-redesign I6.4 / I7.2 (ADR-021).

---

## Class Inventory

| Class | Type | Responsibility |
|-------|------|----------------|
| `Executor` | Interface | Neutral execution-owner contract (ADR-021 AD2) |
| `EngineBootstrap` | Value | Opaque session initialisation token |
| `EngineConfig` | Config | Neutral config carrier (timeout, pollingMs, baseUrl) |
| `DomainRegistrar` | SPI | Domain-registration contract (I6.1) |
| `DomainRegistry` | Factory | Domain-registration factory (I6.1) |

---

## Relocated types

| Former FQN | New FQN |
|------------|---------|
| `core.engine.UIEngine` | `domain.automation.web.engine.UIEngine` |
| `core.engine.UIEngineFactory` | `domain.automation.web.engine.UIEngineFactory` |
| `core.engine.EngineRegistrar` | `domain.automation.web.engine.EngineRegistrar` |
| `core.engine.LocatorDescriptor` | `domain.automation.web.locator.LocatorDescriptor` |
| `core.engine.LocatorStrategy` | `domain.automation.web.locator.LocatorStrategy` |
| `core.engine.selenium.*` | `domain.automation.web.selenium.*` |

---

## See Also

- `domain.automation.web.engine` -- web execution contract (UIEngine, factory, SPI)
- `domain.automation.web.selenium` -- Selenium WebDriver implementation
- ADR-007: UIEngine as Single Execution Authority
- ADR-021: kernel boundary, ontology, physical package topology
