package domain.automation.web.vocabulary.actions;
import core.actions.ActionCapability;

import core.annotations.Beta;
import domain.automation.web.locator.LocatorDescriptor;
import domain.automation.web.engine.UIEngine;
import domain.automation.web.vocabulary.capability.Uploadable;
import domain.automation.web.vocabulary.role.ElementRole;

/**
 * Concrete action for uploading a file via an {@link Uploadable} element.
 *
 * <p>Emitted by {@code Uploadable.upload(String)}. Resolves the INPUT locator,
 * then delegates to {@link UIEngine#uploadFile}.</p>
 *
 * <p>Safe profile: {@link core.actions.ActionProfiles#DEFAULT_SAFE} — waits for element visibility before.</p>
 */
@Beta(since = "0.2", note = "Phase 14 — concrete action subclass")
public final class UploadAction extends ElementAction {

    private final String filePath;

    public UploadAction(Uploadable element, String filePath) {
        super(element, ElementRole.INPUT, ActionCapability.UPLOADABLE);
        this.filePath = filePath;
    }

    @Override
    public String operationLabel() { return "upload"; }

    @Override
    protected void execute(UIEngine engine, LocatorDescriptor descriptor) {
        engine.uploadFile(descriptor, filePath);
    }
}
