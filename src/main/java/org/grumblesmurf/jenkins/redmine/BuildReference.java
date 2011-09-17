package org.grumblesmurf.jenkins.redmine;

import hudson.model.AbstractBuild;
import hudson.scm.ChangeLogSet;
import hudson.scm.RepositoryBrowser;
import jenkins.model.Jenkins;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class BuildReference
{
    final Type type;
    final AbstractBuild<?, ?> mentionedBuild;
    private final ChangeLogSet.Entry change;

    public BuildReference(Type type, AbstractBuild<?, ?> mentionedBuild, ChangeLogSet.Entry change) {
        this.type = type;
        this.mentionedBuild = mentionedBuild;
        this.change = change;
    }

    public String commitId() {
        return change.getCommitId();
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
