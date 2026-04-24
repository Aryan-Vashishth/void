package elements.api;


import java.util.List;
import java.util.stream.Collectors;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import core.resolvers.locator.api.LocatorResolvers;

/**
 * Represents a checkbox (or checkbox group) UI element that is clickable and checkable.
 */
public interface Checkbox extends Clickable {

    /**
     * Returns true if this checkbox element is currently checked.
     * Works on single checkbox case.
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
    default List<? extends Checkbox> getAllCheckboxes() {
        return List.of(this); // default is self
    }

    /**
     * Returns all checked checkboxes from the group or self.
     */
    default List<Checkbox> getChecked(WebDriver driver) {
        return getAllCheckboxes().stream()
                .filter(cb -> cb.isChecked(driver))
                .collect(Collectors.toList());
    }

    /**
     * Returns all unchecked checkboxes from the group or self.
     */
    default List<Checkbox> getUnchecked(WebDriver driver) {
        return getAllCheckboxes().stream()
                .filter(cb -> !cb.isChecked(driver))
                .collect(Collectors.toList());
    }
}
