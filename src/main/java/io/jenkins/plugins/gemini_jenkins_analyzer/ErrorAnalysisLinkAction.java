package io.jenkins.plugins.gemini_jenkins_analyzer;

import hudson.model.Run;
import jenkins.model.RunAction2;

/**
 * Build action that always shows in the sidebar to provide access to error analysis.
 * Unlike ErrorAnalysisAction which is only added after an analysis is generated,
 * this action is always present on completed builds.
 */
public class ErrorAnalysisLinkAction implements RunAction2 {

    private transient Run<?, ?> run;

    public ErrorAnalysisLinkAction(Run<?, ?> run) {
        this.run = run;
    }

    @Override
    public String getIconFileName() {
        // Only show if build is complete
        if (run != null && !run.isBuilding()) {
            return "symbol-cube";
        }
        return null;
    }

    @Override
    public String getDisplayName() {
        return "Analyze Errors";
    }

    @Override
    public String getUrlName() {
        return "analyze-errors";
    }

    @Override
    public void onAttached(Run<?, ?> r) {
        this.run = r;
    }

    @Override
    public void onLoad(Run<?, ?> r) {
        this.run = r;
    }

    public Run<?, ?> getRun() {
        return run;
    }

    /**
     * Check if an analysis already exists for this build.
     */
    public boolean hasExistingAnalysis() {
        if (run == null) return false;
        ErrorAnalysisAction existing = run.getAction(ErrorAnalysisAction.class);
        return existing != null && existing.hasValidAnalysis();
    }

    /**
     * Get the existing analysis if available.
     */
    public ErrorAnalysisAction getExistingAnalysis() {
        if (run == null) return null;
        return run.getAction(ErrorAnalysisAction.class);
    }

    /**
     * Handle requests to this action by redirecting to the appropriate page.
     */
    public void doDynamic(org.kohsuke.stapler.StaplerRequest2 req, org.kohsuke.stapler.StaplerResponse2 rsp) throws java.io.IOException {
        // If an analysis exists, redirect to it
        if (hasExistingAnalysis()) {
            rsp.sendRedirect2("../error-analysis");
        } else {
            // Otherwise, redirect to console page with auto-analyze flag
            rsp.sendRedirect2("../console?autoAnalyze=true");
        }
    }
}
