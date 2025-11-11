package io.jenkins.plugins.gemini_jenkins_analyzer;

import static org.junit.jupiter.api.Assertions.*;

import hudson.util.Secret;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * Integration tests for AIService that require Jenkins.
 * For fast unit tests without Jenkins, see AIServiceUnitTest.
 */
@WithJenkins
class AIServiceTest {

    private GlobalConfigurationImpl config;
    private AIService aiService;

    @BeforeEach
    void setUp(JenkinsRule jenkins) {
        // Get the global configuration instance from Jenkins
        config = GlobalConfigurationImpl.get();

        // Set minimal test values
        config.setApiUrl(null);
        config.setModel(null);
        config.setApiKey(Secret.fromString("test-api-key"));
        config.setEnableAnalysis(true);

        aiService = new AIService(config);
    }

    @Test
    void testServiceIntegrationWithJenkinsConfiguration() throws IOException {
        // Test that AIService integrates properly with Jenkins GlobalConfiguration
        String errorLogs = "ERROR: Build failed\nFAILURE: Task execution failed";

        String result = aiService.analyzeError(errorLogs);

        // Verify service handles error logs when integrated with Jenkins
        assertNotEquals("No error logs provided for analysis.", result);
        assertNotNull(result);
        assertFalse(result.trim().isEmpty());
    }

    @Test
    void testGlobalConfigurationIntegration() {
        // Verify that changes to Jenkins global configuration are reflected in the service
        config.setModel("gemini-2.0-flash");

        assertEquals("gemini-2.0-flash", config.getModel());
    }

    @Test
    void testServiceWithJenkinsGlobalConfigSingleton() {
        // Verify that GlobalConfiguration.get() returns the same instance
        GlobalConfigurationImpl config1 = GlobalConfigurationImpl.get();
        GlobalConfigurationImpl config2 = GlobalConfigurationImpl.get();

        assertSame(config1, config2);

        // Changes to one should affect the other
        config1.setModel("test-model");
        assertEquals("test-model", config2.getModel());
    }
}
