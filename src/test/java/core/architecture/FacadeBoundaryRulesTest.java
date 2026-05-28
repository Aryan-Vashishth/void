package core.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import core.engine.UIEngine;
import core.executor.FlowExecutor;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

/**
 * Architecture rules enforcing the VOID façade boundary.
 *
 * <h3>Policy (from ADR-011 — VOID as Primary Session Façade)</h3>
 * <ul>
 *   <li>Test classes must not hold a field of type {@code UIEngine} — engine references belong on the façade.</li>
 *   <li>Test classes must not directly instantiate {@code FlowExecutor} — use {@code VOID.run()} instead.</li>
 *   <li>Test classes must not call navigation / URL / title methods on {@code UIEngine} directly — these are now on the façade.</li>
 * </ul>
 *
 * <p>Note: custom hook lambdas that <em>receive</em> a {@code UIEngine} as a parameter are
 * intentional and permitted — this rule targets field declarations and direct construction only.</p>
 *
 * @see <a href="../../../../docs/plan/ongoing/011-void-facade-boundary.md">ADR-011</a>
 */
public class FacadeBoundaryRulesTest {

    /** All classes residing in the {@code tests.*} packages. */
    private JavaClasses testClasses;

    @BeforeClass
    public void importClasses() {
        testClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("tests");
    }

    // ─────────────────────────────────────────────────────────────────────
    // Rule 1 — No UIEngine fields in test classes
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Test classes must not declare a field of type {@code UIEngine}.
     *
     * <p>Engine references must be obtained through the VOID façade:
     * {@code VOID.getEngine()} for the escape hatch, not stored as fields.</p>
     */
    @Test(description = "Test classes must not hold a UIEngine field")
    public void testClassesShouldNotDeclareUIEngineFields() {
        ArchRule rule = noFields()
                .that().areDeclaredInClassesThat().resideInAPackage("tests..")
                .should().haveRawType(UIEngine.class)
                .because(
                    "UIEngine must not be stored as a field in test classes. " +
                    "Use the VOID session façade (app.navigateTo(), app.getCurrentUrl(), etc.) " +
                    "or obtain the engine on-demand via app.getEngine() for advanced scenarios. " +
                    "See ADR-011."
                );

        rule.check(testClasses);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Rule 2 — No direct FlowExecutor construction in test classes
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Test classes must not directly instantiate {@code FlowExecutor}.
     *
     * <p>Permitted fix: replace {@code new FlowExecutor(engine).run(flow)} with
     * {@code app.run(flow)}.</p>
     */
    @Test(description = "Test classes must not directly instantiate FlowExecutor")
    public void testClassesShouldNotInstantiateFlowExecutorDirectly() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("tests..")
                .should().callConstructor(FlowExecutor.class, UIEngine.class)
                .because(
                    "Test authors should use VOID.run(flow) or VOID.run(action) instead of " +
                    "constructing FlowExecutor directly. FlowExecutor is an internal detail of " +
                    "the VOID session façade. See ADR-011."
                );

        rule.check(testClasses);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Rule 3 — No FlowExecutor fields in test classes
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Test classes must not hold a field of type {@code FlowExecutor}.
     */
    @Test(description = "Test classes must not hold a FlowExecutor field")
    public void testClassesShouldNotDeclareFlowExecutorFields() {
        ArchRule rule = noFields()
                .that().areDeclaredInClassesThat().resideInAPackage("tests..")
                .should().haveRawType(FlowExecutor.class)
                .because(
                    "FlowExecutor is an internal execution mechanism. " +
                    "Test classes should run flows via VOID.run(flow). See ADR-011."
                );

        rule.check(testClasses);
    }
}
