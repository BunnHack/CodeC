package com.example.shell

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream

class SystemShellBackend(private val scope: CoroutineScope) : ShellBackend {
    private var process: Process? = null
    private var stdin: OutputStream? = null
    private var stdoutJob: Job? = null
    private var stderrJob: Job? = null

    private val _stateFlow = MutableStateFlow<ShellState>(ShellState.Idle)
    override val stateFlow: Flow<ShellState> = _stateFlow.asStateFlow()

    private val _outputFlow = MutableSharedFlow<TerminalOutput>(replay = 0)
    override val outputFlow: Flow<TerminalOutput> = _outputFlow.asSharedFlow()

    override fun start(workingDir: String) {
        if (process != null) return
        _stateFlow.value = ShellState.Starting
        
        try {
            val processBuilder = ProcessBuilder("/system/bin/sh", "-i")
            processBuilder.directory(java.io.File(workingDir))
            processBuilder.environment()["TERM"] = "xterm-256color"
            // Redirect error stream? processBuilder.redirectErrorStream(true) optionally
            
            process = processBuilder.start()
            stdin = process?.outputStream

            _stateFlow.value = ShellState.Running

            stdoutJob = scope.launch(Dispatchers.IO) {
                readStream(process?.inputStream, isError = false)
            }

            stderrJob = scope.launch(Dispatchers.IO) {
                readStream(process?.errorStream, isError = true)
            }

            scope.launch(Dispatchers.IO) {
                val exitCode = process?.waitFor() ?: -1
                _stateFlow.value = ShellState.Exited(exitCode)
                stop()
            }
        } catch (e: Exception) {
            _stateFlow.value = ShellState.Error(e.message ?: "Failed to start shell")
            stop()
        }
    }

    private suspend fun readStream(stream: InputStream?, isError: Boolean) {
        stream?.let {
            val buffer = ByteArray(1024)
            var read: Int
            while (true) {
                read = it.read(buffer)
                if (read == -1) break
                val str = String(buffer, 0, read)
                val output = if (isError) TerminalOutput.Stderr(str) else TerminalOutput.Stdout(str)
                _outputFlow.emit(output)
            }
        }
    }

    override fun write(text: String) {
        scope.launch(Dispatchers.IO) {
            try {
                stdin?.write(text.toByteArray())
                stdin?.flush()
            } catch (e: Exception) {
                // Handle broken pipe
            }
        }
    }

    override fun resize(cols: Int, rows: Int) {
        // Basic ProcessBuilder does not support resizing a PTY because there is no PTY.
        // This is why a real PTY bridge (like Termux's TerminalSession) is required for Phase 3.
    }

    override fun stop() {
        process?.destroy()
        process = null
        stdin?.close()
        stdin = null
        stdoutJob?.cancel()
        stderrJob?.cancel()
        if (_stateFlow.value is ShellState.Running) {
            _stateFlow.value = ShellState.Idle
        }
    }
}
