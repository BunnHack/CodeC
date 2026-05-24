package com.example.terminal

import android.content.Context

object TerminalRuntime {
    fun buildSystemShell(context: Context): TerminalLaunchSpec {
        val workDir = context.filesDir.absolutePath
        return TerminalLaunchSpec(
            executable = "/system/bin/sh",
            workingDirectory = workDir,
            args = arrayOf("/system/bin/sh"),
            environment = arrayOf(
                "TERM=xterm-256color",
                "HOME=${context.filesDir.absolutePath}",
                "TMPDIR=${context.cacheDir.absolutePath}",
                "PATH=/system/bin:/system/xbin"
            )
        )
    }
}
