package core.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
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
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
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

    // ─────────────────────────────────────────────────────────────────────
    // Axis: Domain neutrality -- kernel target-neutrality (ADR-021, I1 phase 1.4;
    // tightened I2 phase 2.2 after the kernel/UI action split removed the last
    // UIElement-dependent types from core.actions -- no exemption needed anymore)
    //
    // Kernel action types must reference only core.target.Target, never
    // elements.api.UIElement, elements.meta.ElementRole, or elements.api.capability.
    // ElementAction and its family (the 3 abstract intermediaries, the 17 concrete
    // UI actions, and the ElementActions factory) physically moved to
    // elements.api.actions in I2.2 -- core.actions is now UI-vocabulary-free with
    // no exceptions.
    // ─────────────────────────────────────────────────────────────────────

    private static final String UI_ELEMENT = "elements.api.UIElement";

    @Test(description = "core.actions kernel types depend only on Target, never UIElement")
    public void actionsKernelIsTargetNeutral() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("core.actions")
                .should().dependOnClassesThat().haveFullyQualifiedName(UI_ELEMENT)
                .because(
                    "Action, ActionCapability, ActionProfile, ActionProfiles, Profile, Profiles, " +
                    "and HookChainAction are kernel types (ADR-021) and must not know UI vocabulary. " +
                    "runtime-redesign I1.4, tightened I2.2.");

        rule.check(allClasses);
    }

    @Test(description = "core.actions kernel types have no elements.meta.ElementRole dependency")
    public void actionsKernelDoesNotDependOnElementRole() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("core.actions")
                .should().dependOnClassesThat().haveFullyQualifiedName("elements.meta.ElementRole")
                .because(
                    "ElementRole is UI-domain locator-role vocabulary; the kernel dispatches on " +
                    "Action/Flow only. runtime-redesign I2.2 exit criterion.");

        rule.check(allClasses);
    }

    @Test(description = "core.actions kernel types have no elements.api.capability dependency")
    public void actionsKernelDoesNotDependOnCapabilityInterfaces() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("core.actions")
                .should().dependOnClassesThat().resideInAPackage("elements.api.capability..")
                .because(
                    "Capability interfaces are UI-domain vocabulary; the kernel never references " +
                    "them. runtime-redesign I2.2 exit criterion.");

        rule.check(allClasses);
    }

    @Test(description = "elements.api.actions (the relocated UI action family) has no Selenium dependency")
    public void uiActionsAreSeleniumFree() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("elements.api.actions..")
                .should().dependOnClassesThat().resideInAPackage(SELENIUM)
                .because("Concrete UI actions delegate to UIEngine, never Selenium directly. ADR-021, I2.2.");

        rule.check(allClasses);
    }

    @Test(description = "core.actions.trace has no UIElement dependency")
    public void traceIsTargetNeutral() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("core.actions.trace..")
                .should().dependOnClassesThat().haveFullyQualifiedName(UI_ELEMENT)
                .because("Trace records carry only String labels for observability. ADR-021, I1.4.");

        rule.check(allClasses);
    }

    @Test(description = "core.executor has no UIElement dependency")
    public void executorIsTargetNeutral() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("core.executor..")
                .should().dependOnClassesThat().haveFullyQualifiedName(UI_ELEMENT)
                .because("FlowExecutor iterates and dispatches only; it must not know UI vocabulary. ADR-021, I1.4.");

        rule.check(allClasses);
    }

    @Test(description = "core.flow has no UIElement dependency")
    public void flowIsTargetNeutral() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("core.flow..")
                .should().dependOnClassesThat().haveFullyQualifiedName(UI_ELEMENT)
                .because("Flow composes Actions only; it must not know UI vocabulary. ADR-021, I1.4.");

        rule.check(allClasses);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Axis: Domain neutrality -- kernel hook ownership (ADR-021, I2 phase 2.1)
    //
    // The hook contract (ActionHandler, BeforeActionHandler, AfterActionHandler)
    // moved to core.actions.hooks so the kernel no longer imports through the
    // frozen core.interactions zone (audit D4). ActionProfiles and Profiles are
    // excluded by name (both package-private/public but not assignable to a
    // common test-visible type) -- they still hold capability-specific default
    // hook constants (Before.*/After.*), which are UI-domain content deferred
    // to I2.2's kernel/UI action split, not this phase's move.
    // ─────────────────────────────────────────────────────────────────────

    @Test(description = "core.actions.hooks has no core.interactions dependency")
    public void kernelHooksPackageDoesNotImportLegacyInteractions() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("core.actions.hooks..")
                .should().dependOnClassesThat().resideInAPackage("core.interactions..")
                .because(
                    "The hook contract is kernel-owned as of I2.1; it must not import through " +
                    "the frozen core.interactions zone. ADR-021, audit D4.");

        rule.check(allClasses);
    }

    @Test(description = "core.actions kernel types (excluding ActionProfiles/Profiles' domain-neutral"
            + " default-hook constants) have no core.interactions dependency")
    public void actionsKernelDoesNotImportLegacyInteractions() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("core.actions")
                .and().haveNameNotMatching("core\\.actions\\.ActionProfiles(\\$.*)?")
                .and().haveNameNotMatching("core\\.actions\\.Profiles(\\$.*)?")
                .should().dependOnClassesThat().resideInAPackage("core.interactions..")
                .because(
                    "Action, HookChainAction, ActionCapability, and ActionProfile are kernel " +
                    "types (ADR-021) and must not import the frozen core.interactions zone. " +
                    "ActionProfiles/Profiles are excluded: even their now UI-action-split-reduced " +
                    "DEFAULT_SAFE/DEFAULT_RELIABLE and RAW/DEBUG/FAST/VISUAL presets still " +
                    "reference Before/After hook constants (core.interactions.hooks), a separate, " +
                    "already-documented deferral (I2.1/I2.2 notes in hooks-pipeline.md). " +
                    "Capability-specific constants (CLICKABLE_SAFE etc.) moved to " +
                    "elements.api.actions.CapabilityProfiles in I2.2 and no longer need this exclusion.");

        rule.check(allClasses);
    }
}
