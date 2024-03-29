package org.grumblesmurf.jenkins.redmine;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.scm.ChangeLogSet;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;
import org.redmine.ta.AuthenticationException;
import org.redmine.ta.NotFoundException;
import org.redmine.ta.RedmineException;

import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.util.Collection;
import java.util.Map;

public class IssueNotifier extends Notifier
{
    public final String redmineUrl;
    public final String apiKey;
    public final String referencedStatus;
    public final String closedStatus;

    public IssueNotifier(String redmineUrl, String apiKey, String referencedStatus, String closedStatus) {
        this.redmineUrl = redmineUrl;
        this.apiKey = apiKey;
        this.referencedStatus = referencedStatus;
        this.closedStatus = closedStatus;
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
        throws InterruptedException, IOException {
        PrintStream console = listener.getLogger();

        if (build.getResult().isWorseThan(Result.SUCCESS)) {
            debug(console, "Skipping unsuccessful build");
            return true;
        }

        debug(console, "Looking for changes since last stable build");
        AbstractBuild<?, ?> lastStableBuild = build.getProject().getLastStableBuild();
        debug(console, "Last stable build was %s", lastStableBuild);
        if (lastStableBuild == null) {
            lastStableBuild = build.getProject().getFirstBuild();
        } else {
            // We don't want the changes from the last stable build
            lastStableBuild = lastStableBuild.getNextBuild();
        }
        debug(console, "Considering changes from build %s to %s", lastStableBuild.number, build.number);

        Multimap<Integer, BuildReference> buildReferences = findAllChangesBetween(lastStableBuild, build, console);

        try {
            Redmine redmine = getRedmine();
            for (Map.Entry<Integer, Collection<BuildReference>> entry : buildReferences.asMap().entrySet()) {
                try {
                    redmine.addBuildReferencesToIssue(entry.getKey(), build.number,
                                                      new URL(new URL(Jenkins.getInstance().getRootUrl()),
                                                              build.getUrl()),
                                                      entry.getValue());
                } catch (RedmineException e) {
                    console.println("ERROR: Redmine denied update of issue #" + entry.getKey());
                    console.println(e.getMessage());
                } catch (NotFoundException e) {
                    console.println("ERROR: Referenced issue #" + entry.getKey() + " not found");
                }
            }

            if (!buildReferences.isEmpty()) {
                try {
                    build.getActions().add(new RedmineIssueLinksAction(redmine, buildReferences));
                } catch (RedmineException e) {
                    console.println("ERROR: Redmine denied fetch");
                    console.println(e.getMessage());
                } catch (NotFoundException e) {
                    // Was reported above
                }
            }
        } catch (AuthenticationException e) {
            console.println("ERROR: Redmine authentication failure:");
            console.println(e.getMessage());
            return false;
        }
        return true;

    }

    private Multimap<Integer, BuildReference> findAllChangesBetween(AbstractBuild<?, ?> from, AbstractBuild<?, ?> to,
                                                                    PrintStream console) {
        AbstractBuild<?, ?> b = from;
        Multimap<Integer, BuildReference> buildReferences = LinkedListMultimap.create();
        do {
            debug(console, "Looking for changes in %s", b);
            findChangesIn(b, console, buildReferences);
            b = b.getNextBuild();
        } while (b != to.getNextBuild());
        return buildReferences;
    }

    private void findChangesIn(AbstractBuild<?, ?> build, PrintStream console,
                               Multimap<Integer, BuildReference> buildReferences) {
        for (ChangeLogSet.Entry entry : build.getChangeSet()) {
            String message = entry.getMsg();
            debug(console, "Looking at %s", message);
            CommitMessageParser commitParser = new CommitMessageParser();
            for (Integer issueId : commitParser.referencedIssueIdsIn(message)) {
                buildReferences.put(issueId, new BuildReference(BuildReference.Type.REFERENCED, build, entry));
            }
            for (Integer issueId : commitParser.closedIssueIdsIn(message)) {
                buildReferences.put(issueId, new BuildReference(BuildReference.Type.CLOSED, build, entry));
            }
        }
    }

    private void debug(PrintStream console, String fmt, Object... args) {
        console.println("[RIN] DEBUG: " + String.format(fmt, args));
    }

    @Override
    public BuildStepDescriptor getDescriptor() {
        return DESCRIPTOR;
    }

    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    private Redmine getRedmine() {
        return new Redmine(redmineUrl, apiKey, Integer.parseInt(referencedStatus),
                           Integer.parseInt(closedStatus));
    }

    public static class DescriptorImpl extends BuildStepDescriptor<Publisher>
    {
        private String redmineUrl;
        private String apiKey;
        private String referencedStatus;
        private String closedStatus;

        public DescriptorImpl() {
            load();
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json)
            throws FormException {
            redmineUrl = json.getString("redmineUrl");
            apiKey = json.getString("apiKey");
            referencedStatus = json.getString("referencedStatus");
            closedStatus = json.getString("closedStatus");
            save();
            return super.configure(req, json);
        }

        @Override
        public Publisher newInstance(StaplerRequest req, JSONObject formData)
            throws FormException {
            return new IssueNotifier(redmineUrl, apiKey, referencedStatus, closedStatus);
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Publish build result to Redmine issues";
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getClosedStatus() {
            return closedStatus;
        }

        public void setClosedStatus(String closedStatus) {
            this.closedStatus = closedStatus;
        }

        public String getRedmineUrl() {
            return redmineUrl;
        }

        public void setRedmineUrl(String redmineUrl) {
            this.redmineUrl = redmineUrl;
        }

        public String getReferencedStatus() {
            return referencedStatus;
        }

        public void setReferencedStatus(String referencedStatus) {
            this.referencedStatus = referencedStatus;
        }
    }
}
