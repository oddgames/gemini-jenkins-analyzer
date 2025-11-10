package io.jenkins.plugins.gemini_jenkins_analyzer;

import static org.junit.jupiter.api.Assertions.*;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.TaskListener;
import hudson.util.Secret;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class ErrorAnalyzerTest {

    @Test
    void testErrorAnalyzerBasicFunctionality(JenkinsRule jenkins) throws Exception {
        ErrorAnalyzer errorAnalyzer = new ErrorAnalyzer();
        GlobalConfigurationImpl config = jenkins.getInstance().getDescriptorByType(GlobalConfigurationImpl.class);

        // Test when plugin is disabled
        config.setEnableAnalysis(false);

        FreeStyleProject project = jenkins.createFreeStyleProject();
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        TaskListener listener = jenkins.createTaskListener();

        // Should not throw exception when disabled
        assertDoesNotThrow(() -> {
            errorAnalyzer.analyzeError(build, listener, "ERROR", null, 100);
        });
    }

    @Test
    void testErrorAnalyzerWithInvalidConfig(JenkinsRule jenkins) throws Exception {
        ErrorAnalyzer errorAnalyzer = new ErrorAnalyzer();
        GlobalConfigurationImpl config = jenkins.getInstance().getDescriptorByType(GlobalConfigurationImpl.class);

        // Test with null API key
        config.setEnableAnalysis(true);
        config.setApiKey(null);

        FreeStyleProject project = jenkins.createFreeStyleProject();
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        TaskListener listener = jenkins.createTaskListener();

        // Should not throw exception with null API key
        assertDoesNotThrow(() -> {
            errorAnalyzer.analyzeError(build, listener, "ERROR", null, 100);
        });
    }

    @Test
    void testErrorAnalyzerTextMethods(JenkinsRule jenkins) throws Exception {
        ErrorAnalyzer errorAnalyzer = new ErrorAnalyzer();
        GlobalConfigurationImpl config = jenkins.getInstance().getDescriptorByType(GlobalConfigurationImpl.class);

        // Setup valid configuration
        config.setEnableAnalysis(true);
        config.setApiKey(Secret.fromString("test-api-key"));
        config.setProvider(AIProvider.GEMINI);
        config.setModel("gemini-2.0-flash");

        FreeStyleProject project = jenkins.createFreeStyleProject();
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);

        // Test with valid error text (will fail with API but should not throw exception)
        assertDoesNotThrow(() -> {
            String result = errorAnalyzer.analyzeErrorText("Build failed", build);
            // Result should be a non-empty error message since we're using a fake API key
            assertNotNull(result);
            assertFalse(result.isEmpty());
            // Should contain error message indicating communication failure
            assertTrue(result.contains("Failed to") || result.contains("ERROR"));
        });

        // Test with null input
        assertDoesNotThrow(() -> {
            String result = errorAnalyzer.analyzeErrorText(null, build);
            // Should return error message about no error text provided
            assertNotNull(result);
            assertEquals("No error text provided to explain.", result);
        });

        // Test with empty input
        assertDoesNotThrow(() -> {
            String result = errorAnalyzer.analyzeErrorText("", build);
            // Should return error message about no error text provided
            assertNotNull(result);
            assertEquals("No error text provided to explain.", result);
        });
    }
}
