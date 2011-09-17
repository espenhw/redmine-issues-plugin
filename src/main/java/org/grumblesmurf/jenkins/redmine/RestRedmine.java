package org.grumblesmurf.jenkins.redmine;

import org.redmine.ta.RedmineManager;

public class RestRedmine implements Redmine
{
    private final RedmineManager mgr;

    public RestRedmine(RedmineManager redmineManager) {
        mgr = redmineManager;
    }
}
