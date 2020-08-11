/**
 * Customer portal support
 */
// import 'wr-dependency!com.atlassian.auiplugin:ajs';
// import 'wr-dependency!com.atlassian.auiplugin:aui-flag';
// import 'wr-dependency!com.atlassian.auiplugin:aui-select2';
import portal from './js/portal';

portal();

if (module.hot) {
  module.hot.accept('./js/portal', () => {
    require('./js/portal').default();
  });
}
