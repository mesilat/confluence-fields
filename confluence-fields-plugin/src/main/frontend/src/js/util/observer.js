import { trace } from './log';
import { isFunction } from './tools';

const WORKER_IDLE = 0;
const WORKER_BUSY = 1;
const WORKER_BUSY_RESTART_REQUIRED = 2;

function Observer(){
  this.listeners = [];
  this.status = [];
}
Observer.prototype.start = function(){
  const MutationObserver = window.MutationObserver || window.WebKitMutationObserver;
  this.mutationObserver = new MutationObserver((mutations, observer) => {
    if (this.timeout){
      clearTimeout(this.timeout);
    }
    this.timeout = setTimeout(() => this.onMutated(), 250);
  });
  this.mutationObserver.observe(document, {childList: true, subtree: true});
};
Observer.prototype.onMutated = function(){
  const self = this;

  function startWorker(i){
    self.status[i] = WORKER_BUSY;
    setTimeout(() => {
      self.listeners[i]();
      switch (self.status[i]){
        case WORKER_BUSY:
          self.status[i] = WORKER_IDLE;
          break;
        case WORKER_BUSY_RESTART_REQUIRED:
          startWorker(i);
      }
    });
  }

  for (let i = 0; i < this.listeners.length; i++){
    switch (this.status[i]){
      case WORKER_IDLE:
        startWorker(i);
        break;
      case WORKER_BUSY:
        this.status[i] = WORKER_BUSY_RESTART_REQUIRED;
        break;
    }
  }
};
Observer.prototype.addListener = function(listener){
  if (isFunction(listener)){
    this.listeners.push(listener);
    this.status.push(WORKER_IDLE);
    trace('observer::addListener()');
  }
};

const observer = new Observer();
observer.start();
trace('observer:: started');

export default observer;
