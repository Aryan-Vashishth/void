package Elements.interfacesv2;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import core.resolvers.locator.LocatorResolverV2;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents a checkbox (or checkbox group) UI element that is clickable and checkable.
 */
public interface CheckboxElement extends ClickableElement, BaseElement {

    /**
     * Returns true if this checkbox element is currently checked.
     * Works on single checkbox case.
     */
    default boolean isChecked(WebDriver driver) {
        WebElement element = driver.findElement(LocatorResolverV2.primary(this));
        return element.isSelected();
    }

    /**
     * Returns all checkboxes related to this element.
     * For individual checkboxes, it returns itself.
     * For checkbox groups, override this method to return multiple.
     */
    default List<? extends CheckboxElement> getAllCheckboxes() {
        return List.of(this); // default is self
    }

    /**
     * Returns all checked checkboxes from the group or self.
     */
    default List<CheckboxElement> getChecked(WebDriver driver) {
        return getAllCheckboxes().stream()
                .filter(cb -> cb.isChecked(driver))
                .collect(Collectors.toList());
    }

    /**
     * Returns all unchecked checkboxes from the group or self.
     */
    default List<CheckboxElement> getUnchecked(WebDriver driver) {
        return getAllCheckboxes().stream()
                .filter(cb -> !cb.isChecked(driver))
                .collect(Collectors.toList());
    }

    boolean isChecked();
}
