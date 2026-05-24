package com.example.terminal

data class TerminalLaunchSpec(
    val executable: String,
    val workingDirectory: String,
    val args: Array<String>? = null,
    val environment: Array<String> = emptyArray()
)
