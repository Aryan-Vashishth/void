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
}
