export function deepGet(obj, desc, value) {
  /*
  * Please refer to underscore.get:
  * https://github.com/NarHakobyan/underscore.get/blob/master/underscore-get.js
  */
  var arr = desc ? desc.split(".") : [];

  while (arr.length && obj) {
    var comp = arr.shift();
    var match = /(.+)\[([0-9]*)\]/.exec(comp);

    // handle arrays
    if ((match !== null) && (match.length === 3)) {
      var arrayData = {
        arrName: match[1],
        arrIndex: match[2]
      };
      if (obj[arrayData.arrName] !== undefined) {
        obj = obj[arrayData.arrName][arrayData.arrIndex];
      } else {
        obj = undefined;
      }
      continue;
    }
    obj = obj[comp];
  }

  if (typeof value !== 'undefined') {
    if (obj === undefined) {
      return value;
    }
  }
  return obj;
}
