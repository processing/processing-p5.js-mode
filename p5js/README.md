# p5.js Mode

## Installation

(Tested with Processing 4.4.7 on MacOS Monterey 12.7.6)

Install the mode by double-clicking the released `.pdex` file. After Processing has started up, hit "Yes" on the opening dialog, and let the mode get installed into your Processing sketchbook folder. Currently, you need to restart Processing to have the new p5.js mode show up in the mode dropdown. (This will be looked into for the September release candiate that will be published until September 30 end of day.)

After restarting Processing, choose “p5.js” from the mode dropdown. If `pnpm` is not found on your system, Processing will automatically install it and the LTS Node version. The status bar in the PDE will tell you once it is ready for you to code on your sketch.

## Demo

https://github.com/user-attachments/assets/f934430d-a71c-4995-8e20-92471e400013

## Features

- Auto-install of `pnpm`, Node, and all dependencies for running p5.js code inside the PDE
- Error reporting
- [Syntax highlighting](https://github.com/stephanmax/processing4/wiki/Syntax-Highlighting)
- Installing `npm` dependencies

## Known Issues/ToDos

- Installation
  - Prepare and test `.pdex` for Windows
- Editor UI/UX
  - Better information in the status bar (`pnpm` version, remove status messages in due time)
  - More stability around processes/coroutines (repeatedly pressing Run button should simplu restart the sketch)
  - Better/responsive npm (de-)install UI
  - Mode options
  - Start sketch in correct dimensions
  - Update `index.html` if sketch files are changed
  - Button to open dev tools in Electron
  - Hiding non-sketch files from tabs
  - Ignore `node_modules` when saving/exporting
- Error handling
  - Run linter for error messaging without having to run the sketch
  - Remove error messaging when error is fixed
- Features
  - Stand-alone app export
