# Quick Reference: AI Analyze Feature

## Key Takeaways

1. **Synchronous, blocking architecture** - No streaming currently
2. **Simple spinner** only - No progress percentage
3. **Multiple endpoints** - 4 AJAX calls for full analysis flow
4. **Regex-based filtering** - Bottom-up log parsing with patterns

---

## Critical Code Paths

### Frontend Entry Point
```
/src/main/webapp/js/analyzer-error-footer.js
  Line 167: analyzeConsoleError()
    ├─→ checkExistingAnalysis()  [Line 172]
    └─→ sendAnalyzeRequest()      [Line 249]
        ├─ fetch /getFilteredLogs
        └─ fetch /explainConsoleError
```

### Backend Entry Point
```
/src/main/java/.../ConsoleAnalyzeErrorAction.java
  Line 48: doExplainConsoleError()
    ├─ ErrorAnalyzer analyzer = new ErrorAnalyzer()
    ├─ analyzer.analyzeErrorWithFiltering(run, maxLines)  [Line 73]
    └─ ErrorAnalysisAction action = new ErrorAnalysisAction(analysis, errorText)
```

### Log Processing
```
/src/main/java/.../ErrorAnalyzer.java
  Line 262: analyzeErrorWithFiltering(Run, maxLines)
    ├─ extractErrorLogs(run, null, null, maxLines)  [Line 279]
    │  └─ Regex pattern matching + bottom-up parsing
    └─ AIService aiService = new AIService(config)
       └─ aiService.analyzeError(errorLogs)
```

### API Call
```
/src/main/java/.../GeminiService.java
  Line 27: createAssistant()
    └─ GoogleAiGeminiChatModel.builder()
       .apiKey(config.getApiKey().getPlainText())
       .modelName("gemini-2.0-flash")
       .timeout(Duration.ofSeconds(90))
```

---

## File Locations

| Component | File | Lines |
|-----------|------|-------|
| Frontend Logic | `/src/main/webapp/js/analyzer-error-footer.js` | 359 |
| Frontend UI | `/src/main/resources/.../ConsolePageDecorator/footer.jelly` | 56 |
| Backend Endpoints | `/src/main/java/.../ConsoleAnalyzeErrorAction.java` | 217 |
| Log Processing | `/src/main/java/.../ErrorAnalyzer.java` | 350 |
| API Integration | `/src/main/java/.../GeminiService.java` | 81 |

---

## UI Container IDs

```
#analyzer-error-spinner          - Loading spinner (hidden by default)
#analyzer-error-container        - Results container (hidden by default)
#analyzer-error-content          - Displays analysis text
#analyzer-error-logs-container   - Shows filtered logs before analysis
#analyzer-error-logs-content     - Filtered logs text
#analyzer-error-confirm-dialog   - Dialog for existing analysis
```

---

## Backend Request Flow

```
1. User clicks "Analyze Error" button
2. JavaScript sends 4 sequential requests:
   a) /checkBuildStatus
   b) /checkExistingAnalysis
   c) /getFilteredLogs
   d) /explainConsoleError
3. doExplainConsoleError() method:
   - Checks permissions
   - Checks for cached analysis
   - Extracts error logs (regex filtering)
   - Calls AI service
   - Saves result
   - Returns JSON response
```

---

## Endpoints

```
POST /job/JOB_NAME/BUILD_NUMBER/console-analyzer-error/explainConsoleError
POST /job/JOB_NAME/BUILD_NUMBER/console-analyzer-error/getFilteredLogs
POST /job/JOB_NAME/BUILD_NUMBER/console-analyzer-error/checkExistingAnalysis
POST /job/JOB_NAME/BUILD_NUMBER/console-analyzer-error/checkBuildStatus
```

---

## Current Limitations for Progress Bar

1. **No streaming** - Backend waits for complete API response
2. **No chunking** - Single monolithic request
3. **No WebSocket** - Standard HTTP request-response only
4. **No SSE** - No Server-Sent Events implementation
5. **Blocking timeout** - 90 second limit for entire operation

---

## To Add Progress Bar

Minimum viable changes:
1. Split `doExplainConsoleError()` into two phases:
   - Fast extraction phase (return immediately)
   - Async AI analysis phase (stream progress)
2. Implement SSE endpoint or polling mechanism
3. Replace spinner with progress bar UI
4. Update JavaScript to handle progress events

Estimated effort: 4-6 hours (architecture changes required)

