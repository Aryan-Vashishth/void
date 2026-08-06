package examples.logging;

import core.logging.CustomLogger;
import core.logging.config.LoggerContext;
import org.apache.logging.log4j.LogManager;

/**
 * Smoke harness that emits a project-package call chain into the trace file.
 */
public final class TraceChainSmokeMain {

    private TraceChainSmokeMain() {}

    public static void main(String[] args) {
        CustomLogger.initialize(TraceChainSmokeMain.class);
        CustomLogger.enableAnsi();
        new TraceChainSmokeMain().entry();
        LogManager.shutdown();
        System.out.println(LoggerContext.getRunId());
    }

    private void entry() {
        layerOne();
    }

    private void layerOne() {
        layerTwo();
    }

    private void layerTwo() {
        CustomLogger.info.log("trace-chain smoke");
    }
}

