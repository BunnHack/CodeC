package com.example.terminal

import android.content.Context
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient
import com.termux.view.TerminalView

class TerminalCoordinator(
    private val context: Context
) {
    var session: TerminalSession? = null
        private set

    fun start(sessionClient: TerminalSessionClient, useProot: Boolean = false) {
        val spec = TerminalRuntime.buildSystemShell(context, useProot)
        session = TerminalSession(
            spec.executable,
            spec.workingDirectory,
            spec.args,
            spec.environment,
            2000,
            sessionClient
        )
    }

    fun attach(view: TerminalView, requestFocus: Boolean = true) {
        session?.let {
            view.attachSession(it)
            view.onScreenUpdated()
            if (requestFocus) {
                view.requestFocus()
            }
        }
    }

    fun write(text: String) {
        session?.write(text)
    }

    fun stop() {
        session?.finishIfRunning()
        session = null
    }
}
