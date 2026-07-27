package elements.api.actions;

import core.actions.ActionCapability;
import core.actions.ActionProfile;
import elements.api.UIElement;
import elements.meta.ElementRole;

/**
 * Abstract base for click-family actions ({@link ClickAction}, {@link ToggleAction}, {@link CheckAction}).
 *
 * <p>Owns the {@link CapabilityProfiles#CLICKABLE_SAFE} and {@link CapabilityProfiles#CLICKABLE_RELIABLE}
 * profile constants shared by all click-family actions. Subclasses declare their own constructor
 * and {@link #execute} implementation — no profile override is needed.</p>
 *
 * <h3>Safe profile ({@code CLICKABLE_SAFE})</h3>
 * <ul>
 *   <li>Before: wait for element clickable</li>
 *   <li>After: wait for Angular loader, highlight element</li>
 * </ul>
 *
 * <h3>Reliable profile ({@code CLICKABLE_RELIABLE})</h3>
 * <ul>
 *   <li>Before: wait for Angular loader, wait for element clickable</li>
 *   <li>After: wait for Angular loader, wait for spinner, highlight element</li>
 * </ul>
 *
 * @see TypeableElementAction
 * @see SelectableElementAction
 */
abstract class ClickableElementAction extends ElementAction {

    protected ClickableElementAction(UIElement element, ElementRole role, ActionCapability capability) {
        super(element, role, capability);
    }

    @Override
    protected ActionProfile defaultSafeProfile() {
        return CapabilityProfiles.CLICKABLE_SAFE;
    }

    @Override
    protected ActionProfile defaultReliableProfile() {
        return CapabilityProfiles.CLICKABLE_RELIABLE;
    }
}
