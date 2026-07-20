# Plans -- Draft

Active and upcoming implementation plans. Phases here are planned or in progress; none are complete yet.

## Initiatives

- `oop-violations-remediation/` -- 4-phase plan to eliminate `instanceof` dispatch chains and `(Enum<?>) this` casts; includes post-plan audit; ADRs 016-017 accepted
- `engine-decoupling/` -- plan to decouple VOID startup from the Selenium bootstrap path to support hot-swap engine loading
- `void-cli-simplification.md` -- standalone draft for CLI simplification (single-phase, not yet planned as phases)
- `generalize-element-into-target/` -- 3-phase plan to introduce Target as a domain-neutral root and rename Element to UIElement; audit in `audit/`
