package processing.p5js

import processing.app.syntax.InputHandler
import processing.app.syntax.PdeInputHandler
import java.awt.event.KeyEvent

class p5jsInputHandler(editor: p5jsEditor): PdeInputHandler(editor) {
    init{
        this.addKeyBinding("ENTER", InputHandler.INSERT_BREAK)
        this.addKeyBinding("TAB", InputHandler.INSERT_TAB)
    }

    override fun handlePressed(event: KeyEvent?): Boolean {
        val c = event!!.keyChar
        val code = event.keyCode

        if ((code == KeyEvent.VK_BACK_SPACE) || (code == KeyEvent.VK_TAB) ||
            (code == KeyEvent.VK_ENTER) || ((c.code >= 32) && (c.code < 128))
        ) {
            editor.sketch.isModified = true
        }
        return super.handlePressed(event)
    }
}