package org.grumblesmurf.jenkins.redmine;

import hudson.Extension;
import hudson.MarkupText;
import hudson.model.AbstractBuild;
import hudson.scm.ChangeLogAnnotator;
import hudson.scm.ChangeLogSet;

import java.util.regex.Pattern;

@Extension
public class RedmineLinkAnnotator extends ChangeLogAnnotator
{
    @Override
    public void annotate(AbstractBuild<?, ?> build, ChangeLogSet.Entry change, MarkupText text) {
        RedmineIssueLinksAction action = build.getAction(RedmineIssueLinksAction.class);
        if (action == null) {
            return;
        }

        for (MarkupText.SubText subText : text.findTokens(Pattern.compile("#([0-9]+)"))) {
            subText.surroundWith(linkStart(action.redmineUrl), "</a>");
        }
    }

    private String linkStart(String baseUrl) {
        return String.format("<a href='%s/issues/$1'>", baseUrl);
    }
}
