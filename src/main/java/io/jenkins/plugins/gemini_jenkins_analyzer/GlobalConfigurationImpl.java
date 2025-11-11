package io.jenkins.plugins.gemini_jenkins_analyzer;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import hudson.util.Secret;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.interceptor.RequirePOST;
import org.jenkinsci.Symbol;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Global configuration for the plugin.
 */
@Extension
@Symbol("geminiAnalyzer")
public class GlobalConfigurationImpl extends GlobalConfiguration {

    private Secret apiKey;
    private String apiUrl;
    private String model;
    private boolean enableAnalysis = true;

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

    @Override
    public String getDisplayName() {
        return "Gemini Jenkins Analyzer Configuration";
    }

    /**
     * Method to test the AI API configuration.
     * This is called when the "Test Configuration" button is clicked.
     */
    @RequirePOST
    public FormValidation doTestConfiguration(@QueryParameter("apiKey") String apiKey,
                                                @QueryParameter("apiUrl") String apiUrl,
                                                @QueryParameter("model") String model) {
        Jenkins.get().checkPermission(Jenkins.ADMINISTER);

        // Validate only the provided parameters
        Secret testApiKeySecret = (apiKey != null) ? Secret.fromString(apiKey) : null;
        String testApiUrl = apiUrl != null ? apiUrl : "";
        String testModel = model != null ? model : "";

        try {
            // Validate API key is provided
            if (testApiKeySecret == null || testApiKeySecret.getPlainText().trim().isEmpty()) {
                return FormValidation.error("API Key is required");
            }

            GlobalConfigurationImpl tempConfig = new GlobalConfigurationImpl();
            tempConfig.setApiKey(testApiKeySecret);
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
