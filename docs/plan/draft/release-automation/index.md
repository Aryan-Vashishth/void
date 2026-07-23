# Release Automation

Objective: eliminate the two-source-of-truth problem between `pom.xml` and
`version.json` by making the release operation a single explicit script invocation
that treats `pom.xml` as the authoritative version source and synchronizes every
downstream artifact from it.

## Program context

**Why this initiative exists.** The 0.4.1 hotfix introduced `version.json` so the
README badge could be dynamic, but left `pom.xml` and `version.json` as independently
maintained files. The release checklist in `CLAUDE.md` documents the required steps,
but documentation of a manual multi-file update is not synchronization -- it is
deferred drift. Every future release is a bet that both files get updated correctly
and in the same commit.

**Why it is sequenced here.** It is a standalone infrastructure concern with no
dependency on any runtime-redesign initiative. It can be implemented on its own
branch at any point without touching production code or architectural invariants.
No initiative blocks it; it blocks nothing.

**What boundary it owns.** The release operation: from "version decided" to "working
tree reflects that version consistently." That boundary is currently split across
developer memory and a checklist.

**What it deliberately does not own.** CI pipeline changes beyond wiring the script
into the existing `demo.yml` / `ci.yml` release trigger. Package publishing, GitHub
Releases API, or changelog generation are out of scope unless a later phase explicitly
extends this plan.

---

## Phase 1 -- `scripts/set-version`: atomic version propagation

- **Objective**: a single cross-platform script that accepts a version string, writes
  it into `pom.xml` and `version.json` atomically, and rejects malformed input. After
  this phase, no human ever touches `version.json` directly; it is a derived file
  maintained exclusively by the script.
- **Motivation**: eliminates the two-source-of-truth condition. The `CLAUDE.md`
  release checklist shrinks to two steps: run `set-version`, add the CHANGELOG entry.
- **Scope / files**:
  - `scripts/set-version` (new, executable) -- reads the version argument, validates
    semver format (`\d+\.\d+\.\d+`), uses `mvn versions:set` (or direct XML edit) to
    update `pom.xml`, writes `{ "version": "x.y.z" }` to `version.json`
  - `CLAUDE.md` release checklist -- updated to reference the script
  - `version.json` -- demoted from manually maintained file to generated artifact;
    add to `.gitattributes` as `generated` if tooling supports it
- **Dependencies**: none.
- **Risks**: (arch) script language choice must work in both Windows dev and Linux CI
  environments -- Python 3 is the lowest-friction cross-platform option; shell scripts
  require WSL or Git Bash on Windows. (compat) `mvn versions:set` requires the
  Versions Maven Plugin on the classpath; if not already present, add it to `pom.xml`
  as a plugin management entry only (no lifecycle binding).
- **Rollback**: delete the script; restore manual checklist wording in `CLAUDE.md`.
- **Validation / tests**: run `python scripts/set-version 9.9.9`; confirm `pom.xml`
  reads `<version>9.9.9</version>` and `version.json` reads `{ "version": "9.9.9" }`;
  run `mvn compile` to verify POM is well-formed; revert with `git checkout`.
- **Exit criteria**: `python scripts/set-version <version>` updates both files
  correctly; invalid inputs (missing arg, non-semver string) exit non-zero with a
  clear error; `mvn compile` succeeds after the script runs.
- **ADR / docs**: no ADR required (no architectural decision). `CLAUDE.md` release
  checklist updated in-phase.
- **Migration notes**: `version.json` already exists at the repo root with the correct
  content for 0.4.1; no data migration needed.

---

## Phase 2 -- `scripts/release`: release invariant enforcement

- **Objective**: a higher-level script that enforces release preconditions before
  committing: working tree is clean, CHANGELOG contains an entry for the target
  version, the git tag does not already exist. On success it calls `set-version`,
  stages both modified files, and commits with the conventional `chore(release):
  bump version to x.y.z` message.
- **Motivation**: the release checklist currently relies on the developer to check
  each invariant manually. Encoding them in a script makes violations a hard failure
  rather than a missed step. This also gives a stable place to add future release
  gates (e.g. suite must be green, no open audit findings).
- **Scope / files**:
  - `scripts/release` (new, executable) -- wraps `set-version`; adds precondition
    checks and the git commit; does NOT push or tag automatically (those remain
    explicit developer actions)
  - `CLAUDE.md` release checklist -- updated to reference `release` as the primary
    command, with `set-version` documented as the low-level primitive
- **Dependencies**: Phase 1 (`set-version` must exist before `release` can call it).
- **Risks**: (arch) auto-commit in a script surprises developers who expect to review
  the staged diff first -- mitigate by defaulting to `--dry-run` mode that prints
  what would happen without executing; the commit is opt-in via `--commit` flag.
  (compat) CHANGELOG format must be stable enough to grep reliably; if the format
  changes, the check must be updated in the same commit.
- **Rollback**: delete the script; `set-version` from Phase 1 remains.
- **Validation / tests**: run `python scripts/release 0.4.1 --dry-run` on the current
  tree; confirm it exits 0 and prints the planned actions. Run with a version that
  has no CHANGELOG entry; confirm it exits non-zero with a clear message.
- **Exit criteria**: `release --dry-run` passes on a clean tree with a valid
  CHANGELOG entry; `release --dry-run` fails with a descriptive error when the tree
  is dirty, the CHANGELOG entry is missing, or the tag already exists.
- **ADR / docs**: no ADR required. `CLAUDE.md` updated in-phase.
- **Migration notes**: none.

---

## Candidate future extensions (not in scope)

The following are recorded here to prevent scope creep into Phases 1-2, not to
schedule them:

- Automatic git tagging after `--commit` (requires explicit opt-in flag).
- GitHub Release creation via `gh release create`.
- Changelog linting (entry format validation beyond presence check).
- Suite-green gate before `release` proceeds (call `mvn test` as a precondition).
