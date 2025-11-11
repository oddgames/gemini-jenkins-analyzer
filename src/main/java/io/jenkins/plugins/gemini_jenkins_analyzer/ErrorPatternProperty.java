package io.jenkins.plugins.gemini_jenkins_analyzer;

import hudson.Extension;
import hudson.model.Job;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest2;

/**
 * Job property to configure error patterns for AI analysis.
 */
public class ErrorPatternProperty extends JobProperty<Job<?, ?>> {

    private String errorPatterns;
    private int contextLines;

    @DataBoundConstructor
    public ErrorPatternProperty() {
        this.errorPatterns = "";
        this.contextLines = 3;
    }

    public String getErrorPatterns() {
        return errorPatterns;
    }

    @DataBoundSetter
    public void setErrorPatterns(String errorPatterns) {
        this.errorPatterns = errorPatterns != null ? errorPatterns : "";
    }

    public int getContextLines() {
        return contextLines;
    }

    @DataBoundSetter
    public void setContextLines(int contextLines) {
        this.contextLines = contextLines >= 0 ? contextLines : 3;
    }

    @Extension
    public static class DescriptorImpl extends JobPropertyDescriptor {

        @Override
        public String getDisplayName() {
            return "Error Pattern Configuration for AI Analysis";
        }

        @Override
        public boolean isApplicable(Class<? extends Job> jobType) {
            // Apply to all job types
            return true;
        }

        @Override
        public JobProperty<?> newInstance(StaplerRequest2 req, JSONObject formData) throws FormException {
            if (formData == null || (!formData.has("errorPatterns") && !formData.has("contextLines"))) {
                return null;
            }
            ErrorPatternProperty property = new ErrorPatternProperty();
            property.setErrorPatterns(formData.optString("errorPatterns", ""));
            property.setContextLines(formData.optInt("contextLines", 3));
            return property;
        }
    }
}
