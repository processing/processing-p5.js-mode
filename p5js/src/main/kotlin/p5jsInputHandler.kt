package processing.p5js

import processing.app.Preferences
import processing.app.syntax.InputHandler
import processing.app.syntax.PdeInputHandler
import java.awt.event.KeyEvent
import java.util.*

class p5jsInputHandler(editor: p5jsEditor) : PdeInputHandler(editor) {
    init {
        this.addKeyBinding("ENTER", InputHandler.INSERT_BREAK)
        this.addKeyBinding("TAB", InputHandler.INSERT_TAB)
    }

    /**
     * Intercepts key pressed events for JEditTextArea. Shamelessly stolen from Java Mode
     *
     *
     * Called by JEditTextArea inside processKeyEvent(). Note that this
     * won't intercept actual characters, because those are fired on
     * keyTyped().
     * @return true if the event has been handled (to remove it from the queue)
     */
    override fun handlePressed(event: KeyEvent): Boolean {
        val c = event.getKeyChar()
        val code = event.getKeyCode()

        val sketch = editor.getSketch()
        val textarea = editor.getTextArea()

        if (event.isMetaDown()) {
            //if ((event.getModifiers() & InputEvent.META_MASK) != 0) {
            //event.consume();  // does nothing
            return false
        }

        if ((code == KeyEvent.VK_BACK_SPACE) || (code == KeyEvent.VK_TAB) ||
            (code == KeyEvent.VK_ENTER) || ((c.code >= 32) && (c.code < 128))
        ) {
            sketch.setModified(true)
        }

        if ((code == KeyEvent.VK_UP) && event.isControlDown()) {
            //((event.getModifiers() & InputEvent.CTRL_MASK) != 0)) {
            // back up to the last empty line
            val contents = textarea.getText().toCharArray()
            //int origIndex = textarea.getCaretPosition() - 1;
            val caretIndex = textarea.getCaretPosition()

            var index: Int = calcLineStart(caretIndex - 1, contents)
            //System.out.println("line start " + (int) contents[index]);
            index -= 2 // step over the newline
            //System.out.println((int) contents[index]);
            var onlySpaces = true
            while (index > 0) {
                if (contents[index].code == 10) {
                    if (onlySpaces) {
                        index++
                        break
                    } else {
                        onlySpaces = true // reset
                    }
                } else if (contents[index] != ' ') {
                    onlySpaces = false
                }
                index--
            }
            // if the first char, index will be -2
            if (index < 0) index = 0

            //if ((event.getModifiers() & InputEvent.SHIFT_MASK) != 0) {
            if (event.isShiftDown()) {
                textarea.setSelectionStart(caretIndex)
                textarea.setSelectionEnd(index)
            } else {
                textarea.setCaretPosition(index)
            }
            event.consume()

            //      return true;
        } else if ((code == KeyEvent.VK_DOWN) && event.isControlDown()) {
            //((event.getModifiers() & InputEvent.CTRL_MASK) != 0)) {
            val contents = textarea.getText().toCharArray()
            val caretIndex = textarea.getCaretPosition()

            var index = caretIndex
            var lineStart = 0
            var onlySpaces = false // don't count this line
            while (index < contents.size) {
                if (contents[index].code == 10) {
                    if (onlySpaces) {
                        index = lineStart // this is it
                        break
                    } else {
                        lineStart = index + 1
                        onlySpaces = true // reset
                    }
                } else if (contents[index] != ' ') {
                    onlySpaces = false
                }
                index++
            }

            //if ((event.getModifiers() & InputEvent.SHIFT_MASK) != 0) {
            if (event.isShiftDown()) {
                textarea.setSelectionStart(caretIndex)
                textarea.setSelectionEnd(index)
            } else {
                textarea.setCaretPosition(index)
            }
            event.consume()

            //      return true;
        } else if (c.code == 9) {
            //if ((event.getModifiers() & InputEvent.SHIFT_MASK) != 0) {
            if (event.isShiftDown()) {
                // if shift is down, the user always expects an outdent
                // https://github.com/processing/processing/issues/497
                editor.handleOutdent()
            } else if (textarea.isSelectionActive()) {
                editor.handleIndent()
            } else if (Preferences.getBoolean("editor.tabs.expand")) {
                val tabSize = Preferences.getInteger("editor.tabs.size")
                textarea.setSelectedText(this.spaces(tabSize))
                event.consume()
            } else {  // !Preferences.getBoolean("editor.tabs.expand")
                textarea.setSelectedText("\t")
                event.consume()
            }
        } else if (code == 10 || code == 13) {  // auto-indent
            if (Preferences.getBoolean("editor.indent")) {
                val contents = textarea.getText().toCharArray()
                val tabSize = Preferences.getInteger("editor.tabs.size")

                // this is the previous character
                // (i.e. when you hit return, it'll be the last character
                // just before where the newline will be inserted)
                val origIndex = textarea.getCaretPosition() - 1

                // if the previous thing is a brace (whether prev line or
                // up farther) then the correct indent is the number of spaces
                // on that line + 'indent'.
                // if the previous line is not a brace, then just use the
                // identical indentation to the previous line

                // calculate the amount of indent on the previous line
                // this will be used *only if the prev line is not an indent*
                var spaceCount: Int = calcSpaceCount(origIndex, contents)

                // If the last character was a left curly brace, then indent.
                // For 0122, walk backwards a bit to make sure that the there isn't a
                // curly brace several spaces (or lines) back. Also moved this before
                // calculating extraCount, since it'll affect that as well.
                var index2 = origIndex
                while ((index2 >= 0) &&
                    Character.isWhitespace(contents[index2])
                ) {
                    index2--
                }
                if (index2 != -1) {
                    // still won't catch a case where prev stuff is a comment
                    if (contents[index2] == '{') {
                        // intermediate lines be damned,
                        // use the indent for this line instead
                        spaceCount = calcSpaceCount(index2, contents)
                        spaceCount += tabSize
                    }
                }

                // now before inserting this many spaces, walk forward from
                // the caret position and count the number of spaces,
                // so that the number of spaces aren't duplicated again
                var index = origIndex + 1
                var extraCount = 0
                while ((index < contents.size) &&
                    (contents[index] == ' ')
                ) {
                    //spaceCount--;
                    extraCount++
                    index++
                }
                var braceCount = 0
                while ((index < contents.size) && (contents[index] != '\n')) {
                    if (contents[index] == '}') {
                        braceCount++
                    }
                    index++
                }

                // Hitting return on a line with spaces *after* the caret
                // can cause trouble. For 0099, was ignoring the case, but this is
                // annoying, so in 0122 we're trying to fix that.
                spaceCount -= extraCount

                if (spaceCount < 0) {
                    // for rev 0122, actually delete extra space
                    //textarea.setSelectionStart(origIndex + 1);
                    textarea.setSelectionEnd(textarea.getSelectionStop() - spaceCount)
                    textarea.setSelectedText("\n")
                    textarea.setCaretPosition(textarea.getCaretPosition() + extraCount + spaceCount)
                } else {
                    val insertion = "\n" + this.spaces(spaceCount)
                    textarea.setSelectedText(insertion)
                    textarea.setCaretPosition(textarea.getCaretPosition() + extraCount)
                }

                // not gonna bother handling more than one brace
                if (braceCount > 0) {
                    val sel = textarea.getSelectionStart()
                    // sel - tabSize will be -1 if start/end parens on the same line
                    // https://download.processing.org/bugzilla/484.html
                    if (sel - tabSize >= 0) {
                        textarea.select(sel - tabSize, sel)
                        val s: String = this.spaces(tabSize)
                        // if these are spaces that we can delete
                        if (s == textarea.getSelectedText()) {
                            textarea.setSelectedText("")
                        } else {
                            textarea.select(sel, sel)
                        }
                    }
                }
            } else {
                // Enter/Return was being consumed by somehow even if false
                // was returned, so this is a band-aid to simply fire the event again.
                // https://download.processing.org/bugzilla/1073.html
                textarea.setSelectedText(c.toString())
            }
            // mark this event as already handled (all but ignored)
            return true

            //      return true;
        } else if (c == '}') {
            if (Preferences.getBoolean("editor.indent")) {
                // first remove anything that was there (in case this multiple
                // characters are selected, so that it's not in the way of the
                // spaces for the auto-indent
                if (textarea.getSelectionStart() != textarea.getSelectionStop()) {
                    textarea.setSelectedText("")
                }

                // if this brace is the only thing on the line, outdent
                val contents = textarea.getText().toCharArray()
                // index to the character to the left of the caret
                val prevCharIndex = textarea.getCaretPosition() - 1

                // backup from the current caret position to the last newline,
                // checking for anything besides whitespace along the way.
                // if there's something besides whitespace, exit without
                // messing any sort of indenting.
                var index = prevCharIndex
                var finished = false
                while ((index != -1) && (!finished)) {
                    if (contents[index].code == 10) {
                        finished = true
                        index++
                    } else if (contents[index] != ' ') {
                        // don't do anything, this line has other stuff on it
                        return false
                    } else {
                        index--
                    }
                }
                if (!finished) return false // brace with no start

                val lineStartIndex = index

                val pairedSpaceCount: Int = calcBraceIndent(prevCharIndex, contents) //, 1);
                if (pairedSpaceCount == -1) return false

                textarea.setSelectionStart(lineStartIndex)
                textarea.setSelectedText(this.spaces(pairedSpaceCount))

                // mark this event as already handled
                event.consume()
                return true
            }
        }
        return false
    }

    /**
     * Return the index for the first character on this line.
     */
    fun calcLineStart(index: Int, contents: CharArray): Int {
        // backup from the current caret position to the last newline,
        // so that we can figure out how far this line was indented
        /*int spaceCount = 0;*/
        var index = index
        var finished = false
        while ((index != -1) && (!finished)) {
            if ((contents[index].code == 10) ||
                (contents[index].code == 13)
            ) {
                finished = true
                //index++; // maybe ?
            } else {
                index-- // new
            }
        }
        // add one because index is either -1 (the start of the document)
        // or it's the newline character for the previous line
        return index + 1
    }

    /**
     * Calculate the number of spaces on this line.
     */
    protected fun calcSpaceCount(index: Int, contents: CharArray): Int {
        var index = index
        index = calcLineStart(index, contents)

        var spaceCount = 0
        // now walk forward and figure out how many spaces there are
        while ((index < contents.size) && (index >= 0) &&
            (contents[index++] == ' ')
        ) {
            spaceCount++
        }
        return spaceCount
    }


    /**
     * Walk back from 'index' until the brace that seems to be
     * the beginning of the current block, and return the number of
     * spaces found on that line.
     */
    protected fun calcBraceIndent(index: Int, contents: CharArray): Int {
        // now that we know things are ok to be indented, walk
        // backwards to the last { to see how far its line is indented.
        // this isn't perfect cuz it'll pick up commented areas,
        // but that's not really a big deal and can be fixed when
        // this is all given a more complete (proper) solution.
        var index = index
        var braceDepth = 1
        var finished = false
        while ((index != -1) && (!finished)) {
            if (contents[index] == '}') {
                // aww crap, this means we're one deeper
                // and will have to find one more extra {
                braceDepth++
                //if (braceDepth == 0) {
                //finished = true;
                //}
                index--
            } else if (contents[index] == '{') {
                braceDepth--
                if (braceDepth == 0) {
                    finished = true
                }
                index--
            } else {
                index--
            }
        }
        // never found a proper brace, be safe and don't do anything
        if (!finished) return -1

        // check how many spaces on the line with the matching open brace
        //int pairedSpaceCount = calcSpaceCount(index, contents);
        return calcSpaceCount(index, contents)
    }


    private fun spaces(count: Int): String {
        val c = CharArray(count)
        Arrays.fill(c, ' ')
        return String(c)
    }
}