import $ from 'jquery';
import { post } from './util/api';
import { trace } from './util/log';
import { isArray } from './util/tools';

/*
 * Fix issue change history by adding links to Confluence pages (if possible)
 */
const FIXED = 'com-mesilat-confluence-fields-fixed';

const getMappings = async history => post('/rest/confield/1.0/changehistory', history);

function tryFixUglyJSON(text){
  try {
    const data = JSON.parse(`[${text.trim().replace(/~\[\$\]~/g, ',')}]`);
    const titles = [];
    data.forEach(obj => titles.push(obj.title));
    return titles.join(', ');
  } catch(ignore){
    return text;
  }
}

export async function updateChangeHistory(){
  trace('changehistory::updateChangeHistory');

  const history = [];
  $('#activitymodule .issue-data-block').each(function(){
    const $issueDataBlock = $(this);
    if ($issueDataBlock.data(FIXED)){
      return;
    }
    $issueDataBlock.data(FIXED, true);
    if (!$issueDataBlock.attr('id'))
      return;

    const changeId = parseInt($issueDataBlock.attr('id').substring(14), 10);
    history.push(changeId);
    $issueDataBlock.find(`#changehistory_${changeId} td`).each(function(){
      const $td = $(this);
      $td.text(tryFixUglyJSON($td.text()));
    });
  });
  trace('changehistory::updateChangeHistory() history to fix', history);

  if (history.length > 0){
    const data = await getMappings(history);
    trace('updateChangeHistory::getMappings() return', data);

    Object.keys(data).forEach((changeId) => {
      $(`#activitymodule .issue-data-block #changehistory_${changeId} td`).each(function(){
        const $td = $(this);
        let text = $td.text().trim();
        if (text in data[changeId]){
          const $a = $('<a target="_blank">').attr('href', data[changeId][text]).text(text);
          $td.empty().append($a);
        } else {
          text = text.split(',');
          text = isArray(text)? text: [ text ];
          const $div = $('<div>');
          let hasChildren = false;
          text.forEach(text => {
            if (hasChildren){
              $div.append(document.createTextNode(', '));
            } else {
              hasChildren = true;
            }
            text = text.trim();
            if (text in data[changeId]){
              $('<a target="_blank">').attr('href', data[changeId][text]).text(text).appendTo($div);
            } else {
              $div.append(document.createTextNode(text));
            }
          });
          $td.html($div.html());
        }
      });
    });
  }
}
