package core.resolvers.locator.sync;

import core.resolvers.locator.sync.LocatorTemplateGenerator.LocatorKey;
import org.testng.annotations.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.testng.Assert.*;

public class LocatorTemplateGeneratorTest {

    private final LocatorTemplateGenerator generator = new LocatorTemplateGenerator();

    @Test
    public void singleRoleInterface_producesRoleSuffixedKey() {
        List<LocatorKey> keys = generator.generateKeys(SyncTestFixturePage.class);
        List<String> flat = keys.stream().map(LocatorKey::key).toList();

        assertTrue(flat.contains("SyncTestFixturePage.Inputs.USERNAME.INPUT"),
            "Expected INPUT role key for Typeable constant");
        assertTrue(flat.contains("SyncTestFixturePage.Actions.SUBMIT.TRIGGER"),
            "Expected TRIGGER role key for Clickable constant");
        assertTrue(flat.contains("SyncTestFixturePage.Labels.ERROR_MSG.TEXT"),
            "Expected TEXT role key for ReadOnly constant");
    }

    @Test
    public void multiRoleInterface_producesTwoKeysPerConstant() {
        List<LocatorKey> keys = generator.generateKeys(SyncTestFixturePage.class);
        List<String> flat = keys.stream().map(LocatorKey::key).toList();

        // Selectable → TRIGGER + LIST
        assertTrue(flat.contains("SyncTestFixturePage.Dropdowns.COUNTRY.TRIGGER"),
            "Expected TRIGGER key for Selectable");
        assertTrue(flat.contains("SyncTestFixturePage.Dropdowns.COUNTRY.LIST"),
            "Expected LIST key for Selectable");
        assertTrue(flat.contains("SyncTestFixturePage.Dropdowns.STATE.TRIGGER"));
        assertTrue(flat.contains("SyncTestFixturePage.Dropdowns.STATE.LIST"));
    }

    @Test
    public void enumSimpleNameMatchesSection() {
        List<LocatorKey> keys = generator.generateKeys(SyncTestFixturePage.class);
        for (LocatorKey lk : keys) {
            assertTrue(lk.key().startsWith("SyncTestFixturePage." + lk.enumSimpleName() + "."),
                "Key prefix must match enum simple name: " + lk);
        }
    }

    @Test
    public void allConstantsPresent() {
        List<LocatorKey> keys = generator.generateKeys(SyncTestFixturePage.class);
        List<String> flat = keys.stream().map(LocatorKey::key).toList();

        // Inputs: 2 constants × 1 role = 2 keys
        assertEquals(flat.stream().filter(k -> k.contains(".Inputs.")).count(), 2L);
        // Actions: 2 constants × 1 role = 2 keys
        assertEquals(flat.stream().filter(k -> k.contains(".Actions.")).count(), 2L);
        // Labels: 1 constant × 1 role = 1 key
        assertEquals(flat.stream().filter(k -> k.contains(".Labels.")).count(), 1L);
        // Dropdowns: 2 constants × 2 roles = 4 keys
        assertEquals(flat.stream().filter(k -> k.contains(".Dropdowns.")).count(), 4L);
    }

    @Test
    public void emptyPage_producesNoKeys() {
        interface EmptyPage {}
        List<LocatorKey> keys = generator.generateKeys(EmptyPage.class);
        assertTrue(keys.isEmpty());
    }

    // ── Nested interface tests ────────────────────────────────────────────────

    @Test
    public void nestedInterfaceEnum_isIncludedInKeys() {
        List<LocatorKey> keys = generator.generateKeys(NestedSyncTestFixturePage.class);
        List<String> flat = keys.stream().map(LocatorKey::key).toList();

        assertTrue(flat.contains("LoginForm.Fields.USERNAME_FIELD.INPUT"),
            "Nested interface enum constant must appear in generated keys");
        assertTrue(flat.contains("LoginForm.Fields.PASSWORD_FIELD.INPUT"));
        assertTrue(flat.contains("LoginForm.Buttons.LOGIN_BUTTON.TRIGGER"));
        assertTrue(flat.contains("ErrorBanner.Labels.ERROR_MSG.TEXT"));
        assertTrue(flat.contains("ErrorBanner.Buttons.DISMISS_BUTTON.TRIGGER"));
    }

    @Test
    public void nestedInterfaceEnum_usesInterfacePrefixNotPagePrefix() {
        List<LocatorKey> keys = generator.generateKeys(NestedSyncTestFixturePage.class);
        List<String> flat = keys.stream().map(LocatorKey::key).toList();

        // Keys must use the immediate enclosing interface name, not the top-level page name
        assertTrue(flat.stream().noneMatch(k -> k.startsWith("NestedSyncTestFixturePage.Fields.")),
            "Nested enum keys must not carry the top-level page name as prefix");
        assertTrue(flat.stream().anyMatch(k -> k.startsWith("LoginForm.Fields.")),
            "Nested enum keys must use the immediate enclosing interface name");
    }

    @Test
    public void directAndNestedEnums_allPresent() {
        List<LocatorKey> keys = generator.generateKeys(NestedSyncTestFixturePage.class);
        List<String> flat = keys.stream().map(LocatorKey::key).toList();

        // Direct enum child of page
        assertTrue(flat.contains("NestedSyncTestFixturePage.PageActions.FORGOT_PASSWORD_LINK.TRIGGER"),
            "Direct enum child of page must use the page simple name as prefix");

        // LoginForm nested enums: 2 + 1 constants = 3 keys
        assertEquals(flat.stream().filter(k -> k.contains(".Fields.")).count(), 2L);
        assertEquals(flat.stream().filter(k -> k.startsWith("LoginForm.Buttons.")).count(), 1L);

        // ErrorBanner nested enums: 1 + 1 constants = 2 keys
        assertEquals(flat.stream().filter(k -> k.startsWith("ErrorBanner.Labels.")).count(), 1L);
        assertEquals(flat.stream().filter(k -> k.startsWith("ErrorBanner.Buttons.")).count(), 1L);
    }
}
