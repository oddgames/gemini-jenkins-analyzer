package io.jenkins.plugins.gemini_jenkins_analyzer;

import hudson.model.Run;
import jenkins.model.RunAction2;

/**
 * Build action to store and display error analysiss.
 */
public class ErrorAnalysisAction implements RunAction2 {

    private final String analysis;
    private final String originalErrorLogs;
    private final long timestamp;
    private transient Run<?, ?> run;

    public ErrorAnalysisAction(String analysis, String originalErrorLogs) {
        this.analysis = analysis;
        this.originalErrorLogs = originalErrorLogs;
        this.timestamp = System.currentTimeMillis();
    }

    @Override
    public String getIconFileName() {
        return "symbol-cube";
    }

    @Override
    public String getDisplayName() {
        return "AI Error Analysis";
    }

    @Override
    public String getUrlName() {
        return "error-analysis";
    }

    public String getAnalysis() {
        return analysis;
    }

    public String getOriginalErrorLogs() {
        return originalErrorLogs;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getFormattedTimestamp() {
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(timestamp));
    }

    @Override
    public void onAttached(Run<?, ?> r) {
        this.run = r;
    }

    @Override
    public void onLoad(Run<?, ?> r) {
        this.run = r;
    }

    /**
     * Get the associated run.
     * @return the run this action is attached to
     */
    public Run<?, ?> getRun() {
        return run;
    }

    /**
     * Check if this action has a valid analysis.
     * @return true if analysis is not null, not empty, and not just whitespace
     */
    public boolean hasValidAnalysis() {
        return analysis != null && !analysis.trim().isEmpty();
    }
}
