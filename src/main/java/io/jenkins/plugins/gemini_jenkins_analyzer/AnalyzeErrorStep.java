package io.jenkins.plugins.gemini_jenkins_analyzer;

import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;
import java.util.Set;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

/**
 * Pipeline step to explain errors using AI.
 */
public class AnalyzeErrorStep extends Step {

    private String logPattern;
    private int maxLines;

    @DataBoundConstructor
    public AnalyzeErrorStep() {
        this.logPattern = "";
        this.maxLines = 100;
    }

    public String getLogPattern() {
        return logPattern;
    }

    @DataBoundSetter
    public void setLogPattern(String logPattern) {
        this.logPattern = logPattern != null ? logPattern : "";
    }

    public int getMaxLines() {
        return maxLines;
    }

    @DataBoundSetter
    public void setMaxLines(int maxLines) {
        this.maxLines = maxLines > 0 ? maxLines : 100;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new AnalyzeErrorStepExecution(context, this);
    }

    @Extension
    public static class DescriptorImpl extends StepDescriptor {

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return Set.of(Run.class, TaskListener.class);
        }

        @Override
        public String getFunctionName() {
            return "analyzeError";
        }

        @Override
        public String getDisplayName() {
            return "Analyze Error with AI";
        }
    }

    private static class AnalyzeErrorStepExecution extends SynchronousNonBlockingStepExecution<Void> {

        private static final long serialVersionUID = 1L;
        private final transient AnalyzeErrorStep step;

        AnalyzeErrorStepExecution(StepContext context, AnalyzeErrorStep step) {
            super(context);
            this.step = step;
        }

        @Override
        protected Void run() throws Exception {
            Run<?, ?> run = getContext().get(Run.class);
            TaskListener listener = getContext().get(TaskListener.class);

            ErrorAnalyzer explainer = new ErrorAnalyzer();
            explainer.analyzeError(run, listener, step.getLogPattern(), step.getMaxLines());

            return null;
        }
    }
}
