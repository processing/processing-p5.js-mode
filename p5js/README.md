# p5.js Mode

## Installation

Currently, installing by double-clicking the released `.pdex` file is not working. (Tested with Processing 4.4.7 on MacOS; error says `Could not find mode in the downloaded file.`) But you can rename `p5js.pdex` to `p5js.zip` and unpack that archive into the Processing sketchbook folder; `~/Documents/Processing/modes/p5js` under MacOS. Next time you start the PDE, you have a new p5.js mode in the dropdown.

The console of the PDE is not very vocal about what is happening behind the scenes of the p5.js mode, at the moment. That’s why I suggest to start the PDE via the command line with `DEBUG=true /Applications/Processing.app/Contents/MacOS/Processing` to get the full debug logs. Once the console says `Done in … with pnpm` you can start running your p5.js sketches inside Processing.
