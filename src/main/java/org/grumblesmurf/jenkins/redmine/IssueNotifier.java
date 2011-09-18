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
import org.kohsuke.stapler.DataBoundConstructor;
import org.redmine.ta.AuthenticationException;
import org.redmine.ta.NotFoundException;
import org.redmine.ta.RedmineException;
import org.redmine.ta.RedmineManager;

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
    private final CommitMessageParser commitParser;
    private final Redmine redmine;

    @DataBoundConstructor
    public IssueNotifier(String redmineUrl, String apiKey, String referencedStatus, String closedStatus) {
        this.redmineUrl = redmineUrl;
        this.apiKey = apiKey;
        this.referencedStatus = referencedStatus;
        this.closedStatus = closedStatus;
        commitParser = new CommitMessageParser();
        redmine = new Redmine(new RedmineManager(redmineUrl, apiKey),
                              Integer.parseInt(referencedStatus),
                              Integer.parseInt(closedStatus));
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

        if (!buildReferences.isEmpty()) {
            build.getActions().add(new RedmineIssueLinksAction(redmineUrl, buildReferences));
        }
        
        try {
            for (Map.Entry<Integer, Collection<BuildReference>> entry : buildReferences.asMap().entrySet()){
                try {
                    redmine.addBuildReferencesToIssue(entry.getKey(), build.number,
                                                      new URL(new URL(Jenkins.getInstance().getRootUrl()), build.getUrl()),
                                                      entry.getValue());
                } catch (RedmineException e) {
                    console.println("ERROR: Redmine denied update of issue #" + entry.getKey());
                    console.println(e.getMessage());
                } catch (NotFoundException e) {
                    console.println("ERROR: Referenced issue #" + entry.getKey() + " not found");
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
