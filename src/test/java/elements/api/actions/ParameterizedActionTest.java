package domain.automation.web.vocabulary.actions;

import core.engine.EngineConfig;
import domain.automation.web.locator.LocatorDescriptor;
import domain.automation.web.locator.LocatorStrategy;
import domain.automation.web.engine.UIEngine;
import domain.automation.web.vocabulary.capability.ParameterizedClickable;
import domain.automation.web.vocabulary.element.UIElement;
import domain.automation.web.vocabulary.role.ElementRole;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.List;

import static org.testng.Assert.*;

/**
 * Unit tests for {@link ParameterizedClickAction} and the {@link ParameterizedClickable} capability.
 *
 * <p>Uses a recording engine stub that captures the {@code Object[]} args passed to
 * {@link UIEngine#resolve(UIElement, ElementRole, Object...)} so tests can assert
 * args forwarding without opening a browser.</p>
 */
public class ParameterizedActionTest {

    // ── Recording engine ──────────────────────────────────────────────────

    static class RecordingEngine implements UIEngine {
        Object[] capturedArgs;

        @Override
        public LocatorDescriptor resolve(UIElement e, ElementRole r, Object... args) {
            this.capturedArgs = args;
            return stub();
        }

        @Override public void click(LocatorDescriptor d) {}
        @Override public LocatorDescriptor resolve(String f, String k, Object... a) { return stub(); }

        private static LocatorDescriptor stub() {
            return new LocatorDescriptor("//stub", LocatorStrategy.XPATH);
        }

        @Override public void initialize(EngineConfig c) {}
        @Override public void shutdown() {}
        @Override public void navigateTo(String u) {}
        @Override public String getCurrentUrl() { return ""; }
        @Override public String getTitle() { return ""; }
        @Override public void refresh() {}
        @Override public String getAttribute(LocatorDescriptor d, String a) { return null; }
        @Override public boolean isVisible(LocatorDescriptor d) { return true; }
        @Override public boolean isEnabled(LocatorDescriptor d) { return true; }
        @Override public boolean isSelected(LocatorDescriptor d) { return false; }
        @Override public int getElementCount(LocatorDescriptor d) { return 0; }
        @Override public List<String> getAllTexts(LocatorDescriptor d) { return List.of(); }
        @Override public String getTextWithAttributeFallback(LocatorDescriptor d, String e, String... a) { return ""; }
        @Override public String getText(LocatorDescriptor d) { return ""; }
        @Override public void hover(LocatorDescriptor d) {}
        @Override public void type(LocatorDescriptor d, String t) {}
        @Override public void clear(LocatorDescriptor d) {}
        @Override public void appendType(LocatorDescriptor d, String t) {}
        @Override public void sendKey(LocatorDescriptor d, String k) {}
        @Override public void selectByVisibleText(LocatorDescriptor d, String t) {}
        @Override public void selectByValue(LocatorDescriptor d, String v) {}
        @Override public void uploadFile(LocatorDescriptor d, String p) {}
        @Override public void waitForOverlay(Duration t) {}
        @Override public void waitForVisible(LocatorDescriptor d, Duration t) {}
        @Override public void waitForClickable(LocatorDescriptor d, Duration t) {}
        @Override public void waitForAbsence(LocatorDescriptor d, Duration t) {}
        @Override public void waitForPresence(LocatorDescriptor d, Duration t) {}
        @Override public boolean getCheckboxState(LocatorDescriptor d) { return false; }
        @Override public Object executeScript(String s, Object... a) { return null; }
        @Override public void scrollTo(LocatorDescriptor d) {}
        @Override public byte[] takeScreenshot() { return new byte[0]; }
        @Override public void highlight(LocatorDescriptor d, String c) {}
        @Override public void switchToFrame(LocatorDescriptor d) {}
        @Override public void switchToDefaultContent() {}
        @Override public void sendKeys(CharSequence... keys) {}
        @Override public Object getNativeDriver() { return null; }
        @Override public String getEngineName() { return "test"; }
    }

    // ── Element stub ──────────────────────────────────────────────────────

    private static ParameterizedClickable stubElement() {
        return new ParameterizedClickable() {
            @Override public String getTriggerLocator()   { return "trigger"; }
            @Override public String getExternalFileName() { return "stub.json"; }
            @Override public Object[] getArgs()           { return new Object[0]; }
        };
    }

    // ── Tests ─────────────────────────────────────────────────────────────

    @Test
    public void withArgs_singleArg_passedToEngineResolve() {
        RecordingEngine engine = new RecordingEngine();
        stubElement().click().withArgs("sauce-labs-backpack").perform(engine);
        assertEquals(engine.capturedArgs, new Object[]{"sauce-labs-backpack"});
    }

    @Test
    public void withArgs_multipleArgs_allPassedToEngineResolve() {
        RecordingEngine engine = new RecordingEngine();
        stubElement().click().withArgs("arg1", "arg2").perform(engine);
        assertEquals(engine.capturedArgs, new Object[]{"arg1", "arg2"});
    }

    @Test
    public void withArgs_notCalled_emptyArgsForwarded() {
        RecordingEngine engine = new RecordingEngine();
        stubElement().click().perform(engine);
        assertEquals(engine.capturedArgs.length, 0);
    }

    @Test
    public void withArgs_calledWithNull_treatedAsNoOverride() {
        RecordingEngine engine = new RecordingEngine();
        stubElement().click().withArgs((Object[]) null).perform(engine);
        assertEquals(engine.capturedArgs.length, 0);
    }

    @Test(description = "Second call silently overwrites first; do not reuse action instances")
    public void withArgs_calledTwice_secondCallOverwritesFirst() {
        RecordingEngine engine = new RecordingEngine();
        stubElement().click().withArgs("first").withArgs("second").perform(engine);
        assertEquals(engine.capturedArgs, new Object[]{"second"});
    }

    @Test
    public void withArgs_returnsItselfForChaining() {
        ParameterizedClickAction action = stubElement().click();
        assertSame(action.withArgs("x"), action);
    }
}
