/**
 * Support for issue change history: turn confluence-fields in
 * issue change history into links
 */

import observer from './js/util/observer';
import { trace } from './js/util/log';
import { updateChangeHistory } from './js/changehistory';

observer.addListener(async () => await updateChangeHistory());

trace('::changehistory');
