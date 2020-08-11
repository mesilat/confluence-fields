import $ from 'jquery';
import observer from './util/observer';
import { listFields } from './util/api';
import { trace } from './util/log';
import { editProjectSettings } from './settings';

// add configuration link to every applicable custom field
async function initProjectConfig($panel, projectKey){
  const fields = await listFields();
  trace('project-settings::initProjectConfig()', fields);

  $panel.find('.project-config-associated-screens a').each(function(){
    const $a = $(this);
    const url = $a.prop('href');
    const m = url.match(/fieldId=customfield_(\d+)&projectKey=(.+)/);
    if (m && (m[1] in fields)) {
      const fieldId = m[1];
      projectKey = projectKey||m[2];
      const $tr = $a.closest('.project-config-fieldconfig-row');
      let $span = $tr.find('.project-config-fieldconfig-description, .project-config-fieldconfig-name');
      if ($span.length === 0) {
        trace('project-settings::initProjectConfig(): WARNING: could not find .project-config-fieldconfig-description, or .project-config-fieldconfig-name');
        return;
      } else {
        $span = $($span[0]);
      }
      const fieldName = $tr.find('.project-config-fieldconfig-name').text();
      $('<a>')
        .attr('href', 'javascript:;')
        .text(AJS.I18n.getText('com.mesilat.confluence-field.configure.dlg.project-settings.link'))
        .appendTo(
          $('<p>').appendTo($span)
        )
        .on('click', () => {
          editProjectSettings(projectKey, {
            id: fieldId,
            name: fieldName
          });
        });
    }
  });
}

// aait for project settings panel to appear
const re = /(.+)\/plugins\/servlet\/project-config\/(.+)\/fields$/;
function init(){
  const match = re.exec(window.location);
  if (!match)
    return;

  const $panel = $('#project-config-panel-fields');
  if ($panel.length === 0)
    return;

  if ($panel.data('com.mesilat.confluence-fields'))
    return;

  $panel.data('com.mesilat.confluence-fields', true);
  initProjectConfig($panel, match[2]);
}

let timeout;
function project () {
  if (timeout)
    clearTimeout(timeout);
  timeout = setTimeout(() => init(), 100);
}
export default () => observer.addListener(() => project());
