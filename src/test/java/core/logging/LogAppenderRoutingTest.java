package core.logging;

import examples.logging.CallerChainHelper;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.layout.PatternLayout;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static core.logging.CustomLogger.*;
import static org.testng.Assert.*;

/**
 * Unit tests for the appender-routing logic in {@code LogActions.logMultiline}.
 *
 * <p>Covers the three regressions fixed on branch hotfix/debug-trace-caller-chain:
 * <ol>
 *   <li>DEBUG entries must appear in debug-trace regardless of root logger level.</li>
 *   <li>DEBUG entries must NOT appear in partial-trace when root logger is INFO.</li>
 *   <li>debug-trace must always include the caller chain even when the root logger
 *       is at INFO (i.e. {@code showCaller} is false in the console path).</li>
 * </ol>
 *
 * <p>Each test method attaches in-memory {@link CapturingAppender}s to the three
 * named Log4j2 loggers, executes a log call, then asserts on the captured strings.
 * Appenders are detached and root / named logger levels are restored after every test.
 */
public class LogAppenderRoutingTest {

    // ── In-memory appender ────────────────────────────────────────────────────

    private static final class CapturingAppender extends AbstractAppender {

        private final List<String> captured = new CopyOnWriteArrayList<>();

        CapturingAppender(String name) {
            super(name, null, PatternLayout.createDefaultLayout(), true, Property.EMPTY_ARRAY);
        }

        @Override
        public void append(LogEvent event) {
            captured.add(event.getMessage().getFormattedMessage());
        }

        List<String> messages()             { return captured; }
        boolean       hasMessage(String s)   { return captured.stream().anyMatch(m -> m.contains(s)); }
    }

    // ── Appender handles ──────────────────────────────────────────────────────

    private CapturingAppender debugCapture;
    private CapturingAppender partialCapture;
    private CapturingAppender traceCapture;

    // Saved logger levels for restore
    private Level savedRootLevel;
    private Level savedDebugTraceLevel;
    private Level savedPartialTraceLevel;

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @BeforeMethod
    public void setup() {
        initialize(LogAppenderRoutingTest.class);
        disableAnsi();

        org.apache.logging.log4j.core.LoggerContext ctx = log4jCtx();
        Configuration cfg = ctx.getConfiguration();

        // Save current levels so they can be restored after every test
        savedRootLevel         = cfg.getRootLogger().getLevel();
        savedDebugTraceLevel   = cfg.getLoggerConfig("debug-trace").getLevel();
        savedPartialTraceLevel = cfg.getLoggerConfig("partial-trace").getLevel();

        // Fix root logger to INFO so rootOk=false for DEBUG -- this is the
        // scenario that exposed all three regressions
        cfg.getRootLogger().setLevel(Level.INFO);
        cfg.getLoggerConfig("debug-trace").setLevel(Level.DEBUG);
        cfg.getLoggerConfig("partial-trace").setLevel(Level.DEBUG);

        debugCapture   = new CapturingAppender("test-debug-capture");
        partialCapture = new CapturingAppender("test-partial-capture");
        traceCapture   = new CapturingAppender("test-trace-capture");

        debugCapture.start();
        partialCapture.start();
        traceCapture.start();

        cfg.getLoggerConfig("debug-trace").addAppender(debugCapture,   null, null);
        cfg.getLoggerConfig("partial-trace").addAppender(partialCapture, null, null);
        cfg.getLoggerConfig("trace").addAppender(traceCapture,           null, null);
        ctx.updateLoggers();
    }

    @AfterMethod
    public void teardown() {
        org.apache.logging.log4j.core.LoggerContext ctx = log4jCtx();
        Configuration cfg = ctx.getConfiguration();

        cfg.getLoggerConfig("debug-trace").removeAppender("test-debug-capture");
        cfg.getLoggerConfig("partial-trace").removeAppender("test-partial-capture");
        cfg.getLoggerConfig("trace").removeAppender("test-trace-capture");

        cfg.getRootLogger().setLevel(savedRootLevel);
        cfg.getLoggerConfig("debug-trace").setLevel(savedDebugTraceLevel);
        cfg.getLoggerConfig("partial-trace").setLevel(savedPartialTraceLevel);
        ctx.updateLoggers();
    }

    private static org.apache.logging.log4j.core.LoggerContext log4jCtx() {
        return (org.apache.logging.log4j.core.LoggerContext) LogManager.getContext(false);
    }

    // ── Regression 1: DEBUG entries must appear in debug-trace ────────────────

    @Test(description = "Regression 1 -- DEBUG entry must appear in debug-trace when root logger is INFO")
    public void debugLevelEntry_appearsInDebugTrace_whenRootIsInfo() {
        debug.log("dt-regression-1-marker");

        assertTrue(debugCapture.hasMessage("dt-regression-1-marker"),
                "debug-trace must capture DEBUG entries regardless of root logger level");
    }

    // ── Regression 2: DEBUG entries must NOT appear in partial-trace ──────────

    @Test(description = "Regression 2 -- DEBUG entry must be absent from partial-trace when root logger is INFO")
    public void debugLevelEntry_absentFromPartialTrace_whenRootIsInfo() {
        debug.log("pt-regression-2-marker");

        assertFalse(partialCapture.hasMessage("pt-regression-2-marker"),
                "partial-trace must not capture DEBUG entries when root logger is INFO");
    }

    // ── Regression 3: caller chain must always appear in debug-trace ──────────

    @Test(description = "Regression 3 -- debug-trace must include rawCaller even when root logger is INFO (showCaller=false)")
    public void debugTrace_containsCallerChain_fromProjectFrame_whenRootIsInfo() {
        // CallerChainHelper is in tests.* which is a project prefix, so its frame
        // survives the filteredOut() check and becomes rawCaller in logMultiline.
        CallerChainHelper.triggerDebugLog("chain-regression-3-marker");

        List<String> entries = debugCapture.messages().stream()
                .filter(m -> m.contains("chain-regression-3-marker"))
                .toList();
        assertFalse(entries.isEmpty(),
                "debug-trace must capture the DEBUG entry");
        assertTrue(entries.stream().anyMatch(m -> m.contains("CallerChainHelper")),
                "debug-trace entry must include the caller class in the chain suffix");
    }

    // ── Baseline: INFO routes to both debug-trace and partial-trace ───────────

    @Test(description = "INFO entry must appear in both debug-trace and partial-trace")
    public void infoLevelEntry_appearsInBothDebugTraceAndPartialTrace() {
        info.log("info-routing-marker");

        assertTrue(debugCapture.hasMessage("info-routing-marker"),
                "debug-trace must receive INFO entries");
        assertTrue(partialCapture.hasMessage("info-routing-marker"),
                "partial-trace must receive INFO entries");
    }

    @Test(description = "ERROR entry must appear in both debug-trace and partial-trace")
    public void errorLevelEntry_appearsInBothDebugTraceAndPartialTrace() {
        error.log("error-routing-marker");

        assertTrue(debugCapture.hasMessage("error-routing-marker"),
                "debug-trace must receive ERROR entries");
        assertTrue(partialCapture.hasMessage("error-routing-marker"),
                "partial-trace must receive ERROR entries");
    }

    // ── full-trace always writes regardless of level ──────────────────────────

    @Test(description = "DEBUG entry must always appear in full-trace regardless of root logger level")
    public void debugLevelEntry_alwaysAppearsInFullTrace() {
        debug.log("full-trace-always-marker");

        assertTrue(traceCapture.hasMessage("full-trace-always-marker"),
                "full-trace must capture all entries regardless of root logger level");
    }

    // ── Caller chain isolation between appenders ──────────────────────────────

    @Test(description = "partial-trace entries must never contain a caller chain arrow")
    public void partialTrace_neverContainsCallerChain() {
        CallerChainHelper.triggerInfoLog("pt-no-chain-marker");

        List<String> entries = partialCapture.messages().stream()
                .filter(m -> m.contains("pt-no-chain-marker"))
                .toList();
        assertFalse(entries.isEmpty(),
                "partial-trace must receive the INFO entry");
        assertTrue(entries.stream().noneMatch(m -> m.contains("←")),
                "partial-trace entries must not contain a caller chain arrow (←)");
    }

    @Test(description = "debug-trace INFO entry must include caller chain when called from a project frame")
    public void debugTrace_containsCallerChain_forInfoEntry_fromProjectFrame() {
        CallerChainHelper.triggerInfoLog("dt-chain-info-marker");

        List<String> entries = debugCapture.messages().stream()
                .filter(m -> m.contains("dt-chain-info-marker"))
                .toList();
        assertFalse(entries.isEmpty(), "debug-trace must receive the INFO entry");
        assertTrue(entries.stream().anyMatch(m -> m.contains("CallerChainHelper")),
                "debug-trace INFO entry must include the caller class in the chain suffix");
    }

    // ── Structural correctness ────────────────────────────────────────────────

    @Test(description = "null message must be rendered as the literal string 'null' without throwing")
    public void nullMessage_renderedSafely() {
        info.log((String) null);

        assertTrue(partialCapture.messages().stream().anyMatch(m -> m.contains("null")),
                "null message must be rendered as the literal string 'null'");
    }

    @Test(description = "Multiline message must produce one entry per line in each appender")
    public void multilineMessage_producesOneEntryPerLine() {
        info.log("line-alpha\nline-beta\nline-gamma");

        long count = partialCapture.messages().stream()
                .filter(m -> m.contains("line-alpha")
                          || m.contains("line-beta")
                          || m.contains("line-gamma"))
                .count();
        assertEquals(count, 3L,
                "Each line of a multiline message must produce a separate appender entry");
    }

    @Test(description = "Caller chain must appear only on the first line of a multiline message in debug-trace")
    public void multilineMessage_callerChainOnFirstLineOnly() {
        CallerChainHelper.triggerInfoLog("first-ml-line\nsecond-ml-line");

        List<String> firstLines  = debugCapture.messages().stream()
                .filter(m -> m.contains("first-ml-line")).toList();
        List<String> secondLines = debugCapture.messages().stream()
                .filter(m -> m.contains("second-ml-line")).toList();

        assertFalse(firstLines.isEmpty(),  "debug-trace must capture the first line");
        assertFalse(secondLines.isEmpty(), "debug-trace must capture the second line");
        assertTrue(firstLines.stream().anyMatch(m -> m.contains("CallerChainHelper")),
                "Caller chain must appear on the first line of a multiline entry in debug-trace");
        assertTrue(secondLines.stream().noneMatch(m -> m.contains("CallerChainHelper")),
                "Caller chain must not be repeated on subsequent lines in debug-trace");
    }
}
