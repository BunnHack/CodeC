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

    fun start(sessionClient: TerminalSessionClient) {
        val spec = TerminalRuntime.buildSystemShell(context)
        session = TerminalSession(
            spec.executable,
            spec.workingDirectory,
            spec.args,
            spec.environment,
            2000,
            sessionClient
        )
    }

    fun attach(view: TerminalView) {
        session?.let {
            view.attachSession(it)
            view.onScreenUpdated()
            view.requestFocus()
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
