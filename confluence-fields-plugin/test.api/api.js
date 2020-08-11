//const https = require('https');
const axios = require('axios');
const _ = require('lodash');

const JIRA_HOME = process.env.TEST_JIRA_HOME;
const JIRA_USER = process.env.TEST_JIRA_USER;
const JIRA_PASSWORD = process.env.TEST_JIRA_PASSWORD;

//const agent = new https.Agent({
//  rejectUnauthorized: false
//});

function toError(err) {
  if (err.response && err.response.data) {
    if (_.isObject(err.response.data)) {
      return new Error(err.response.data.message || err.response.data.reason);
    } else {
      return new Error(err.response.data);
    }
  } else if (err.message) {
    return new Error(err.message);
  } else {
    return err;
  }
}

async function get(url, params) {
  return new Promise((resolve, reject) => {
    axios.get(`${JIRA_HOME}${url}`,
      {
        auth: {
          username: JIRA_USER,
          password: JIRA_PASSWORD
        },
        //httpsAgent: agent,
        headers: {
          'x-atlassian-token': 'no-check'
        },
        params: params||{}
      }
    )
    .then(result => resolve(result.data))
    .catch(err => reject(toError(err)));
  });
}

/*
export const listConfluenceLinks = async () => get('/rest/applinks/1.0/listApplicationlinks')
.then(data => {
  const links = [];
  data.list
  .filter(rec => rec.application.typeId === 'confluence')
  .forEach(rec => {
    links.push({
      id: rec.application.id,
      name: rec.application.name,
      iconUrl: rec.application.iconUrl
    });
  });
  return links;
});
*/
export const listConfluenceLinks = async () => get('/rest/confield/1.0/field/confluence-links');
export const getField = async (fieldId) => get(`/rest/confield/1.0/field/${fieldId}`);
export const getPages = async (projectKey, fieldId, params) => get(`/rest/confield/1.0/field/${projectKey}/${fieldId}/pages`, params);
export const getPagesById = async (projectKey, fieldId, params) => get(`/rest/confield/1.0/field/${projectKey}/${fieldId}/pages-by-id`, params);
export const getFilterFields = async (projectKey, fieldId) => get(`/rest/confield/1.0/field/${projectKey}/${fieldId}/filter-fields`);
export const getValues = async (fieldId) => get(`/rest/confield/1.0/field/${fieldId}/values`);
export const resolve = async (fieldId, projectKey, url) => get(`/rest/confield/1.0/field/${fieldId}/resolve`, { projectKey, url });
