package io.jenkins.plugins.gemini_jenkins_analyzer;

import java.util.logging.Level;
import java.util.logging.Logger;

import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.service.AiServices;
import java.time.Duration;

/**
 * Google Gemini-specific implementation of the AI service using LangChain4j.
 */
public class GeminiService extends BaseAIService {

    protected static final Logger LOGGER = Logger.getLogger(GeminiService.class.getName());

    // Simple interface for testing without structured output
    interface SimpleAssistant {
        String chat(String message);
    }

    public GeminiService(GlobalConfigurationImpl config) {
        super(config);
    }

    @Override
    protected Assistant createAssistant() {
        String baseUrl = determineBaseUrl("Gemini");

        // Use configured model or default to gemini-2.0-flash
        String modelName = (config.getModel() != null && !config.getModel().trim().isEmpty())
            ? config.getModel()
            : "gemini-2.0-flash";

        var model = GoogleAiGeminiChatModel.builder()
            .baseUrl(baseUrl) // Will use default if null
            .apiKey(config.getApiKey().getPlainText())
            .modelName(modelName)
            .temperature(0.3)
            .timeout(Duration.ofSeconds(90)) // 90 second timeout for error analysis
            .logRequests(LOGGER.getLevel() == Level.FINE)
            .logResponses(LOGGER.getLevel() == Level.FINE)
            .build();

        return AiServices.create(Assistant.class, model);
    }

    /**
     * Test the connection without structured output for configuration validation.
     * @return simple text response
     */
    public String testConnection() {
        try {
            String baseUrl = determineBaseUrl("Gemini");
            String modelName = (config.getModel() != null && !config.getModel().trim().isEmpty())
                ? config.getModel()
                : "gemini-2.0-flash";

            LOGGER.info("Testing connection with model: " + modelName);

            var model = GoogleAiGeminiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(config.getApiKey().getPlainText())
                .modelName(modelName)
                .temperature(0.3)
                .timeout(Duration.ofSeconds(30)) // Shorter timeout for test
                .logRequests(true)
                .logResponses(true)
                .build();

            SimpleAssistant assistant = AiServices.create(SimpleAssistant.class, model);
            String response = assistant.chat("Reply with 'OK' if you can read this.");
            LOGGER.info("Test connection successful. Response: " + response);
            return response;
        } catch (Exception e) {
            LOGGER.severe("Test connection failed: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Connection test failed: " + e.getMessage(), e);
        }
    }
}
