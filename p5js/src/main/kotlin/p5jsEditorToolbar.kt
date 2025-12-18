package processing.p5js

import processing.app.ui.EditorToolbar
import java.awt.event.ActionEvent

class p5jsEditorToolbar(editor: p5jsEditor) : EditorToolbar(editor) {
    override fun handleRun(modifiers: Int) {
        val editor = editor as p5jsEditor
        editor.sketch.save()

        val present = (modifiers and ActionEvent.SHIFT_MASK) != 0
        editor.runSketch(present)
    }

    override fun handleStop() {
        val editor = editor as p5jsEditor
        editor.deactivateRun()
    }
}