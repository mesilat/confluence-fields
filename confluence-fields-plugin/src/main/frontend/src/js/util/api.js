import $ from 'jquery';
import { getBaseUrl, getJiraIssueKey } from './jira';
import { trace } from './log';
import { extend, isArray, isUndefined } from './tools';

const DEFAULT_TIMEOUT = 30000;
/*
function toError(xhr) {
  // http://localhost:8090/authenticate.action?destination=%2Fplugins%2Fservlet%2Fupm
  /*if (xhr.status === 401) {
    const destination = window.location.href.substr(window.location.href.indexOf(AJS.params.baseURL) + AJS.params.baseURL.length);
    setTimeout(() => {
      window.location = `${AJS.params.baseURL}/authenticate.action?destination=${encodeURIComponent(destination)}`;
    });
    return new Error(AJS.I18n.getText("com.mesilat.general.error.permission"));
  } else* /
  if (xhr.responseJSON && xhr.responseJSON.message) {
    return new Error(xhr.responseJSON.message);
  } else if (xhr.responseText) {
    return new Error(xhr.responseText.substr(0, 1000));
  } else {
    return new Error("API call failed");
  }
}
*/
const toError = xhr => xhr;

export async function get(url, data, options) {
  return new Promise((resolve, reject) => {
    $.ajax(extend({
        url: `${getBaseUrl()}${url}`,
        type: 'GET',
        data,
        dataType: 'json',
        timeout: DEFAULT_TIMEOUT
      }, options)
    ).then(data => resolve(data), xhr => reject(toError(xhr)));
  });
}
export async function post(url, data, options) {
  return new Promise((resolve, reject) => {
    $.ajax(extend({
        url: `${getBaseUrl()}${url}`,
        type: 'POST',
        data: JSON.stringify(data),
        dataType: 'json',
        contentType: 'application/json',
        processData: false,
        timeout: DEFAULT_TIMEOUT
      }, options)
    ).then(data => resolve(data), xhr => reject(toError(xhr)));
  });
}
export async function put(url, data, options) {
  return new Promise((resolve, reject) => {
    $.ajax(extend({
        url: `${getBaseUrl()}${url}`,
        type: 'PUT',
        data: JSON.stringify(data),
        dataType: 'json',
        contentType: 'application/json',
        processData: false,
        timeout: DEFAULT_TIMEOUT
      }, options)
    ).then(data => resolve(data), xhr => reject(toError(xhr)));
  });
}
export async function del(url, data, options) {
  return new Promise((resolve, reject) => {
    $.ajax(extend({
        url: `${getBaseUrl()}${url}`,
        type: 'DELETE',
        data,
        timeout: DEFAULT_TIMEOUT
      }, options)
    ).then(() => resolve(), xhr => reject(toError(xhr)));
  });
}

export const validateField = async (projectKey, fieldId, value, filterFields) =>
  post(`/rest/confield/1.0/field/${projectKey}/${fieldId}/validate`, { pages: value, 'filter-fields': filterFields });

export async function listPages(projectKey, fieldId, pageId) {
  const _pageId = [];
  (isArray(pageId) ? pageId : isUndefined(pageId) ? [] : [pageId])
    .forEach(id => _pageId.push('page-id=' + id));
  return get(`/rest/confield/1.0/field/${projectKey}/${fieldId}/pages-by-id?${ _pageId.join('&')}`);
}

export async function getJiraIssue(key) {
  key = key || getJiraIssueKey();
  const issue = await get(`/rest/api/2/issue/${key}`, { expand: 'schema' });
  Object.keys(issue.fields).forEach(key => {
    if (
      key.indexOf('customfield_') === 0
      && issue.fields[key] !== null
      && key in issue.schema
      && issue.schema[key].custom === 'com.mesilat.confluence-fields:confluence-field'
    ) {
      try {
        issue.fields[key] = JSON.parse(`[${issue.fields[key]}]`);
      } catch(e){
        trace(`getJiraIssue(${key})`, `Failed to convert string to JSON array: ${issue.fields[key]}`);
      }
    }
  });
  return issue;
}

export const getField = async fieldId =>
  get(`/rest/confield/1.0/field/${fieldId}`);
export const listFields = async () =>
  get('/rest/confield/1.0/settings');
export const listConfluenceLinks = async () =>
  get('/rest/confield/1.0/field/confluence-links');
export const getJiraProject = async key =>
  get(`/rest/api/2/project/${key}`);
export const getFilterFields = async (projectKey, fieldId) =>
  get(`/rest/confield/1.0/field/${projectKey}/${fieldId}/filter-fields`);
export const getFieldSettings = async fieldId => fieldId?
  get(`/rest/confield/1.0/settings/${fieldId}`):
  get(`/rest/confield/1.0/settings`);
export const putFieldSettings = async (fieldId, settings) =>
  put(`/rest/confield/1.0/settings/${fieldId}`, settings);
export const getProjectFieldsSettings = async (projectKey) =>
  get(`/rest/confield/1.0/project-settings/${projectKey}`);
export const getProjectFieldSettings = async (projectKey, fieldId) =>
  get(`/rest/confield/1.0/project-settings/${projectKey}/${fieldId}`);
export const putProjectFieldSettings = async (projectKey, fieldId, settings) =>
  put(`/rest/confield/1.0/project-settings/${projectKey}/${fieldId}`, settings);


export async function postFieldValue(fieldName, value, issueId){
  const url = `${getBaseUrl()}/secure/AjaxIssueAction.jspa?decorator=none`;
  const data = {
    issueId: issueId,
    singleFieldEdit: true,
    fieldsToForcePresent: fieldName,
    atl_token: atl_token()
  };
  data[fieldName] = value;

  return AJS.$.ajax({
    url,
    type: 'POST',
    dataType: 'json',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8'
    },
    data
  });
}

export function handleError(err, field) {
  if (isUndefined(err)) {
    AJS.flag({
      type: 'error',
      title: AJS.I18n.getText('com.mesilat.confluence-field.err.unknown'),
      body: (this && this.url) ? format(AJS.I18n.getText('com.mesilat.confluence-field.err.resource'), this.url) : ''
    });
  } else {
    var msg = err.responseText || '';
    try {
      var obj = JSON.parse(msg);
      if (obj && obj.message)
        msg = obj.message;
    } catch (ignore) {
    }

    if (err.getResponseHeader('X-CONFFIELDS-ADVICE') === 'authorize jira'){
      AJS.flag({
        type: 'error',
        title: AJS.I18n.getText('com.mesilat.confluence-field.err.authentication'),
        body: isUndefined(field) ? msg :
        AJS.I18n.getText('com.mesilat.confluence-field.err.authDance', msg, AJS.params.baseURL + '/plugins/servlet/confield/dance?field-id=' + field.id)
      });
    } else if (err.status === 401 && msg === 'This resource requires WebSudo.') {
      AJS.flag({
        type: 'error',
        title: AJS.I18n.getText('com.mesilat.confluence-field.err.authentication'),
        body: msg + ` To authenticate click <a target="_blank" href="${AJS.params.baseURL}/secure/admin/authenticate.jsp">here</a>`
      });
    } else {
      AJS.flag({
        type: 'error',
        title: AJS.I18n.getText('com.mesilat.confluence-field.name') + (isUndefined(field) ? '' : `: ${field.name}`),
        body: msg
      });
    }
  }
}
export async function getAny(url, data, options) {
  return new Promise((resolve, reject) => {
    $.ajax(extend({
        url: `${getBaseUrl()}${url}`,
        type: 'GET',
        data,
        timeout: DEFAULT_TIMEOUT
      }, options)
    ).then(data => resolve(data), xhr => reject(toError(xhr)));
  });
}
export const resolvePage = async (fieldId, data) =>
  getAny(`/rest/confield/1.0/field/${fieldId}/resolve`, data);
