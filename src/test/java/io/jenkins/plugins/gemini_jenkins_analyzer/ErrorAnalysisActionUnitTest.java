package io.jenkins.plugins.gemini_jenkins_analyzer;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Fast unit tests for ErrorAnalysisAction that don't require Jenkins.
 */
class ErrorAnalysisActionUnitTest {

    private ErrorAnalysisAction action;
    private String testAnalysis;
    private String testErrorLogs;

    @BeforeEach
    void setUp() {
        testAnalysis = "This is a test analysis of the error";
        testErrorLogs = "ERROR: Build failed\nFAILED: Compilation error";
        action = new ErrorAnalysisAction(testAnalysis, testErrorLogs);
    }

    @Test
    void testConstructor() {
        assertNotNull(action);
        assertEquals(testAnalysis, action.getAnalysis());
        assertEquals(testErrorLogs, action.getOriginalErrorLogs());
        assertTrue(action.getTimestamp() > 0);
        assertTrue(action.getTimestamp() <= System.currentTimeMillis());
    }

    @Test
    void testGetIconFileName() {
        assertEquals("symbol-cube", action.getIconFileName());
    }

    @Test
    void testGetDisplayName() {
        assertEquals("AI Error Analysis", action.getDisplayName());
    }

    @Test
    void testGetUrlName() {
        assertEquals("error-analysis", action.getUrlName());
    }

    @Test
    void testGetAnalysis() {
        assertEquals(testAnalysis, action.getAnalysis());
    }

    @Test
    void testGetOriginalErrorLogs() {
        assertEquals(testErrorLogs, action.getOriginalErrorLogs());
    }

    @Test
    void testGetTimestamp() {
        long timestamp = action.getTimestamp();
        assertTrue(timestamp > 0);
        assertTrue(timestamp <= System.currentTimeMillis());
    }

    @Test
    void testGetFormattedTimestamp() {
        String formatted = action.getFormattedTimestamp();
        assertNotNull(formatted);
        assertFalse(formatted.trim().isEmpty());
    }

    @Test
    void testWithNullAnalysis() {
        ErrorAnalysisAction nullAction = new ErrorAnalysisAction(null, testErrorLogs);
        assertNull(nullAction.getAnalysis());
        assertEquals(testErrorLogs, nullAction.getOriginalErrorLogs());
    }

    @Test
    void testWithNullErrorLogs() {
        ErrorAnalysisAction nullAction = new ErrorAnalysisAction(testAnalysis, null);
        assertEquals(testAnalysis, nullAction.getAnalysis());
        assertNull(nullAction.getOriginalErrorLogs());
    }

    @Test
    void testWithEmptyStrings() {
        ErrorAnalysisAction emptyAction = new ErrorAnalysisAction("", "");
        assertEquals("", emptyAction.getAnalysis());
        assertEquals("", emptyAction.getOriginalErrorLogs());
    }

    @Test
    void testWithLongAnalysis() {
        StringBuilder longAnalysis = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longAnalysis.append("This is line ").append(i).append(" of a very long analysis.\n");
        }

        ErrorAnalysisAction longAction = new ErrorAnalysisAction(longAnalysis.toString(), testErrorLogs);
        assertEquals(longAnalysis.toString(), longAction.getAnalysis());
    }

    @Test
    void testWithSpecialCharacters() {
        String specialAnalysis = "Error with special chars: <>&\"'\nUnicode: ñáéíóú 中文 العربية";
        String specialErrorLogs = "ERROR: File 'test@#$%^&*().txt' not found";

        ErrorAnalysisAction specialAction = new ErrorAnalysisAction(specialAnalysis, specialErrorLogs);
        assertEquals(specialAnalysis, specialAction.getAnalysis());
        assertEquals(specialErrorLogs, specialAction.getOriginalErrorLogs());
    }

    @Test
    void testTimestampConsistency() throws InterruptedException {
        long beforeCreation = System.currentTimeMillis();
        Thread.sleep(10); // Small delay to ensure timestamp difference

        ErrorAnalysisAction timedAction = new ErrorAnalysisAction("test", "test");

        Thread.sleep(10); // Small delay to ensure timestamp difference
        long afterCreation = System.currentTimeMillis();

        long actionTimestamp = timedAction.getTimestamp();
        assertTrue(actionTimestamp >= beforeCreation);
        assertTrue(actionTimestamp <= afterCreation);
    }

    @Test
    void testHasValidAnalysis() {
        // Test with valid analysis
        ErrorAnalysisAction validAction = new ErrorAnalysisAction("Valid analysis", "Error logs");
        assertTrue(validAction.hasValidAnalysis());

        // Test with null analysis
        ErrorAnalysisAction nullAction = new ErrorAnalysisAction(null, "Error logs");
        assertFalse(nullAction.hasValidAnalysis());

        // Test with empty analysis
        ErrorAnalysisAction emptyAction = new ErrorAnalysisAction("", "Error logs");
        assertFalse(emptyAction.hasValidAnalysis());

        // Test with whitespace-only analysis
        ErrorAnalysisAction whitespaceAction = new ErrorAnalysisAction("   \n  \t  ", "Error logs");
        assertFalse(whitespaceAction.hasValidAnalysis());

        // Test with analysis containing only spaces
        ErrorAnalysisAction spacesAction = new ErrorAnalysisAction("     ", "Error logs");
        assertFalse(spacesAction.hasValidAnalysis());

        // Test with valid analysis containing whitespace
        ErrorAnalysisAction validWithWhitespaceAction = new ErrorAnalysisAction("  Valid analysis  ", "Error logs");
        assertTrue(validWithWhitespaceAction.hasValidAnalysis());
    }
}
