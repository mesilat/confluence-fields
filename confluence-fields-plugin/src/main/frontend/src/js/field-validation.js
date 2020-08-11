/**
 * Validation takes place after inline issue modification. If modified issue field
 * is referenced by a confluence field and this confluence field is in autoFilter
 * mode, then we may have to remove the confluence field values which do not
 * match the value of the modified issue field.
 */
import $ from 'jquery';
import { ConfluenceFieldType } from './constants';
import { trace } from './util/log';
import { issueToFilterFields } from './util/tools';
import { getJiraIssueKey } from './util/jira';
import {
  getJiraIssue, getJiraProject, getFilterFields, validateField,
  postFieldValue, getProjectFieldsSettings
} from './util/api';
import { updateField } from './confluence-fields';

let issueKey;
let issue;
let fieldSettingsCache = {};

function convertToObject(fieldSettings) {
  const _fieldSettings = {};
  fieldSettings.forEach(field => {
    _fieldSettings[`customfield_${field.id}`] = field;
  });
  return _fieldSettings;
}

export async function validateCustomFields(){
  const _issueKey = getJiraIssueKey();
  if (!_issueKey){
    trace('field-validation::validateCustomFields()', 'failed to obtain issue key, can\'t validate fields');
    return;
  }
  trace('field-validation::validateCustomFields()');
  if (issueKey !== _issueKey){
    issue = await getJiraIssue(_issueKey);
    issueKey = _issueKey;
    issue.fields.project = await getJiraProject(issue.fields.project.key);
  }
  if (!(issue.fields.project.key in fieldSettingsCache)) {
    fieldSettingsCache[issue.fields.project.key] = convertToObject(
      await getProjectFieldsSettings(issue.fields.project.key)
    );
  }
  validateIssue(issue, fieldSettingsCache[issue.fields.project.key]);
}
async function validateIssue(issue, fieldSettings){
  trace('field-validation::validateIssue()', issue, fieldSettings);

  let modifications = [];
  Object.keys(issue.fields).forEach((key) => {
    if (
      key.indexOf('customfield_') === 0
      && (key in fieldSettings)
      && (fieldSettings[key].autoFilter || fieldSettings[key].auto)
      && issue.fields[key] !== null
      // && key in issue.schema
      // && issue.schema[key].custom === ConfluenceFieldType
    ){
      modifications.push(validate(key, issue.fields[key], issue));
    }
  });

  async function hasAny(modifications) {
    if (!modifications)
      return false;
    let any = false;
    const result = await Promise.all(modifications);
    for (let i = 0; i < result.length; i++){
      any |= (!!result[i]);
    }
    return any;
  }

  if (await hasAny(modifications)){
    trace('field-validation::validateIssue(): some fields were modified, issue page reload is required');
    setTimeout(() => location.reload());
  }
}
async function validate(key, values, issue) {
  let filter;
  // take field filter from cache if possible
  if ((issue.fields.project.key in fieldSettingsCache) && (key in fieldSettingsCache[issue.fields.project.key])) {
    filter = fieldSettingsCache[issue.fields.project.key][key].filter;
  } else {
    filter = await getFilterFields(issue.fields.project.key, key.substr(12));
  }

  if (filter && filter.length === 0)
    return false; // no filter for the field - nothing to validate

  trace('field-validation::validate()', key, values, filter);

  // get filter values from the issue
  const filterFields = issueToFilterFields(issue, filter);

  // request field values that match filter values
  const data = await validateField(issue.fields.project.key, key.substr(12), values, filterFields);

  // check if no field value was filtered out
  if (data && data.length === values.length)
    return false;

  // if yes, modify field value to match the filtered set
  const _value = [];
  data.forEach(rec => _value.push(JSON.stringify(rec)));
  await postFieldValue(key, _value.join(','), issue.id);
  return true;
}
