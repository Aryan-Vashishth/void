package core.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Set;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
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
 * <p>{@code core.runtime} must not import {@code WebDriver} or {@code SeleniumDriverContext}.
 * {@code core.engine.LocatorDescriptor} must not import {@code org.openqa.selenium.By}.</p>
 */
public class KernelBoundaryRulesTest {

    private static final String SELENIUM = "org.openqa.selenium..";

    private JavaClasses allClasses;

    @BeforeClass
    public void importClasses() {
        allClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("core", "domain", "dsl");
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

    @Test(description = "domain.automation.web.vocabulary has no Selenium dependency")
    public void elementsApiIsSeleniumFree() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("domain.automation.web.vocabulary..")
                .should().dependOnClassesThat().resideInAPackage(SELENIUM)
                .because(
                    "vocabulary.* defines capability interfaces, actions, and structural " +
                    "utilities. All are domain-neutral vocabulary. ADR-021. " +
                    "Relocated from elements.* in I6.4.");

        rule.check(allClasses);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Runtime facade -- no WebDriver/SeleniumDriverContext fields (ADR-018)
    //
    // Note: VOID.getDriver() is a @Deprecated(forRemoval=true) escape hatch
    // whose return type is WebDriver. That method-level dependency is a known
    // tracked bridge, scheduled for deletion in I9.3. This check locks the
    // stronger form of coupling: a WebDriver or SeleniumDriverContext stored as a field.
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

    @Test(description = "core.runtime does not store SeleniumDriverContext as a field")
    public void runtimeDoesNotDeclareDriverContextField() {
        ArchRule rule = noFields()
                .that().areDeclaredInClassesThat().resideInAPackage("core.runtime..")
                .should().haveRawType("core.driver.SeleniumDriverContext")
                .because("VOID must not hold a SeleniumDriverContext field. ADR-018 + ADR-021.");

        rule.check(allClasses);
    }

    // ─────────────────────────────────────────────────────────────────────
    // LocatorDescriptor -- Selenium-free (ADR-019)
    // ─────────────────────────────────────────────────────────────────────

    @Test(description = "LocatorDescriptor has no org.openqa.selenium.By dependency")
    public void locatorDescriptorIsSeleniumFree() {
        ArchRule rule = noClasses()
                .that().haveFullyQualifiedName("domain.automation.web.locator.LocatorDescriptor")
                .should().dependOnClassesThat().resideInAPackage(SELENIUM)
                .because("LocatorDescriptor must not import Selenium types. ADR-019 + ADR-021.");

        rule.check(allClasses);
    }

    @Test(description = "core.engine contains no Locator* class after I7.2 move")
    public void coreEngineHasNoLocatorTypes() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("core.engine")
                .should().haveSimpleNameStartingWith("Locator")
                .because("LocatorDescriptor and LocatorStrategy moved to elements.locator in I7.2. ADR-019.");

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

    private static final String UI_ELEMENT = "domain.automation.web.vocabulary.element.UIElement";

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
                .should().dependOnClassesThat().haveFullyQualifiedName("domain.automation.web.vocabulary.role.ElementRole")
                .because(
                    "ElementRole is UI-domain locator-role vocabulary; the kernel dispatches on " +
                    "Action/Flow only. runtime-redesign I2.2 exit criterion.");

        rule.check(allClasses);
    }

    @Test(description = "core.actions kernel types have no elements.api.capability dependency")
    public void actionsKernelDoesNotDependOnCapabilityInterfaces() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("core.actions")
                .should().dependOnClassesThat().resideInAPackage("domain.automation.web.vocabulary.capability..")
                .because(
                    "Capability interfaces are UI-domain vocabulary; the kernel never references " +
                    "them. runtime-redesign I2.2 exit criterion.");

        rule.check(allClasses);
    }

    // ─────────────────────────────────────────────────────────────────────
    // I3.3 -- Neutral capability contract
    // The kernel references capabilities contract-typed (ActionCapability interface)
    // only. Concrete Web-domain capability interfaces (elements.api.capability.*)
    // must never appear in any kernel package.
    // ─────────────────────────────────────────────────────────────────────

    @Test(description = "full kernel never references concrete Web-domain capability types (I3.3)")
    public void kernelCapabilityReferencesAreContractTypedOnly() {
        ArchRule rule = noClasses()
                .that().resideInAnyPackage(
                    "core.actions..", "core.flow..", "core.executor..",
                    "core.runtime..", "core.bootstrap..", "core.context..")
                .should().dependOnClassesThat().resideInAPackage("domain.automation.web.vocabulary.capability..")
                .because(
                    "Kernel capability references must be contract-typed via ActionCapability only. " +
                    "Concrete Web-domain capability interfaces (Clickable, Typeable, Selectable, ...) " +
                    "are elements.api.capability domain vocabulary, not kernel types (ADR-021). " +
                    "The open-set extension point is ActionCapability.of(String). " +
                    "runtime-redesign I3.3.");

        rule.check(allClasses);
    }

    @Test(description = "domain.automation.web.vocabulary.actions has no Selenium dependency")
    public void uiActionsAreSeleniumFree() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("domain.automation.web.vocabulary.actions..")
                .should().dependOnClassesThat().resideInAPackage(SELENIUM)
                .because("Concrete UI actions delegate to UIEngine, never Selenium directly. ADR-021, I2.2. Relocated from elements.api.actions in I6.4.");

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

    // ─────────────────────────────────────────────────────────────────────
    // Axis: Domain neutrality -- cycle break (ADR-021, I2 phase 2.3)
    //
    // Audit D1: the elements.api <-> kernel mutual dependency was proof the two
    // packages were one bounded context. After I2.2 physically moved
    // ElementAction and its family to elements.api.actions, the kernel->elements
    // edge was already gone in practice (verified: zero elements.* imports in
    // any kernel package). This is the permanent ratchet that makes D1
    // unrecurrable -- broader than the individual UIElement/ElementRole/
    // capability checks above, it forbids the kernel from depending on
    // elements.* at all, full stop, across every kernel-owned package
    // (ADR-021 membership: core.actions, core.actions.trace, core.actions.hooks,
    // core.flow, core.executor, core.context, core.runtime).
    // ─────────────────────────────────────────────────────────────────────

    // ─────────────────────────────────────────────────────────────────────
    // Axis: Engine neutrality -- factory/implementation separation (I4.1)
    //
    // UIEngineFactory is the neutral engine contract; it must not reference any
    // concrete engine implementation. Adding a second engine must require zero edits
    // inside core.engine -- only a new EngineRegistrar + services descriptor entry.
    // ─────────────────────────────────────────────────────────────────────

    @Test(description = "UIEngineFactory does not depend on domain.automation.web.selenium (I4.1)")
    public void engineFactoryIsImplementationFree() {
        ArchRule rule = noClasses()
                .that().haveFullyQualifiedName("domain.automation.web.engine.UIEngineFactory")
                .should().dependOnClassesThat().resideInAPackage("domain.automation.web.selenium..")
                .because(
                    "UIEngineFactory is the neutral engine contract. Concrete engine " +
                    "implementations register via the EngineRegistrar SPI; the factory " +
                    "must not import or reference them directly. runtime-redesign I4.1, P8. " +
                    "Relocated from core.engine to domain.automation.web.engine in I6.4.");

        rule.check(allClasses);
    }

    @Test(description = "DomainRegistry does not depend on core.engine.selenium (I6.1)")
    public void domainRegistryIsImplementationFree() {
        ArchRule rule = noClasses()
                .that().haveFullyQualifiedName("core.engine.DomainRegistry")
                .should().dependOnClassesThat().resideInAPackage("core.engine.selenium..")
                .because(
                    "DomainRegistry is the neutral domain contract. Domain implementations " +
                    "register via the DomainRegistrar SPI; the registry must not import or " +
                    "reference them directly. runtime-redesign I6.1.");

        rule.check(allClasses);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Axis: Engine neutrality -- contract package driver-free (I4.2)
    //
    // EngineBootstrap previously carried SeleniumDriverFactory.Profile, pulling core.driver
    // into the neutral contract package. After I4.2, EngineBootstrap carries only
    // an opaque Properties map; the driver-layer type is resolved inside
    // SeleniumEngineRegistrar (core.engine.selenium), which is allowed to import it.
    // ─────────────────────────────────────────────────────────────────────

    // ─────────────────────────────────────────────────────────────────────
    // Axis: Domain neutrality -- Executor contract purity (I4.3)
    //
    // Executor is the kernel-neutral execution-owner concept (ADR-021 AD2).
    // It may import only JDK types. No elements.*, Selenium, or core.driver
    // may appear on it -- any such import would permanently bind all domain
    // executors to web vocabulary, violating domain neutrality by definition.
    // This is the roadmap's central risk (I4.3 plan) and is enforced here.
    // ─────────────────────────────────────────────────────────────────────

    @Test(description = "Executor is a neutral kernel contract: no elements, Selenium, or driver (I4.3)")
    public void executorContractIsNeutral() {
        ArchRule rule = noClasses()
                .that().haveFullyQualifiedName("core.engine.Executor")
                .should().dependOnClassesThat()
                        .resideInAnyPackage("domain.automation.web..", "org.openqa.selenium..")
                .because(
                    "Executor is the kernel's neutral execution-owner contract (ADR-021 AD2). " +
                    "No UI vocabulary, Selenium, or driver infrastructure may appear on it. " +
                    "A method a non-web executor could only implement by throwing does not " +
                    "belong here. runtime-redesign I4.3.");

        rule.check(allClasses);
    }

    @Test(description = "core.engine contract package has no core.driver dependency (I4.2)")
    public void engineContractIsDriverFree() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("core.engine")
                .should().dependOnClassesThat().resideInAPackage("domain.automation.web.selenium.driver..")
                .because(
                    "The engine contract package must not import driver infrastructure. " +
                    "domain.automation.web.selenium may import domain.automation.web.selenium.driver; " +
                    "the contract package core.engine may not. runtime-redesign I4.2.");

        rule.check(allClasses);
    }

    @Test(description = "no kernel package depends on domain.automation.web.vocabulary.* (cycle break, D1 unrecurrable)")
    public void kernelPackagesDoNotDependOnElements() {
        // domain.automation.web.locator and domain.automation.web.engine are excepted:
        // LocatorDescriptor is the kernel's neutral locator value type (moved I7.2, relocated I6.4).
        // UIEngine and UIEngineFactory are the kernel's engine contract types (relocated I6.4).
        // All three bridges close in I9.4.
        ArchRule rule = noClasses()
                .that().resideInAnyPackage(
                        "core.actions", "core.actions.trace..", "core.actions.hooks..",
                        "core.flow..", "core.executor..", "core.context..", "core.runtime..")
                .should().dependOnClassesThat(
                    JavaClass.Predicates.resideInAPackage("domain.automation.web..")
                        .and(DescribedPredicate.describe(
                            "is not domain.automation.web.locator or domain.automation.web.engine (kernel bridges, close I9.4)",
                            jc -> !jc.getPackageName().startsWith("domain.automation.web.locator")
                               && !jc.getPackageName().startsWith("domain.automation.web.engine"))))
                .because(
                    "The kernel/UI-domain dependency direction is one-way: domain.automation.web.vocabulary.* " +
                    "and domain.automation.web.selenium.* may depend on the kernel, never the reverse. " +
                    "Audit D1; runtime-redesign I2.3, I6.4. " +
                    "Exceptions: domain.automation.web.locator (neutral locator value types) and " +
                    "domain.automation.web.engine (engine contract types) -- bridges close I9.4.");

        rule.check(allClasses);
    }

    // ═════════════════════════════════════════════════════════════════════
    // Axis: Domain neutrality -- kernel purity gate (ADR-021, I2 phase 2.4)
    //
    // Consolidates I2.1-I2.3 into one named, positive-allowlist boundary,
    // rather than the scattered negative ("must not depend on X") checks above.
    // The kernel may depend on: JDK/javax, core.logging, core.annotations,
    // core.target, itself (core.actions/.trace/.hooks, core.flow, core.executor,
    // core.context minus the legacy ExecutionContext, core.runtime,
    // core.bootstrap -- ADR-021 groups FrameworkBootstrap into "Runtime"), and
    // NOTHING ELSE except the short, explicitly named list below.
    //
    // Every entry in KERNEL_PURITY_TEMPORARY_EXCEPTIONS is a real, currently-live
    // dependency, not a hypothetical -- each is load-bearing today and closes on
    // a specific later phase:
    //
    //   core.engine.Executor
    //       The kernel's neutral execution-owner contract (ADR-021 AD2). I4.4
    //       retyped Action.perform(), ActionHandler.execute(), and FlowExecutor
    //       to accept Executor. Unlike the others, this is NOT temporary --
    //       Executor is the permanent neutral contract; it will not be removed
    //       from kernel method signatures. Listed here because the allowed
    //       predicate does not yet enumerate individual core.engine types;
    //       consolidate into the predicate when the full core.engine split lands.
    //   core.engine.UIEngine
    //       Two distinct load-bearing cases:
    //       (a) Deprecated bridge default methods on Action (perform/resolve) and
    //           ActionHandler (execute, legacy) still carry UIEngine in their
    //           signatures. Scheduled for deletion I9.4.
    //       (b) VOID (engine field, getEngine(), navigateTo, etc.),
    //           SessionContext (engine field, engine(), getEngineName, toString),
    //           and VOIDBuilder (start()) -- session-wiring code that is
    //           explicitly out of scope for I4.4. Closes across later phases
    //           (I4.5 and the Migration Ledger, I9.3).
    //   elements.locator.LocatorDescriptor
    //       Action.resolve(), ActionHandler.execute(), and HookChainAction carry
    //       LocatorDescriptor in their kernel-side signatures. The UIEngine-typed
    //       bridge overloads from I4.4 are scheduled for deletion in I9.4.
    //       Moved from core.engine to elements.locator in I7.2.
    //   core.engine.DomainRegistry
    //       VOIDBuilder's domain-selection wiring (I6.1). DomainRegistry is the
    //       neutral domain registration contract and is a permanent fixture of the
    //       session bootstrap path, not a removal target. Introduced in I6.1.
    //   core.engine.EngineBootstrap, core.engine.UIEngineFactory
    //       VOID/VOIDBuilder's engine-selection wiring. Already in the
    //       runtime-redesign Migration Ledger (EngineBootstrap: pre-existing,
    //       closes 4.2).
    //   core.driver.SeleniumDriverFactory
    //       VOIDBuilder.profile(SeleniumDriverFactory.Profile) @Deprecated bridge (I6.4 F4
    //       resolved: SessionProfile introduced in core.runtime; bridge closes I9.3).
    //       VOID.start(SeleniumDriverFactory.Profile) @Deprecated bridge (closes I9.3).
    //   core.utils.ConfigLoader, core.utils.ConfigPaths
    //       Config-driven default profile selection (ActionProfiles) and
    //       bootstrap config paths (FrameworkBootstrap). Narrow, non-domain
    //       utility dependency -- not yet slated for closure by a specific
    //       phase; revisit if core.utils.web's ADR-020 work ever generalizes.
    //   core.interactions.hooks.Before, core.interactions.hooks.After
    //       ActionProfiles/Profiles' default hook constants -- already
    //       documented and excluded by name in the I2.1/I2.2 checks above.
    //   core.interactions.Interactions, org.openqa.selenium.WebDriver
    //       VOID.interaction() and VOID.getDriver() -- both already
    //       @Deprecated(forRemoval = true) bridges to the legacy path, tracked
    //       in the Migration Ledger, closing I9.3.
    //
    // Risk (per the phase's own risk note): a check with silent, undocumented
    // exceptions is a false-pass ratchet -- it would look green while hiding the
    // real remaining gaps. Every exception here is named, reasoned, and
    // cross-referenced so the check stays honest about what "kernel purity"
    // actually means today versus the roadmap's end state.
    // ═════════════════════════════════════════════════════════════════════

    private static final Set<String> KERNEL_PURITY_TEMPORARY_EXCEPTIONS = Set.of(
            "core.engine.Executor",
            "domain.automation.web.engine.UIEngine",
            "domain.automation.web.locator.LocatorDescriptor",
            "core.engine.DomainRegistry",
            "core.engine.EngineBootstrap",
            "domain.automation.web.engine.UIEngineFactory",
            "core.utils.ConfigLoader",
            "core.utils.ConfigPaths",
            "core.interactions.hooks.Before",
            "core.interactions.hooks.After",
            "core.interactions.Interactions",
            "org.openqa.selenium.WebDriver"
    );

    private static final DescribedPredicate<JavaClass> KERNEL_PURITY_ALLOWED_DEPENDENCIES =
            JavaClass.Predicates.resideInAnyPackage(
                    "java..", "javax..",
                    "core.logging..", "core.annotations..", "core.target..",
                    "core.actions", "core.actions.trace..", "core.actions.hooks..",
                    "core.flow..", "core.executor..", "core.runtime..", "core.bootstrap..")
            .or(DescribedPredicate.describe(
                    "is core.context.SessionContext (the kernel's own Session member, ADR-021)",
                    javaClass -> javaClass.getFullName().equals("core.context.SessionContext")))
            .or(DescribedPredicate.describe(
                    "is a documented kernel-purity temporary exception (see class javadoc)",
                    javaClass -> KERNEL_PURITY_TEMPORARY_EXCEPTIONS.contains(javaClass.getFullName())));

    @Test(description = "kernel purity: kernel depends only on JDK/logging/annotations/target/itself,"
            + " plus documented temporary exceptions")
    public void kernelPurity() {
        ArchRule rule = classes()
                .that().resideInAnyPackage(
                        "core.actions", "core.actions.trace..", "core.actions.hooks..",
                        "core.flow..", "core.executor..", "core.context..", "core.runtime..",
                        "core.bootstrap..")
                .and().haveNameNotMatching("core\\.context\\.ExecutionContext")
                .should().onlyDependOnClassesThat(KERNEL_PURITY_ALLOWED_DEPENDENCIES)
                .because(
                    "Consolidates I2.1-I2.3 into one named boundary (runtime-redesign I2.4): the " +
                    "kernel may depend only on JDK, core.logging, core.annotations, core.target, " +
                    "itself, and the short, explicitly documented list of temporary exceptions in " +
                    "KERNEL_PURITY_TEMPORARY_EXCEPTIONS. core.context.ExecutionContext is excluded " +
                    "from the subject set -- it is frozen legacy content (Migration Ledger, deleted " +
                    "I9.3), not a kernel member. ADR-021.");

        rule.check(allClasses);
    }
}
