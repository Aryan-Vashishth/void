package core.logging;

import core.logging.config.LoggerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Small CLI smoke harness used to verify per-run log file naming.
 */
public final class LogFileSmokeMain {

    private LogFileSmokeMain() {}

    public static void main(String[] args) {
        if (args.length > 0 && args[0] != null && !args[0].isBlank()) {
            System.setProperty("void.logDir", args[0]);
        }

        CustomLogger.initialize(LogFileSmokeMain.class);
        CustomLogger.enableAnsi();
        CustomLogger.info.log("log smoke start");

        Logger traceLogger = LogManager.getLogger("trace");
        traceLogger.info("trace smoke start");

        LogManager.shutdown();
        System.out.println(LoggerContext.getRunId());
    }
}

