package io.jenkins.plugins.gemini_jenkins_analyzer;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

/**
 * Structured response for error analysis using LangChain4j structured output.
 * This class represents the AI's analysis of Jenkins build errors in a structured format.
 */
public class ErrorAnalysis {

    @JsonProperty("summary")
    @JsonPropertyDescription("A concise summary of what caused the error")
    private String summary;

    @JsonProperty("resolutionSteps")
    @JsonPropertyDescription("Specific steps to resolve the issue")
    private List<String> resolutionSteps;

    @JsonProperty("bestPractices")
    @JsonPropertyDescription("Relevant best practices to prevent similar issues")
    private List<String> bestPractices;

    public ErrorAnalysis() {
        // Default constructor required for JSON deserialization
    }

    public ErrorAnalysis(String summary, List<String> resolutionSteps, List<String> bestPractices) {
        this.summary = summary;
        this.resolutionSteps = resolutionSteps;
        this.bestPractices = bestPractices;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<String> getResolutionSteps() {
        return resolutionSteps;
    }

    public void setResolutionSteps(List<String> resolutionSteps) {
        this.resolutionSteps = resolutionSteps;
    }

    public List<String> getBestPractices() {
        return bestPractices;
    }

    public void setBestPractices(List<String> bestPractices) {
        this.bestPractices = bestPractices;
    }

    /**
     * Formats the error analysis as a readable plain text string.
     * @return formatted text representation
     */
    public String toFormattedString() {
        StringBuilder sb = new StringBuilder();

        sb.append("SUMMARY:\n");
        sb.append(summary != null ? summary : "No summary available").append("\n\n");

        sb.append("RESOLUTION STEPS:\n");
        if (resolutionSteps != null && !resolutionSteps.isEmpty()) {
            for (int i = 0; i < resolutionSteps.size(); i++) {
                sb.append((i + 1)).append(". ").append(resolutionSteps.get(i)).append("\n");
            }
        } else {
            sb.append("No resolution steps provided\n");
        }
        sb.append("\n");

        sb.append("BEST PRACTICES:\n");
        if (bestPractices != null && !bestPractices.isEmpty()) {
            for (int i = 0; i < bestPractices.size(); i++) {
                sb.append((i + 1)).append(". ").append(bestPractices.get(i)).append("\n");
            }
        } else {
            sb.append("No best practices provided\n");
        }

        return sb.toString();
    }

    @Override
    public String toString() {
        return toFormattedString();
    }
}
