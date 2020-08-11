import $ from 'jquery';
import { trace } from './util/log';
import { initCustomFields } from './confluence-fields';

let bound = false;

export default (options) => $(() => {
  let timeout = null;
  function onNewContent(){
    if (timeout)
      clearTimeout(timeout);
    timeout = setTimeout(() => initCustomFields(options), 50);
  }

  if (!bound){
    bound = true;
    JIRA.bind(JIRA.Events.NEW_CONTENT_ADDED, (e) => onNewContent(e));
    JIRA.bind(JIRA.Events.ISSUE_REFRESHED, (e) => onNewContent(e));
    trace('view-edit::(): bound initCustomFields() to NEW_CONTENT_ADDED,ISSUE_REFRESHED');
  }

  onNewContent();
});
