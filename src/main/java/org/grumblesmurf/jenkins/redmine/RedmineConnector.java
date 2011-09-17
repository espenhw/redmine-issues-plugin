package org.grumblesmurf.jenkins.redmine;

import org.redmine.ta.RedmineManager;

public class RedmineConnector
{
    private final Redmine redmine;

    public RedmineConnector(String uri, String apiKey) {
        this(new RestRedmine(new RedmineManager(uri, apiKey)));
    }

    public RedmineConnector(Redmine redmine) {
        this.redmine = redmine;
    }
}
