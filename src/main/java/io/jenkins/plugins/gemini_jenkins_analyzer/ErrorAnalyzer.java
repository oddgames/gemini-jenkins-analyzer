package io.jenkins.plugins.gemini_jenkins_analyzer;

import hudson.model.Run;
import hudson.model.TaskListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;

/**
 * Service class responsible for explaining errors using AI.
 */
public class ErrorAnalyzer {

    private static final Logger LOGGER = Logger.getLogger(ErrorAnalyzer.class.getName());

    public void analyzeError(Run<?, ?> run, TaskListener listener, String logPattern, String errorPatterns, int maxLines) {
        String jobInfo = run != null ? ("[" + run.getParent().getFullName() + " #" + run.getNumber() + "]") : "[unknown]";
        try {
            GlobalConfigurationImpl config = GlobalConfigurationImpl.get();

            if (config.getApiKey() == null || StringUtils.isBlank(config.getApiKey().getPlainText())) {
                listener.getLogger()
                        .println("ERROR: API key is not configured. Please configure it in Jenkins global settings.");
                return;
            }

            // Extract error logs - errorPatterns parameter takes priority over config
            String errorLogs = extractErrorLogs(run, logPattern, errorPatterns, maxLines);

            if (StringUtils.isBlank(errorLogs)) {
                listener.getLogger().println("No error logs found to explain.");
                return;
            }

            // Get AI analysis
            AIService aiService = new AIService(config);
            String analysis = aiService.analyzeError(errorLogs);
            LOGGER.info(jobInfo + " AI error analysis succeeded.");

            // Store analysis in build action
            ErrorAnalysisAction action = new ErrorAnalysisAction(analysis, errorLogs);
            run.addOrReplaceAction(action);

            // Analysis is now available on the job page, no need to clutter console output

        } catch (Exception e) {
            LOGGER.severe(jobInfo + " Failed to explain error: " + e.getMessage());
            listener.getLogger().println(jobInfo + " Failed to explain error: " + e.getMessage());
        }
    }

    private String extractErrorLogs(Run<?, ?> run, String logPattern, String errorPatterns, int maxLines) throws IOException {
        LOGGER.info("=== ERROR LOG EXTRACTION DEBUG ===");

        List<String> patternsToUse = new ArrayList<>();

        // Priority 1: errorPatterns parameter (newline-separated)
        if (!StringUtils.isBlank(errorPatterns)) {
            String[] patternLines = errorPatterns.split("\\r?\\n");
            for (String pattern : patternLines) {
                if (!StringUtils.isBlank(pattern)) {
                    patternsToUse.add(pattern.trim());
                }
            }
            LOGGER.info("Using errorPatterns parameter with " + patternsToUse.size() + " patterns");
        } else {
            // Priority 2: Check job property
            ErrorPatternProperty property = run.getParent().getProperty(ErrorPatternProperty.class);
            if (property != null && !StringUtils.isBlank(property.getErrorPatterns())) {
                String[] patternLines = property.getErrorPatterns().split("\\r?\\n");
                for (String pattern : patternLines) {
                    if (!StringUtils.isBlank(pattern)) {
                        patternsToUse.add(pattern.trim());
                    }
                }
                LOGGER.info("Using job property patterns with " + patternsToUse.size() + " patterns");
            }
        }

        if (!patternsToUse.isEmpty() && LOGGER.isLoggable(java.util.logging.Level.FINE)) {
            LOGGER.fine("First pattern: " + patternsToUse.get(0));
        }

        LOGGER.info("Patterns to use count: " + patternsToUse.size());

        // If no patterns configured, return the last maxLines unfiltered
        if (patternsToUse.isEmpty()) {
            LOGGER.warning("No patterns configured! Returning unfiltered logs.");
            List<String> logLines = run.getLog(maxLines);
            return String.join("\n", logLines);
        }

        // Compile all patterns
        List<Pattern> compiledPatterns = new ArrayList<>();
        for (String patternStr : patternsToUse) {
            if (!StringUtils.isBlank(patternStr)) {
                compiledPatterns.add(Pattern.compile(patternStr.trim(), Pattern.CASE_INSENSITIVE));
            }
        }

        if (compiledPatterns.isEmpty()) {
            List<String> logLines = run.getLog(maxLines);
            return String.join("\n", logLines);
        }

        // Get a large number of log lines to ensure we have enough to parse
        // We use 10x maxLines as a reasonable upper bound
        int fetchLimit = Math.max(maxLines * 10, 10000);
        List<String> allLogLines = run.getLog(fetchLimit);

        // Parse bottom-up: start from the end and work backwards
        List<String> matchedLines = new ArrayList<>();
        for (int i = allLogLines.size() - 1; i >= 0 && matchedLines.size() < maxLines; i--) {
            String line = allLogLines.get(i);

            // Check if line matches any of the patterns
            for (Pattern pattern : compiledPatterns) {
                if (pattern.matcher(line).find()) {
                    matchedLines.add(line);
                    break; // Don't add the same line multiple times
                }
            }
        }

        // Reverse to restore chronological order (oldest to newest)
        Collections.reverse(matchedLines);

        LOGGER.info("Total log lines scanned: " + allLogLines.size());
        LOGGER.info("Matched lines: " + matchedLines.size());

        return String.join("\n", matchedLines);
    }

    /**
     * Filters error logs using configured regex patterns, parsing bottom-up.
     * Used for console output error analysis with regex filtering.
     */
    public String analyzeErrorWithFiltering(Run<?, ?> run, int maxLines) throws IOException {
        String jobInfo = run != null ? ("[" + run.getParent().getFullName() + " #" + run.getNumber() + "]") : "[unknown]";

        try {
            GlobalConfigurationImpl config = GlobalConfigurationImpl.get();

            if (!config.isEnableAnalysis()) {
                LOGGER.warning("AI error analysis is disabled in global configuration");
                return "AI error analysis is disabled in global configuration.";
            }

            if (config.getApiKey() == null || StringUtils.isBlank(config.getApiKey().getPlainText())) {
                LOGGER.warning("API key is not configured");
                return "ERROR: API key is not configured. Please configure it in Jenkins global settings.";
            }

            // Extract error logs using the same logic as the pipeline step
            String errorLogs = extractErrorLogs(run, null, null, maxLines);

            if (StringUtils.isBlank(errorLogs)) {
                LOGGER.warning("No error logs found to explain");
                return "No error logs found to explain.";
            }

            // Get AI analysis
            AIService aiService = new AIService(config);
            String analysis = aiService.analyzeError(errorLogs);
            LOGGER.info(jobInfo + " AI error analysis succeeded.");
            LOGGER.fine("Analysis length: " + (analysis != null ? analysis.length() : 0));

            return analysis;
        } catch (Exception e) {
            LOGGER.severe(jobInfo + " Failed to explain error: " + e.getMessage());
            e.printStackTrace();
            return "Failed to explain error: " + e.getMessage();
        }
    }

    /**
     * Explains error text directly without extracting from logs.
     * Used for console output error analysis.
     */
    public String analyzeErrorText(String errorText, Run<?, ?> run) {
        String jobInfo = run != null ? ("[" + run.getParent().getFullName() + " #" + run.getNumber() + "]") : "[unknown]";

        try {
            GlobalConfigurationImpl config = GlobalConfigurationImpl.get();

            if (!config.isEnableAnalysis()) {
                LOGGER.warning("AI error analysis is disabled in global configuration");
                return "AI error analysis is disabled in global configuration.";
            }

            if (config.getApiKey() == null || StringUtils.isBlank(config.getApiKey().getPlainText())) {
                LOGGER.warning("API key is not configured");
                return "ERROR: API key is not configured. Please configure it in Jenkins global settings.";
            }

            if (StringUtils.isBlank(errorText)) {
                LOGGER.warning("No error text provided");
                return "No error text provided to explain.";
            }

            // Get AI analysis
            AIService aiService = new AIService(config);
            String analysis = aiService.analyzeError(errorText);
            LOGGER.info(jobInfo + " AI error analysis succeeded.");
            LOGGER.fine("Analysis length: " + (analysis != null ? analysis.length() : 0));

            return analysis;
        } catch (Exception e) {
            LOGGER.severe(jobInfo + " Failed to explain error text: " + e.getMessage());
            e.printStackTrace();
            return "Failed to explain error: " + e.getMessage();
        }
    }

    /**
     * Extracts and returns filtered error logs based on configured patterns.
     * Used to show users what logs will be sent to the AI before analysis.
     *
     * @param run the Jenkins run to extract logs from
     * @param maxLines maximum number of matching lines to return
     * @return filtered error logs as a string, or empty string if no matches
     */
    public String extractFilteredLogs(Run<?, ?> run, int maxLines) throws IOException {
        return extractErrorLogs(run, null, null, maxLines);
    }
}
