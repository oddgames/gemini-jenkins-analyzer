# Gemini Jenkins Analyzer - AI Analyze Feature Architecture

## Executive Summary

The AI analyze feature works through a **synchronous request-response pattern** with no streaming or real-time progress updates. The frontend makes blocking AJAX calls to the backend, which processes logs and sends them to the Google Gemini API. A progress bar is technically feasible but requires architectural changes to support streaming or chunked responses.

---

## 1. UI COMPONENTS FOR THE AI ANALYZE FEATURE

### 1.1 Console Page Footer (Main Entry Point)
**File**: `/src/main/resources/io/jenkins/plugins/gemini_jenkins_analyzer/ConsolePageDecorator/footer.jelly`

The Jelly template includes:
- **Error Logs Container**: Shows filtered logs being analyzed
- **Analysis Container**: Shows AI results
- **Spinner Container**: Simple loading indicator (text: "Analyzing error logs...")
- **Confirmation Dialog**: Prompts user to view existing analysis or generate new one

Key HTML elements:
- `#analyzer-error-logs-container` - Container for filtered logs
- `#analyzer-error-container` - Container for analysis results
- `#analyzer-error-spinner` - Loading indicator
- `#analyzer-error-confirm-dialog` - Existing analysis confirmation dialog

### 1.2 JavaScript Handler
**File**: `/src/main/webapp/js/analyzer-error-footer.js` (359 lines)

Key functions:
```javascript
analyzeConsoleError()          // Entry point
checkExistingAnalysis()        // Check if cached result exists
sendAnalyzeRequest()           // Main AJAX call
showSpinner()                  // Show loading indicator
showErrorAnalysis()            // Display results
showErrorLogs()                // Display filtered logs
```

### 1.3 Build Action Display Page
**File**: `/src/main/resources/io/jenkins/plugins/gemini_jenkins_analyzer/ErrorAnalysisAction/index.jelly`

Simple Jelly template that displays:
- Original error logs sent to AI
- AI analysis results
- Timestamp of analysis generation

---

## 2. LOG PARSING/PROCESSING LOGIC

### 2.1 ErrorAnalyzer Service
**File**: `/src/main/java/io/jenkins/plugins/gemini_jenkins_analyzer/ErrorAnalyzer.java` (350 lines)

Main methods:
```java
analyzeErrorWithFiltering(Run, maxLines)
  → Used for console UI analysis
  → Parses logs bottom-up (most recent errors first)
  → Returns AI analysis directly
  
extractErrorLogs(Run, logPattern, errorPatterns, maxLines)
  → Filters logs using regex patterns
  → Bottom-up parsing from end of log
  → Returns filtered log lines
  
extractErrorLogsWithContext(Run, logPattern, errorPatterns, maxLines, contextLines)
  → Used by pipeline step
  → Extracts error lines with surrounding context
  → Creates "error blocks" with X lines before/after each error
  
extractFilteredLogs(Run, maxLines)
  → Returns filtered logs for display
  → Uses same logic as extractErrorLogs
```

### 2.2 Processing Flow

**Step 1: Fetch Logs**
```
run.getLog(maxLines) → List<String> allLogLines
```

**Step 2: Apply Patterns** (Priority order)
1. errorPatterns parameter (from UI/step)
2. Job property (ErrorPatternProperty.class)
3. No filtering (send all logs)

**Step 3: Compile Regex**
```java
Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE)
```

**Step 4: Filter Bottom-Up**
```
Iterate from end of logs backward
Match patterns (case-insensitive)
Keep matched lines in order
Limit to maxLines
Reverse to restore chronological order
```

**Step 5: Add Context** (Pipeline only)
```
For each error line: add 3 lines before + after
Create error blocks with separators
Limit total lines to maxLines
```

### 2.3 Error Pattern Sources

1. **UI Console Analysis**: Uses configured global patterns (GlobalConfigurationImpl)
2. **Pipeline Step**: Uses errorPatterns parameter from step config
3. **Job Property**: ErrorPatternProperty can store patterns per-job

---

## 3. BACKEND-TO-FRONTEND COMMUNICATION

### 3.1 AJAX Endpoints (ConsoleAnalyzeErrorAction)

**File**: `/src/main/java/io/jenkins/plugins/gemini_jenkins_analyzer/ConsoleAnalyzeErrorAction.java`

All endpoints are POST only (require CSRF token):

#### Endpoint 1: `doExplainConsoleError()`
```
URL: /console-analyzer-error/explainConsoleError
Method: POST
Parameters: forceNew=true|false, maxLines=int

Response: JSON-encoded string with AI analysis
Timeline: 
  1. Check for existing cached analysis
  2. If exists and not forced: return cached + note
  3. If forced/new: extract filtered logs
  4. Send to AI service
  5. Save result as ErrorAnalysisAction
  6. Return analysis result
```

#### Endpoint 2: `doGetFilteredLogs()`
```
URL: /console-analyzer-error/getFilteredLogs
Method: POST
Parameters: maxLines=int

Response: JSON-encoded string with filtered error logs
Purpose: Show user what will be sent to AI BEFORE analysis
```

#### Endpoint 3: `doCheckExistingAnalysis()`
```
URL: /console-analyzer-error/checkExistingAnalysis
Method: POST
No parameters

Response: JSON object
{
  "hasAnalysis": true/false,
  "timestamp": "yyyy-MM-dd HH:mm:ss" (if exists)
}
```

#### Endpoint 4: `doCheckBuildStatus()`
```
URL: /console-analyzer-error/checkBuildStatus
Method: POST
No parameters

Response: JSON object
{
  "buildingStatus": 0|1|2
}
Values:
  0 = SUCCESS (button shown)
  1 = RUNNING (wait and retry)
  2 = FAILURE/UNSTABLE (button shown)
```

### 3.2 Communication Flow

```
JavaScript                          Java Backend                         Gemini API
    |                                  |                                    |
    |--[1] checkBuildStatus()--------->|                                    |
    |<--[JSON: status]-----------------|                                    |
    |                                  |                                    |
    |--[2] checkExistingAnalysis()---->|                                    |
    |<--[JSON: hasAnalysis]------------|                                    |
    |                                  |                                    |
    |  [If cached, show dialog]        |                                    |
    |                                  |                                    |
    |--[3] getFilteredLogs()---------->| extractFilteredLogs()              |
    |<--[JSON: filtered logs]---------|  (regex + bottom-up)                |
    |                                  |                                    |
    | [Display filtered logs to user]  |                                    |
    |                                  |                                    |
    |--[4] explainConsoleError()------>| extractErrorLogs()                 |
    |                                  | errorAnalyzer.analyzeError()       |
    |                                  |          |                         |
    |                                  |          +--[Gemini API Call]----->|
    |                                  |                                    |
    |                                  |<--[Analysis Result]----------------|
    |                                  | run.addOrReplaceAction()           |
    |                                  | run.save()                         |
    |<--[JSON: analysis]----------------|                                    |
    |                                  |                                    |
    | [Display analysis to user]       |                                    |
```

### 3.3 Blocking Request-Response Pattern

**Critical Finding**: All requests are **synchronous and blocking**:

```javascript
// From analyzer-error-footer.js (line 264-306)
fetch(logsUrl, { ... })
.then(response => response.text())
.then(responseText => {
  // Display filtered logs
});

fetch(analyzeUrl, { ... })
.then(response => { ... })
.then(responseText => {
  // Display analysis
});
```

**No streaming**, **no chunking**, **no progress events** sent from backend during analysis.

---

## 4. EXISTING PROGRESS TRACKING MECHANISMS

### 4.1 Current Progress Indicators

1. **Spinner (Jelly template)**
   ```xml
   <div id="analyzer-error-spinner" class="jenkins-hidden">
     <l:spinner text="Analyzing error logs..."/>
   </div>
   ```
   - Shows while waiting for response
   - Uses Jenkins standard spinner component
   - Text only: "Analyzing error logs..."
   - No percentage or progress indication

2. **Visual Flow**
   ```
   User clicks "Analyze Error"
        ↓
   Show spinner (divs unhidden)
        ↓
   [BLOCKING WAIT for Gemini API response]
        ↓
   Hide spinner, show results
   ```

### 4.2 No Webhook/Streaming Support

- No WebSocket implementation
- No Server-Sent Events (SSE)
- No long-polling mechanism
- No mid-request progress callbacks

### 4.3 Backend Logging Only

Progress is only logged to Jenkins log (LOGGER):
```java
LOGGER.info("Sending request to AI service...");
// ... wait for response ...
LOGGER.info("Received response from AI service");
```

Visible in Jenkins logs but NOT to UI.

---

## 5. JELLY FILES AND JAVASCRIPT HANDLERS

### 5.1 All Jelly Template Files

1. **ConsolePageDecorator/footer.jelly** (56 lines)
   - Includes JavaScript file
   - Defines UI containers (logs, analysis, spinner, dialog)
   - Conditional: only loads if plugin is active

2. **ErrorAnalysisAction/index.jelly** (17 lines)
   - Display page for saved analysis
   - Shows original logs and analysis
   - Timestamp formatting

3. **GlobalConfigurationImpl/config.jelly** (120 lines)
   - Admin configuration form
   - API key, provider selection, model name
   - Error pattern preset selector
   - Inline JavaScript for preset application
   - Test Configuration button

4. **AnalyzeErrorStep/config.jelly** (17 lines)
   - Pipeline step configuration
   - Error patterns textarea
   - Max lines and context lines fields

5. **ErrorPatternProperty/config.jelly**
   - Job-level error pattern configuration

### 5.2 JavaScript Files

**Single file**: `/src/main/webapp/js/analyzer-error-footer.js` (359 lines)

Functions:
- `checkBuildStatusAndAddButton()` - Polls build status until complete
- `checkBuildStatus()` - Fetch build status
- `addAnalyzeErrorButton()` - Inject button into console page
- `analyzeConsoleError()` - Main entry point
- `checkExistingAnalysis()` - Check for cached results
- `sendAnalyzeRequest()` - Core AJAX call
- `showErrorAnalysis()` - Display results
- `showErrorLogs()` - Display filtered logs
- `showConfirmationDialog()` - Show existing analysis dialog
- `viewExistingAnalysis()` - View cached result
- `generateNewAnalysis()` - Force new analysis
- `cancelAnalysis()` - Cancel operation

**Key behavior**: 
- DOM manipulation using standard querySelector/classList
- Fetch API (not jQuery)
- CSRF token from Jenkins `crumb` object
- Displays containers with Jenkins CSS classes

---

## 6. ARCHITECTURE INSIGHTS

### 6.1 Current Design Pattern

This is a **classic request-response architecture**:
- Stateless AJAX endpoints
- Synchronous backend processing
- No streaming or chunked responses
- Results cached in build action for reuse

### 6.2 Bottlenecks

1. **Log Parsing**: Bottom-up regex matching O(n) time
2. **API Call**: Blocking wait for Gemini response (typical 5-30 seconds)
3. **No intermediate updates**: User sees spinner only, no progress percentage

### 6.3 Data Flow Summary

```
Console Page
    ↓
JavaScript event (button click)
    ↓
4 sequential AJAX calls:
  1. Check build status
  2. Check existing analysis
  3. Get filtered logs (optional, for display)
  4. Explain console error (main blocking call)
    ↓
ErrorAnalyzer.extractErrorLogs() - Regex filtering
    ↓
ErrorAnalyzer.analyzeError() - Calls ErrorAnalyzer
    ↓
ErrorAnalyzer (wraps AIService)
    ↓
AIService (factory)
    ↓
GeminiService (extends BaseAIService)
    ↓
LangChain4j GoogleAiGeminiChatModel
    ↓
Google Gemini API
    ↓
Response comes back
    ↓
ErrorAnalysisAction saved to run
    ↓
JSON response to JavaScript
    ↓
Display in pre element
```

---

## 7. PROGRESS BAR FEASIBILITY ASSESSMENT

### Current State: NOT FEASIBLE without changes

Reason: The request is completely blocking. The backend doesn't report progress until the entire response is complete.

### What Would Be Needed

**Option 1: Chunked Response with Multiple Endpoints** (Recommended)
```
Phase 1: Extract logs (10-20% progress)
  → Return: intermediate response
Phase 2: Prepare prompt (20-30% progress)
  → Return: intermediate response
Phase 3: Send to API (40-90% progress)
  → Stream progress from API if supported
Phase 4: Parse response (90-100% progress)
  → Return: final response
```

**Option 2: Server-Sent Events (SSE)**
```
Keep connection open
Stream progress events from backend
JavaScript updates progress bar in real-time
```

**Option 3: WebSocket**
```
Bidirectional streaming
Backend sends progress updates
Frontend updates UI without polling
```

### What Information Could Be Tracked

1. **Deterministic phases** (if splitting into steps):
   - Logs fetched: X / Y bytes
   - Patterns compiled: X / Y patterns
   - Logs filtered: X / Y lines processed
   - API request sent: waiting for response

2. **API-level progress**:
   - Only if Gemini API supports streaming (likely via LangChain4j)
   - Token count estimation
   - Partial responses

---

## 8. KEY FILES REFERENCE

| File Path | Lines | Purpose |
|-----------|-------|---------|
| ErrorAnalyzer.java | 350 | Log parsing, filtering, regex matching |
| ConsoleAnalyzeErrorAction.java | 217 | AJAX endpoints, main backend logic |
| analyzer-error-footer.js | 359 | UI interaction, fetch calls |
| ConsolePageDecorator/footer.jelly | 56 | HTML template, spinner, containers |
| BaseAIService.java | 103 | LangChain4j integration, API calls |
| GeminiService.java | 81 | Google Gemini model configuration |
| ErrorAnalysisAction.java | 78 | Build action storage |

---

## 9. TECHNICAL CONSTRAINTS

1. **LangChain4j**: Synchronous API calls only in current implementation
2. **Jenkins Stapler**: Request-response model, not designed for streaming
3. **CSRF Protection**: All POST requests require Jenkins CSRF token
4. **API Timeout**: 90 seconds for Gemini API call (in GeminiService)
5. **Log Limits**: Fetches up to 10x maxLines (default 1000 lines)

---

## 10. RECOMMENDED NEXT STEPS FOR PROGRESS BAR

1. **Phase 1 - Non-blocking logs extraction**
   - Implement `doGetFilteredLogsAsync()`
   - Return immediately with streamed logs
   - Show percentage as logs arrive

2. **Phase 2 - Streaming API responses**
   - Check if LangChain4j supports streaming (Google Gemini supports streaming)
   - Implement SSE endpoint that streams analysis as it arrives
   - Show token/word count progress

3. **Phase 3 - UI Enhancement**
   - Replace simple spinner with progress bar
   - Add percentage display
   - Add ETA estimation based on processed tokens

---

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                   Jenkins Build Console Page                     │
│                                                                   │
│  ┌──────────────────┐                                           │
│  │ Analyze Button   │ ← injected by JavaScript                  │
│  └────────┬─────────┘                                           │
│           │ onclick                                              │
│           ▼                                                       │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │      analyzer-error-footer.js (359 lines)                │  │
│  │                                                            │  │
│  │  checkBuildStatusAndAddButton()                           │  │
│  │  checkExistingAnalysis()                                  │  │
│  │  sendAnalyzeRequest()                                     │  │
│  │    ├─→ fetch /checkBuildStatus                            │  │
│  │    ├─→ fetch /checkExistingAnalysis                       │  │
│  │    ├─→ fetch /getFilteredLogs ────────────┐              │  │
│  │    │                                       │              │  │
│  │    └─→ fetch /explainConsoleError ────┐   │              │  │
│  └────────────────────────────────────────┼───┼──────────────┘  │
│                                           │   │                  │
│  ┌──────────────────────────────────────┐ │   │                  │
│  │  #analyzer-error-spinner             │ │   │                  │
│  │  (shows while waiting)                │ │   │                  │
│  └──────────────────────────────────────┘ │   │                  │
│                                           │   │                  │
│  ┌──────────────────────────────────────┐ │   │                  │
│  │  #analyzer-error-logs-container      │◄───┘                  │
│  │  (filtered logs display)              │                      │
│  └──────────────────────────────────────┘                      │
│                                           │                      │
│  ┌──────────────────────────────────────┐ │                      │
│  │  #analyzer-error-container           │◄┘                      │
│  │  (analysis results display)           │                      │
│  └──────────────────────────────────────┘                      │
└─────────────────────────────────────────────────────────────────┘
                      │
                      │ HTTP POST (with CSRF token)
                      ▼
┌──────────────────────────────────────────────────────────────────┐
│            ConsoleAnalyzeErrorAction (Backend)                   │
│                                                                   │
│  doExplainConsoleError()                                         │
│  ├─ Check permission (hudson.model.Item.READ)                   │
│  ├─ Check for cached ErrorAnalysisAction                        │
│  └─ If not cached:                                              │
│     └─ ErrorAnalyzer.analyzeErrorWithFiltering()                │
│        ├─ extractErrorLogs(Run, null, null, maxLines)           │
│        │  ├─ run.getLog(fetchLimit) [10x maxLines]             │
│        │  ├─ Compile regex patterns (case insensitive)         │
│        │  ├─ Bottom-up parsing (newest errors first)           │
│        │  └─ Return: matched lines in chronological order      │
│        │                                                         │
│        └─ AIService.analyzeError(filteredLogs)                  │
│           └─ GeminiService.createAssistant()                    │
│              └─ LangChain4j.AiServices.create()                 │
│                 └─ GoogleAiGeminiChatModel                      │
│                                                                   │
│  Save: run.addOrReplaceAction(ErrorAnalysisAction)              │
│  Response: JSON string with analysis                            │
└───────────────────────────────┬──────────────────────────────────┘
                                │
                                │ Blocking wait for API response
                                ▼
                    ┌───────────────────────┐
                    │ Google Gemini API     │
                    │ (90 second timeout)   │
                    │ gemini-2.0-flash      │
                    └───────────────────────┘
```

