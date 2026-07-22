package core.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

/**
 * Architecture ratchet encoding boundaries that are already true as of ADR-021 (Phase 0.2).
 *
 * <p>Rules are tightened by subsequent phases as each boundary is won. See
 * {@code docs/contributing/architecture-rules.md} for the tightening protocol.</p>
 *
 * <h3>Axis: Domain neutrality (ADR-021)</h3>
 * <p>Engine-agnostic kernel layers must not import Selenium types. The packages listed
 * here are verified clean at the start of the runtime-redesign initiative; future
 * phases expand this list as more packages are cleaned.</p>
 *
 * <h3>Axis: Engine neutrality (ADR-018, ADR-019)</h3>
 * <p>{@code core.runtime} must not import {@code WebDriver} or {@code DriverContext}.
 * {@code core.engine.LocatorDescriptor} must not import {@code org.openqa.selenium.By}.</p>
 */
public class KernelBoundaryRulesTest {

    private static final String SELENIUM = "org.openqa.selenium..";

    private JavaClasses allClasses;

    @BeforeClass
    public void importClasses() {
        allClasses = new ClassFileImporter()
                .importPackages("core", "elements", "dsl");
    }

    // ─────────────────────────────────────────────────────────────────────
    // Logging layer -- Selenium-free
    // ─────────────────────────────────────────────────────────────────────

    @Test(description = "core.logging has no Selenium dependency")
    public void loggingIsSeleniumFree() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("core.logging..")
                .should().dependOnClassesThat().resideInAPackage(SELENIUM)
                .because("core.logging is a domain-neutral utility; Selenium must never enter it. ADR-021.");

        rule.check(allClasses);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Flow layer -- Selenium-free
    // ─────────────────────────────────────────────────────────────────────

    @Test(description = "core.flow has no Selenium dependency")
    public void flowIsSeleniumFree() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("core.flow..")
                .should().dependOnClassesThat().resideInAPackage(SELENIUM)
                .because("Flow is a domain-neutral interaction collection. ADR-021 kernel membership.");

        rule.check(allClasses);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Actions layer -- Selenium-free
    // ─────────────────────────────────────────────────────────────────────

    @Test(description = "core.actions has no Selenium dependency")
    public void actionsAreSeleniumFree() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("core.actions..")
                .should().dependOnClassesThat().resideInAPackage(SELENIUM)
                .because(
                    "Action descriptions are domain-neutral kernel types (ADR-021). " +
                    "Execution is delegated to UIEngine, not called via WebDriver directly.");

        rule.check(allClasses);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Elements API -- Selenium-free
    // ─────────────────────────────────────────────────────────────────────

    @Test(description = "elements.api and elements.meta have no Selenium dependency")
    public void elementsApiIsSeleniumFree() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("elements..")
                .should().dependOnClassesThat().resideInAPackage(SELENIUM)
                .because(
                    "elements.api defines capability interfaces; elements.meta defines " +
                    "structural utilities. Both are domain-neutral vocabulary. ADR-021.");

        rule.check(allClasses);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Runtime facade -- no WebDriver/DriverContext fields (ADR-018)
    //
    // Note: VOID.getDriver() is a @Deprecated(forRemoval=true) escape hatch
    // whose return type is WebDriver. That method-level dependency is a known
    // tracked bridge, scheduled for deletion in I9.3. This check locks the
    // stronger form of coupling: a WebDriver or DriverContext stored as a field.
    // ─────────────────────────────────────────────────────────────────────

    @Test(description = "core.runtime does not store WebDriver as a field")
    public void runtimeDoesNotDeclareWebDriverField() {
        ArchRule rule = noFields()
                .that().areDeclaredInClassesThat().resideInAPackage("core.runtime..")
                .should().haveRawType("org.openqa.selenium.WebDriver")
                .because(
                    "VOID and VOIDBuilder must not store WebDriver as a field. " +
                    "The deprecated getDriver() return type is tracked separately (I9.3). ADR-018 + ADR-021.");

        rule.check(allClasses);
    }

    @Test(description = "core.runtime does not store DriverContext as a field")
    public void runtimeDoesNotDeclareDriverContextField() {
        ArchRule rule = noFields()
                .that().areDeclaredInClassesThat().resideInAPackage("core.runtime..")
                .should().haveRawType("core.driver.DriverContext")
                .because("VOID must not hold a DriverContext field. ADR-018 + ADR-021.");

        rule.check(allClasses);
    }

    // ─────────────────────────────────────────────────────────────────────
    // LocatorDescriptor -- Selenium-free (ADR-019)
    // ─────────────────────────────────────────────────────────────────────

    @Test(description = "LocatorDescriptor has no org.openqa.selenium.By dependency")
    public void locatorDescriptorIsSeleniumFree() {
        ArchRule rule = noClasses()
                .that().haveFullyQualifiedName("core.engine.LocatorDescriptor")
                .should().dependOnClassesThat().resideInAPackage(SELENIUM)
                .because("LocatorDescriptor must not import Selenium types. ADR-019 + ADR-021.");

        rule.check(allClasses);
    }
}
