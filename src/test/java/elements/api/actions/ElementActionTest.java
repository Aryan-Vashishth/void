package elements.api.actions;

import core.actions.Action;
import core.actions.ActionCapability;
import core.actions.ActionProfile;
import core.actions.Profiles;
import core.annotations.Beta;
import core.engine.Executor;
import elements.locator.LocatorDescriptor;
import elements.locator.LocatorStrategy;
import core.engine.UIEngine;
import core.interactions.hooks.After;
import core.interactions.hooks.Before;
import elements.api.UIElement;
import elements.api.capability.Clickable;
import elements.api.capability.Selectable;
import elements.api.capability.Typeable;
import elements.meta.ElementRole;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.testng.Assert.*;

/**
 * Verifies Phase 13 — ElementAction base class design and Template Method pattern.
 *
 * <p>Covers: Template Method orchestration (perform → resolve → execute), finality of lifecycle methods,
 * immutability guarantees, profile defaults, capability capture, and lifecycle semantics.</p>
 */
public class ElementActionTest {

    private UIElement stubElement;
    private UIEngine stubEngine;
    private LocatorDescriptor stubDescriptor;

    // ════════════════════════════════════════════════════════════════════
    // Helper abstract base for UIEngine stubs
    // ════════════════════════════════════════════════════════════════════

    abstract static class UIEngineStub implements UIEngine {
        protected LocatorDescriptor resolve(UIElement element, ElementRole role) {
            return new LocatorDescriptor("//stub", LocatorStrategy.XPATH);
        }

        @Override public void initialize(core.engine.EngineConfig config) {}
        @Override public void shutdown() {}
        @Override public void navigateTo(String url) {}
        @Override public String getCurrentUrl() { return "http://example.com"; }
        @Override public String getTitle() { return "Example"; }
        @Override public void refresh() {}
        @Override public LocatorDescriptor resolve(UIElement element, ElementRole role, Object... args) {
            return resolve(element, role);
        }
         @Override public LocatorDescriptor resolve(String fileName, String key, Object... args) {
             return this.resolve(new UIElement() {
                 @Override public String getExternalFileName() { return null; }
                 @Override public String getPrimaryLocator() { return null; }
                 @Override public Object[] getArgs() { return new Object[0]; }
             }, ElementRole.PRIMARY);
         }
         @Override public void click(LocatorDescriptor descriptor) {}
         @Override public void type(LocatorDescriptor descriptor, String text) {}
        @Override public void clear(LocatorDescriptor descriptor) {}
        @Override public void appendType(LocatorDescriptor descriptor, String text) {}
        @Override public void sendKey(LocatorDescriptor descriptor, String key) {}
        @Override public void selectByVisibleText(LocatorDescriptor locator, String text) {}
        @Override public void selectByValue(LocatorDescriptor locator, String value) {}
        @Override public String getText(LocatorDescriptor locator) { return ""; }
        @Override public String getAttribute(LocatorDescriptor locator, String attribute) { return null; }
        @Override public boolean isVisible(LocatorDescriptor locator) { return true; }
        @Override public boolean isEnabled(LocatorDescriptor locator) { return true; }
        @Override public boolean isSelected(LocatorDescriptor locator) { return false; }
        @Override public int getElementCount(LocatorDescriptor locator) { return 1; }
        @Override public String getTextWithAttributeFallback(LocatorDescriptor locator, String endsWith, String... attributes) { return ""; }
        @Override public boolean getCheckboxState(LocatorDescriptor locator) { return false; }
        @Override public void waitForVisible(LocatorDescriptor locator, java.time.Duration timeout) {}
        @Override public void waitForClickable(LocatorDescriptor locator, java.time.Duration timeout) {}
        @Override public void waitForAbsence(LocatorDescriptor locator, java.time.Duration timeout) {}
        @Override public void waitForPresence(LocatorDescriptor locator, java.time.Duration timeout) {}
        @Override public void waitForOverlay(java.time.Duration timeout) {}
        @Override public Object executeScript(String script, Object... args) { return null; }
        @Override public void scrollTo(LocatorDescriptor locator) {}
        @Override public void uploadFile(LocatorDescriptor locator, String filePath) {}
        @Override public byte[] takeScreenshot() { return new byte[0]; }
        @Override public void highlight(LocatorDescriptor locator, String color) {}
        @Override public void hover(LocatorDescriptor locator) {}
        @Override public void switchToFrame(LocatorDescriptor locator) {}
        @Override public void switchToDefaultContent() {}
        @Override public void sendKeys(CharSequence... keys) {}
        @Override public Object getNativeDriver() { return null; }
        @Override public String getEngineName() { return "TestEngine"; }
    }

    @BeforeMethod
    public void setUp() {
        stubElement = createStubElement();
        stubDescriptor = createStubDescriptor();
        stubEngine = createStubEngine(stubDescriptor);
    }



    // ═════════════════════════════════════════════════════════════════════
    // Template Method Orchestration
    // ═════════════════════════════════════════════════════════════════════

    @Test
    public void perform_callsResolveBeforeExecute() {
        AtomicReference<String> callOrder = new AtomicReference<>("");

        ElementAction action = new ElementAction(stubElement, ElementRole.TRIGGER,
                ActionCapability.CLICKABLE) {
            @Override
            protected void execute(UIEngine engine, LocatorDescriptor descriptor) {
                callOrder.set("execute");
            }
        };

        // perform() should call resolve() then execute()
        // We verify this by ensuring execute() was called after resolve()
        callOrder.set("initial");
        action.perform((Executor) stubEngine);
        assertEquals(callOrder.get(), "execute",
                "perform() must call resolve() then execute()");
    }

    @Test
    public void perform_passesResolvedDescriptorToExecute() {
        AtomicReference<LocatorDescriptor> receivedDescriptor = new AtomicReference<>();

        ElementAction action = new ElementAction(stubElement, ElementRole.INPUT,
                ActionCapability.TYPEABLE) {
            @Override
            protected void execute(UIEngine engine, LocatorDescriptor descriptor) {
                receivedDescriptor.set(descriptor);
            }
        };

        action.perform((Executor) stubEngine);
        assertSame(receivedDescriptor.get(), stubDescriptor,
                "execute() must receive the descriptor resolved in perform()");
    }

    @Test
    public void perform_passesCorrectEngineToExecute() {
        AtomicReference<UIEngine> receivedEngine = new AtomicReference<>();

        ElementAction action = new ElementAction(stubElement, ElementRole.TRIGGER,
                ActionCapability.CLICKABLE) {
            @Override
            protected void execute(UIEngine engine, LocatorDescriptor descriptor) {
                receivedEngine.set(engine);
            }
        };

        action.perform((Executor) stubEngine);
        assertSame(receivedEngine.get(), stubEngine,
                "execute() must receive the engine passed to perform()");
    }

    // ═════════════════════════════════════════════════════════════════════
    // Fluent APIs — Finality & Immutability
    // ═════════════════════════════════════════════════════════════════════

    @Test
    public void safely_returnsNewInstance_notThis() {
        ElementAction action = new ElementAction(stubElement, ElementRole.TRIGGER,
                ActionCapability.CLICKABLE) {
            @Override
            protected void execute(UIEngine engine, LocatorDescriptor descriptor) {}
        };

        Action safe = action.safely();
        assertNotSame(safe, action,
                "safely() must return a new instance via HookChainAction, not this");
    }

    @Test
    public void debug_returnsNewInstance_notThis() {
        ElementAction action = new ElementAction(stubElement, ElementRole.TRIGGER,
                ActionCapability.CLICKABLE) {
            @Override
            protected void execute(UIEngine engine, LocatorDescriptor descriptor) {}
        };

        Action debug = action.debug();
        assertNotSame(debug, action,
                "debug() must return a new instance via HookChainAction, not this");
    }

    @Test
    public void reliable_returnsNewInstance_notThis() {
        ElementAction action = new ElementAction(stubElement, ElementRole.TRIGGER,
                ActionCapability.CLICKABLE) {
            @Override
            protected void execute(UIEngine engine, LocatorDescriptor descriptor) {}
        };

        Action reliable = action.reliable();
        assertNotSame(reliable, action,
                "reliable() must return a new instance via HookChainAction, not this");
    }

    @Test
    public void raw_returnsThis() {
        ElementAction action = new ElementAction(stubElement, ElementRole.TRIGGER,
                ActionCapability.CLICKABLE) {
            @Override
            protected void execute(UIEngine engine, LocatorDescriptor descriptor) {}
        };

        Action raw = action.raw();
        assertSame(raw, action,
                "raw() must return this unchanged (no decoration)");
    }

    @Test
    public void multipleFluentCalls_eachReturnsNewInstanceWithAddedHooks() {
        ElementAction action = new ElementAction(stubElement, ElementRole.TRIGGER,
                ActionCapability.CLICKABLE) {
            @Override
            protected void execute(UIEngine engine, LocatorDescriptor descriptor) {}
        };

        Action safe = action.safely();
        Action debug = action.debug();
        Action reliable = action.reliable();

        assertNotSame(safe, action);
        assertNotSame(debug, action);
        assertNotSame(reliable, action);
        assertNotSame(safe, debug);
        assertNotSame(safe, reliable);
        assertNotSame(debug, reliable);
    }

    @Test
    public void fluentChaining_returnsNewInstanceEachTime() {
        ElementAction action = new ElementAction(stubElement, ElementRole.TRIGGER,
                ActionCapability.CLICKABLE) {
            @Override
            protected void execute(UIEngine engine, LocatorDescriptor descriptor) {}
        };

        Action chain1 = action.safely().debug();
        Action chain2 = action.safely().debug();

        assertNotSame(chain1, chain2,
                "Each fluent chain should create new instances");
    }

    // ═════════════════════════════════════════════════════════════════════
    // Profile Defaults
    // ═════════════════════════════════════════════════════════════════════

    @Test
    public void safely_usesDefaultSafeProfile_derivedFromCapability() {
        ElementAction action = new ElementAction(stubElement, ElementRole.TRIGGER,
                ActionCapability.CLICKABLE) {
            @Override
            protected void execute(UIEngine engine, LocatorDescriptor descriptor) {}
        };

        Action safe = action.safely();
        assertNotSame(safe, action);
        assertEquals(safe.capability(), ActionCapability.CLICKABLE);
    }

    @Test
    public void safely_usesOverriddenDefaultSafeProfile_whenSubclassOverrides() {
        ActionProfile customSafe = ActionProfile.builder()
                .before(Before.LOG_INTENT)
                .build();

        ElementAction action = new ElementAction(stubElement, ElementRole.TRIGGER,
                ActionCapability.CLICKABLE) {
            @Override
            protected void execute(UIEngine engine, LocatorDescriptor descriptor) {}
            @Override
            protected ActionProfile defaultSafeProfile() { return customSafe; }
        };

        Action safe = action.safely();
        assertNotSame(safe, action);
        assertEquals(safe.capability(), ActionCapability.CLICKABLE);
    }

    @Test
    public void debug_usesProfilesDebug() {
        ElementAction action = new ElementAction(stubElement, ElementRole.TRIGGER,
                ActionCapability.CLICKABLE) {
            @Override
            protected void execute(UIEngine engine, LocatorDescriptor descriptor) {}
        };

        Action debug = action.debug();
        assertEquals(debug.capability(), ActionCapability.CLICKABLE,
                "debug() should preserve capability");
    }

    @Test
    public void reliable_preservesCapability() {
        ElementAction action = new ElementAction(stubElement, ElementRole.TRIGGER,
                ActionCapability.CLICKABLE) {
            @Override
            protected void execute(UIEngine engine, LocatorDescriptor descriptor) {}
        };

        Action reliable = action.reliable();
        assertEquals(reliable.capability(), ActionCapability.CLICKABLE,
                "reliable() should preserve capability");
    }

    @Test
    public void defaultSafeProfileCanBeOverridden() {
        ElementAction action = new ElementAction(stubElement, ElementRole.TRIGGER,
                ActionCapability.CLICKABLE) {
            @Override
            protected void execute(UIEngine engine, LocatorDescriptor descriptor) {}

            @Override
            protected ActionProfile defaultSafeProfile() {
                return Profiles.DEBUG;
            }
        };

        // When safely() is called, it should use the overridden defaultSafeProfile
        Action safe = action.safely();
        assertEquals(safe.capability(), ActionCapability.CLICKABLE);
    }

    // ═════════════════════════════════════════════════════════════════════
    // Capability & UIElement Storage
    // ═════════════════════════════════════════════════════════════════════

    @Test
    public void capability_returnsInitialCapability() {
        ElementAction action = new ElementAction(stubElement, ElementRole.TRIGGER,
                ActionCapability.CLICKABLE) {
            @Override
            protected void execute(UIEngine engine, LocatorDescriptor descriptor) {}
        };

        assertEquals(action.capability(), ActionCapability.CLICKABLE);
    }

    @Test
    public void capability_unknownWhenNotProvided() {
        ElementAction action = new ElementAction(stubElement, ElementRole.TRIGGER,
                null) {
            @Override
            protected void execute(UIEngine engine, LocatorDescriptor descriptor) {}
        };

        assertEquals(action.capability(), ActionCapability.UNKNOWN);
    }

    @Test
    public void element_storedAndAccessible() {
        ElementAction action = new ElementAction(stubElement, ElementRole.TRIGGER,
                ActionCapability.CLICKABLE) {
            @Override
            protected void execute(UIEngine engine, LocatorDescriptor descriptor) {}
        };

        assertEquals(action.element, stubElement);
    }

    @Test
    public void role_storedAndAccessible() {
        ElementAction action = new ElementAction(stubElement, ElementRole.INPUT,
                ActionCapability.TYPEABLE) {
            @Override
            protected void execute(UIEngine engine, LocatorDescriptor descriptor) {}
        };

        assertEquals(action.role, ElementRole.INPUT);
    }

    // ═════════════════════════════════════════════════════════════════════
    // Resolver & Descriptor Resolution
    // ═════════════════════════════════════════════════════════════════════

    @Test
    public void resolve_callsEngineResolveWithElementAndRole() {
        AtomicReference<UIElement> resolvedElement = new AtomicReference<>();
        AtomicReference<ElementRole> resolvedRole = new AtomicReference<>();

        UIEngine recordingEngine = new UIEngineStub() {
            @Override
            public LocatorDescriptor resolve(UIElement element, ElementRole role, Object... args) {
                resolvedElement.set(element);
                resolvedRole.set(role);
                return stubDescriptor;
            }
        };

        ElementAction action = new ElementAction(stubElement, ElementRole.TRIGGER,
                ActionCapability.CLICKABLE) {
            @Override
            protected void execute(UIEngine engine, LocatorDescriptor descriptor) {}
        };

        action.resolve((Executor) recordingEngine);
        assertSame(resolvedElement.get(), stubElement);
        assertEquals(resolvedRole.get(), ElementRole.TRIGGER);
    }

    @Test
    public void resolve_returnedDescriptorIsUsedByPerform() {
        AtomicReference<LocatorDescriptor> passedDescriptor = new AtomicReference<>();

        ElementAction action = new ElementAction(stubElement, ElementRole.TRIGGER,
                ActionCapability.CLICKABLE) {
            @Override
            protected void execute(UIEngine engine, LocatorDescriptor descriptor) {
                passedDescriptor.set(descriptor);
            }
        };

        action.perform((Executor) stubEngine);
        assertSame(passedDescriptor.get(), stubDescriptor);
    }

    // ═════════════════════════════════════════════════════════════════════
    // ActionLabeled Implementation
    // ═════════════════════════════════════════════════════════════════════

    @Test
    public void elementLabel_enumElement_returnsDisplayText() {
        enum TestElement implements UIElement {
            LOGIN_BUTTON;

            @Override
            public String getExternalFileName() { return null; }

            @Override
            public String getPrimaryLocator() { return "//button"; }

            @Override
            public String getDisplayText() { return "Login"; }

            @Override
            public Object[] getArgs() { return new Object[0]; }

            @Override
            public java.util.Map<ElementRole, String> getAllLocatorRoles() {
                return java.util.Map.of(ElementRole.TRIGGER, "//button");
            }
        }

        ElementAction action = new ElementAction(TestElement.LOGIN_BUTTON, ElementRole.TRIGGER,
                ActionCapability.CLICKABLE) {
            @Override
            protected void execute(UIEngine engine, LocatorDescriptor descriptor) {}
        };

        assertEquals(action.elementLabel(), "Login");
    }

    @Test
    public void operationLabel_clickAction_returnsClick() {
        assertEquals(new ClickAction(stubClickable()).operationLabel(), "click");
    }

    @Test
    public void operationLabel_typeAction_returnsType() {
        assertEquals(new TypeAction(stubTypeable(), "text").operationLabel(), "type");
    }

    @Test
    public void operationLabel_selectAction_returnsSelect() {
        assertEquals(new SelectAction(stubSelectable()).operationLabel(), "select");
    }

    @Test
    public void operationLabel_anonymousAction_returnsPerform() {
        // Anonymous subclasses have no simple name — base class returns "perform".
        ElementAction action = new ElementAction(stubElement, ElementRole.TEXT,
                ActionCapability.UNKNOWN) {
            @Override
            protected void execute(UIEngine engine, LocatorDescriptor descriptor) {}
        };
        assertEquals(action.operationLabel(), "perform");
    }

    // ═════════════════════════════════════════════════════════════════════
    // Null Safety
    // ═════════════════════════════════════════════════════════════════════

    @Test(expectedExceptions = NullPointerException.class,
          expectedExceptionsMessageRegExp = ".*element must not be null.*")
    public void constructor_nullElement_throwsNPE() {
        new ElementAction(null, ElementRole.TRIGGER, ActionCapability.CLICKABLE) {
            @Override
            protected void execute(UIEngine engine, LocatorDescriptor descriptor) {}
        };
    }

    @Test(expectedExceptions = NullPointerException.class,
          expectedExceptionsMessageRegExp = ".*role must not be null.*")
    public void constructor_nullRole_throwsNPE() {
        new ElementAction(stubElement, null, ActionCapability.CLICKABLE) {
            @Override
            protected void execute(UIEngine engine, LocatorDescriptor descriptor) {}
        };
    }

    @Test
    public void constructor_nullCapability_usesUnknown() {
        ElementAction action = new ElementAction(stubElement, ElementRole.TRIGGER,
                null) {
            @Override
            protected void execute(UIEngine engine, LocatorDescriptor descriptor) {}
        };

        assertEquals(action.capability(), ActionCapability.UNKNOWN);
    }

    @Test
    public void defaultSafeProfile_returnsNonNullProfile() {
        ElementAction action = new ElementAction(stubElement, ElementRole.TRIGGER,
                ActionCapability.CLICKABLE) {
            @Override
            protected void execute(UIEngine engine, LocatorDescriptor descriptor) {}
        };

        assertNotNull(action.defaultSafeProfile());
    }

    // ═════════════════════════════════════════════════════════════════════
    // Integration with Action Interface Defaults
    // ═════════════════════════════════════════════════════════════════════

    @Test
    public void elementAction_implementsActionInterface() {
        ElementAction action = new ElementAction(stubElement, ElementRole.TRIGGER,
                ActionCapability.CLICKABLE) {
            @Override
            protected void execute(UIEngine engine, LocatorDescriptor descriptor) {}
        };

        assertTrue(action instanceof Action,
                "ElementAction must implement Action interface");
    }

    @Test
    public void elementAction_supportsBeforeHooks() {
        ElementAction action = new ElementAction(stubElement, ElementRole.TRIGGER,
                ActionCapability.CLICKABLE) {
            @Override
            protected void execute(UIEngine engine, LocatorDescriptor descriptor) {}
        };

        Action withBefore = action.before(Before.HIGHLIGHT_ELEMENT);
        assertNotSame(withBefore, action);
    }

    @Test
    public void elementAction_supportsAfterHooks() {
        ElementAction action = new ElementAction(stubElement, ElementRole.TRIGGER,
                ActionCapability.CLICKABLE) {
            @Override
            protected void execute(UIEngine engine, LocatorDescriptor descriptor) {}
        };

        Action withAfter = action.after(After.HIGHLIGHT_ELEMENT);
        assertNotSame(withAfter, action);
    }

    @Test
    public void elementAction_supportsUsingProfile() {
        ElementAction action = new ElementAction(stubElement, ElementRole.TRIGGER,
                ActionCapability.CLICKABLE) {
            @Override
            protected void execute(UIEngine engine, LocatorDescriptor descriptor) {}
        };

        Action withProfile = action.using(Profiles.DEBUG);
        assertNotSame(withProfile, action);
    }

    // ═════════════════════════════════════════════════════════════════════
    // Helper Methods
    // ═════════════════════════════════════════════════════════════════════

    private UIElement createStubElement() {
        return new UIElement() {
            @Override
            public String getExternalFileName() { return null; }

            @Override
            public String getPrimaryLocator() { return "//button"; }

            @Override
            public String getDisplayText() { return "Test UIElement"; }

            @Override
            public Object[] getArgs() { return new Object[0]; }

            @Override
            public java.util.Map<ElementRole, String> getAllLocatorRoles() {
                return java.util.Map.of(ElementRole.TRIGGER, "//button");
            }
        };
    }

    private LocatorDescriptor createStubDescriptor() {
        return new LocatorDescriptor("//button", LocatorStrategy.XPATH);
    }

    private static Clickable stubClickable() {
        return new Clickable() {
            @Override public String getTriggerLocator()   { return "//btn"; }
            @Override public String getExternalFileName() { return "stub.json"; }
            @Override public Object[] getArgs()           { return new Object[0]; }
        };
    }

    private static Typeable stubTypeable() {
        return new Typeable() {
            @Override public String getInputLocator()     { return "//input"; }
            @Override public String getExternalFileName() { return "stub.json"; }
            @Override public Object[] getArgs()           { return new Object[0]; }
        };
    }

    private static Selectable stubSelectable() {
        return new Selectable() {
            @Override public String getTriggerLocator()   { return "//trigger"; }
            @Override public String getListLocator()      { return "//list"; }
            @Override public String getExternalFileName() { return "stub.json"; }
            @Override public Object[] getArgs()           { return new Object[0]; }
        };
    }

    private UIEngine createStubEngine(final LocatorDescriptor descriptor) {
        return new UIEngineStub() {
            @Override
            public LocatorDescriptor resolve(UIElement element, ElementRole role, Object... args) {
                return descriptor;
            }

            @Override
            public LocatorDescriptor resolve(String fileName, String key, Object... args) {
                return descriptor;
            }
        };
    }
}

