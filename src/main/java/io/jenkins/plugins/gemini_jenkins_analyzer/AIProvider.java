package io.jenkins.plugins.gemini_jenkins_analyzer;

/**
 * Enum representing the supported AI providers.
 */
public enum AIProvider {
    GEMINI("Google Gemini", "gemini-2.0-flash");

    private final String displayName;
    private final String defaultModel;

    AIProvider(String displayName, String defaultModel) {
        this.displayName = displayName;
        this.defaultModel = defaultModel;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDefaultModel() {
        return defaultModel;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
