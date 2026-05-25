    private void emitGitHubWorkflowNotice(String plainLine, LogIntent intent) {
        // Only emit GitHub workflow notices if the log level is actually enabled
        if (!isLogLevelEnabled()) {
            return;
        }

        String command = null;
        if ("ERROR".equals(logLevel) || LogIntent.ALERT.equals(intent)) {
            command = "error";
        } else if ("WARN".equals(logLevel)) {
            command = "warning";
        }
        // Only emit error/warning commands, not info/debug notices
        if (command == null || plainLine == null || plainLine.isBlank()) {
            return;
        }
        System.out.println("::" + command + "::" + escapeGitHubWorkflowText(plainLine));
    }
