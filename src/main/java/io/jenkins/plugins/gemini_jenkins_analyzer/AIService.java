package io.jenkins.plugins.gemini_jenkins_analyzer;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Service class for communicating with AI APIs.
 * Factory that creates appropriate AI service implementation based on the configured provider.
 */
public class AIService {

    private static final Logger LOGGER = Logger.getLogger(AIService.class.getName());

    private final GeminiService delegate;

    public AIService(GlobalConfigurationImpl config) {
        this.delegate = new GeminiService(config);
    }

    /**
     * Explain error logs using the configured AI provider.
     * @param errorLogs the error logs to explain
     * @return the AI analysis
     * @throws IOException if there's a communication error
     */
    public String analyzeError(String errorLogs) throws IOException {
        return delegate.analyzeError(errorLogs);
    }

    /**
     * Test the connection to the AI service.
     * @return simple text response
     */
    public String testConnection() {
        return delegate.testConnection();
    }
}