package io.jenkins.plugins.gemini_jenkins_analyzer;

import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;

/**
 * Base class for AI service implementations using LangChain4j.
 * Provides common functionality for different AI providers.
 */
public abstract class BaseAIService {

    protected static final Logger LOGGER = Logger.getLogger(BaseAIService.class.getName());

    protected final GlobalConfigurationImpl config;

    public BaseAIService(GlobalConfigurationImpl config) {
        this.config = config;
    }

    interface Assistant {
        String chat(String message);
    }

    /**
     * Explain error logs using the configured AI provider.
     * @param errorLogs the error logs to explain
     * @return the AI analysis
     * @throws IOException if there's a communication error
     */
    public String analyzeError(String errorLogs) throws IOException {
        Assistant assistant;

        if (StringUtils.isBlank(errorLogs)) {
            return "No error logs provided for analysis.";
        }

        // Validate API key
        if (config.getApiKey() == null || StringUtils.isBlank(config.getApiKey().getPlainText())) {
            return "Unable to create assistant: API key is not configured.";
        }

        try {
           assistant = createAssistant();
        } catch (Exception e) {
            LOGGER.severe("Failed to create assistant: " + e.getMessage());
            e.printStackTrace();
            return "Unable to create assistant: " + e.getMessage() + ". Please check your API key and model configuration.";
        }

        // Use PromptTemplate for dynamic prompt creation
        PromptTemplate promptTemplate = PromptTemplate.from(
            "Senior dev: analyze this failure. Skip obvious stuff.\n\n"
            + "{{errorLogs}}\n\n"
            + "1. Root cause (environment/dependency/config issues only)\n"
            + "2. Fix (1-2 sentences max)\n"
            + "3. Prevention (if non-trivial)\n\n"
            + "MAX 5 LINES TOTAL. Plain text only."
        );

        Map<String, Object> variables = new HashMap<>();
        variables.put("errorLogs", errorLogs);
        Prompt prompt = promptTemplate.apply(variables);

        try {
            LOGGER.info("Sending request to AI service...");
            String analysis = assistant.chat(prompt.text());
            LOGGER.info("Received response from AI service");
            return analysis != null && !analysis.trim().isEmpty() ? analysis : "No response received from AI service.";
        } catch (Exception e) {
            LOGGER.severe("AI API request failed: " + e.getMessage());
            e.printStackTrace();
            return "Failed to communicate with AI service: " + e.getMessage();
        }
    }

    /**
     * Determines the base URL to use for the AI service.
     * Returns the custom URL if provided and not empty, otherwise returns null
     * to let the service use its default endpoint.
     * 
     * @param providerName the name of the provider (for logging purposes)
     * @return the base URL to use, or null to use the default
     */
    protected String determineBaseUrl(String providerName) {
        String baseUrl = (config.getApiUrl() != null && !config.getApiUrl().trim().isEmpty()) 
            ? config.getApiUrl() 
            : null;
        
        if (baseUrl != null) {
            LOGGER.info("Using custom " + providerName + " API URL: " + baseUrl);
        }
        
        return baseUrl;
    }

    protected abstract Assistant createAssistant();
}
