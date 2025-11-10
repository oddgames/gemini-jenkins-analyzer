package io.jenkins.plugins.gemini_jenkins_analyzer;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Predefined error pattern presets for common build environments.
 */
public enum ErrorPatternPreset {
    NONE("None - Custom Patterns", Collections.emptyList()),

    UNITY("Unity (Xcode, Android, iOS)",
        Arrays.asList(
            // Unity-specific errors
            "(?i)\\bError\\s*:\\s*",
            "(?i)\\bCompilerError\\b",
            "(?i)\\bBuildFailedException\\b",
            "(?i)\\bUnityException\\b",
            "(?i)Assets/.*\\.cs\\(\\d+,\\d+\\):\\s*error",

            // Xcode errors (strict)
            "(?i)^\\s*\\*\\*\\s*BUILD FAILED\\s*\\*\\*",
            "(?i)ld:\\s*error:",
            "(?i)clang:\\s*error:",
            "(?i)error:\\s*(?:linker command failed|Build input file cannot be found)",
            "(?i)❌\\s*.*error",
            "(?i)\\berror:\\s*[^\\s]",

            // Android/Gradle errors (strict)
            "(?i)BUILD FAILED",
            "(?i)FAILURE:\\s*Build failed",
            "(?i)^\\s*>\\s*Task.*FAILED",
            "(?i)Execution failed for task",
            "(?i)\\* What went wrong:",
            "(?i)error:\\s*package .* does not exist",
            "(?i)error:\\s*cannot find symbol",

            // Make/Build tool errors
            "(?i)make:\\s*\\*\\*\\*.*Error",
            "(?i)make\\[\\d+\\]:\\s*\\*\\*\\*.*Error",
            "(?i)ninja:\\s*build stopped"
        ));

    private final String displayName;
    private final List<String> patterns;

    ErrorPatternPreset(String displayName, List<String> patterns) {
        this.displayName = displayName;
        this.patterns = patterns;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getPatterns() {
        return patterns;
    }

    /**
     * Get preset by name, returns NONE if not found.
     */
    public static ErrorPatternPreset fromString(String name) {
        if (name == null || name.trim().isEmpty()) {
            return NONE;
        }
        try {
            return ErrorPatternPreset.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return NONE;
        }
    }
}
