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

    public void analyzeError(Run<?, ?> run, TaskListener listener, String logPattern, String errorPatterns, int maxLines, int contextLines) {
        String jobInfo = run != null ? ("[" + run.getParent().getFullName() + " #" + run.getNumber() + "]") : "[unknown]";
        try {
            GlobalConfigurationImpl config = GlobalConfigurationImpl.get();

            if (config.getApiKey() == null || StringUtils.isBlank(config.getApiKey().getPlainText())) {
                listener.getLogger()
                        .println("ERROR: API key is not configured. Please configure it in Jenkins global settings.");
                return;
            }

            // Extract error logs with context - errorPatterns parameter takes priority over config
            String errorLogs = extractErrorLogsWithContext(run, logPattern, errorPatterns, maxLines, contextLines);

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

    /**
     * Extract error logs with surrounding context.
     * Captures contextLines before and after each error, forming error blocks.
     * An error block ends when we haven't seen an error for contextLines.
     */
    private String extractErrorLogsWithContext(Run<?, ?> run, String logPattern, String errorPatterns, int maxLines, int contextLines) throws IOException {
        List<String> patternsToUse = new ArrayList<>();

        // Priority 1: errorPatterns parameter (newline-separated)
        if (!StringUtils.isBlank(errorPatterns)) {
            String[] patternLines = errorPatterns.split("\\r?\\n");
            for (String pattern : patternLines) {
                if (!StringUtils.isBlank(pattern)) {
                    patternsToUse.add(pattern.trim());
                }
            }
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
            }
        }

        // If no patterns configured, return the last maxLines unfiltered
        if (patternsToUse.isEmpty()) {
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

        // Get all log lines
        int fetchLimit = Math.max(maxLines * 10, 10000);
        List<String> allLogLines = run.getLog(fetchLimit);

        // Build error blocks with context
        List<String> outputLines = new ArrayList<>();
        List<String> contextBuffer = new ArrayList<>();
        int linesSinceLastError = contextLines + 1; // Start with no active block
        int errorCount = 0;
        int totalLinesAdded = 0;

        for (int i = 0; i < allLogLines.size() && totalLinesAdded < maxLines; i++) {
            String line = allLogLines.get(i);
            boolean isError = false;

            // Check if this line matches any error pattern
            for (Pattern pattern : compiledPatterns) {
                if (pattern.matcher(line).find()) {
                    isError = true;
                    errorCount++;
                    break;
                }
            }

            if (isError) {
                // Found an error line
                // If starting a new block, add buffered context first
                if (linesSinceLastError > contextLines && !contextBuffer.isEmpty()) {
                    // Add separator for new block
                    if (!outputLines.isEmpty()) {
                        outputLines.add("");
                        outputLines.add("--- Error Block " + (errorCount > 1 ? "" : "---"));
                        outputLines.add("");
                        totalLinesAdded += 3;
                    }
                    // Add context before error
                    int contextToAdd = Math.min(contextBuffer.size(), contextLines);
                    for (int j = Math.max(0, contextBuffer.size() - contextToAdd); j < contextBuffer.size() && totalLinesAdded < maxLines; j++) {
                        outputLines.add(contextBuffer.get(j));
                        totalLinesAdded++;
                    }
                }

                // Add the error line with marker
                if (totalLinesAdded < maxLines) {
                    outputLines.add(">>> ERROR: " + line);
                    totalLinesAdded++;
                }
                linesSinceLastError = 0;
                contextBuffer.clear();
            } else {
                // Non-error line
                linesSinceLastError++;

                if (linesSinceLastError <= contextLines) {
                    // Within context window after error - add directly to output
                    if (totalLinesAdded < maxLines) {
                        outputLines.add(line);
                        totalLinesAdded++;
                    }
                } else {
                    // Outside context window - buffer for potential next error
                    contextBuffer.add(line);
                    // Keep buffer size limited
                    if (contextBuffer.size() > contextLines) {
                        contextBuffer.remove(0);
                    }
                }
            }
        }

        if (outputLines.isEmpty()) {
            return "";
        }

        // Add explanation header
        StringBuilder result = new StringBuilder();
        result.append("=== ERROR ANALYSIS ===\n");
        result.append("Note: Lines marked with '>>> ERROR:' matched configured error patterns.\n");
        result.append("Context of ").append(contextLines).append(" lines is shown before and after each error.\n");
        result.append("\n");
        result.append(String.join("\n", outputLines));

        return result.toString();
    }

    private String extractErrorLogs(Run<?, ?> run, String logPattern, String errorPatterns, int maxLines) throws IOException {
        List<String> patternsToUse = new ArrayList<>();

        // Priority 1: errorPatterns parameter (newline-separated)
        if (!StringUtils.isBlank(errorPatterns)) {
            String[] patternLines = errorPatterns.split("\\r?\\n");
            for (String pattern : patternLines) {
                if (!StringUtils.isBlank(pattern)) {
                    patternsToUse.add(pattern.trim());
                }
            }
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
            }
        }

        // If no patterns configured, return the last maxLines unfiltered
        if (patternsToUse.isEmpty()) {
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
