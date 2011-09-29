package org.grumblesmurf.jenkins.redmine;

import static com.google.common.collect.Iterables.any;
import com.google.common.collect.Multimap;
import hudson.model.Action;
import static org.grumblesmurf.jenkins.redmine.BuildReference.Type.CLOSED;
import static org.grumblesmurf.jenkins.redmine.BuildReference.Type.REFERENCED;

import java.util.*;

public class RedmineIssueLinksAction implements Action
{
    public final String redmineUrl;
    public final Map<BuildReference.Type, Set<Integer>> issues;

    public RedmineIssueLinksAction(String redmineUrl, Multimap<Integer, BuildReference> buildReferences) {
        this.redmineUrl = redmineUrl;
        this.issues = new HashMap<BuildReference.Type, Set<Integer>>();
        issues.put(REFERENCED, new TreeSet<Integer>());
        issues.put(CLOSED, new TreeSet<Integer>());
        for (Map.Entry<Integer, Collection<BuildReference>> e : buildReferences.asMap().entrySet()) {
            Integer issueId = e.getKey();
            if (any(e.getValue(), BuildReference.IS_CLOSED)) {
                issues.get(CLOSED).add(issueId);
            } else {
                issues.get(REFERENCED).add(issueId);
            }
        }
    }

    public boolean hasReferences() {
        return !references().isEmpty();
    }

    public Set<Integer> references() {
        return issues.get(REFERENCED);
    }

    public boolean hasCloses() {
        return !closes().isEmpty();
    }

    public Set<Integer> closes() {
        return issues.get(CLOSED);
    }

    public String getIconFileName() {
        // Return null to hide from action list
        return null;
    }

    public String getDisplayName() {
        return "Issues referenced";
    }

    public String getUrlName() {
        return "redmine-issues";
    }
}
