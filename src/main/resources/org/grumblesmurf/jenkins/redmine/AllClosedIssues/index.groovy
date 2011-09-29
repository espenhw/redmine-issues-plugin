package org.grumblesmurf.jenkins.redmine.AllClosedIssues

import hudson.Functions
import hudson.model.AbstractBuild
import org.grumblesmurf.jenkins.redmine.RedmineIssueLinksAction

f = namespace(lib.FormTagLib)
l = namespace(lib.LayoutTagLib)
t = namespace("/lib/hudson")
st = namespace("jelly:stapler")

l.layout(title: _("All closed issues", my.project.name)) {
  st.include(page: "sidepanel.jelly", it: my.project)
  l.main_panel() {
    def from = buildNumber(request.getParameter('from'));
    def to = buildNumber(request.getParameter('to'));

    h1(_("All closed issues"))
    def builds = Functions.filter(my.project.buildsAsMap, from, to).values()
    if (builds.empty) {
      text(_("No builds."))
    } else {
      showIssues(builds)
    }
  }
}

private buildNumber(String build) {
  if (build?.isInteger()) {
    return build
  } else {
    def permaLink = my.project.getPermalinks().get(build)
    def run = permaLink?.resolve(my.project)
    return run?.number?.toString()
  }
}

private showIssues(Collection<AbstractBuild> builds) {
  def hasIssues = false
  for (AbstractBuild build in builds) {
    def issues = issueReferencesFrom(build)
    if (!issues.isEmpty()) {
      hasIssues = true
      h2(build.project.displayName + " " + build.displayName)
      show(issues)
    }
    for (AbstractBuild contributingBuild in my.contributingBuilds(build)) {
      issues = issueReferencesFrom(contributingBuild)
      if (!issues.isEmpty()) {
        hasIssues = true
        h3(contributingBuild.project.displayName + " " + contributingBuild.displayName)
        show(issues)
      }
    }
  }
  if (!hasIssues) {
    text("No issues closed in any build.")
  }
}


private def showIssuesFrom(AbstractBuild build) {
  def issues = issueReferencesFrom(build)
  if (issues.isEmpty()) {
    text("No issues closed.")
  } else {
    show(issues)
  }
}

private def show(issues) {
  ul() {
    for (issue in issues) {
      li() {
        a(href: issue.href, issue.text)
      }
    }
  }
}

def linkTo(issue, String url) {
  (url + "/issues/" + issue).replaceAll("([^:])//", '$1/')
}

def issueReferencesFrom(AbstractBuild build) {
  RedmineIssueLinksAction action = build.getAction(RedmineIssueLinksAction.class)
  def closes = []
  if (action) {
    for (issue in action.closes()) {
      closes << [ href: linkTo(issue.id, action.redmineUrl), text: "#$issue.id: " + issue.subject ]
    }
  }
  return new TreeSet(closes)
}
