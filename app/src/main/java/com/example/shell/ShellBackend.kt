package com.example.shell

import kotlinx.coroutines.flow.Flow

sealed class TerminalOutput {
    data class Stdout(val text: String) : TerminalOutput()
    data class Stderr(val text: String) : TerminalOutput()
}

sealed class ShellState {
    data object Idle : ShellState()
    data object Starting : ShellState()
    data object Running : ShellState()
    data class Exited(val code: Int) : ShellState()
    data class Error(val message: String) : ShellState()
}

interface ShellBackend {
    fun start(workingDir: String)
    fun write(text: String)
    fun resize(cols: Int, rows: Int)
    fun stop()
    
    val outputFlow: Flow<TerminalOutput>
    val stateFlow: Flow<ShellState>
}
