package com.example.terminal

import android.app.Activity
import android.content.Context
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import com.termux.terminal.TerminalSession
import com.termux.view.TerminalView
import com.termux.view.TerminalViewClient

class AppTerminalViewClient(
    private val activity: Activity,
    private val terminalView: TerminalView
) : TerminalViewClient {
    override fun onScale(scale: Float): Float = 1f

    override fun onSingleTapUp(e: MotionEvent?) {
        terminalView.requestFocus()
        val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(terminalView, InputMethodManager.SHOW_IMPLICIT)
    }

    override fun onKeyDown(keyCode: Int, e: KeyEvent?, session: TerminalSession?): Boolean = false
    override fun onKeyUp(keyCode: Int, e: KeyEvent?): Boolean = false
    override fun onCodePoint(codePoint: Int, ctrlDown: Boolean, session: TerminalSession?): Boolean = false

    override fun shouldBackButtonBeMappedToEscape() = false
    override fun shouldEnforceCharBasedInput() = false
    override fun shouldUseCtrlSpaceWorkaround() = false
    override fun isTerminalViewSelected() = true
    override fun copyModeChanged(copyMode: Boolean) {}
    override fun onLongPress(event: MotionEvent?) = false
    override fun readControlKey() = false
    override fun readAltKey() = false
    override fun readShiftKey() = false
    override fun readFnKey() = false
    override fun onEmulatorSet() {}
    override fun logError(tag: String?, message: String?) {}
    override fun logWarn(tag: String?, message: String?) {}
    override fun logInfo(tag: String?, message: String?) {}
    override fun logDebug(tag: String?, message: String?) {}
    override fun logVerbose(tag: String?, message: String?) {}
    override fun logStackTraceWithMessage(tag: String?, message: String?, e: Exception?) {}
    override fun logStackTrace(tag: String?, e: Exception?) {}
}
