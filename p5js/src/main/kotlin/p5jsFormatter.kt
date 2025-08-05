package processing.p5js

import processing.app.Formatter

class p5jsFormatter: Formatter {
    override fun format(text: String?): String {
        return text!!
    }
}