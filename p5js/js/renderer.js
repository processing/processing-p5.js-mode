const sendToMainHandler = {
  get(target, prop) {
    const consoleMethod = target[prop];
    // Only intercept methods, return properties
    if (typeof consoleMethod !== "function") {
      return Reflect.get(...arguments);
    }
    return new Proxy(consoleMethod, {
      apply(target, thisArg, args) {
        // Notify main process via own API through contextBridge
        window.electron.sendMessage({
          level: prop,
          msgArgs: args
        });
        // Retain original behavior
        return Reflect.apply(...arguments);
      }
    });
  }
};
window.console = new Proxy(console, sendToMainHandler);
