package core.actions;

import elements.api.Element;
import elements.meta.ElementRole;

/**
 * Abstract base for select-family actions ({@link OpenAction}, {@link SelectAction},
 * {@link SelectByTextAction}, {@link SelectByValueAction}, {@link SearchAndSelectAction}).
 *
 * <p>Owns the {@link ActionProfiles#SELECTABLE_SAFE} and {@link ActionProfiles#SELECTABLE_RELIABLE}
 * profile constants shared by all select-family actions. Subclasses declare their own constructor
 * and {@link #execute} implementation — no profile override is needed.</p>
 *
 * <h3>Safe profile ({@code SELECTABLE_SAFE})</h3>
 * <ul>
 *   <li>Before: wait for element visible, wait for element clickable, wait for Angular loader</li>
 *   <li>After: highlight element</li>
 * </ul>
 *
 * <h3>Reliable profile ({@code SELECTABLE_RELIABLE})</h3>
 * <ul>
 *   <li>Before: wait for Angular loader, wait for element visible, wait for element clickable</li>
 *   <li>After: wait for Angular loader, wait for spinner, highlight element</li>
 * </ul>
 *
 * @see ClickableElementAction
 * @see TypeableElementAction
 */
abstract class SelectableElementAction extends ElementAction {

    protected SelectableElementAction(Element element, ElementRole role, ActionCapability capability) {
        super(element, role, capability);
    }

    @Override
    protected ActionProfile defaultSafeProfile() {
        return ActionProfiles.SELECTABLE_SAFE;
    }

    @Override
    protected ActionProfile defaultReliableProfile() {
        return ActionProfiles.SELECTABLE_RELIABLE;
    }
}
