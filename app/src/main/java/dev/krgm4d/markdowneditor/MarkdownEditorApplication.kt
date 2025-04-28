package dev.krgm4d.markdowneditor

import android.app.Application

class MarkdownEditorApplication : Application() {
    companion object {
        private var _isRearDisplay = false
        
        var isRearDisplay: Boolean
            get() = _isRearDisplay
            set(value) {
                _isRearDisplay = value
            }
    }
}
