/**
 * View / edit issue support
 */
import 'wr-dependency!com.atlassian.auiplugin:ajs';
import 'wr-dependency!com.atlassian.auiplugin:aui-flag';
import 'wr-dependency!com.atlassian.auiplugin:aui-select2';
import { trace } from './js/util/log';
import viewEdit from './js/view-edit';

viewEdit();

if (module.hot) {
    module.hot.accept('./js/view-edit', () => {
        require('./js/view-edit').default();
    });
}

trace('::view-edit');
