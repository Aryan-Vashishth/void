package core.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import elements.api.UIElement;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Architecture rules enforcing the UIElement API nested enum structure.
 *
 * <h3>Policy (from Phase 15 — Element API Simplification)</h3>
 * <ul>
 *   <li>Every enum that implements {@link UIElement} must be declared as a member of an
 *       enclosing page class or container — never as a top-level class.</li>
 * </ul>
 *
 * <p>This preserves compile-time discoverability via the
 * {@code PageName.GroupName.CONSTANT} navigation pattern and ensures the IDE presents
 * element groups through the page type rather than a flat global namespace.</p>
 */
public class ElementStructureRulesTest {

    private JavaClasses allClasses;

    @BeforeClass
    public void importClasses() {
        allClasses = new ClassFileImporter()
                .importPackages("tests", "elements", "core");
    }

    // ─────────────────────────────────────────────────────────────────────
    // Rule 1 — UIElement enums must be nested inside a page class
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Every enum that implements {@link UIElement} must be a member class of an enclosing
     * page container — not a standalone top-level class.
     *
     * <p>Permitted: {@code DemoLoginPage.Credentials}, {@code AccountMappingElements.Header}<br>
     * Forbidden: a top-level {@code Credentials} enum that happens to implement UIElement.</p>
     */
    @Test(description = "UIElement enums must be nested inside a page class")
    public void elementEnumsMustBeNested() {
        ArchRule rule = noClasses()
                .that().areEnums()
                .and().implement(UIElement.class)
                .should().beTopLevelClasses()
                .because(
                    "UIElement enums must be declared as members of an enclosing page class so " +
                    "that they are discovered via PageName.Group.CONSTANT autocomplete. " +
                    "A top-level element enum breaks IDE navigation and loses the capability " +
                    "grouping context. See Phase 15 — Preserve Nested Enum Organization."
                );

        rule.check(allClasses);
    }
}
