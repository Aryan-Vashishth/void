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

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Architecture ratchet enforcing {@code core.driver} isolation as Selenium-executor internals.
 *
 * <h3>Policy (ADR-021, runtime-redesign I4.5)</h3>
 * <p>{@code core.driver} ({@code DriverFactory}, {@code DriverContext}, {@code DriverManager},
 * {@code Waiter}) is Selenium-specific infrastructure. Only {@code core.engine.selenium.*}
 * may import it in production code. Every other import is a tracked violation
 * with a named closing phase in {@link #DRIVER_ISOLATION_EXCEPTIONS}.</p>
 *
 * <h3>Physical package move</h3>
 * <p>Moving {@code core/driver/} to {@code core/engine/selenium/driver/} (with class renames)
 * is optional and decided at activation. NOT done in I4.5. If the move happens, the exception
 * list collapses as each caller is migrated.</p>
 *
 * <h3>Test infrastructure</h3>
 * <p>This rule covers production code only ({@code DO_NOT_INCLUDE_TESTS}).
 * Test files ({@code InteractionsTest}, {@code InteractionsEndToEndTest}, {@code VOIDBuilderTest})
 * also import {@code core.driver.*} and are Selenium-specific by definition; they may keep
 * importing from the platform side (I4.5 plan). Tracked in the pre-implementation audit.</p>
 */
public class DriverIsolationRulesTest {

    // ═════════════════════════════════════════════════════════════════════
    // Driver isolation -- core.driver is Selenium-executor internal (ADR-021, I4.5)
    //
    // Every entry is a currently-live dependency with a documented closing phase:
    //
    //   core.interactions.Via
    //       Via.webElement(UIElement) and Via.webElement(By) convenience overloads
    //       call DriverContext.getActiveDriver() (thread-local driver access).
    //       Via is entirely Selenium-coupled; cleanup deferred to 9.x.
    //   core.utils.web.WaitUtils
    //       DriverContext.getDriver() called throughout deprecated utility methods.
    //       Closes 9.2 (Migration Ledger, broader core.utils dismantling).
    //   core.utils.web.DOMUtils
    //       Same pattern. @Deprecated. Closes 9.2.
    //   core.utils.web.TableHandler
    //       Same pattern. @Deprecated. Closes 9.2.
    //   core.utils.web.KeyValuePairHandler
    //       DriverContext and Waiter used. @Deprecated. Closes 9.2.
    //   core.utils.web.Upload
    //       Waiter used for upload-completion wait. @Deprecated. Closes 9.2.
    //   core.runtime.VOID
    //       DriverFactory.Profile in deprecated VOID.start(Profile) static factory.
    //       Post-4.2 residue. Closes 9.3 (Migration Ledger).
    //   core.runtime.VOIDBuilder
    //       profile(DriverFactory.Profile) -- public fluent builder API. Blocking
    //       decision: removing or renaming DriverFactory.Profile is a breaking
    //       external change. Closes when the I6.4 API surface decision is made.
    // ═════════════════════════════════════════════════════════════════════

    private static final Set<String> DRIVER_ISOLATION_EXCEPTIONS = Set.of(
            "core.interactions.Via",
            "core.utils.web.WaitUtils",
            "core.utils.web.DOMUtils",
            "core.utils.web.TableHandler",
            "core.utils.web.KeyValuePairHandler",
            "core.utils.web.Upload",
            "core.runtime.VOID",
            "core.runtime.VOIDBuilder"
    );

    private JavaClasses allClasses;

    @BeforeClass
    public void importClasses() {
        allClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("core", "elements", "dsl", "tests");
    }

    @Test(description = "core.driver imported only from the Selenium engine boundary (ADR-021, I4.5)")
    public void driverIsolation() {
        DescribedPredicate<JavaClass> isNotAnException = DescribedPredicate.describe(
                "is not a documented driver-isolation exception (see class javadoc)",
                javaClass -> !DRIVER_ISOLATION_EXCEPTIONS.contains(javaClass.getFullName())
        );

        ArchRule rule = noClasses()
                .that().resideOutsideOfPackages("core.engine.selenium..", "core.driver..")
                .and(isNotAnException)
                .should().dependOnClassesThat().resideInAPackage("core.driver..")
                .because("core.driver is Selenium-specific infrastructure and must not be treated as "
                        + "framework-neutral infrastructure. Only core.engine.selenium.* may import it. "
                        + "ADR-021, runtime-redesign I4.5. Every current exception is named and "
                        + "cross-referenced to its closing phase in DRIVER_ISOLATION_EXCEPTIONS.");

        rule.check(allClasses);
    }
}
