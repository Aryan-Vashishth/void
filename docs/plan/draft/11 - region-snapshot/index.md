# Region Snapshot

Identified: 2026-10-29, while comparing VOID with the vartopia-e2e Playwright suite's
`saveAriaSnapshot` helper.
Branch: `initiative/region-snapshot`, cut from `main`.
Status: design only, not started.
Engines: Selenium and Playwright, with identical output, enforced by one contract test run
against each engine.

---

## Problem statement

Filling a page's `locators.json` after `/write` generates the template is the expensive step of
adding a page. Today the values come from DevTools by hand. An LLM doing the same work has no
compact view of the page: a screenshot has no attributes, and the full DOM of a real app page runs
to hundreds of KB.

vartopia-e2e solves this with `saveAriaSnapshot(region, name)`: it writes the accessibility tree of
ONE region (a form, a dialog, a grid header) to a file and logs only the path. The agent reads the
file and writes locators from it.

VOID needs the same, with one difference that drives the whole design:

**VOID locators are XPATH / CSS / ID / NAME** (`LocatorStrategy`). Playwright's snapshot gives role
and accessible name, which suit `getByRole` but cannot be written into `locators.json`. A VOID
snapshot must emit, per node, the **attribute anchors** a locator is built from (`data-test*`,
`id`, `name`) and a suggested XPath, alongside role and name.

---

## Goals

1. One call dumps one region's structure to a file and logs the path only.
2. Each actionable node carries what is needed to write its `locators.json` value.
3. **Selenium and Playwright both supported, with byte-identical output for the same DOM.** A
   snapshot taken under one engine produces locators that work under the other, and switching
   engines never changes what an agent sees.
4. Secrets never reach the file.
5. No new dependencies in this initiative (ADR-004). The Playwright Java dependency arrives with
   the `PlaywrightEngine` initiative, not with this one.

## Non-goals

- Generating `locators.json` values automatically. Possible follow-up, see "Later".
- A true accessibility-tree computation. Role and name are hints; anchors are the payload.
- Report attachment. Extent/Allure listeners live in `examples`; core does not depend on them.
- Building `PlaywrightEngine` itself. This initiative defines what that engine must do for
  snapshots and supplies the test it must pass.

---

## Design

### Layering (ADR-007, ADR-011, ADR-013)

| Layer | Responsibility | Engine-specific? |
|---|---|---|
| `region-snapshot.js` (resource) | The DOM walk and formatting. All snapshot logic lives here. | no |
| `RegionSnapshotScript` (`domain.automation.web.snapshot`) | Loads the JS once, holds the options object built from config. | no |
| `UIEngine.snapshot(LocatorDescriptor region)` | Resolve exactly one region element in the current frame, run the script on it, return the text. | yes, thin |
| `Debug.snapshot(...)` (facade) | Resolve the element's descriptor, mask secrets, write the file, log the path. | no |

Everything that decides what the output contains is in the shared JS. Each engine supplies only
"find one element, run this function on it", which is why the output can be identical.

`UIEngine` gains one method, following the ADR-020 precedent of extending the engine seam for a
missing capability. File IO and masking stay out of the engine: the engine owns browser interaction,
not artifacts.

The facade method goes on `Debug`, not `Browser` or `Elements`: `Debug` is documented as the home for
"diagnostic tooling", and a snapshot call is temporary by design (removed once the page's locators
exist), so it should not sit beside the test-facing services.

### API

```java
// Region already in a page contract
Path snapshot(UIElement region, String name);                    // element's single locator role
Path snapshot(UIElement region, ElementRole role, String name);  // element with several roles

// Region not in any contract yet (the common case: you snapshot BEFORE writing locators)
Path snapshot(String locator, String name);                      // LocatorDescriptor.of(value) infers strategy
```

```java
app.debug().snapshot("//div[@class='login_wrapper']", "login-form");
// INFO [snapshot] login-form -> logs/snapshots/<sessionId>/login-form.txt
```

Same call, same output, whichever engine the session was built with.

The single-role overload throws `IllegalArgumentException` naming the roles when the element has
more than one, rather than guessing.

Naming follows plan 07: if `UIElement` has become `Target` when this lands, the signatures use
`Target`.

### The shared script

`src/main/resources/void/snapshot/region-snapshot.js` is a single **function expression**:

```js
(root, opts) => { /* walk root, return the snapshot text */ }
```

That one shape runs unchanged on both engines:

| Engine | How it runs the script |
|---|---|
| Selenium | `executeScript("return (" + js + ")(arguments[0], arguments[1]);", element, opts)` |
| Playwright | `locator.evaluate(js, opts)`; Playwright passes the element as the first argument |

`opts` is a plain JSON-compatible map (anchor list, generated-id pattern as a string, node cap), so
it crosses both bridges the same way. The script returns a string, never DOM objects, so neither
engine has to serialise elements.

The script uses only standard DOM APIs available in Chromium, Firefox and WebKit, since Playwright
runs all three and Selenium runs Chrome, Firefox and Edge.

### Engine responsibilities

Both engines must behave identically on the points below. Each is a case in the contract test.

| Concern | Selenium | Playwright |
|---|---|---|
| Descriptor → element | `toBy(descriptor)` (existing) | `XPATH` → `xpath=…`; `CSS` → as is; `ID` → `[id="…"]`; `NAME` → `[name="…"]` |
| Waiting | wait for presence, `EngineConfig` timeout | `locator.waitFor(ATTACHED)`, same timeout |
| Zero matches | throw `RegionNotFoundException(descriptor)` | same exception, same message |
| More than one match | throw `AmbiguousRegionException(descriptor, count)` | same; count first via `locator.count()` rather than relying on Playwright's strict-mode error, whose message differs |
| Current frame | uses the frame selected by `switchToFrame` | uses the engine's current frame scope (however `PlaywrightEngine` implements `switchToFrame`) |
| Shadow DOM | not entered (the JS reports `- shadow-root`) | Playwright CSS pierces open shadow roots when *finding* the region, but the JS still reports, not enters, shadow roots inside it |

The shadow-DOM row is the one real behavioural gap between the engines: on Playwright, a CSS region
locator can match an element *inside* an open shadow root that Selenium cannot reach. The snapshot
output for that region is still identical; only whether the region is findable differs. Documented,
not papered over.

### Output format

Indented text, one node per line, Playwright-like so it reads familiarly. Produced by the JS, so it
is the same on both engines. Hand-written, no YAML library (ADR-004); extension `.txt` so it does
not claim to be YAML.

```text
- form:
  - textbox "Username" {data-test=username, id=user-name, name=user-name} xpath=//input[@data-test='username']
  - textbox "Password" [type=password] value=**** {data-test=password, id=password} xpath=//input[@data-test='password']
  - button "Login" {data-test=login-button, id=login-button} xpath=//input[@data-test='login-button']
- heading "Epic sadface: Username is required" [level=3] {data-test=error} xpath=//h3[@data-test='error']
```

Per node:

| Part | Content |
|---|---|
| role | explicit `role` attribute, else implicit role from a tag/input-type table (~25 entries) |
| `"name"` | `aria-labelledby` → `aria-label` → associated `<label>` → `alt` / `title` / `placeholder` → own text (buttons, links, headings, cells) |
| `[state]` | `disabled`, `checked`, `selected`, `expanded`, `required`, `level`, `type=password` |
| `value=` | current value of text inputs; always `****` for password inputs (fixed width, no length leak) |
| `{anchors}` | configured anchor attributes present on the node, in preference order |
| `xpath=` | suggested locator from the best anchor, only on actionable nodes |

Suggested locators are XPath because both engines accept XPath. A CSS suggestion would also work on
both, but one format keeps `locators.json` consistent.

Suggested-XPath preference: `data-test*` > `id` > `name` > `//tag[normalize-space()='text']`.
An `id` matching `void.snapshot.generatedIdPattern` (default: a run of 4+ digits, or a `mat-`/`cdk-`
/`ember`/`react-` prefix) is listed in anchors but never used for the suggestion.

### What the walk includes

- Kept: actionable elements (links, buttons, inputs, selects, textareas, `[role]`, `[onclick]`,
  `[tabindex>=0]`), headings, labels, table header cells, and leaf text up to 80 chars.
- Skipped: hidden nodes (`display:none`, `visibility:hidden`, zero size, `aria-hidden=true`),
  `script` / `style` / `svg` internals.
- Collapsed: wrapper elements with no role, no name and no anchors; their children move up a level.
- Capped at `void.snapshot.maxNodes` (default 2000), ending with `- … truncated at 2000 nodes`.
- Iframes and shadow roots: reported as a single `- iframe` / `- shadow-root` line, not entered.
  Snapshot inside a frame after `switchToFrame`.

### Why one shared script, not each engine's native tools

| Option | Selenium | Playwright | Identical output? |
|---|---|---|---|
| **Shared JS (chosen)** | `executeScript` | `evaluate` | yes, same code |
| Native per engine | CDP `Accessibility.getFullAXTree` (Chromium only) | `locator.ariaSnapshot()` | no: different name algorithms, different formats, and neither emits anchors |

Native tools give exact accessible names but would make the snapshot engine-dependent, which breaks
goal 3 and the point of VOID's engine seam. The anchors are what go into `locators.json`, and the
shared script produces them in one pass on any browser. Names only need to be good enough to
identify a node.

### Secret masking

Applied in `Debug.snapshot` before writing, so it is engine-independent:

1. Password input values never leave the browser (the JS emits `****`).
2. Values of environment variables and system properties whose names match
   `PASSWORD|SECRET|_KEY|TOKEN`, 4+ chars long, are replaced with `****`, both raw and in their
   escaped form (`\"`, `\\`), longest first so a short secret cannot leave part of a long one.

Related finding, out of scope: `LogActions.mask()` returns `"*".repeat(text.length())`, which leaks
password length into logs. Worth a fixed-width mask in a separate hotfix.

### Output location

`<void.snapshot.dir>/<sessionId>/<name>.txt`, default dir `logs/snapshots`. Per-session folders keep
parallel sessions from overwriting each other; the same name in one session overwrites (re-snapshot
after a fix). Log line: `CustomLogger.info` with name and path only, never content.

### Configuration

| Property | Default |
|---|---|
| `void.snapshot.dir` | `logs/snapshots` |
| `void.snapshot.anchors` | `data-test,data-testid,data-qa,data-cy,id,name` |
| `void.snapshot.generatedIdPattern` | `\d{4,}\|^(mat-\|cdk-\|ember\|react-)` |
| `void.snapshot.maxNodes` | `2000` |

Same properties for both engines. Documented in `docs/contributing/configuration-reference.md`.

---

## Enforcing "identical on both engines"

`RegionSnapshotContractTest` is an **abstract** test class in `src/test/java`. It holds every case
and the expected output; a subclass per engine supplies only how to start that engine.

```java
abstract class RegionSnapshotContractTest {
    protected abstract UIEngine startEngine();   // the only engine-specific line
    // cases: implicit roles, label association, hidden nodes, wrapper collapse,
    // generated-id rejection, node cap, password never emitted, zero/many matches,
    // frame scoping, shadow-root reporting
}
class SeleniumRegionSnapshotTest extends RegionSnapshotContractTest { … }
class PlaywrightRegionSnapshotTest extends RegionSnapshotContractTest { … }   // added in Phase 3
```

- Fixture pages: static HTML in `src/test/resources/snapshot/`, loaded via `file://`.
- Expected output: golden files in `src/test/resources/snapshot/expected/`. Both subclasses compare
  against the **same** golden files, so any divergence between engines fails the build.
- An ArchUnit rule: nothing outside `domain.automation.web.snapshot` and the engine
  implementations reads `region-snapshot.js`, so snapshot logic cannot fork per engine.

---

## Phases

### Phase 1: shared contract

- `UIEngine.snapshot(LocatorDescriptor)`; `RegionNotFoundException`, `AmbiguousRegionException`.
- `region-snapshot.js` and `RegionSnapshotScript`.
- `RegionSnapshotContractTest`, fixture pages, golden files.
- ArchUnit rule.

### Phase 2: Selenium engine

- `SeleniumEngine.snapshot`: resolve via `toBy`, wait, check the count, run the script.
- `SeleniumRegionSnapshotTest` passes against the golden files.

### Phase 3: Playwright engine

Depends on `PlaywrightEngine` existing.

- If `PlaywrightEngine` lands first: implement `PlaywrightEngine.snapshot` here, add
  `PlaywrightRegionSnapshotTest`, and it must pass against the same golden files unchanged.
- If this initiative lands first: `PlaywrightEngine` cannot satisfy `UIEngine` without
  `snapshot`, so its initiative must implement it. Add a line to that initiative's plan:
  "`PlaywrightRegionSnapshotTest` passes against the region-snapshot golden files with no golden
  file edited." Editing a golden file to make Playwright pass means the engines diverged, which
  is a bug in the engine, not in the file.

Implementation notes for whoever writes it:
- Descriptor mapping per the table under "Engine responsibilities".
- Count matches with `locator.count()` before `evaluate`, so zero and many raise VOID's exceptions
  rather than Playwright's.
- Run against Chromium, Firefox and WebKit in the contract test; Selenium covers Chrome and Firefox.

### Phase 4: facade, masking, file output

- `Debug.snapshot` overloads; masking; file write; log line.
- Unit tests without a browser: masking (raw, escaped, overlapping secrets) and path building.
- Docs: a "Region snapshots" section in `docs/architecture/locator-resolution.md` (the workflow:
  `/write` template → snapshot region → fill values → remove the call), configuration reference,
  `CHANGELOG.md`.

Phase 4 does not depend on Phase 3; it can ship on Selenium alone.

### Phase 5: ADR

`docs/decisions/pending-review/0NN-uiengine-region-snapshot.md`: records the new engine method, the
shared-script-over-native-tools choice, the snapshot format as an engine-neutral contract, and the
golden-file rule that keeps the engines identical.

Rough size, not measured: JS ~150 lines, Java ~200 (both engines), tests ~250.

---

## Later (not in this initiative)

- **Prefill `locators.json` from a snapshot.** `JsonMigratorCli --write <Class> --from <snapshot>`
  matches enum constants to nodes by name (`USERNAME_FIELD` ↔ textbox "Username") and fills the
  suggested XPath, leaving unmatched keys blank. Data generation, not code generation, so ADR-003 is
  not in play. This is the step that closes the gap with an agent finding locators unaided.
- A `.claude/commands/snapshot.md` command once there is a way to run a single flow to a page from
  the CLI.
- **A `ROLE` locator strategy** (`role=button[name="Login"]`). `LocatorStrategy` is open to it
  (ADR-023), and the snapshot already emits role and name, so it would only add a suggestion line.
  Its own initiative, ideally landing with `PlaywrightEngine`, where role lookup is native
  (`getByRole`). On `SeleniumEngine` there is no native role lookup: it would need the same
  approximate accessible-name logic as the snapshot JS, run at every click instead of once at
  discovery time, so an approximation error becomes a test failure rather than a wrong hint.

---

## Open questions

1. `Debug` vs `Browser` for the facade method. Recommended: `Debug` (reasoning above).
2. Emit non-password text input values? Recommended: yes, masked; they show state the agent needs
   (pre-filled forms, edit dialogs).
3. Can CI's `mvn -B test` job start headless browsers? Phase 2 needs Chrome; Phase 3 needs
   Playwright's browsers (`mvn exec:java -e -Dexec.mainClass=com.microsoft.playwright.CLI
   -Dexec.args="install --with-deps"` or the Playwright Docker image). Not verified for either.
4. How will `PlaywrightEngine` model the current frame (a stored `FrameLocator`, or a `Frame`)?
   Phase 3's frame-scoping case depends on it.
