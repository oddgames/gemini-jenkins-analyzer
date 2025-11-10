package io.jenkins.plugins.gemini_jenkins_analyzer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import hudson.util.Secret;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Fast unit tests for AIService that don't require Jenkins.
 * These tests use mocks and run in milliseconds instead of seconds.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AIServiceUnitTest {

    @Mock
    private GlobalConfigurationImpl config;

    private AIService aiService;

    @BeforeEach
    void setUp() {
        // Set up default mock behavior
        when(config.getProvider()).thenReturn(AIProvider.GEMINI);
        when(config.getApiKey()).thenReturn(Secret.fromString("test-api-key"));
        when(config.getModel()).thenReturn("gemini-2.0-flash");
        when(config.getApiUrl()).thenReturn(null);
        when(config.isEnableAnalysis()).thenReturn(true);

        aiService = new AIService(config);
    }

    @Test
    void testAnalyzeErrorWithNullInput() throws IOException {
        String result = aiService.analyzeError(null);
        assertEquals("No error logs provided for analysis.", result);
    }

    @Test
    void testAnalyzeErrorWithEmptyInput() throws IOException {
        String result = aiService.analyzeError("");
        assertEquals("No error logs provided for analysis.", result);
    }

    @Test
    void testAnalyzeErrorWithBlankInput() throws IOException {
        String result = aiService.analyzeError("   ");
        assertEquals("No error logs provided for analysis.", result);
    }

    @Test
    void testAnalyzeErrorWithWhitespaceOnlyInput() throws IOException {
        String result = aiService.analyzeError("\n\t  \n");
        assertEquals("No error logs provided for analysis.", result);
    }

    @Test
    void testAnalyzeErrorWithValidInput() throws IOException {
        String errorLogs = "ERROR: Failed to compile\n" +
                          "FAILED: Task execution failed\n" +
                          "BUILD FAILED in 2s";

        // Since we don't have a real API key or network connection,
        // the service should handle the error gracefully
        String result = aiService.analyzeError(errorLogs);

        // The result should not be the "no error logs" message
        assertNotEquals("No error logs provided for analysis.", result);
        assertNotNull(result);
        assertFalse(result.trim().isEmpty());
    }

    @Test
    void testErrorLogsProcessing() throws IOException {
        String complexErrorLogs = "Started by user admin\n" +
                                 "Building in workspace /var/jenkins_home/workspace/test\n" +
                                 "ERROR: Could not find or load main class Application\n" +
                                 "FAILURE: Build failed with an exception.\n" +
                                 "* What went wrong:\n" +
                                 "Execution failed for task ':compileJava'.\n" +
                                 "> Compilation failed; see the compiler error output for details.\n" +
                                 "BUILD FAILED in 15s";

        String result = aiService.analyzeError(complexErrorLogs);

        // Should not return the "no error logs" message for valid input
        assertNotEquals("No error logs provided for analysis.", result);
        assertNotNull(result);
        assertFalse(result.trim().isEmpty());
    }

    @Test
    void testServiceWithNullApiKey() {
        when(config.getApiKey()).thenReturn(null);
        AIService serviceWithNullKey = new AIService(config);

        String result = assertDoesNotThrow(() -> serviceWithNullKey.analyzeError("Some error"));

        // Should handle null API key gracefully
        assertNotNull(result);
        assertFalse(result.trim().isEmpty());
    }

    @Test
    void testServiceWithEmptyApiKey() {
        when(config.getApiKey()).thenReturn(Secret.fromString(""));
        AIService serviceWithEmptyKey = new AIService(config);

        String result = assertDoesNotThrow(() -> serviceWithEmptyKey.analyzeError("Some error"));

        // Should handle empty API key gracefully
        assertNotNull(result);
        assertFalse(result.trim().isEmpty());
    }

    @Test
    void testServiceWithNullModel() {
        when(config.getModel()).thenReturn(null);
        AIService serviceWithNullModel = new AIService(config);

        String result = assertDoesNotThrow(() -> serviceWithNullModel.analyzeError("Some error"));

        // Should handle null model gracefully
        assertNotNull(result);
    }

    @Test
    void testLongErrorLogs() throws IOException {
        // Test with moderately long error logs
        StringBuilder longErrorLog = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            longErrorLog.append("ERROR: Line ").append(i).append(" of error log\n");
        }

        String result = aiService.analyzeError(longErrorLog.toString());

        // Should handle long input without throwing exception
        assertNotEquals("No error logs provided for analysis.", result);
        assertNotNull(result);
    }

    @Test
    void testSpecialCharactersInErrorLogs() throws IOException {
        String errorLogsWithSpecialChars = "ERROR: Failed to process file 'test@#$%^&*().txt'\n" +
                                          "FAILURE: Build failed with special chars: <>&\"'\n" +
                                          "Unicode characters: ñáéíóú 中文 العربية";

        String result = aiService.analyzeError(errorLogsWithSpecialChars);

        // Should handle special characters without throwing exception
        assertNotEquals("No error logs provided for analysis.", result);
        assertNotNull(result);
    }

    @Test
    void testMultilineErrorLogs() throws IOException {
        String multilineErrorLogs = "ERROR: Multiple\n" +
                                   "lines\n" +
                                   "of\n" +
                                   "error\n" +
                                   "messages\n" +
                                   "BUILD FAILED";

        String result = aiService.analyzeError(multilineErrorLogs);

        // Should handle multiline input properly
        assertNotEquals("No error logs provided for analysis.", result);
        assertNotNull(result);
    }

    @Test
    void testJSONEscaping() throws IOException {
        String errorLogsWithJSON = "ERROR: JSON parsing failed\n" +
                                   "Expected: {\"key\": \"value\"}\n" +
                                   "Actual: {\"key\": \"broken";

        String result = aiService.analyzeError(errorLogsWithJSON);

        // Should handle JSON-like content without breaking
        assertNotEquals("No error logs provided for analysis.", result);
        assertNotNull(result);
    }

    @Test
    void testGeminiProviderConfiguration() {
        when(config.getProvider()).thenReturn(AIProvider.GEMINI);
        when(config.getApiKey()).thenReturn(Secret.fromString("test-gemini-key"));

        AIService geminiService = new AIService(config);
        String result = assertDoesNotThrow(() -> geminiService.analyzeError("Test error"));

        // Should create Gemini service successfully
        assertNotNull(result);
        assertFalse(result.trim().isEmpty());
        assertNotEquals("No error logs provided for analysis.", result);
    }

    @Test
    void testServiceCreationWithDifferentProviders() {
        // Test that service can be created with GEMINI provider
        when(config.getProvider()).thenReturn(AIProvider.GEMINI);
        AIService geminiService = new AIService(config);
        assertNotNull(geminiService);
    }

    @Test
    void testVeryLongSingleLineErrorLog() throws IOException {
        // Test with a very long single line (potential edge case)
        StringBuilder longLine = new StringBuilder("ERROR: ");
        for (int i = 0; i < 1000; i++) {
            longLine.append("very long error message part ").append(i).append(" ");
        }

        String result = aiService.analyzeError(longLine.toString());

        assertNotEquals("No error logs provided for analysis.", result);
        assertNotNull(result);
    }

    @Test
    void testTabsAndSpecialWhitespace() throws IOException {
        String errorWithTabs = "ERROR:\tTabbed\terror\tmessage\n" +
                              "\t\tIndented error details\n" +
                              "BUILD FAILED";

        String result = aiService.analyzeError(errorWithTabs);

        assertNotEquals("No error logs provided for analysis.", result);
        assertNotNull(result);
    }
}
