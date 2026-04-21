package core.logging;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static core.logging.CustomLogger.*;

/**
 * Focused experiment class for testing different ANSI injection strategies
 * inside logMultiline-style output.
 *
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  DEFINITIVE FINDINGS  (run 2026-04-20)                                  ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║  IntelliJ's TestNG output viewer splits at EVERY ANSI escape sequence,  ║
 * ║  not just at ANSI_RESET.  The rule is exact:                            ║
 * ║                                                                          ║
 * ║    N ANSI codes in the string  =  N styled text runs  =  N lines        ║
 * ║                                   in Test History                        ║
 * ║                                                                          ║
 * ║  Tested approaches and their Test History line count:                   ║
 * ║                                                                          ║
 * ║  Baseline A — plain text (0 ANSI codes)          → 1 line  ✅           ║
 * ║  Baseline B — single [OPEN…RESET] (2 codes)      → 1 line  ✅           ║
 * ║  Baseline C — 4 blocks + intermediate RESET       → 6 lines ❌           ║
 * ║  Exp 1A — chained BG changes, no RESET mid       → 4 lines ❌           ║
 * ║  Exp 1B — chained FG changes, no RESET mid       → 4 lines ❌           ║
 * ║  Exp 1C — chained FG+BG changes, no RESET mid    → 4 lines ❌           ║
 * ║  Exp 2A — BOLD within single-color block         → 4 lines ❌           ║
 * ║  Exp 2B — DIM for caller only                    → 2 lines ❌           ║
 * ║  Exp 3A — CustomLogger single-block (control)    → 1 line  ✅           ║
 * ║  Exp 4A — exactly 2 blocks (2 RESETs)            → 2 lines ❌           ║
 * ║  Exp 4B — exactly 3 blocks (3 RESETs)            → 3 lines ❌           ║
 * ║  Exp 5  — ideal chained format                   → 4 lines ❌           ║
 * ║                                                                          ║
 * ║  CONCLUSION: The ONLY approaches that produce a single line are:        ║
 * ║    1. Zero ANSI codes — plain text                                       ║
 * ║    2. Exactly one ANSI open + one ANSI_RESET (single block)             ║
 * ║                                                                          ║
 * ║  Per-part coloring (ts grey, label colored, message colored, caller     ║
 * ║  grey) is INCOMPATIBLE with single-line Test History output.            ║
 * ║  CustomLogger uses approach 2 (single block per line).                  ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
public class LogMultilineExperimentTest {

    private static final DateTimeFormatter TS_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    // ── helpers ───────────────────────────────────────────────────────────────

    private static String ts() {
        return "[" + LocalDateTime.now().format(TS_FMT) + "]";
    }

    /** Print directly to stdout, bypassing log4j entirely. */
    private static void out(String line) {
        System.out.println(line);
    }

    /** Separator printed before each experiment so output is easy to scan. */
    @BeforeMethod
    public void header(Method m) {
        out("");
        out("══════════════════════════════════════════════════════════════════");
        out("  EXPERIMENT: " + m.getName());
        out("══════════════════════════════════════════════════════════════════");
    }

    // =========================================================================
    // BASELINE — known behaviours
    // =========================================================================

    /**
     * BASELINE A — Plain text, zero ANSI.
     * Expected: 1 line in Test History ✅
     */
    @Test(priority = 1, description = "Baseline A — plain text, zero ANSI")
    public void baselinePlainText() {
        out("LABEL: [plain text]");
        out(ts() + " [INFO] This is plain text  testMethod ← runMethod");
    }

    /**
     * BASELINE B — Single ANSI open + single ANSI_RESET.
     * Expected: 1 line in Test History ✅ (proven)
     */
    @Test(priority = 2, description = "Baseline B — single ANSI block [OPEN...RESET]")
    public void baselineSingleBlock() {
        out("LABEL: [single ANSI block]");
        String line = BG_GREY_100 + FG_BLACK
                + ts() + " [INFO] This is a single-block line  testMethod ← runMethod"
                + ANSI_RESET;
        out(line);
    }

    /**
     * BASELINE C — Multiple ANSI blocks with intermediate RESET between each.
     * Expected: MULTIPLE lines in Test History ❌ (proven broken)
     */
    @Test(priority = 3, description = "Baseline C — multiple blocks WITH intermediate RESET [known broken]")
    public void baselineMultipleBlocksWithReset() {
        out("LABEL: [multiple blocks + intermediate RESET — expect broken in Test History]");
        String line =
                BG_GREY_100 + FG_BLACK + ts() + ANSI_RESET           // ts chip
                + " " + BG_MAGENTA + FG_BRIGHT_WHITE + "[INFO]" + ANSI_RESET  // label chip
                + " " + FG_BRIGHT_WHITE + "This is the message" + ANSI_RESET  // message
                + " " + BG_GREY_100 + FG_BRIGHT_CYAN + "testMethod ← runMethod" + ANSI_RESET; // caller chip
        out(line);
    }

    // =========================================================================
    // EXPERIMENT 1 — Chained colors, NO intermediate RESET
    // =========================================================================

    /**
     * EXP 1A — Chain BG changes only (FG stays constant), no intermediate RESET.
     * Hypothesis: IntelliJ might treat this as one segment since no RESET occurs.
     * → Check Test History: 1 line or multiple?
     */
    @Test(priority = 10, description = "Exp 1A — chained BG-only changes, no intermediate RESET")
    public void exp1A_chainedBgNoReset() {
        out("LABEL: [chained BG changes, single RESET at end]");
        // Start: dark-grey BG + black FG
        // Mid:   change BG to magenta (FG inherits black)
        // End:   change BG to black (FG inherits black — may be invisible, but tests the split)
        String line =
                BG_GREY_100 + FG_BLACK + ts() + " "   // ts: dark-grey bg
                + BG_MAGENTA + "[INFO] "               // label: magenta bg, NO RESET, just BG change
                + BG_BLACK + FG_BRIGHT_WHITE + "This is the message  " // message: black bg
                + BG_GREY_100 + FG_BRIGHT_CYAN + "testMethod ← runMethod"  // caller: dark-grey
                + ANSI_RESET;                          // ONE reset at end
        out(line);
    }

    /**
     * EXP 1B — Chain FG changes only (BG stays constant), no intermediate RESET.
     */
    @Test(priority = 11, description = "Exp 1B — chained FG-only changes, no intermediate RESET")
    public void exp1B_chainedFgNoReset() {
        out("LABEL: [chained FG changes, single BG, single RESET at end]");
        String line =
                BG_GREY_100                                        // one BG for the whole line
                + FG_BLACK + ts() + " "                           // ts: black fg
                + FG_BRIGHT_WHITE + "[INFO] "                     // label: bright-white fg
                + FG_BRIGHT_YELLOW + "This is the message  "      // message: yellow fg
                + FG_BRIGHT_CYAN + "testMethod ← runMethod"       // caller: cyan fg
                + ANSI_RESET;                                      // ONE reset
        out(line);
    }

    /**
     * EXP 1C — Chain both FG and BG changes, no intermediate RESET.
     */
    @Test(priority = 12, description = "Exp 1C — chained FG+BG changes, no intermediate RESET")
    public void exp1C_chainedFgBgNoReset() {
        out("LABEL: [chained FG+BG changes, single RESET at end]");
        String line =
                BG_GREY_100 + FG_BLACK + ts() + " "              // ts: grey bg, black fg
                + BG_MAGENTA + FG_BRIGHT_WHITE + "[INFO] "        // label: magenta bg, white fg
                + BG_BLACK + FG_BRIGHT_WHITE + "This is the message  "  // message: black bg
                + BG_GREY_100 + FG_BRIGHT_CYAN + "testMethod ← runMethod"  // caller
                + ANSI_RESET;                                      // ONE reset
        out(line);
    }

    // =========================================================================
    // EXPERIMENT 2 — BOLD / DIM attributes (no color changes)
    // =========================================================================

    /**
     * EXP 2A — BOLD on/off within a single colored block.
     * Tests whether text-attribute changes (not color) also cause splits.
     */
    @Test(priority = 20, description = "Exp 2A — BOLD attribute change within single-color block")
    public void exp2A_boldWithinBlock() {
        out("LABEL: [bold attribute changes within one color block]");
        // \u001B[1m = bold on,  \u001B[22m = bold off  (no color change, no RESET)
        String BOLD_ON  = "\u001B[1m";
        String BOLD_OFF = "\u001B[22m";
        String line =
                BG_GREY_100 + FG_BRIGHT_WHITE
                + BOLD_OFF + ts() + " "                           // ts: normal weight
                + BOLD_ON  + "[INFO] " + BOLD_OFF                 // label: bold, then back to normal
                + "This is the message  "                          // message: normal
                + BOLD_OFF + FG_BRIGHT_CYAN + "testMethod ← runMethod"  // caller: dimmer
                + ANSI_RESET;
        out(line);
    }

    /**
     * EXP 2B — DIM attribute for the caller section only.
     * \u001B[2m = dim,  \u001B[22m = normal intensity
     */
    @Test(priority = 21, description = "Exp 2B — DIM for caller within single-color block")
    public void exp2B_dimCaller() {
        out("LABEL: [DIM attribute for caller, single color block]");
        String DIM = "\u001B[2m";
        String NORMAL = "\u001B[22m";
        String line =
                BG_GREY_100 + FG_BRIGHT_WHITE
                + ts() + " [INFO] This is the message  "
                + DIM + "testMethod ← runMethod" + NORMAL
                + ANSI_RESET;
        out(line);
    }

    // =========================================================================
    // EXPERIMENT 3 — CustomLogger logMultiline equivalent
    // =========================================================================

    /**
     * EXP 3A — Reproduce current CustomLogger single-block exactly.
     * Must be 1 line in Test History ✅ (control)
     */
    @Test(priority = 30, description = "Exp 3A — reproduce CustomLogger single-block (control)")
    public void exp3A_customLoggerSingleBlock() {
        out("LABEL: [CustomLogger single-block — control, must be 1 line]");
        enableAnsi();
        info.log("CustomLogger INFO single-block — control line");
        warn.log("CustomLogger WARN single-block — control line");
        error.log("CustomLogger ERROR single-block — control line");
        disableAnsi();
    }

    /**
     * EXP 3B — CustomLogger with chained multi-color (Exp 1C logic applied to logMultiline).
     * Manually builds the same output that a chained-color logMultiline would produce.
     * Compare its Test History appearance with 3A.
     */
    @Test(priority = 31, description = "Exp 3B — chained FG+BG (no intermediate RESET) mimicking logMultiline")
    public void exp3B_chainedColorMimicLogMultiline() {
        out("LABEL: [chained color mimicking logMultiline — compare with Exp 3A in Test History]");

        String caller = "testInfoLog ← runMethod";
        String message = "This is a chained-color INFO line";

        // Exactly what a chained-color logMultiline would emit:
        String line =
                BG_GREY_100 + FG_BLACK + ts() + " "              // ts segment
                + BG_MAGENTA + FG_BRIGHT_WHITE + "[INFO] "        // label segment
                + BG_BLACK + FG_BRIGHT_WHITE + message + "  "     // message segment
                + BG_GREY_100 + FG_BRIGHT_CYAN + caller           // caller segment
                + ANSI_RESET;                                      // single RESET
        out(line);

        // Also try warn:
        String warnLine =
                BG_GREY_100 + FG_BLACK + ts() + " "
                + BG_YELLOW + FG_BLACK + "[WARN] "
                + BG_BLACK + FG_BRIGHT_YELLOW + "This is a chained-color WARN line  "
                + BG_GREY_100 + FG_BRIGHT_CYAN + caller
                + ANSI_RESET;
        out(warnLine);
    }

    // =========================================================================
    // EXPERIMENT 4 — Minimal splits: 2-block vs 3-block
    // =========================================================================

    /**
     * EXP 4A — Exactly 2 ANSI blocks (2×RESET).
     * Tests whether 2 blocks always = 2 lines, or if Test History is more lenient.
     */
    @Test(priority = 40, description = "Exp 4A — exactly 2 ANSI blocks (2 RESET codes)")
    public void exp4A_twoBlocks() {
        out("LABEL: [exactly 2 ANSI blocks — does Test History show 1 or 2 lines?]");
        // Block 1: ts+label  Block 2: message+caller
        String line =
                BG_GREY_100 + FG_BLACK + ts() + " [INFO] " + ANSI_RESET
                + BG_MAGENTA + FG_BRIGHT_WHITE + "message  testMethod ← runMethod" + ANSI_RESET;
        out(line);
    }

    /**
     * EXP 4B — Exactly 3 ANSI blocks (3×RESET).
     */
    @Test(priority = 41, description = "Exp 4B — exactly 3 ANSI blocks (3 RESET codes)")
    public void exp4B_threeBlocks() {
        out("LABEL: [exactly 3 ANSI blocks — does Test History show 1, 2, or 3 lines?]");
        String line =
                BG_GREY_100 + FG_BLACK + ts() + " [INFO] " + ANSI_RESET
                + BG_MAGENTA + FG_BRIGHT_WHITE + "message" + ANSI_RESET
                + " " + BG_GREY_100 + FG_BRIGHT_CYAN + "testMethod ← runMethod" + ANSI_RESET;
        out(line);
    }

    // =========================================================================
    // EXPERIMENT 5 — Conclusion candidates
    // =========================================================================

    /**
     * EXP 5 — Best readable format if chained-no-RESET works (Exp 1C result is 1 line).
     * Ideal log line: grey ts | colored label | black bg message | grey caller
     */
    @Test(priority = 50, description = "Exp 5 — ideal multi-color format (if Exp 1C passed)")
    public void exp5_idealIfChainedWorks() {
        out("LABEL: [ideal chained-color format — readable AND (hopefully) single-line]");

        // INFO
        out(BG_GREY_100 + FG_BLACK + ts() + " " + BG_WHITE + FG_BLACK + "[INFO] " + BG_BLACK + FG_BRIGHT_WHITE + "Login successful  " + BG_GREY_100 + FG_BRIGHT_CYAN + "MyPage.login ← TestRunner.run" + ANSI_RESET);
        // WARN
        out(BG_GREY_100 + FG_BLACK + ts() + " " + BG_YELLOW + FG_BLACK + "[WARN] " + BG_BLACK + FG_BRIGHT_YELLOW + "Session expiring soon  " + BG_GREY_100 + FG_BRIGHT_CYAN + "MyPage.login ← TestRunner.run" + ANSI_RESET);
        // ERROR
        out(BG_GREY_100 + FG_BLACK + ts() + " " + BG_DARKER_RED + FG_BRIGHT_WHITE + "[ERROR] " + BG_BLACK + FG_BRIGHT_RED + "Element not found  " + BG_GREY_100 + FG_BRIGHT_CYAN + "MyPage.login ← TestRunner.run" + ANSI_RESET);
        // CLICK
        out(BG_GREY_100 + FG_BLACK + ts() + " " + BG_MAGENTA + FG_BRIGHT_WHITE + "[CLICK [>]] " + BG_BLACK + FG_BRIGHT_WHITE + "Clicked Submit  " + BG_GREY_100 + FG_BRIGHT_CYAN + "MyPage.login ← TestRunner.run" + ANSI_RESET);
        // SUCCESS
        out(BG_GREY_100 + FG_BLACK + ts() + " " + BG_GREEN + FG_BLACK + "[SUCCESS [+]] " + BG_BLACK + FG_BRIGHT_GREEN + "All assertions passed  " + BG_GREY_100 + FG_BRIGHT_CYAN + "MyPage.login ← TestRunner.run" + ANSI_RESET);
    }
}

