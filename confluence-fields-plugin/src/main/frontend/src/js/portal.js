import $ from 'jquery';
import observer from './util/observer';
import { error, trace } from './util/log';
import { issueToFilterFields, extend } from './util/tools';
import { updateField } from './confluence-fields';
import { ConfluenceFieldInitialized } from './constants';
import { getProjectFieldsSettings } from './util/api';

// Get customer portal info (including project)
async function waitForData($elt, ms) {
  return new Promise((resolve, reject) => {
    function checkForData(ms) {
      if ($elt.text() === '' && ms > 0) {
        setTimeout(() => checkForData(ms - 10), 10);
        return;
      }

      try {
        resolve(JSON.parse($elt.text()));
        return;
      } catch (err) {
        if (ms > 0) {
          setTimeout(() => checkForData(ms - 10), 10);
          return;
        }
      }
      reject('Timeout waiting for JSON payload');
    }
    checkForData(ms)
  });
}

async function getIssue() {
/*
  if ($('body').data('projectId')) {
    return $('body').data('projectId');
  }
*/
  if ($('#jsonPayload').length > 0) {
    let jsonPayload;
    try {
      //const jsonPayload = JSON.parse($('#jsonPayload').text());
      jsonPayload = await waitForData($('#jsonPayload'), 2000);
    } catch(err) {
      trace('portal::getIssue()', err);
      return null;
    }

    if (!jsonPayload || !jsonPayload.portal) {
      trace('portal::getIssue(): no jsonPayload or missing portal attribute in jsonPayload');
      return null;
    }
    trace('portal::getIssue(): jsonPayload', jsonPayload);

    const issue = {};
    if (jsonPayload.reqDetails) {
      extend(issue, jsonPayload.reqDetails.issue);
    } else if (jsonPayload.reqCreate) {
      issue.fields = jsonPayload.reqCreate.fields;
    }
    issue.fields = issue.fields||{};
    issue.fields.project = {
      id: jsonPayload.portal.projectId,
      key: jsonPayload.portal.key.toUpperCase()
    };
    return issue;
  } else {
    trace('portal::getIssue(): failed to get JIRA issue details');
    return null;
  }
}

const settingsCache = {};

async function initPortalFields() {
  trace('portal::initPortalFields()');

  try {
    const issue = await getIssue();
    if (!issue)
      return;
    trace('portal::initPortalFields() WP 1')

    let settings = {};
    if (issue.fields.project.key in settingsCache) {
      trace('portal::initPortalFields() WP 2')
      settings = settingsCache[issue.fields.project.key];
      trace('portal::initPortalFields() WP 3')
    } else {
      trace('portal::initPortalFields() WP 4')
      const _settings = await getProjectFieldsSettings(issue.fields.project.key);
      trace('portal::initPortalFields() WP 5')
      _settings.forEach(setting => { settings[setting.id] = setting });
      trace('portal::initPortalFields() WP 6')
      settingsCache[issue.fields.project.key] = settings;
      trace('portal::initPortalFields()::loadSettings()', settings);
    }
    trace('portal::initPortalFields() WP 7')

    Object.keys(settings).forEach((fieldId) => {
      trace('portal::initPortalFields() WP 8')
      const $input = $(`input#customfield_${fieldId}`);
      if ($input.length === 0)
        return;
      trace('portal::initPortalFields() WP 9')

      if (!$input.data(ConfluenceFieldInitialized)) {
        $input.data(ConfluenceFieldInitialized, true);
        trace('portal::initPortalFields() WP 10')
        updateField(
          $input,
          { results:[] },
          issueToFilterFields(issue, settings[fieldId].filter || []),
          extend({}, settings[fieldId], { project: issue.fields.project })
        );
        trace('portal::initPortalFields() WP 11')
      }
    });
    trace('portal::initPortalFields() WP 12')
  } catch (err) {
    error('portal::initPortalFields()', err);
  }
}

function addAjaxHook() {
  trace('portal::addAjaxHook()');

  $(document).ajaxSend((e,xhr,options) => {
    if (options.type === "POST" && options.url.match(/\/servicedesk\/customer\/portal\/\d+\/create\/\d+$/)){
      if (options.data) {
        options.data = options.data.replace(/~%5B%24%5D~/g,'%2C');
      }
    }
  });
}
function addObserver() {
  trace('portal::addObserver()');

  observer.addListener(() => onNewContent());
}
let timeout;
function onNewContent () {
  if (timeout)
    clearTimeout(timeout);
  timeout = setTimeout(() => initPortalFields(), 100);
}

function initPortal() {
  if (!$(document).data(ConfluenceFieldInitialized)) {
    $(document).data(ConfluenceFieldInitialized, true);
    trace('portal::initPortal()');

    addAjaxHook();
    addObserver();
  }
}

export default () => initPortal();
