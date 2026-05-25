package com.example.terminal

import android.content.Context
import java.io.File

object TerminalRuntime {
    fun buildSystemShell(context: Context): TerminalLaunchSpec {
        // Ensure installer runs first
        RuntimeInstaller.ensureInstalled(context)

        val prefix = File(context.filesDir, "usr")
        val home = File(context.filesDir, "home")
        val tmp = File(context.cacheDir, "tmp")
        
        val executable = File(prefix, "bin/sh")
        val shExec = if (executable.exists()) executable.absolutePath else "/system/bin/sh"
        val binPath = File(prefix, "bin").absolutePath

        return TerminalLaunchSpec(
            executable = shExec,
            workingDirectory = home.absolutePath,
            args = arrayOf(shExec, "-l"),
            environment = arrayOf(
                "PREFIX=${prefix.absolutePath}",
                "HOME=${home.absolutePath}",
                "TMPDIR=${tmp.absolutePath}",
                "PATH=$binPath:/system/bin:/system/xbin",
                "TERM=xterm-256color"
            )
        )
    }
}
