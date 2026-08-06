package examples.logging;

import static core.logging.CustomLogger.debug;
import static core.logging.CustomLogger.info;

/**
 * Thin call-site fixture for caller-chain unit tests.
 *
 * <p>Lives in {@code tests.*} so that its class name passes the
 * {@code isProjectFrame} filter in {@code LogActions} and therefore
 * appears as the callee in the caller chain written to debug-trace.
 * Tests in {@code core.logging.*} cannot serve this role because that
 * package is excluded by {@code isProjectFrame}.</p>
 */
public final class CallerChainHelper {

    private CallerChainHelper() {}

    public static void triggerDebugLog(String message) {
        debug.log(message);
    }

    public static void triggerInfoLog(String message) {
        info.log(message);
    }
}
