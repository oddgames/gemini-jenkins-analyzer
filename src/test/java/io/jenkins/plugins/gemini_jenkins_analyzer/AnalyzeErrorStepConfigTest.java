package io.jenkins.plugins.gemini_jenkins_analyzer;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class AnalyzeErrorStepConfigTest {

    @Test
    void testAnalyzeErrorStepWithParameters(JenkinsRule jenkins) throws Exception {
        // Create a test pipeline job
        WorkflowJob job = jenkins.createProject(WorkflowJob.class, "test-analyze-error-config");

        // Define a simple pipeline that uses the step with parameters
        String pipelineScript = "node {\n" +
                "    echo 'This is a test build'\n" +
                "    echo 'ERROR: Something went wrong'\n" +
                "    echo 'FAILED: Build failed'\n" +
                "    analyzeError logPattern: 'ERROR|FAILED', maxLines: 50\n" +
                "}";

        job.setDefinition(new CpsFlowDefinition(pipelineScript, true));

        // Run the build
        WorkflowRun build = jenkins.buildAndAssertSuccess(job);

        // Verify that the AnalyzeErrorStep was executed
        // Note: We can't test the actual AI analysis without a real API key
        // but we can verify the step executed without errors
        jenkins.assertLogContains("This is a test build", build);
    }
}
