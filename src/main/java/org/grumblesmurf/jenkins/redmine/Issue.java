package org.grumblesmurf.jenkins.redmine;

public class Issue implements Comparable<Issue>
{
    public final Integer id;
    public final String subject;

    public Issue(Integer id, String subject) {
        this.id = id;
        this.subject = subject;
    }

    public int compareTo(Issue o) {
        return id.compareTo(o.id);
    }
}
