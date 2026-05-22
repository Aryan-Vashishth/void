# Main Branch Migration Archive (2026-05-22)

## Purpose
Archive record for promoting `feature/engine-abstraction` to `main` while preserving the previous `main` as legacy.

## References
- Old `main` commit: `8eb93790bb98fb69883d35dfee98157a38e5edca`
- New `main` source branch: `feature/engine-abstraction`
- New `main` source commit at migration start: `183504f9919edc011c282737c073bf6ac62da949`
- Legacy backup branch: `legacy/main-deprecated`
- Backup tag: `archive/main-before-engine-abstraction-2026-05-22`

## Rollback
If needed, restore `main` to the archived commit:

```bash
git fetch origin
git checkout main
git reset --hard 8eb93790bb98fb69883d35dfee98157a38e5edca
git push --force-with-lease origin main
```

