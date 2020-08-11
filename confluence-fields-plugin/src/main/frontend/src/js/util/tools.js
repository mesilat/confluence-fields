import $ from 'jquery';
import Formatter from 'formatter';
import { deepGet } from './deep';

export function isUndefined(obj) {
  return typeof obj === 'undefined';
}
export function isArray(arr) {
  return Array.isArray(arr);
}
export function isFunction(func) {
  return typeof func === 'function';
}
export function isString(str) {
  return str && (typeof str.valueOf() === 'string');
}
export function extend(target, ...sources) {
  const length = sources.length;
  if (length < 1 || target == null) return target;
  for (let i = 0; i < length; i++) {
    const source = sources[i];
    for (const key in source) {
      target[key] = source[key];
    }
  }
  return target;
}
export function format() {
  return Formatter.format.apply(null, Array.from(arguments));
}
export function formatOptionValue(id, title) {
  return JSON.stringify({ id: `${id}`, title }).replace(/\,/g, '~[$]~');
}
export function convertData(results) {
  const list = [];
  if (!isUndefined(results)) {
    results.forEach(rec => list.push({ id: formatOptionValue(rec.id, rec.title), text: rec.title }));
    list.sort((a, b) => a.text.toLowerCase().localeCompare(b.text.toLowerCase()));
  }
  return list;
}
export function issueToFilterFields(issue, fields){
  const filterFields = {};
  if (isArray(fields)) {
    fields.forEach((filterField) => {
      switch (filterField){
        case '':
          break;
        case 'id':
          filterFields.id = issue.id;
          break;
        case 'key':
          filterFields.key = issue.key;
          break;
        default:
          filterFields[filterField] = deepGet(issue.fields, filterField);
      }
    });
  }
  return filterFields;
}
