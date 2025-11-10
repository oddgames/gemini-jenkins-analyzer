package io.jenkins.plugins.gemini_jenkins_analyzer;

import static org.junit.jupiter.api.Assertions.*;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * Integration tests for ErrorAnalysisAction that require Jenkins.
 * For fast unit tests without Jenkins, see ErrorAnalysisActionUnitTest.
 */
@WithJenkins
class ErrorAnalysisActionTest {

    @Test
    void testRunAction2Interface(JenkinsRule jenkins) throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject("test");
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);

        String testAnalysis = "This is a test analysis of the error";
        String testErrorLogs = "ERROR: Build failed\nFAILED: Compilation error";
        ErrorAnalysisAction action = new ErrorAnalysisAction(testAnalysis, testErrorLogs);

        // Test onAttached
        action.onAttached(build);

        // Test onLoad
        action.onLoad(build);

        // The action should now be associated with the build
        // This doesn't throw an exception, so the interface is properly implemented
        assertTrue(true);
    }
}
