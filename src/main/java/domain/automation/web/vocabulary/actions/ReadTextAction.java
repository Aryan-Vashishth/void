package domain.automation.web.vocabulary.actions;
import core.actions.ActionCapability;

import core.annotations.Beta;
import domain.automation.web.locator.LocatorDescriptor;
import domain.automation.web.engine.UIEngine;
import domain.automation.web.vocabulary.capability.ReadOnly;
import domain.automation.web.vocabulary.role.ElementRole;

/**
 * Concrete action for reading visible text from a {@link ReadOnly} element.
 *
 * <p>Emitted by {@code ReadOnly.readText()}. Resolves the TEXT locator, then delegates
 * to {@link UIEngine#getText}.</p>
 *
 * <p>Safe profile: {@link core.actions.ActionProfiles#DEFAULT_SAFE} — waits for element visibility before.</p>
 */
@Beta(since = "0.2", note = "Phase 19 — concrete action subclass for ReadOnly")
public final class ReadTextAction extends ElementAction {

    public ReadTextAction(ReadOnly element) {
        super(element, ElementRole.TEXT, ActionCapability.READ_ONLY);
    }

    @Override
    public String operationLabel() { return "read"; }

    @Override
    protected void execute(UIEngine engine, LocatorDescriptor descriptor) {
        engine.getText(descriptor);
    }
}
