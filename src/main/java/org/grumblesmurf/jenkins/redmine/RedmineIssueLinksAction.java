package org.grumblesmurf.jenkins.redmine;

import com.google.common.collect.Multimap;
import hudson.model.Action;
import org.redmine.ta.AuthenticationException;
import org.redmine.ta.NotFoundException;
import org.redmine.ta.RedmineException;

import java.io.IOException;
import java.util.*;

import static com.google.common.collect.Iterables.any;
import static org.grumblesmurf.jenkins.redmine.BuildReference.Type.CLOSED;
import static org.grumblesmurf.jenkins.redmine.BuildReference.Type.REFERENCED;

public class RedmineIssueLinksAction implements Action
{
    public final String redmineUrl;
    public final HashMap<BuildReference.Type, Set<Issue>> issues;

    public RedmineIssueLinksAction(Redmine redmine, Multimap<Integer, BuildReference> buildReferences)
          throws IOException, AuthenticationException, RedmineException, NotFoundException {
        this.redmineUrl = redmine.getBaseUrl();
        this.issues = new HashMap<BuildReference.Type, Set<Issue>>();
        issues.put(REFERENCED, new TreeSet<Issue>());
        issues.put(CLOSED, new TreeSet<Issue>());
        for (Map.Entry<Integer, Collection<BuildReference>> e : buildReferences.asMap().entrySet()) {
            Integer issueId = e.getKey();
            if (any(e.getValue(), BuildReference.IS_CLOSED)) {
                issues.get(CLOSED).add(new Issue(issueId, redmine.titleOf(issueId)));
            } else {
                issues.get(REFERENCED).add(new Issue(issueId, redmine.titleOf(issueId)));
            }
        }
    }

    public boolean hasReferences() {
        return !references().isEmpty();
    }

    public Set<Issue> references() {
        return issues.get(REFERENCED);
    }

    public boolean hasCloses() {
        return !closes().isEmpty();
    }

    public Set<Issue> closes() {
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
