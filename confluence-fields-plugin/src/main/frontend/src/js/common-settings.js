import $ from 'jquery';
import observer from './util/observer';
import { getField } from './util/api';
import { trace } from './util/log';
import { editSettings } from './settings';

function initFieldConfig(field){
  const $ul = $('.aui-page-panel-content table.jiraform.jirapanel ul.square');
  if ($ul.data('com.mesilat.confluence-fields')) {
    return; // Already initialized
  }
  $ul.data('com.mesilat.confluence-fields', true);
  trace('common-settings::initFieldConfig()', field);

  const $a = $('<a id="com-mesilat-confluence-field-configure" href="javascript:;">')
    .attr('title', AJS.I18n.getText('com.mesilat.confluence-field.configure.title'))
    .text(AJS.I18n.getText('com.mesilat.confluence-field.name'));
  $('<li>').append($a).appendTo($ul);
  $a.on('click', () => editSettings(field));
}

const re = /(.+)\/secure\/admin\/ConfigureCustomField\!default\.jspa\?customFieldId=(\d+)/;
async function init(){
  const match = re.exec(window.location);
  if (match){
    try {
      const field = await getField(match[2]);
      if (field.type.key === 'com.mesilat.confluence-fields:confluence-field'){
        initFieldConfig(field);
      }
    } catch (err) {
      error('common-settings::init()', err);
    }
  }
}

let timeout;
function common () {
  if (timeout) {
    clearTimeout(timeout);
  }
  timeout = setTimeout(() => init(), 100);
}

export default () => observer.addListener(() => common());
