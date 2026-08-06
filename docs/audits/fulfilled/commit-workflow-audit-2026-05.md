# VOID Framework — Commit Workflow Audit

**Date:** 2026-05-13  
**Scope:** Git commit rules and process for both LLM agents and human contributors  
**Goal:** Establish a single, enforceable commit workflow that keeps history clean, reviewable, and bi-directionally safe (human ↔ LLM)

---

## 1. Why This Audit Exists

LLM agents (Copilot, Claude, etc.) now commit code alongside human developers.  
Without shared rules, the result is:

- bloated commits mixing code, docs, and config
- vague or hallucinated commit messages
- lost work from improper stash/reset usage
- merge conflicts from uncommitted local state
- no traceability on who (or what) authored a change

This document defines the **exact process** both humans and LLMs must follow.

---

## 2. Pre-Commit Discipline

### 2.1 Working Directory Hygiene

| Rule | Human | LLM | Why |
|------|-------|-----|-----|
| Check `git status` before starting any task | ✅ Required | ✅ Required | Prevents committing stale or unrelated changes |
| Check `git diff --stat` before staging | ✅ Required | ✅ Required | Confirms only intended files are modified |
| Never commit from a dirty working tree with unrelated changes | ✅ Required | ✅ Required | Keeps commits atomic |

### 2.2 Stashing (`git stash`)

| Rule | Human | LLM |
|------|-------|-----|
| Stash unrelated work before switching context: `git stash push -m "description"` | ✅ | ✅ |
| Always use a descriptive stash message (`-m`) | ✅ | ✅ |
| Verify stash content before popping: `git stash show -p stash@{0}` | ✅ | ✅ |
| Pop stash immediately after switching back — don't leave stashes to rot | ✅ | ✅ |
| LLMs must never `git stash pop` without first confirming the stash belongs to the current task | — | ✅ |
| Never use `git stash drop` without explicit user confirmation | — | ✅ |

**Anti-pattern:** Using `git stash` as a temporary save-point instead of a proper commit.  
If the work is meaningful, commit it on a branch instead.

---

## 3. Staging (`git add`)

### 3.1 Rules

| Rule | Human | LLM | Why |
|------|-------|-----|-----|
| Stage files explicitly — never use `git add .` or `git add -A` in production branches | ✅ | ✅ | Prevents accidental inclusion of build artifacts, logs, or IDE files |
| Use `git add -p` (patch mode) when a file contains both relevant and unrelated changes | ✅ | Optional | Keeps commits focused |
| LLMs must list every file being staged and the reason before executing `git add` | — | ✅ | Traceability |
| Verify staging with `git diff --cached --stat` before committing | ✅ | ✅ | Final sanity check |

### 3.2 What Must Never Be Staged

| Path / Pattern | Reason |
|----------------|--------|
| `target/` | Build output |
| `*.class` | Compiled bytecode |
| `logs/` | Runtime logs |
| `.idea/`, `*.iml` | IDE-specific config |
| `*.dumpstream` | Surefire crash dumps |
| `driver.properties` with real credentials | Security |
| `cp.txt` | Local classpath dump |

> The `.gitignore` must cover all of the above. If it doesn't, fix `.gitignore` first — don't rely on discipline alone.

---

## 4. Commit Messages

VOID uses [Conventional Commits](https://www.conventionalcommits.org/) as defined in `CONTRIBUTING.md`.

### 4.1 Format

```
<type>(<scope>): <imperative summary, ≤72 chars>

<optional body — wrap at 80 chars>

<optional footer>
```

### 4.2 Types (reference)

| Type | Use |
|------|-----|
| `feat` | New feature or capability |
| `fix` | Bug fix |
| `docs` | Documentation only |
| `refactor` | Behavior-preserving code change |
| `test` | Adding or updating examples |
| `chore` | Build, CI, dependency, tooling |
| `perf` | Performance improvement |
| `audit` | Audit documents and architecture reviews |

### 4.3 Mandatory Rules

| Rule | Human | LLM |
|------|-------|-----|
| Subject line must be imperative mood ("add", not "added" or "adds") | ✅ | ✅ |
| Subject line ≤ 72 characters | ✅ | ✅ |
| Scope must be a real package or module (`engine`, `flow`, `locator`, `hooks`, `docs`, `examples`) | ✅ | ✅ |
| Body must explain **why**, not just **what** | ✅ | ✅ |
| No AI-generated filler ("This commit improves the codebase…") | — | ✅ |
| LLM commits must include `Generated-by: <agent>` in the footer | — | ✅ |
| Multi-file changes must itemize affected areas in the body | ✅ | ✅ |

### 4.4 LLM Footer Convention

```
feat(engine): add retry-on-stale to click execution

Add StaleElementReferenceException retry loop in SeleniumEngine.click()
with configurable max-attempts (default: 3). Prevents flaky failures on
dynamic DOM re-renders.

Affects: SeleniumEngine.java, engine.properties
Generated-by: GitHub Copilot
```

### 4.5 Bad Commit Messages (Examples to Avoid)

| ❌ Bad | Why |
|--------|-----|
| `update files` | No type, no scope, no meaning |
| `fix stuff` | Describes nothing |
| `feat: changes` | No scope, no summary |
| `refactor(core): refactor code for better structure` | Tautological — says nothing about what changed |
| `WIP` | Never commit WIP to shared branches; use a local branch or stash |
| `feat(engine): This commit adds a new feature to improve the engine` | AI filler; be specific |

---

## 5. Commit Granularity

### 5.1 One Commit = One Logical Change

| ✅ Good | ❌ Bad |
|---------|--------|
| `feat(hooks): add Before.SCROLL_INTO_VIEW hook` | One commit with a new hook + unrelated refactor + doc update |
| `docs(readme): update project structure section` | Docs mixed into a feature commit |
| `fix(locator): handle null fileName in LocatorRequest` | Fix bundled with a feature |

### 5.2 Splitting Rules

| Scenario | Action |
|----------|--------|
| New feature + examples | Can be **one commit** if examples are for that feature only |
| New feature + doc update | **Two commits** — `feat(...)` then `docs(...)` |
| Refactor + behavior change | **Two commits** — `refactor(...)` then `feat(...)` or `fix(...)` |
| Multiple unrelated fixes | **One commit per fix** |

### 5.3 LLM-Specific Granularity Rules

| Rule | Reason |
|------|--------|
| LLMs must not batch unrelated changes into a single commit | LLMs tend to over-bundle |
| LLMs must confirm the diff scope with the user before committing if more than 5 files are touched | Safety check |
| LLMs should prefer smaller, reviewable commits over large "done" commits | Easier to revert and review |

---

## 6. Branching Discipline at Commit Time

| Rule | Human | LLM |
|------|-------|-----|
| Never commit directly to `main` | ✅ | ✅ |
| Always verify current branch before committing: `git branch --show-current` | ✅ | ✅ |
| LLMs must refuse to commit if on `main` and ask user to create a branch | — | ✅ |
| Feature work goes on `feature/<name>`, fixes on `bugfix/<name>`, docs on `docs/<name>` | ✅ | ✅ |
| Rebase on `develop` before pushing: `git pull --rebase origin develop` | ✅ | ✅ |

---

## 7. Pre-Commit Checks

Before any commit is finalized, the following must pass:

| Check | Command | Human | LLM |
|-------|---------|-------|-----|
| Compilation | `mvn compile -q` | ✅ | ✅ |
| No new compiler warnings | `mvn compile 2>&1 \| grep -i warn` | ✅ | ✅ |
| Tests pass (if code changed) | `mvn test -q` | ✅ | ✅ |
| Diff review | `git diff --cached` | ✅ | ✅ |
| No secrets/credentials in diff | Manual / grep for passwords, tokens | ✅ | ✅ |
| `.gitignore` coverage | Verify no build artifacts staged | ✅ | ✅ |

### LLM-Specific Pre-Commit Checklist

Before executing `git commit`, an LLM agent must:

1. Run `git status` — confirm only expected files are modified
2. Run `git diff --stat` — confirm scope
3. Stage files explicitly with `git add <file>...`
4. Run `git diff --cached --stat` — confirm staged content
5. Run `mvn compile -q` — confirm compilation
6. Compose commit message following §4 rules
7. Include `Generated-by:` footer
8. Execute `git commit -m "..."` only after all above pass

---

## 8. Post-Commit Discipline

| Rule | Human | LLM |
|------|-------|-----|
| Verify commit: `git log --oneline -1` | ✅ | ✅ |
| Verify no leftover changes: `git status` | ✅ | ✅ |
| Push promptly — don't accumulate local-only commits | ✅ | ✅ |
| LLMs must report the final commit hash and summary to the user | — | ✅ |

---

## 9. Revert and Amend Rules

| Action | Rule |
|--------|------|
| `git commit --amend` | Only on **unpushed** commits. Never amend pushed history. |
| `git reset --soft HEAD~1` | Acceptable to re-stage and re-commit locally. Never on pushed commits. |
| `git reset --hard` | **Dangerous.** LLMs must never use `git reset --hard` without explicit user confirmation. |
| `git revert <hash>` | Preferred method for undoing pushed commits. Creates a new commit. |
| `git rebase -i` | Humans only, for local cleanup before push. LLMs must not interactive-rebase. |

---

## 10. Summary: LLM vs Human Differences

| Concern | Human | LLM |
|---------|-------|-----|
| `git add .` | Discouraged | **Forbidden** |
| `git stash drop` | Use carefully | **Requires user confirmation** |
| `git reset --hard` | Use carefully | **Requires user confirmation** |
| `git rebase -i` | Allowed locally | **Forbidden** |
| Commit to `main` | Forbidden | **Forbidden + must refuse** |
| `Generated-by:` footer | Not required | **Required** |
| Pre-commit diff review | Recommended | **Mandatory** |
| Multi-file commit (>5 files) | Review before push | **Must confirm with user** |
| WIP commits | Avoid on shared branches | **Forbidden** |

---

## 11. Enforcement Recommendations

| Mechanism | Status | Priority |
|-----------|--------|----------|
| `.gitignore` covers `target/`, `logs/`, `.idea/`, `cp.txt` | ⚠️ Verify | **Critical** |
| Pre-commit hook: reject commits without conventional format | 🔲 Not implemented | High |
| Pre-commit hook: reject commits on `main` | 🔲 Not implemented | High |
| Pre-commit hook: reject staged `*.class`, `*.dumpstream` | 🔲 Not implemented | Medium |
| CI check: validate commit message format | 🔲 Not implemented | High |
| CI check: `Generated-by:` footer present on LLM commits | 🔲 Not implemented | Medium |
| LLM agent prompt includes commit rules reference | 🔲 Not implemented | High |

---

## 12. Quick Reference Card

```text
┌─────────────────────────────────────────────────────────┐
│                  VOID Commit Workflow                    │
├─────────────────────────────────────────────────────────┤
│  1. git status                    — check state         │
│  2. git branch --show-current     — verify branch       │
│  3. git stash push -m "..."       — stash if needed     │
│  4. (make changes)                                      │
│  5. git diff --stat               — review changes      │
│  6. git add <file> <file>         — stage explicitly    │
│  7. git diff --cached --stat      — verify staging      │
│  8. mvn compile -q                — verify build        │
│  9. git commit -m "type(scope): summary"                │
│ 10. git log --oneline -1          — verify commit       │
│ 11. git status                    — confirm clean       │
│ 12. git push                      — push promptly       │
└─────────────────────────────────────────────────────────┘
```

---

*This audit is a living document. Update it as tooling (pre-commit hooks, CI checks) is implemented.*

