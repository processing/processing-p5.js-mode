package processing.p5js

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import processing.app.ui.EditorToolbar

class p5jsEditorToolbar(editor: p5jsEditor) : EditorToolbar(editor) {
    override fun handleRun(modifiers: Int) {
        val editor = editor as p5jsEditor
        editor.sketch.save()

        // TODO: Re-create index.html here instead of on saveAs/rename/delete of code?
        editor.createIndexHtml()

        activateRun()
        editor.statusNotice("Starting up sketch…")

        editor.scope.launch {
            // TODO: Smarter way to only install deps when needed?
            // --dangerously-allow-all-builds allows electron in particular to install properly
            editor.runCommand("pnpm install --dangerously-allow-all-builds")
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