import Events from 'jiraUtilEvents';
import EventTypes from 'jiraEventTypes';
import { trace } from './util/log';
import { validateCustomFields } from './field-validation';

let bound = false;
export default () => {
  if (!bound){
    Events.bind(EventTypes.INLINE_EDIT_SAVE_COMPLETE, () => validateCustomFields());
    bound = true;
    trace('inline-edit::()', 'bound validateCustomFields() to INLINE_EDIT_SAVE_COMPLETE');
  }
};
