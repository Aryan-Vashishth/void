package elements.api.actions;
import core.actions.ActionCapability;

import core.annotations.Beta;
import elements.locator.LocatorDescriptor;
import core.engine.UIEngine;
import elements.api.capability.Selectable;
import elements.meta.ElementRole;

import java.time.Duration;

/**
 * Concrete action for selecting an option from a {@link Selectable} dropdown.
 *
 * <p>Emitted by {@code Selectable.select()}. Composite action:
 * <ol>
 *   <li>Clicks the TRIGGER locator to open the dropdown</li>
 *   <li>Waits for the Angular CDK overlay pane to appear</li>
 *   <li>Resolves and clicks the LIST locator (using the element's args)</li>
 * </ol>
 * </p>
 *
 * <p>To open the dropdown without selecting, use {@link OpenAction}.
 * To select by text or value attribute, use {@link SelectByTextAction} or
 * {@link SelectByValueAction}.</p>
 *
 * <p>Safe profile: {@link CapabilityProfiles#SELECTABLE_SAFE} — waits for visibility,
 * clickability, and Angular loader before; highlights after.</p>
 */
@Beta(since = "0.2", note = "Phase 14 — concrete action subclass")
public final class SelectAction extends SelectableElementAction {

    public SelectAction(Selectable element) {
        super(element, ElementRole.TRIGGER, ActionCapability.SELECTABLE);
    }

    @Override
    public String operationLabel() { return "select"; }

    @Override
    protected void execute(UIEngine engine, LocatorDescriptor descriptor) {
        engine.click(descriptor);
        engine.waitForOverlay(Duration.ofSeconds(5));
        engine.click(engine.resolve(element, ElementRole.LIST, element.getArgs()));
    }
}
