package org.grumblesmurf.jenkins.redmine;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class AllClosedIssues implements Action
{
    public final AbstractProject project;

    public AllClosedIssues(AbstractProject project) {
        this.project = project;
    }

    public String getIconFileName() {
        return "notepad.png";
    }

    public String getDisplayName() {
        return "All Closed Issues";
    }

    public String getUrlName() {
        return "all-closed-issues";
    }

    public Set<AbstractBuild> contributingBuilds(AbstractBuild build) {
        Set<AbstractBuild> builds = Sets.newHashSet();
        builds.add(build);
        int size = 0;
        do {
            size = builds.size();
            Set<AbstractBuild> newBuilds = Sets.newHashSet();
            for (AbstractBuild depBuild : builds) {
                newBuilds.addAll(changedDependencies(depBuild));
            }
            builds.addAll(newBuilds);
        } while (size < builds.size());
        builds.remove(build);
        return builds;
    }

    private Collection<AbstractBuild> changedDependencies(AbstractBuild build) {
        ImmutableList.Builder<AbstractBuild> builder = ImmutableList.<AbstractBuild>builder();
        @SuppressWarnings("unchecked")
        Map<AbstractProject, AbstractBuild.DependencyChange> depChanges =
              build.getDependencyChanges((AbstractBuild) build.getPreviousBuild());
        for (AbstractBuild.DependencyChange depChange : depChanges.values()) {
            builder.addAll(depChange.getBuilds());
        }
        return builder.build();
    }
}
