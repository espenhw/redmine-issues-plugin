package org.grumblesmurf.jenkins.redmine;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.scm.ChangeLogSet;
import hudson.tasks.*;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.io.PrintStream;

public class IssueNotifier extends Notifier
{
    public final String redmineUrl;
    public final String apiKey;

    @DataBoundConstructor
    public IssueNotifier(String redmineUrl, String apiKey) {
        this.redmineUrl = redmineUrl;
        this.apiKey = apiKey;
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
          throws InterruptedException, IOException {
        PrintStream console = listener.getLogger();
        debug(console, "Looking for build changes");
        for (ChangeLogSet.Entry entry : build.getChangeSet()) {
            debug(console, "Looking at %s", entry.getMsg());
        }
        return true;
    }

    private void debug(PrintStream console, String fmt, Object... args) {
        console.println("[RIN] DEBUG: " + String.format(fmt, args));
    }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher>
    {
        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Publish build result to Redmine issues";
        }
    }
}
