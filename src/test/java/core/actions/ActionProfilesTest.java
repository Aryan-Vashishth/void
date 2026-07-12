package core.actions;

import core.engine.LocatorDescriptor;
import core.engine.LocatorStrategy;
import core.engine.UIEngine;
import core.interactions.hooks.After;
import core.interactions.hooks.Before;
import core.utils.ConfigLoader;
import elements.api.capability.Clickable;
import elements.api.capability.Typeable;
import elements.meta.ElementRole;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class ActionProfilesTest {

    private static final LocatorDescriptor STUB_DESCRIPTOR =
            new LocatorDescriptor("//*[@id='stub']", LocatorStrategy.XPATH);

    private static final Clickable CLICKABLE = new Clickable() {
        @Override
        public String getTriggerLocator() { return "CLICK_KEY"; }

        @Override
        public String getExternalFileName() { return "stub.json"; }

        @Override
        public Object[] getArgs() { return new Object[0]; }
    };

    private static final Typeable TYPEABLE = new Typeable() {
        @Override
        public String getInputLocator() { return "INPUT_KEY"; }

        @Override
        public String getExternalFileName() { return "stub.json"; }

        @Override
        public Object[] getArgs() { return new Object[0]; }
    };

    @AfterMethod
    public void clearActiveConfig() {
        ConfigLoader.setActive(new Properties());
    }

    @Test
    public void safely_onTypeable_executesProfileHooksAroundAction() {
        List<String> calls = new ArrayList<>();
        UIEngine engine = buildEngine(calls);

        TYPEABLE.type("admin").safely().perform(engine);

        assertTrue(calls.contains("clear"));
        assertTrue(calls.contains("waitForVisible"));
        assertTrue(calls.contains("type"));
        assertTrue(calls.contains("highlight"));

        int clearIndex = calls.indexOf("clear");
        int waitVisibleIndex = calls.indexOf("waitForVisible");
        int typeIndex = calls.indexOf("type");
        int highlightIndex = calls.lastIndexOf("highlight");

        assertTrue(clearIndex < typeIndex, "SAFE before-hook clear must run before type");
        assertTrue(waitVisibleIndex < typeIndex, "SAFE before-hook wait must run before type");
        assertTrue(typeIndex < highlightIndex, "SAFE after-hook highlight must run after type");
    }

    @Test
    public void configuredDefaultProfile_debug_isAppliedToElementActionsOf() {
        Properties props = new Properties();
        props.setProperty(ActionProfiles.DEFAULT_PROFILE_KEY, "DEBUG");
        ConfigLoader.setActive(props);

        List<String> calls = new ArrayList<>();
        UIEngine engine = buildEngine(calls);

        ElementActions.of(CLICKABLE, ElementRole.TRIGGER, (e, d) -> e.click(d)).perform(engine);

        assertTrue(calls.contains("click"), "Core click should still execute");
        assertTrue(calls.contains("highlight"), "Configured DEBUG profile should add highlight hooks");
    }

    @Test
    public void fromName_safe_fallsBackToRaw() {
        assertSame(Profiles.fromName("SAFE"), Profiles.RAW);
    }

    @Test
    public void fromName_reliable_fallsBackToRaw() {
        assertSame(Profiles.fromName("RELIABLE"), Profiles.RAW);
    }

    private UIEngine buildEngine(List<String> calls) {
        InvocationHandler handler = (proxy, method, args) -> {
            if ("resolve".equals(method.getName())) {
                calls.add("resolve");
                return STUB_DESCRIPTOR;
            }
            calls.add(method.getName());
            return defaultFor(method.getReturnType());
        };

        return (UIEngine) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{UIEngine.class},
                handler);
    }

    @Test
    public void debugProfile_hasExpectedHookLists() {
        assertEquals(Profiles.DEBUG.before(), List.of(Before.LOG_INTENT, Before.HIGHLIGHT_ELEMENT));
        assertEquals(Profiles.DEBUG.after(), List.of(After.HIGHLIGHT_ELEMENT));
    }

    @Test
    public void debug_onTypeable_executesHighlightAroundAction() {
        List<String> calls = new ArrayList<>();
        UIEngine engine = buildEngine(calls);

        TYPEABLE.type("test").debug().perform(engine);

        assertTrue(calls.contains("type"), "Core type must execute");
        assertTrue(calls.contains("highlight"), "HIGHLIGHT_ELEMENT hook must run");
        assertTrue(calls.indexOf("highlight") < calls.indexOf("type"),
                "Before highlight must precede type");
        assertTrue(calls.lastIndexOf("highlight") > calls.indexOf("type"),
                "After highlight must follow type");
    }

    @Test
    public void rawProfile_hasNoHooks() {
        assertEquals(Profiles.RAW.before(), List.of());
        assertEquals(Profiles.RAW.after(), List.of());
    }

    @Test
    public void raw_onClickable_skipsAllHooks() {
        List<String> calls = new ArrayList<>();
        UIEngine engine = buildEngine(calls);

        CLICKABLE.click().raw().perform(engine);

        assertTrue(calls.contains("click"), "Core click must execute");
        assertFalse(calls.contains("highlight"), "raw() must not add any hooks");
        assertFalse(calls.contains("waitForClickable"), "raw() must not add any hooks");
    }

    @Test
    public void usingCustomProfile_appliesHooksFromCustomProfile() {
        List<String> calls = new ArrayList<>();
        UIEngine engine = buildEngine(calls);

        ActionProfile custom = ActionProfile.builder()
                .before(Before.HIGHLIGHT_ELEMENT)
                .after(After.HIGHLIGHT_ELEMENT)
                .build();

        TYPEABLE.type("test").using(custom).perform(engine);

        assertTrue(calls.contains("type"), "Core type must execute");
        assertTrue(calls.contains("highlight"), "Custom profile hooks must run");
        assertTrue(calls.indexOf("highlight") < calls.indexOf("type"),
                "Custom before-hook must precede the type action");
        assertTrue(calls.lastIndexOf("highlight") > calls.indexOf("type"),
                "Custom after-hook must follow the type action");
    }

    private static Object defaultFor(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0f;
        if (type == double.class) return 0d;
        if (type == char.class) return '\0';
        return null;
    }
}

