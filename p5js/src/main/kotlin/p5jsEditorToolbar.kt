package processing.p5js

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import processing.app.ui.EditorToolbar

class p5jsEditorToolbar(editor: p5jsEditor) : EditorToolbar(editor) {
    override fun handleRun(modifiers: Int) {
        val editor = editor as p5jsEditor

        editor.sketch.save()
        activateRun()
        editor.statusNotice("Starting up sketch…")

        editor.scope.launch {
            editor.runCommand("pnpm sketch:start") {
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