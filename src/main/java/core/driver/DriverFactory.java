package core.driver;

import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import core.utils.ConfigLoader;
import core.utils.ConfigPaths;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;

import static core.logging.CustomLogger.*; // debug.log(...), info.log(...), warn.log(...)

/**
 * DriverFactory
 * -----------------------------------------------------------------------------
 * Fluent, null-safe builder for {@link WebDriver} with support for:
 * <ul>
 *   <li>Local or Remote (Selenium Grid / Selenoid)</li>
 *   <li>Chrome / Firefox / Edge</li>
 *   <li>Headless, maximize, explicit window size</li>
 *   <li>Implicit / script / page-load timeouts + {@link PageLoadStrategy}</li>
 *   <li>Proxy configuration and binary overrides</li>
 *   <li>Downloads directory and Chrome mobile emulation</li>
 *   <li>CLI args, browser prefs, and extra capabilities</li>
 *   <li>Centralized config layering via {@link ConfigLoader}:
 *       <strong>classpath only by default</strong> (plus System & ENV); filesystem is opt-in</li>
 * </ul>
 *
 * <p>Lifecycle is not managed here. Pair with your thread-safe context (e.g., {@code DriverContext}).</p>
 * <p>Selenium Manager (built into Selenium 4.6+) handles driver binaries automatically.</p>
 */
public final class DriverFactory {

    // ---------------------------------------------------------------------
    // Classpath config locations (internal)
    // ---------------------------------------------------------------------
    public static final String DEFAULT_PROPERTIES_CLASSPATH      = ConfigPaths.DRIVER_DEFAULT;
    public static final String DEFAULT_LOCAL_PROPERTIES_CLASSPATH = ConfigPaths.DRIVER_LOCAL;
    public static final String DEFAULT_CI_PROPERTIES_CLASSPATH    = ConfigPaths.DRIVER_CI;
    public static final String DEFAULT_GRID_PROPERTIES_CLASSPATH  = ConfigPaths.DRIVER_GRID;

    // Where we WRITE templates so they end up on the classpath at runtime.
    // Adjust if you prefer src/test/resources.
    public static final Path RESOURCES_BASE = Paths.get("src", "main", "resources");

    // ---------------------------------------------------------------------
    // Profiles for layered loading
    // ---------------------------------------------------------------------
    public enum Profile { DEFAULT, LOCAL, CI, GRID }

    // ---------------------------------------------------------------------
    // Defaults (behavior)
    // ---------------------------------------------------------------------
    public static final Browser DEFAULT_BROWSER = Browser.CHROME;
    public static final boolean DEFAULT_REMOTE = false;
    public static final boolean DEFAULT_HEADLESS = false;
    public static final boolean DEFAULT_MAXIMIZE = true;
    public static final Integer DEFAULT_WINDOW_WIDTH = null;
    public static final Integer DEFAULT_WINDOW_HEIGHT = null;

    public static final PageLoadStrategy DEFAULT_PAGE_LOAD_STRATEGY = PageLoadStrategy.NORMAL;
    public static final Duration DEFAULT_IMPLICIT_WAIT    = Duration.ofSeconds(5);
    public static final Duration DEFAULT_PAGELOAD_TIMEOUT = Duration.ofSeconds(60);
    public static final Duration DEFAULT_SCRIPT_TIMEOUT   = Duration.ofSeconds(30);

    public static final boolean DEFAULT_ACCEPT_INSECURE_CERTS = true;

    /** Headless viewport fallback when no explicit size provided. */
    public static final int DEFAULT_HEADLESS_WIDTH  = 1920;
    public static final int DEFAULT_HEADLESS_HEIGHT = 1080;

    // ---------------------------------------------------------------------
    // Property keys (single source of truth)
    // ---------------------------------------------------------------------
    public static final String PROP_BROWSER             = "browser";
    public static final String PROP_HEADLESS            = "headless";
    public static final String PROP_REMOTE              = "remote";
    public static final String PROP_GRID_URL            = "gridUrl";
    public static final String PROP_MAXIMIZE            = "maximize";
    public static final String PROP_WIDTH               = "width";
    public static final String PROP_HEIGHT              = "height";
    public static final String PROP_IMPLICIT_WAIT       = "implicitWait";
    public static final String PROP_PAGELOAD_TIMEOUT    = "pageLoadTimeout";
    public static final String PROP_SCRIPT_TIMEOUT      = "scriptTimeout";
    public static final String PROP_PAGELOAD_STRATEGY   = "pageLoadStrategy";
    public static final String PROP_ACCEPT_INSECURE_CERTS = "acceptInsecureCerts";
    public static final String PROP_DOWNLOADS_DIR       = "downloadsDir";
    public static final String PROP_MOBILE_DEVICE       = "mobileEmulationDevice";
    public static final String PROP_CHROME_BINARY       = "chromeBinary";
    public static final String PROP_FIREFOX_BINARY      = "firefoxBinary";
    public static final String PROP_EDGE_BINARY         = "edgeBinary";

    private DriverFactory() {}

    // ---------------------------------------------------------------------
    // Entry points (ALL defaults internal via ConfigLoader)
    // ---------------------------------------------------------------------

    /** Start a new fluent builder (no config pre-populated). */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Build a builder using centralized layered config from {@link ConfigLoader}.
     * <br>Order (first → last, last wins):
     * <ol>
     *   <li>Classpath defaults &amp; profile overlays</li>
     *   <li>System properties</li>
     *   <li>ENV mapped to properties</li>
     * </ol>
     * <p><i>No filesystem or external-override file by default.</i></p>
     */
    public static Builder fromProfile(Profile profile) {
        debug.log("DriverFactory.fromProfile(): " + profile);

        ConfigLoader.Layered.Builder lb = ConfigLoader.Layered.builder()
                // classpath defaults & profile overlays (INTERNAL)
                .addClasspath(DEFAULT_PROPERTIES_CLASSPATH)
                .addClasspath(profile == Profile.LOCAL ? DEFAULT_LOCAL_PROPERTIES_CLASSPATH : null)
                .addClasspath(profile == Profile.CI    ? DEFAULT_CI_PROPERTIES_CLASSPATH    : null)
                .addClasspath(profile == Profile.GRID  ? DEFAULT_GRID_PROPERTIES_CLASSPATH  : null)
                // NO .addFile(...) and NO externalOverrideKeys(...): keep defaults internal
                .includeSystemProperties(true)
                .includeEnvironment(true)
                .mapEnv(buildEnvMap());

        Properties p = lb.build();
        return fromProperties(p);
    }

    /** Build a builder from exactly one classpath resource (still via ConfigLoader). */
    public static Builder fromConfigClasspath(String resourcePath) {
        debug.log("DriverFactory.fromConfigClasspath(): " + resourcePath);
        Properties p = ConfigLoader.loadFromClasspath(resourcePath);
        return fromProperties(p);
    }

    /**
     * Opt-in: Build a builder from a specific external file (explicit use only).
     * This is NOT part of the default path strategy.
     */
    public static Builder fromConfigFile(Path path) {
        debug.log("DriverFactory.fromConfigFile(): " + path);
        Properties p = ConfigLoader.loadFromFile(path);
        return fromProperties(p);
    }

    /** Build a builder pre-populated from System properties (all fields optional). */
    public static Builder fromSystemProperties() {
        return fromProperties(System.getProperties());
    }

    /** Build a builder from a {@link Properties} map (missing values fall back to defaults). */
    public static Builder fromProperties(Properties p) {
        Builder b = builder();

        // Core flags
        b.browser(Browser.safeParse(p.getProperty(PROP_BROWSER)));
        b.remote(parseBool(p.getProperty(PROP_REMOTE), DEFAULT_REMOTE));
        b.gridUrl(p.getProperty(PROP_GRID_URL));
        b.headless(parseBool(p.getProperty(PROP_HEADLESS), DEFAULT_HEADLESS));
        b.maximize(parseBool(p.getProperty(PROP_MAXIMIZE), DEFAULT_MAXIMIZE));
        b.acceptInsecureCerts(parseBool(p.getProperty(PROP_ACCEPT_INSECURE_CERTS), DEFAULT_ACCEPT_INSECURE_CERTS));

        // Window size
        Integer w = parseInt(p.getProperty(PROP_WIDTH), DEFAULT_WINDOW_WIDTH);
        Integer h = parseInt(p.getProperty(PROP_HEIGHT), DEFAULT_WINDOW_HEIGHT);
        if (w != null && h != null) b.windowSize(w, h);

        // Timeouts & strategy
        b.implicitWait(parseSeconds(p.getProperty(PROP_IMPLICIT_WAIT), DEFAULT_IMPLICIT_WAIT));
        b.pageLoadTimeout(parseSeconds(p.getProperty(PROP_PAGELOAD_TIMEOUT), DEFAULT_PAGELOAD_TIMEOUT));
        b.scriptTimeout(parseSeconds(p.getProperty(PROP_SCRIPT_TIMEOUT), DEFAULT_SCRIPT_TIMEOUT));

        String pls = p.getProperty(PROP_PAGELOAD_STRATEGY);
        if (pls != null && !pls.isBlank()) {
            try {
                b.pageLoadStrategy(PageLoadStrategy.valueOf(pls.trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ignore) {
                warn.log("Ignoring invalid pageLoadStrategy value: " + pls);
            }
        }

        // Extras
        String dd = p.getProperty(PROP_DOWNLOADS_DIR);
        if (dd != null && !dd.isBlank()) b.downloadsDir(dd);

        String dev = p.getProperty(PROP_MOBILE_DEVICE);
        if (dev != null && !dev.isBlank()) b.mobileEmulationDevice(dev);

        String cb = p.getProperty(PROP_CHROME_BINARY);
        if (cb != null && !cb.isBlank()) b.chromeBinary(cb.trim());
        String fb = p.getProperty(PROP_FIREFOX_BINARY);
        if (fb != null && !fb.isBlank()) b.firefoxBinary(fb.trim());
        String eb = p.getProperty(PROP_EDGE_BINARY);
        if (eb != null && !eb.isBlank()) b.edgeBinary(eb.trim());

        // Proxy
        Proxy proxy = buildProxyFromProps(p);
        if (proxy != null) b.proxy(proxy);

        // Args / Prefs / Caps
        extractArgs(p).forEach(b::addArg);
        extractPrefs(p).forEach(b::addPref);
        extractCapabilities(p).forEach(b::addCapability);

        return b;
    }

    // ---------------------------------------------------------------------
    // Templates (write into resources so they are internal at runtime)
    // ---------------------------------------------------------------------

    /** Create a driver .properties template under {@code src/test/resources/config/} for DEFAULT. */
    public static Path createPropertiesTemplate() {
        return createPropertiesTemplate(Profile.DEFAULT, true, true, false, false);
    }

    /** Create a driver .properties template under {@code src/test/resources/config/} for a profile. */
    public static Path createPropertiesTemplate(Profile profile) {
        return createPropertiesTemplate(profile, true, true, false, false);
    }

    /**
     * Create a driver .properties template under {@code src/test/resources/config/} for a profile.
     * (Adjust {@link #RESOURCES_BASE} to {@code src/main/resources} if you prefer.)
     *
     * @param includeComments include header and section comments
     * @param includeExamples include commented examples section
     * @param overwrite       if true, overwrite an existing file
     * @param fillDefaults    if true, populate keys with factory default values
     */
    public static Path createPropertiesTemplate(Profile profile,
                                                boolean includeComments,
                                                boolean includeExamples,
                                                boolean overwrite,
                                                boolean fillDefaults) {
        Path path = resolveResourceTemplatePath(profile);
        return createPropertiesTemplate(path, includeComments, includeExamples, overwrite, fillDefaults);
    }

    /** Backwards-compatible overload (defaults fillDefaults=false). */
    public static Path createPropertiesTemplate(Profile profile,
                                                boolean includeComments,
                                                boolean includeExamples,
                                                boolean overwrite) {
        return createPropertiesTemplate(profile, includeComments, includeExamples, overwrite, false);
    }

    /** Create a driver .properties template at an explicit path (directories auto-created). */
    public static Path createPropertiesTemplate(Path path,
                                                boolean includeComments,
                                                boolean includeExamples,
                                                boolean overwrite,
                                                boolean fillDefaults) {
        Objects.requireNonNull(path, "path");
        try {
            if (path.getParent() != null) Files.createDirectories(path.getParent());
            if (Files.exists(path) && !overwrite) {
                warn.log("Template not written; file already exists: " + path.toAbsolutePath());
                return path; // do not throw; honor user's preference
            }
            String content = getTemplateString(includeComments, includeExamples, fillDefaults);
            Files.writeString(path, content, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            info.log("DriverFactory properties template written to: " + path.toAbsolutePath());
            return path;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to write template: " + path, e);
        }
    }

    /** Backwards-compatible overload (defaults fillDefaults=false). */
    public static Path createPropertiesTemplate(Path path,
                                                boolean includeComments,
                                                boolean includeExamples,
                                                boolean overwrite) {
        return createPropertiesTemplate(path, includeComments, includeExamples, overwrite, false);
    }


    public static Path createDefaultTemplate(Boolean overwriteProperties) {
        return createPropertiesTemplate(Profile.DEFAULT, true, true, overwriteProperties, true);
    }



    /** Build the template text without writing to disk (useful for preview/tests). */
    public static String getTemplateString(boolean includeComments, boolean includeExamples) {
        return getTemplateString(includeComments, includeExamples, false);
    }

    /**
     * Build the template text, optionally pre-populating with factory defaults.
     *
     * @param includeComments include header/section comments
     * @param includeExamples include examples section
     * @param fillDefaults    populate keys with default values when available
     */
    public static String getTemplateString(boolean includeComments, boolean includeExamples, boolean fillDefaults) {
        String nl = System.lineSeparator();
        StringBuilder sb = new StringBuilder(2048);

        // Helper to write "key=value" honoring fillDefaults
        class Line {
            void kv(String key, String valueOrNull) {
                sb.append(key).append("=").append(valueOrNull == null ? "" : valueOrNull).append(nl);
            }
        }
        Line line = new Line();

        if (includeComments) {
            sb.append("# DriverFactory Properties Template").append(nl)
                    .append("# Generated: ").append(new Date()).append(nl)
                    .append("# Fill or override any keys as needed. Empty values fall back to defaults.")
                    .append(nl).append(nl);
        }

        if (includeComments) sb.append("# Core").append(nl);
        line.kv(PROP_BROWSER,           fillDefaults ? DEFAULT_BROWSER.name().toLowerCase(Locale.ROOT) : null);
        line.kv(PROP_HEADLESS,          fillDefaults ? String.valueOf(DEFAULT_HEADLESS) : null);
        line.kv(PROP_REMOTE,            fillDefaults ? String.valueOf(DEFAULT_REMOTE) : null);
        line.kv(PROP_GRID_URL,          null); // no default
        line.kv(PROP_MAXIMIZE,          fillDefaults ? String.valueOf(DEFAULT_MAXIMIZE) : null);
        line.kv(PROP_WIDTH,             null); // null default
        line.kv(PROP_HEIGHT,            null);
        sb.append(nl);

        if (includeComments) sb.append("# Timeouts & Strategy (seconds)").append(nl);
        line.kv(PROP_IMPLICIT_WAIT,     fillDefaults ? String.valueOf(DEFAULT_IMPLICIT_WAIT.toSeconds()) : null);
        line.kv(PROP_PAGELOAD_TIMEOUT,  fillDefaults ? String.valueOf(DEFAULT_PAGELOAD_TIMEOUT.toSeconds()) : null);
        line.kv(PROP_SCRIPT_TIMEOUT,    fillDefaults ? String.valueOf(DEFAULT_SCRIPT_TIMEOUT.toSeconds()) : null);
        line.kv(PROP_PAGELOAD_STRATEGY, fillDefaults ? DEFAULT_PAGE_LOAD_STRATEGY.name() : null);
        sb.append(nl);

        if (includeComments) sb.append("# Certificates / Security").append(nl);
        line.kv(PROP_ACCEPT_INSECURE_CERTS, fillDefaults ? String.valueOf(DEFAULT_ACCEPT_INSECURE_CERTS) : null);
        sb.append(nl);

        if (includeComments) sb.append("# Downloads").append(nl);
        line.kv(PROP_DOWNLOADS_DIR, null); // no default path
        sb.append(nl);

        if (includeComments) sb.append("# Mobile emulation (Chrome)").append(nl);
        line.kv(PROP_MOBILE_DEVICE, null);
        sb.append(nl);

        if (includeComments) sb.append("# Binary overrides (optional)").append(nl);
        line.kv(PROP_CHROME_BINARY,  null);
        line.kv(PROP_FIREFOX_BINARY, null);
        line.kv(PROP_EDGE_BINARY,    null);
        sb.append(nl);

        if (includeComments) sb.append("# Proxy (optional)").append(nl);
        line.kv("proxy.http",        null);
        line.kv("proxy.ssl",         null);
        line.kv("proxy.socks",       null);
        line.kv("proxy.socksVersion",null);
        sb.append(nl);

        if (includeComments) sb.append("# Browser arguments (CSV 'args' or numbered 'arg.N')").append(nl);
        line.kv("args",  null);
        line.kv("arg.1", null);
        line.kv("arg.2", null);
        sb.append(nl);

        if (includeComments) sb.append("# Browser preferences (pref.<key>=<value>)").append(nl);
        line.kv("pref.download.prompt_for_download", null);
        sb.append(nl);

        if (includeComments) sb.append("# Capabilities (cap.<key>=<value>)").append(nl);
        line.kv("cap.someCapability", null);
        sb.append(nl);

        if (includeExamples) {
            sb.append("# ------------------ Examples ------------------").append(nl)
                    .append("# ").append(PROP_BROWSER).append("=chrome | firefox | edge").append(nl)
                    .append("# ").append(PROP_HEADLESS).append("=true").append(nl)
                    .append("# ").append(PROP_REMOTE).append("=true").append(nl)
                    .append("# ").append(PROP_GRID_URL).append("=http://localhost:4444/wd/hub").append(nl)
                    .append("# ").append(PROP_MAXIMIZE).append("=true").append(nl)
                    .append("# ").append(PROP_WIDTH).append("=1920").append(nl)
                    .append("# ").append(PROP_HEIGHT).append("=1080").append(nl)
                    .append("# ").append(PROP_IMPLICIT_WAIT).append("=0").append(nl)
                    .append("# ").append(PROP_PAGELOAD_TIMEOUT).append("=45").append(nl)
                    .append("# ").append(PROP_SCRIPT_TIMEOUT).append("=20").append(nl)
                    .append("# ").append(PROP_PAGELOAD_STRATEGY).append("=EAGER").append(nl)
                    .append("# ").append(PROP_ACCEPT_INSECURE_CERTS).append("=true").append(nl)
                    .append("# ").append(PROP_DOWNLOADS_DIR).append("=/tmp/downloads").append(nl)
                    .append("# ").append(PROP_MOBILE_DEVICE).append("=Pixel 7").append(nl)
                    .append("# ").append(PROP_CHROME_BINARY).append("=/usr/bin/google-chrome").append(nl)
                    .append("# ").append(PROP_FIREFOX_BINARY).append("=/usr/bin/firefox").append(nl)
                    .append("# ").append(PROP_EDGE_BINARY).append("=/usr/bin/microsoft-edge").append(nl)
                    .append("# proxy.http=proxy.acme.local:8080").append(nl)
                    .append("# proxy.ssl=proxy.acme.local:8443").append(nl)
                    .append("# proxy.socks=proxy.acme.local:1080").append(nl)
                    .append("# proxy.socksVersion=5").append(nl)
                    .append("# args=--no-sandbox,--disable-dev-shm-usage").append(nl)
                    .append("# arg.1=--start-maximized").append(nl)
                    .append("# pref.download.prompt_for_download=false").append(nl)
                    .append("# cap.acceptInsecureCerts=true").append(nl);
        }

        return sb.toString();
    }

    // ---------------------------------------------------------------------
    // Browser enum
    // ---------------------------------------------------------------------

    /** Supported browsers. */
    public enum Browser {
        CHROME, FIREFOX, EDGE;

        /** Parse a string to a Browser. Returns {@link #DEFAULT_BROWSER} for null/blank/unknown values. */
        public static Browser safeParse(String s) {
            if (s == null || s.isBlank()) return DEFAULT_BROWSER;
            return switch (s.trim().toLowerCase(Locale.ROOT)) {
                case "chrome", "gc", "googlechrome" -> CHROME;
                case "ff", "firefox"                -> FIREFOX;
                case "edge", "msedge"               -> EDGE;
                default -> {
                    warn.log("Unknown browser '" + s + "', falling back to " + DEFAULT_BROWSER);
                    yield DEFAULT_BROWSER;
                }
            };
        }
    }

    // ---------------------------------------------------------------------
    // Fluent Builder
    // ---------------------------------------------------------------------

    public static final class Builder {

        // Core
        private Browser browser = DEFAULT_BROWSER;
        private boolean remote = DEFAULT_REMOTE;
        private URL gridUrl;

        // Windowing & headless
        private boolean headless = DEFAULT_HEADLESS;
        private boolean maximize = DEFAULT_MAXIMIZE;
        private Integer width = DEFAULT_WINDOW_WIDTH;
        private Integer height = DEFAULT_WINDOW_HEIGHT;

        // Strategy & timeouts
        private PageLoadStrategy pageLoadStrategy = DEFAULT_PAGE_LOAD_STRATEGY;
        private Duration implicitWait    = DEFAULT_IMPLICIT_WAIT;
        private Duration pageLoadTimeout = DEFAULT_PAGELOAD_TIMEOUT;
        private Duration scriptTimeout   = DEFAULT_SCRIPT_TIMEOUT;

        // Security / network
        private boolean acceptInsecureCerts = DEFAULT_ACCEPT_INSECURE_CERTS;
        private Proxy proxy;

        // Extras
        private String downloadsDir;
        private String mobileEmulationDevice;
        private Map<String, Object> mobileEmulationMetrics;

        // Binary overrides
        private String chromeBinary;
        private String firefoxBinary;
        private String edgeBinary;

        // Raw knobs
        private final List<String> arguments = new ArrayList<>();
        private final Map<String, Object> prefs = new LinkedHashMap<>();
        private final Map<String, Object> capabilities = new LinkedHashMap<>();

        // ----- setters -----

        public Builder browser(Browser browser) {
            this.browser = (browser == null) ? DEFAULT_BROWSER : browser;
            return this;
        }

        public Builder remote(Boolean remote) {
            this.remote = (remote == null) ? DEFAULT_REMOTE : remote;
            return this;
        }

        public Builder gridUrl(String url) {
            if (url == null || url.isBlank()) this.gridUrl = null;
            else {
                try { this.gridUrl = new URL(url); }
                catch (MalformedURLException e) { throw new IllegalArgumentException("Invalid grid URL: " + url, e); }
            }
            return this;
        }

        public Builder headless(Boolean headless) {
            this.headless = (headless == null) ? DEFAULT_HEADLESS : headless;
            return this;
        }

        public Builder maximize(Boolean maximize) {
            this.maximize = (maximize == null) ? DEFAULT_MAXIMIZE : maximize;
            return this;
        }

        /** Set explicit window size (applied only when both width and height are non-null). */
        public Builder windowSize(Integer width, Integer height) {
            this.width = width; this.height = height; return this;
        }

        public Builder pageLoadStrategy(PageLoadStrategy pls) {
            this.pageLoadStrategy = (pls == null) ? DEFAULT_PAGE_LOAD_STRATEGY : pls;
            return this;
        }

        public Builder implicitWait(Duration d) {
            this.implicitWait = (d == null) ? DEFAULT_IMPLICIT_WAIT : d;
            return this;
        }

        public Builder pageLoadTimeout(Duration d) {
            this.pageLoadTimeout = (d == null) ? DEFAULT_PAGELOAD_TIMEOUT : d;
            return this;
        }

        public Builder scriptTimeout(Duration d) {
            this.scriptTimeout = (d == null) ? DEFAULT_SCRIPT_TIMEOUT : d;
            return this;
        }

        public Builder acceptInsecureCerts(Boolean accept) {
            this.acceptInsecureCerts = (accept == null) ? DEFAULT_ACCEPT_INSECURE_CERTS : accept;
            return this;
        }

        public Builder proxy(Proxy proxy) {
            this.proxy = proxy;
            return this;
        }

        public Builder downloadsDir(String path) {
            this.downloadsDir = (path == null || path.isBlank()) ? null : path;
            return this;
        }

        public Builder mobileEmulationDevice(String deviceName) {
            this.mobileEmulationDevice = (deviceName == null || deviceName.isBlank()) ? null : deviceName;
            return this;
        }

        public Builder mobileEmulationMetrics(Map<String, Object> metrics) {
            this.mobileEmulationMetrics = (metrics == null || metrics.isEmpty()) ? null : new LinkedHashMap<>(metrics);
            return this;
        }

        /** Add a browser CLI argument (ignored if null/blank). */
        public Builder addArg(String arg) {
            if (arg != null && !arg.isBlank()) arguments.add(arg);
            return this;
        }

        /** Add a browser preference (ignored if key is null). */
        public Builder addPref(String key, Object value) {
            if (key != null) prefs.put(key, value);
            return this;
        }

        /** Add an extra capability (ignored if key is null). */
        public Builder addCapability(String key, Object value) {
            if (key != null) capabilities.put(key, value);
            return this;
        }

        /** Override Chrome binary path. */
        public Builder chromeBinary(String path) {
            this.chromeBinary = (path == null || path.isBlank()) ? null : path.trim();
            return this;
        }

        /** Override Firefox binary path. */
        public Builder firefoxBinary(String path) {
            this.firefoxBinary = (path == null || path.isBlank()) ? null : path.trim();
            return this;
        }

        /** Override Edge binary path. */
        public Builder edgeBinary(String path) {
            this.edgeBinary = (path == null || path.isBlank()) ? null : path.trim();
            return this;
        }

        // ----- build -----

        /** Build and return a configured {@link WebDriver}. */
        public WebDriver build() {
            debug.log("DriverFactory.Builder.build(): browser=" + browser + ", remote=" + remote + ", headless=" + headless);

            // Defensive defaults
            if (browser == null) browser = DEFAULT_BROWSER;
            if (pageLoadStrategy == null) pageLoadStrategy = DEFAULT_PAGE_LOAD_STRATEGY;
            if (implicitWait == null) implicitWait = DEFAULT_IMPLICIT_WAIT;
            if (pageLoadTimeout == null) pageLoadTimeout = DEFAULT_PAGELOAD_TIMEOUT;
            if (scriptTimeout == null) scriptTimeout = DEFAULT_SCRIPT_TIMEOUT;

            WebDriver driver = switch (browser) {
                case CHROME  -> buildChrome();
                case FIREFOX -> buildFirefox();
                case EDGE    -> buildEdge();
            };

            // Global timeouts
            if (!implicitWait.isZero())    driver.manage().timeouts().implicitlyWait(implicitWait);
            if (!pageLoadTimeout.isZero()) driver.manage().timeouts().pageLoadTimeout(pageLoadTimeout);
            if (!scriptTimeout.isZero())   driver.manage().timeouts().scriptTimeout(scriptTimeout);

            // Window handling
            if (maximize) {
                try { driver.manage().window().maximize(); } catch (Exception ignored) {}
            }
            if (width != null && height != null) {
                try { driver.manage().window().setSize(new org.openqa.selenium.Dimension(width, height)); } catch (Exception ignored) {}
            }

            // Headless viewport fallback
            if (headless && (width == null || height == null)) {
                try {
                    driver.manage().window().setSize(new org.openqa.selenium.Dimension(DEFAULT_HEADLESS_WIDTH, DEFAULT_HEADLESS_HEIGHT));
                    debug.log("Applied headless fallback viewport: " + DEFAULT_HEADLESS_WIDTH + "x" + DEFAULT_HEADLESS_HEIGHT);
                } catch (Exception ignored) {}
            }

            // Remote downloads caveat
            if (remote && downloadsDir != null) {
                warn.log("downloadsDir='" + downloadsDir + "' set while remote=true. " +
                        "Containerized browsers may not write to local FS. Map volumes or use Grid/Selenoid strategies.");
            }

            debug.log("DriverFactory.Builder.build(): WebDriver created.");
            return driver;
        }

        // ----- per-browser builders -----

        private WebDriver buildChrome() {
            ChromeOptions opts = new ChromeOptions();
            opts.setPageLoadStrategy(pageLoadStrategy);
            opts.setAcceptInsecureCerts(acceptInsecureCerts);

            if (proxy != null) opts.setProxy(proxy);
            if (headless) opts.addArguments("--headless=new");
            if (!arguments.isEmpty()) opts.addArguments(arguments);
            if (chromeBinary != null) {
                opts.setBinary(chromeBinary);
                debug.log("Chrome binary: " + chromeBinary);
            }

            // Preferences (downloads, etc.)
            Map<String, Object> mergedPrefs = new LinkedHashMap<>(prefs);
            if (downloadsDir != null) {
                mergedPrefs.put("download.default_directory", downloadsDir);
                mergedPrefs.put("download.prompt_for_download", false);
                mergedPrefs.put("download.directory_upgrade", true);
                mergedPrefs.put("safebrowsing.enabled", true);
            }
            if (!mergedPrefs.isEmpty()) opts.setExperimentalOption("prefs", mergedPrefs);

            // Mobile emulation
            if (mobileEmulationDevice != null) {
                opts.setExperimentalOption("mobileEmulation", Map.of("deviceName", mobileEmulationDevice));
            } else if (mobileEmulationMetrics != null && !mobileEmulationMetrics.isEmpty()) {
                opts.setExperimentalOption("mobileEmulation", mobileEmulationMetrics);
            }

            // Extra capabilities
            capabilities.forEach(opts::setCapability);

            debug.log("Building CHROME driver. remote=" + remote);
            return remote ? new RemoteWebDriver(requireGridUrl(), opts) : new ChromeDriver(opts);
        }

        private WebDriver buildFirefox() {
            FirefoxOptions opts = new FirefoxOptions();
            opts.setPageLoadStrategy(pageLoadStrategy);
            opts.setAcceptInsecureCerts(acceptInsecureCerts);

            if (proxy != null) opts.setProxy(proxy);
            if (headless) opts.addArguments("-headless");
            if (!arguments.isEmpty()) opts.addArguments(arguments.toArray(new String[0]));
            if (firefoxBinary != null) {
                opts.setBinary(firefoxBinary);
                debug.log("Firefox binary: " + firefoxBinary);
            }

            // Downloads (Firefox prefs)
            if (downloadsDir != null) {
                opts.addPreference("browser.download.folderList", 2);
                opts.addPreference("browser.download.dir", downloadsDir);
                opts.addPreference("browser.helperApps.neverAsk.saveToDisk",
                        "application/pdf,application/octet-stream,application/zip,text/csv," +
                                "application/vnd.ms-excel,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                opts.addPreference("pdfjs.disabled", true);
            }
            // Merge additional prefs
            prefs.forEach((k, v) -> {
                if (k == null) return;
                if (v instanceof Boolean b) opts.addPreference(k, b);
                else if (v instanceof Integer i) opts.addPreference(k, i);
                else if (v instanceof String s) opts.addPreference(k, s);
            });

            capabilities.forEach(opts::setCapability);

            debug.log("Building FIREFOX driver. remote=" + remote);
            return remote ? new RemoteWebDriver(requireGridUrl(), opts) : new FirefoxDriver(opts);
        }

        private WebDriver buildEdge() {
            EdgeOptions opts = new EdgeOptions();
            opts.setPageLoadStrategy(pageLoadStrategy);
            opts.setAcceptInsecureCerts(acceptInsecureCerts);

            if (proxy != null) opts.setProxy(proxy);
            if (headless) opts.addArguments("--headless=new");
            if (!arguments.isEmpty()) opts.addArguments(arguments);
            if (edgeBinary != null) {
                opts.setBinary(edgeBinary);
                debug.log("Edge binary: " + edgeBinary);
            }

            if (downloadsDir != null) {
                Map<String, Object> mergedPrefs = new LinkedHashMap<>(prefs);
                mergedPrefs.put("download.default_directory", downloadsDir);
                mergedPrefs.put("download.prompt_for_download", false);
                mergedPrefs.put("download.directory_upgrade", true);
                opts.setExperimentalOption("prefs", mergedPrefs);
            } else if (!prefs.isEmpty()) {
                opts.setExperimentalOption("prefs", new LinkedHashMap<>(prefs));
            }

            capabilities.forEach(opts::setCapability);

            debug.log("Building EDGE driver. remote=" + remote);
            return remote ? new RemoteWebDriver(requireGridUrl(), opts) : new EdgeDriver(opts);
        }

        private URL requireGridUrl() {
            if (!remote) return null; // never used when remote=false
            if (gridUrl == null) throw new IllegalStateException("remote=true but gridUrl is not set");
            return gridUrl;
        }
    }

    // ---------------------------------------------------------------------
    // ENV → property mapping used by ConfigLoader
    // ---------------------------------------------------------------------
    private static Map<String, String> buildEnvMap() {
        Map<String, String> envMap = new LinkedHashMap<>();
        envMap.put("BROWSER", PROP_BROWSER);
        envMap.put("HEADLESS", PROP_HEADLESS);
        envMap.put("REMOTE", PROP_REMOTE);
        envMap.put("GRID_URL", PROP_GRID_URL);
        envMap.put("MAXIMIZE", PROP_MAXIMIZE);
        envMap.put("WIDTH", PROP_WIDTH);
        envMap.put("HEIGHT", PROP_HEIGHT);
        envMap.put("IMPLICIT_WAIT", PROP_IMPLICIT_WAIT);
        envMap.put("PAGELOAD_TIMEOUT", PROP_PAGELOAD_TIMEOUT);
        envMap.put("SCRIPT_TIMEOUT", PROP_SCRIPT_TIMEOUT);
        envMap.put("PAGELOAD_STRATEGY", PROP_PAGELOAD_STRATEGY);
        envMap.put("ACCEPT_INSECURE_CERTS", PROP_ACCEPT_INSECURE_CERTS);
        envMap.put("DOWNLOADS_DIR", PROP_DOWNLOADS_DIR);
        envMap.put("MOBILE_DEVICE", PROP_MOBILE_DEVICE);
        envMap.put("CHROME_BINARY", PROP_CHROME_BINARY);
        envMap.put("FIREFOX_BINARY", PROP_FIREFOX_BINARY);
        envMap.put("EDGE_BINARY", PROP_EDGE_BINARY);
        // Proxy & args
        envMap.put("PROXY_HTTP", "proxy.http");
        envMap.put("PROXY_SSL", "proxy.ssl");
        envMap.put("PROXY_SOCKS", "proxy.socks");
        envMap.put("PROXY_SOCKS_VERSION", "proxy.socksVersion");
        envMap.put("BROWSER_ARGS", "args");
        return envMap;
    }

    // ---------------------------------------------------------------------
    // Extraction helpers
    // ---------------------------------------------------------------------
    private static List<String> extractArgs(Properties p) {
        List<String> args = new ArrayList<>();
        String argsCsv = p.getProperty("args");
        if (argsCsv != null && !argsCsv.isBlank()) {
            for (String a : argsCsv.split(",")) {
                if (a != null && !a.isBlank()) args.add(a.trim());
            }
        }
        p.stringPropertyNames().stream()
                .filter(k -> k.startsWith("arg."))
                .sorted()
                .forEach(k -> {
                    String val = p.getProperty(k);
                    if (val != null && !val.isBlank()) args.add(val.trim());
                });
        return args;
    }

    private static Map<String, Object> extractPrefs(Properties p) {
        Map<String, Object> prefs = new LinkedHashMap<>();
        p.stringPropertyNames().stream()
                .filter(k -> k.startsWith("pref."))
                .forEach(k -> {
                    String key = k.substring("pref.".length());
                    String val = p.getProperty(k);
                    if (!key.isBlank() && val != null && !val.isBlank()) {
                        prefs.put(key, val);
                    }
                });
        return prefs;
    }

    private static Map<String, Object> extractCapabilities(Properties p) {
        Map<String, Object> caps = new LinkedHashMap<>();
        p.stringPropertyNames().stream()
                .filter(k -> k.startsWith("cap."))
                .forEach(k -> {
                    String key = k.substring("cap.".length());
                    String raw = p.getProperty(k);
                    if (!key.isBlank() && raw != null && !raw.isBlank()) {
                        caps.put(key, coerce(raw));
                    }
                });
        return caps;
    }

    private static boolean parseBool(String v, boolean def) {
        return (v == null || v.isBlank()) ? def : Boolean.parseBoolean(v.trim());
    }

    private static Integer parseInt(String v, Integer def) {
        if (v == null || v.isBlank()) return def;
        try { return Integer.parseInt(v.trim()); } catch (NumberFormatException e) { return def; }
    }

    private static Duration parseSeconds(String v, Duration def) {
        if (v == null || v.isBlank()) return def;
        try { return Duration.ofSeconds(Long.parseLong(v.trim())); } catch (NumberFormatException e) { return def; }
    }

    /** Coerce string into Boolean/Integer/Long/Double if possible, else String. */
    private static Object coerce(String raw) {
        if (raw == null) return null;
        String v = raw.trim();
        if (v.equalsIgnoreCase("true") || v.equalsIgnoreCase("false")) return Boolean.parseBoolean(v);
        try { return Integer.valueOf(v); } catch (NumberFormatException ignore) {}
        try { return Long.valueOf(v); }    catch (NumberFormatException ignore) {}
        try { return Double.valueOf(v); }  catch (NumberFormatException ignore) {}
        return v;
    }

    /** Build a Selenium Proxy from proxy.* properties (all optional). */
    private static Proxy buildProxyFromProps(Properties p) {
        String http  = p.getProperty("proxy.http");
        String ssl   = p.getProperty("proxy.ssl");
        String socks = p.getProperty("proxy.socks");
        String socksVer = p.getProperty("proxy.socksVersion");

        if ((http == null || http.isBlank()) &&
                (ssl  == null || ssl.isBlank())  &&
                (socks== null || socks.isBlank())) {
            return null;
        }

        Proxy proxy = new Proxy();
        if (http  != null && !http.isBlank())  proxy.setHttpProxy(http.trim());
        if (ssl   != null && !ssl.isBlank())   proxy.setSslProxy(ssl.trim());
        if (socks != null && !socks.isBlank()) proxy.setSocksProxy(socks.trim());
        if (socksVer != null && !socksVer.isBlank()) {
            try { proxy.setSocksVersion(Integer.parseInt(socksVer.trim())); }
            catch (NumberFormatException ignore) { warn.log("Invalid proxy.socksVersion: " + socksVer); }
        }
        return proxy;
    }

    // ---------------------------------------------------------------------
    // Internal: map profile → resources path (so templates land on classpath)
    // ---------------------------------------------------------------------
    private static Path resolveResourceTemplatePath(Profile profile) {
        String cp = switch (profile) {
            case LOCAL -> DEFAULT_LOCAL_PROPERTIES_CLASSPATH;
            case CI    -> DEFAULT_CI_PROPERTIES_CLASSPATH;
            case GRID  -> DEFAULT_GRID_PROPERTIES_CLASSPATH;
            default    -> DEFAULT_PROPERTIES_CLASSPATH;
        };
        return RESOURCES_BASE.resolve(cp.replace("/", FileSystems.getDefault().getSeparator()));
    }
}
