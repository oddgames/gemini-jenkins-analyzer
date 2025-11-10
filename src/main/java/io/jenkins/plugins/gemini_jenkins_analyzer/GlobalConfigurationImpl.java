package io.jenkins.plugins.gemini_jenkins_analyzer;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.interceptor.RequirePOST;
import org.jenkinsci.Symbol;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Global configuration for the plugin.
 */
@Extension
@Symbol("geminiAnalyzer")
public class GlobalConfigurationImpl extends GlobalConfiguration {

    private Secret apiKey;
    private AIProvider provider = AIProvider.GEMINI;
    private String apiUrl;
    private String model;
    private boolean enableAnalysis = true;
    private List<String> errorPatterns = new ArrayList<>();

    public GlobalConfigurationImpl() {
        load();
    }

    /**
     * Get the singleton instance of GlobalConfigurationImpl.
     * @return the GlobalConfigurationImpl instance
     */
    public static GlobalConfigurationImpl get() {
        return Jenkins.get().getDescriptorByType(GlobalConfigurationImpl.class);
    }

    @Override
    public boolean configure(StaplerRequest2 req, JSONObject json) throws Descriptor.FormException {
        try {
            // Validate required fields before binding
            if (json.has("enableAnalysis")) {
                this.enableAnalysis = json.getBoolean("enableAnalysis");
            }

            if (json.has("provider")) {
                String providerStr = json.getString("provider");
                try {
                    this.provider = AIProvider.valueOf(providerStr);
                } catch (IllegalArgumentException e) {
                    throw new Descriptor.FormException("Invalid provider: " + providerStr, "provider");
                }
            }

            if (json.has("apiKey")) {
                String apiKeyStr = json.getString("apiKey");
                this.apiKey = Secret.fromString(apiKeyStr);
            }

            if (json.has("apiUrl")) {
                this.apiUrl = json.getString("apiUrl");
            }

            if (json.has("model")) {
                this.model = json.getString("model");
            }

            if (json.has("errorPatterns")) {
                this.errorPatterns = new ArrayList<>();
                Object patternsObj = json.get("errorPatterns");
                if (patternsObj instanceof net.sf.json.JSONArray) {
                    net.sf.json.JSONArray patternsArray = (net.sf.json.JSONArray) patternsObj;
                    for (Object obj : patternsArray) {
                        if (obj instanceof String && !((String) obj).trim().isEmpty()) {
                            this.errorPatterns.add(((String) obj).trim());
                        }
                    }
                } else if (patternsObj instanceof String && !((String) patternsObj).trim().isEmpty()) {
                    this.errorPatterns.add(((String) patternsObj).trim());
                }
            }

            save();
            return true;
        } catch (Exception e) {
            Logger.getLogger(GlobalConfigurationImpl.class.getName()).log(Level.SEVERE, "Configuration failed", e);
            throw new Descriptor.FormException("Configuration failed: " + e.getMessage(), e, "");
        }
    }

    // Getters and setters
    public Secret getApiKey() {
        return apiKey;
    }

    @DataBoundSetter
    public void setApiKey(Secret apiKey) {
        this.apiKey = apiKey;
    }

    public AIProvider getProvider() {
        return provider != null ? provider : AIProvider.GEMINI;
    }

    @DataBoundSetter
    public void setProvider(AIProvider provider) {
        this.provider = provider;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    @DataBoundSetter
    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public String getModel() {
        return model;
    }

    /**
     * Get the raw configured model without defaults, used for validation.
     */
    public String getRawModel() {
        return model;
    }

    @DataBoundSetter
    public void setModel(String model) {
        this.model = model;
    }

    public boolean isEnableAnalysis() {
        return enableAnalysis;
    }

    @DataBoundSetter
    public void setEnableAnalysis(boolean enableAnalysis) {
        this.enableAnalysis = enableAnalysis;
    }

    public List<String> getErrorPatterns() {
        return errorPatterns != null ? errorPatterns : new ArrayList<>();
    }

    @DataBoundSetter
    public void setErrorPatterns(List<String> errorPatterns) {
        this.errorPatterns = errorPatterns != null ? errorPatterns : new ArrayList<>();
    }

    @Override
    public String getDisplayName() {
        return "Analyze Error Plugin Configuration";
    }

    /**
     * Get all available AI providers for the dropdown.
     */
    public AIProvider[] getProviderValues() {
        return AIProvider.values();
    }

    /**
     * Populate the provider dropdown items for the UI.
     */
    @RequirePOST
    public ListBoxModel doFillProviderItems() {
        Jenkins.get().checkPermission(Jenkins.ADMINISTER);

        ListBoxModel model = new ListBoxModel();
        AIProvider currentProvider = getProvider(); // Get the current provider

        for (AIProvider p : AIProvider.values()) {
            model.add(new ListBoxModel.Option(
                p.getDisplayName(),          // display name
                p.name(),                    // actual value
                p == currentProvider         // is selected
            ));
        }

        return model;
    }

    /**
     * Populate the error pattern preset dropdown items for the UI.
     */
    @RequirePOST
    public ListBoxModel doFillErrorPatternPresetItems() {
        Jenkins.get().checkPermission(Jenkins.ADMINISTER);

        ListBoxModel model = new ListBoxModel();
        for (ErrorPatternPreset preset : ErrorPatternPreset.values()) {
            model.add(new ListBoxModel.Option(
                preset.getDisplayName(),
                preset.name()
            ));
        }
        return model;
    }

    /**
     * Get the patterns for a specific preset via AJAX.
     * This is called when the user selects a preset from the dropdown.
     */
    @RequirePOST
    public FormValidation doGetPresetPatterns(@QueryParameter("preset") String presetName) {
        Jenkins.get().checkPermission(Jenkins.ADMINISTER);

        try {
            ErrorPatternPreset preset = ErrorPatternPreset.fromString(presetName);
            List<String> patterns = preset.getPatterns();

            if (patterns.isEmpty()) {
                return FormValidation.ok("");
            }

            // Return patterns as newline-separated string
            return FormValidation.ok(String.join("\n", patterns));
        } catch (Exception e) {
            Logger.getLogger(GlobalConfigurationImpl.class.getName()).log(Level.WARNING, "Failed to get preset patterns", e);
            return FormValidation.error("Failed to get preset patterns: " + e.getMessage());
        }
    }

    /**
     * Method to test the AI API configuration.
     * This is called when the "Test Configuration" button is clicked.
     */
    @RequirePOST
    public FormValidation doTestConfiguration(@QueryParameter("apiKey") String apiKey,
                                                @QueryParameter("provider") String provider,
                                                @QueryParameter("apiUrl") String apiUrl,
                                                @QueryParameter("model") String model) {
        Jenkins.get().checkPermission(Jenkins.ADMINISTER);

        // Validate only the provided parameters
        Secret testApiKeySecret = (apiKey != null) ? Secret.fromString(apiKey) : null;
        AIProvider testProvider = null;
        if (provider != null && !provider.isEmpty()) {
            try {
                testProvider = AIProvider.valueOf(provider);
            } catch (IllegalArgumentException e) {
                return FormValidation.error("Invalid provider: " + provider);
            }
        }
        String testApiUrl = apiUrl != null ? apiUrl : "";
        String testModel = model != null ? model : "";

        try {
            // Validate API key is provided
            if (testApiKeySecret == null || testApiKeySecret.getPlainText().trim().isEmpty()) {
                return FormValidation.error("API Key is required");
            }

            GlobalConfigurationImpl tempConfig = new GlobalConfigurationImpl();
            tempConfig.setApiKey(testApiKeySecret);
            if (testProvider != null) {
                tempConfig.setProvider(testProvider);
            }
            tempConfig.setApiUrl(testApiUrl);
            tempConfig.setModel(testModel);

            AIService aiService = new AIService(tempConfig);
            String testResponse = aiService.testConnection();

            if (testResponse != null && !testResponse.trim().isEmpty()) {
                return FormValidation.ok("Configuration test successful! API connection is working properly.\n\nModel: "
                    + (testModel != null && !testModel.isEmpty() ? testModel : "gemini-2.0-flash (default)")
                    + "\nResponse: " + testResponse);
            } else {
                return FormValidation.error("Connection failed: No response received from AI service.");
            }

        } catch (Exception e) {
            Logger.getLogger(GlobalConfigurationImpl.class.getName()).log(Level.WARNING, "Configuration test failed", e);
            String errorMsg = e.getMessage();
            if (errorMsg != null && errorMsg.contains("API key not valid")) {
                return FormValidation.error("Invalid API Key. Please check your Gemini API key.");
            } else if (errorMsg != null && errorMsg.contains("PERMISSION_DENIED")) {
                return FormValidation.error("Permission denied. Please check your API key and ensure the Gemini API is enabled.");
            } else if (errorMsg != null && errorMsg.contains("model")) {
                return FormValidation.error("Invalid model: " + errorMsg);
            } else {
                return FormValidation.error("Test failed: " + (errorMsg != null ? errorMsg : "Unknown error"));
            }
        }
    }
}
