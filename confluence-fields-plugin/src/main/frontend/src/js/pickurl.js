import $ from 'jquery';
import { trace } from './util/log';
import { getBaseUrl } from './util/jira';
import { resolvePage } from './util/api';
import { convertData, formatOptionValue } from './util/tools';

async function resolveUrl(fieldId, winPickUrl, url, $input) {
  let pageText;
  try {
    pageText = await resolvePage(fieldId, { url });
  } catch (xhr) {
    pageText = xhr.responseText;
  }

  const page = {};
  let m = pageText.match(/<meta name="ajs-page-id" content="(\d+)">/);
  if (m && m.length === 2) {
    page.id = m[1];
  }
  m = pageText.match(/<meta name="ajs-page-title" content="(.+?)">/);
  if (m && m.length === 2) {
    page.title = m[1];
  }

  trace("pickurl::resolveUrl", page);

  if (!!page.id && !!page.title) {
    console.debug(page);
    winPickUrl.close();

    if ($input.data('com-mesilat-confields-select-multi')) {
      const _values = $input.val().split(',');
      const data = JSON.parse(`[${$input.val().replace(/~\[\$\]~/g,',')}]`);
      const _data = [];
      data.forEach(page => {
        _data.push({ id: formatOptionValue(page.id, page.title), text: page.title });
      });

      const id = formatOptionValue(page.id, page.title);
      _values.push(id);
      _data.push({ id, text: page.title });

      console.debug(_data, _values);

      setTimeout(() => {
        $input
        .data('com-mesilat-confields-init-selection', _data)
        .val(_values)
        .trigger('change', [ true ]);
      });
    } else {
      const _values = [], _data = [];
      const id = formatOptionValue(page.id, page.title);
      _values.push(id);
      _data.push({ id, text: page.title });

      setTimeout(() => {
        $input
        .data('com-mesilat-confields-init-selection', _data[0])
        .val(_values[0])
        .trigger('change', [ true ]);
      });
    }

  } else {
    $('.error', winPickUrl.document).text('Failed to read Confluence page. Either this Confluence page does not exist or you did not authorize your JIRA to read this page');
  }
}

export function createUrlPicker($input) {
  const fieldId = $input.attr('id').substr(12);
  if ($input.parent().find('.popup-trigger').length > 0)
    return;

  const $a = $(`
    <a href="#" class="popup-trigger">
    <span class="icon-default aui-icon aui-icon-small aui-iconfont-add" title="Add with url"></span>
    </a>`
  ).on('click', e => {
    e.preventDefault();
    const winPickUrl = window.open(`${getBaseUrl()}/secure/PickConfluencePage.jspa`, 'Select Page', 'status=no,resizable=yes,top=100,left=100,width=530,height=300,scrollbars=no');
    winPickUrl.opener = self;
    winPickUrl.focus();
    winPickUrl.addEventListener('load', () => {
      $('.aui-button-primary', winPickUrl.document).on('click', e => {
        e.preventDefault();
        const url = $('#page-url', winPickUrl.document).val();
        if (url === '') {
          $('.error', winPickUrl.document).text('No Confluence page specified');
        }
        resolveUrl(fieldId, winPickUrl, url, $input);
      });
      $('.aui-button-link', winPickUrl.document).on('click', e => {
        e.preventDefault();
        winPickUrl.close();
      });
    }, false);
  });
  $a.insertAfter($input);
}
