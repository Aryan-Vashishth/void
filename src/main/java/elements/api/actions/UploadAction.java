package elements.api.actions;
import core.actions.ActionCapability;

import core.annotations.Beta;
import core.engine.LocatorDescriptor;
import core.engine.UIEngine;
import elements.api.capability.Uploadable;
import elements.meta.ElementRole;

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
