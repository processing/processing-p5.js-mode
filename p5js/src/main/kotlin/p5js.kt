package processing.p5js

import processing.app.Base
import processing.app.Mode
import processing.app.ui.Editor
import processing.app.ui.EditorState
import java.io.File

class p5js(base: Base, folder: File): Mode(base, folder) {
    override fun getTitle(): String {
        return "p5.js"
    }

    override fun createEditor(base: Base?, path: String?, state: EditorState?): Editor {

        return p5jsEditor(base!!, path, state, this)
    }

    override fun getDefaultExtension(): String {
        return "js"
    }

    override fun getExtensions(): Array<String> {
        return arrayOf("js", "ts")
    }

    override fun getIgnorable(): Array<String> {
        return arrayOf("node_modules")
    }
}