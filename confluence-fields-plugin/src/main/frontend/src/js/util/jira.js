export function getJiraIssueId() {
  if (JIRA.Issue) {
    return JIRA.Issue.getIssueId();// || AJS.$('form#issue-edit input[name="id"]').val();
  } else {
    return null;
  }
}
export function getJiraIssueKey() {
  if (JIRA.Issue) {
    return JIRA.Issue.getIssueKey();
  } else {
    return AJS.Meta.get("issue-key");
  }
}
export function getBaseUrl() {
  if (AJS.params.baseURL) {
    return AJS.params.baseURL;
  }
  const p = window.location.href.indexOf('/servicedesk/customer/portal');
  if (p >= 0) {
    return window.location.href.substring(0, p);
  }
  return '';
}
