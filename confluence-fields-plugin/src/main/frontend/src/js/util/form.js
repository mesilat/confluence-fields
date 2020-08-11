import $ from 'jquery';
import { deepGet } from './deep';
import { trace } from './log';
import { isUndefined } from './tools';

// Collect values from JIRA issue form
export function getFormValues($form){
  const values = {};
  $form.find('input,textarea,select').each(function(){
    const $input = $(this);
    if ($input.attr('com-mesilat-confields')){
      values[$input.attr('name')] = JSON.parse('[' + $input.val().replace(/~\[\$\]~/g,',') + ']');
    } else if ($input.hasClass('select') || $input.prop('tagName') === 'SELECT'){
      $input.find('option').each(function(){
        const $option = $(this);
        if ($option.val() === $input.val()){
          values[$input.attr('name')] = $option.val() === '-1'? null: {
            id: $option.val(),
            value: $option.text(),
            self: `${AJS.params.baseURL}/rest/api/2/customFieldOption/${$option.val()}`
          };
        }
      });
    } else {
      values[$input.attr('name')] = $input.val();
    }
  });
  return values;
}

// Update fields from the form
export function updateFields($form, fields){
  const formValues = getFormValues($form);
  trace('confluence-fields::updatefields()', formValues, fields);
  Object.keys(fields).forEach((key) => {
    const val = deepGet(formValues, key);
    if (!isUndefined(val)){
      fields[key] = val;
    }
  });
}

// Return true if JIRA page has a create issue dialog
export function isCreateIssueDialog($input) {
  const f = $input? $input.closest: $;
  return f('div#create-issue-dialog').length > 0 || f('form#issue-create').length > 0;
}

// Get JIRA project key ot id from create issue dialog
export function getProjectKeyOrId($input) {
  try {
    const f = $input? $input.closest: $;
    if (f('div#create-issue-dialog').length > 0){
      return f('div#create-issue-dialog').find('#project-field').val().replace(/^.+\(([^\(]+)\)$/, '$1');
    } else if (f('form#issue-create').length > 0){
      return f('form#issue-create').find('input[name="pid"]').val();
    }
  } catch (err) {
    trace("confluence-fields::getProjectKeyOrId()", err)
  }
}

// Get label text for the input field
export function getLabel($input){
  return $input.closest('.field-group').find('label').text();
}
