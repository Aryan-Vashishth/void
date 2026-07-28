package elements.api.actions;

import core.actions.ActionCapability;
import core.actions.ActionProfile;
import elements.api.UIElement;
import elements.meta.ElementRole;

/**
 * Abstract base for type-family actions ({@link TypeAction}, {@link ClearAction},
 * {@link AppendTypeAction}, {@link TypeAndPressAction}, {@link TypeSearchAction},
 * {@link SubmitSearchAction}).
 *
 * <p>Owns the {@link CapabilityProfiles#TYPEABLE_SAFE} and {@link CapabilityProfiles#TYPEABLE_RELIABLE}
 * profile constants shared by all type-family actions. Subclasses declare their own constructor
 * and {@link #execute} implementation — no profile override is needed.</p>
 *
 * <h3>Safe profile ({@code TYPEABLE_SAFE})</h3>
 * <ul>
 *   <li>Before: clear field, wait for element visible</li>
 *   <li>After: highlight element</li>
 * </ul>
 *
 * <h3>Reliable profile ({@code TYPEABLE_RELIABLE})</h3>
 * <ul>
 *   <li>Before: wait for Angular loader, wait for element visible, clear field</li>
 *   <li>After: wait for Angular loader, wait for spinner, highlight element</li>
 * </ul>
 *
 * @see ClickableElementAction
 * @see SelectableElementAction
 */
abstract class TypeableElementAction extends ElementAction {

    protected TypeableElementAction(UIElement element, ElementRole role, ActionCapability capability) {
        super(element, role, capability);
    }

    @Override
    protected ActionProfile defaultSafeProfile() {
        return CapabilityProfiles.TYPEABLE_SAFE;
    }

    @Override
    protected ActionProfile defaultReliableProfile() {
        return CapabilityProfiles.TYPEABLE_RELIABLE;
    }
}
