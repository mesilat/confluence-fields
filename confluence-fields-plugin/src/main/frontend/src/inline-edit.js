/**
 * Inline edit support
 */
import 'wr-dependency!com.atlassian.auiplugin:ajs';
import 'wr-dependency!com.atlassian.auiplugin:aui-flag';
import { trace } from './js/util/log';
import inlineEdit from './js/inline-edit';

inlineEdit();
if (module.hot) {
  module.hot.accept('./js/inline-edit', () => {
    require('./js/inline-edit').default();
  });
}

trace('::inline-edit');
