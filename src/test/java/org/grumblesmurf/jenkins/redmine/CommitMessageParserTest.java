package org.grumblesmurf.jenkins.redmine;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.*;

@RunWith(Parameterized.class)
public class CommitMessageParserTest
{
    private final String message;
    private final Set<Integer> referenced;
    private final Set<Integer> closed;

    @Parameterized.Parameters
    public static Collection<Object[]> parameters() {
        Set<Integer> none = Collections.<Integer>emptySet();
        return asList(p("No issue ids here", none, none),
                      p("This message refs #123", setOf(123), none),
                      p("This message references #123", setOf(123), none),
                      p("This message concerns IssueID #123", setOf(123), none),
                      p("This message fixes #123", none, setOf(123)),
                      p("This message closes #123", none, setOf(123)),
                      p("This message refs #123, #456 #789 & #42", setOf(123, 456, 789, 42), none),
                      p("This message refs #123 and fixes #456", setOf(123), setOf(456)),
                      p("This message refs #123, fixes #456, closes #789 and references #42", setOf(123, 42),
                        setOf(456, 789))
        );
    }

    private static Set<Integer> setOf(Integer... ints) {
        LinkedHashSet<Integer> set = new LinkedHashSet<Integer>();
        Collections.addAll(set, ints);
        return set;
    }

    private static Object[] p(String message, Set<Integer> referenced, Set<Integer> closed) {
        return new Object[] { message, referenced, closed };
    }

    public CommitMessageParserTest(String message, Set<Integer> referenced, Set<Integer> closed) {
        this.message = message;
        this.referenced = referenced;
        this.closed = closed;
    }

    private CommitMessageParser sut = new CommitMessageParser();

    @Test
    public void findsExpectedReferencedIssues() throws Exception {
        assertThat(String.format("Referenced in '%s'", message),
                   sut.referencedIssueIdsIn(message), is(referenced));
    }

    @Test
    public void findsExpectedClosedIssues() throws Exception {
        assertThat(String.format("Closed in '%s'", message),
                   sut.closedIssueIdsIn(message), is(closed));
    }
}
