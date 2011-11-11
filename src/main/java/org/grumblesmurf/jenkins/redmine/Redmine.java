package org.grumblesmurf.jenkins.redmine;

import org.redmine.ta.AuthenticationException;
import org.redmine.ta.NotFoundException;
import org.redmine.ta.RedmineException;
import org.redmine.ta.RedmineManager;
import org.redmine.ta.beans.Issue;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;

import static com.google.common.collect.Iterables.any;

public class Redmine
{
    private RedmineManager mgr;
    private final String baseUrl;
    private final String apiKey;
    private final Integer closedStatus;
    private final Integer referencedStatus;

    public Redmine(String baseUrl, String apiKey, Integer referencedStatus, Integer closedStatus) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.closedStatus = closedStatus;
        this.referencedStatus = referencedStatus;
    }

    public void addBuildReferencesToIssue(Integer issueId, int buildNumber, URL buildUrl,
                                          Collection<BuildReference> references)
        throws IOException, RedmineException, AuthenticationException, NotFoundException {
        Issue issue = new Issue();
        issue.setId(issueId);

        if (any(references, BuildReference.IS_CLOSED)) {
            issue.setStatusId(closedStatus);
        } else {
            issue.setStatusId(referencedStatus);
        }

        StringBuilder notes = new StringBuilder();
        notes.append(String.format("From \"Jenkins build %s\":%s\n\n", buildNumber, buildUrl.toExternalForm()));
        notes.append("Relevant commits:\n");
        for (BuildReference reference : references) {
            URL changeSetLink = reference.changeSetLink();
            String commitId = reference.commitId();
            if (commitId == null && changeSetLink != null) {
                notes.append("* ").append(changeSetLink.toExternalForm());
            } else if (changeSetLink != null) {
                notes.append("* \"").append(commitId).append("\":").append(changeSetLink.toExternalForm());
            } else {
                notes.append("* ").append(commitId);
            }
            if (buildNumber != reference.buildNumber()) {
                notes.append(" (in \"build ").append(reference.buildNumber()).append("\":")
                    .append(reference.mentionedBuildUrl().toExternalForm())
                    .append(")");
            }
            notes.append("\n");
        }
        issue.setNotes(notes.toString());
        getMgr().updateIssue(issue);
    }

    public String titleOf(Integer issueId)
        throws IOException, AuthenticationException, RedmineException, NotFoundException {
        Issue issue = getMgr().getIssueById(issueId);
        return issue.getSubject();
    }

    private RedmineManager getMgr() {
        if (mgr == null) {
            mgr = new RedmineManager(baseUrl, apiKey);
        }
        return mgr;
    }

    public String getBaseUrl() {
        return baseUrl;
    }
}
