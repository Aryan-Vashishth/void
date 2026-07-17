package core.resolvers.locator.sync;

import core.resolvers.locator.sync.LocatorTemplateGenerator.LocatorKey;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates that every expected locator key has a non-blank value in the properties file.
 *
 * <p>Keys added by the current sync run but not yet filled produce an error with
 * {@code lineNumber == -1} — these appear as {@code (new)} in CLI output.</p>
 */
final class EmptyKeyValidator {

    record EmptyKeyError(String key, int lineNumber) {}

    List<EmptyKeyError> validate(List<LocatorKey> expectedKeys, LineTrackingPropertiesReader reader) {
        List<EmptyKeyError> errors = new ArrayList<>();
        for (LocatorKey lk : expectedKeys) {
            String value = reader.getValue(lk.key());
            if (value == null || value.isBlank()) {
                errors.add(new EmptyKeyError(lk.key(), reader.getLineNumber(lk.key())));
            }
        }
        return errors;
    }
}
