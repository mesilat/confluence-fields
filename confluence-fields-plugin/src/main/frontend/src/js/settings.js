import $ from 'jquery';
import { format, convertData } from './util/tools';
import {
  getFieldSettings, listConfluenceLinks, putFieldSettings,
  getProjectFieldSettings, putProjectFieldSettings, handleError
} from './util/api';
import { trace } from './util/log';

function formatOption(option){
  const $span = $('<span class="ml-select2-option">').text(option.name);
  if (option.iconUrl){
    $span.text(' ' + $span.text());
    $('<img>').attr('src', option.iconUrl).prependTo($span);
  }
  return $span;
}

function showDialog(field, settings, confluenceLinks, callback){
  trace('settings::showDialog', field, settings, confluenceLinks);

  const caption = format(AJS.I18n.getText('com.mesilat.confluence-field.configure.dlg.caption'), field.name);
  const $dlg = $(Mesilat.Confield.Templates.configureDialog({
    caption: caption,
    confluenceLinks: confluenceLinks,
    rec: settings
  }));

  const dlg = AJS.dialog2($dlg);
  dlg.on('show', function(e){
    const $dlg = $(e.target),
    $confluenceId = $dlg.find('#ml-configure-confluenceLink'),
    $filter = $dlg.find('#ml-configure-filter'),
    $test = $dlg.find('#ml-configure-test'),
    $multiSelect = $dlg.find('#ml-configure-multiselect'),
    $autoFilter = $dlg.find('#ml-configure-autofilter'),
    $asDefiner = $dlg.find('#ml-configure-asdefiner'),
    $form = $dlg.find('form'),
    $btnOk = $dlg.find('button.aui-button-primary'),
    $btnCancel = $dlg.find('button.cancel');

    $confluenceId.auiSelect2({
      data: confluenceLinks,
      formatResult: formatOption,
      formatSelection: formatOption
    });
    if (settings.confluenceId){
      $confluenceId.val(settings.confluenceId).trigger('change');
    }
    if (settings.filter){
      $filter.val(settings.filter).trigger('change');
    }
    $multiSelect[0].checked = !!(settings.multiSelect);
    $autoFilter[0].checked = !!(settings.autoFilter);
    $asDefiner[0].checked = !!(settings.definer);

    $test.auiSelect2({
      multiple: true,
      ajax: {
        url: AJS.params.baseURL + '/rest/confield/1.0/field/test',
        dataType: 'json',
        data: function(text){
          return {
            'confluence-id': $confluenceId.val(),
            filter: $filter.val(),
            q: text,
            'max-results': 10
          };
        },
        results: function(data){
          $form.find('.aui-message').remove();
          return {
            start: data.start,
            limit: data.limit,
            size: data.size,
            results: convertData(data.results)
          };
        },
        transport: function (params) {
          const $request = $.ajax(params);
          $request.fail(function(err){
            const msg = err.responseText||'';
            try {
              const obj = JSON.parse(msg);
              if (obj && obj.message) msg = obj.message;
            } catch(ignore){
            }

            if (err.getResponseHeader('X-CONFFIELDS-ADVICE') === 'authorize jira'){
              AJS.messages.error($form, {
                body: AJS.I18n.getText('com.mesilat.confluence-field.err.authDance', msg, AJS.params.baseURL + '/plugins/servlet/applinks/oauth/login-dance/access?applicationLinkID=' + $confluenceId.val()),
                closeable: true,
                insert: 'append'
              });
            } else {
              AJS.messages.error($form, {
                body: msg,
                closeable: true,
                insert: 'append'
              });
            }
          });
          return $request;
        },
        delay: 250
      }
    });

    $btnCancel.on('click', function(e){
      e.preventDefault();
      dlg.hide();
    });
    $btnOk.on('click', async (e) => {
      e.preventDefault();
      if (await callback({
        confluenceId: $confluenceId.val(),
        filter: $filter.val(),
        multiSelect: $multiSelect.is(':checked'),
        autoFilter: $autoFilter.is(':checked'),
        definer: $asDefiner.is(':checked')
      })) {
        dlg.hide();
      }
    });
  });

  dlg.show();
}

export async function editSettings(field) {
  trace('settings::editSettings');
  try {
    const [ settings, confluenceLinks ] = await Promise.all([
      getFieldSettings(field.id),
      listConfluenceLinks()
    ]);

    if (_.keys(settings).length === 0){
      const primary = confluenceLinks.filter(confluenceLink => confluenceLink.isPrimary);
      if (primary.length > 0)
        settings.confluenceId = primary[0].id;
    }

    showDialog(field, settings, confluenceLinks, async (settings) => {
      try {
        await putFieldSettings(field.id, settings);
        return true;
      } catch (err) {
        error('settings::editSettings::showDialog', err);
        handleError(err);
        return false;
      }
    });
  } catch (err) {
    error('settings::editSettings', err);
    handleError(err);
  }
}

export async function editProjectSettings(projectKey, field) {
  trace('settings::editProjectSettings');
  try {
    const [ settings, confluenceLinks ] = await Promise.all([
      getProjectFieldSettings(projectKey, field.id),
      listConfluenceLinks()
    ]);

    if (_.keys(settings).length === 0){
      const primary = confluenceLinks.filter(confluenceLink => confluenceLink.isPrimary);
      if (primary.length > 0)
        settings.confluenceId = primary[0].id;
    }

    showDialog(field, settings, confluenceLinks, async (settings) => {
      try {
        await putProjectFieldSettings(projectKey, field.id, settings);
        return true;
      } catch (err) {
        error('settings::editProjectSettings::showDialog', err);
        handleError(err);
        return false;
      }
    });
  } catch (err) {
    error('settings::editProjectSettings', err);
    handleError(err);
  }
}
