# AI Analyze Feature - Complete Understanding

## What You Now Know

This Jenkins plugin analyzes build failures using Google Gemini AI. Here's the complete architecture:

---

## One-Minute Overview

1. User clicks "Analyze Error" on console page
2. JavaScript makes 4 sequential AJAX calls to backend
3. Backend extracts error logs using regex filtering (bottom-up)
4. Sends logs to Google Gemini API
5. Receives analysis and displays to user
6. Saves result in build for reuse

**Key limitation**: It's all blocking/synchronous - no progress updates during analysis.

---

## Architecture Components

### Frontend Stack
- **Technology**: Vanilla JavaScript + Fetch API (no jQuery)
- **Framework**: Jenkins Stapler templating (Jelly)
- **CSS**: Jenkins standard styles
- **File**: `analyzer-error-footer.js` (359 lines)
- **Templates**: `footer.jelly` (UI containers), `index.jelly` (results display)

### Backend Stack
- **Language**: Java
- **Framework**: Jenkins Plugin SDK
- **API Library**: LangChain4j
- **AI Provider**: Google Gemini (gemini-2.0-flash)
- **Key Class**: `ConsoleAnalyzeErrorAction` (main endpoint handler)
- **Processing**: `ErrorAnalyzer` (log filtering with regex)
- **Storage**: `ErrorAnalysisAction` (build action for caching)

### External APIs
- **Google Gemini API**: Called via LangChain4j
- **Timeout**: 90 seconds
- **Model**: gemini-2.0-flash (configurable)
- **Temperature**: 0.3 (deterministic)

---

## Data Flow Diagram

```
User Click
    |
    v
JavaScript: analyzeConsoleError()
    |
    +---> checkBuildStatus()          [Poll until build complete]
    |
    +---> checkExistingAnalysis()     [Check if cached]
    |
    +---> getFilteredLogs()           [Show user what will be sent]
    |                                   |
    |                                   v
    |                            Show filtered logs preview
    |
    +---> explainConsoleError()       [MAIN BLOCKING CALL]
            |
            v
    ConsoleAnalyzeErrorAction.doExplainConsoleError()
            |
            +---> Check permissions
            |
            +---> Check for cached analysis
            |
            +---> ErrorAnalyzer.extractErrorLogs()
            |       |
            |       +---> Get all logs from build
            |       |
            |       +---> Compile regex patterns
            |       |
            |       +---> Filter bottom-up (recent errors first)
            |       |
            |       +---> Return filtered logs
            |
            +---> AIService.analyzeError()
            |       |
            |       +---> GeminiService.createAssistant()
            |       |
            |       +---> LangChain4j.AiServices
            |       |
            |       +---> GoogleAiGeminiChatModel
            |       |
            |       v
            |   [Google Gemini API Call - 90 second timeout]
            |
            +---> Save as ErrorAnalysisAction
            |
            +---> Return JSON response
                    |
                    v
    JavaScript receives response
            |
            +---> Hide spinner
            |
            +---> Display analysis
            |
            +---> Done!
```

---

## Key Architectural Decisions

### 1. Synchronous Processing
- Backend waits for complete API response
- No streaming or chunking
- Simple but blocks on slow networks

### 2. Bottom-Up Log Parsing
- Starts from end of logs (most recent errors)
- Matches regex patterns
- Returns in chronological order
- Prevents huge log overhead

### 3. Result Caching
- Saves analysis in build action (ErrorAnalysisAction)
- Can reuse without re-querying API
- User prompted if cached result exists

### 4. Regex Pattern Filtering
- Priority 1: Parameter from UI/step
- Priority 2: Job property (per-job configuration)
- Priority 3: Global config patterns (admin settings)
- If no patterns: analyze all logs

### 5. Security
- All endpoints require Jenkins READ permission
- CSRF token required for all POST requests
- API key stored as Jenkins Secret

---

## Critical Files Reference

| File | Lines | Purpose |
|------|-------|---------|
| `analyzer-error-footer.js` | 359 | All JavaScript logic, AJAX calls, UI updates |
| `ConsolePageDecorator/footer.jelly` | 56 | HTML containers, spinner, dialog |
| `ErrorAnalysisAction/index.jelly` | 17 | Display saved analysis |
| `ConsoleAnalyzeErrorAction.java` | 217 | 4 main endpoints (explain, getFilteredLogs, checkExisting, checkBuildStatus) |
| `ErrorAnalyzer.java` | 350 | Log extraction with regex, filtering logic |
| `BaseAIService.java` | 103 | Prompt template, API call wrapper |
| `GeminiService.java` | 81 | Gemini model configuration, LangChain4j setup |
| `ErrorAnalysisAction.java` | 78 | Build action for storing/displaying results |
| `GlobalConfigurationImpl.java` | N/A | Admin settings (API key, patterns, model) |

---

## The 4 AJAX Endpoints

### 1. checkBuildStatus
```
POST /console-analyzer-error/checkBuildStatus
Response: { buildingStatus: 0|1|2 }
Purpose: Determine if button should be shown
Time: Instant
```

### 2. checkExistingAnalysis
```
POST /console-analyzer-error/checkExistingAnalysis
Response: { hasAnalysis: true|false, timestamp?: "2024-11-11 10:30:00" }
Purpose: Check if cached result exists
Time: Instant
```

### 3. getFilteredLogs
```
POST /console-analyzer-error/getFilteredLogs
Response: JSON-encoded filtered logs string
Purpose: Show user what will be sent to AI (transparent)
Time: ~100-500ms (regex matching)
```

### 4. explainConsoleError [MAIN CALL]
```
POST /console-analyzer-error/explainConsoleError
Parameters: forceNew=true|false, maxLines=int
Response: JSON-encoded analysis string
Purpose: Get AI analysis
Time: 5-30 seconds (depends on Gemini API latency)
Action: Saves result, reusable
```

---

## Current Progress Indication

**What exists:**
- Simple spinner with text "Analyzing error logs..."
- Spinner hides when response arrives
- No percentage, no ETA, no intermediate updates

**What's missing:**
- No progress bar
- No chunked responses
- No streaming capability
- No WebSocket support
- No SSE implementation

---

## Why No Progress Bar Currently?

The architecture is **completely blocking**:

1. Request sent to backend
2. Backend waits 100% of time for API response
3. Response returned all at once
4. No intermediate checkpoints to report progress

To add progress bar, would need:
- Option A: Split into multiple endpoints with phases
- Option B: Server-Sent Events (SSE) streaming
- Option C: WebSocket bidirectional communication
- Option D: Polling mechanism (inefficient)

**Estimate to implement**: 4-6 hours + testing

---

## The Log Processing Algorithm

```
Input: Run, maxLines=100

Step 1: Determine which patterns to use
  - Priority 1: errorPatterns parameter
  - Priority 2: Job property ErrorPatternProperty
  - Priority 3: None (analyze all)

Step 2: If patterns exist, compile them as regex
  - Case insensitive matching
  - Pattern.compile(str, CASE_INSENSITIVE)

Step 3: Fetch logs (up to 10x maxLines or 10000)
  - List<String> allLogLines = run.getLog(fetchLimit)

Step 4: Parse bottom-up (newest errors first)
  for i = allLogLines.size() - 1 down to 0:
    if line matches any pattern:
      matchedLines.add(line)
      count++
      if count >= maxLines: break

Step 5: Reverse to restore chronological order
  - Collections.reverse(matchedLines)

Output: Newline-joined filtered logs
```

---

## The AI Prompt

```
Senior dev: analyze this failure. Skip obvious stuff.

{{errorLogs}}

1. Root cause (environment/dependency/config issues only)
2. Fix (1-2 sentences max)
3. Prevention (if non-trivial)

MAX 5 LINES TOTAL. Plain text only.
```

Key points:
- Designed for conciseness
- Focuses on root cause, not symptoms
- Limits to 5 lines
- Plain text (no Markdown)

---

## UI Container Relationships

```
Console Page
    |
    +-- analyzer-error-logs-container (hidden)
    |   |
    |   +-- l:card (logs title)
    |   |
    |   +-- pre#analyzer-error-logs-content (text)
    |
    +-- analyzer-error-container (hidden)
    |   |
    |   +-- l:card (analysis title)
    |   |
    |   +-- div#analyzer-error-spinner (hidden)
    |   |   |
    |   |   +-- l:spinner (Jenkins spinner)
    |   |
    |   +-- pre#analyzer-error-content (text)
    |
    +-- analyzer-error-confirm-dialog (hidden)
        |
        +-- l:card (existing analysis prompt)
        |
        +-- buttons (View Existing, Generate New, Cancel)
```

All use `jenkins-hidden` class for visibility toggle.

---

## Summary of Findings

1. **Architecture**: Request-response, no streaming
2. **Frontend**: Pure JavaScript (Fetch API)
3. **Backend**: Java, single endpoint class
4. **Logging**: Regex filtering, bottom-up parsing
5. **API**: Google Gemini via LangChain4j
6. **Caching**: Results stored in build action
7. **UI**: Jenkins Jelly templates, standard spinner
8. **Progress**: Not currently trackable (blocking)
9. **Timeout**: 90 seconds for entire operation
10. **Security**: CSRF protected, permission checks

---

## To Understand More...

Read these in order:
1. **QUICK_REFERENCE.md** - Code paths and file locations
2. **AI_ANALYZE_ARCHITECTURE.md** - Detailed architecture
3. **CODE_SNIPPETS.md** - Key code excerpts
4. **This file (SUMMARY.md)** - Overview

---

## Next Steps for Progress Bar

If you decide to add a progress bar:

1. **Phase 1**: Extract logs asynchronously
   - Modify `doExplainConsoleError()` to return early
   - Create separate `doAnalyzeLogsAsync()` endpoint
   - Report back with progress events

2. **Phase 2**: Stream from Gemini
   - Check if LangChain4j supports streaming
   - Gemini API supports it natively
   - Stream response as tokens arrive

3. **Phase 3**: Update UI
   - Replace spinner with progress bar
   - Add percentage display
   - Show estimated time remaining

**Total estimate**: 6-8 hours of development + testing

