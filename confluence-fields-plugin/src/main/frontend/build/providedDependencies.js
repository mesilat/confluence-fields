const providedDependencies = new Map();

providedDependencies.set('jquery', {
  dependency: 'jira.webresources:jquery',
  import: {
    var: `require('jquery')`,
    amd: 'jquery',
  },
});

providedDependencies.set('formatter', {
  dependency: 'jira.webresources:jira-formatter',
  import: {
    var: `require('jira/util/formatter')`,
    amd: 'jira/util/formatter',
  },
});

providedDependencies.set('jiraUtilEvents', {
  dependency: 'jira.webresources:jira-events',
  import: {
    var: `require('jira/util/events')`,
    amd: 'jira/util/events',
  },
});

providedDependencies.set('jiraEventTypes', {
  dependency: 'com.atlassian.jira.jira-issue-nav-components:issueeditor',
  import: {
    var: `require('jira/components/issueeditor/eventtypes')`,
    amd: 'jira/components/issueeditor/eventtypes',
  },
});

module.exports = providedDependencies;
