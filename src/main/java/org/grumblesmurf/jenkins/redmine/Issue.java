package org.grumblesmurf.jenkins.redmine;

public class Issue
{
    public final Integer id;
    public final String subject;

    public Issue(Integer id, String subject) {
        this.id = id;
        this.subject = subject;
    }
}
