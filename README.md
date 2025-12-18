# p5.js Electron mode for Processing
Desktop Support for p5.js via a New Processing Mode

<img width="1600" height="998" alt="image" src="https://github.com/user-attachments/assets/ceb70725-b8b3-436e-9f8b-4e45fa7d61b2" />

This mode integrates p5.js into the Processing Development Environment (PDE) and provides a desktop runtime based on Electron.

Here is a **usage- and contribution-focused README**, with background, grant framing, and personal reflections removed.

## Features

* Run and stop p5.js sketches directly from Processing
* Electron-based embedded browser for rendering
* Error reporting in the editor
* Built-in examples (WIP)
* Syntax highlighting (Note: The current syntax highlighting system has [known limitations](https://github.com/processing/processing-p5.js-mode/wiki/Syntax-Highlighting) and may change)
* Works on Linux, macOS, and Windows
* **PNPM** for Node package and dependency management
* Automatic installation of required tools:
  * PNPM
  * Node.js
  * Electron

### Exporting Sketches

* Export p5.js sketches as standalone desktop apps
* Uses `electron-builder` for packaging

## Installation

### Using Prebuilt Releases

1. Download the latest `.pdex` file from the [Releases](https://github.com/processing/processing-p5.js-mode/releases) page.   
2. Opening the `.pdex` file in Processing

### From Source (for contributors)

* The mode is implemented in **Kotlin**
* Mode-specific code lives in the top-level `p5js` directory
* Build and distribution tasks are handled with **Gradle**

See the wiki for [architecture notes](https://github.com/processing/processing-p5.js-mode/wiki).

## Licensing

License information can be found in [LICENCE.md](LICENSE.md).