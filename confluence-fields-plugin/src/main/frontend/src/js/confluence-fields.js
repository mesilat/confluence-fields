import $ from 'jquery';
import { trace, error } from './util/log';
import {
  convertData, issueToFilterFields, formatOptionValue, isUndefined, extend, isString
} from './util/tools';
import {
  getJiraIssue, getJiraProject, validateField, listPages, handleError
} from './util/api';
import {
  getFormValues, isCreateIssueDialog, getProjectKeyOrId, getLabel, updateFields
} from './util/form';
import { deepGet } from './util/deep';
import { getJiraIssueId, getJiraIssueKey, getBaseUrl } from './util/jira';
import { createUrlPicker } from './pickurl';

export function updateField($input, values, filterFields, options){
  trace('confluence-fields::updateField()', values, filterFields, options);
  options = options||{};
  const initial = convertData(values.results);
  const isMultiSelect = !!($input.data('com-mesilat-confields-select-multi')) || (!!options.multi);

  if (!isUndefined(initial) && initial.length > 0){
    const _values = [];
    initial.forEach((val) => _values.push(val.id));
    if (isMultiSelect){
      $input.val(_values.join(','));
    } else if (_values.length > 0) {
      $input.val(_values[0]);
    }
  }

  $input.data('com-mesilat-confields-init-selection', isMultiSelect? initial: initial[0]);

  $input.auiSelect2({
    multiple: isMultiSelect,
    ajax: {
      url: `${getBaseUrl()}/rest/confield/1.0/field/${options.project.key}/${options.id||$input.attr('id').substr(12)}/pages`,
      dataType: 'json',
      data: (text) => {
        // Issue #2: Improved CQL Filter definition
        //var $form = $input.closest('form[name="jiraform"]');
        const $form = $input.closest('form');
        if ($form.length > 0){
          updateFields($form, filterFields);
        }
        return {
          q: text,
          'max-results': 10,
          'filter-fields': JSON.stringify(filterFields)
        };
      },
      results: (data) => {
        try {
          const result = {
            start: data.start,
            limit: data.limit,
            size: data.size,
            results: convertData(data.results)
          };
          return result;
        } catch (err){
          error('confluence-fields::auiSelect2', err);
        }
      },
      delay: 250,
      transport: (params) => {
        return $.ajax(extend({}, params, {
          error: (jqxhr) => {
            if (jqxhr.status === 0 && jqxhr.statusText === 'abort') {
              // ignore
              return;
            }
            error('confluence-fields::auiSelect2', jqxhr);
            if (jqxhr.getResponseHeader('X-CONFFIELDS-ADVICE') === 'authorize jira'){
            //if (jqxhr.status === 401){
              // responseText: You do not have an authorized access token for the remote resource
              AJS.flag({
                type: 'error',
                body: AJS.I18n.getText('com.mesilat.confluence-field.err.authDance', jqxhr.responseText, `${AJS.params.baseURL}/plugins/servlet/confield/dance?field-id=${$input.attr('id').substr(12)}`),
                close: 'manual'
              });
            }
          }
        }));
      }
    },
    initSelection: ($input, callback) => callback($input.data('com-mesilat-confields-init-selection'))
  });
}
function updateFieldHeight($input) {
  var $group = $input.closest('div.field-group');
  var $container = $group.find('div.select2-container');
  var $choices = $container.find('ul.select2-choices');
  $container.height($choices.height() + 16);
}
function getFieldValues($input) {
  const values = $input.data('com-mesilat-confields-values');
  return isString(values) ? JSON.parse(values) : values;
}

async function init($input, val, issue){
  trace('confluence-fields::init()', val, issue);
  let filterFields = {};
  if ($input.data('com-mesilat-confields-filter-fields')) {
    filterFields = issueToFilterFields(issue, $input.data('com-mesilat-confields-filter-fields').split(','));
  }
  const isMultiSelect = !!($input.data('com-mesilat-confields-select-multi'));

  if ($input.data('com-mesilat-confields-auto-filter')){
    trace('confluence-fields::init()', `auto-filter enabled for field: ${$input.attr('id')} [${$input.closest('div.field-group').find('label').text()}]`);
    Object.keys(filterFields).forEach((filterField) => {
      const m = /([a-z,A-Z][a-z,A-Z,0-9,_]*)/.exec(filterField);
      if (m){
        const $form = $input.closest('form');
        $form.find(`input[name="${m[0]}"],select[name="${m[0]}"],textarea[name="${m[0]}"]`).each(function(){
          trace('confluence-fields::init()', `installing hook on field: ${m[0]}`);

          $(this).on('change', async (e) => {
            if ($input.val() === ''){
              trace('confluence-fields::()', `auto-filter: confield=${$input.attr('name')} is empty`);
              return;
            }

            trace('confluence-fields::()', `auto-filter: confield=${$input.attr('name')}; referenced field=${$(e.target).attr('name')}; new value=${$(e.target).val()}`);
            updateFields($form, filterFields);
            trace('confluence-fields::()', `auto-filter: confield=${$input.attr('name')}; filter=${JSON.stringify(filterFields)}`);
            $form.find('#edit-issue-submit, #issue-edit-submit, #create-issue-submit, #issue-create-submit').attr('disabled', true);

            try {
              const data = await validateField(
                issue.fields.project.key,
                $input.attr('id').substr(12),
                $input.val() === ''? []: JSON.parse(`[${$input.val().replace(/~\[\$\]~/g,',')}]`),
                filterFields
              );
              trace('confluence-fields::()', `auto-filter: confield=${$input.attr('name')}; valid pages=${JSON.stringify(data)}`);

              const _values = [], _data = [];
              data.forEach(page => {
                const id = formatOptionValue(page.id, page.title);
                _values.push(id);
                _data.push({ id, text: page.title });
              });

              if (_values.join(',') !== $input.val()){
                if (isMultiSelect){
                  trace('confluence-fields::()', `auto-filter confield=${$input.attr('name')}; init selection=${JSON.stringify(_data)}; values=${JSON.stringify(_values)}`);
                  setTimeout(() => {
                    $input
                    .data('com-mesilat-confields-init-selection', _data)
                    .val(_values)
                    .trigger('change', [ true ]);
                  });
                } else if (_values.length > 0) {
                  // It's a single select and the value is already selected
                  // $input.val(_values[0]).trigger('change', [ true ]);
                } else {
                  setTimeout(() => {
                    //$input.val('').trigger('change', [ true ]);
                    $input.auiSelect2('val', '');
                  });
                }
              }
            } catch(err) {
              error('confluence-fields::()', `auto-filter: confield=${$input.attr('name')} error`, err);
            } finally {
              $form.find('#edit-issue-submit, #issue-edit-submit, #create-issue-submit, #issue-create-submit').removeAttr('disabled');
            }
          });
        });
      }
    });
  }

  if (isUndefined(val) || val.length === 0){
    updateField($input, [], filterFields, { project: issue.fields.project });
  } else {
    const pageId = [];
    val.forEach((v) => { pageId.push(v.id); });
    try {
      const data = await listPages(issue.fields.project.key, $input.attr('id').substr(12), pageId);
      updateField($input, data, filterFields, { project: issue.fields.project });
    } catch(err){
      error('confluence-fields::init::listPages()', err);
      handleError(err, { name: getLabel($input), id: $input.attr('id').substr(12) });
    }
  }
}

const initNew = init;
function initCustomFieldsNewIssue(force, issue){
  $('input[com-mesilat-confields="true"]').each(async function(){
    const $input = $(this);
    if (!$input.data('com-mesilat-confields-initialized') || force){
      await initNew($input, [], issue);
      postInit($input);
      createUrlPicker($input);
    }
  });
}
function initCustomFieldsExistingIssue(force, issue){
  $('input[com-mesilat-confields="true"]').each(async function(){
    const $input = $(this);
    if (!$input.data('com-mesilat-confields-initialized') || force){
      await init($input, getFieldValues($input), issue);
      postInit($input);
    }
  });
}
function postInit($input){
  $input.data('com-mesilat-confields-initialized', true);
  $input.on('change', () => updateFieldHeight($input));
  setTimeout(() => updateFieldHeight($input), 200);
}

let _projectKey;
let _project;
let _issueKey;
let _issue;

export async function initCustomFields(options){
  options = options||{};

  if (isCreateIssueDialog()){
    const projectKey = getProjectKeyOrId();
    if (isUndefined(projectKey)){
      trace('confluence-fields::initCustomFields()', 'Failed to get project key for the issue');
      const project = {};
      initCustomFieldsNewIssue(options.force, { fields: { project }});
    } else {
      let project;
      if (options.force || projectKey !== _projectKey){
        project = await getJiraProject(projectKey);
        _projectKey = projectKey;
        _project = project;
      } else {
        project = _project;
      }
      initCustomFieldsNewIssue(options.force, { fields: { project }});
    }
  } else {
    const issueKey = getJiraIssueKey() || getJiraIssueId();
    trace(`confluence-fields::initCustomFields() issueKey=${issueKey}`);
    if (!!issueKey && issueKey !== 'undefined' && issueKey !== 'null'){
      if (options.force || issueKey !== _issueKey){
        const issue = await getJiraIssue(issueKey);
        const project = await getJiraProject(issue.fields.project.key);
        issue.fields.project = project;
        _issueKey = issueKey;
        _issue = issue;
        initCustomFieldsExistingIssue(options.force, issue);
      } else {
        initCustomFieldsExistingIssue(options.force, _issue);
      }
    }
  }
}
