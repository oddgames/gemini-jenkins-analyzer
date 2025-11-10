package io.jenkins.plugins.gemini_jenkins_analyzer;

import static org.junit.jupiter.api.Assertions.*;

import hudson.util.FormValidation;
import hudson.util.Secret;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * Integration tests for GlobalConfigurationImpl that require Jenkins.
 * Consolidated from 19 tests to 8 tests for better efficiency.
 */
@WithJenkins
class GlobalConfigurationImplTest {

    // Don't store config in a field - get it fresh in each test method
    // because Jenkins initialization happens per-test

    @Test
    void testGetSingletonInstance(JenkinsRule jenkins) {
        GlobalConfigurationImpl instance1 = GlobalConfigurationImpl.get();
        GlobalConfigurationImpl instance2 = GlobalConfigurationImpl.get();

        assertNotNull(instance1);
        assertNotNull(instance2);
        assertSame(instance1, instance2);
    }

    @Test
    void testDefaultValues(JenkinsRule jenkins) {
        GlobalConfigurationImpl config = GlobalConfigurationImpl.get();
        config.setApiKey(null);
        config.setModel(null);

        assertNull(config.getModel());
        assertTrue(config.isEnableAnalysis());
        assertNull(config.getApiKey());
    }

    @Test
    void testSettersAndGetters(JenkinsRule jenkins) {
        GlobalConfigurationImpl config = GlobalConfigurationImpl.get();

        // Test API key
        Secret testSecret = Secret.fromString("test-api-key");
        config.setApiKey(testSecret);
        assertEquals(testSecret, config.getApiKey());

        // Test model
        config.setModel("test-model");
        assertEquals("test-model", config.getModel());

        // Test enable analysis toggle
        config.setEnableAnalysis(false);
        assertFalse(config.isEnableAnalysis());
        config.setEnableAnalysis(true);
        assertTrue(config.isEnableAnalysis());

        // Test provider
        config.setProvider(AIProvider.GEMINI);
        assertEquals(AIProvider.GEMINI, config.getProvider());
    }

    @Test
    void testEdgeCases(JenkinsRule jenkins) {
        GlobalConfigurationImpl config = GlobalConfigurationImpl.get();

        // Null API key
        config.setApiKey(null);
        assertNull(config.getApiKey());

        // Empty API key
        Secret emptySecret = Secret.fromString("");
        config.setApiKey(emptySecret);
        assertEquals(emptySecret, config.getApiKey());

        // Null model
        config.setModel(null);
        assertNull(config.getRawModel());
        assertNull(config.getModel());

        // Empty model
        config.setModel("");
        assertEquals("", config.getRawModel());
        assertEquals("", config.getModel());

        // Null provider defaults to GEMINI
        config.setProvider(null);
        assertEquals(AIProvider.GEMINI, config.getProvider());
    }

    @Test
    void testConfiguration_MissingApiKey(JenkinsRule jenkins) {
        GlobalConfigurationImpl config = GlobalConfigurationImpl.get();
        FormValidation result = config.doTestConfiguration("", "GEMINI", "", "gemini-2.0-flash");

        assertEquals(FormValidation.Kind.ERROR, result.kind);
        assertTrue(result.getMessage().contains("API Key is required"),
            "Error message should indicate API key is required. Got: " + result.getMessage());
    }

    @Test
    void testConfiguration_NullApiKey(JenkinsRule jenkins) {
        GlobalConfigurationImpl config = GlobalConfigurationImpl.get();
        FormValidation result = config.doTestConfiguration(null, "GEMINI", "", "gemini-2.0-flash");

        assertEquals(FormValidation.Kind.ERROR, result.kind);
        assertTrue(result.getMessage().contains("API Key is required"));
    }

    @Test
    void testConfiguration_InvalidProvider(JenkinsRule jenkins) {
        GlobalConfigurationImpl config = GlobalConfigurationImpl.get();
        FormValidation result = config.doTestConfiguration("test-key", "INVALID_PROVIDER", "", "gemini-2.0-flash");

        assertEquals(FormValidation.Kind.ERROR, result.kind);
        assertTrue(result.getMessage().contains("Invalid provider"));
    }

    @Test
    void testConfiguration_WithInvalidApiKey(JenkinsRule jenkins) {
        GlobalConfigurationImpl config = GlobalConfigurationImpl.get();
        // This will fail to connect but shouldn't crash
        FormValidation result = config.doTestConfiguration("invalid-key-123", "GEMINI", "", "gemini-2.0-flash");

        // Should return an error (not crash)
        assertNotNull(result);
        assertEquals(FormValidation.Kind.ERROR, result.kind);
    }

    /**
     * Integration test with real API key.
     * Set GEMINI_API_KEY environment variable to run this test.
     * Example: export GEMINI_API_KEY="your-api-key-here"
     */
    @Test
    void testConfiguration_WithRealApiKey(JenkinsRule jenkins) {
        GlobalConfigurationImpl config = GlobalConfigurationImpl.get();
        String apiKey = System.getenv("GEMINI_API_KEY");

        if (apiKey == null || apiKey.trim().isEmpty()) {
            System.out.println("Skipping real API test - set GEMINI_API_KEY environment variable to enable");
            return;
        }

        FormValidation result = config.doTestConfiguration(
            apiKey,
            "GEMINI",
            "", // Use default API URL
            "gemini-2.0-flash"
        );

        assertEquals(FormValidation.Kind.OK, result.kind,
            "Test should succeed with valid API key. Error: " + result.getMessage());
        assertTrue(result.getMessage().contains("successful"),
            "Success message should contain 'successful'. Got: " + result.getMessage());
    }

    @Test
    void testPersistence(JenkinsRule jenkins) {
        GlobalConfigurationImpl config = GlobalConfigurationImpl.get();

        config.setApiKey(Secret.fromString("test-key"));
        config.setProvider(AIProvider.GEMINI);
        config.setModel("test-model");
        config.setEnableAnalysis(false);

        config.save();

        assertEquals("test-key", config.getApiKey().getPlainText());
        assertEquals(AIProvider.GEMINI, config.getProvider());
        assertEquals("test-model", config.getModel());
        assertFalse(config.isEnableAnalysis());

        config.load();
        assertEquals(AIProvider.GEMINI, config.getProvider());
    }

    @Test
    void testDisplayName(JenkinsRule jenkins) {
        GlobalConfigurationImpl config = GlobalConfigurationImpl.get();
        String displayName = config.getDisplayName();
        assertNotNull(displayName);
        assertEquals("Analyze Error Plugin Configuration", displayName);
    }

    @Test
    void testSingletonBehavior(JenkinsRule jenkins) {
        GlobalConfigurationImpl config1 = GlobalConfigurationImpl.get();
        GlobalConfigurationImpl config2 = GlobalConfigurationImpl.get();

        config1.setModel("test-model-1");

        assertSame(config1, config2);
        assertEquals("test-model-1", config2.getModel());
    }
}
