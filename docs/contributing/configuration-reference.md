# Configuration Reference

Framework behavior controlled by runtime configuration keys (read via `ConfigLoader`).

---

## `void.profile.default`

**Type:** String  
**Default:** `RAW`  
**Valid values:** `RAW`, `DEBUG`, `VISUAL`, `FAST`

Sets the global default execution profile applied to every action created via
`ElementActions.of()`. When set to anything other than `RAW`, that profile's hooks
run around every matching action automatically.

### Behavior since runtime-redesign I3.2

Actions with `ActionCapability.UNKNOWN` are **excluded** from configured-default
profile application. The runtime cannot select browser-wait hooks for a capability
it does not recognise. A `WARN`-level log line is emitted instead:

```
applyConfiguredDefault: action with UNKNOWN capability skipped -- no profile applied.
Declare a specific ActionCapability, or set void.profile.default=RAW to suppress this warning.
```

### Remedies

| Situation | Remedy |
|---|---|
| Custom action with unknown capability getting the warning | Declare `ActionCapability.of("MY_CAP")` and pass it to the action constructor |
| Warning is noise in your environment | Set `void.profile.default=RAW` (default behavior, no profiles applied) |
| Lambda action needs a specific profile | Call `.using(yourProfile)` explicitly instead of relying on the global default |

### `safely()` on UNKNOWN capability

Calling `.safely()` on an action whose `capability()` returns `ActionCapability.UNKNOWN`
throws `IllegalStateException`. The method cannot determine which browser-wait contract
applies to an unrecognised capability. Use `.raw()` for no-hooks execution, or declare
a specific capability.

---

## Adding new configuration keys

1. Define a `static final String MY_KEY = "void.my.key"` constant in the owning class.
2. Read it via `ConfigLoader.get(MY_KEY, defaultValue)`.
3. Document it in this file with type, default, valid values, and any capability-guard
   behavior introduced after I3.2.
