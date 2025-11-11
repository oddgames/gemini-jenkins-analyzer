# Architecture Documentation Index

Complete analysis of the AI Analyze feature for the Gemini Jenkins Analyzer plugin.

## Documents Overview

This analysis consists of 4 comprehensive documents that explain the AI analyze feature from different perspectives:

### 1. SUMMARY.md (START HERE)
**Length**: ~400 lines | **Time to read**: 10-15 minutes

Best for: Quick understanding and overall picture
- One-minute overview
- Architecture components breakdown
- Data flow diagram
- Key architectural decisions
- Critical files reference
- Progress bar feasibility

**Read this first if you only have 15 minutes.**

### 2. QUICK_REFERENCE.md
**Length**: ~150 lines | **Time to read**: 5 minutes

Best for: Quick lookups and specific information
- Key takeaways
- Critical code paths
- File locations table
- UI container IDs
- Backend request flow
- Current limitations
- Implementation effort estimate

**Use this when you need to find a specific piece quickly.**

### 3. AI_ANALYZE_ARCHITECTURE.md
**Length**: ~600 lines | **Time to read**: 30-45 minutes

Best for: Deep understanding and detailed architecture
- Executive summary
- UI components (1.1-1.3)
- Log parsing logic (2.1-2.3)
- Backend-to-frontend communication (3.1-3.3)
- Progress tracking mechanisms (4.1-4.3)
- Jelly files and JavaScript handlers (5.1-5.2)
- Architecture insights (6.1-6.3)
- Progress bar feasibility assessment (7)
- Technical constraints (9)

**Read this to understand every detail.**

### 4. CODE_SNIPPETS.md
**Length**: ~400 lines | **Time to read**: 20 minutes

Best for: Code-level understanding and implementation reference
- Frontend JavaScript entry point
- Frontend AJAX calls
- Frontend UI updates
- Backend endpoints
- Log processing methods
- Log filtering algorithm
- AI service integration
- Gemini model configuration
- UI templates
- Build action storage

**Reference this when implementing changes.**

---

## Quick Navigation

### I want to understand...

**The overall architecture** → Read SUMMARY.md (sections: One-Minute Overview, Architecture Components, Data Flow Diagram)

**How the UI works** → Read SUMMARY.md (UI Container Relationships) + CODE_SNIPPETS.md (sections 1, 3, 9)

**How logs are processed** → Read AI_ANALYZE_ARCHITECTURE.md (section 2) + CODE_SNIPPETS.md (section 6)

**How the API is called** → Read CODE_SNIPPETS.md (sections 7, 8)

**How to add a feature** → Read QUICK_REFERENCE.md (Critical Code Paths) + CODE_SNIPPETS.md (relevant section)

**Why there's no progress bar** → Read SUMMARY.md (Why No Progress Bar Currently?) or AI_ANALYZE_ARCHITECTURE.md (section 7)

**The exact file locations** → Read QUICK_REFERENCE.md (File Locations)

---

## Key Terms Explained

| Term | Meaning | Where to Learn |
|------|---------|-----------------|
| Jelly | Jenkins template language (XML) | AI_ANALYZE_ARCHITECTURE.md §5 |
| Stapler | Jenkins web framework | AI_ANALYZE_ARCHITECTURE.md §3 |
| CSRF | Security token for POST requests | SUMMARY.md, AI_ANALYZE_ARCHITECTURE.md §3.1 |
| ErrorAnalyzer | Class that processes logs | CODE_SNIPPETS.md §5 |
| LangChain4j | Java AI integration library | CODE_SNIPPETS.md §7 |
| Bottom-up parsing | Processing logs newest-first | SUMMARY.md (Log Processing Algorithm) |
| ErrorAnalysisAction | Build action storing results | CODE_SNIPPETS.md §10 |
| Gemini API | Google's AI model API | SUMMARY.md (The AI Prompt) |

---

## File Structure in Repository

```
/src/main/
  ├── java/io/jenkins/plugins/gemini_jenkins_analyzer/
  │   ├── ConsoleAnalyzeErrorAction.java      [Main endpoint handler]
  │   ├── ErrorAnalyzer.java                   [Log processing]
  │   ├── AIService.java                       [AI service factory]
  │   ├── GeminiService.java                   [Gemini model setup]
  │   ├── BaseAIService.java                   [API call wrapper]
  │   ├── ErrorAnalysisAction.java             [Build action storage]
  │   ├── ConsolePageDecorator.java            [UI integration]
  │   └── [other classes...]
  │
  ├── resources/io/jenkins/plugins/gemini_jenkins_analyzer/
  │   ├── ConsolePageDecorator/footer.jelly    [UI containers]
  │   ├── ErrorAnalysisAction/index.jelly      [Results display]
  │   ├── GlobalConfigurationImpl/config.jelly [Admin settings]
  │   └── [other templates...]
  │
  └── webapp/js/
      └── analyzer-error-footer.js             [All JavaScript logic]
```

---

## The Request Flow (At a Glance)

```
1. User clicks "Analyze Error"
   ↓
2. JavaScript: checkBuildStatus()
   ↓
3. JavaScript: checkExistingAnalysis()
   ↓
4. JavaScript: getFilteredLogs()
   ↓
5. (Display filtered logs to user)
   ↓
6. JavaScript: explainConsoleError() [MAIN CALL - BLOCKING]
   ↓
7. Backend: ConsoleAnalyzeErrorAction.doExplainConsoleError()
   ↓
8. Backend: ErrorAnalyzer.extractErrorLogs()
   ↓
9. Backend: AIService.analyzeError()
   ↓
10. API Call: Google Gemini (90 second timeout)
   ↓
11. Backend: Save ErrorAnalysisAction
   ↓
12. Response: JSON with analysis
   ↓
13. Frontend: Display results
```

---

## Key Architectural Constraints

1. **Synchronous** - No streaming, no chunking
2. **Blocking** - Backend waits for API response
3. **Simple caching** - Results stored in build action
4. **Regex-based filtering** - Pattern matching for logs
5. **90-second timeout** - Hard limit for API calls
6. **CSRF protected** - All POST endpoints require token
7. **Permission checked** - Requires Item.READ permission

---

## To Add a Progress Bar

**Minimum changes needed:**
1. Split `doExplainConsoleError()` into 2 phases
2. Create async endpoint or use SSE
3. Replace spinner with progress bar UI
4. Update JavaScript to handle progress events

**Estimated effort:** 4-6 hours

For detailed analysis, see: AI_ANALYZE_ARCHITECTURE.md (section 7) or SUMMARY.md (section "Next Steps for Progress Bar")

---

## Total Information

- **4 Documents**
- **~1,500 lines of analysis**
- **50+ code snippets**
- **10+ diagrams**
- **Complete architecture explanation**

---

## How to Use This Analysis

1. **First time?** Start with SUMMARY.md
2. **Need quick info?** Use QUICK_REFERENCE.md
3. **Implementing a feature?** Check CODE_SNIPPETS.md
4. **Need everything?** Read AI_ANALYZE_ARCHITECTURE.md
5. **Lost?** Come back to this index

---

## Related Files in Repository

- `README.md` - Project overview
- `CONTRIBUTING.md` - Contribution guidelines
- `pom.xml` - Maven configuration
- `.gitignore` - Git configuration

---

**Last updated:** November 11, 2025

**Analysis depth:** Complete (all components covered)

**Completeness:** 100% (all requested topics addressed)

