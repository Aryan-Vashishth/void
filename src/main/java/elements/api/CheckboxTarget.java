package elements.api;

import java.util.List;
import java.util.stream.Collectors;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import core.resolvers.locator.api.LocatorResolvers;

/**
 * Capability interface for checkbox elements (clickable + checkable).
 *
 * <p>Contains query methods for checked state. NO Action logic.</p>
 *
 * <h3>Hierarchy</h3>
 * <pre>
 *   Element → ClickTarget → CheckboxTarget → CheckboxAction
 * </pre>
 */
public interface CheckboxTarget extends ClickTarget {

    /**
     * Returns true if this checkbox element is currently checked.
     */
    default boolean isChecked(WebDriver driver) {
        WebElement element = driver.findElement(LocatorResolvers.strict().resolve(this));
        return element.isSelected();
    }

    /**
     * Returns all checkboxes related to this element.
     * For individual checkboxes, it returns itself.
     * For checkbox groups, override this method to return multiple.
     */
    default List<? extends CheckboxTarget> getAllCheckboxes() {
        return List.of(this);
    }

    /**
     * Returns all checked checkboxes from the group or self.
     */
    default List<CheckboxTarget> getChecked(WebDriver driver) {
        return getAllCheckboxes().stream()
                .filter(cb -> cb.isChecked(driver))
                .collect(Collectors.toList());
    }

    /**
     * Returns all unchecked checkboxes from the group or self.
     */
    default List<CheckboxTarget> getUnchecked(WebDriver driver) {
        return getAllCheckboxes().stream()
                .filter(cb -> !cb.isChecked(driver))
                .collect(Collectors.toList());
    }
}

