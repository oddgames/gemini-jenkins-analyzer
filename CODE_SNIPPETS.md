# Code Snippets - AI Analyze Feature

This document contains key code snippets from the AI analyze feature.

---

## 1. Frontend - JavaScript Entry Point

**File**: `/src/main/webapp/js/analyzer-error-footer.js` (Lines 167-200)

```javascript
function analyzeConsoleError() {
  // First, check if an analysis already exists
  checkExistingAnalysis();
}

function checkExistingAnalysis() {
  const basePath = window.location.pathname.replace(/\/console(Full)?$/, '');
  const url = basePath + '/console-analyzer-error/checkExistingAnalysis';

  const headers = crumb.wrap({
    "Content-Type": "application/x-www-form-urlencoded",
  });

  fetch(url, {
    method: "POST",
    headers: headers,
    body: ""
  })
  .then(response => response.json())
  .then(data => {
    if (data.hasAnalysis) {
      // Show confirmation dialog
      showConfirmationDialog(data.timestamp);
    } else {
      // No existing analysis, proceed with new request
      sendAnalyzeRequest(false);
    }
  })
  .catch(error => {
    console.warn('Error checking existing analysis:', error);
    // If check fails, proceed with new request
    sendAnalyzeRequest(false);
  });
}
```

---

## 2. Frontend - Main AJAX Call

**File**: `/src/main/webapp/js/analyzer-error-footer.js` (Lines 249-307)

```javascript
function sendAnalyzeRequest(forceNew = false) {
  const basePath = window.location.pathname.replace(/\/console(Full)?$/, '');
  const analyzeUrl = basePath + '/console-analyzer-error/explainConsoleError';
  const logsUrl = basePath + '/console-analyzer-error/getFilteredLogs';

  const headers = crumb.wrap({
    "Content-Type": "application/x-www-form-urlencoded",
  });

  // Add forceNew parameter if needed
  const body = forceNew ? "forceNew=true" : "";

  showSpinner();

  // First, fetch and display the filtered logs
  fetch(logsUrl, {
    method: "POST",
    headers: headers,
    body: ""
  })
  .then(response => response.text())
  .then(responseText => {
    try {
      const filteredLogs = JSON.parse(responseText);
      if (filteredLogs && filteredLogs.trim()) {
        showErrorLogs(filteredLogs);
      }
    } catch (e) {
      console.warn('Could not parse filtered logs:', e);
    }
  })
  .catch(error => {
    console.warn('Could not fetch filtered logs:', error);
  });

  // Then send the analysis request
  fetch(analyzeUrl, {
    method: "POST",
    headers: headers,
    body: body
  })
  .then(response => {
    if (!response.ok) {
      notificationBar.show('Analysis failed', notificationBar.ERROR);
    }
    return response.text();
  })
  .then(responseText => {
    try {
      const jsonResponse = JSON.parse(responseText);
      showErrorAnalysis(jsonResponse);
    } catch (e) {
      showErrorAnalysis(responseText);
    }
  })
  .catch(error => {
    showErrorAnalysis(`Error: ${error.message}`);
  });
}
```

---

## 3. Frontend - UI Updates

**File**: `/src/main/webapp/js/analyzer-error-footer.js` (Lines 309-358)

```javascript
function showErrorAnalysis(message) {
  const container = document.getElementById('analyzer-error-container');
  const spinner = document.getElementById('analyzer-error-spinner');
  const content = document.getElementById('analyzer-error-content');

  container.classList.remove('jenkins-hidden');
  spinner.classList.add('jenkins-hidden');
  content.textContent = message;
}

function showSpinner() {
  const container = document.getElementById('analyzer-error-container');
  const spinner = document.getElementById('analyzer-error-spinner');
  container.classList.remove('jenkins-hidden');
  spinner.classList.remove('jenkins-hidden');
}

function showErrorLogs(logs) {
  const logsContainer = document.getElementById('analyzer-error-logs-container');
  const logsContent = document.getElementById('analyzer-error-logs-content');

  if (logsContainer && logsContent) {
    logsContainer.classList.remove('jenkins-hidden');
    logsContent.textContent = logs;
  }
}
```

---

## 4. Backend - Main Endpoint

**File**: `/src/main/java/.../ConsoleAnalyzeErrorAction.java` (Lines 48-94)

```java
@RequirePOST
public void doExplainConsoleError(StaplerRequest2 req, StaplerResponse2 rsp) 
        throws ServletException, IOException {
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
```

---

## 5. Log Processing - Main Method

**File**: `/src/main/java/.../ErrorAnalyzer.java` (Lines 262-298)

```java
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
```

---

## 6. Log Filtering - Bottom-Up Regex Matching

**File**: `/src/main/java/.../ErrorAnalyzer.java` (Lines 190-256)

```java
private String extractErrorLogs(Run<?, ?> run, String logPattern, 
        String errorPatterns, int maxLines) throws IOException {
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
```

---

## 7. AI Service - API Call

**File**: `/src/main/java/.../BaseAIService.java` (Lines 36-80)

```java
public String analyzeError(String errorLogs) throws IOException {
    Assistant assistant;

    if (StringUtils.isBlank(errorLogs)) {
        return "No error logs provided for analysis.";
    }

    // Validate API key
    if (config.getApiKey() == null || StringUtils.isBlank(config.getApiKey().getPlainText())) {
        return "Unable to create assistant: API key is not configured.";
    }

    try {
       assistant = createAssistant();
    } catch (Exception e) {
        LOGGER.severe("Failed to create assistant: " + e.getMessage());
        e.printStackTrace();
        return "Unable to create assistant: " + e.getMessage() + ". Please check your API key and model configuration.";
    }

    // Use PromptTemplate for dynamic prompt creation
    PromptTemplate promptTemplate = PromptTemplate.from(
        "Senior dev: analyze this failure. Skip obvious stuff.\n\n"
        + "{{errorLogs}}\n\n"
        + "1. Root cause (environment/dependency/config issues only)\n"
        + "2. Fix (1-2 sentences max)\n"
        + "3. Prevention (if non-trivial)\n\n"
        + "MAX 5 LINES TOTAL. Plain text only."
    );

    Map<String, Object> variables = new HashMap<>();
    variables.put("errorLogs", errorLogs);
    Prompt prompt = promptTemplate.apply(variables);

    try {
        LOGGER.info("Sending request to AI service...");
        String analysis = assistant.chat(prompt.text());
        LOGGER.info("Received response from AI service");
        return analysis != null && !analysis.trim().isEmpty() ? analysis : "No response received from AI service.";
    } catch (Exception e) {
        LOGGER.severe("AI API request failed: " + e.getMessage());
        e.printStackTrace();
        return "Failed to communicate with AI service: " + e.getMessage();
    }
}
```

---

## 8. Gemini Service - Model Configuration

**File**: `/src/main/java/.../GeminiService.java` (Lines 27-46)

```java
@Override
protected Assistant createAssistant() {
    String baseUrl = determineBaseUrl("Gemini");

    // Use configured model or default to gemini-2.0-flash
    String modelName = (config.getModel() != null && !config.getModel().trim().isEmpty())
        ? config.getModel()
        : "gemini-2.0-flash";

    var model = GoogleAiGeminiChatModel.builder()
        .baseUrl(baseUrl) // Will use default if null
        .apiKey(config.getApiKey().getPlainText())
        .modelName(modelName)
        .temperature(0.3)
        .timeout(Duration.ofSeconds(90)) // 90 second timeout for error analysis
        .logRequests(LOGGER.getLevel() == Level.FINE)
        .logResponses(LOGGER.getLevel() == Level.FINE)
        .build();

    return AiServices.create(Assistant.class, model);
}
```

---

## 9. UI Template - Console Footer

**File**: `/src/main/resources/.../ConsolePageDecorator/footer.jelly` (Key sections)

```xml
<?jelly escape-by-default='true'?>
<j:jelly xmlns:j="jelly:core" xmlns:l="/lib/layout">
    <j:if test="${it.pluginActive}">
        <script src="${rootURL}/plugin/gemini-jenkins-analyzer/js/analyzer-error-footer.js" type="text/javascript"/>

        <!-- Error Logs Container -->
        <div id="analyzer-error-logs-container" class="jenkins-hidden">
          <l:card title="Error Logs Being Analyzed">
            <pre id="analyzer-error-logs-content" style="background-color: #f5f5f5; padding: 10px; border-radius: 4px;" class="jenkins-!-margin-bottom-0"></pre>
          </l:card>
        </div>

        <!-- Analysis Container -->
        <div id="analyzer-error-container" class="jenkins-hidden">
          <l:card title="AI Error Analysis">
            <div id="analyzer-error-spinner" class="jenkins-hidden">
              <l:spinner text="Analyzing error logs..."/>
            </div>
            <pre id="analyzer-error-content" class="jenkins-!-margin-bottom-0"></pre>
          </l:card>
        </div>

        <!-- Confirmation Dialog -->
        <div id="analyzer-error-confirm-dialog" class="jenkins-hidden">
          <l:card title="AI Error Analysis Exists">
            <p>An AI analysis was already generated on <span id="existing-analysis-timestamp"></span>.</p>
            <p>Do you want to view the existing analysis or generate a new one?</p>
            <div class="jenkins-button-bar">
              <button type="button" class="jenkins-button jenkins-button--primary jenkins-!-margin-1 eep-view-existing-button" onclick="viewExistingAnalysis()">
                View Existing
              </button>
              <button type="button" class="jenkins-button jenkins-!-margin-1 eep-generate-new-button">
                Generate New
              </button>
              <button type="button" class="jenkins-button jenkins-!-margin-1 eep-cancel-button" onclick="cancelAnalysis()">
                Cancel
              </button>
            </div>
          </l:card>
        </div>
    </j:if>
</j:jelly>
```

---

## 10. Build Action - Result Storage

**File**: `/src/main/java/.../ErrorAnalysisAction.java` (Lines 1-78)

```java
package io.jenkins.plugins.gemini_jenkins_analyzer;

import hudson.model.Run;
import jenkins.model.RunAction2;

/**
 * Build action to store and display error analysiss.
 */
public class ErrorAnalysisAction implements RunAction2 {

    private final String analysis;
    private final String originalErrorLogs;
    private final long timestamp;
    private transient Run<?, ?> run;

    public ErrorAnalysisAction(String analysis, String originalErrorLogs) {
        this.analysis = analysis;
        this.originalErrorLogs = originalErrorLogs;
        this.timestamp = System.currentTimeMillis();
    }

    @Override
    public String getIconFileName() {
        return "symbol-cube";
    }

    @Override
    public String getDisplayName() {
        return "AI Error Analysis";
    }

    @Override
    public String getUrlName() {
        return "error-analysis";
    }

    public String getAnalysis() {
        return analysis;
    }

    public String getOriginalErrorLogs() {
        return originalErrorLogs;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getFormattedTimestamp() {
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(timestamp));
    }

    @Override
    public void onAttached(Run<?, ?> r) {
        this.run = r;
    }

    @Override
    public void onLoad(Run<?, ?> r) {
        this.run = r;
    }

    public Run<?, ?> getRun() {
        return run;
    }

    public boolean hasValidAnalysis() {
        return analysis != null && !analysis.trim().isEmpty();
    }
}
```

---

## Dependencies

Key Maven dependencies for this feature:

```xml
<!-- LangChain4j for AI integration -->
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-google-ai-gemini</artifactId>
    <version>1.4.0</version>
</dependency>

<!-- Jenkins core APIs -->
<dependency>
    <groupId>org.jenkins-ci</groupId>
    <artifactId>jenkins-core</artifactId>
</dependency>

<!-- Pipeline support -->
<dependency>
    <groupId>org.jenkins-ci.plugins.workflow</groupId>
    <artifactId>workflow-step-api</artifactId>
</dependency>

<!-- Jackson for JSON handling -->
<dependency>
    <groupId>org.jenkins-ci.plugins</groupId>
    <artifactId>jackson2-api</artifactId>
</dependency>
```

