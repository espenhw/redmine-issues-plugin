package org.grumblesmurf.jenkins.redmine;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import hudson.model.AbstractBuild;
import hudson.scm.ChangeLogSet;
import org.junit.Test;

import static org.mockito.Mockito.mock;

public class RedmineIssueLinksActionTest
{
    private static final int ISSUE_ID_1 = 123;
    private static final int ISSUE_ID_2 = 124;

    @Test
    public void remembersIssuesClosed()
        throws Exception {
        Multimap<Integer, BuildReference> buildReferences = HashMultimap.create();
        buildReferences.put(ISSUE_ID_1, new BuildReference(BuildReference.Type.CLOSED, mock(AbstractBuild.class),
                                                           mock(ChangeLogSet.Entry.class)));
        buildReferences.put(ISSUE_ID_2, new BuildReference(BuildReference.Type.CLOSED, mock(AbstractBuild.class),
                                                           mock(ChangeLogSet.Entry.class)));

        new RedmineIssueLinksAction(mock(Redmine.class), buildReferences);
    }
}
