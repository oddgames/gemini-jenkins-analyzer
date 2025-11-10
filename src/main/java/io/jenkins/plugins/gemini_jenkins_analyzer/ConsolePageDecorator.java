package io.jenkins.plugins.gemini_jenkins_analyzer;

import hudson.Extension;
import hudson.model.PageDecorator;
import hudson.util.Secret;

/**
 * Page decorator to add "Analyze Error" functionality to console output pages.
 */
@Extension
public class ConsolePageDecorator extends PageDecorator {

    public ConsolePageDecorator() {
        super();
    }

    public boolean isAnalyzeErrorEnabled() {
        GlobalConfigurationImpl config = GlobalConfigurationImpl.get();

        // Must have analysis enabled and API key configured.
        if (!config.isEnableAnalysis()) {
            return false;
        }

        if (Secret.toString(config.getApiKey()).isBlank()) {
            return false;
        }

        // If no API URL is set, defaults will be used - that's valid
        // If API URL is set to a non-empty value, that's also valid
        return true;
    }
    
    /**
     * Helper method for JavaScript to check if a build is completed.
     * Returns true if the plugin is enabled (for JavaScript inclusion),
     * actual build status check is done in JavaScript.
     */
    public boolean isPluginActive() {
        return isAnalyzeErrorEnabled();
    }
}
