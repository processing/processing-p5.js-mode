const path = require('node:path');

// Proxy calls to `console` and send output to main process
const sendToMainHandler = {
    get(target, prop) {
        const consoleMethod = target[prop];
        // Only intercept methods, return properties
        if (typeof consoleMethod !== "function") {
            return Reflect.get(...arguments);
        }
        return new Proxy(consoleMethod, {
            apply(target, thisArg, args) {
                // API in preload.js
                pde.sendMessage(`console.${prop}: ${args.join(' ')}`);
                // Retain original behavior
                return Reflect.apply(...arguments);
            }
        });
    }
};
console = new Proxy(console, sendToMainHandler);

// TODO: move error handling logic to Kotlin
// TODO: Let p5.js FES do the error parsing?
addEventListener("error", ({ message, filename, lineno, colno}) => {
    pde.sendMessage(
        ["error", message, path.basename(filename), lineno, colno].join("|")
    );
});
addEventListener("unhandledrejection", ({ reason: { message, stack}}) => {
    const stackLinesWithFiles = stack.split("\n").slice(1);
    const potentialSourcesOfError = stackLinesWithFiles
        .map(line => /\/([^\/]+)\)/.exec(line)[1]);
    // TODO: improve stack trace parsing; first non-p5 file is suspected source of error
    const [filename, line, column] =
        potentialSourcesOfError.find(s => !s.startsWith("p5")).split(":");
    pde.sendMessage(
        ["error", message, filename, line, column].join("|")
    );
});