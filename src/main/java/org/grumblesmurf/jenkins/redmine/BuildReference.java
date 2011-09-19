package org.grumblesmurf.jenkins.redmine;

import com.google.common.base.Predicate;
import hudson.model.AbstractBuild;
import hudson.scm.ChangeLogSet;
import hudson.scm.RepositoryBrowser;
import hudson.scm.SubversionChangeLogSet;
import jenkins.model.Jenkins;
import static org.grumblesmurf.jenkins.redmine.BuildReference.Type.CLOSED;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class BuildReference
{
    final Type type;
    static final Predicate<BuildReference> IS_CLOSED = new Predicate<BuildReference>()
    {
        public boolean apply(BuildReference input) {
            return input.type == CLOSED;
        }
    };
    final AbstractBuild<?, ?> mentionedBuild;
    private final ChangeLogSet.Entry change;

    public BuildReference(Type type, AbstractBuild<?, ?> mentionedBuild, ChangeLogSet.Entry change) {
        this.type = type;
        this.mentionedBuild = mentionedBuild;
        this.change = change;
    }

    public String commitId() {
        String commitId = change.getCommitId();
        if (commitId == null && change instanceof SubversionChangeLogSet.LogEntry) {
            commitId = String.valueOf(((SubversionChangeLogSet.LogEntry) change).getRevision());
        }
        return commitId;
    }

    public URL changeSetLink() {
        @SuppressWarnings("unchecked")
        RepositoryBrowser<ChangeLogSet.Entry> browser =
              (RepositoryBrowser<ChangeLogSet.Entry>) mentionedBuild.getProject().getScm().getEffectiveBrowser();
        if (browser == null) {
            return null;
        }
        try {
            return browser.getChangeSetLink(change);
        } catch (IOException e) {
            return null;
        }
    }

    public int buildNumber() {
        return mentionedBuild.number;
    }

    public URL mentionedBuildUrl() throws MalformedURLException {
        return new URL(new URL(Jenkins.getInstance().getRootUrl()), mentionedBuild.getUrl());
    }

    public enum Type
    {
        CLOSED, REFERENCED
    }
}
