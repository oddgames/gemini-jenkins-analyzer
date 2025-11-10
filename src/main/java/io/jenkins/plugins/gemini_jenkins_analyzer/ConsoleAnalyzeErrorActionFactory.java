package io.jenkins.plugins.gemini_jenkins_analyzer;

import hudson.Extension;
import hudson.model.Action;
import hudson.model.Run;
import jenkins.model.TransientActionFactory;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Logger;
import javax.annotation.Nonnull;

/**
 * TransientActionFactory to dynamically inject ConsoleAnalyzeErrorAction into all runs.
 * This approach works for both new and existing runs, unlike RunListener which only
 * works for runs started after the plugin was installed.
 */
@Extension
public class ConsoleAnalyzeErrorActionFactory extends TransientActionFactory<Run<?, ?>> {

    private static final Logger LOGGER = Logger.getLogger(ConsoleAnalyzeErrorActionFactory.class.getName());

    @Override
    @SuppressWarnings("unchecked")
    public Class<Run<?, ?>> type() {
        return (Class<Run<?, ?>>) (Class<?>) Run.class;
    }

    @Nonnull
    @Override
    public Collection<? extends Action> createFor(@Nonnull Run<?, ?> run) {
        try {
            // Create and return the ConsoleAnalyzeErrorAction for this run
            ConsoleAnalyzeErrorAction action = new ConsoleAnalyzeErrorAction(run);
            return Collections.singletonList(action);
        } catch (Exception e) {
            LOGGER.severe("Failed to create ConsoleAnalyzeErrorAction for run: " + run.getFullDisplayName() + ". Error: " + e.getMessage());
            return Collections.emptyList();
        }
    }
}
