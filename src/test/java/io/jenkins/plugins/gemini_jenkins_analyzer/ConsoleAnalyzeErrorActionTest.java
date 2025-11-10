package io.jenkins.plugins.gemini_jenkins_analyzer;

import static org.junit.jupiter.api.Assertions.*;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * Integration tests for ConsoleAnalyzeErrorAction that require Jenkins.
 * These tests focus on essential integration behavior.
 */
@WithJenkins
class ConsoleAnalyzeErrorActionTest {

    @Test
    void testBasicFunctionality(JenkinsRule jenkins) throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject("test");
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        ConsoleAnalyzeErrorAction action = new ConsoleAnalyzeErrorAction(build);

        assertNotNull(action);
        assertEquals(build, action.getRun());
        assertNull(action.getIconFileName()); // Should be null for AJAX functionality
        assertNull(action.getDisplayName()); // Should be null for AJAX functionality
        assertEquals("console-analyzer-error", action.getUrlName());
    }

    @Test
    void testExistingAnalysisDetection(JenkinsRule jenkins) throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject("test");
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);

        // Initially no analysis should exist
        ErrorAnalysisAction existingAction = build.getAction(ErrorAnalysisAction.class);
        assertNull(existingAction);

        // Add an analysis
        ErrorAnalysisAction action = new ErrorAnalysisAction("Test analysis", "Error logs");
        build.addAction(action);

        // Now analysis should exist and be valid
        ErrorAnalysisAction retrievedAction = build.getAction(ErrorAnalysisAction.class);
        assertNotNull(retrievedAction);
        assertTrue(retrievedAction.hasValidAnalysis());
        assertEquals("Test analysis", retrievedAction.getAnalysis());
    }

    @Test
    void testExistingAnalysisDetectionWithInvalidAnalysis(JenkinsRule jenkins) throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject("test");
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);

        // Add an invalid analysis (null content)
        ErrorAnalysisAction invalidAction = new ErrorAnalysisAction(null, "Error logs");
        build.addAction(invalidAction);

        // Analysis exists but should not be valid
        ErrorAnalysisAction retrievedAction = build.getAction(ErrorAnalysisAction.class);
        assertNotNull(retrievedAction);
        assertFalse(retrievedAction.hasValidAnalysis());
    }

    @Test
    void testExistingAnalysisDetectionWithEmptyAnalysis(JenkinsRule jenkins) throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject("test");
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);

        // Add an empty analysis
        ErrorAnalysisAction emptyAction = new ErrorAnalysisAction("", "Error logs");
        build.addAction(emptyAction);

        // Analysis exists but should not be valid
        ErrorAnalysisAction retrievedAction = build.getAction(ErrorAnalysisAction.class);
        assertNotNull(retrievedAction);
        assertFalse(retrievedAction.hasValidAnalysis());
    }

    @Test
    void testActionWithValidAnalysis(JenkinsRule jenkins) throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject("test");
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);

        // Add a valid analysis
        ErrorAnalysisAction validAction = new ErrorAnalysisAction("Valid analysis", "Error logs");
        build.addAction(validAction);

        ErrorAnalysisAction retrieved = build.getAction(ErrorAnalysisAction.class);
        assertNotNull(retrieved);
        assertTrue(retrieved.hasValidAnalysis());
        assertNotNull(retrieved.getFormattedTimestamp());
    }

    @Test
    void testGetRun(JenkinsRule jenkins) throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject("test");
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        ConsoleAnalyzeErrorAction action = new ConsoleAnalyzeErrorAction(build);

        assertEquals(build, action.getRun());
    }

    @Test
    void testBuildStatusCheck(JenkinsRule jenkins) throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject("test");
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        ConsoleAnalyzeErrorAction action = new ConsoleAnalyzeErrorAction(build);

        // Test that the action can access build status through the run
        assertNotNull(action.getRun());
        // Build should not be building since it's completed
        assertFalse(action.getRun().isBuilding());
    }
}
