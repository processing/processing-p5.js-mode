package processing.p5js

import kotlinx.coroutines.launch
import processing.app.ui.Editor
import processing.app.ui.EditorToolbar

class p5jsEditorToolbar(editor: p5jsEditor?) : EditorToolbar(editor) {
    override fun handleRun(modifiers: Int) {
        val editor = editor as p5jsEditor

        editor.scope.launch {
            editor.sketch.save()

            runButton.setSelected(true)
            editor.runNpmActions(editor.sketch.folder, p5jsEditor.TYPE.npx, listOf("electron", ".")){
                runButton.setSelected(false)
            }

        }
    }

    override fun handleStop() {
        val editor = editor as p5jsEditor
        editor.processes.forEach { it.destroy() }
    }
}