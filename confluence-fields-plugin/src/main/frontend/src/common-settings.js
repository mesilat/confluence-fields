/**
 * Global Confluence Fields settings
 */
import 'wr-dependency!com.atlassian.auiplugin:ajs';
import 'wr-dependency!com.atlassian.auiplugin:aui-flag';
import 'wr-dependency!com.atlassian.auiplugin:aui-select2';
import 'wr-dependency!com.atlassian.auiplugin:dialog2';
import { trace } from './js/util/log';
import commonSettings from './js/common-settings';

commonSettings();

if (module.hot) {
  module.hot.accept('./js/common-settings', () => {
    require('./js/common-settings').default();
  });
}

trace('::common-settings');
