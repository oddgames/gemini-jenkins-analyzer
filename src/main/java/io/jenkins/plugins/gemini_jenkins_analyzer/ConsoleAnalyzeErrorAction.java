package io.jenkins.plugins.gemini_jenkins_analyzer;

import com.fasterxml.jackson.databind.ObjectMapper;
import hudson.model.Action;
import hudson.model.Result;
import hudson.model.Run;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.StaplerResponse2;
import org.kohsuke.stapler.interceptor.RequirePOST;

/**
 * Action to add "Analyze Error" functionality to console output pages.
 * This action needs to be manually added to builds.
 */
public class ConsoleAnalyzeErrorAction implements Action {

    private static final Logger LOGGER = Logger.getLogger(ConsoleAnalyzeErrorAction.class.getName());

    private final Run<?, ?> run;

    public ConsoleAnalyzeErrorAction(Run<?, ?> run) {
        this.run = run;
    }

    @Override
    public String getIconFileName() {
        return null; // No icon in sidebar - this is for AJAX functionality only
    }

    @Override
    public String getDisplayName() {
        return null; // No display name in sidebar
    }

    @Override
    public String getUrlName() {
        return "console-analyzer-error";
    }

    /**
     * AJAX endpoint to explain error from console output.
     * Called via JavaScript from the console output page.
     */
    @RequirePOST
    public void doExplainConsoleError(StaplerRequest2 req, StaplerResponse2 rsp) throws ServletException, IOException {
        try {
            run.checkPermission(hudson.model.Item.READ);

            // Check if user wants to force a new analysis
            boolean forceNew = "true".equals(req.getParameter("forceNew"));

            // Check if an analysis already exists
            ErrorAnalysisAction existingAction = run.getAction(ErrorAnalysisAction.class);
            if (!forceNew && existingAction != null && existingAction.hasValidAnalysis()) {
                // Return existing analysis with a flag indicating it's cached
                writeJsonResponse(rsp, createCachedResponse(existingAction.getAnalysis()));
                return;
            }

            // Optionally allow maxLines as a parameter, default to 200
            int maxLines = 200;
            String maxLinesParam = req.getParameter("maxLines");
            if (maxLinesParam != null) {
                try { maxLines = Integer.parseInt(maxLinesParam); } catch (NumberFormatException ignore) {}
            }

            // Use the new filtering method that applies regex patterns and bottom-up parsing
            ErrorAnalyzer explainer = new ErrorAnalyzer();
            String analysis = explainer.analyzeErrorWithFiltering(run, maxLines);

            if (analysis != null && !analysis.trim().isEmpty()) {
                // Fetch logs for storage (can be filtered or unfiltered depending on config)
                java.util.List<String> logLines = run.getLog(maxLines);
                String errorText = String.join("\n", logLines);

                // Save the analysis as a build action (like the sidebar functionality)
                ErrorAnalysisAction action = new ErrorAnalysisAction(analysis, errorText);
                run.addOrReplaceAction(action);
                run.save();

                writeJsonResponse(rsp, analysis);
            } else {
                writeJsonResponse(rsp, "Error: Could not generate analysis. Please check your AI API configuration.");
            }
        } catch (Exception e) {
            LOGGER.severe("=== EXPLAIN ERROR REQUEST FAILED ===");
            LOGGER.severe("Error explaining console error: " + e.getMessage());
            writeJsonResponse(rsp, "Error: " + e.getMessage());
        }
    }

    /**
     * AJAX endpoint to get filtered error logs before sending to AI.
     * Returns the filtered logs based on configured patterns.
     */
    @RequirePOST
    public void doGetFilteredLogs(StaplerRequest2 req, StaplerResponse2 rsp) throws ServletException, IOException {
        try {
            run.checkPermission(hudson.model.Item.READ);

            int maxLines = 200;
            String maxLinesParam = req.getParameter("maxLines");
            if (maxLinesParam != null) {
                try { maxLines = Integer.parseInt(maxLinesParam); } catch (NumberFormatException ignore) {}
            }

            // Extract filtered logs using the same logic as analysis
            ErrorAnalyzer analyzer = new ErrorAnalyzer();
            String filteredLogs = analyzer.extractFilteredLogs(run, maxLines);

            writeJsonResponse(rsp, filteredLogs != null ? filteredLogs : "");
        } catch (Exception e) {
            LOGGER.severe("Error getting filtered logs: " + e.getMessage());
            rsp.setStatus(500);
            writeJsonResponse(rsp, "Error: " + e.getMessage());
        }
    }

    /**
     * AJAX endpoint to check if an analysis already exists.
     * Returns JSON with hasAnalysis boolean and timestamp if it exists.
     */
    @RequirePOST
    public void doCheckExistingAnalysis(StaplerRequest2 req, StaplerResponse2 rsp) throws ServletException, IOException {
        try {
            run.checkPermission(hudson.model.Item.READ);

            ErrorAnalysisAction existingAction = run.getAction(ErrorAnalysisAction.class);
            boolean hasAnalysis = existingAction != null && existingAction.hasValidAnalysis();

            rsp.setContentType("application/json");
            rsp.setCharacterEncoding("UTF-8");
            PrintWriter writer = rsp.getWriter();

            if (hasAnalysis && existingAction != null) {
                String response = String.format(
                    "{\"hasAnalysis\": true, \"timestamp\": \"%s\"}",
                    existingAction.getFormattedTimestamp()
                );
                writer.write(response);
            } else {
                writer.write("{\"hasAnalysis\": false}");
            }

            writer.flush();
        } catch (Exception e) {
            LOGGER.severe("Error checking existing analysis: " + e.getMessage());
            rsp.setStatus(500);
        }
    }

    /**
     * AJAX endpoint to check build status.
     * Returns JSON with buildingStatus to determine if button should be shown. 0 - SUCCESS, 1 - RUNNING, 2 - FINISHED and FAILURE
     */
    @RequirePOST
    public void doCheckBuildStatus(StaplerRequest2 req, StaplerResponse2 rsp) throws ServletException, IOException {
        try {
            run.checkPermission(hudson.model.Item.READ);
            
            Integer buildingStatus = run.isBuilding() ? 1 : 0;

            if (buildingStatus == 0) {
                Result result = run.getResult();
                if (result == Result.SUCCESS) {
                    buildingStatus = 0;
                } else {
                    buildingStatus = 2;
                }
            }
            
            rsp.setContentType("application/json");
            rsp.setCharacterEncoding("UTF-8");
            PrintWriter writer = rsp.getWriter();
            
            String response = String.format("{\"buildingStatus\": %s}", buildingStatus);
            writer.write(response);
            writer.flush();
        } catch (Exception e) {
            LOGGER.severe("Error checking build status: " + e.getMessage());
            rsp.setStatus(500);
        }
    }

    private void writeJsonResponse(StaplerResponse2 rsp, String message) throws IOException {
        rsp.setContentType("application/json");
        rsp.setCharacterEncoding("UTF-8");
        PrintWriter writer = rsp.getWriter();

        try {
            ObjectMapper mapper = new ObjectMapper();
            String jsonResponse = mapper.writeValueAsString(message);
            writer.write(jsonResponse);
        } catch (Exception e) {
            // Fallback to simple JSON string
            writer.write("\"" + message.replace("\"", "\\\"") + "\"");
        }
        writer.flush();
    }

    /**
     * Create a response indicating this is a cached result.
     * @param analysis The cached analysis
     * @return The response string with cached indicator
     */
    private String createCachedResponse(String analysis) {
        return analysis + "\n\n[Note: This is a previously generated analysis. Use the 'Generate New' option to create a new one.]";
    }

    public Run<?, ?> getRun() {
        return run;
    }
}
