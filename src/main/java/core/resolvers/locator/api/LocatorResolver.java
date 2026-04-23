package core.resolvers.locator.api;

import core.resolvers.locator.parser.ByParser;
import core.resolvers.locator.source.LocatorSource;
import core.resolvers.locator.source.LocatorSourceRegistry;
import core.resolvers.locator.template.LocatorTemplate;
import elements.api.Element;
import elements.meta.ElementRole;
import org.openqa.selenium.By;

import java.util.Arrays;
import java.util.LinkedHashMap;
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

    private LocatorResolver(Builder b) {
        this.registry       = b.registry;
        this.templatePolicy = b.policy;
        this.byParser       = b.byParser;
    }

    public static Builder builder() { return new Builder(); }

    public LocatorSourceRegistry registry()       { return registry; }
    public LocatorTemplate.Policy templatePolicy() { return templatePolicy; }
    public ByParser byParser()                    { return byParser; }

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
        return resolve(e.getExternalFileName(), key, e.effectiveArgs(overrideArgs));
    }

    /**
     * Resolve the best available locator: PRIMARY → SECONDARY → first non-blank role value.
     */
    public By resolveBest(Element e, Object... overrideArgs) {
        String file = e.getExternalFileName();
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

        public Builder registry(LocatorSourceRegistry r) { this.registry = r; return this; }
        public Builder policy(LocatorTemplate.Policy p)  { this.policy = p; return this; }
        public Builder byParser(ByParser p)              { this.byParser = p; return this; }
        public LocatorResolver build()                   { return new LocatorResolver(this); }
    }
}

