package core.resolvers.locator.json;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

/**
 * Command-line entry point for {@link JsonLocatorMigrator}.
 *
 * <p>Extracted from the migrator class itself so library code carries no CLI baggage and
 * the migrator can be unit-tested without exercising {@link System#exit(int)}.</p>
 *
 * <pre>
 *   # Print JSON for a class to stdout:
 *   java JsonMigratorCli --print  com.example.MyPageElements
 *
 *   # Write JSON to the default output directory (src/main/resources/locators/json/):
 *   java JsonMigratorCli --write  com.example.MyPageElements
 *
 *   # Write to a specific file:
 *   java JsonMigratorCli --write  com.example.MyPageElements  path/to/output.json
 *
 *   # Write to the Phase 5 conventional path (src/main/resources/pkg/ClassName/locators.json):
 *   java JsonMigratorCli --write-conventional  com.example.MyPageElements
 * </pre>
 */
public final class JsonMigratorCli {

    private JsonMigratorCli() {}

    public static void main(String[] args) {
        if (args == null || args.length < 2) {
            printUsage();
            return;
        }

        String mode      = args[0].toLowerCase(Locale.ROOT);
        String className = args[1];

        Class<?> rootClass;
        try {
            rootClass = Class.forName(className);
        } catch (ClassNotFoundException e) {
            System.err.println("[error] Class not found: " + className);
            System.exit(1);
            return;
        }

        switch (mode) {
            case "--print" -> System.out.println(JsonLocatorMigrator.buildResolvedJson(rootClass));
            case "--write" -> {
                Path written = (args.length >= 3)
                        ? JsonLocatorMigrator.writeResolvedJsonTo(rootClass, Paths.get(args[2]))
                        : JsonLocatorMigrator.writeResolvedJson(rootClass);
                System.out.println("[done] Written to: " + written.toAbsolutePath());
            }
            case "--write-conventional" -> {
                Path written = JsonLocatorMigrator.writeResolvedJsonConventional(rootClass);
                System.out.println("[done] Written to: " + written.toAbsolutePath());
            }
            default -> {
                System.err.println("[error] Unknown mode: " + mode
                        + ". Use --print, --write, or --write-conventional.");
                System.exit(1);
            }
        }
    }

    private static void printUsage() {
        System.out.println("Usage:");
        System.out.println("  JsonMigratorCli --print               <fully.qualified.ClassName>");
        System.out.println("  JsonMigratorCli --write               <fully.qualified.ClassName>");
        System.out.println("  JsonMigratorCli --write               <fully.qualified.ClassName>  <outputFile>");
        System.out.println("  JsonMigratorCli --write-conventional  <fully.qualified.ClassName>");
    }
}

