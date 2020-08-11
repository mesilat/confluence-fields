import { CFP_TRACE_ENABLED } from '../constants';

export function trace(...args){
  const traceEnabled = !!window.localStorage[CFP_TRACE_ENABLED];
  if (traceEnabled){
    console.debug('confluence-fields', ...args);
  }
}
export function error(...args){
  console.error('confluence-fields', ...args);
}
