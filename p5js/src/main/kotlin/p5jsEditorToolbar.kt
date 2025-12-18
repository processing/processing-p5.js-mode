package processing.p5js

import kotlinx.coroutines.launch
import processing.app.ui.EditorToolbar
import java.awt.event.ActionEvent

class p5jsEditorToolbar(editor: p5jsEditor) : EditorToolbar(editor) {
    override fun handleRun(modifiers: Int) {
        val editor = editor as p5jsEditor
        editor.sketch.save()

        val present = (modifiers and ActionEvent.SHIFT_MASK) != 0

        // TODO: Re-create index.html here instead of on saveAs/rename/delete of code?
        editor.createIndexHtml()

        activateRun()
        editor.statusNotice("Starting up sketch…")

        editor.scope.launch {
            // TODO: Smarter way to only install deps when needed?
            // --dangerously-allow-all-builds allows electron in particular to install properly
            editor.runCommand("pnpm install --dangerously-allow-all-builds")
            editor.runCommand("PRESENT=$present pnpm sketch:start") {
                deactivateRun()
                editor.statusEmpty()
            }
        }
    }

    override fun handleStop() {
        val editor = editor as p5jsEditor
        editor.sketchProcess?.destroy()
        deactivateRun()
    }
}