package org.grumblesmurf.jenkins.redmine;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommitMessageParser
{
    private static final Pattern ISSUE = Pattern.compile("#([0-9]+)[,& ]*");
    private static final Pattern REFERENCES = Pattern.compile("(?:refs|references|IssueID)");
    private static final Pattern CLOSES = Pattern.compile("(?:fixes|closes)");

    public Set<Integer> referencedIssueIdsIn(String message) {
        return parseIssues(message, REFERENCES, CLOSES);
    }

    public Set<Integer> closedIssueIdsIn(String message) {
        return parseIssues(message, CLOSES, REFERENCES);
    }

    private Set<Integer> parseIssues(String message, Pattern include, Pattern exclude) {
        Matcher includeMatcher = include.matcher(message);
        Matcher excludeMatcher = exclude.matcher(message);

        Set<Integer> issues = new LinkedHashSet<Integer>();
        while (includeMatcher.find()) {
            int start = includeMatcher.end();
            int end = message.length();
            if (excludeMatcher.find(start)) {
                end = excludeMatcher.start();
            }
            Matcher issueMatcher = ISSUE.matcher(message.substring(start, end));
            while (issueMatcher.find()) {
                issues.add(Integer.parseInt(issueMatcher.group(1)));
            }
        }
        return issues;
    }
}
