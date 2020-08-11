/**
 * Project-specific Confluence Fields settings
 */
import 'wr-dependency!com.atlassian.auiplugin:ajs';
import 'wr-dependency!com.atlassian.auiplugin:aui-flag';
import 'wr-dependency!com.atlassian.auiplugin:aui-select2';
import 'wr-dependency!com.atlassian.auiplugin:dialog2';
import { trace } from './js/util/log';
import projectSettings from './js/project-settings';

projectSettings();

if (module.hot) {
  module.hot.accept('./js/project-settings', () => {
    require('./js/project-settings').default();
  });
}

trace('::project-settings');
