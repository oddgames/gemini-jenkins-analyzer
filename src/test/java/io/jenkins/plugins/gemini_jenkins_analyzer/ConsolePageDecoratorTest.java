package io.jenkins.plugins.gemini_jenkins_analyzer;

import static org.junit.jupiter.api.Assertions.*;

import hudson.util.Secret;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class ConsolePageDecoratorTest {

    private ConsolePageDecorator decorator;
    private GlobalConfigurationImpl config;

    @BeforeEach
    void setUp(JenkinsRule jenkins) {
        decorator = new ConsolePageDecorator();
        config = GlobalConfigurationImpl.get();

        // Reset to default state
        config.setEnableAnalysis(true);
        config.setApiKey(Secret.fromString("test-api-key"));
        config.setModel("gpt-3.5-turbo");
    }

    @Test
    void testDecoratorCreation() {
        assertNotNull(decorator);
    }

    @Test
    void testIsAnalyzeErrorEnabledWithValidConfig() {
        // With valid configuration, should return true
        assertTrue(decorator.isAnalyzeErrorEnabled());
    }

    @Test
    void testIsAnalyzeErrorEnabledWhenDisabled() {
        config.setEnableAnalysis(false);

        // Should return false when analysis is disabled
        assertFalse(decorator.isAnalyzeErrorEnabled());
    }

    @Test
    void testIsAnalyzeErrorEnabledWithNullApiKey() {
        config.setApiKey(null);

        // Should return false when API key is null
        assertFalse(decorator.isAnalyzeErrorEnabled());
    }

    @Test
    void testIsAnalyzeErrorEnabledWithEmptyApiKey() {
        config.setApiKey(Secret.fromString(""));

        // Should return false when API key is empty
        assertFalse(decorator.isAnalyzeErrorEnabled());
    }

    @Test
    void testIsAnalyzeErrorEnabledWithBlankApiKey() {
        config.setApiKey(Secret.fromString("   "));

        // Should return false when API key is blank
        assertFalse(decorator.isAnalyzeErrorEnabled());
    }

    @Test
    void testExtensionAnnotation() {
        // Test that the decorator is properly annotated as an Extension
        assertTrue(decorator.getClass().isAnnotationPresent(hudson.Extension.class));
    }

    @Test
    void testInheritance() {
        // Test that the decorator extends PageDecorator
        assertTrue(decorator instanceof hudson.model.PageDecorator);
    }

    @Test
    void testMultipleConditionsDisabled() {
        // Test when multiple conditions are not met
        config.setEnableAnalysis(false);
        config.setApiKey(null);

        assertFalse(decorator.isAnalyzeErrorEnabled());
    }

    @Test
    void testPartiallyValidConfig() {
        // Test with some valid and some invalid settings
        config.setEnableAnalysis(true);
        config.setApiKey(null);

        assertFalse(decorator.isAnalyzeErrorEnabled());
    }

    @Test
    void testAnotherPartiallyValidConfig() {
        // Test with different combination
        config.setEnableAnalysis(true);
        config.setApiKey(Secret.fromString("")); // Invalid key

        assertFalse(decorator.isAnalyzeErrorEnabled());
    }

    @Test
    void testConfigurationChanges() {
        // Test that the decorator responds to configuration changes
        assertTrue(decorator.isAnalyzeErrorEnabled());

        config.setEnableAnalysis(false);
        assertFalse(decorator.isAnalyzeErrorEnabled());

        config.setEnableAnalysis(true);
        assertTrue(decorator.isAnalyzeErrorEnabled());

        config.setApiKey(null);
        assertFalse(decorator.isAnalyzeErrorEnabled());
    }

    @Test
    void testEdgeCaseApiKey() {
        // Test with various edge cases for API key
        config.setApiKey(Secret.fromString("a")); // Very short key
        assertTrue(decorator.isAnalyzeErrorEnabled()); // Should still work, validation is elsewhere

        config.setApiKey(Secret.fromString("\t\n\r ")); // Whitespace only
        assertFalse(decorator.isAnalyzeErrorEnabled());
    }

    @Test
    void testConsistentBehavior() {
        // Test that multiple calls return consistent results
        boolean result1 = decorator.isAnalyzeErrorEnabled();
        boolean result2 = decorator.isAnalyzeErrorEnabled();
        boolean result3 = decorator.isAnalyzeErrorEnabled();

        assertEquals(result1, result2);
        assertEquals(result2, result3);
    }

    @Test
    void testValidMinimalConfiguration() {
        // Test with minimal valid configuration
        config.setEnableAnalysis(true);
        config.setApiKey(Secret.fromString("k"));

        assertTrue(decorator.isAnalyzeErrorEnabled());
    }

    @Test
    void testIsPluginActive() {
        // isPluginActive should return the same as isAnalyzeErrorEnabled
        assertEquals(decorator.isAnalyzeErrorEnabled(), decorator.isPluginActive());

        config.setEnableAnalysis(false);
        assertEquals(decorator.isAnalyzeErrorEnabled(), decorator.isPluginActive());

        config.setEnableAnalysis(true);
        config.setApiKey(null);
        assertEquals(decorator.isAnalyzeErrorEnabled(), decorator.isPluginActive());
    }
}
