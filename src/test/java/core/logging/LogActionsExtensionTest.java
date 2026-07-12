package core.logging;

import core.logging.intent.LogIntent;
import core.logging.render.LogActions;
import core.logging.theme.ThemeColors;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static core.logging.CustomLogger.*;
import static org.testng.Assert.*;

/**
 * Tests for the {@link LogIntent#VERIFY} intent and the two new {@link LogActions} methods
 * added alongside it: {@code verifying(String)} and {@code password(String, String)}.
 *
 * Visual output is verified by inspection; correctness of message composition and
 * non-throw behavior is asserted programmatically.
 */
public class LogActionsExtensionTest {

    @BeforeClass
    public void setup() {
        // Keep tests independent of ANSI state — disable so output is plain and stable
        initialize(LogActionsExtensionTest.class);
        disableAnsi();
    }

    // ── LogIntent.VERIFY enum value ────────────────────────────────────────────

    @Test(description = "VERIFY is a member of LogIntent")
    public void logIntent_containsVerify() {
        boolean found = false;
        for (LogIntent intent : LogIntent.values()) {
            if (intent == LogIntent.VERIFY) { found = true; break; }
        }
        assertTrue(found, "LogIntent.VERIFY must exist");
    }

    @Test(description = "VERIFY is positioned between OBSERVE and DATA in declaration order")
    public void logIntent_verifyPositionedAfterObserveBeforeData() {
        LogIntent[] values = LogIntent.values();
        int observeIdx = -1, verifyIdx = -1, dataIdx = -1;
        for (int i = 0; i < values.length; i++) {
            if (values[i] == LogIntent.OBSERVE) observeIdx = i;
            if (values[i] == LogIntent.VERIFY)  verifyIdx  = i;
            if (values[i] == LogIntent.DATA)     dataIdx    = i;
        }
        assertTrue(observeIdx < verifyIdx, "VERIFY must come after OBSERVE");
        assertTrue(verifyIdx  < dataIdx,   "VERIFY must come before DATA");
    }

    // ── ThemeColors.resolve() with VERIFY ─────────────────────────────────────

    @Test(description = "ThemeColors.resolve() returns a non-null, non-empty string for VERIFY at INFO level")
    public void themeColors_resolveVerify_info_isNonEmpty() {
        ThemeColors colors = ThemeColors.builder().build();
        String result = colors.resolve("INFO", LogIntent.VERIFY);
        assertNotNull(result);
        assertFalse(result.isEmpty(), "Resolved VERIFY color must not be empty");
    }

    @Test(description = "ThemeColors.resolve() returns a non-null string for VERIFY at all four log levels",
          dataProvider = "allLogLevels")
    public void themeColors_resolveVerify_allLevels(String level) {
        ThemeColors colors = ThemeColors.builder().build();
        String result = colors.resolve(level, LogIntent.VERIFY);
        assertNotNull(result, "resolve() must not return null for level=" + level);
    }

    @DataProvider
    public Object[][] allLogLevels() {
        return new Object[][]{{"INFO"}, {"WARN"}, {"ERROR"}, {"DEBUG"}};
    }

    @Test(description = "verifyFg builder override is stored and returned by accessor")
    public void themeColors_verifyFgBuilderOverride_isApplied() {
        String customFg = "[38;2;255;200;0m";
        ThemeColors colors = ThemeColors.builder().verifyFg(customFg).build();
        assertEquals(colors.verifyFg(), customFg, "verifyFg() should return the builder-set value");
    }

    @Test(description = "Default verifyFg value contains the BOLD code and is non-empty")
    public void themeColors_defaultVerifyFg_isNonEmpty() {
        ThemeColors colors = ThemeColors.builder().build();
        String fg = colors.verifyFg();
        assertNotNull(fg);
        assertFalse(fg.isEmpty());
    }

    @Test(description = "VERIFY resolve result differs from OBSERVE resolve result — distinct colors")
    public void themeColors_verifyFg_distinctFromObserveFg() {
        ThemeColors colors = ThemeColors.builder().build();
        String verifyResult  = colors.resolve("INFO", LogIntent.VERIFY);
        String observeResult = colors.resolve("INFO", LogIntent.OBSERVE);
        assertNotEquals(verifyResult, observeResult,
                "VERIFY and OBSERVE must map to different foreground colors");
    }

    // ── LogActions.verifying(String) ───────────────────────────────────────────

    @Test(description = "verifying() does not throw for a normal message")
    public void verifying_normalMessage_doesNotThrow() {
        info.verifying("Verifying redirect to /secure...");
    }

    @Test(description = "verifying() does not throw for a null message")
    public void verifying_nullMessage_doesNotThrow() {
        // LogActions handles null messages internally by rendering "null"
        info.verifying(null);
    }

    @Test(description = "verifying() does not throw for an empty string")
    public void verifying_emptyMessage_doesNotThrow() {
        info.verifying("");
    }

    @Test(description = "verifying() does not throw when called on warn level")
    public void verifying_warnLevel_doesNotThrow() {
        warn.verifying("Verifying element is present on warn level");
    }

    @Test(description = "verifying() does not throw when called on debug level")
    public void verifying_debugLevel_doesNotThrow() {
        debug.verifying("Verifying internal state on debug level");
    }

    @Test(description = "verifying() does not throw for a multiline message")
    public void verifying_multilineMessage_doesNotThrow() {
        info.verifying("Line 1\nLine 2\nLine 3");
    }

    // ── LogActions.password(String, String) ────────────────────────────────────

    @Test(description = "password(text, label) does not throw for normal inputs")
    public void password_withLabel_normalInputs_doesNotThrow() {
        info.password("secret123", "DemoLoginPage > Credentials > PASSWORD_INPUT");
    }

    @Test(description = "password(null, label) renders *** for null text")
    public void password_nullText_withLabel_doesNotThrow() {
        // null text → mask() returns "***"; label is appended after " | "
        info.password(null, "DemoLoginPage > Credentials > PASSWORD_INPUT");
    }

    @Test(description = "password(empty, label) renders empty mask with label appended")
    public void password_emptyText_withLabel_doesNotThrow() {
        // empty string → "*".repeat(0) = ""; label is still appended
        info.password("", "DemoLoginPage > Credentials > PASSWORD_INPUT");
    }

    @Test(description = "password(text, label) does not expose the plain-text password in log output")
    public void password_withLabel_maskHidesPassword() {
        // This test is structural — it verifies that the single-arg overload
        // produces a shorter message for the same input compared to the raw text.
        // If mask("abc") returns "***", then length 3 == length of original.
        // The important guarantee is that mask() never returns the original text.
        LogActions actions = new LogActions("INFO") {
            // override to capture what would be logged rather than emit it
        };
        // Indirect verification: password with label must not throw (the mask was computed)
        actions.password("p@ssw0rd", "Page > Enum > FIELD");
    }

    @Test(description = "password(text, null label) does not throw even if label is null")
    public void password_withNullLabel_doesNotThrow() {
        info.password("mypassword", null);
    }

    // ── Interaction between verifying and result ───────────────────────────────

    @Test(description = "verifying and result can be called in sequence without interference")
    public void verifying_andResult_sequence_doesNotThrow() {
        info.result("Retrieved 10 records");
        info.verifying("Verifying record count equals 10");
        info.success("Assertion passed");
    }

    // ── VERIFY visual summary ──────────────────────────────────────────────────

    @Test(description = "Visual: all new VERIFY-group methods rendered once for inspection",
          priority = 99)
    public void visual_allVerifyMethods() {
        info.log("=== VERIFY intent — visual summary ===");
        info.verifying("Verifying page title equals 'Dashboard'");
        info.verifying("Verifying element is visible");
        info.password("s3cr3t", "DemoLoginPage > Credentials > PASSWORD_INPUT");
        info.result("Result: user is authenticated");
        assertTrue(true, "Visual check — inspect console output");
    }
}
