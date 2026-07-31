package core.resolvers.locator.sync;

import domain.automation.web.resolve.api.ConventionalLocatorPath;
import core.resolvers.locator.json.JsonLocatorMigrator;
import core.resolvers.locator.sync.EmptyKeyValidator.EmptyKeyError;
import core.resolvers.locator.sync.LocatorTemplateGenerator.LocatorKey;
import core.resolvers.locator.sync.OrphanKeyDetector.OrphanWarning;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

/**
 * Orchestrates the four sync steps for a single page class:
 * <ol>
 *   <li>Template generation — create or merge-with-preserve</li>
 *   <li>Orphan detection — warn for keys with no matching enum constant</li>
 *   <li>Empty key validation — fail with line numbers for any unfilled key</li>
 *   <li>JSON generation — write the conventional {@code locators.json}</li>
 * </ol>
 *
 * <p>All steps operate under {@code resourcesBase}. The production entry point uses
 * {@code src/main/resources}; tests pass a temp directory.</p>
 */
public final class LocatorSyncOrchestrator {

    public static final int EXIT_OK         = 0;
    public static final int EXIT_EMPTY_KEYS = 1;
    public static final int EXIT_IO_ERROR   = 3;

    private static final Path DEFAULT_RESOURCES_BASE = Paths.get("src/main/resources");

    private final LocatorTemplateGenerator generator     = new LocatorTemplateGenerator();
    private final LocatorTemplateWriter    writer        = new LocatorTemplateWriter();
    private final OrphanKeyDetector        orphanChecker = new OrphanKeyDetector();
    private final EmptyKeyValidator        emptyChecker  = new EmptyKeyValidator();

    /** Production entry point — writes to {@code src/main/resources}. */
    public int sync(Class<?> pageClass, boolean prune) {
        return sync(pageClass, prune, DEFAULT_RESOURCES_BASE);
    }

    /** Test-friendly overload — writes to {@code resourcesBase}. */
    int sync(Class<?> pageClass, boolean prune, Path resourcesBase) {
        String conventionalPath = ConventionalLocatorPath.forClassProperties(pageClass);
        Path propsFile = resourcesBase.resolve(conventionalPath);

        // ── Step 1: Template generation ──────────────────────────────────────────
        List<LocatorKey> expectedKeys = generator.generateKeys(pageClass);
        try {
            if (!Files.exists(propsFile)) {
                System.out.println("[sync] Creating: " + propsFile);
                writer.writeNew(propsFile, pageClass.getSimpleName(), expectedKeys);
                System.out.println("[sync] Created " + expectedKeys.size() + " key(s).");
            } else {
                LineTrackingPropertiesReader existing = new LineTrackingPropertiesReader();
                existing.load(propsFile);
                Set<String> existingKeySet = existing.getProperties().stringPropertyNames();
                boolean changed = writer.mergeInto(propsFile, expectedKeys, existingKeySet);
                long added = expectedKeys.stream()
                    .filter(k -> !existingKeySet.contains(k.key()))
                    .count();
                if (changed) {
                    System.out.println("[sync] Merged " + added + " new key(s) into: " + propsFile);
                } else {
                    System.out.println("[sync] No new keys — file is up to date.");
                }
            }
        } catch (IOException e) {
            System.err.println("[error] I/O error during template generation: " + e.getMessage());
            return EXIT_IO_ERROR;
        }

        // ── Step 2: Orphan detection ──────────────────────────────────────────────
        LineTrackingPropertiesReader reader = new LineTrackingPropertiesReader();
        try {
            reader.load(propsFile);
        } catch (IOException e) {
            System.err.println("[error] Cannot read " + propsFile + ": " + e.getMessage());
            return EXIT_IO_ERROR;
        }

        List<OrphanWarning> orphans = orphanChecker.detect(pageClass, reader);
        if (!orphans.isEmpty()) {
            System.err.println("[warn] Orphan keys in " + propsFile.getFileName() + ":");
            for (OrphanWarning w : orphans) {
                System.err.printf("       Line %3d: %s — %s%n", w.lineNumber(), w.key(), w.reason());
            }
            if (prune) {
                System.err.println("[warn] --prune not yet implemented; orphan keys retained.");
            } else {
                System.err.println("[warn] Re-run with --prune to remove orphan keys.");
            }
        }

        // ── Step 3: Empty key validation ─────────────────────────────────────────
        List<EmptyKeyError> emptyErrors = emptyChecker.validate(expectedKeys, reader);
        if (!emptyErrors.isEmpty()) {
            System.err.println("[error] Empty locator value(s) in " + propsFile.getFileName() + ":");
            for (EmptyKeyError err : emptyErrors) {
                String lineRef = err.lineNumber() > 0
                    ? String.format("Line %3d", err.lineNumber())
                    : "  (new) ";
                System.err.printf("        %s: %s=%n", lineRef, err.key());
            }
            System.err.println();
            System.err.println("Fill these values and re-run --sync. JSON generation skipped.");
            return EXIT_EMPTY_KEYS;
        }

        // ── Step 4: JSON generation ───────────────────────────────────────────────
        try {
            Path jsonOut = resourcesBase.resolve(ConventionalLocatorPath.forClass(pageClass));
            Path written = JsonLocatorMigrator.writeJsonString(
                JsonLocatorMigrator.buildResolvedJson(pageClass), jsonOut);
            System.out.println("[sync] JSON written to: " + written.toAbsolutePath());
        } catch (Exception e) {
            System.err.println("[error] JSON generation failed: " + e.getMessage());
            return EXIT_IO_ERROR;
        }

        System.out.println("[sync] Done — " + pageClass.getSimpleName() + " is in sync.");
        return EXIT_OK;
    }
}
