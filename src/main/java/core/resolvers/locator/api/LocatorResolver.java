package core.resolvers.locator.api;

import core.engine.LocatorDescriptor;
import core.engine.LocatorStrategy;
import core.resolvers.locator.parser.ByParser;
import core.resolvers.locator.source.LocatorSource;
import core.resolvers.locator.source.LocatorSourceRegistry;
import core.resolvers.locator.template.LocatorTemplate;
import elements.api.Element;
import elements.meta.ElementRole;
import org.openqa.selenium.By;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import static core.logging.CustomLogger.debug;
import static core.logging.CustomLogger.error;

/**
 * Instance-based, configurable orchestrator for locator resolution.
 *
 * <p>Composes {@link LocatorSourceRegistry}, {@link LocatorTemplate.Policy} and
 * {@link ByParser} into one cohesive object. Replaces the procedural static façades
 * ({@code ElementLocatorResolverV1}, {@code LocatorResolverV1}) that historically
 * duplicated this orchestration logic.</p>
 *
 * <p>Use {@link Builder} for explicit configuration, or one of the preconfigured
 * instances on {@link LocatorResolvers}.</p>
 *
 * <p>This class is thread-safe and immutable; backing sources may hold their own caches.</p>
 */
public final class LocatorResolver {

    private final LocatorSourceRegistry registry;
    private final LocatorTemplate.Policy templatePolicy;
    private final ByParser byParser;
    private final LocatorContext locatorContext;

    private LocatorResolver(Builder b) {
        this.registry       = b.registry;
        this.templatePolicy = b.policy;
        this.byParser       = b.byParser;
        this.locatorContext = b.locatorContext;
    }

    public static Builder builder() { return new Builder(); }

    public LocatorSourceRegistry registry()       { return registry; }
    public LocatorTemplate.Policy templatePolicy() { return templatePolicy; }
    public ByParser byParser()                    { return byParser; }
    public LocatorContext locatorContext()        { return locatorContext; }

    // ---------------------------------------------------------------------
    // Raw lookup
    // ---------------------------------------------------------------------

    /**
     * Fetch the raw, un-formatted template for {@code request}.
     *
     * @throws IllegalStateException if no source could find a value for the request
     */
    public String rawTemplate(LocatorRequest request) {
        LocatorSource source = registry.select(request.fileName());
        String raw = source.readRaw(request);
        if (raw == null) {
            throw new IllegalStateException(
                    "Locator not found (no raw template): file=" + request.fileName() +
                    " key=" + request.key() + " source=" + source.name());
        }
        return raw;
    }

    // ---------------------------------------------------------------------
    // Full pipeline
    // ---------------------------------------------------------------------

    /** Full pipeline: raw lookup → template formatting → {@link By} parsing. */
    public By resolve(LocatorRequest request) {
        String template = rawTemplate(request);

        debug.log("[LOCATOR] Resolving:",
                "File", String.valueOf(request.fileName()),
                "Key",  request.key(),
                "Args", request.args().length > 0 ? Arrays.toString(request.args()) : "[]",
                "Hardcoded", String.valueOf(request.isHardcoded()));

        String resolved = new LocatorTemplate(template, templatePolicy).format(request.args());
        if (resolved == null) {
            error.log("[LOCATOR] Could not resolve template",
                    "Template", template, "Args", Arrays.toString(request.args()));
            throw new IllegalStateException(
                    "Could not resolve locator template [file=" + request.fileName() +
                    ", key=" + request.key() + ", template=" + template +
                    ", args=" + Arrays.toString(request.args()) + "]");
        }

        By by = byParser.parse(resolved);
        debug.log(request.isHardcoded()
                        ? "[LOCATOR] Final (HARDCODED):"
                        : "[LOCATOR] Final:",
                "Key", request.key(), "Resolved", resolved, "By", by.toString());
        return by;
    }

    /** Convenience: build a request and resolve. */
    public By resolve(String fileName, String key, Object... args) {
        return resolve(LocatorRequest.of(fileName, key, args));
    }

    // ---------------------------------------------------------------------
    // Engine-agnostic descriptor resolution
    // ---------------------------------------------------------------------

    /**
     * Resolves a {@link LocatorRequest} into a {@link LocatorDescriptor} (engine-agnostic).
     * Performs the same pipeline as {@link #resolve(LocatorRequest)} but returns the
     * resolved string + inferred strategy rather than a Selenium {@link By}.
     */
    public LocatorDescriptor resolveDescriptor(LocatorRequest request) {
        String template = rawTemplate(request);
        String resolved = new LocatorTemplate(template, templatePolicy).format(request.args());
        if (resolved == null) {
            throw new IllegalStateException(
                    "Could not resolve locator template [file=" + request.fileName() +
                    ", key=" + request.key() + ", template=" + template +
                    ", args=" + Arrays.toString(request.args()) + "]");
        }
        LocatorStrategy strategy = inferStrategy(resolved);
        String value = stripPrefix(resolved);
        debug.log("[LOCATOR] Descriptor:",
                "Key", request.key(), "Strategy", strategy.name(), "Value", value);
        return LocatorDescriptor.of(value, strategy, request.args());
    }

    /** Convenience: resolve a descriptor from file/key/args. */
    public LocatorDescriptor resolveDescriptor(String fileName, String key, Object... args) {
        return resolveDescriptor(LocatorRequest.of(fileName, key, args));
    }

    /** Resolve descriptor for the primary locator of an {@link Element}. */
    public LocatorDescriptor resolveDescriptor(Element e) {
        return resolveDescriptorBest(e);
    }

    /** Resolve descriptor for a specific role on an {@link Element}. */
    public LocatorDescriptor resolveDescriptor(Element e, ElementRole role, Object... overrideArgs) {
        Map<ElementRole, String> roles = safeRoles(e.getAllLocatorRoles());
        String key = roles.get(role);
        if (isBlank(key)) {
            throw new IllegalStateException("Missing locator for role: " + role +
                    (e.getDisplayText() == null ? "" : (" (element=\"" + e.getDisplayText() + "\")")));
        }
        return resolveDescriptor(locatorContext.resolveFileName(e), key, e.effectiveArgs(overrideArgs))
                .withLabel(labelOf(e));
    }

    /** Resolve the best-available descriptor: PRIMARY → SECONDARY → first non-blank role. */
    public LocatorDescriptor resolveDescriptorBest(Element e, Object... overrideArgs) {
        String file  = locatorContext.resolveFileName(e);
        Object[] args = e.effectiveArgs(overrideArgs);
        String label = labelOf(e);

        String key = e.getPrimaryLocator();
        if (!isBlank(key)) return resolveDescriptor(file, key, args).withLabel(label);

        key = e.getSecondaryLocator();
        if (!isBlank(key)) return resolveDescriptor(file, key, args).withLabel(label);

        Map<ElementRole, String> roles = safeRoles(e.getAllLocatorRoles());
        key = roles.values().stream()
                .filter(v -> v != null && !v.isBlank())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No locators defined for element: " + e.getDisplayText()));
        return resolveDescriptor(file, key, args).withLabel(label);
    }

    private static String labelOf(Element element) {
        if (!(element instanceof Enum<?> en)) return null;
        Class<?> page = en.getClass().getDeclaringClass();
        String prefix = page != null ? page.getSimpleName() + " > " : "";
        return prefix + en.getClass().getSimpleName() + " > " + en.name();
    }

    // ─── Strategy inference helpers ─────────────────────────────────────────

    private static LocatorStrategy inferStrategy(String resolved) {
        String lower = resolved.trim().toLowerCase(Locale.ROOT);
        if (lower.startsWith("xpath=")) return LocatorStrategy.XPATH;
        if (lower.startsWith("css="))   return LocatorStrategy.CSS;
        if (lower.startsWith("id="))    return LocatorStrategy.ID;
        if (lower.startsWith("name="))  return LocatorStrategy.NAME;
        // Heuristic fallback
        return LocatorStrategy.infer(resolved);
    }

    private static String stripPrefix(String resolved) {
        String lower = resolved.trim().toLowerCase(Locale.ROOT);
        for (String prefix : new String[]{"xpath=", "css=", "id=", "name=", "class=", "tag=", "linktext=", "partiallinktext="}) {
            if (lower.startsWith(prefix)) {
                return resolved.trim().substring(prefix.length());
            }
        }
        return resolved.trim();
    }

    // ---------------------------------------------------------------------
    // Element-based resolution
    // ---------------------------------------------------------------------

    /** Resolve the primary locator for an {@link Element}. */
    public By resolve(Element e) {
        return resolveBest(e);
    }

    /** Resolve a specific role for an {@link Element}; throws if the role is not declared. */
    public By resolve(Element e, ElementRole role, Object... overrideArgs) {
        Map<ElementRole, String> roles = safeRoles(e.getAllLocatorRoles());
        String key = roles.get(role);
        if (isBlank(key)) {
            throw new IllegalStateException("Missing locator for role: " + role +
                    (e.getDisplayText() == null ? "" : (" (element=\"" + e.getDisplayText() + "\")")));
        }
        return resolve(locatorContext.resolveFileName(e), key, e.effectiveArgs(overrideArgs));
    }

    /**
     * Resolve the best available locator: PRIMARY → SECONDARY → first non-blank role value.
     */
    public By resolveBest(Element e, Object... overrideArgs) {
        String file = locatorContext.resolveFileName(e);
        Object[] args = e.effectiveArgs(overrideArgs);

        String key = e.getPrimaryLocator();
        if (!isBlank(key)) return resolve(file, key, args);

        key = e.getSecondaryLocator();
        if (!isBlank(key)) return resolve(file, key, args);

        Map<ElementRole, String> roles = safeRoles(e.getAllLocatorRoles());
        key = roles.values().stream()
                .filter(v -> v != null && !v.isBlank())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No locators defined for element: " + e.getDisplayText()));
        return resolve(file, key, args);
    }

    // ---- helpers -----------------------------------------------------------

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }

    private static Map<ElementRole, String> safeRoles(Map<ElementRole, String> in) {
        if (in == null || in.isEmpty()) return new LinkedHashMap<>();
        return (in instanceof LinkedHashMap) ? in : new LinkedHashMap<>(in);
    }

    // ---------------------------------------------------------------------
    // Builder
    // ---------------------------------------------------------------------

    public static final class Builder {
        private LocatorSourceRegistry registry  = LocatorSourceRegistry.DEFAULT;
        private LocatorTemplate.Policy policy   = LocatorTemplate.Policy.STRICT;
        private ByParser byParser               = ByParser.DEFAULT;
        private LocatorContext locatorContext   = DefaultLocatorContext.INSTANCE;

        public Builder registry(LocatorSourceRegistry r)      { this.registry = r; return this; }
        public Builder policy(LocatorTemplate.Policy p)        { this.policy = p; return this; }
        public Builder byParser(ByParser p)                    { this.byParser = p; return this; }
        public Builder locatorContext(LocatorContext c)        { this.locatorContext = c; return this; }
        public LocatorResolver build()                         { return new LocatorResolver(this); }
    }
}

