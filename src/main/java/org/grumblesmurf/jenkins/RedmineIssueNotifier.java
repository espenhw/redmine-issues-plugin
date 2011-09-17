package org.grumblesmurf.jenkins;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.tasks.*;
import org.kohsuke.stapler.DataBoundConstructor;

public class RedmineIssueNotifier extends Notifier
{
    public final String redmineUrl;
    public final String apiKey;

    @DataBoundConstructor
    public RedmineIssueNotifier(String redmineUrl, String apiKey) {
        this.redmineUrl = redmineUrl;
        this.apiKey = apiKey;
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
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
