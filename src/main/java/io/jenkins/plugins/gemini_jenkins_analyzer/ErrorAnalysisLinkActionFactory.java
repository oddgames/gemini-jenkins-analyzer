package io.jenkins.plugins.gemini_jenkins_analyzer;

import hudson.Extension;
import hudson.model.Action;
import hudson.model.Run;
import jenkins.model.TransientActionFactory;
import java.util.Collection;
import java.util.Collections;
import javax.annotation.Nonnull;

/**
 * Factory to add the "Analyze Errors" link to all completed builds.
 */
@Extension
public class ErrorAnalysisLinkActionFactory extends TransientActionFactory<Run<?, ?>> {

    @Override
    @SuppressWarnings("unchecked")
    public Class<Run<?, ?>> type() {
        return (Class<Run<?, ?>>) (Class<?>) Run.class;
    }

    @Nonnull
    @Override
    public Collection<? extends Action> createFor(Run<?, ?> target) {
        return Collections.singletonList(new ErrorAnalysisLinkAction(target));
    }
}
